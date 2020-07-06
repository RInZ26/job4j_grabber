package ru.job4j.grabber;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.*;
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

    public static void main(String[] args) throws Exception {
        var grabber = new Grabber(getDefaultCfg());
        Store store = new PSqlStore(PSqlStore.getDefaultCfg());
        grabber.init(store, grabber.scheduler(), new SqlRuPostParser(
                "https://www.sql.ru/forum/job-offers/1",
                "https://www.sql.ru/forum/job-offers/2"));
        grabber.init(store, grabber.scheduler(), new SqlRuPostParser(
                "https://www.sql.ru/forum/job-offers/3"));
        grabber.init(store, grabber.scheduler(), new SqlRuPostParser(
                "https://www.sql.ru/forum/job-offers/4"));
        grabber.init(store, grabber.scheduler(), new SqlRuPostParser());
        grabber.web(grabber.getStore());
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

        /**
         * Сохранение в рамках одной транзакции, для этого и posts
         */
        @Override
        public void execute(JobExecutionContext jobExecutionContext)
                throws JobExecutionException {
            JobDataMap jobDataMap = jobExecutionContext.getJobDetail()
                                                       .getJobDataMap();
            Parse parse = (Parse) jobDataMap.get("parse");
            Store store = (Store) jobDataMap.get("store");
            if (!Objects.isNull(parse.getUrls())) {
                List<Post> posts = new ArrayList<>();
                for (String url : parse.getUrls()) {
                    posts.addAll(parse.parsePosts(url));
                }
                store.saveAll(posts.stream()
                                   .filter(isJava)
                                   .collect(Collectors.toList()));
                LOG.debug("Job with urls {} was finished ",
                          Arrays.toString(parse.getUrls()));
            } else {
                LOG.debug("Urls which should be parsed not found");
            }
        }
    }

    /**
     * Вывод store на localhost
     * cp866 - кодировка, чтобы победить рутекст
     *
     * @param store
     */
    public void web(Store store) {
        LOG.debug("web was started");
        try (ServerSocket server = new ServerSocket(
                Integer.parseInt(cfg.getProperty("port")))) {
            while (!server.isClosed()) {
                Socket socket = server.accept();
                try (var out = new PrintWriter(socket.getOutputStream(), true,
                                               Charset.forName("cp866"))) {
                    out.print("HTTP/1.1 200 OK\r\n\r\n");
                    for (Post post : store.getAll()) {
                        out.print(post.toString());
                        out.print(System.lineSeparator());
                        out.print(System.lineSeparator());
                    }
                } catch (IOException io) {
                    io.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}