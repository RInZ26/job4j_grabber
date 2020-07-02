package ru.job4j.grabber;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Класс - модель данных
 */
public class Post {
    /** id из БД */
    private int id;
    /**
     * Название темы
     */
    private String topicName;

    /**
     * Юрлька темы - сердце поста
     */
    private final String topicUrl;

    /**
     * Описание вакансии - первый пост
     */
    private String description;

    /**
     * Когда тема создана
     */
    private LocalDateTime created;

    /**
     * Основной конструктор - очевидно по url, без него заявки быть не может
     * Дальнейшие конструкторы уже по важности, можно вообще будет builder
     * сделать TODO
     */
    public Post(String topicUrl) {
        this.topicUrl = topicUrl;
    }

    /**
     * Конструктор из самого главного - того, что хранится в БД
     */
    public Post(int id, String topicUrl, String topicName, String description,
                LocalDateTime created) {
        this(topicUrl);
        this.id = id;
        this.topicName = topicName;
        this.created = created;
        this.description = description;
    }

    /**
     * Идейно post - это url, поэтому именно он будет в hashcode и equals
     * Все остальные поля - либо не обеспечивают уникальность(могут
     * повторяться - название темы автор), либо уж слишком динамичны (часто
     * меняются -просмотры и т.п.)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Post post = (Post) o;
        return topicUrl.equals(post.topicUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicUrl);
    }

    @Override
    public String toString() {
        return id + " " + created + " " + topicName + " " + topicUrl + " "
                + description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicUrl() {
        return topicUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }
}
