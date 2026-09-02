package com.sunrise.service;

import com.sunrise.dao.AppointmentDao;
import com.sunrise.dao.PatientDao;
import com.sunrise.model.Appointment;
import com.sunrise.model.AppointmentStatus;
import com.sunrise.model.Patient;
import com.sunrise.model.Role;
import com.sunrise.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AppointmentService} - Requirement 2.
 *
 * <p><b>Test driven development.</b> Written before the implementation, so
 * the first run fails on purpose.</p>
 *
 * <p>These are the most important tests in the system. The scenario names
 * double bookings as the clinic's main problem, and TC-05 and TC-06 are the
 * tests that prove the software prevents them.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService - registering a patient visit")
class AppointmentServiceTest {

    /** 07 September 2026 is a Monday. */
    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 7);
    private static final LocalTime NINE_THIRTY = LocalTime.of(9, 30);

    @Mock
    private AppointmentDao appointmentDao;

    @Mock
    private PatientDao patientDao;

    @Mock
    private SlotService slotService;

    private AppointmentService appointmentService;
    private User receptionist;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(
                appointmentDao, patientDao, slotService, new ValidationService());

        receptionist = new User(2, "nimali", "hash", "salt",
                "Nimali Perera", Role.RECEPTIONIST, true);
    }

    /** Calls register with a correct form, so each test varies one thing. */
    private AppointmentService.RegistrationResult registerValidForm() {
        return appointmentService.register(
                "Saman Kumara",
                "No 45, Galle Road, Colombo 03",
                "0712345678",
                "saman@gmail.com",
                "199012345678",
                "1",
                "2",
                MONDAY,
                "09:30",
                "Regular cleaning",
                receptionist);
    }

    // =================================================================
    //  Appointment numbers
    // =================================================================

    @Test
    @DisplayName("TC-01 the first booking of a day is numbered 001")
    void numbersTheFirstBookingOfADayAsOne() {
        when(appointmentDao.countByDate(MONDAY)).thenReturn(0);

        assertEquals("APT-20260907-001", appointmentService.generateAppointmentNumber(MONDAY));
    }

    @Test
    @DisplayName("TC-02 the number continues from the bookings already made")
    void continuesTheNumberingWithinTheDay() {
        when(appointmentDao.countByDate(MONDAY)).thenReturn(2);

        assertEquals("APT-20260907-003", appointmentService.generateAppointmentNumber(MONDAY));
    }

    @Test
    @DisplayName("TC-03 the counter is padded to three digits")
    void padsTheCounterToThreeDigits() {
        when(appointmentDao.countByDate(MONDAY)).thenReturn(41);

        assertEquals("APT-20260907-042", appointmentService.generateAppointmentNumber(MONDAY));
    }

    @Test
    @DisplayName("TC-04 each day starts its own numbering")
    void startsNumberingAgainOnANewDay() {
        LocalDate tuesday = LocalDate.of(2026, 9, 8);
        when(appointmentDao.countByDate(tuesday)).thenReturn(0);

        assertEquals("APT-20260908-001", appointmentService.generateAppointmentNumber(tuesday));
    }

    // =================================================================
    //  Double booking, the main problem in the scenario
    // =================================================================

    @Test
    @DisplayName("TC-05 a time that is already taken is refused")
    void refusesADoubleBooking() {
        when(slotService.isSlotAvailable(1, MONDAY, NINE_THIRTY)).thenReturn(false);

        AppointmentService.RegistrationResult result = registerValidForm();

        assertAll(
                () -> assertFalse(result.isSuccess()),
                () -> assertTrue(result.getErrors().stream()
                                .anyMatch(e -> e.toLowerCase().contains("already booked")),
                        "The message must say the time is taken, got: " + result.getErrors()),
                () -> verify(appointmentDao, never()).insert(any()),
                () -> verify(patientDao, never()).insert(any())
        );
    }

    @Test
    @DisplayName("TC-06 nothing is written to the database when the slot is taken")
    void writesNothingWhenTheSlotIsTaken() {
        when(slotService.isSlotAvailable(1, MONDAY, NINE_THIRTY)).thenReturn(false);

        registerValidForm();

        verify(appointmentDao, never()).insert(any());
    }

    // =================================================================
    //  The happy path
    // =================================================================

    @Test
    @DisplayName("TC-07 a correct form is saved and given a number")
    void savesAValidAppointment() {
        when(slotService.isSlotAvailable(1, MONDAY, NINE_THIRTY)).thenReturn(true);
        when(appointmentDao.countByDate(MONDAY)).thenReturn(0);
        when(patientDao.findByContactNumber("0712345678")).thenReturn(Optional.empty());
        when(appointmentDao.insert(any(Appointment.class)))
                .thenAnswer(call -> call.getArgument(0));

        AppointmentService.RegistrationResult result = registerValidForm();

        assertAll(
                () -> assertTrue(result.isSuccess(), "Errors: " + result.getErrors()),
                () -> assertEquals("APT-20260907-001", result.getAppointment().getAppointmentNo()),
                () -> assertEquals(AppointmentStatus.BOOKED, result.getAppointment().getStatus()),
                () -> assertEquals(MONDAY, result.getAppointment().getAppointmentDate()),
                () -> assertEquals(NINE_THIRTY, result.getAppointment().getAppointmentTime()),
                () -> assertEquals("Nimali Perera",
                        result.getAppointment().getCreatedBy().getFullName())
        );
    }

    @Test
    @DisplayName("TC-08 a returning patient is not stored twice")
    void reusesAnExistingPatientRecord() {
        Patient existing = new Patient("Saman Kumara", "No 45, Galle Road", "0712345678");
        existing.setPatientId(7);

        when(slotService.isSlotAvailable(1, MONDAY, NINE_THIRTY)).thenReturn(true);
        when(appointmentDao.countByDate(MONDAY)).thenReturn(0);
        when(patientDao.findByContactNumber("0712345678")).thenReturn(Optional.of(existing));
        when(appointmentDao.insert(any(Appointment.class)))
                .thenAnswer(call -> call.getArgument(0));

        AppointmentService.RegistrationResult result = registerValidForm();

        assertAll(
                () -> assertTrue(result.isSuccess()),
                () -> assertEquals(7, result.getAppointment().getPatient().getPatientId()),
                () -> verify(patientDao, never()).insert(any()),
                () -> verify(appointmentDao).insert(any(Appointment.class))
        );
    }

    // =================================================================
    //  Validation comes before anything else
    // =================================================================

    @Test
    @DisplayName("TC-09 an invalid form is refused before the database is touched")
    void refusesAnInvalidFormWithoutTouchingTheDatabase() {
        AppointmentService.RegistrationResult result = appointmentService.register(
                "X",                 // too short
                "",                  // missing address
                "12345",             // wrong contact number
                "not-an-email",
                null,
                "",                  // no dentist chosen
                "2",
                MONDAY,
                "09:30",
                null,
                receptionist);

        assertAll(
                () -> assertFalse(result.isSuccess()),
                () -> assertTrue(result.getErrors().size() >= 4,
                        "Every problem should be reported at once, got: " + result.getErrors()),
                () -> verify(appointmentDao, never()).insert(any()),
                () -> verify(patientDao, never()).insert(any())
        );
    }

    @Test
    @DisplayName("TC-10 a date in the past is refused")
    void refusesAPastDate() {
        AppointmentService.RegistrationResult result = appointmentService.register(
                "Saman Kumara",
                "No 45, Galle Road, Colombo 03",
                "0712345678",
                null,
                null,
                "1",
                "2",
                LocalDate.now().minusDays(1),
                "09:30",
                null,
                receptionist);

        assertAll(
                () -> assertFalse(result.isSuccess()),
                () -> verify(appointmentDao, never()).insert(any())
        );
    }

    // =================================================================
    //  Finding an appointment again, Requirement 3
    // =================================================================

    @Test
    @DisplayName("TC-11 an appointment can be found by its number")
    void findsAnAppointmentByItsNumber() {
        Appointment stored = new Appointment();
        stored.setAppointmentNo("APT-20260907-001");
        when(appointmentDao.findByNumber("APT-20260907-001")).thenReturn(Optional.of(stored));

        Optional<Appointment> found = appointmentService.findByNumber("APT-20260907-001");

        assertAll(
                () -> assertTrue(found.isPresent()),
                () -> assertEquals("APT-20260907-001", found.get().getAppointmentNo())
        );
    }

    @Test
    @DisplayName("TC-12 an unknown number returns nothing rather than failing")
    void returnsNothingForAnUnknownNumber() {
        when(appointmentDao.findByNumber("APT-19990101-999")).thenReturn(Optional.empty());

        assertTrue(appointmentService.findByNumber("APT-19990101-999").isEmpty());
    }

    @Test
    @DisplayName("TC-13 a blank search term never reaches the database")
    void doesNotSearchForABlankNumber() {
        assertAll(
                () -> assertTrue(appointmentService.findByNumber("   ").isEmpty()),
                () -> assertTrue(appointmentService.findByNumber(null).isEmpty()),
                () -> verify(appointmentDao, never()).findByNumber(any())
        );
    }
}
