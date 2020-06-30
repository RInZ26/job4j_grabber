package ru.job4j.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Класс для парсинга дат с сайта Sql.ru посредством Jsoup
 */
public class SqlRuDateParser {
    /**
     * Логгер
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            SqlRuDateParser.class.getName());
    /**
     * Мапа - вместо dateFormat, чтобы правильно парсить месяцы вида MMM с
     * тремя русскими
     * буквами
     */
    private static final Map<String, Integer> RU_MONTHS_MAP = new HashMap<>() {
        {
            put("янв", 1);
            put("фев", 2);
            put("мар", 3);
            put("апр", 4);
            put("май", 5);
            put("июн", 6);
            put("июл", 7);
            put("авг", 8);
            put("сеп", 9);
            put("окт", 10);
            put("ноя", 11);
            put("дек", 12);
        }
    };

    /**
     * Более простая реализация диспатчера "сразу на месте"
     */
    private static final Map<String, Function<String, LocalDateTime>> DISPATCHER = new HashMap<>() {
        {
            put("сегодня", SqlRuDateParser::todayFunction);
            put("вчера", SqlRuDateParser::yesterdayFunction);
        }
    };

    /**
     * Регулярка для проверки корректности строки. Чтобы обезопасить себя
     * частично от Exception'ов, связанных с IndexOf и т.п
     * Не спасёт от NPE при работе с RU_MONTHS_MAP, если [а-я]{3} включает
     * что-то постороннее
     */
    private static final Pattern RIGHT_DATE_FORMAT = Pattern.compile(
            "((\\d{1,2} [а-я]{3} \\d{1,2})|([а-яА-Я]+)), \\d{2}:\\d{2}");

    /**
     * Фукция для диспатчера, обрабатывающая случай "сегодня, hh:mm"
     */
    private static LocalDateTime todayFunction(String rawDate) {
        return LocalDateTime.of(LocalDate.now(), parseHhMm(
                rawDate.substring(rawDate.indexOf(",") + 1)));
    }

    /**
     * Метод для диспатчера, обрабатывающая случай "вчера, hh:mm"
     */
    private static LocalDateTime yesterdayFunction(String rawDate) {
        return todayFunction(rawDate).minusDays(1);
    }

    /**
     * Метод для диспатчера, обрабатывающая общий случай "dd MMM yy, hh:mm"
     */
    private static LocalDateTime defaultFunction(String rawDate) {
        String[] splitted = rawDate.split(",");
        return LocalDateTime.of(parseDdMmYy(splitted[0]),
                                parseHhMm(splitted[1]));
    }

    /**
     * Парсит конкретно hh:mm
     * Т.к. такой шаблон использутеся и в случае defaultFunction и в today и
     * т.п. - разумно вынести его отдельно
     *
     * @param rawTime строка формата hh:mm
     *
     * @return LocalTime для скрещивания с LocalDateTime
     */
    private static LocalTime parseHhMm(String rawTime) {
        rawTime = rawTime.trim();
        String[] splitted = rawTime.split(":");
        return LocalTime.of(Integer.parseInt(splitted[0]),
                            Integer.parseInt(splitted[1]));
    }

    /**
     * Парсит конкретно dd MMM yy, причем MMM берется по мапе, если месяц
     * записан как-то по-другому -> мапа ничего не найдёт -> всё упадет с NPE
     *
     * @param rawDate строка формата dd MMM YY
     *
     * @return LocalDate для скрещивания с LocalDateTime
     */
    private static LocalDate parseDdMmYy(String rawDate) {
        rawDate = rawDate.trim();
        String[] splitted = rawDate.split(" ");
        return LocalDate.of(Integer.parseInt(splitted[2]) + 2000,
                            RU_MONTHS_MAP.get(splitted[1]),
                            Integer.parseInt(splitted[0]));
    }

    /**
     * Парсит, если может, дату.
     * Если не может - возвращает null и фиксирует в логе.
     * Стоит понимать, что паттерн не проверяет корректность месяца на
     * наличие в RU_Months. И метод может выкинуть NPE, если rawDate содержит
     * некорректный месяц.
     *
     * @param rawDate парсируемое выражение
     */
    public static LocalDateTime parseDate(String rawDate) {
        if (!RIGHT_DATE_FORMAT.matcher(rawDate)
                              .matches()) {
            LOG.warn("Парсинг даты невозможен из-за некорректного формата");
            return null;
        }
        return DISPATCHER.getOrDefault(rawDate.split(",")[0].trim(),
                                       SqlRuDateParser::defaultFunction)
                         .apply(rawDate);
    }

    /**
     * Расширенная версия parseDates для удобства
     */
    public static List<LocalDateTime> parseDates(List<String> rawDate) {
        return rawDate.stream()
                      .map(SqlRuDateParser::parseDate)
                      .collect(Collectors.toList());
    }

    /**
     * Парсит данные с сайта в промежутке start-finish.
     * Никакие try-catch не нужны, всё уже учтено в используемых методах
     *
     * @param start  начало поиска
     * @param finish конец поиска
     * @param url    url любой по номеру страницы, которые нужно парсить
     *
     * @return объединненая коллекцию
     */
    public static List<LocalDateTime> parseDatesBetween(int start, int finish,
                                                        String url) {
        if (start < 0 || finish < 0 || finish < start || Objects.isNull(url)) {
            LOG.warn("Неадекватные входные данные в parseDatesBetween");
            return Collections.emptyList();
        }
        var result = new ArrayList<LocalDateTime>();
        StringBuilder defaultUrl = new StringBuilder(url);
        for (int c = start; c <= finish; c++) {
            defaultUrl.delete(defaultUrl.lastIndexOf("/") + 1,
                              defaultUrl.length())
                      .append(c);
            result.addAll(parseDates(getRawDatesFrom(defaultUrl.toString())));
        }
        return result;
    }

    /**
     * Забирает со страницы даты последнего апдейта всех тем.
     * Поддерживается запись в LOG
     * Класс postslisttopic является маркером, что табличная запись является
     * не системной, а тематической - которую можно парсить
     *
     * @param url откуда берем - понятно, что тянуться будет не с любых
     *            страниц, а только с тех где есть используемая разметка.
     */
    public static List<String> getRawDatesFrom(String url) {
        List<String> rawDates = new ArrayList<>();
        try {
            Document page = Jsoup.connect(url)
                                 .get();
            for (Element tr : page.selectFirst(".forumTable")
                                  .select("tr")) {
                if (Objects.isNull(tr.selectFirst(".postslisttopic"))) {
                    continue;
                }
                rawDates.add(tr.select(".altCol")
                               .get(1)
                               .text());
            }
        } catch (IOException e) {
            LOG.error("Некорректный url в getRawDatesFrom", e);
        } catch (Exception e) {
            LOG.error("Непридведенная ошибка в getRawDatesFrom", e);
        }
        return rawDates;
    }
}