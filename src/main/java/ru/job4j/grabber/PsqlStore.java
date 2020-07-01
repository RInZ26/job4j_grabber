package ru.job4j.grabber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class PsqlStore implements Store<Post>, AutoCloseable {
    private static final String SELECT_ALL_QUERY = "SELECT * FROM post;";
    private static final String FIND_BY_ID_QUERY = "SELECT * FROM post WHERE post.id = ?;";
    private static final String INSERT_QUERY = "INSERT INTO post(link, name, "
            + "description, created) VALUES(?, ? , ?, ?);";

    /**
     * Логгер
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            PsqlStore.class.getName());
    /**
     * Коннекшен к БД
     */
    private Connection connection;

    /**
     * Выбрасывает Exception, потому что простым логгированием тут не обойтись
     *
     * @param cfg - кофиг
     *
     * @throws Exception если что-то пошло не так в иниициализации Connection
     */
    PsqlStore(Properties cfg) {
        try {
            initConnection(cfg);
        } catch (Exception e) {
            LOG.error("Инициализация connection провалена", e);
        }
    }

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

    /**
     * Стоит ли вообще тут возвращать заявку, почему бы просто не апдейдить
     * выданную?
     *
     * @param post - сохраняемая запись, возвращающая post с generatedKeys
     *
     * @return
     */
    @Override
    public void save(Post post) {
        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_QUERY, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, post.getTopicUrl());
            statement.setString(2, post.getTopicName());
            statement.setString(3, post.getDescription());
            statement.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            if (0 < statement.executeUpdate()) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        post.setId(generatedKeys.getInt("id"));
                    }
                } catch (Exception e) {
                    LOG.error("generatedKeys в save упал", e);
                }
            }
        } catch (SQLException e) {
            LOG.error("save упал {} ", post, e);
        }
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
    public Post findById(int id) {
        Post post = null;
        try (PreparedStatement statement = connection.prepareStatement(
                FIND_BY_ID_QUERY)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    post = buildPostFrom(rs);
                }
            } catch (Exception e) {
                LOG.error("rs в findById упал", e);
            }
        } catch (Exception e) {
            LOG.error("findById упал", e);
        }
        return post;
    }

    /**
     * Чтобы не писать каждый раз эти огромные конструкторы*
     *
     * @throws SQLException
     */
    private Post buildPostFrom(ResultSet rs) throws SQLException {
        return new Post(rs.getInt("id"), rs.getString("link"),
                        rs.getString("name"), rs.getString("description"),
                        rs.getTimestamp("created")
                          .toLocalDateTime());
    }

    /**
     * Загружает дефолтный cfg из resources
     */
    private static Properties getDefaultCfg() {
        Properties conf = null;
        try (InputStream in = PsqlStore.class.getClassLoader()
                                             .getResourceAsStream(
                                                     "postgre.properties")) {
            conf = new Properties();
            conf.load(in);
        } catch (Exception e) {
            LOG.error("loadCFG", e);
        }
        return conf;
    }

    public static void main(String[] args) {
        var sqlParser = new SqlRuPostParser();
        try (var psqlStore = new PsqlStore(getDefaultCfg())) {
            sqlParser.parsePosts("https://www.sql.ru/forum/job-offers/")
                     .forEach(psqlStore::save);
            psqlStore.getAll()
                     .forEach(System.out::println);
            System.out.printf("\n\n ЗАПИСЬ С ID №5 %s \n",
                              psqlStore.findById(5));
        } catch (Exception e) {
            LOG.error("main fell down", e);
        }
    }
}