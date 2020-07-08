package ru.job4j.grabber;

import java.util.List;

/**
 * Интерфейс парсинга постов с сайта
 */
public interface Parse {
    /**
     * Загружает список всех постов
     */
    List<Post> parsePosts(String url);

    /**
     * Загружает детали одного поста
     */
    Post parsePost(String url);

    /**
     * Расширенная версия parePosts, позволяющая парсить диапазон страниц
     * @param start inclusive начало
     * @param finish inclusive конец
     * @param url юрлька, которая обрубается до последнего \
     * @return объединенная коллекция
     */
    List<Post> parsePostsBetween(int start, int finish, String url);

    /** Возвращает список основной url, которую может парсить парсер, ведь мы
     *  исходим из того, что каждый парсер заточен под что-то одно и точно
     *  значем, что с такими Url он справится (смотря каким методом, ведь
     *  parsePosts/parsePost всё равно работаем с разными, но обычно MainUrl
     *  мы понимаем страницу списка тем - для parsePosts)
     */
    String getMainUrl();

    /**
    Номер начальной страницы парсинга
     */
    int getStart();

    /**
     Номер конечной страницы парсинга
     */
    int getFinish();
}
