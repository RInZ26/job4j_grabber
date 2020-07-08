package ru.job4j.grabber;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
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
        Scheduler currentScheduler = grabber.scheduler();
        try (PSqlStore store = new PSqlStore(PSqlStore.getDefaultCfg())) {
            grabber.init(store, currentScheduler, new SqlRuPostParser());
            grabber.init(store, currentScheduler, new SqlRuPostParser(2, 3));
            grabber.init(store, currentScheduler, new SqlRuPostParser(4, 4));
            Thread.sleep(30000);
            currentScheduler.pauseAll();
            currentScheduler.shutdown();
            grabber.web(grabber.getStore());
        }
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
     * cp866 - кодировка, чтобы победить рутекст
     */
    public void web(Store store) {
        try {
            int port = Integer.parseInt(cfg.getProperty("port"));
            HttpPostsServer server = new HttpPostsServer(port, store.getAll());
            LOG.debug("webServer was started http://localhost:{}/posts", port);
            server.startServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}