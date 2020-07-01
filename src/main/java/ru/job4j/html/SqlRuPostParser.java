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

public class SqlRuPostParser implements Parse<Post> {
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
                Post post = parsePost(url);

                post.setAnswers(Short.parseShort(tr.child(3)
                                                   .text()));
                post.setViews(Integer.parseInt(tr.child(4)
                                                 .text()));
                post.setLastUpdated(SqlRuDateParser.parseDate(
                        tr.select(".altCol")
                          .get(1)
                          .text()));
                posts.add(post);
            }
        } catch (IOException e) {
            LOG.warn("Некорректный url", e);
        }
        return posts;
    }

    /**
     * Думаю лучше заполнять post по ходу парсинга, то есть не дожидаться,
     * когда всё запарсится и потом уже присваивать, а делать это сразу -в
     * таком случае мы хоть и можем получить "огрозочный" пост, но идейно это
     * лучше чем просто пустой. Всё равно LOG есть
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
            Element author = table.select("tr")
                                  .get(1)
                                  .selectFirst(".msgBody");
            result.setDescription(table.select("tr")
                                       .get(1)
                                       .select("td")
                                       .get(1)
                                       .text());
            result.setTopicName(eraseTags(messageHeader.text()));
            result.setAuthorUrl(author.child(0)
                                      .attr("href"));
            result.setAuthorName(author.child(0)
                                       .text());
            result.setCreated(SqlRuDateParser.parseDate(eraseTags(
                    table.selectFirst(".msgFooter")
                         .text())));
        } catch (IOException e) {
            LOG.error("Не удалось подключиться в parsePost", e);
        } catch (Exception e) {
            LOG.error("Произошло что-то страшное в parsePost", e);
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

    public static void main(String[] args) {
        new SqlRuPostParser().parsePosts("https://www.sql.ru/forum/job-offers")
                             .forEach(System.out::println);
    }
}