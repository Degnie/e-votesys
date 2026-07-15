package com.evotesys.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton: single access point for the Oracle connection. Only DAO classes should use this;
 * UI/controller classes must not know about JDBC or where the credentials are stored.
 */
public final class DatabaseConnection {

    private static final String CONFIG_FILE = "config.properties";
    private static DatabaseConnection instance;

    private final String url;
    private final String user;
    private final String password;

    private DatabaseConnection() {
        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new IllegalStateException(
                    CONFIG_FILE + " not found on the classpath. Copy config.properties.example "
                        + "into the NetBeans project source root, rename it to config.properties "
                        + "and fill in your Oracle credentials.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + CONFIG_FILE, e);
        }

        this.url = props.getProperty("db.url");
        this.user = props.getProperty("db.user");
        this.password = props.getProperty("db.password");

        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("ojdbc driver not found on the classpath. Add ojdbc8.jar (or newer) to the project libraries.", e);
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    // Each DAO requests its own connection and closes it in its own try-with-resources.
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
