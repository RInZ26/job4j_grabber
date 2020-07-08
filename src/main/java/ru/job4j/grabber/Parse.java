package ru.job4j.grabber;

import java.util.List;

/**
 * Интерфейс парсинга постов с сайта
 */
public interface Parse {
    int A = 3;
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
     *  исходим из того, что каждый парсер заточен под что-то одно
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
