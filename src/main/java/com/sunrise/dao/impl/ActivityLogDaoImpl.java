package com.sunrise.dao.impl;

import com.sunrise.dao.ActivityLogDao;
import com.sunrise.dao.DBConnection;
import com.sunrise.model.ActivityAction;
import com.sunrise.model.ActivityLog;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of {@link ActivityLogDao}.
 *
 * <p>The filtered query is assembled from the parts the caller actually
 * supplied, but every value is still bound as a {@code ?} parameter. Only
 * the fixed SQL fragments are concatenated, never user input, so the screen
 * cannot be used to inject SQL.</p>
 */
public class ActivityLogDaoImpl implements ActivityLogDao {

    private static final Logger LOGGER = Logger.getLogger(ActivityLogDaoImpl.class.getName());

    private static final String COLUMNS =
            "log_id, user_id, username, action, entity, entity_ref, details, "
            + "ip_address, created_at";

    private static final String INSERT =
            "INSERT INTO activity_log (user_id, username, action, entity, entity_ref, "
            + "details, ip_address) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_RECENT =
            "SELECT " + COLUMNS + " FROM activity_log ORDER BY created_at DESC, log_id DESC LIMIT ?";

    private final DBConnection dbConnection;

    public ActivityLogDaoImpl() {
        this(DBConnection.getInstance());
    }

    public ActivityLogDaoImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public ActivityLog insert(ActivityLog entry) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            if (entry.getUserId() == null) {
                statement.setNull(1, Types.INTEGER);
            } else {
                statement.setInt(1, entry.getUserId());
            }
            statement.setString(2, entry.getUsername());
            statement.setString(3, entry.getAction().name());
            statement.setString(4, entry.getEntity());
            statement.setString(5, entry.getEntityRef());
            statement.setString(6, trim(entry.getDetails(), 255));
            statement.setString(7, entry.getIpAddress());

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    entry.setLogId(keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            // Recording activity must never break the action the staff
            // member was performing, so this is logged and swallowed.
            LOGGER.log(Level.WARNING, "Could not write the activity log entry", e);
        }
        return entry;
    }

    @Override
    public List<ActivityLog> findRecent(int limit) {
        List<ActivityLog> entries = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_RECENT)) {

            statement.setInt(1, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read the activity log", e);
        }
        return entries;
    }

    @Override
    public List<ActivityLog> search(String username, String action,
                                    LocalDate from, LocalDate to, int limit) {

        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM activity_log WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();

        if (isPresent(username)) {
            sql.append(" AND username = ?");
            parameters.add(username.trim());
        }
        if (isPresent(action)) {
            sql.append(" AND action = ?");
            parameters.add(action.trim());
        }
        if (from != null) {
            sql.append(" AND created_at >= ?");
            parameters.add(Timestamp.valueOf(from.atStartOfDay()));
        }
        if (to != null) {
            sql.append(" AND created_at < ?");
            parameters.add(Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
        }

        sql.append(" ORDER BY created_at DESC, log_id DESC LIMIT ?");
        parameters.add(limit);

        List<ActivityLog> entries = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not search the activity log", e);
        }
        return entries;
    }

    @Override
    public int countByDate(LocalDate date) {
        String sql = "SELECT COUNT(*) FROM activity_log WHERE created_at >= ? AND created_at < ?";

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setTimestamp(1, Timestamp.valueOf(date.atStartOfDay()));
            statement.setTimestamp(2, Timestamp.valueOf(date.plusDays(1).atStartOfDay()));

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not count the activity for " + date, e);
            return 0;
        }
    }

    @Override
    public int countByAction(String action, LocalDate from) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM activity_log WHERE action = ?");
        if (from != null) {
            sql.append(" AND created_at >= ?");
        }

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            statement.setString(1, action);
            if (from != null) {
                statement.setTimestamp(2, Timestamp.valueOf(from.atStartOfDay()));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not count the action " + action, e);
            return 0;
        }
    }

    @Override
    public List<String> distinctUsernames() {
        List<String> names = new ArrayList<>();
        String sql = "SELECT DISTINCT username FROM activity_log "
                + "WHERE username IS NOT NULL ORDER BY username";

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                names.add(resultSet.getString(1));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not read the list of names in the log", e);
        }
        return names;
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank() && !"ALL".equalsIgnoreCase(value.trim());
    }

    /** Keeps a long message inside the column, rather than failing the insert. */
    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private ActivityLog mapRow(ResultSet resultSet) throws SQLException {
        ActivityLog entry = new ActivityLog();
        entry.setLogId(resultSet.getInt("log_id"));

        int userId = resultSet.getInt("user_id");
        entry.setUserId(resultSet.wasNull() ? null : userId);

        entry.setUsername(resultSet.getString("username"));
        entry.setAction(ActivityAction.fromString(resultSet.getString("action")));
        entry.setEntity(resultSet.getString("entity"));
        entry.setEntityRef(resultSet.getString("entity_ref"));
        entry.setDetails(resultSet.getString("details"));
        entry.setIpAddress(resultSet.getString("ip_address"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            entry.setCreatedAt(createdAt.toLocalDateTime());
        }
        return entry;
    }

    @Override
    public List<ActivityLog> searchPage(String username, String action,
                                        LocalDate from, LocalDate to,
                                        int offset, int limit) {

        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM activity_log WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        appendFilters(sql, parameters, username, action, from, to);

        sql.append(" ORDER BY created_at DESC, log_id DESC LIMIT ? OFFSET ?");
        parameters.add(Math.max(limit, 1));
        parameters.add(Math.max(offset, 0));

        List<ActivityLog> entries = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read a page of the activity log", e);
        }
        return entries;
    }

    @Override
    public int countSearch(String username, String action, LocalDate from, LocalDate to) {

        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM activity_log WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        appendFilters(sql, parameters, username, action, from, to);

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not count the activity log", e);
            return 0;
        }
    }

    /**
     * Builds the WHERE clause shared by the page query and the count query.
     * Written once so the two can never disagree about what is being
     * counted, which would put a page link where there is no page.
     */
    private void appendFilters(StringBuilder sql, List<Object> parameters,
                               String username, String action,
                               LocalDate from, LocalDate to) {
        if (isPresent(username)) {
            sql.append(" AND username = ?");
            parameters.add(username.trim());
        }
        if (isPresent(action)) {
            sql.append(" AND action = ?");
            parameters.add(action.trim());
        }
        if (from != null) {
            sql.append(" AND created_at >= ?");
            parameters.add(Timestamp.valueOf(from.atStartOfDay()));
        }
        if (to != null) {
            sql.append(" AND created_at < ?");
            parameters.add(Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
        }
    }
}
