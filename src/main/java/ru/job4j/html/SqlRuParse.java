package ru.job4j.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Класс для парсинга сайта посредством jsoup
 */
public class SqlRuParse {
    /**
     * Наш логгер и наконец-то с properties!
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            SqlRuParse.class.getName());
    /**
     * Дата парсинга сайта, чтобы учитывать корректность "сегодня/вчера" у дат
     */
    private Calendar parsingDate = new GregorianCalendar();

    public static void main(String[] args) throws Exception {
        SqlRuParse sqlRuParse = new SqlRuParse();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MM yy, kk:mm");
        sqlRuParse.parseDate(sqlRuParse.parseDoc())
                  .forEach(calendar -> System.out.println(
                          dateFormat.format(calendar.getTime())));
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
            parsingDate.setTime(new Date());
            Elements rows = doc.select(".forumTable").select("tr");
            for (Element row : rows) {
                if (Objects.isNull(row.select(".postslisttopic").first())) {
                    continue;
                }
                rawDates.add(row.child((5)).text());
            }
        } catch (Exception e) {
            LOG.error("Проблема с подключением к сайту в parseDoc", e);
        }
        return rawDates;
    }

    /**
     * Парсит уже непосредственно даты, которые мы набрали в parseDoc
     * В качестве ключа для мапы используется фраза до запятой. С IndexOutOf
     * мы всё равно не упадём, так как индекс 0, а если что-то не так -
     * вернётся дефолтная функция
     */
    public List<Calendar> parseDate(List<String> rawDates) {
        List<Calendar> dates = new ArrayList();
        Dispatcher dispatcher = new Dispatcher(parsingDate, LOG);
        for (String rawDate : rawDates) {
            if (dispatcher.RIGHT_DATE.matcher(rawDate).matches()) {
                dates.add(dispatcher.getFunctions(rawDate.split(",")[0])
                                    .apply(rawDate));
            } else {
                LOG.warn("Дата - не дата. Входные данные ошибочны");
            }
        }
        return dates;
    }
}

/**
 * Диспатчер, так как у нас есть сегодня/вчера как опции + он сам делает всю
 * грязную работу по парсингу
 */
class Dispatcher {
    /**
     * Регулярка для проверки, что перед нами что-то "похожее" на дату
     * Хотя по факту, она не спасает от падения в SimpleDateFormat parse()
     * Но всё равно должна быть полезной, т.к. позволяет "спокойно" работать
     * с  todayFunction и т.д.
     */
    public static final Pattern RIGHT_DATE = Pattern.compile(
            "((\\d{1,2} [а-я]{3} \\d{1,2})|([а-яА-Я]+)), \\d{2}:\\d{2}");
    private static final String TODAY = "сегодня";
    private static final String YESTERDAY = "вчера";

    /** Шаблон дефолтной даты вида 24 июн 20, 06:53 */
    private String defaultDatePat = "d MMM yy, k:mm";
    /** Для парсинга дефолтных дат */
    private SimpleDateFormat defaultDateFormat = new SimpleDateFormat(
            defaultDatePat, new DateFormatSymbols() {
        @Override
        public String[] getMonths() {
            return new String[]{"янв", "фев", "мар", "апр", "май", "июн", "июл",
                    "авг", "сеп", "окт", "ноя", "дек"};
        }
    });

    /** Мапа функций по ключевым словам - реализация диспатчера */
    private Map<String, Function<String, Calendar>> functions = new HashMap<>();

    /**
     * Время парсинга сайта, чтобы понимать что такое "сегодня" и "вчера".
     * Мы не можем пользоваться просто ссылкой на calendar, мало ли как он
     * изменится дальше. А нужно именно время на момент парсинга
     */
    private final Calendar parsingTime;

    /** Ссылка на родной логгер. */
    private final Logger log;

    /**
     * Лучше дружить не ссылкой на calendar из парсера, а иметь копию
     * всё-таки, потому что мало ли там во время выполнения этого, вызовется
     * новый запрос к сайту и календарь испортится
     *
     * @param calendar
     * @param log
     */
    Dispatcher(Calendar calendar, Logger log) {
        init();
        this.parsingTime = new GregorianCalendar();
        this.parsingTime.setTime(calendar.getTime());
        cleanCalendar(parsingTime);
        this.log = log;
    }

    /**
     * Заполнение мапы созданными функциями
     */
    private void init() {
        functions.put(TODAY, todayFunction());
        functions.put(YESTERDAY, yesterdayFunction());
    }

    /**
     * Счищает поля минут/часов/секунд у календаря, чтобы ему можно было
     * "некрасивым" образом их задать через dat
     */
    private void cleanCalendar(Calendar calendar) {
        calendar.clear(Calendar.HOUR_OF_DAY);
        calendar.clear(Calendar.HOUR);
        calendar.clear(Calendar.MINUTE);
        calendar.clear(Calendar.SECOND);
        calendar.clear(Calendar.MILLISECOND);
    }

    /**
     * Возвращает функцию, которая парсит "сегодня, [hh:mm]"
     * Мы можем спокойно делать эти IndexOf и прочее, потому что
     * предварительно мы проверяем все даты через RIGHT_DATE =>
     * с indexOutOfBounds не должны упасть
     * Хотя и выглядит некрасиво
     */
    private Function<String, Calendar> todayFunction() {
        return (dateStr) -> {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(parsingTime.getTime());
            StringBuffer stringBuffer = new StringBuffer(dateStr);
            String[] splitted = stringBuffer.substring(
                    dateStr.indexOf(", ") + 2).split(":");
            try {
                calendar.set(Calendar.HOUR_OF_DAY,
                             Integer.parseInt(splitted[0]));
                calendar.set(Calendar.MINUTE, Integer.parseInt(splitted[1]));
            } catch (Exception e) {
                log.error("todayFunction", e);
            }
            return calendar;
        };
    }

    /**
     * Возвращает функцию, которая парсит "вчера, [hh/mm]"
     */
    private Function<String, Calendar> yesterdayFunction() {
        return (dateStr) -> {
            Calendar calendar = todayFunction().apply(dateStr);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            return calendar;
        };
    }

    /**
     * Дефолтная функция, для работы с нормальной датой.
     * Используется предварительно clear, чтобы дата при Exception'e на выходе
     * была "явно не такой" как все и это можно было бы увидеть
     */
    private Function<String, Calendar> defaultFunction() {
        return (dataStr) -> {
            Calendar calendar = new GregorianCalendar();
            calendar.clear();
            try {
                calendar.setTime(defaultDateFormat.parse(dataStr));
            } catch (Exception e) {
                log.error("Парсинг упал", e);
            }
            return calendar;
        };
    }

    /**
     * Псеведогеттер для мапы функций
     * Псевдо, потому что возвращает не саму мапу (это нехорошо), а выполняет
     * её работу
     */
    public Function<String, Calendar> getFunctions(String query) {
        return functions.getOrDefault(query, defaultFunction());
    }
}