package com.sunrise.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * A patient together with a summary of their history at the clinic.
 *
 * <p>The patient list needs more than the {@link Patient} row itself: how
 * many times the person has visited, when they were last seen, and what they
 * have been billed. Those figures come from {@code GROUP BY} queries over the
 * appointments and bills, so they are gathered once by the database rather
 * than by asking for each patient's visits in a loop.</p>
 */
public class PatientSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final Patient patient;
    private final int visitCount;
    private final int completedCount;
    private final int cancelledCount;
    private final LocalDate lastVisit;
    private final LocalDate nextVisit;
    private final BigDecimal totalBilled;

    public PatientSummary(Patient patient, int visitCount, int completedCount,
                          int cancelledCount, LocalDate lastVisit, LocalDate nextVisit,
                          BigDecimal totalBilled) {
        this.patient = patient;
        this.visitCount = visitCount;
        this.completedCount = completedCount;
        this.cancelledCount = cancelledCount;
        this.lastVisit = lastVisit;
        this.nextVisit = nextVisit;
        this.totalBilled = totalBilled == null ? BigDecimal.ZERO : totalBilled;
    }

    public Patient getPatient() {
        return patient;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public int getCancelledCount() {
        return cancelledCount;
    }

    public LocalDate getLastVisit() {
        return lastVisit;
    }

    /** @return the next booked appointment, if the patient has one */
    public LocalDate getNextVisit() {
        return nextVisit;
    }

    public BigDecimal getTotalBilled() {
        return totalBilled;
    }

    /** @return {@code true} when the patient has an appointment still to come */
    public boolean isUpcoming() {
        return nextVisit != null;
    }

    /** @return {@code true} when this is the patient's first booking */
    public boolean isNewPatient() {
        return visitCount <= 1;
    }

    public String getFormattedTotal() {
        return MONEY.format(totalBilled);
    }

    public String getFormattedLastVisit() {
        return lastVisit == null ? "No visits yet" : lastVisit.format(DATE);
    }

    public String getFormattedNextVisit() {
        return nextVisit == null ? "None booked" : nextVisit.format(DATE);
    }

    @Override
    public String toString() {
        return patient.getPatientName() + " visits=" + visitCount + " billed=" + totalBilled;
    }
}
