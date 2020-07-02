package ru.job4j.grabber;

import java.util.List;

/**
 * Интерфейс - связь с БД
 * T - класс модели данных
 */
public interface Store<T> {
    /**
     * Сохранение. Запросам, выполняющим этот метод и saveAll лучше дружить с
     * "on conflict do nothing"
     *
     * @param post - сохраняемая запись, возвращающая post с generatedKeys
     *
     * @return лучше boolean чем войд, так хоть можно проверить через 0 <
     * executeUpdate, что пост сохранился
     */
    boolean save(T post);

    /**
     * Более удобная версия save, чтобы повысить производетельность работы, в
     * сравнении с миллионом вызовыов одноразового save
     *
     * @param posts - что заносим
     *
     * @return добавилось/не добавилось
     */
    boolean saveAll(List<T> posts);

    /**
     * Выгрузка всех постов из БД
     */
    List<T> getAll();

    /**
     * Поиск поста по id
     * Используем String - потому что это универсально, в качестве id могут
     * быть не только чиселки, но и url к примеру и т.п.
     *
     * @param id ~
     *
     * @return добавилось/не добавилось
     */
    T findById(String id);
}