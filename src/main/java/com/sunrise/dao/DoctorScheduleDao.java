package com.sunrise.dao;

import com.sunrise.model.DoctorSchedule;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

/**
 * Data access contract for the {@code doctor_schedule} table.
 */
public interface DoctorScheduleDao {

    /**
     * @param doctorId the dentist
     * @return every working day recorded for that dentist
     */
    List<DoctorSchedule> findByDoctor(int doctorId);

    /**
     * The lookup the slot calculation needs: what hours does this dentist
     * work on the weekday of the chosen date?
     *
     * @return the schedule, or {@link Optional#empty()} when the dentist does
     *         not work that day
     */
    Optional<DoctorSchedule> findByDoctorAndDay(int doctorId, DayOfWeek dayOfWeek);

    /**
     * Adds or replaces the hours for one weekday.
     *
     * @return {@code true} if the row was written
     */
    boolean save(DoctorSchedule schedule);

    /**
     * Removes a working day, which makes the dentist unavailable then.
     *
     * @return {@code true} if one row was removed
     */
    boolean delete(int doctorId, DayOfWeek dayOfWeek);
}
