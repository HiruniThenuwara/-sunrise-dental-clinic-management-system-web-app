package com.sunrise.dao;

import com.sunrise.model.Appointment;
import com.sunrise.model.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Data access contract for the {@code appointments} table
 * (Requirements 2 and 3).
 */
public interface AppointmentDao {

    /**
     * Stores a new appointment together with its patient, inside one
     * transaction. Either both rows are written or neither is.
     *
     * @param appointment the appointment to store
     * @return the same object with its generated id and number filled in
     */
    Appointment insert(Appointment appointment);

    /**
     * The lookup described in the scenario: find a visit by the unique
     * number printed on the patient's card.
     *
     * @param appointmentNo for example {@code APT-20260901-001}
     * @return the appointment with its patient, dentist and treatment loaded
     */
    Optional<Appointment> findByNumber(String appointmentNo);

    /**
     * @param appointmentId the primary key
     * @return the appointment, or {@link Optional#empty()} if not found
     */
    Optional<Appointment> findById(int appointmentId);

    /**
     * @return the most recent appointments, newest first
     */
    List<Appointment> findRecent(int limit);

    /**
     * @param date the day to list
     * @return every appointment on that date, ordered by time
     */
    List<Appointment> findByDate(LocalDate date);

    /**
     * The visit history of one patient, which is what the patient record
     * screen shows.
     *
     * @param patientId the patient
     * @return their appointments, most recent first
     */
    List<Appointment> findByPatient(int patientId);

    /**
     * Used by the slot calculation to mark taken times as unavailable.
     *
     * @return the times a dentist is already booked on that date
     */
    List<LocalTime> findBookedTimes(int doctorId, LocalDate date);

    /**
     * The double booking check. This runs before the insert so the staff
     * member gets a clear message rather than a database error.
     *
     * @return {@code true} when that dentist already has an appointment then
     */
    boolean existsAtSlot(int doctorId, LocalDate date, LocalTime time);

    /**
     * @param date the day being booked
     * @return how many appointments already exist on that date, used to build
     *         the next appointment number
     */
    int countByDate(LocalDate date);

    /**
     * @return {@code true} if one row was changed
     */
    boolean updateStatus(int appointmentId, AppointmentStatus status);

    /**
     * One page of appointments, newest first.
     *
     * <p>The database returns only the rows the screen shows. Reading the
     * whole table to display ten of it would grow slower every week the
     * clinic stays open.</p>
     *
     * @param offset how many rows to skip
     * @param limit  how many rows to return
     */
    List<Appointment> findPage(int offset, int limit);

    /** @return how many appointments exist, for the page count */
    int countAll();

    /**
     * @return how many appointments hold that status, for the badges above
     *         the list. Counted in the database, because the screen now
     *         holds one page rather than every row.
     */
    int countByStatus(AppointmentStatus status);
}
