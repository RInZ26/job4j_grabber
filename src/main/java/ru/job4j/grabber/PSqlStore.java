package ru.job4j.grabber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

//  Подлый Serial ломает структуру id'шников, при добавлени дублей
//  (инкрементация происходит, хотя insert не срабатывает)
public class PSqlStore implements Store, AutoCloseable {
    private static final String SELECT_ALL_QUERY = "SELECT * FROM post;";
    private static final String FIND_BY_ID_QUERY = "SELECT * FROM post WHERE post.id = ?;";
    private static final String INSERT_QUERY = "INSERT INTO post(link, name, "
            + "description, created) VALUES(?, ? , ?, ?) on conflict do "
            + "nothing;";

    /**
     * Логгер
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            PSqlStore.class.getName());
    /**
     * Коннекшен к БД
     */
    private Connection connection;

    /**
     * Инициализирует Connection, если может прочитать конфиг
     */
    PSqlStore(Properties cfg) {
        try {
            initConnection(cfg);
        } catch (Exception e) {
            LOG.error("Инициализация connection провалена", e);
        }
    }

    /**
     * Загружает дефолтный cfg из resources
     */
    public static Properties getDefaultCfg() {
        Properties conf = null;
        try (InputStream in = PSqlStore.class.getClassLoader()
                                             .getResourceAsStream(
                                                     "postgre.properties")) {
            conf = new Properties();
            conf.load(in);
        } catch (Exception e) {
            LOG.error("loadCFG", e);
        }
        return conf;
    }

    /**
     * Попытка прочесть конфиг
     */
    private void initConnection(Properties cfg) throws Exception {
        Class.forName(cfg.getProperty("jdbc.driver"));
        connection = DriverManager.getConnection(cfg.getProperty("jdbc.url"),
                                                 cfg.getProperty(
                                                         "jdbc.username"),
                                                 cfg.getProperty(
                                                         "jdbc.password"));
    }

    @Override
    public void close() throws Exception {
        if (!Objects.isNull(connection)) {
            connection.close();
        }
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_QUERY, Statement.RETURN_GENERATED_KEYS)) {
            fillState(statement, post.getTopicUrl(), post.getTopicName(),
                      post.getDescription(), post.getCreated());
            statement.execute();
        } catch (SQLException e) {
            LOG.error("fillState упал {}", post, e);
        } catch (Exception e) {
            LOG.error("save упал {} ", post, e);
        }
    }

    /**
     * Сохранение заявок через механизм batch-команд, когда мы заранее
     * оформляет пачку запросов, потом её коммитим через одну команду
     * @param posts - что заносим
     */
    @Override
    public void saveAll(List<Post> posts) {
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(
                    INSERT_QUERY)) {
                for (Post post : posts) {
                    fillState(statement, post.getTopicUrl(),
                              post.getTopicName(), post.getDescription(),
                              post.getCreated());
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                LOG.error("Не удалось выполнить работу с Batch)",
                          e);
            }
        } catch (Exception e) {
            LOG.error("saveAll упал ", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (Exception e) {
                LOG.error("Не удалось вернуть autoCommit в true", e);
            }
        }
    }

    /**
     * Следуем DRY и заполняем PreparedStatement здесь. Полей(параметров) не
     * так и много, чтобы оборачивать это в varargs с черной магией и
     * диспатчером
     */
    private void fillState(PreparedStatement statement, String url, String name,
                           String description, LocalDateTime created)
            throws SQLException {
        statement.setString(1, url);
        statement.setString(2, name);
        statement.setString(3, description);
        statement.setTimestamp(4, Timestamp.valueOf(created));
    }

    @Override
    public List<Post> getAll() {
        List<Post> posts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                SELECT_ALL_QUERY)) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    posts.add(buildPostFrom(rs));
                }
            } catch (Exception e) {
                LOG.error("rs в getAll упал", e);
            }
        } catch (Exception e) {
            LOG.error("getAll упал", e);
        }
        return posts;
    }

    @Override
    public Post findById(String id) {
        Post post = null;
        try (PreparedStatement statement = connection.prepareStatement(
                FIND_BY_ID_QUERY)) {
            statement.setInt(1, Integer.parseInt(id));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    post = buildPostFrom(rs);
                }
            } catch (Exception e) {
                LOG.error("rs в findById упал", e);
            }
        } catch (Exception e) {
            LOG.error("findById упал {}", id, e);
        }
        return post;
    }

    /**
     * Чтобы не писать каждый раз эти огромные конструкторы*
     */
    private Post buildPostFrom(ResultSet rs) throws SQLException {
        return new Post(rs.getInt("id"), rs.getString("link"),
                        rs.getString("name"), rs.getString("description"),
                        rs.getTimestamp("created")
                          .toLocalDateTime());
    }

    public static void main(String[] args) {
        try (var sqlStore = new PSqlStore(getDefaultCfg())) {
            var sqlParser = new SqlRuPostParser();
            sqlStore.saveAll(sqlParser.parsePostsBetween(1, 3,
                                                         "https://www.sql.ru/forum/job-offers/"));
            sqlStore.getAll()
                    .forEach(System.out::println);
        } catch (Exception e) {
            LOG.error("main fell down", e);
        }
    }
}