package com.sunrise.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Server side input rules for every form in the system.
 *
 * <p>The browser checks the same fields with HTML5 attributes, which gives
 * the receptionist immediate feedback. Those checks are a convenience, not a
 * defence: anyone can remove them with the browser developer tools or send a
 * request straight to the servlet. Every rule is therefore checked again
 * here, on the server, before anything reaches the database.</p>
 *
 * <p>The class holds no state, so one instance can be shared safely by all
 * requests.</p>
 */
public class ValidationService {

    /** Letters, spaces, full stops, apostrophes and hyphens only. */
    private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]*$");

    /** Sri Lankan format: ten digits beginning with zero. */
    private static final Pattern CONTACT_NUMBER = Pattern.compile("^0\\d{9}$");

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** 24 hour time, for example 09:30 or 16:00. */
    private static final Pattern TIME = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private static final int NAME_MIN = 3;
    private static final int NAME_MAX = 100;
    private static final int ADDRESS_MIN = 5;
    private static final int ADDRESS_MAX = 255;

    /** No single visit at this clinic can reasonably cost more than this. */
    private static final BigDecimal AMOUNT_CEILING = new BigDecimal("1000000");

    /** Appointments further ahead than this are almost certainly a typing slip. */
    private static final int MAX_MONTHS_AHEAD = 12;

    /**
     * Checks a person's name, used for patients and dentists.
     *
     * <p>Digits and punctuation such as {@code <}, {@code >} and {@code '}
     * are refused. That keeps obviously wrong entries out of the patient
     * records, and it also turns away the classic injection and script
     * strings before they are ever stored.</p>
     *
     * @param name the value typed on the form
     * @return {@code true} when the name may be saved
     */
    public boolean isValidName(String name) {
        if (isBlank(name)) {
            return false;
        }
        String trimmed = name.trim();
        if (trimmed.length() < NAME_MIN || trimmed.length() > NAME_MAX) {
            return false;
        }
        return NAME.matcher(trimmed).matches();
    }

    /**
     * Checks a Sri Lankan telephone number.
     *
     * @param contactNumber the value typed on the form
     * @return {@code true} for exactly ten digits starting with zero
     */
    public boolean isValidContactNumber(String contactNumber) {
        return !isBlank(contactNumber)
                && CONTACT_NUMBER.matcher(contactNumber.trim()).matches();
    }

    /**
     * Checks a postal address. Addresses contain digits, commas and slashes,
     * so only the length is restricted.
     *
     * @param address the value typed on the form
     * @return {@code true} when the address may be saved
     */
    public boolean isValidAddress(String address) {
        if (isBlank(address)) {
            return false;
        }
        int length = address.trim().length();
        return length >= ADDRESS_MIN && length <= ADDRESS_MAX;
    }

    /**
     * Checks whether an appointment may be booked on this date.
     *
     * <p>Today is allowed, because a patient may walk in and be given a later
     * slot on the same day. Yesterday is not, and neither is a date more than
     * a year ahead, which is nearly always a typing mistake in the year.</p>
     *
     * @param date the chosen appointment date
     * @return {@code true} when the date may be booked
     */
    public boolean isBookableDate(LocalDate date) {
        if (date == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return !date.isBefore(today) && !date.isAfter(today.plusMonths(MAX_MONTHS_AHEAD));
    }

    /**
     * Checks a time in 24 hour form.
     *
     * @param time for example {@code 09:30}
     * @return {@code true} when the time can be read
     */
    public boolean isValidTime(String time) {
        return !isBlank(time) && TIME.matcher(time.trim()).matches();
    }

    /**
     * Checks an email address. The field is optional, so leaving it empty is
     * accepted, but a value that is present must be well formed.
     *
     * @param email the value typed on the form, may be {@code null}
     * @return {@code true} when the field may be saved
     */
    public boolean isValidOptionalEmail(String email) {
        if (isBlank(email)) {
            return true;
        }
        return EMAIL.matcher(email.trim()).matches();
    }

    /**
     * Checks a money value such as a fee, a cost or a discount.
     *
     * @param amount the value to check
     * @return {@code true} when the amount is zero or more and below the
     *         clinic ceiling
     */
    public boolean isValidAmount(BigDecimal amount) {
        return amount != null
                && amount.compareTo(BigDecimal.ZERO) >= 0
                && amount.compareTo(AMOUNT_CEILING) <= 0;
    }

    /**
     * Checks that a dropdown selection carries a real database id.
     *
     * @param value the submitted value, for example {@code "3"}
     * @return {@code true} when it is a positive whole number
     */
    public boolean isValidId(String value) {
        if (isBlank(value)) {
            return false;
        }
        try {
            return Integer.parseInt(value.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks the whole appointment registration form at once.
     *
     * <p>Every problem is collected rather than returning at the first one,
     * so the receptionist sees all of them together and does not have to
     * submit the form repeatedly to find the next mistake.</p>
     *
     * @return a list of messages to show, empty when the form is correct
     */
    public List<String> validateAppointmentForm(String patientName,
                                                String address,
                                                String contactNumber,
                                                String email,
                                                String doctorId,
                                                String treatmentId,
                                                LocalDate appointmentDate,
                                                String appointmentTime) {

        List<String> errors = new ArrayList<>();

        if (!isValidName(patientName)) {
            errors.add("Patient name must be 3 to 100 letters, with no digits or symbols.");
        }
        if (!isValidAddress(address)) {
            errors.add("Address is required and must be 5 to 255 characters.");
        }
        if (!isValidContactNumber(contactNumber)) {
            errors.add("Contact number must be 10 digits starting with 0, for example 0712345678.");
        }
        if (!isValidOptionalEmail(email)) {
            errors.add("Email address is not in a valid format.");
        }
        if (!isValidId(doctorId)) {
            errors.add("Please select a dentist.");
        }
        if (!isValidId(treatmentId)) {
            errors.add("Please select a treatment type.");
        }
        if (!isBookableDate(appointmentDate)) {
            errors.add("Appointment date cannot be in the past or more than a year ahead.");
        }
        if (!isValidTime(appointmentTime)) {
            errors.add("Please choose an available time slot.");
        }
        return errors;
    }

    /**
     * Checks the dentist form used on the dentist management screen.
     *
     * @return a list of messages to show, empty when the form is correct
     */
    public List<String> validateDoctorForm(String doctorName,
                                           String specialization,
                                           String contactNumber,
                                           String email,
                                           BigDecimal consultationFee) {

        List<String> errors = new ArrayList<>();

        if (!isValidName(doctorName)) {
            errors.add("Dentist name must be 3 to 100 letters, with no digits or symbols.");
        }
        if (isBlank(specialization)) {
            errors.add("Please select a specialization.");
        }
        if (!isBlank(contactNumber) && !isValidContactNumber(contactNumber)) {
            errors.add("Contact number must be 10 digits starting with 0.");
        }
        if (!isValidOptionalEmail(email)) {
            errors.add("Email address is not in a valid format.");
        }
        if (!isValidAmount(consultationFee)) {
            errors.add("Consultation fee must be zero or more.");
        }
        return errors;
    }

    /** @return {@code true} when the value is null, empty or only spaces */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
