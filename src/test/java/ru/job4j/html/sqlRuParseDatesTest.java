package ru.job4j.html;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class sqlRuParseDatesTest {

    @Test
    public void ifListEmptyThenEmpty() {
        assertThat(new SqlRuParseDates().parseDates(Collections.EMPTY_LIST),
                   is(Collections.emptyList()));
    }

    @Test (expected = NullPointerException.class)
    public void ifDataIsDirtyWhenNPE()  {
        SqlRuParseDates sqlRuParseDates = new SqlRuParseDates();
        LocalDateTime actual = sqlRuParseDates.parseDates(
                Arrays.asList("24 оой 20, 07:01")).get(0);
    }

    @Test
    public void ifDataIsNotDateWhenPatternWorkThenNull() {
        SqlRuParseDates sqlRuParseDates = new SqlRuParseDates();
        assertNull(
                sqlRuParseDates.parseDates(Arrays.asList(
                        "ЗдесьМоглаБытьРеклама")).get(0));
    }

    @Test
    public void whenDataOk() {
        var actual = new SqlRuParseDates().parseDates(
                Arrays.asList("сегодня, 09:57", "вчера, 10:56",
                              "2 дек 19, 22:29"));
        LocalDateTime today = LocalDateTime.now();
        assertThat(actual.get(0).getHour(), is(9));
        assertThat(actual.get(0).getMonthValue(),
                   is(today.getMonthValue()));
        assertThat(actual.get(1).getMinute(), is(56));
        assertThat(actual.get(1).getDayOfMonth(),
                   is(today.getDayOfMonth() - 1));
        assertThat(actual.get(2).getMonthValue(), is(12));
    }
}