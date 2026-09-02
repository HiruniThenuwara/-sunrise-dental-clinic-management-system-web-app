package com.sunrise.dao.impl;

import com.sunrise.dao.DBConnection;
import com.sunrise.dao.TreatmentDao;
import com.sunrise.model.Treatment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of {@link TreatmentDao}.
 */
public class TreatmentDaoImpl implements TreatmentDao {

    private static final Logger LOGGER = Logger.getLogger(TreatmentDaoImpl.class.getName());

    private static final String COLUMNS =
            "treatment_id, treatment_name, description, base_cost, estimated_minutes, is_active";

    private static final String SELECT_ALL =
            "SELECT " + COLUMNS + " FROM treatments ORDER BY base_cost, treatment_name";

    private static final String SELECT_ACTIVE =
            "SELECT " + COLUMNS + " FROM treatments WHERE is_active = 1 ORDER BY base_cost, treatment_name";

    private static final String SELECT_BY_ID =
            "SELECT " + COLUMNS + " FROM treatments WHERE treatment_id = ?";

    private static final String SELECT_BY_NAME =
            "SELECT " + COLUMNS + " FROM treatments WHERE treatment_name = ?";

    private static final String INSERT =
            "INSERT INTO treatments (treatment_name, description, base_cost, "
            + "estimated_minutes, is_active) VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE =
            "UPDATE treatments SET treatment_name = ?, description = ?, base_cost = ?, "
            + "estimated_minutes = ?, is_active = ? WHERE treatment_id = ?";

    private static final String SET_ACTIVE =
            "UPDATE treatments SET is_active = ? WHERE treatment_id = ?";

    private final DBConnection dbConnection;

    public TreatmentDaoImpl() {
        this(DBConnection.getInstance());
    }

    public TreatmentDaoImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public List<Treatment> findAll() {
        return query(SELECT_ALL);
    }

    @Override
    public List<Treatment> findAllActive() {
        return query(SELECT_ACTIVE);
    }

    @Override
    public Optional<Treatment> findById(int treatmentId) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {

            statement.setInt(1, treatmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read treatment " + treatmentId, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Treatment> findByName(String treatmentName) {
        if (treatmentName == null || treatmentName.isBlank()) {
            return Optional.empty();
        }
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_NAME)) {

            statement.setString(1, treatmentName.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read treatment by name", e);
        }
        return Optional.empty();
    }

    @Override
    public Treatment insert(Treatment treatment) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            bindTreatment(statement, treatment);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    treatment.setTreatmentId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not add treatment", e);
        }
        return treatment;
    }

    @Override
    public boolean update(Treatment treatment) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {

            bindTreatment(statement, treatment);
            statement.setInt(6, treatment.getTreatmentId());
            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not update treatment " + treatment.getTreatmentId(), e);
            return false;
        }
    }

    @Override
    public boolean setActive(int treatmentId, boolean active) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SET_ACTIVE)) {

            statement.setBoolean(1, active);
            statement.setInt(2, treatmentId);
            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not change the treatment status", e);
            return false;
        }
    }

    private List<Treatment> query(String sql) {
        List<Treatment> treatments = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                treatments.add(mapRow(resultSet));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read the treatment list", e);
        }
        return treatments;
    }

    private void bindTreatment(PreparedStatement statement, Treatment treatment)
            throws SQLException {
        statement.setString(1, treatment.getTreatmentName());
        statement.setString(2, treatment.getDescription());
        statement.setBigDecimal(3, treatment.getBaseCost());
        statement.setInt(4, treatment.getEstimatedMinutes());
        statement.setBoolean(5, treatment.isActive());
    }

    private Treatment mapRow(ResultSet resultSet) throws SQLException {
        Treatment treatment = new Treatment();
        treatment.setTreatmentId(resultSet.getInt("treatment_id"));
        treatment.setTreatmentName(resultSet.getString("treatment_name"));
        treatment.setDescription(resultSet.getString("description"));
        treatment.setBaseCost(resultSet.getBigDecimal("base_cost"));
        treatment.setEstimatedMinutes(resultSet.getInt("estimated_minutes"));
        treatment.setActive(resultSet.getBoolean("is_active"));
        return treatment;
    }
}
