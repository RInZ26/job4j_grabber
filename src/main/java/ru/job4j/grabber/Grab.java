package ru.job4j.grabber;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;

/**
 * Прослойка между нами и шедулером, который и будет запускать parser
 */
public interface Grab {
    <T extends Holder> void init(Store store, Scheduler scheduler, T... parsers)
            throws SchedulerException;
    //fixme как-то туповато здесь держать класс - уточнить

    /**
     * Класс - структурка, вмещаюшая парсер и список ссылок, которые он умеет
     * парсить. Если парсер - отражает конкретный сайт, то он ведь может
     * уметь парсить несколько типовых но в то же время разниц этого сайта,
     * поэтому здесь varagrs. Заодно это полезно, если мы хотим парсить сразу
     * нескольо страниц, обходясь без parseBetween - который уже чисто моя идея
     */
    class Holder {
        private Parse parser;
        private String[] urls;

        public Holder(Parse parse, String... urls) {
            this.parser = parse;
            this.urls = urls;
        }

        public Parse getParser() {
            return parser;
        }

        public String[] getUrls() {
            return urls;
        }
    }
}
