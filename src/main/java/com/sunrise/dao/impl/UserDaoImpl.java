package com.sunrise.dao.impl;

import com.sunrise.dao.DBConnection;
import com.sunrise.dao.UserDao;
import com.sunrise.model.Role;
import com.sunrise.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
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

    private static final String COLUMNS =
            "user_id, username, password_hash, salt, full_name, role, "
            + "is_active, last_login, created_at";

    private static final String SELECT_BY_USERNAME =
            "SELECT " + COLUMNS + " FROM users WHERE username = ?";

    private static final String SELECT_BY_ID =
            "SELECT " + COLUMNS + " FROM users WHERE user_id = ?";

    private static final String SELECT_ALL =
            "SELECT " + COLUMNS + " FROM users ORDER BY role, full_name";

    private static final String UPDATE_LAST_LOGIN =
            "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";

    private static final String INSERT =
            "INSERT INTO users (username, password_hash, salt, full_name, role, is_active) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE =
            "UPDATE users SET full_name = ?, role = ?, is_active = ? WHERE user_id = ?";

    private static final String UPDATE_PASSWORD =
            "UPDATE users SET password_hash = ?, salt = ? WHERE user_id = ?";

    private static final String SET_ACTIVE =
            "UPDATE users SET is_active = ? WHERE user_id = ?";

    private static final String USERNAME_EXISTS =
            "SELECT 1 FROM users WHERE username = ? AND user_id <> ? LIMIT 1";

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
    public List<User> findAll() {
        List<User> users = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                users.add(mapRow(resultSet));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read the staff list", e);
        }
        return users;
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

    @Override
    public User insert(User user) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getSalt());
            statement.setString(4, user.getFullName());
            statement.setString(5, user.getRole().name());
            statement.setBoolean(6, user.isActive());

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setUserId(keys.getInt(1));
                }
            }
            LOGGER.info("Staff account created: " + user.getUsername());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not create the staff account", e);
        }
        return user;
    }

    @Override
    public boolean update(User user) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {

            statement.setString(1, user.getFullName());
            statement.setString(2, user.getRole().name());
            statement.setBoolean(3, user.isActive());
            statement.setInt(4, user.getUserId());

            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not update the staff account", e);
            return false;
        }
    }

    @Override
    public boolean updatePassword(int userId, String passwordHash, String salt) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_PASSWORD)) {

            statement.setString(1, passwordHash);
            statement.setString(2, salt);
            statement.setInt(3, userId);

            boolean changed = statement.executeUpdate() == 1;
            if (changed) {
                // The password itself is never logged, only that it changed.
                LOGGER.info("Password changed for user id " + userId);
            }
            return changed;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not change the password", e);
            return false;
        }
    }

    @Override
    public boolean setActive(int userId, boolean active) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SET_ACTIVE)) {

            statement.setBoolean(1, active);
            statement.setInt(2, userId);
            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not change the account status", e);
            return false;
        }
    }

    @Override
    public boolean usernameExists(String username, int excludeUserId) {
        if (username == null || username.isBlank()) {
            return false;
        }
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(USERNAME_EXISTS)) {

            statement.setString(1, username.trim());
            statement.setInt(2, excludeUserId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            // On error the safe answer is "taken", so a duplicate account is
            // never created on the strength of a failed check.
            LOGGER.log(Level.SEVERE, "Could not check the username", e);
            return true;
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
