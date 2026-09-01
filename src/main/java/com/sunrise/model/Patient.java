package com.sunrise.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * A patient of the clinic (Requirement 2).
 *
 * <p>Holds the personal details listed in the scenario: name, address and
 * contact number, plus optional NIC, email, date of birth and gender.</p>
 */
public class Patient implements Serializable {

    private static final long serialVersionUID = 1L;

    private int patientId;
    private String patientName;
    private String address;
    private String contactNumber;
    private String email;
    private String nic;
    private LocalDate dateOfBirth;
    private Gender gender;

    public Patient() {
        // used when building the object from a ResultSet
    }

    public Patient(String patientName, String address, String contactNumber) {
        this.patientName = patientName;
        this.address = address;
        this.contactNumber = contactNumber;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    /**
     * @return the patient's age in whole years, or -1 when the date of birth
     *         was not recorded
     */
    public int getAge() {
        if (dateOfBirth == null) {
            return -1;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /** @return initials for the avatar shown beside the patient name */
    public String getInitials() {
        if (patientName == null || patientName.isBlank()) {
            return "?";
        }
        StringBuilder initials = new StringBuilder();
        for (String part : patientName.trim().split("\\s+")) {
            if (!part.isEmpty()) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 2) {
                break;
            }
        }
        return initials.toString();
    }

    /**
     * @return the contact number grouped for reading, for example
     *         071 234 5678
     */
    public String getFormattedContactNumber() {
        if (contactNumber == null || contactNumber.length() != 10) {
            return contactNumber;
        }
        return contactNumber.substring(0, 3) + " "
                + contactNumber.substring(3, 6) + " "
                + contactNumber.substring(6);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Patient)) {
            return false;
        }
        return patientId == ((Patient) other).patientId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(patientId);
    }

    @Override
    public String toString() {
        return "Patient{id=" + patientId + ", name='" + patientName
                + "', contact='" + contactNumber + "'}";
    }
}
