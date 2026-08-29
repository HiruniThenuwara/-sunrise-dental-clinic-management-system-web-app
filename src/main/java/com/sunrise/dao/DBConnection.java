package com.sunrise.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides database connections for the whole application.
 *
 * <p><b>Design pattern: Singleton.</b> The configuration in
 * {@code db.properties} is read from disk and the JDBC driver is registered
 * only <b>once</b>, no matter how many DAO objects are created. Every DAO
 * asks this single instance for a connection, so the connection settings
 * exist in exactly one place in the system.</p>
 *
 * <p>Note that the <i>configuration</i> is shared, but each call to
 * {@link #getConnection()} returns a <b>new</b> {@link Connection}. A single
 * JDBC connection is not thread safe, and a web application serves many
 * requests at the same time, so sharing one connection object would cause
 * errors. Each DAO method therefore opens a connection and closes it again
 * using try-with-resources.</p>
 *
 * @author Sunrise Dental Clinic Management System
 */
public final class DBConnection {

    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());

    private static final String CONFIG_FILE = "db.properties";

    /** The one and only instance (volatile so all threads see it safely). */
    private static volatile DBConnection instance;

    private final String url;
    private final String username;
    private final String password;

    /**
     * Private constructor - nobody outside this class can create an instance.
     * This is what makes the class a Singleton.
     */
    private DBConnection() {
        Properties properties = loadProperties();

        this.url = properties.getProperty("db.url");
        this.username = properties.getProperty("db.username");
        this.password = properties.getProperty("db.password", "");

        registerDriver(properties.getProperty("db.driver"));
    }

    /**
     * Returns the single shared instance, creating it on first use.
     * Double checked locking keeps this safe when several requests arrive
     * at the same time.
     *
     * @return the single {@code DBConnection} instance
     */
    public static DBConnection getInstance() {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) {
                    instance = new DBConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Opens a new database connection using the shared configuration.
     * The caller is responsible for closing it, normally with
     * try-with-resources.
     *
     * @return an open JDBC connection to the sunrise_dental database
     * @throws SQLException if the database cannot be reached
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Simple check used by the application to confirm the database is
     * reachable before showing the login screen.
     *
     * @return {@code true} if a connection can be opened
     */
    public boolean isAvailable() {
        try (Connection connection = getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Database is not reachable", e);
            return false;
        }
    }

    /** Reads db.properties from the classpath (src/main/resources). */
    private Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream input = DBConnection.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {

            if (input == null) {
                throw new IllegalStateException(
                        CONFIG_FILE + " was not found on the classpath. "
                        + "It must be in src/main/resources.");
            }
            properties.load(input);

        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + CONFIG_FILE, e);
        }
        return properties;
    }

    /** Loads the JDBC driver class so DriverManager can find it. */
    private void registerDriver(String driverClassName) {
        try {
            Class.forName(driverClassName);
            LOGGER.info("JDBC driver registered: " + driverClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "JDBC driver not found: " + driverClassName
                    + ". Check the mysql-connector-j dependency in pom.xml", e);
        }
    }
}
