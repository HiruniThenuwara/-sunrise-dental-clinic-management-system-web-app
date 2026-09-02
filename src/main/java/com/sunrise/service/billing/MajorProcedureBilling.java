package com.sunrise.service.billing;

import com.sunrise.model.Treatment;

import java.math.BigDecimal;

/**
 * Pricing for a major procedure such as a root canal, a crown or braces.
 *
 * <p>Clinic policy: on a procedure of this size the consultation is treated
 * as part of the procedure and is not charged separately. The patient pays
 * the quoted price of the treatment and nothing more, which is what they were
 * told when the work was agreed.</p>
 *
 * <p>This is exactly the kind of rule that would otherwise be buried in an
 * {@code if} inside the billing method. As a class it can be read on its own,
 * and the threshold can be changed in one place.</p>
 */
public class MajorProcedureBilling implements BillingStrategy {

    @Override
    public boolean supports(Treatment treatment) {
        return treatment != null
                && treatment.getBaseCost() != null
                && treatment.getBaseCost()
                        .compareTo(StandardTreatmentBilling.MAJOR_PROCEDURE_FROM) >= 0;
    }

    @Override
    public BigDecimal consultationCharge(Treatment treatment, BigDecimal consultationFee) {
        // Included in the procedure price, so nothing is added.
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal treatmentCharge(Treatment treatment) {
        return treatment.getBaseCost();
    }

    @Override
    public String describe() {
        return "Major procedure";
    }
}
