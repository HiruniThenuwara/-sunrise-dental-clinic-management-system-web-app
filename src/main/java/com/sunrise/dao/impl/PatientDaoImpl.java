package com.sunrise.dao.impl;

import com.sunrise.dao.DBConnection;
import com.sunrise.dao.PatientDao;
import com.sunrise.model.Gender;
import com.sunrise.model.Patient;
import com.sunrise.model.PatientSummary;

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

    /**
     * One query returns every patient with their history. The counts come
     * from the database rather than from loading each patient's
     * appointments in turn, which would be one query per row.
     *
     * <p>{@code LEFT JOIN} is used so that a patient with no appointments
     * still appears, with zero visits, rather than vanishing from the list.</p>
     */
    private static final String WITH_HISTORY =
            "SELECT p.patient_id, p.patient_name, p.address, p.contact_number, p.email, "
            + "p.nic, p.date_of_birth, p.gender, "
            + "COUNT(a.appointment_id) AS visits, "
            + "SUM(a.status = 'COMPLETED') AS completed, "
            + "SUM(a.status = 'CANCELLED') AS cancelled, "
            + "MAX(CASE WHEN a.appointment_date <= CURRENT_DATE THEN a.appointment_date END) AS last_visit, "
            + "MIN(CASE WHEN a.appointment_date > CURRENT_DATE AND a.status = 'BOOKED' "
            + "         THEN a.appointment_date END) AS next_visit, "
            + "COALESCE(SUM(b.total_amount), 0) AS total_billed "
            + "FROM patients p "
            + "LEFT JOIN appointments a ON a.patient_id = p.patient_id "
            + "LEFT JOIN bills b ON b.appointment_id = a.appointment_id ";

    private static final String GROUP_AND_ORDER =
            "GROUP BY p.patient_id, p.patient_name, p.address, p.contact_number, p.email, "
            + "p.nic, p.date_of_birth, p.gender "
            + "ORDER BY COALESCE(MAX(a.appointment_date), '1900-01-01') DESC, p.patient_name";

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

    @Override
    public List<PatientSummary> findAllWithHistory(String search) {
        List<PatientSummary> summaries = new ArrayList<>();

        boolean filtered = search != null && !search.isBlank();
        String sql = WITH_HISTORY
                + (filtered ? "WHERE p.patient_name LIKE ? OR p.contact_number LIKE ? "
                            + "OR p.nic LIKE ? " : "")
                + GROUP_AND_ORDER;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (filtered) {
                String term = "%" + search.trim() + "%";
                statement.setString(1, term);
                statement.setString(2, term);
                statement.setString(3, term);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    summaries.add(mapSummary(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read the patient list", e);
        }
        return summaries;
    }

    @Override
    public Optional<PatientSummary> findSummaryById(int patientId) {
        String sql = WITH_HISTORY + "WHERE p.patient_id = ? " + GROUP_AND_ORDER;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, patientId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapSummary(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read the patient summary", e);
        }
        return Optional.empty();
    }

    /** Builds a patient and their history figures from one joined row. */
    private PatientSummary mapSummary(ResultSet resultSet) throws SQLException {
        Patient patient = mapRow(resultSet);

        Date lastVisit = resultSet.getDate("last_visit");
        Date nextVisit = resultSet.getDate("next_visit");

        return new PatientSummary(
                patient,
                resultSet.getInt("visits"),
                resultSet.getInt("completed"),
                resultSet.getInt("cancelled"),
                lastVisit == null ? null : lastVisit.toLocalDate(),
                nextVisit == null ? null : nextVisit.toLocalDate(),
                resultSet.getBigDecimal("total_billed"));
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
