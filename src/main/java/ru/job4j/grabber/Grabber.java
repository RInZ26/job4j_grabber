package ru.job4j.grabber;

import com.sun.net.httpserver.HttpServer;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class Grabber implements Grab {
    /**
     * Логгер
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            Grabber.class.getName());
    private final Properties cfg;
    /**
     * Общее хранилище
     */
    private Store store;

    public Grabber(Properties cfg) {
        this.cfg = cfg;
    }

    public static Properties getDefaultCfg() throws IOException {
        Properties cfg = new Properties();
        try (InputStream in = Grabber.class.getClassLoader()
                                           .getResourceAsStream(
                                                   "grabber.properties")) {
            cfg.load(in);
        }
        return cfg;
    }

    public Scheduler scheduler() throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        return scheduler;
    }

    @Override
    public Store getStore() {
        return store;
    }

    @Override
    public void init(Store store, Scheduler scheduler, Parse parse)
            throws SchedulerException {
        JobDataMap data = new JobDataMap();
        this.store = store;
        data.put("store", store);
        data.put("parse", parse);
        JobDetail job = newJob(GrabJob.class).usingJobData(data)
                                             .build();
        SimpleScheduleBuilder times = simpleSchedule().withIntervalInSeconds(
                Integer.parseInt(cfg.getProperty("interval")))
                                                      .repeatForever();
        Trigger trigger = newTrigger().startNow()
                                      .withSchedule(times)
                                      .build();
        scheduler.scheduleJob(job, trigger);
    }

    public static class GrabJob implements Job {
        /**
         * Чтобы отсекать чистый JavaScript к примеру
         */
        private static final Pattern JAVA_JOB_PATTERN = Pattern.compile(
                ".*Java(?!Script).*", Pattern.CASE_INSENSITIVE);

        private static Predicate<Post> isJava = (post ->
                JAVA_JOB_PATTERN.matcher(post.getTopicName())
                                .find() || JAVA_JOB_PATTERN.matcher(
                        post.getDescription())
                                                           .find());

        @Override
        public void execute(JobExecutionContext jobExecutionContext)
                throws JobExecutionException {
            JobDataMap jobDataMap = jobExecutionContext.getJobDetail()
                                                       .getJobDataMap();
            Parse parse = (Parse) jobDataMap.get("parse");
            Store store = (Store) jobDataMap.get("store");
            store.saveAll(
                    parse.parsePostsBetween(parse.getStart(), parse.getFinish(),
                                            parse.getMainUrl())
                         .stream()
                         .filter(isJava)
                         .collect(Collectors.toList()));
            LOG.debug("Job with pages {} {} and Url was finished ",
                      parse.getStart(), parse.getFinish(), parse.getMainUrl());
        }

    }

    /**
     * Вывод store на localhost, но уже посредством создания HttpServer
     * getAll() из store получаем именно из лямбды exchange, потому что он
     * отвечает за перезагрузку страницы(неточно) и т.к. нам нужны актуальные
     * данные - пусть он их и апдейтит, а сам рефреш орагнизован через html
     */
    public void web(Store store) {
        try {
            int port = Integer.parseInt(cfg.getProperty("port"));
            HttpServer server = HttpServer.create(
                    new InetSocketAddress("localhost", port), 0);
            System.out.printf("http://localhost:%d/posts", port);
            server.createContext("/posts", exchange -> {
                var posts = store.getAll();
                StringJoiner html = new StringJoiner(System.lineSeparator());
                html.add("<!DOCTYPE html>");
                html.add("<html>"); //HTML
                html.add("<head>"); //HEAD
                html.add("<meta charset=\"UTF-8\">");
                html.add("<meta http-equiv=Refresh content=15 />");
                html.add("<style type=\"text/css\">"); //Style
                html.add("td {   font-size: 120%; \n"
                                 + "    font-family: Times New " + "Roman;"
                                 + "    color: Black;}");
                html.add("th {   font-size: 200%;" + "color: Black;}");
                html.add("body {background: NavajoWhite;}");
                html.add("</style>"); // /Style
                html.add("</head>");
                html.add("<body>"); //BODY
                html.add("<table cellpadding=30 border=10 align=center "
                                 + " bgcolor=Bisque>");
                html.add("<caption>");
                html.add("<h1> Список вакансий <h1>");
                html.add("</caption>");
                html.add("<tr>");
                html.add("<th>");
                html.add("Дата публикации:");
                html.add("</th>");
                html.add("<th>");
                html.add("Вакансия:");
                html.add("</th>");
                html.add("<th>");
                html.add("Описание:");
                html.add("</th>");
                html.add("</tr>");
                for (Post post : posts) {
                    html.add("<tr>");
                    html.add(String.format("<td align=center>%s</td>",
                                           post.getCreated()));
                    html.add(String.format(
                            "<td align=center> <a href=%s>%s </a> </td>",
                            post.getTopicUrl(), post.getTopicName()));
                    html.add(String.format("<td>%s</td>",
                                           post.getDescription()));
                    html.add("</tr>");
                }
                html.add("</table>");
                html.add("</body>");
                html.add("</html>");
                byte[] data = html.toString()
                                  .getBytes();
                exchange.sendResponseHeaders(200, data.length);
                try (var out = exchange.getResponseBody()) {
                    out.write(data);
                    out.flush();
                }
            });
            server.setExecutor(null);
            server.start();
        } catch (Exception e) {
            LOG.error("web fell down", e);
        }
    }

    public static void main(String[] args) throws Exception {
        var grabber = new Grabber(getDefaultCfg());
        Scheduler currentScheduler = grabber.scheduler();
        try (PSqlStore store = new PSqlStore(PSqlStore.getDefaultCfg())) {
            grabber.init(store, currentScheduler, new SqlRuPostParser());
            grabber.init(store, currentScheduler, new SqlRuPostParser(2, 3));
            grabber.init(store, currentScheduler, new SqlRuPostParser(4, 4));
            grabber.web(store);
            Thread.sleep(Long.MAX_VALUE);
        }
    }
}