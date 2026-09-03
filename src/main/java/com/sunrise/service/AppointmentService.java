package com.sunrise.service;

import com.sunrise.dao.AppointmentDao;
import com.sunrise.dao.DaoFactory;
import com.sunrise.dao.PatientDao;
import com.sunrise.model.Appointment;
import com.sunrise.model.AppointmentStatus;
import com.sunrise.model.Doctor;
import com.sunrise.model.Patient;
import com.sunrise.model.Treatment;
import com.sunrise.model.User;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Registering and finding patient visits (Requirements 2 and 3).
 *
 * <p>This class carries the rule that answers the clinic's main complaint.
 * Before anything is written it asks {@link SlotService} whether the dentist
 * is free at that exact time, and refuses the booking with a clear message if
 * they are not. The {@code UNIQUE} constraint on
 * (dentist, date, time) remains as a final safety net for the rare case where
 * two receptionists submit at the same instant.</p>
 *
 * <p>The order of work matters: validate first, then check the slot, and only
 * then touch the database. An invalid form never reaches MySQL at all.</p>
 */
public class AppointmentService {

    private static final Logger LOGGER = Logger.getLogger(AppointmentService.class.getName());

    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String NUMBER_PREFIX = "APT-";

    private final AppointmentDao appointmentDao;
    private final PatientDao patientDao;
    private final SlotService slotService;
    private final ValidationService validationService;

    /** Production constructor - takes the DAOs from the factory. */
    public AppointmentService() {
        this(DaoFactory.getAppointmentDao(),
             DaoFactory.getPatientDao(),
             new SlotService(DaoFactory.getDoctorScheduleDao(), DaoFactory.getAppointmentDao()),
             new ValidationService());
    }

    /**
     * Constructor used by the unit tests, so mocks can be supplied in place
     * of the database and the slot calculation.
     */
    public AppointmentService(AppointmentDao appointmentDao,
                              PatientDao patientDao,
                              SlotService slotService,
                              ValidationService validationService) {
        this.appointmentDao = appointmentDao;
        this.patientDao = patientDao;
        this.slotService = slotService;
        this.validationService = validationService;
    }

    /**
     * Registers a patient visit, without recording the patient's gender or
     * how the booking was made.
     *
     * <p>Kept as the shorter form of the call: a walk-in with no gender
     * recorded is the ordinary case at the front desk, and it is what every
     * appointment was before those two fields existed.</p>
     *
     * @return the saved appointment, or the problems to show on the form
     */
    public RegistrationResult register(String patientName,
                                       String address,
                                       String contactNumber,
                                       String email,
                                       String nic,
                                       String doctorIdText,
                                       String treatmentIdText,
                                       LocalDate appointmentDate,
                                       String appointmentTimeText,
                                       String notes,
                                       User createdBy) {

        return register(patientName, address, contactNumber, email, nic, null,
                doctorIdText, treatmentIdText, appointmentDate, appointmentTimeText,
                null, notes, createdBy);
    }

    /**
     * Registers a patient visit (Requirement 2).
     *
     * @param genderText      the patient's gender, or {@code null} if not given
     * @param bookingTypeText {@code WALK_IN} or {@code ONLINE}; anything else
     *                        is treated as a walk-in
     * @return the saved appointment, or the problems to show on the form
     */
    public RegistrationResult register(String patientName,
                                       String address,
                                       String contactNumber,
                                       String email,
                                       String nic,
                                       String genderText,
                                       String doctorIdText,
                                       String treatmentIdText,
                                       LocalDate appointmentDate,
                                       String appointmentTimeText,
                                       String bookingTypeText,
                                       String notes,
                                       User createdBy) {

        // 1. Validate every field first. An invalid form never reaches the
        //    database, and the staff member sees all the problems at once.
        List<String> errors = validationService.validateAppointmentForm(
                patientName, address, contactNumber, email,
                doctorIdText, treatmentIdText, appointmentDate, appointmentTimeText);

        if (!errors.isEmpty()) {
            return RegistrationResult.failed(errors);
        }

        int doctorId = Integer.parseInt(doctorIdText.trim());
        int treatmentId = Integer.parseInt(treatmentIdText.trim());
        LocalTime appointmentTime = LocalTime.parse(appointmentTimeText.trim());

        // 2. The double booking check, before anything is written.
        if (!slotService.isSlotAvailable(doctorId, appointmentDate, appointmentTime)) {
            LOGGER.warning("Refused a double booking: dentist " + doctorId
                    + " on " + appointmentDate + " at " + appointmentTime);
            return RegistrationResult.failed(List.of(
                    "That time is already booked for this dentist. Please choose another slot."));
        }

        // 3. A returning patient keeps their existing record, matched on the
        //    telephone number, so the same person is not stored twice.
        Patient patient = findOrCreatePatient(
                patientName, address, contactNumber, email, nic, genderText);

        // 4. Build and store the appointment.
        Appointment appointment = new Appointment();
        appointment.setAppointmentNo(generateAppointmentNumber(appointmentDate));
        appointment.setPatient(patient);
        appointment.setDoctor(referenceDoctor(doctorId));
        appointment.setTreatment(referenceTreatment(treatmentId));
        appointment.setAppointmentDate(appointmentDate);
        appointment.setAppointmentTime(appointmentTime);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setBookingType(com.sunrise.model.BookingType.fromString(bookingTypeText));
        appointment.setNotes(emptyToNull(notes));
        appointment.setCreatedBy(createdBy);

        Appointment saved = appointmentDao.insert(appointment);

        if (saved == null) {
            return RegistrationResult.failed(List.of(
                    "The appointment could not be saved. Please try again."));
        }

        LOGGER.info("Appointment registered: " + saved.getAppointmentNo());
        return RegistrationResult.saved(saved);
    }

