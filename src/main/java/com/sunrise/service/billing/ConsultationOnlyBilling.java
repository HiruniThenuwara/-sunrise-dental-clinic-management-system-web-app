package com.sunrise.service.billing;

import com.sunrise.model.Treatment;

import java.math.BigDecimal;

/**
 * Pricing for a visit where no treatment was carried out.
 *
 * <p>A check-up or an advice appointment has no treatment charge of its own,
 * so the patient pays the dentist's consultation fee and nothing else.</p>
 */
public class ConsultationOnlyBilling implements BillingStrategy {

    @Override
    public boolean supports(Treatment treatment) {
        return treatment != null
                && (treatment.getBaseCost() == null
                    || treatment.getBaseCost().compareTo(BigDecimal.ZERO) == 0);
    }

    @Override
    public BigDecimal consultationCharge(Treatment treatment, BigDecimal consultationFee) {
        return consultationFee == null ? BigDecimal.ZERO : consultationFee;
    }

    @Override
    public BigDecimal treatmentCharge(Treatment treatment) {
        return BigDecimal.ZERO;
    }

    @Override
    public String describe() {
        return "Consultation only";
    }
}
