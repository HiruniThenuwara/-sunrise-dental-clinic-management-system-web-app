package com.sunrise.dao;

import com.sunrise.dao.impl.AppointmentDaoImpl;
import com.sunrise.model.Appointment;
import com.sunrise.model.AppointmentStatus;
import com.sunrise.model.Doctor;
import com.sunrise.model.Patient;
import com.sunrise.model.Treatment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link AppointmentDaoImpl}.
 *
 * <p>These are the tests that exercise the parts of the system a mock cannot
 * reach: the joins that load a patient, dentist and treatment in one query,
 * the transaction that writes a patient and an appointment together, and the
 * {@code UNIQUE} constraint that refuses a double booking at database
 * level.</p>
 */
@DisplayName("AppointmentDaoImpl - appointment storage")
class AppointmentDaoImplTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 14);

    private TestDatabase database;
    private AppointmentDao appointmentDao;

    @BeforeEach
    void setUp() {
        database = new TestDatabase();
        appointmentDao = new AppointmentDaoImpl(database.connectionSource());

        // The rows every appointment needs to exist.
        database.execute("INSERT INTO doctors (doctor_id, doctor_name, specialization, "
                + "consultation_fee, is_active) VALUES "
                + "(1, 'Dr. Anura Jayasinghe', 'General Dentistry', 1500.00, TRUE)");
        database.execute("INSERT INTO doctors (doctor_id, doctor_name, specialization, "
                + "consultation_fee, is_active) VALUES "
                + "(2, 'Dr. Kasun Silva', 'Oral Surgery', 3000.00, TRUE)");
        database.execute("INSERT INTO treatments (treatment_id, treatment_name, description, "
                + "base_cost, estimated_minutes) VALUES "
                + "(1, 'Scaling', 'Teeth cleaning', 4500.00, 45)");
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    /** Builds an appointment with a brand new patient attached. */
    private Appointment appointment(String number, int doctorId, LocalTime time, String patientName) {
        Patient patient = new Patient(patientName, "No 45, Galle Road, Colombo 03", "0712345678");

        Doctor doctor = new Doctor();
        doctor.setDoctorId(doctorId);

        Treatment treatment = new Treatment();
        treatment.setTreatmentId(1);

        Appointment appointment = new Appointment();
        appointment.setAppointmentNo(number);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setTreatment(treatment);
        appointment.setAppointmentDate(MONDAY);
        appointment.setAppointmentTime(time);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setNotes("Test booking");
        return appointment;
    }

    // -----------------------------------------------------------------
    //  The transaction
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-01 the patient and the appointment are written together")
    void writesPatientAndAppointmentInOneTransaction() {
        Appointment saved = appointmentDao.insert(
                appointment("APT-20260914-001", 1, LocalTime.of(9, 0), "Saman Kumara"));

        assertAll(
                () -> assertTrue(saved.getAppointmentId() > 0),
                () -> assertTrue(saved.getPatient().getPatientId() > 0,
                        "The new patient must have been stored too"),
                () -> assertEquals(1, database.count("SELECT COUNT(*) FROM patients")),
                () -> assertEquals(1, database.count("SELECT COUNT(*) FROM appointments"))
        );
    }

    @Test
    @DisplayName("TC-02 nothing is written when the appointment insert fails")
    void rollsBackThePatientWhenTheAppointmentFails() {
        appointmentDao.insert(appointment("APT-20260914-001", 1, LocalTime.of(9, 0), "First Patient"));

        // Same dentist, date and time, so the UNIQUE constraint refuses it.
        Appointment clash = appointment("APT-20260914-002", 1, LocalTime.of(9, 0), "Second Patient");
        Appointment result = appointmentDao.insert(clash);

        assertAll(
                () -> assertNull(result, "A refused booking must return nothing"),
                () -> assertEquals(1, database.count("SELECT COUNT(*) FROM appointments")),
                () -> assertEquals(1, database.count("SELECT COUNT(*) FROM patients"),
                        "The second patient must have been rolled back, not left orphaned")
        );
    }

    // -----------------------------------------------------------------
    //  The double booking constraint
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-03 the same dentist cannot be booked twice at one time")
    void refusesADoubleBooking() {
        appointmentDao.insert(appointment("APT-20260914-001", 1, LocalTime.of(10, 0), "Patient One"));

        Appointment refused = appointmentDao.insert(
                appointment("APT-20260914-002", 1, LocalTime.of(10, 0), "Patient Two"));

        assertNull(refused);
    }

    @Test
    @DisplayName("TC-04 a different dentist may use the same time")
    void allowsTheSameTimeForADifferentDentist() {
        appointmentDao.insert(appointment("APT-20260914-001", 1, LocalTime.of(10, 0), "Patient One"));

        Appointment second = appointmentDao.insert(
                appointment("APT-20260914-002", 2, LocalTime.of(10, 0), "Patient Two"));

        assertAll(
                () -> assertTrue(second != null && second.getAppointmentId() > 0),
                () -> assertEquals(2, database.count("SELECT COUNT(*) FROM appointments"))
        );
    }

    @Test
    @DisplayName("TC-05 the same appointment number cannot be used twice")
    void refusesADuplicateAppointmentNumber() {
        appointmentDao.insert(appointment("APT-20260914-001", 1, LocalTime.of(9, 0), "Patient One"));

        Appointment duplicate = appointmentDao.insert(
                appointment("APT-20260914-001", 1, LocalTime.of(11, 0), "Patient Two"));

        assertNull(duplicate);
    }

    // -----------------------------------------------------------------
    //  Reading it back, Requirement 3
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-06 searching by number loads the patient, dentist and treatment")
    void findByNumberLoadsTheWholeVisit() {
        appointmentDao.insert(appointment("APT-20260914-007", 1, LocalTime.of(9, 30), "Saman Kumara"));

        Optional<Appointment> found = appointmentDao.findByNumber("APT-20260914-007");

        assertTrue(found.isPresent());
        Appointment visit = found.get();
        assertAll(
                () -> assertEquals("Saman Kumara", visit.getPatient().getPatientName()),
                () -> assertEquals("Dr. Anura Jayasinghe", visit.getDoctor().getDoctorName()),
                () -> assertEquals("Scaling", visit.getTreatment().getTreatmentName()),
                () -> assertEquals(LocalTime.of(9, 30), visit.getAppointmentTime()),
                () -> assertEquals(AppointmentStatus.BOOKED, visit.getStatus()),
                () -> assertEquals(0, visit.getDoctor().getConsultationFee()
                        .compareTo(new java.math.BigDecimal("1500.00")))
        );
    }

    @Test
    @DisplayName("TC-07 an unknown number returns empty rather than throwing")
    void returnsEmptyForAnUnknownNumber() {
        assertFalse(appointmentDao.findByNumber("APT-19990101-999").isPresent());
    }

    // -----------------------------------------------------------------
    //  Slots and counting
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-08 booked times are reported for the slot calculation")
    void reportsTheTimesAlreadyBooked() {
        appointmentDao.insert(appointment("APT-20260914-001", 1, LocalTime.of(9, 0), "Patient One"));
        appointmentDao.insert(appointment("APT-20260914-002", 1, LocalTime.of(11, 0), "Patient Two"));

        List<LocalTime> booked = appointmentDao.findBookedTimes(1, MONDAY);

        assertAll(
                () -> assertEquals(2, booked.size()),
                () -> assertTrue(booked.contains(LocalTime.of(9, 0))),
                () -> assertTrue(booked.contains(LocalTime.of(11, 0)))
        );
    }

    @Test
    @DisplayName("TC-09 a cancelled visit releases its time slot")
    void cancelledVisitsFreeTheirSlot() {
        Appointment saved = appointmentDao.insert(
                appointment("APT-20260914-001", 1, LocalTime.of(9, 0), "Patient One"));

        appointmentDao.updateStatus(saved.getAppointmentId(), AppointmentStatus.CANCELLED);

        assertAll(
                () -> assertTrue(appointmentDao.findBookedTimes(1, MONDAY).isEmpty(),
                        "A cancelled appointment must not block the time"),
                () -> assertFalse(appointmentDao.existsAtSlot(1, MONDAY, LocalTime.of(9, 0)))
        );
    }

    @Test
    @DisplayName("TC-10 the day counter drives the next appointment number")
    void countsTheNumbersIssuedForADate() {
        assertEquals(0, appointmentDao.countByDate(MONDAY));

        appointmentDao.insert(appointment("APT-20260914-001", 1, LocalTime.of(9, 0), "Patient One"));
        appointmentDao.insert(appointment("APT-20260914-002", 1, LocalTime.of(9, 30), "Patient Two"));

        assertAll(
                () -> assertEquals(2, appointmentDao.countByDate(MONDAY)),
                () -> assertEquals(0, appointmentDao.countByDate(MONDAY.plusDays(1)),
                        "Each day counts its own numbers")
        );
    }

    @Test
    @DisplayName("TC-11 status changes are stored")
    void updatesTheStatus() {
        Appointment saved = appointmentDao.insert(
                appointment("APT-20260914-001", 1, LocalTime.of(9, 0), "Patient One"));

        boolean changed = appointmentDao.updateStatus(
                saved.getAppointmentId(), AppointmentStatus.COMPLETED);

        assertAll(
                () -> assertTrue(changed),
                () -> assertEquals(AppointmentStatus.COMPLETED,
                        appointmentDao.findByNumber("APT-20260914-001").orElseThrow().getStatus())
        );
    }

    // -----------------------------------------------------------------
    //  Paging
    // -----------------------------------------------------------------

    /** Stores 25 appointments across three days, newest last. */
    private void storeManyAppointments() {
        for (int i = 0; i < 25; i++) {
            Appointment booking = appointment(
                    String.format("APT-20260914-%03d", i + 1), 1,
                    LocalTime.of(9, 0).plusMinutes(15L * i), "Patient " + (i + 1));
            booking.setAppointmentDate(MONDAY.plusDays(i / 10));
            appointmentDao.insert(booking);
        }
    }

    @Test
    @DisplayName("TC-12 a page holds only the rows asked for, and the count holds them all")
    void readsOnePageAtATime() {
        storeManyAppointments();

        List<Appointment> first = appointmentDao.findPage(0, 10);
        List<Appointment> last = appointmentDao.findPage(20, 10);

        assertAll(
                () -> assertEquals(25, appointmentDao.countAll()),
                () -> assertEquals(10, first.size()),
                () -> assertEquals(5, last.size(), "the last page is short"),
                () -> assertTrue(appointmentDao.findPage(30, 10).isEmpty(),
                        "past the end there is nothing to show")
        );
    }

    @Test
    @DisplayName("TC-13 consecutive pages neither repeat a row nor skip one")
    void pagesDoNotOverlap() {
        storeManyAppointments();

        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int offset = 0; offset < 25; offset += 10) {
            for (Appointment booking : appointmentDao.findPage(offset, 10)) {
                assertTrue(seen.add(booking.getAppointmentNo()),
                        booking.getAppointmentNo() + " appeared on two pages");
            }
        }
        assertEquals(25, seen.size(), "every appointment was on exactly one page");
    }

    @Test
    @DisplayName("TC-14 the badge counts come from the database, not from the page")
    void countsByStatus() {
        appointmentDao.insert(appointment("APT-20260914-001", 1, LocalTime.of(9, 0), "Saman"));
        Appointment second = appointmentDao.insert(
                appointment("APT-20260914-002", 1, LocalTime.of(9, 30), "Nimal"));
        appointmentDao.updateStatus(second.getAppointmentId(), AppointmentStatus.COMPLETED);

        assertAll(
                () -> assertEquals(1, appointmentDao.countByStatus(AppointmentStatus.BOOKED)),
                () -> assertEquals(1, appointmentDao.countByStatus(AppointmentStatus.COMPLETED)),
                () -> assertEquals(0, appointmentDao.countByStatus(AppointmentStatus.CANCELLED)),
                () -> assertEquals(0, appointmentDao.countByStatus(null))
        );
    }
}
