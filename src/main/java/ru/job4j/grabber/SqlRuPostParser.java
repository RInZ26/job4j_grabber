package ru.job4j.grabber;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SqlRuPostParser implements Parse {
    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            SqlRuPostParser.class.getName());

    /**
     * @param pageUrl - юрл с таблицей тем, например https://www.sql.ru/forum/job-offers/1
     *
     * @return Лист с инфой обо всех постах на странице
     */
    @Override
    public List<Post> parsePosts(String pageUrl) {
        List<Post> posts = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(pageUrl)
                                .get();
            for (Element tr : doc.selectFirst(".forumTable")
                                 .select("tr")) {
                Element postsListTopic = tr.selectFirst(".postslisttopic");
                if (Objects.isNull(postsListTopic)) {
                    continue;
                }
                String url = postsListTopic.child(0)
                                           .attr("href");
                posts.add(parsePost(url));
            }
        } catch (Exception e) {
            LOG.error("Произошло что-то страшное в parsePosts {}", pageUrl, e);
        }
        return posts;
    }

    /**
     * Парсит топик, а именно первый пост в теме.
     *
     * Каждый пост сопровождается плавующим тегом new и обойти
     * его можно только извращениями со String, потому что никакими другими
     * способами отделить text ДО класса newMessage в блоке которого и
     * содержится new - нереально
     */
    @Override
    public Post parsePost(String pageUrl) {
        Post result = new Post(pageUrl);
        try {
            Document doc = Jsoup.connect(pageUrl)
                                .get();
            Element table = doc.selectFirst(".msgTable");
            Element messageHeader = table.selectFirst(".messageHeader");
            result.setDescription(table.select("tr")
                                       .get(1)
                                       .select("td")
                                       .get(1)
                                       .text());
            result.setTopicName(eraseTags(messageHeader.text()));
            result.setCreated(SqlRuDateParser.parseDate(eraseTags(
                    table.selectFirst(".msgFooter")
                         .text())));
        } catch (Exception e) {
            LOG.error("Произошло что-то страшное в parsePost {}", pageUrl, e);
        }
        return result;
    }

    /**
     * То самое извращение со String, чтобы как-то избавиться от плавающих
     * тагов [new]
     *
     * @param topic тело messageHeader'a
     *
     * @return очищенный заголовок темы
     */
    private String eraseTags(String topic) {
        StringBuilder stringBuilder = new StringBuilder(topic);
        int startBracket;
        if ((startBracket = stringBuilder.lastIndexOf("[")) != -1) {
            stringBuilder.delete(startBracket, stringBuilder.length());
        }
        return stringBuilder.toString();
    }

    /**
     * Парсит посты с сайта в промежутке start-finish.
     * Никакие try-catch не нужны, всё уже учтено в используемых методах
     * url ДОЛЖНА заканчиваться либо на номера страниц, либо на /
     * иначе метод упадёт
     *
     * @param start  начало поиска inclusive
     * @param finish конец поиска inclusive
     * @param url    url любой по номеру страницы(она всё равно обрубится),
     *               которые нужно парсить
     *
     * @return объединненая коллекцию
     */
    @Override
    public List<Post> parsePostsBetween(int start, int finish, String url) {
        if (start < 0 || finish < 0 || finish < start || Objects.isNull(url)) {
            LOG.warn("Неадекватные входные данные в parsePosts {} - {} - {}",
                     start, finish, url);
            return Collections.emptyList();
        }
        var result = new ArrayList<Post>();
        StringBuilder defaultUrl = new StringBuilder(url);
        for (int c = start; c <= finish; c++) {
            LOG.debug("Парсируется страница {} ", c);
            defaultUrl.delete(defaultUrl.lastIndexOf("/") + 1,
                              defaultUrl.length())
                      .append(c);
            result.addAll(parsePosts(defaultUrl.toString()));
        }
        return result;
    }
}