package ru.job4j.html;

import java.util.Calendar;
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
    private Calendar lastUpdated;

    /**
     * Статус поста, изначально открыт
     */
    private PostStatus postStatus = PostStatus.OPENED;

    /**
     * Основной конструктор - очевидно по url, без него заявки быть не может
     * Дальнейшие конструкторы уже по важности, можно вообще будет builder
     * сделать TODO
     */
    public Post(String topicUrl) {
        this.topicUrl = topicUrl;
    }

    public Post(String topicName, String topicUrl, String authorName,
                String authorUrl, short answers, int views,
                Calendar lastUpdated) {
        this(topicUrl);
        this.topicName = topicName;
        this.authorName = authorName;
        this.authorUrl = authorUrl;
        this.answers = answers;
        this.views = views;
        this.lastUpdated = lastUpdated;
    }

    public Post(String topicName, String topicUrl, String authorName,
                String authorUrl, short answers, int views,
                Calendar lastUpdated, PostStatus postStatus) {
        this(topicName, topicUrl, authorName, authorUrl, answers, views,
             lastUpdated);
        this.postStatus = postStatus;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getTopicUrl() {
        return topicUrl;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorUrl() {
        return authorUrl;
    }

    public short getAnswers() {
        return answers;
    }

    public int getViews() {
        return views;
    }

    public Calendar getLastUpdated() {
        return lastUpdated;
    }

    public PostStatus getPostStatus() {
        return postStatus;
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

    /**
     * Мало ли сколько там может быть статусов у постов кроме "закрыт" -
     * лучше сделать сразу перечисление
     */
    public enum PostStatus {
        CLOSED("закрыт"), OPENED("");

        private String name;

        PostStatus(String name) {
            this.name = name;
        }
    }
}
