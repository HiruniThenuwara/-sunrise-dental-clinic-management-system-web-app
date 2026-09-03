package com.sunrise.dao.impl;

import com.sunrise.dao.DBConnection;
import com.sunrise.dao.DoctorDao;
import com.sunrise.model.Doctor;

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
 * JDBC implementation of {@link DoctorDao}.
 *
 * <p>Every statement is a {@link PreparedStatement} with {@code ?}
 * placeholders. Values are never concatenated into the SQL text, so a name
 * such as {@code Robert'); DROP TABLE doctors; --} is stored as an ordinary
 * string instead of being executed.</p>
 */
public class DoctorDaoImpl implements DoctorDao {

    private static final Logger LOGGER = Logger.getLogger(DoctorDaoImpl.class.getName());

    private static final String COLUMNS =
            "doctor_id, doctor_name, specialization, contact_number, email, "
            + "consultation_fee, is_active";

    private static final String SELECT_ALL =
            "SELECT " + COLUMNS + " FROM doctors ORDER BY is_active DESC, doctor_name";

    private static final String SELECT_ACTIVE =
            "SELECT " + COLUMNS + " FROM doctors WHERE is_active = 1 ORDER BY doctor_name";

    private static final String SELECT_BY_ID =
            "SELECT " + COLUMNS + " FROM doctors WHERE doctor_id = ?";

    private static final String INSERT =
            "INSERT INTO doctors (doctor_name, specialization, contact_number, email, "
            + "consultation_fee, is_active) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE =
            "UPDATE doctors SET doctor_name = ?, specialization = ?, contact_number = ?, "
            + "email = ?, consultation_fee = ?, is_active = ? WHERE doctor_id = ?";

    private static final String SET_ACTIVE =
            "UPDATE doctors SET is_active = ? WHERE doctor_id = ?";

    private static final String COUNT_ACTIVE =
            "SELECT COUNT(*) FROM doctors WHERE is_active = 1";

    private final DBConnection dbConnection;

    public DoctorDaoImpl() {
        this(DBConnection.getInstance());
    }

    /**
     * @param dbConnection the connection source, so tests can supply their own
     */
    public DoctorDaoImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public List<Doctor> findAll() {
        return query(SELECT_ALL);
    }

    @Override
    public List<Doctor> findAllActive() {
        return query(SELECT_ACTIVE);
    }

    @Override
    public Optional<Doctor> findById(int doctorId) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {

            statement.setInt(1, doctorId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read dentist " + doctorId, e);
        }
        return Optional.empty();
    }

    @Override
    public Doctor insert(Doctor doctor) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            bindDoctor(statement, doctor);

            if (statement.executeUpdate() == 0) {
                return doctor;
            }

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    doctor.setDoctorId(keys.getInt(1));
                }
            }
            LOGGER.info("Dentist added: " + doctor.getDoctorName());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not add dentist", e);
        }
        return doctor;
    }

    @Override
    public boolean update(Doctor doctor) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {

            bindDoctor(statement, doctor);
            statement.setInt(7, doctor.getDoctorId());

            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not update dentist " + doctor.getDoctorId(), e);
            return false;
        }
    }

    @Override
    public boolean setActive(int doctorId, boolean active) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SET_ACTIVE)) {

            statement.setBoolean(1, active);
            statement.setInt(2, doctorId);

            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not change the status of dentist " + doctorId, e);
            return false;
        }
    }

    @Override
    public int countActive() {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT_ACTIVE);
             ResultSet resultSet = statement.executeQuery()) {

            return resultSet.next() ? resultSet.getInt(1) : 0;

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not count active dentists", e);
            return 0;
        }
    }

    /** Runs a query that returns a list of dentists. */
    private List<Doctor> query(String sql) {
        List<Doctor> doctors = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                doctors.add(mapRow(resultSet));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read the dentist list", e);
        }
        return doctors;
    }

    /** Fills the first six parameters, shared by the insert and the update. */
    private void bindDoctor(PreparedStatement statement, Doctor doctor) throws SQLException {
        statement.setString(1, doctor.getDoctorName());
        statement.setString(2, doctor.getSpecialization());
        statement.setString(3, doctor.getContactNumber());
        statement.setString(4, doctor.getEmail());
        statement.setBigDecimal(5, doctor.getConsultationFee());
        statement.setBoolean(6, doctor.isActive());
    }

    /** Converts the current row into a {@link Doctor}. */
    private Doctor mapRow(ResultSet resultSet) throws SQLException {
        Doctor doctor = new Doctor();
        doctor.setDoctorId(resultSet.getInt("doctor_id"));
        doctor.setDoctorName(resultSet.getString("doctor_name"));
        doctor.setSpecialization(resultSet.getString("specialization"));
        doctor.setContactNumber(resultSet.getString("contact_number"));
        doctor.setEmail(resultSet.getString("email"));
        doctor.setConsultationFee(resultSet.getBigDecimal("consultation_fee"));
        doctor.setActive(resultSet.getBoolean("is_active"));
        return doctor;
    }

    @Override
    public List<Doctor> findPage(int offset, int limit) {
        return queryPage(SELECT_ALL + " LIMIT ? OFFSET ?", offset, limit);
    }

    @Override
    public int countAll() {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement("SELECT COUNT(*) FROM doctors");
             ResultSet resultSet = statement.executeQuery()) {

            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not count the dentists", e);
            return 0;
        }
    }

    private List<Doctor> queryPage(String sql, int offset, int limit) {
        List<Doctor> doctors = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, Math.max(limit, 1));
            statement.setInt(2, Math.max(offset, 0));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    doctors.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read a page of dentists", e);
        }
        return doctors;
    }

    @Override
    public java.math.BigDecimal highestFee() {
        return singleAmount("SELECT MAX(consultation_fee) FROM doctors",
                            "Could not read the highest consultation fee");
    }

    /** Runs a query that returns one money value, or zero when there is none. */
    private java.math.BigDecimal singleAmount(String sql, String failureMessage) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                java.math.BigDecimal value = resultSet.getBigDecimal(1);
                return value == null ? java.math.BigDecimal.ZERO : value;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, failureMessage, e);
        }
        return java.math.BigDecimal.ZERO;
    }
}
