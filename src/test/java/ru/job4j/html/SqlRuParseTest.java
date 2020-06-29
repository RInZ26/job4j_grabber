package ru.job4j.html;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SqlRuParseTest {

    @Test
    public void ifListEmptyThenEmpty() {
        assertThat(new SqlRuParse().parseDate(Collections.EMPTY_LIST),
                   is(Collections.emptyList()));
    }

    @Test
    public void ifDataIsDirtyWhen1970Date() {
        SqlRuParse sqlRuParse = new SqlRuParse();
        Calendar actual = sqlRuParse.parseDate(
                Arrays.asList("24 оой 20, 07:01")).get(0);
        Calendar expected = new GregorianCalendar();
        expected.clear();
        assertThat(actual, is(expected));
    }

    @Test
    public void ifDataIsNotDateWhenPatternWork() {
        SqlRuParse sqlRuParse = new SqlRuParse();
        assertThat(sqlRuParse.parseDate(Arrays.asList("ЗдесьМоглаБытьРеклама")),
                   is(Collections.emptyList()));
    }

    @Test
    public void whenDataOk() {
        var actual = new SqlRuParse().parseDate(
                Arrays.asList("сегодня, 09:57", "вчера, 10:56",
                              "2 дек 19, 22:29"));
        Calendar today = new GregorianCalendar();
        assertThat(actual.get(0).get(Calendar.HOUR_OF_DAY), is(9));
        assertThat(actual.get(0).get(Calendar.DAY_OF_MONTH),
                   is(today.get(Calendar.DAY_OF_MONTH)));
        assertThat(actual.get(1).get(Calendar.MINUTE), is(56));
        assertThat(actual.get(1).get(Calendar.DAY_OF_MONTH),
                   is(today.get(Calendar.DAY_OF_MONTH) - 1));
        assertThat(actual.get(2).get(Calendar.MONTH), is(11));
    }
}