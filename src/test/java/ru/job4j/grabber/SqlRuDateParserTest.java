package ru.job4j.grabber;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class SqlRuDateParserTest {

    @Test
    public void ifListEmptyThenEmpty() {
        assertThat(new SqlRuDateParser().parseDates(Collections.EMPTY_LIST),
                   is(Collections.emptyList()));
    }

    @Test (expected = NullPointerException.class)
    public void ifDataIsDirtyWhenNPE()  {
        SqlRuDateParser sqlRuDateParser = new SqlRuDateParser();
        LocalDateTime actual = sqlRuDateParser.parseDates(
                Arrays.asList("24 оой 20, 07:01")).get(0);
    }

    @Test
    public void ifDataIsNotDateWhenPatternWorkThenNull() {
        SqlRuDateParser sqlRuDateParser = new SqlRuDateParser();
        assertNull(
                sqlRuDateParser.parseDates(Arrays.asList(
                        "ЗдесьМоглаБытьРеклама")).get(0));
    }

    @Test
    public void whenDataOk() {
        var actual = new SqlRuDateParser().parseDates(
                Arrays.asList("сегодня, 09:57", "вчера, 10:56",
                              "2 дек 19, 22:29"));
        LocalDateTime today = LocalDateTime.now();
        assertThat(actual.get(0).getHour(), is(9));
        assertThat(actual.get(0).getMonthValue(),
                   is(today.getMonthValue()));
        assertThat(actual.get(1).getMinute(), is(56));
        assertThat(actual.get(1).getDayOfMonth(),
                   is(today.minusDays(1).getDayOfMonth()));
        assertThat(actual.get(2).getMonthValue(), is(12));
    }
}