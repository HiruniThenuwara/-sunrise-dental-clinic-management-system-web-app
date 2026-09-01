package com.sunrise.dao;

import com.sunrise.model.Patient;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Data access contract for the {@code patients} table.
 */
public interface PatientDao {

    /**
     * @param patientId the primary key
     * @return the patient, or {@link Optional#empty()} if not found
     */
    Optional<Patient> findById(int patientId);

    /**
     * Used when registering an appointment, so a returning patient is not
     * stored twice.
     *
     * @param contactNumber the telephone number typed on the form
     * @return the existing patient, or {@link Optional#empty()} for a new one
     */
    Optional<Patient> findByContactNumber(String contactNumber);

    /**
     * @param nameOrContact part of a name or a contact number
     * @return matching patients, ordered by name
     */
    List<Patient> search(String nameOrContact);

    /**
     * Inserts a new patient.
     *
     * @param patient the patient to store, without an id
     * @return the same object with its generated id filled in
     */
    Patient insert(Patient patient);

    /**
     * Inserts a patient using a caller supplied connection, so the patient
     * and the appointment can be written inside <b>one transaction</b>. If
     * the appointment insert then fails, the patient row is rolled back with
     * it and no orphan record is left behind.
     *
     * @param patient    the patient to store
     * @param connection the open transactional connection
     * @return the same object with its generated id filled in
     * @throws SQLException so the caller can roll back
     */
    Patient insert(Patient patient, Connection connection) throws SQLException;

    /**
     * @param patient the patient to update, with an id
     * @return {@code true} if one row was changed
     */
    boolean update(Patient patient);

    /**
     * @return how many patients are registered
     */
    int countAll();
}
