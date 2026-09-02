package com.sunrise.dao.impl;

import com.sunrise.dao.DBConnection;
import com.sunrise.dao.DoctorScheduleDao;
import com.sunrise.model.DoctorSchedule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of {@link DoctorScheduleDao}.
 *
 * <p>The {@code day_of_week} column stores the weekday name in upper case,
 * which matches {@link DayOfWeek#name()} exactly, so no lookup table is
 * needed to convert between the two.</p>
 */
public class DoctorScheduleDaoImpl implements DoctorScheduleDao {

    private static final Logger LOGGER = Logger.getLogger(DoctorScheduleDaoImpl.class.getName());

    private static final String COLUMNS =
            "schedule_id, doctor_id, day_of_week, start_time, end_time, "
            + "slot_duration_minutes, is_active";

    private static final String SELECT_BY_DOCTOR =
            "SELECT " + COLUMNS + " FROM doctor_schedule WHERE doctor_id = ? "
            + "ORDER BY FIELD(day_of_week,'MONDAY','TUESDAY','WEDNESDAY','THURSDAY',"
            + "'FRIDAY','SATURDAY','SUNDAY')";

    private static final String SELECT_BY_DOCTOR_AND_DAY =
            "SELECT " + COLUMNS + " FROM doctor_schedule "
            + "WHERE doctor_id = ? AND day_of_week = ?";

    /** Inserts, or overwrites the row when that weekday already exists. */
    private static final String UPSERT =
            "INSERT INTO doctor_schedule (doctor_id, day_of_week, start_time, end_time, "
            + "slot_duration_minutes, is_active) VALUES (?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE start_time = VALUES(start_time), "
            + "end_time = VALUES(end_time), "
            + "slot_duration_minutes = VALUES(slot_duration_minutes), "
            + "is_active = VALUES(is_active)";

    private static final String DELETE =
            "DELETE FROM doctor_schedule WHERE doctor_id = ? AND day_of_week = ?";

    private final DBConnection dbConnection;

    public DoctorScheduleDaoImpl() {
        this(DBConnection.getInstance());
    }

    public DoctorScheduleDaoImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public List<DoctorSchedule> findByDoctor(int doctorId) {
        List<DoctorSchedule> schedules = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_DOCTOR)) {

            statement.setInt(1, doctorId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    schedules.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read the working hours of dentist " + doctorId, e);
        }
        return schedules;
    }

    @Override
    public Optional<DoctorSchedule> findByDoctorAndDay(int doctorId, DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            return Optional.empty();
        }
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(SELECT_BY_DOCTOR_AND_DAY)) {

            statement.setInt(1, doctorId);
            statement.setString(2, dayOfWeek.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read the working hours for " + dayOfWeek, e);
        }
        return Optional.empty();
    }

    @Override
    public boolean save(DoctorSchedule schedule) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPSERT)) {

            statement.setInt(1, schedule.getDoctorId());
            statement.setString(2, schedule.getDayOfWeek().name());
            statement.setTime(3, Time.valueOf(schedule.getStartTime()));
            statement.setTime(4, Time.valueOf(schedule.getEndTime()));
            statement.setInt(5, schedule.getSlotDurationMinutes());
            statement.setBoolean(6, schedule.isActive());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not save the working hours", e);
            return false;
        }
    }

    @Override
    public boolean delete(int doctorId, DayOfWeek dayOfWeek) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE)) {

            statement.setInt(1, doctorId);
            statement.setString(2, dayOfWeek.name());
            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not remove the working day", e);
            return false;
        }
    }

    private DoctorSchedule mapRow(ResultSet resultSet) throws SQLException {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setScheduleId(resultSet.getInt("schedule_id"));
        schedule.setDoctorId(resultSet.getInt("doctor_id"));
        schedule.setDayOfWeek(DayOfWeek.valueOf(resultSet.getString("day_of_week")));
        schedule.setStartTime(resultSet.getTime("start_time").toLocalTime());
        schedule.setEndTime(resultSet.getTime("end_time").toLocalTime());
        schedule.setSlotDurationMinutes(resultSet.getInt("slot_duration_minutes"));
        schedule.setActive(resultSet.getBoolean("is_active"));
        return schedule;
    }
}
