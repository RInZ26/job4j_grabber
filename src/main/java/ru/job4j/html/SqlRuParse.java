package ru.job4j.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Objects;

public class SqlRuParse {
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
        Document doc = Jsoup.connect("https://www.sql.ru/forum/job-offers")
                            .get();
        Elements rows = doc.select(".forumTable").select("tr");
        for (Element row : rows) {
            Element ref = row.select(".postslisttopic").first();
            if (Objects.isNull(ref)) {
                continue;
            }
            Element title = ref.child(0);
            System.out.println(title.text());
            System.out.println(title.attr("href"));
            System.out.println(row.child(5).text() + System.lineSeparator());
        }
    }
}