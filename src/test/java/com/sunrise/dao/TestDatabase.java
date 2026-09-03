package com.sunrise.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * An empty in-memory database for one test class.
 *
 * <p>Each instance gets its own H2 database named after a random identifier,
 * so test classes cannot interfere with one another even when the build runs
 * them in parallel. The database exists only while a connection is open and
 * disappears when {@link #shutdown()} is called.</p>
 *
 * <p>Using H2 rather than the clinic's MySQL server means the integration
 * tests need no database installed, no credentials and no clean-up, which is
 * what lets them run unattended on the GitHub Actions build server.</p>
 *
 * <p>The trade-off is honest to record: H2 is not MySQL, so these tests prove
 * the DAO's SQL is correct and its mapping works, but they cannot prove a
 * MySQL-specific behaviour. That is why the application is also exercised
 * against the real MySQL database before release.</p>
 */
public final class TestDatabase implements AutoCloseable {

    private final String url;
    private final DBConnection dbConnection;

    /** Creates and populates the schema for a fresh, empty database. */
    public TestDatabase() {
        this.url = "jdbc:h2:mem:sunrise_" + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE"
                + ";CASE_INSENSITIVE_IDENTIFIERS=TRUE";
        this.dbConnection = DBConnection.forTesting(url, "sa", "");
        createSchema();
    }

    /** @return the connection source to hand to a DAO under test */
    public DBConnection connectionSource() {
        return dbConnection;
    }

    /** Opens a connection, for a test that needs to set up rows directly. */
    public Connection open() throws SQLException {
        return dbConnection.getConnection();
    }

    /** Runs a statement, used by tests to insert the rows they need. */
    public void execute(String sql) {
        try (Connection connection = open();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Test setup failed for: " + sql, e);
        }
    }

    /** @return the single value of a one row, one column query */
    public int count(String sql) {
        try (Connection connection = open();
             Statement statement = connection.createStatement();
             var resultSet = statement.executeQuery(sql)) {

            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Test query failed: " + sql, e);
        }
    }

    /** Drops everything, so the memory is released between test classes. */
    public void shutdown() {
        try (Connection connection = open();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
        } catch (SQLException e) {
            // Nothing useful to do; the database is in memory and goes away
            // with the JVM in any case.
        }
    }

    @Override
    public void close() {
        shutdown();
    }

    /** Reads test-schema.sql from the test classpath and runs it. */
    private void createSchema() {
        String script = readScript();

        try (Connection connection = open();
             Statement statement = connection.createStatement()) {

            for (String command : script.split(";")) {
                if (!command.isBlank()) {
                    statement.execute(command);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create the test schema", e);
        }
    }

    private String readScript() {
        try (InputStream input = TestDatabase.class.getClassLoader()
                .getResourceAsStream("test-schema.sql")) {

            if (input == null) {
                throw new IllegalStateException(
                        "test-schema.sql was not found on the test classpath");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new IllegalStateException("Could not read test-schema.sql", e);
        }
    }
}
