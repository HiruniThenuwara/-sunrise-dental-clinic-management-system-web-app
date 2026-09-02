package com.sunrise.dao.impl;

import com.sunrise.dao.DBConnection;
import com.sunrise.dao.AppointmentDao;
import com.sunrise.model.Appointment;
import com.sunrise.model.AppointmentStatus;
import com.sunrise.model.Doctor;
import com.sunrise.model.Patient;
import com.sunrise.model.Treatment;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of {@link AppointmentDao}.
 *
 * <p>The insert runs inside a <b>transaction</b>. A new patient and their
 * appointment are two separate rows, and both must succeed together. Without
 * the transaction, a failure on the second insert would leave a patient in
 * the database with no visit attached.</p>
 */
public class AppointmentDaoImpl implements AppointmentDao {

    private static final Logger LOGGER = Logger.getLogger(AppointmentDaoImpl.class.getName());

    /** Joins everything the details screen needs in one query. */
    private static final String SELECT_FULL =
            "SELECT a.appointment_id, a.appointment_no, a.appointment_date, a.appointment_time, "
            + "a.status, a.notes, a.created_at, "
            + "p.patient_id, p.patient_name, p.address, p.contact_number, p.email, p.nic, "
            + "p.date_of_birth, p.gender, "
            + "d.doctor_id, d.doctor_name, d.specialization, d.consultation_fee, "
            + "t.treatment_id, t.treatment_name, t.description, t.base_cost, t.estimated_minutes "
            + "FROM appointments a "
            + "JOIN patients p ON p.patient_id = a.patient_id "
            + "JOIN doctors d ON d.doctor_id = a.doctor_id "
            + "JOIN treatments t ON t.treatment_id = a.treatment_id ";

    private static final String INSERT =
            "INSERT INTO appointments (appointment_no, patient_id, doctor_id, treatment_id, "
            + "appointment_date, appointment_time, status, notes, created_by) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String EXISTS_AT_SLOT =
            "SELECT 1 FROM appointments WHERE doctor_id = ? AND appointment_date = ? "
            + "AND appointment_time = ? AND status <> 'CANCELLED' LIMIT 1";

    private static final String BOOKED_TIMES =
            "SELECT appointment_time FROM appointments WHERE doctor_id = ? "
            + "AND appointment_date = ? AND status <> 'CANCELLED'";

    /**
     * Counts the numbers already issued for a date by matching the number
     * itself, so the counter and the appointment number can never drift
     * apart. See the note on the same query in {@code BillDaoImpl}.
     */
    private static final String COUNT_BY_DATE =
            "SELECT COUNT(*) FROM appointments WHERE appointment_no LIKE ?";

    private static final String UPDATE_STATUS =
            "UPDATE appointments SET status = ? WHERE appointment_id = ?";

    private final DBConnection dbConnection;
    private final PatientDaoImpl patientDao;

    public AppointmentDaoImpl() {
        this(DBConnection.getInstance());
    }

