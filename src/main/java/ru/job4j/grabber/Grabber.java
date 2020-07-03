package ru.job4j.grabber;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
    public void init(Store store, Scheduler scheduler, Holder... holders)
            throws SchedulerException {
        JobDataMap data = new JobDataMap();
        this.store = store;
        data.put("store", store);
        data.put("holders", holders);
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
            Holder[] holders = (Holder[]) jobDataMap.get("holders");
            Store store = (Store) jobDataMap.get("store");
            List<Post> posts = new ArrayList<>();
            for (Holder holder : holders) {
                for (String url : holder.getUrls()) {
                    posts.addAll(holder.getParser()
                                       .parsePosts(url)
                                       .stream()
                                       .filter(isJava)
                                       .collect(Collectors.toList()));
                }
            }
            store.saveAll(posts);
        }
    }

    public Store getStore() {
        return store;
    }

    public static void main(String[] args) throws Exception {
        Grabber grab = new Grabber(Grabber.getDefaultCfg());
        try (AutoCloseable store = new PSqlStore(PSqlStore.getDefaultCfg())) {
            Scheduler scheduler = grab.scheduler();
            grab.init((Store) store, scheduler,
                      new Holder(new SqlRuPostParser(),
                                 "https://www.sql" + ".ru" + "/forum" + "/job"
                                         + "-offers/1",
                                 "https://www.sql" + ".ru" + "/forum" + "/job"
                                         + "-offers" + "/2"),
                      new Holder(new SqlRuPostParser(),
                                 "https://www.sql.ru/forum/job/1"));
            Thread.sleep(60000);
            scheduler.shutdown();
            grab.store.getAll()
                      .forEach(System.out::println);
        }
    }
}


