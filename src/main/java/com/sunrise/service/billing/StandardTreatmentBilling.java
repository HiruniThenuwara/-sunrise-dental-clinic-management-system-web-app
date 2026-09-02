package com.sunrise.service.billing;

import com.sunrise.model.Treatment;

import java.math.BigDecimal;

/**
 * Pricing for an everyday treatment such as a scaling, a filling or an
 * extraction.
 *
 * <p>The patient pays the dentist's consultation fee plus the cost of the
 * treatment. This is the ordinary case and covers most visits.</p>
 */
public class StandardTreatmentBilling implements BillingStrategy {

    /** At or above this amount the visit counts as a major procedure. */
    static final BigDecimal MAJOR_PROCEDURE_FROM = new BigDecimal("25000");

    @Override
    public boolean supports(Treatment treatment) {
        if (treatment == null || treatment.getBaseCost() == null) {
            return false;
        }
        BigDecimal cost = treatment.getBaseCost();
        return cost.compareTo(BigDecimal.ZERO) > 0
                && cost.compareTo(MAJOR_PROCEDURE_FROM) < 0;
    }

    @Override
    public BigDecimal consultationCharge(Treatment treatment, BigDecimal consultationFee) {
        return consultationFee == null ? BigDecimal.ZERO : consultationFee;
    }

    @Override
    public BigDecimal treatmentCharge(Treatment treatment) {
        return treatment.getBaseCost();
    }

    @Override
    public String describe() {
        return "Standard treatment";
    }
}
