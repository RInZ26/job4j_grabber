package ru.job4j.grabber;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Класс - модель данных
 */
public class Post {
    /**
     * Название темы
     */
    private String topicName;

    /**
     * Юрлька темы - сердце поста
     */
    private final String topicUrl;

    /**
     * Ник автора темы
     */
    private String authorName;

    /**
     * Юрлька автора
     */
    private String authorUrl;

    /**
     * Количество постов (кроме первого поста автора) в теме
     */
    private short answers;

    /**
     * Количество просмотров темы
     */
    private int views;

    /**
     * Дата последнего поста в теме
     */
    private LocalDateTime lastUpdated;

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
        return topicName + " " + topicUrl + " " + description;
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

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorUrl() {
        return authorUrl;
    }

    public void setAuthorUrl(String authorUrl) {
        this.authorUrl = authorUrl;
    }

    public short getAnswers() {
        return answers;
    }

    public void setAnswers(short answers) {
        this.answers = answers;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
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
