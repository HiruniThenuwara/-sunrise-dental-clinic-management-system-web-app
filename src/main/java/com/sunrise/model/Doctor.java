package com.sunrise.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A dentist working at the clinic.
 *
 * <p>The consultation fee lives here rather than on the appointment, because
 * it belongs to the dentist. When a bill is produced the fee is <b>copied</b>
 * onto the {@link Bill}, so raising a dentist's fee next month never changes
 * what a patient was charged last month.</p>
 */
public class Doctor implements Serializable {

    private static final long serialVersionUID = 1L;

    private int doctorId;
    private String doctorName;
    private String specialization;
    private String contactNumber;
    private String email;
    private BigDecimal consultationFee = BigDecimal.ZERO;
    private boolean active = true;

    public Doctor() {
        // used when building the object from a ResultSet
    }

    public Doctor(int doctorId, String doctorName, String specialization,
                  BigDecimal consultationFee, boolean active) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.specialization = specialization;
        this.consultationFee = consultationFee;
        this.active = active;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * The fee written the way it appears on screen and on a receipt, for
     * example {@code 1,500.00}.
     *
     * <p>Formatting lives here rather than in the JSP so that it is applied
     * the same way on every page and can be unit tested.</p>
     *
     * @return the consultation fee with a thousands separator and two decimals
     */
    public String getFormattedFee() {
        java.text.DecimalFormat format = new java.text.DecimalFormat("#,##0.00");
        return format.format(consultationFee == null ? BigDecimal.ZERO : consultationFee);
    }

    /**
     * @return up to two capital letters for the avatar in the dentist table,
     *         ignoring the "Dr." prefix
     */
    public String getInitials() {
        if (doctorName == null || doctorName.isBlank()) {
            return "?";
        }
        StringBuilder initials = new StringBuilder();
        for (String part : doctorName.trim().split("\\s+")) {
            if (part.isEmpty() || part.equalsIgnoreCase("Dr.") || part.equalsIgnoreCase("Dr")) {
                continue;
            }
            initials.append(Character.toUpperCase(part.charAt(0)));
            if (initials.length() == 2) {
                break;
            }
        }
        return initials.length() == 0 ? "?" : initials.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Doctor)) {
            return false;
        }
        return doctorId == ((Doctor) other).doctorId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(doctorId);
    }

    @Override
    public String toString() {
        return "Doctor{id=" + doctorId + ", name='" + doctorName
                + "', specialization='" + specialization + "', active=" + active + '}';
    }
}
