package ru.job4j.html;

import java.util.List;

/**
 * Интерфейс парсинга постов с сайта
 */
public interface Parse<T> {
    /**
     * Загружает список всех постов
     */
    List<T> parsePosts(String url);

    /**
     * Загружает детали одного поста
     */
    T parsePost(String url);
}