    public AppointmentDaoImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
        this.patientDao = new PatientDaoImpl(dbConnection);
    }

    /**
     * Stores the appointment, and the patient too when they are new, inside
     * a single transaction.
     */
    @Override
    public Appointment insert(Appointment appointment) {

        Connection connection = null;
        try {
            connection = dbConnection.getConnection();
            connection.setAutoCommit(false);          // start the transaction

            Patient patient = appointment.getPatient();
            if (patient != null && patient.getPatientId() == 0) {
                patientDao.insert(patient, connection);
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

                statement.setString(1, appointment.getAppointmentNo());
                statement.setInt(2, patient == null ? 0 : patient.getPatientId());
                statement.setInt(3, appointment.getDoctor().getDoctorId());
                statement.setInt(4, appointment.getTreatment().getTreatmentId());
                statement.setDate(5, Date.valueOf(appointment.getAppointmentDate()));
                statement.setTime(6, Time.valueOf(appointment.getAppointmentTime()));
                statement.setString(7, appointment.getStatus().name());
                statement.setString(8, appointment.getNotes());

                if (appointment.getCreatedBy() == null) {
                    statement.setNull(9, java.sql.Types.INTEGER);
                } else {
                    statement.setInt(9, appointment.getCreatedBy().getUserId());
                }

                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        appointment.setAppointmentId(keys.getInt(1));
                    }
                }
            }

            connection.commit();                      // both rows or neither
            return appointment;

        } catch (SQLIntegrityConstraintViolationException e) {
            // The UNIQUE (doctor, date, time) constraint fired. Two staff
            // members submitted the same slot at the same moment.
            rollback(connection);
            LOGGER.log(Level.WARNING, "Double booking blocked by the database constraint", e);
            return null;

        } catch (SQLException e) {
            rollback(connection);
            LOGGER.log(Level.SEVERE, "Could not save the appointment", e);
            return null;

        } finally {
            closeQuietly(connection);
        }
    }

    @Override
    public Optional<Appointment> findByNumber(String appointmentNo) {
        return findOne(SELECT_FULL + "WHERE a.appointment_no = ?", appointmentNo);
    }

    @Override
    public Optional<Appointment> findById(int appointmentId) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(SELECT_FULL + "WHERE a.appointment_id = ?")) {

            statement.setInt(1, appointmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read appointment " + appointmentId, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Appointment> findRecent(int limit) {
        List<Appointment> appointments = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_FULL + "ORDER BY a.appointment_date DESC, a.appointment_time DESC LIMIT ?")) {

            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    appointments.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read the appointment list", e);
        }
        return appointments;
    }

    @Override
    public List<Appointment> findByDate(LocalDate date) {
        List<Appointment> appointments = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_FULL + "WHERE a.appointment_date = ? ORDER BY a.appointment_time")) {

            statement.setDate(1, Date.valueOf(date));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    appointments.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read appointments for " + date, e);
        }
        return appointments;
    }

    @Override
    public List<LocalTime> findBookedTimes(int doctorId, LocalDate date) {
        List<LocalTime> times = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(BOOKED_TIMES)) {

            statement.setInt(1, doctorId);
            statement.setDate(2, Date.valueOf(date));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    times.add(resultSet.getTime("appointment_time").toLocalTime());
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read booked times", e);
        }
        return times;
    }

    @Override
    public boolean existsAtSlot(int doctorId, LocalDate date, LocalTime time) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(EXISTS_AT_SLOT)) {

            statement.setInt(1, doctorId);
            statement.setDate(2, Date.valueOf(date));
            statement.setTime(3, Time.valueOf(time));

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            // On error the safe answer is "taken", so a booking is never made
            // on the strength of a failed check.
            LOGGER.log(Level.SEVERE, "Could not check the slot, refusing the booking", e);
            return true;
        }
    }

    @Override
    public int countByDate(LocalDate date) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT_BY_DATE)) {

            statement.setString(1, "APT-"
                    + date.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-%");

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not count appointments for " + date, e);
            return 0;
        }
    }

    @Override
    public boolean updateStatus(int appointmentId, AppointmentStatus status) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS)) {

            statement.setString(1, status.name());
            statement.setInt(2, appointmentId);
            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not change the appointment status", e);
            return false;
        }
    }

    /** Runs a single parameter query that returns at most one appointment. */
    private Optional<Appointment> findOne(String sql, String parameter) {
        if (parameter == null || parameter.isBlank()) {
            return Optional.empty();
        }
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, parameter.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read the appointment", e);
        }
        return Optional.empty();
    }

    /** Builds the whole object graph from one joined row. */
    private Appointment mapRow(ResultSet resultSet) throws SQLException {

        Appointment appointment = new Appointment();
        appointment.setAppointmentId(resultSet.getInt("appointment_id"));
        appointment.setAppointmentNo(resultSet.getString("appointment_no"));
        appointment.setAppointmentDate(resultSet.getDate("appointment_date").toLocalDate());
        appointment.setAppointmentTime(resultSet.getTime("appointment_time").toLocalTime());
        appointment.setStatus(AppointmentStatus.fromString(resultSet.getString("status")));
        appointment.setNotes(resultSet.getString("notes"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            appointment.setCreatedAt(createdAt.toLocalDateTime());
        }

        appointment.setPatient(PatientDaoImpl.mapRow(resultSet));

        Doctor doctor = new Doctor();
        doctor.setDoctorId(resultSet.getInt("doctor_id"));
        doctor.setDoctorName(resultSet.getString("doctor_name"));
        doctor.setSpecialization(resultSet.getString("specialization"));
        doctor.setConsultationFee(resultSet.getBigDecimal("consultation_fee"));
        appointment.setDoctor(doctor);

        Treatment treatment = new Treatment();
        treatment.setTreatmentId(resultSet.getInt("treatment_id"));
        treatment.setTreatmentName(resultSet.getString("treatment_name"));
        treatment.setDescription(resultSet.getString("description"));
        treatment.setBaseCost(resultSet.getBigDecimal("base_cost"));
        treatment.setEstimatedMinutes(resultSet.getInt("estimated_minutes"));
        appointment.setTreatment(treatment);

        return appointment;
    }

    private void rollback(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Rollback failed", e);
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.setAutoCommit(true);
            connection.close();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not close the connection", e);
        }
    }
}
