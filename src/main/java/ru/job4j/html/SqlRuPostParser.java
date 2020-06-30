package ru.job4j.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SqlRuPostParser {
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
    public static List<Post> parsePost(String pageUrl) {
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
                Post post = new Post(postsListTopic.child(0)
                                                   .attr("href"));
                post.setTopicName(postsListTopic.child(0)
                                                .text());
                Element author = tr.selectFirst(".altCol");
                post.setAuthorName(author.child(0)
                                         .text());
                post.setAuthorUrl(author.child(0)
                                        .attr("href"));
                post.setAnswers(Short.parseShort(tr.child(3)
                                                   .text()));
                post.setViews(Integer.parseInt(tr.child(4)
                                                 .text()));
                post.setLastUpdated(SqlRuDateParser.parseDate(
                        tr.select(".altCol")
                          .get(1)
                          .text()));
                parseTopic(post);
                posts.add(post);
            }
        } catch (IOException e) {
            LOG.warn("Некорректный url", e);
        }
        return posts;
    }

    /**
     * Парсит тему по ссылке на предмет первого сообщения и апдейтит
     * переданный Post.
     * Сначала пытаемся всё спарсить, уже потом апдейтим post, чтобы не было
     * огрызков, если данные некорректны
     *
     * По поводу created - сплитуется по [ из-за наполнения msgFooter - вчера,
     * 11:01    [22158875]
     */
    private static boolean parseTopic(Post post) {
        boolean result = false;
        try {
            Document doc = Jsoup.connect(post.getTopicUrl())
                                .get();
            String description = doc.selectFirst(".msgTable")
                                    .select(".msgBody")
                                    .get(1)
                                    .text();
            String created = doc.selectFirst(".msgFooter")
                                .text()
                                .split("\\[")[0].trim();
            post.setCreated(SqlRuDateParser.parseDate(created));
            post.setDescription(description);
            result = true;
        } catch (Exception e) {
            LOG.error("parseTopic упал", e);
        }
        return result;
    }

    public static void main(String[] args) {
        parsePost("https://www.sql.ru/forum/job-offers/1").forEach(
                System.out::println);
    }
}
