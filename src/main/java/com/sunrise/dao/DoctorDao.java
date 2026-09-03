package com.sunrise.dao;

import com.sunrise.model.Doctor;

import java.util.List;
import java.util.Optional;

/**
 * Data access contract for the {@code doctors} table.
 *
 * <p>Note that there is no {@code delete} method. A dentist who leaves is
 * deactivated instead, because their name still appears on past appointments
 * and bills. Deleting the row would either destroy that history or leave
 * broken references behind it.</p>
 */
public interface DoctorDao {

    /**
     * @return every dentist, active and inactive, ordered by name
     */
    List<Doctor> findAll();

    /**
     * @return only dentists who can currently accept bookings
     */
    List<Doctor> findAllActive();

    /**
     * @param doctorId the primary key
     * @return the dentist, or {@link Optional#empty()} if not found
     */
    Optional<Doctor> findById(int doctorId);

    /**
     * Inserts a new dentist.
     *
     * @param doctor the dentist to store, without an id
     * @return the same object with its generated id filled in
     */
    Doctor insert(Doctor doctor);

    /**
     * Updates an existing dentist.
     *
     * @param doctor the dentist to update, with an id
     * @return {@code true} if one row was changed
     */
    boolean update(Doctor doctor);

    /**
     * Turns a dentist on or off for new bookings, leaving their history
     * untouched.
     *
     * @param doctorId the dentist to change
     * @param active   {@code true} to allow bookings again
     * @return {@code true} if one row was changed
     */
    boolean setActive(int doctorId, boolean active);

    /**
     * @return how many dentists are currently active
     */
    int countActive();

    /** One page of dentists, active ones first. */
    List<Doctor> findPage(int offset, int limit);

    /** @return how many dentists exist, for the page count */
    int countAll();

    /** @return the highest consultation fee on file, for the statistic card */
    java.math.BigDecimal highestFee();
}
