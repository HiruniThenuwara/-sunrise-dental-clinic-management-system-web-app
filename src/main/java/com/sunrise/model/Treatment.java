package com.sunrise.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A treatment type offered by the clinic (Requirement 4).
 *
 * <p>The cost is stored as {@link BigDecimal} rather than {@code double}.
 * Binary floating point cannot represent decimal money exactly, so a total
 * built from doubles can end in 8,999.999999 and print incorrectly on a
 * patient's receipt.</p>
 */
public class Treatment implements Serializable {

    private static final long serialVersionUID = 1L;

    private int treatmentId;
    private String treatmentName;
    private String description;
    private BigDecimal baseCost = BigDecimal.ZERO;
    private int estimatedMinutes = 30;
    private boolean active = true;

    public Treatment() {
        // used when building the object from a ResultSet
    }

    public Treatment(int treatmentId, String treatmentName, BigDecimal baseCost,
                     int estimatedMinutes) {
        this.treatmentId = treatmentId;
        this.treatmentName = treatmentName;
        this.baseCost = baseCost;
        this.estimatedMinutes = estimatedMinutes;
    }

    public int getTreatmentId() {
        return treatmentId;
    }

    public void setTreatmentId(int treatmentId) {
        this.treatmentId = treatmentId;
    }

    public String getTreatmentName() {
        return treatmentName;
    }

    public void setTreatmentName(String treatmentName) {
        this.treatmentName = treatmentName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBaseCost() {
        return baseCost;
    }

    public void setBaseCost(BigDecimal baseCost) {
        this.baseCost = baseCost;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * A consultation carries no treatment charge of its own; the patient pays
     * only the dentist's fee. The billing strategy uses this to decide how to
     * price the visit.
     *
     * @return {@code true} when the treatment has no cost of its own
     */
    public boolean isConsultationOnly() {
        return baseCost == null || baseCost.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Treatment)) {
            return false;
        }
        return treatmentId == ((Treatment) other).treatmentId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(treatmentId);
    }

    @Override
    public String toString() {
        return "Treatment{id=" + treatmentId + ", name='" + treatmentName
                + "', baseCost=" + baseCost + '}';
    }
}
