package com.sunrise.dao;

import com.sunrise.model.Treatment;

import java.util.List;
import java.util.Optional;

/**
 * Data access contract for the {@code treatments} table.
 *
 * <p>As with dentists, treatments are deactivated rather than deleted, so
 * that old appointments and bills keep showing what was really done and
 * charged.</p>
 */
public interface TreatmentDao {

    List<Treatment> findAll();

    List<Treatment> findAllActive();

    Optional<Treatment> findById(int treatmentId);

    Optional<Treatment> findByName(String treatmentName);

    Treatment insert(Treatment treatment);

    boolean update(Treatment treatment);

    boolean setActive(int treatmentId, boolean active);
}
