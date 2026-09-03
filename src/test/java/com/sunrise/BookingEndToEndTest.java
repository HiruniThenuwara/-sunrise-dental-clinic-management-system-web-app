package com.sunrise;

import com.sunrise.dao.TestDatabase;
import com.sunrise.dao.impl.AppointmentDaoImpl;
import com.sunrise.dao.impl.BillDaoImpl;
import com.sunrise.dao.impl.DoctorScheduleDaoImpl;
import com.sunrise.dao.impl.PatientDaoImpl;
import com.sunrise.model.Appointment;
import com.sunrise.model.AppointmentStatus;
import com.sunrise.model.Bill;
import com.sunrise.model.PaymentMethod;
import com.sunrise.model.Role;
import com.sunrise.model.TimeSlot;
import com.sunrise.model.User;
import com.sunrise.service.AppointmentService;
import com.sunrise.service.BillingService;
import com.sunrise.service.SlotService;
import com.sunrise.service.ValidationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End to end test of the clinic's main workflow.
 *
 * <p>Everything here is real except the database, which is H2 rather than
 * MySQL: real services, real DAOs, real SQL. Nothing is mocked. The test
 * follows one patient through the whole journey described in the scenario:</p>
 *
 * <ol>
 *   <li>the receptionist looks at the free times for a dentist;</li>
 *   <li>registers the visit (Requirement 2);</li>
 *   <li>a second attempt on the same slot is refused (the double booking
 *       problem the clinic asked to solve);</li>
 *   <li>the visit is found again by its number (Requirement 3);</li>
 *   <li>the bill is produced and stored (Requirement 4);</li>
 *   <li>billing the same visit twice is refused.</li>
 * </ol>
 *
 * <p>The unit tests prove each rule on its own. This one proves the parts fit
 * together, which is the failure the bill numbering defect slipped through:
 * every unit test passed while the application was broken, because the tests
 * mocked away the SQL that was wrong.</p>
 */
@DisplayName("End to end - book a visit, find it, bill it")
class BookingEndToEndTest {

    /**
     * A Monday, the day the dentist works, always a week or more ahead.
     *
     * <p>It is calculated rather than written down because the booking rules
     * refuse a time that has gone by. A fixed calendar date would one day be
     * today, and this test would then start failing every afternoon for a
     * reason that has nothing to do with the workflow it checks.</p>
     */
    private static final LocalDate MONDAY = LocalDate.now()
            .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            .plusWeeks(1);

    private TestDatabase database;
    private AppointmentService appointmentService;
    private BillingService billingService;
    private SlotService slotService;
    private User receptionist;

