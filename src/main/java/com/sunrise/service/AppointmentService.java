package com.sunrise.service;

import com.sunrise.dao.AppointmentDao;
import com.sunrise.dao.PatientDao;
import com.sunrise.model.Appointment;
import com.sunrise.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Appointment registration and lookup - <b>skeleton, not yet implemented</b>.
 *
 * <p>Written so that {@code AppointmentServiceTest} compiles and can be run.
 * Every method throws {@link UnsupportedOperationException}, so all thirteen
 * test cases fail on purpose. That failing run is recorded before the real
 * code is written.</p>
 */
public class AppointmentService {

    private final AppointmentDao appointmentDao;
    private final PatientDao patientDao;
    private final SlotService slotService;
    private final ValidationService validationService;

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
     * Registers a patient visit (Requirement 2).
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
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Builds the next appointment number for a date, for example
     * {@code APT-20260907-001}.
     */
    public String generateAppointmentNumber(LocalDate date) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Finds a visit by the number printed on the patient's card
     * (Requirement 3).
     */
    public Optional<Appointment> findByNumber(String appointmentNo) {
        throw new UnsupportedOperationException("Not implemented yet");
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
