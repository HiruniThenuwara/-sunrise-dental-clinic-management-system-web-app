package com.sunrise.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * A patient visit (Requirements 2 and 3).
 *
 * <p>This is the centre of the system. It holds references to the patient,
 * the dentist and the treatment rather than copies of their fields, so a
 * corrected patient name appears everywhere at once.</p>
 *
 * <p>The database enforces
 * {@code UNIQUE (doctor_id, appointment_date, appointment_time)}, which makes
 * the double bookings described in the scenario impossible even if two
 * receptionists submit at the same moment.</p>
 */
public class Appointment implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_DISPLAY = DateTimeFormatter.ofPattern("hh:mm a");

    private int appointmentId;
    private String appointmentNo;
    private Patient patient;
    private Doctor doctor;
    private Treatment treatment;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private AppointmentStatus status = AppointmentStatus.BOOKED;
    private BookingType bookingType = BookingType.WALK_IN;
    private String notes;
    private User createdBy;
    private LocalDateTime createdAt;

    public Appointment() {
        // used when building the object from a ResultSet
    }

    public int getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(int appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getAppointmentNo() {
        return appointmentNo;
    }

    public void setAppointmentNo(String appointmentNo) {
        this.appointmentNo = appointmentNo;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public Treatment getTreatment() {
        return treatment;
    }

    public void setTreatment(Treatment treatment) {
        this.treatment = treatment;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    /** @return whether the patient walked in or booked online */
    public BookingType getBookingType() {
        return bookingType;
    }

    public void setBookingType(BookingType bookingType) {
        this.bookingType = bookingType == null ? BookingType.WALK_IN : bookingType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * An appointment can be cancelled only while it is still booked and the
     * date has not passed.
     *
     * @return {@code true} when the Cancel action should be offered
     */
    public boolean isCancellable() {
        return status == AppointmentStatus.BOOKED
                && appointmentDate != null
                && !appointmentDate.isBefore(LocalDate.now());
    }

    /**
     * The amount the patient is expected to pay, shown before the visit.
     * The real total is fixed when the bill is generated.
     *
     * @return consultation fee plus treatment cost, or zero when either is
     *         missing
     */
    public BigDecimal getEstimatedTotal() {
        BigDecimal fee = (doctor == null || doctor.getConsultationFee() == null)
                ? BigDecimal.ZERO : doctor.getConsultationFee();
        BigDecimal cost = (treatment == null || treatment.getBaseCost() == null)
                ? BigDecimal.ZERO : treatment.getBaseCost();
        return fee.add(cost);
    }

    /** @return the date written for staff, for example 01 Sep 2026 */
    public String getFormattedDate() {
        return appointmentDate == null ? "" : appointmentDate.format(DATE_DISPLAY);
    }

    /** @return the time written for staff, for example 09:00 AM */
    public String getFormattedTime() {
        return appointmentTime == null ? "" : appointmentTime.format(TIME_DISPLAY);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Appointment)) {
            return false;
        }
        return Objects.equals(appointmentNo, ((Appointment) other).appointmentNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appointmentNo);
    }

    @Override
    public String toString() {
        return "Appointment{no='" + appointmentNo + "', date=" + appointmentDate
                + ", time=" + appointmentTime + ", status=" + status + '}';
    }
}
