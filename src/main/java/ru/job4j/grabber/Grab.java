package ru.job4j.grabber;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;

/**
 * Прослойка между нами и шедулером, который и будет запускать parser
 */
public interface Grab {
    void init(Parse parse, Store store, Scheduler scheduler)
            throws SchedulerException;
}
