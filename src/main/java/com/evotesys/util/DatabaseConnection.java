package com.evotesys.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton: single access point for the Oracle connection pool. Only DAO classes should use
 * this; UI/controller classes must not know about JDBC, HikariCP or where the credentials are
 * stored. Credentials come from environment variables (DB_URL, DB_USER, DB_PASSWORD) first;
 * config.properties is only a local-dev fallback when those are unset.
 */
public final class DatabaseConnection {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseConnection.class);
    private static final String CONFIG_FILE = "config.properties";
    private static DatabaseConnection instance;

    private final HikariDataSource dataSource;

    private DatabaseConnection() {
        Properties fallback = loadFallbackProperties();

        String url = firstNonBlank(System.getenv("DB_URL"), fallback.getProperty("db.url"));
        String user = firstNonBlank(System.getenv("DB_USER"), fallback.getProperty("db.user"));
        String password = firstNonBlank(System.getenv("DB_PASSWORD"), fallback.getProperty("db.password"));

        if (url == null || user == null || password == null) {
            throw new IllegalStateException(
                "Missing Oracle credentials. Set DB_URL/DB_USER/DB_PASSWORD environment variables, "
                    + "or provide config.properties as a fallback.");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setPoolName("evotesys-pool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10_000);

        this.dataSource = new HikariDataSource(config);
        LOG.info("Oracle connection pool initialized (pool={}, url={})", config.getPoolName(), url);
    }

    private static Properties loadFallbackProperties() {
        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            LOG.warn("Could not read {} fallback file", CONFIG_FILE, e);
        }
        return props;
    }

    private static String firstNonBlank(String primary, String secondary) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return (secondary != null && !secondary.isBlank()) ? secondary : null;
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    // Each DAO borrows its own connection from the pool and closes it in its own try-with-resources.
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
