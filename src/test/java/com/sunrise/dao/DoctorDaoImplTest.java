package com.sunrise.dao;

import com.sunrise.dao.impl.DoctorDaoImpl;
import com.sunrise.model.Doctor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link DoctorDaoImpl}.
 *
 * <p>Unlike the service tests, these run the real SQL against a real database
 * engine. They are what proves the statements are valid, the columns are
 * spelled correctly and the {@code ResultSet} is mapped back into an object
 * properly - none of which a mocked DAO can tell you.</p>
 *
 * <p>Each test starts from an empty database, so the tests can run in any
 * order and none depends on data left behind by another.</p>
 */
@DisplayName("DoctorDaoImpl - dentist storage")
class DoctorDaoImplTest {

    private TestDatabase database;
    private DoctorDao doctorDao;

    @BeforeEach
    void setUp() {
        database = new TestDatabase();
        doctorDao = new DoctorDaoImpl(database.connectionSource());
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    private Doctor newDoctor(String name, String fee, boolean active) {
        Doctor doctor = new Doctor();
        doctor.setDoctorName(name);
        doctor.setSpecialization("General Dentistry");
        doctor.setContactNumber("0771234567");
        doctor.setEmail("test@sunrisedental.lk");
        doctor.setConsultationFee(new BigDecimal(fee));
        doctor.setActive(active);
        return doctor;
    }

    // -----------------------------------------------------------------
    //  Create
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-01 a new dentist is stored and given an id")
    void insertsAndReturnsGeneratedId() {
        Doctor saved = doctorDao.insert(newDoctor("Dr. Anura Jayasinghe", "1500", true));

        assertAll(
                () -> assertTrue(saved.getDoctorId() > 0, "The database must supply the id"),
                () -> assertEquals(1, database.count("SELECT COUNT(*) FROM doctors"))
        );
    }

    @Test
    @DisplayName("TC-02 the stored dentist reads back exactly as it was saved")
    void readsBackEveryField() {
        Doctor saved = doctorDao.insert(newDoctor("Dr. Kasun Silva", "3000.50", true));

        Optional<Doctor> found = doctorDao.findById(saved.getDoctorId());

        assertTrue(found.isPresent());
        Doctor doctor = found.get();
        assertAll(
                () -> assertEquals("Dr. Kasun Silva", doctor.getDoctorName()),
                () -> assertEquals("General Dentistry", doctor.getSpecialization()),
                () -> assertEquals("0771234567", doctor.getContactNumber()),
                () -> assertEquals(0, doctor.getConsultationFee().compareTo(new BigDecimal("3000.50")),
                        "Money must survive the round trip without losing its decimals"),
                () -> assertTrue(doctor.isActive())
        );
    }

    // -----------------------------------------------------------------
    //  Read
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-03 an unknown id returns empty rather than throwing")
    void returnsEmptyForAnUnknownId() {
        assertFalse(doctorDao.findById(9999).isPresent());
    }

    @Test
    @DisplayName("TC-04 findAllActive leaves out the withdrawn dentists")
    void listsOnlyActiveDentists() {
        doctorDao.insert(newDoctor("Dr. Active One", "1500", true));
        doctorDao.insert(newDoctor("Dr. Active Two", "2000", true));
        doctorDao.insert(newDoctor("Dr. Retired", "1000", false));

        assertAll(
                () -> assertEquals(3, doctorDao.findAll().size()),
                () -> assertEquals(2, doctorDao.findAllActive().size()),
                () -> assertEquals(2, doctorDao.countActive())
        );
    }

    @Test
    @DisplayName("TC-05 an empty table returns an empty list, never null")
    void returnsAnEmptyListWhenThereAreNoDentists() {
        List<Doctor> doctors = doctorDao.findAll();

        assertAll(
                () -> assertTrue(doctors.isEmpty()),
                () -> assertEquals(0, doctorDao.countActive())
        );
    }

    // -----------------------------------------------------------------
    //  Update
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-06 an edited dentist keeps the change")
    void updatesAnExistingDentist() {
        Doctor saved = doctorDao.insert(newDoctor("Dr. Anura Jayasinghe", "1500", true));

        saved.setConsultationFee(new BigDecimal("2200"));
        saved.setSpecialization("Orthodontics");
        boolean updated = doctorDao.update(saved);

        Doctor reloaded = doctorDao.findById(saved.getDoctorId()).orElseThrow();
        assertAll(
                () -> assertTrue(updated),
                () -> assertEquals(0, reloaded.getConsultationFee().compareTo(new BigDecimal("2200"))),
                () -> assertEquals("Orthodontics", reloaded.getSpecialization())
        );
    }

    @Test
    @DisplayName("TC-07 updating a dentist that does not exist changes nothing")
    void updatingAMissingDentistReportsFailure() {
        Doctor ghost = newDoctor("Dr. Nobody", "1000", true);
        ghost.setDoctorId(4242);

        assertFalse(doctorDao.update(ghost));
    }

    // -----------------------------------------------------------------
    //  Withdraw, which replaces delete
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-08 deactivating hides the dentist without deleting the row")
    void deactivateKeepsTheRow() {
        Doctor saved = doctorDao.insert(newDoctor("Dr. Leaving Soon", "1500", true));

        boolean changed = doctorDao.setActive(saved.getDoctorId(), false);

        assertAll(
                () -> assertTrue(changed),
                () -> assertEquals(1, database.count("SELECT COUNT(*) FROM doctors"),
                        "The row must still exist, because past bills refer to it"),
                () -> assertFalse(doctorDao.findById(saved.getDoctorId()).orElseThrow().isActive()),
                () -> assertEquals(0, doctorDao.countActive())
        );
    }

    @Test
    @DisplayName("TC-09 a withdrawn dentist can be brought back")
    void reactivateWorks() {
        Doctor saved = doctorDao.insert(newDoctor("Dr. Back Again", "1500", false));

        doctorDao.setActive(saved.getDoctorId(), true);

        assertTrue(doctorDao.findById(saved.getDoctorId()).orElseThrow().isActive());
    }

    // -----------------------------------------------------------------
    //  Safety
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-10 an injection string is stored as text, not executed")
    void treatsInjectionAttemptsAsOrdinaryText() {
        String attack = "Robert'); DROP TABLE doctors; --";

        Doctor saved = doctorDao.insert(newDoctor(attack, "1500", true));
        Doctor reloaded = doctorDao.findById(saved.getDoctorId()).orElseThrow();

        assertAll(
                () -> assertEquals(attack, reloaded.getDoctorName(),
                        "The value must come back exactly as it went in"),
                () -> assertEquals(1, database.count("SELECT COUNT(*) FROM doctors"),
                        "The table must still be there")
        );
    }

    // -----------------------------------------------------------------
    //  Paging
    // -----------------------------------------------------------------

    @Test
    @DisplayName("TC-11 dentists are read one page at a time")
    void readsOnePageOfDentists() {
        for (int i = 0; i < 14; i++) {
            Doctor doctor = new Doctor();
            doctor.setDoctorName("Dr. Number " + (i + 1));
            doctor.setSpecialization("General Dentistry");
            doctor.setConsultationFee(new java.math.BigDecimal("1500.00"));
            doctor.setActive(true);
            doctorDao.insert(doctor);
        }

        assertAll(
                () -> assertEquals(14, doctorDao.countAll()),
                () -> assertEquals(10, doctorDao.findPage(0, 10).size()),
                () -> assertEquals(4, doctorDao.findPage(10, 10).size()),
                () -> assertTrue(doctorDao.findPage(20, 10).isEmpty())
        );
    }
}
