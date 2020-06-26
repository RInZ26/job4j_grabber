package ru.job4j.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;

/**
 * Класс для парсинга сайта посредством jsoup
 */
public class SqlRuParse {

    private static final Logger LOG = LoggerFactory.getLogger(
            SqlRuParse.class.getName());
    /**
     * Для хранения уже отпарсенных дат
     */
    private List<Date> dates = new ArrayList<>();
    /**
     * Дата парсинга сайта, чтобы учитывать корректность "сегодня/вчера" у дат
     */
    private Date parsingDate;
    private DateFormatSymbols ruDate = new DateFormatSymbols() {

        @Override
        public String[] getMonths() {
            return new String[]{"янв", "фев", "мар", "апр", "май", "июн", "июл",
                    "авг", "сеп", "окт", "ноя", "дек"};
        }
    };

    private Dispatcher dispatcher = new Dispatcher();
    private String datePattern = "d MMM yy, k:mm";

    /**
     * Общая идея:
     * Захватываем всю таблицу, далее дробим её по тегу tr - это одна
     * конкретная запись, иными словами строка в таблице. Но, вперемешку с
     * системными (Заголовочная tr), поэтому нужна проверка на Null, когда мы
     * ищем по классу .postslisttopic - это ликвидная строка в таблице с темой
     *
     * Из минусов - идёт работа с child через индекс - это не совсем хорошо,
     * можно упасть с IndexOutOfBounds
     */
    public static void main(String[] args) throws Exception {
        SqlRuParse sqlRuParse = new SqlRuParse();
        sqlRuParse.parseDoc().forEach(System.out::println);

        sqlRuParse.parseDate(sqlRuParse.parseDoc());
        sqlRuParse.dates.forEach(System.out::println);
    }

    /**
     * Парсим непосредственно таблицу (.forumtable), класс .postslisttopic
     * позволяет проверть - является ли строка обычноый или системной
     * (например заголовки), что в свою очередь даёт "гарантию", что у строки
     * будет child(5), что и является датой
     * Результаты заносим в список для последующей обработки
     */
    public List<String> parseDoc() {
        List<String> rawDates = new ArrayList<>();
        try {
            Document doc = Jsoup.connect("https://www.sql.ru/forum/job-offers")
                                .get();
            parsingDate = new Date();
            Elements rows = doc.select(".forumTable").select("tr");
            for (Element row : rows) {
                if (Objects.isNull(row.select(".postslisttopic").first())) {
                    continue;
                }
                rawDates.add(row.child((5)).text());
            }
        } catch (Exception e) {
            LOG.error("Проблема с подклчением к сайту в parseDoc", e);
        }
        //"MMM"
        return rawDates;
    }

    public void parseDate(List<String> rawDates) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern, ruDate);
        try {
            for (String rawDate : rawDates) {
                dates.add(
                        dispatcher.functions.getOrDefault(rawDate.split(",")[0],
                                                          (() -> {
                                                              try {
                                                                  return dateFormat
                                                                          .parse(rawDate);
                                                              } catch (ParseException e) {
                                                                  e.printStackTrace();
                                                              }
                                                              return null;
                                                          })).get());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class Dispatcher {
        private Map<String, Supplier<Date>> functions = new HashMap<>();
        private final String today = "сегодня";
        private final String yesterday = "вчера";

        Dispatcher() {
            init();
        }

        private void init() {
            functions.put(today, todaySuppplier());
            functions.put(yesterday, yesterdaySupplier());
        }

        Supplier<Date> todaySuppplier() {
            return () -> getToday();
        }

        Supplier<Date> yesterdaySupplier() {
            return () -> getYesterday();
        }

        private Date getToday() {
            return SqlRuParse.this.parsingDate;
        }

        private Date getYesterday() {
            long day = 60 * 60 * 24;
            return new Date(getToday().getTime() - day);
        }
    }
}