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

    /** Возвращает список url, котоыре нужно парсить
     */
    String[] getUrls();
}
