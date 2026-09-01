package com.sunrise.service;

import com.sunrise.dao.DaoFactory;
import com.sunrise.dao.DoctorDao;
import com.sunrise.model.Doctor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Business rules for managing dentists.
 *
 * <p>The servlet passes raw form values in; this class validates them, builds
 * the {@link Doctor} object and asks the DAO to store it. Keeping that work
 * here rather than in the servlet is what allows it to be tested without a
 * web server.</p>
 */
public class DoctorService {

    private final DoctorDao doctorDao;
    private final ValidationService validationService;

    /** Production constructor - takes the DAO from the factory. */
    public DoctorService() {
        this(DaoFactory.getDoctorDao(), new ValidationService());
    }

    /**
     * Constructor used by the unit tests, so a mock DAO can be supplied.
     */
    public DoctorService(DoctorDao doctorDao, ValidationService validationService) {
        this.doctorDao = doctorDao;
        this.validationService = validationService;
    }

    /**
     * @return every dentist, for the management screen
     */
    public List<Doctor> findAll() {
        return doctorDao.findAll();
    }

    /**
     * @return only dentists who can accept bookings, for the appointment form
     */
    public List<Doctor> findAllActive() {
        return doctorDao.findAllActive();
    }

    public Optional<Doctor> findById(int doctorId) {
        return doctorDao.findById(doctorId);
    }

    public int countActive() {
        return doctorDao.countActive();
    }

    /**
     * Validates the dentist form and saves it, adding a new record or
     * updating an existing one.
     *
     * @param doctorId 0 for a new dentist, otherwise the id being edited
     * @return the outcome, carrying either the saved dentist or the list of
     *         problems to show on the form
     */
    public SaveResult save(int doctorId,
                           String doctorName,
                           String specialization,
                           String contactNumber,
                           String email,
                           String consultationFee,
                           boolean active) {

        BigDecimal fee = parseFee(consultationFee);

        List<String> errors = validationService.validateDoctorForm(
                doctorName, specialization, contactNumber, email, fee);

        if (!errors.isEmpty()) {
            return SaveResult.failed(errors);
        }

        Doctor doctor = new Doctor();
        doctor.setDoctorId(doctorId);
        doctor.setDoctorName(doctorName.trim());
        doctor.setSpecialization(specialization.trim());
        doctor.setContactNumber(emptyToNull(contactNumber));
        doctor.setEmail(emptyToNull(email));
        doctor.setConsultationFee(fee);
        doctor.setActive(active);

        if (doctorId > 0) {
            boolean updated = doctorDao.update(doctor);
            return updated
                    ? SaveResult.updated(doctor)
                    : SaveResult.failed(List.of("The dentist could not be updated. Please try again."));
        }

        Doctor saved = doctorDao.insert(doctor);
        return saved.getDoctorId() > 0
                ? SaveResult.created(saved)
                : SaveResult.failed(List.of("The dentist could not be added. Please try again."));
    }

    /**
     * Turns a dentist on or off for new bookings.
     *
     * <p>There is deliberately no delete. A dentist who leaves still appears
     * on past appointments and bills, and removing the row would destroy that
     * history.</p>
     */
    public boolean setActive(int doctorId, boolean active) {
        return doctorId > 0 && doctorDao.setActive(doctorId, active);
    }

    /** Reads the fee from the form, treating anything unreadable as invalid. */
    private BigDecimal parseFee(String consultationFee) {
        if (consultationFee == null || consultationFee.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(consultationFee.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /**
     * The outcome of saving a dentist: either the saved record, or the list
     * of problems to show back on the form.
     */
    public static final class SaveResult {

        private final boolean success;
        private final boolean newRecord;
        private final Doctor doctor;
        private final List<String> errors;

        private SaveResult(boolean success, boolean newRecord, Doctor doctor, List<String> errors) {
            this.success = success;
            this.newRecord = newRecord;
            this.doctor = doctor;
            this.errors = errors;
        }

        static SaveResult created(Doctor doctor) {
            return new SaveResult(true, true, doctor, List.of());
        }

        static SaveResult updated(Doctor doctor) {
            return new SaveResult(true, false, doctor, List.of());
        }

        static SaveResult failed(List<String> errors) {
            return new SaveResult(false, false, null, errors);
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isNewRecord() {
            return newRecord;
        }

        public Doctor getDoctor() {
            return doctor;
        }

        public List<String> getErrors() {
            return errors;
        }

        /** @return the message to show in the toast after a successful save */
        public String getSuccessMessage() {
            if (!success) {
                return "";
            }
            return newRecord
                    ? doctor.getDoctorName() + " was added successfully."
                    : doctor.getDoctorName() + " was updated successfully.";
        }
    }
}