    /**
     * Builds the next appointment number for a date, for example
     * {@code APT-20260907-001}.
     *
     * <p>The date is part of the number so that staff can tell at a glance
     * which day a card refers to, and the counter restarts each morning.</p>
     */
    public String generateAppointmentNumber(LocalDate date) {
        int alreadyBooked = appointmentDao.countByDate(date);
        return NUMBER_PREFIX + date.format(NUMBER_DATE)
                + String.format("-%03d", alreadyBooked + 1);
    }

    /**
     * Finds a visit by the number printed on the patient's card
     * (Requirement 3).
     *
     * @return the appointment with its patient, dentist and treatment loaded
     */
    public Optional<Appointment> findByNumber(String appointmentNo) {
        if (appointmentNo == null || appointmentNo.isBlank()) {
            return Optional.empty();
        }
        return appointmentDao.findByNumber(appointmentNo.trim().toUpperCase());
    }

    /** @return the most recent appointments, for the dashboard and the list */
    public List<Appointment> findRecent(int limit) {
        return appointmentDao.findRecent(limit);
    }

    /** @return every appointment on one date */
    public List<Appointment> findByDate(LocalDate date) {
        return appointmentDao.findByDate(date);
    }

    /** Changes the status, for example when a visit is completed. */
    public boolean updateStatus(int appointmentId, AppointmentStatus status) {
        return appointmentId > 0 && appointmentDao.updateStatus(appointmentId, status);
    }

    /**
     * Reuses the patient record when the telephone number is already on file,
     * otherwise stores a new one.
     */
    private Patient findOrCreatePatient(String patientName, String address,
                                        String contactNumber, String email,
                                        String nic, String genderText) {

        Optional<Patient> existing = patientDao.findByContactNumber(contactNumber.trim());
        if (existing.isPresent()) {
            return existing.get();
        }

        Patient patient = new Patient(patientName.trim(), address.trim(), contactNumber.trim());
        patient.setEmail(emptyToNull(email));
        patient.setNic(emptyToNull(nic));
        patient.setGender(com.sunrise.model.Gender.fromString(genderText));

        Patient inserted = patientDao.insert(patient);
        return inserted == null ? patient : inserted;
    }

    /** A lightweight Doctor carrying only the id, for the insert statement. */
    private Doctor referenceDoctor(int doctorId) {
        Doctor doctor = new Doctor();
        doctor.setDoctorId(doctorId);
        return doctor;
    }

    /** A lightweight Treatment carrying only the id, for the insert statement. */
    private Treatment referenceTreatment(int treatmentId) {
        Treatment treatment = new Treatment();
        treatment.setTreatmentId(treatmentId);
        return treatment;
    }

    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /**
     * The outcome of registering a visit: either the saved appointment, or
     * the list of problems to show back on the form.
     */
    public static final class RegistrationResult {

        private final boolean success;
        private final Appointment appointment;
        private final List<String> errors;

        private RegistrationResult(boolean success, Appointment appointment, List<String> errors) {
            this.success = success;
            this.appointment = appointment;
            this.errors = errors;
        }

        static RegistrationResult saved(Appointment appointment) {
            return new RegistrationResult(true, appointment, List.of());
        }

        static RegistrationResult failed(List<String> errors) {
            return new RegistrationResult(false, null, errors);
        }

        public boolean isSuccess() {
            return success;
        }

        public Appointment getAppointment() {
            return appointment;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
