package ru.job4j.grabber;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class SqlRuPostParserTest {
    /**
     * Количество постов на странице sql.ru, чтобы хоть как-то протестировать
     * parsePosts
     */
    private static final int COUNT_OF_POSTS_PER_PAGE = 53;

    @Test(expected = IllegalArgumentException.class)
    public void whenStartMoreThanfinishThenException() {
        SqlRuPostParser parser = new SqlRuPostParser(2, 1);
    }

    @Test
    public void whenParsePosts() {
        SqlRuPostParser parser = new SqlRuPostParser();
        var posts = parser.parsePosts("https://www.sql.ru/forum/job-offers");
        assertThat(posts.size(), is(COUNT_OF_POSTS_PER_PAGE));
    }

    @Test
    public void whenParsePostIsOk() {
        SqlRuPostParser parser = new SqlRuPostParser();
        var post = parser.parsePost(
                "https://www.sql" + ".ru/forum/1326595/inzhener-po-"
                        + "nagruzochnomu-testirovaniu");
        assertTrue(post.getDescription()
                       .contains("hr@bellintegrator.ru"));
        assertThat(post.getCreated(),
                   is(LocalDateTime.of(2020, 6, 19, 16, 50)));
        assertThat(post.getTopicName(),
                   is("Инженер по нагрузочному тестированию "));
    }

    @Test
    public void whenParsePostsBetweenIsOk() {
        SqlRuPostParser parser = new SqlRuPostParser();
        var posts = parser.parsePostsBetween(1, 2, parser.getMainUrl());
        assertThat(posts.size(), is(COUNT_OF_POSTS_PER_PAGE * 2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenParsePostsBetweenGotWrongParametrs() {
        new SqlRuPostParser().parsePostsBetween(3, 1, null);
    }
}
