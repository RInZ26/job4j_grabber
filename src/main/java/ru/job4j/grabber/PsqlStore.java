package ru.job4j.grabber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;

//FIXME Подлый Serial увеличивает id и при провале добавления. Нужно это
// как-то обойти
public class PsqlStore implements Store<Post>, AutoCloseable {
    private static final String SELECT_ALL_QUERY = "SELECT * FROM post;";
    private static final String FIND_BY_ID_QUERY = "SELECT * FROM post WHERE post.id = ?;";
    private static final String INSERT_QUERY = "INSERT INTO post(link, name, "
            + "description, created) VALUES(?, ? , ?, ?) ON conflict do "
            + "nothing;";

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
     * Инициализирует Connection, если может прочитать конфиг
     */
    PsqlStore(Properties cfg) {
        try {
            initConnection(cfg);
        } catch (Exception e) {
            LOG.error("Инициализация connection провалена", e);
        }
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

    /**
     * Читай интерфейс
     */
    @Override
    public boolean save(Post post) {
        boolean result = false;
        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_QUERY, Statement.RETURN_GENERATED_KEYS)) {
            fillState(statement, post.getTopicUrl(), post.getTopicName(),
                      post.getDescription(), post.getCreated());
            result = 0 < statement.executeUpdate();
        } catch (SQLException e) {
            LOG.error("fillState упал {}", post, e);
        } catch (Exception e) {
            LOG.error("save упал {} ", post, e);
        }
        return result;
    }

    @Override
    public boolean saveAll(List<Post> posts) {
        int countOfSaved = 0;
        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_QUERY)) {
            for (Post post : posts) {
                try { //чтобы fore не прекращался  - отдельный try
                    fillState(statement, post.getTopicUrl(),
                              post.getTopicName(), post.getDescription(),
                              post.getCreated());
                    countOfSaved += statement.executeUpdate();
                } catch (SQLException e) {
                    LOG.error("fillState упал {}", post, e);
                }
            }
        } catch (Exception e) {
            LOG.error("saveAll упал, было сохранено {} записей ", countOfSaved,
                      e);
        }
        return countOfSaved == posts.size();
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

   /* static Consumer<List<Post>> saver = (posts) -> {
        try (var psqlStore = new PsqlStore(getDefaultCfg())) {
            posts.forEach(psqlStore::save);
        } catch (Exception e) {
            LOG.error("main fell down", e); //3254283900   100 pages
        }
    };

    static Consumer<List<Post>> allSaver = (posts) -> {
        try (var psqlStore = new PsqlStore(getDefaultCfg())) {
            psqlStore.saveAll(posts);
        } catch (Exception e) {
            LOG.error("main fell down", e); //2186217900   100 pages
        }
    };

    public static void main(String[] args) {
        var sqlParser = new SqlRuPostParser();
        List<Post> posts = sqlParser.parsePostsBetween(1, 10, "https://www"
                + ".sql.ru/forum/job-offers/2");
        analyze(saver, posts);
        analyze(allSaver, posts);
    }

    static void analyze(Consumer f, List<Post> posts) {
        rebase();
        LocalTime start, finish;
        start = LocalTime.now();
        f.accept(posts);
        finish = LocalTime.now();
        System.out.println(
                "diff  secs " + (finish.getSecond() - start.getSecond()));
        System.out.println(
                "diff nanosecs  " + (finish.toNanoOfDay() - start.toNanoOfDay()));
    }

    static void rebase() {
        try (var sqlStore = new PsqlStore(getDefaultCfg())) {
            try (PreparedStatement st = sqlStore.connection.prepareStatement(
                    "DROP TABLE IF EXISTS post;\n" + "CREATE TABLE post(\n"
                            + "id SERIAL PRIMARY KEY,\n"
                            + "link VARCHAR(200) NOT NULL UNIQUE,\n"
                            + "name VARCHAR(150),\n" + "description TEXT,\n"
                            + "created TIMESTAMP\n" + ");")) {
                st.execute();
            }
        } catch (Exception e) {
            LOG.error("gg", e);
        }
    }*/
}