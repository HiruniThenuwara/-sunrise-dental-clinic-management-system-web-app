package com.sunrise.service.billing;

import com.sunrise.model.Treatment;

import java.math.BigDecimal;

/**
 * How one kind of visit is priced.
 *
 * <p><b>Design pattern: Strategy.</b> The clinic prices three kinds of visit
 * differently. Without this pattern {@code BillingService} would hold a
 * growing {@code if-else} chain, and every new pricing rule would mean
 * editing a method that already works and re-testing all of it.</p>
 *
 * <p>With the pattern each rule is a small class that can be read, tested and
 * replaced on its own, and adding a fourth rule means adding a class rather
 * than changing an existing one.</p>
 *
 * <p>The interface separates the two charges instead of returning a single
 * total, because the receipt has to show them as separate lines and the
 * {@code bills} table stores them in separate columns.</p>
 */
public interface BillingStrategy {

    /**
     * @param treatment the treatment the patient received
     * @return {@code true} when this rule is the one that applies
     */
    boolean supports(Treatment treatment);

    /**
     * @param treatment        the treatment received
     * @param consultationFee  the dentist's standard fee
     * @return how much of the consultation fee to charge
     */
    BigDecimal consultationCharge(Treatment treatment, BigDecimal consultationFee);

    /**
     * @param treatment the treatment received
     * @return how much to charge for the treatment itself
     */
    BigDecimal treatmentCharge(Treatment treatment);

    /**
     * @return the name of the rule, shown on the receipt and used in the
     *         tests to prove the right rule was chosen
     */
    String describe();
}
