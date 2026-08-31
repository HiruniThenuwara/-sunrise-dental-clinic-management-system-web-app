package com.sunrise.dao.impl;

import com.sunrise.dao.DBConnection;
import com.sunrise.dao.UserDao;
import com.sunrise.model.Role;
import com.sunrise.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of {@link UserDao}.
 *
 * <p>Every query uses a {@link PreparedStatement} with {@code ?} placeholders
 * rather than string concatenation. This is what makes the login screen safe
 * against SQL injection: a value such as {@code ' OR '1'='1} is treated as an
 * ordinary username to look up, not as SQL to execute.</p>
 *
 * <p>Connections are opened per operation and closed automatically by
 * try-with-resources, so no connection is ever left open.</p>
 */
public class UserDaoImpl implements UserDao {

    private static final Logger LOGGER = Logger.getLogger(UserDaoImpl.class.getName());

    private static final String SELECT_BY_USERNAME =
            "SELECT user_id, username, password_hash, salt, full_name, role, "
            + "is_active, last_login, created_at "
            + "FROM users WHERE username = ?";

    private static final String SELECT_BY_ID =
            "SELECT user_id, username, password_hash, salt, full_name, role, "
            + "is_active, last_login, created_at "
            + "FROM users WHERE user_id = ?";

    private static final String UPDATE_LAST_LOGIN =
            "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";

    private final DBConnection dbConnection;

    /** Uses the shared singleton connection source. */
    public UserDaoImpl() {
        this(DBConnection.getInstance());
    }

    /**
     * Constructor used by the integration tests so a test database can be
     * supplied instead of the live one.
     *
     * @param dbConnection the connection source to use
     */
    public UserDaoImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_USERNAME)) {

            statement.setString(1, username.trim());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read user by username", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findById(int userId) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {

            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read user by id", e);
        }
        return Optional.empty();
    }

    @Override
    public boolean updateLastLogin(int userId) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_LAST_LOGIN)) {

            statement.setInt(1, userId);
            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not update last login time", e);
            return false;
        }
    }

    /** Converts the current row of a ResultSet into a {@link User} object. */
    private User mapRow(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setUserId(resultSet.getInt("user_id"));
        user.setUsername(resultSet.getString("username"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setSalt(resultSet.getString("salt"));
        user.setFullName(resultSet.getString("full_name"));
        user.setRole(Role.fromString(resultSet.getString("role")));
        user.setActive(resultSet.getBoolean("is_active"));

        Timestamp lastLogin = resultSet.getTimestamp("last_login");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin.toLocalDateTime());
        }

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        return user;
    }
}
