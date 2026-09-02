package com.sunrise.dao.impl;

import com.sunrise.dao.DBConnection;
import com.sunrise.dao.PatientDao;
import com.sunrise.model.Gender;
import com.sunrise.model.Patient;

import java.sql.Connection;
import java.sql.Date;
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
 * JDBC implementation of {@link PatientDao}.
 */
public class PatientDaoImpl implements PatientDao {

    private static final Logger LOGGER = Logger.getLogger(PatientDaoImpl.class.getName());

    private static final String COLUMNS =
            "patient_id, patient_name, address, contact_number, email, nic, date_of_birth, gender";

    private static final String SELECT_BY_ID =
            "SELECT " + COLUMNS + " FROM patients WHERE patient_id = ?";

    private static final String SELECT_BY_CONTACT =
            "SELECT " + COLUMNS + " FROM patients WHERE contact_number = ? LIMIT 1";

    private static final String SEARCH =
            "SELECT " + COLUMNS + " FROM patients "
            + "WHERE patient_name LIKE ? OR contact_number LIKE ? ORDER BY patient_name LIMIT 50";

    private static final String INSERT =
            "INSERT INTO patients (patient_name, address, contact_number, email, nic, "
            + "date_of_birth, gender) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE =
            "UPDATE patients SET patient_name = ?, address = ?, contact_number = ?, "
            + "email = ?, nic = ?, date_of_birth = ?, gender = ? WHERE patient_id = ?";

    private static final String COUNT_ALL = "SELECT COUNT(*) FROM patients";

    private final DBConnection dbConnection;

    public PatientDaoImpl() {
        this(DBConnection.getInstance());
    }

    public PatientDaoImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public Optional<Patient> findById(int patientId) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {

            statement.setInt(1, patientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read patient " + patientId, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Patient> findByContactNumber(String contactNumber) {
        if (contactNumber == null || contactNumber.isBlank()) {
            return Optional.empty();
        }
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_CONTACT)) {

            statement.setString(1, contactNumber.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not look up a patient by contact number", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Patient> search(String nameOrContact) {
        List<Patient> patients = new ArrayList<>();
        String term = "%" + (nameOrContact == null ? "" : nameOrContact.trim()) + "%";

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SEARCH)) {

            statement.setString(1, term);
            statement.setString(2, term);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    patients.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not search patients", e);
        }
        return patients;
    }

    @Override
    public Patient insert(Patient patient) {
        try (Connection connection = dbConnection.getConnection()) {
            return insert(patient, connection);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not add patient", e);
            return patient;
        }
    }

    @Override
    public Patient insert(Patient patient, Connection connection) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            bindPatient(statement, patient);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    patient.setPatientId(keys.getInt(1));
                }
            }
        }
        return patient;
    }

    @Override
    public boolean update(Patient patient) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {

            bindPatient(statement, patient);
            statement.setInt(8, patient.getPatientId());
            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not update patient " + patient.getPatientId(), e);
            return false;
        }
    }

    @Override
    public int countAll() {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT_ALL);
             ResultSet resultSet = statement.executeQuery()) {

            return resultSet.next() ? resultSet.getInt(1) : 0;

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not count patients", e);
            return 0;
        }
    }

    private void bindPatient(PreparedStatement statement, Patient patient) throws SQLException {
        statement.setString(1, patient.getPatientName());
        statement.setString(2, patient.getAddress());
        statement.setString(3, patient.getContactNumber());
        statement.setString(4, patient.getEmail());
        statement.setString(5, patient.getNic());
        statement.setDate(6, patient.getDateOfBirth() == null
                ? null : Date.valueOf(patient.getDateOfBirth()));
        statement.setString(7, patient.getGender() == null
                ? null : patient.getGender().name());
    }

    /** Converts the current row into a {@link Patient}. */
    static Patient mapRow(ResultSet resultSet) throws SQLException {
        Patient patient = new Patient();
        patient.setPatientId(resultSet.getInt("patient_id"));
        patient.setPatientName(resultSet.getString("patient_name"));
        patient.setAddress(resultSet.getString("address"));
        patient.setContactNumber(resultSet.getString("contact_number"));
        patient.setEmail(resultSet.getString("email"));
        patient.setNic(resultSet.getString("nic"));

        Date dateOfBirth = resultSet.getDate("date_of_birth");
        if (dateOfBirth != null) {
            patient.setDateOfBirth(dateOfBirth.toLocalDate());
        }
        patient.setGender(Gender.fromString(resultSet.getString("gender")));
        return patient;
    }
}