    @BeforeEach
    void setUp() {
        database = new TestDatabase();

        // Clinic setup: one dentist, working hours, and a treatment.
        database.execute("INSERT INTO users (user_id, username, password_hash, salt, "
                + "full_name, role, is_active) VALUES "
                + "(1, 'nimali', 'hash', 'salt', 'Nimali Perera', 'RECEPTIONIST', TRUE)");
        database.execute("INSERT INTO doctors (doctor_id, doctor_name, specialization, "
                + "consultation_fee, is_active) VALUES "
                + "(1, 'Dr. Anura Jayasinghe', 'General Dentistry', 1500.00, TRUE)");
        database.execute("INSERT INTO doctor_schedule (doctor_id, day_of_week, start_time, "
                + "end_time, slot_duration_minutes, is_active) VALUES "
                + "(1, 'MONDAY', '09:00:00', '13:00:00', 30, TRUE)");
        database.execute("INSERT INTO treatments (treatment_id, treatment_name, description, "
                + "base_cost, estimated_minutes) VALUES "
                + "(1, 'Scaling', 'Teeth cleaning and polishing', 4500.00, 45)");

        var appointmentDao = new AppointmentDaoImpl(database.connectionSource());
        var patientDao = new PatientDaoImpl(database.connectionSource());
        var scheduleDao = new DoctorScheduleDaoImpl(database.connectionSource());
        var billDao = new BillDaoImpl(database.connectionSource());

        slotService = new SlotService(scheduleDao, appointmentDao);
        appointmentService = new AppointmentService(
                appointmentDao, patientDao, slotService, new ValidationService());
        billingService = new BillingService(billDao);

        receptionist = new User(1, "nimali", "hash", "salt",
                "Nimali Perera", Role.RECEPTIONIST, true);
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    /** Registers the patient used throughout the journey. */
    private AppointmentService.RegistrationResult bookSamanAtNineThirty() {
        return appointmentService.register(
                "Saman Kumara",
                "No 45, Galle Road, Colombo 03",
                "0712345678",
                "saman@gmail.com",
                "199012345678",
                "1",
                "1",
                MONDAY,
                "09:30",
                "Regular cleaning",
                receptionist);
    }

    @Test
    @DisplayName("The whole journey: eight free slots, book one, it disappears, bill it")
    void completeJourneyFromEmptyDiaryToPrintedBill() {

        // ---------- 1. the diary starts empty ----------
        List<TimeSlot> before = slotService.generateSlots(1, MONDAY);
        assertAll("an empty Monday",
                () -> assertEquals(8, before.size(), "09:00 to 13:00 in 30 minute slots"),
                () -> assertEquals(8, slotService.countAvailable(1, MONDAY))
        );

        // ---------- 2. register the visit ----------
        AppointmentService.RegistrationResult booking = bookSamanAtNineThirty();

        assertTrue(booking.isSuccess(), "Errors: " + booking.getErrors());
        Appointment visit = booking.getAppointment();

        assertAll("the registered visit",
                () -> assertEquals("APT-20260914-001", visit.getAppointmentNo()),
                () -> assertEquals(AppointmentStatus.BOOKED, visit.getStatus()),
                () -> assertEquals(LocalTime.of(9, 30), visit.getAppointmentTime()),
                () -> assertEquals(1, database.count("SELECT COUNT(*) FROM patients")),
                () -> assertEquals(1, database.count("SELECT COUNT(*) FROM appointments"))
        );

        // ---------- 3. that time is now gone from the diary ----------
        List<TimeSlot> after = slotService.generateSlots(1, MONDAY);
        assertAll("the slot is taken",
                () -> assertEquals(8, after.size(), "The slot is still listed"),
                () -> assertEquals(7, slotService.countAvailable(1, MONDAY), "but no longer free"),
                () -> assertFalse(slotService.isSlotAvailable(1, MONDAY, LocalTime.of(9, 30)))
        );

        // ---------- 4. the double booking the clinic asked us to prevent ----------
        AppointmentService.RegistrationResult clash = appointmentService.register(
                "Ishara Senanayake", "No 8, Station Road, Dehiwala", "0754443332",
                null, null, "1", "1", MONDAY, "09:30", null, receptionist);

        assertAll("the second attempt on the same slot",
                () -> assertFalse(clash.isSuccess()),
                () -> assertTrue(clash.getErrors().stream()
                        .anyMatch(e -> e.toLowerCase().contains("already booked"))),
                () -> assertEquals(1, database.count("SELECT COUNT(*) FROM appointments"),
                        "Nothing may be written"),
                () -> assertEquals(1, database.count("SELECT COUNT(*) FROM patients"),
                        "Not even the patient")
        );

        // ---------- 5. find it again by its number (Requirement 3) ----------
        Optional<Appointment> found = appointmentService.findByNumber("APT-20260914-001");

        assertTrue(found.isPresent());
        Appointment reloaded = found.get();
        assertAll("the visit found by its number",
                () -> assertEquals("Saman Kumara", reloaded.getPatient().getPatientName()),
                () -> assertEquals("0712345678", reloaded.getPatient().getContactNumber()),
                () -> assertEquals("Dr. Anura Jayasinghe", reloaded.getDoctor().getDoctorName()),
                () -> assertEquals("Scaling", reloaded.getTreatment().getTreatmentName())
        );

        // ---------- 6. bill it (Requirement 4) ----------
        BillingService.BillResult billing = billingService.generate(
                reloaded, new BigDecimal("500"), PaymentMethod.CARD, receptionist);

        assertTrue(billing.isSuccess(), "Errors: " + billing.getErrors());
        Bill bill = billing.getBill();

        assertAll("the bill",
                () -> assertEquals("Standard treatment", billing.getRuleApplied()),
                () -> assertEquals(0, bill.getConsultationFee().compareTo(new BigDecimal("1500"))),
                () -> assertEquals(0, bill.getTreatmentCost().compareTo(new BigDecimal("4500"))),
                () -> assertEquals(0, bill.getDiscount().compareTo(new BigDecimal("500"))),
                () -> assertEquals(0, bill.getTotalAmount().compareTo(new BigDecimal("5500")),
                        "1500 + 4500 - 500"),
                () -> assertEquals(PaymentMethod.CARD, bill.getPaymentMethod()),
                () -> assertEquals(1, database.count("SELECT COUNT(*) FROM bills"))
        );

        // ---------- 7. the same visit cannot be billed twice ----------
        BillingService.BillResult again = billingService.generate(
                reloaded, BigDecimal.ZERO, PaymentMethod.CASH, receptionist);

        assertAll("billing the same visit again",
                () -> assertFalse(again.isSuccess()),
                () -> assertTrue(again.getErrors().stream()
                        .anyMatch(e -> e.toLowerCase().contains("already"))),
                () -> assertEquals(1, database.count("SELECT COUNT(*) FROM bills"))
        );
    }

    @Test
    @DisplayName("A second visit on the same day gets the next number in sequence")
    void numbersRunInSequenceWithinTheDay() {
        bookSamanAtNineThirty();

        AppointmentService.RegistrationResult second = appointmentService.register(
                "Ruwan Perera", "No 8, Station Road, Dehiwala", "0761234567",
                null, null, "1", "1", MONDAY, "10:00", null, receptionist);

        assertAll(
                () -> assertTrue(second.isSuccess(), "Errors: " + second.getErrors()),
                () -> assertEquals("APT-20260914-002", second.getAppointment().getAppointmentNo())
        );
    }

    @Test
    @DisplayName("A returning patient is matched on their telephone number")
    void doesNotStoreTheSamePatientTwice() {
        bookSamanAtNineThirty();

        // Same person, same number, a later appointment.
        AppointmentService.RegistrationResult repeat = appointmentService.register(
                "Saman Kumara", "No 45, Galle Road, Colombo 03", "0712345678",
                null, null, "1", "1", MONDAY, "11:00", "Follow up", receptionist);

        assertAll(
                () -> assertTrue(repeat.isSuccess()),
                () -> assertEquals(1, database.count("SELECT COUNT(*) FROM patients"),
                        "The patient must be reused, not duplicated"),
                () -> assertEquals(2, database.count("SELECT COUNT(*) FROM appointments"))
        );
    }

    @Test
    @DisplayName("A cancelled visit puts its time back into the diary")
    void cancellingReleasesTheSlot() {
        AppointmentService.RegistrationResult booking = bookSamanAtNineThirty();
        int appointmentId = booking.getAppointment().getAppointmentId();

        assertFalse(slotService.isSlotAvailable(1, MONDAY, LocalTime.of(9, 30)));

        appointmentService.updateStatus(appointmentId, AppointmentStatus.CANCELLED);

        assertAll(
                () -> assertTrue(slotService.isSlotAvailable(1, MONDAY, LocalTime.of(9, 30)),
                        "The time must be bookable again"),
                () -> assertEquals(8, slotService.countAvailable(1, MONDAY))
        );
    }

    @Test
    @DisplayName("A day the dentist does not work offers no slots at all")
    void offersNothingOnANonWorkingDay() {
        LocalDate tuesday = MONDAY.plusDays(1);

        assertAll(
                () -> assertTrue(slotService.generateSlots(1, tuesday).isEmpty()),
                () -> assertFalse(slotService.isSlotAvailable(1, tuesday, LocalTime.of(9, 30)))
        );
    }
}
