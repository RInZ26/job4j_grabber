package ru.job4j.grabber;

import java.util.List;

/**
 * Интерфейс - связь с БД
 * T - класс модели данных
 * V - тип возвращаего id через - generatedKeys
 */
public interface Store<T, V> {
    /**
     * Сохранение
     *
     * @param post - сохраняемая запись
     */
    V save(T post);

    /**
     * Выгрузка всех постов из БД
     */
    List<T> getAll();
}