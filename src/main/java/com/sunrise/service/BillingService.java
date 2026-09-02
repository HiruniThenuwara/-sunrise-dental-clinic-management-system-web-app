package com.sunrise.service;

import com.sunrise.dao.BillDao;
import com.sunrise.dao.DaoFactory;
import com.sunrise.model.Appointment;
import com.sunrise.model.Bill;
import com.sunrise.model.PaymentMethod;
import com.sunrise.model.PaymentStatus;
import com.sunrise.model.Treatment;
import com.sunrise.model.User;
import com.sunrise.service.billing.BillingStrategy;
import com.sunrise.service.billing.ConsultationOnlyBilling;
import com.sunrise.service.billing.MajorProcedureBilling;
import com.sunrise.service.billing.StandardTreatmentBilling;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Calculating and storing patient bills (Requirement 4).
 *
 * <p>The scenario lists billing errors as one of the clinic's problems. Doing
 * the arithmetic here, once, in code that is unit tested, is what removes
 * them: the same visit always produces the same total, no matter who is at
 * the front desk.</p>
 *
 * <p><b>Design pattern: Strategy.</b> The service does not decide prices
 * itself. It selects the {@link BillingStrategy} that matches the treatment
 * and delegates. Adding a new pricing rule means writing a new strategy class
 * and adding it to the list below, without touching this method.</p>
 *
 * <p>All money is {@link BigDecimal} rounded to two decimal places with
 * {@link RoundingMode#HALF_UP}, the rounding people expect on a receipt.</p>
 */
public class BillingService {

    private static final Logger LOGGER = Logger.getLogger(BillingService.class.getName());

    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String NUMBER_PREFIX = "BILL-";
    private static final int MONEY_SCALE = 2;

    private final BillDao billDao;
    private final List<BillingStrategy> strategies;

    /** Production constructor - takes the DAO from the factory. */
    public BillingService() {
        this(DaoFactory.getBillDao());
    }

    /**
     * Constructor used by the unit tests, so a mock DAO can be supplied.
     * The pricing rules are the real ones, because they are what is tested.
     */
    public BillingService(BillDao billDao) {
        this(billDao, List.of(
                new ConsultationOnlyBilling(),
                new MajorProcedureBilling(),
                new StandardTreatmentBilling()));
    }

    /**
     * Constructor that also accepts the pricing rules, so a future rule can
     * be tested in isolation.
     */
    public BillingService(BillDao billDao, List<BillingStrategy> strategies) {
        this.billDao = billDao;
        this.strategies = strategies;
    }

    /**
     * Produces the bill for a completed visit and stores it.
     *
     * @param appointment   the visit being billed
     * @param discount      any discount approved by the dentist
     * @param paymentMethod how the patient paid
     * @param billedBy      the staff member taking the payment
     * @return the stored bill, or the problems to show on the screen
     */
    public BillResult generate(Appointment appointment,
                               BigDecimal discount,
                               PaymentMethod paymentMethod,
                               User billedBy) {

        List<String> errors = new ArrayList<>();

        if (appointment == null || appointment.getTreatment() == null
                || appointment.getDoctor() == null) {
            errors.add("Find an appointment first, then generate the bill.");
            return BillResult.failed(errors);
        }

        // A visit is billed once. Billing it again would double the takings.
        if (billDao.findByAppointment(appointment.getAppointmentId()).isPresent()) {
            errors.add("This appointment has already been billed.");
            return BillResult.failed(errors);
        }

        BigDecimal safeDiscount = discount == null ? BigDecimal.ZERO : discount;
        if (safeDiscount.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Discount cannot be a negative amount.");
            return BillResult.failed(errors);
        }

        // The Strategy pattern in use: pick the rule, then delegate.
        BillingStrategy strategy = selectStrategy(appointment.getTreatment());

        BigDecimal consultationFee = money(strategy.consultationCharge(
                appointment.getTreatment(), appointment.getDoctor().getConsultationFee()));
        BigDecimal treatmentCost = money(strategy.treatmentCharge(appointment.getTreatment()));
        BigDecimal subtotal = consultationFee.add(treatmentCost);

        if (safeDiscount.compareTo(subtotal) > 0) {
            errors.add("Discount cannot be more than the total of "
                    + subtotal.toPlainString() + ".");
            return BillResult.failed(errors);
        }

        Bill bill = new Bill();
        bill.setBillNo(generateBillNumber(LocalDate.now()));
        bill.setAppointment(appointment);
        bill.setConsultationFee(consultationFee);
        bill.setTreatmentCost(treatmentCost);
        bill.setDiscount(money(safeDiscount));
        bill.setTax(money(BigDecimal.ZERO));
        bill.setTotalAmount(money(subtotal.subtract(safeDiscount)));
        bill.setPaymentMethod(paymentMethod == null ? PaymentMethod.CASH : paymentMethod);
        bill.setPaymentStatus(PaymentStatus.PAID);
        bill.setBilledBy(billedBy);

        Bill stored = billDao.insert(bill);
        if (stored == null) {
            return BillResult.failed(List.of("The bill could not be saved. Please try again."));
        }

        LOGGER.info("Bill " + stored.getBillNo() + " produced using the "
                + strategy.describe() + " rule, total " + stored.getTotalAmount());

        return BillResult.saved(stored, strategy.describe());
    }

    /**
     * Chooses the pricing rule for a treatment.
     *
     * @return the first rule that supports the treatment, falling back to the
     *         standard rule so a bill can always be produced
     */
    public BillingStrategy selectStrategy(Treatment treatment) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(treatment))
                .findFirst()
                .orElseGet(StandardTreatmentBilling::new);
    }

    /**
     * Builds the next bill number for a date, for example
     * {@code BILL-20260907-001}.
     */
    public String generateBillNumber(LocalDate date) {
        int alreadyBilled = billDao.countByDate(date);
        return NUMBER_PREFIX + date.format(NUMBER_DATE)
                + String.format("-%03d", alreadyBilled + 1);
    }

    /** @return the bill already produced for a visit, if there is one */
    public Optional<Bill> findByAppointment(int appointmentId) {
        return billDao.findByAppointment(appointmentId);
    }

    /** @return the money taken on one date, for the dashboard */
    public BigDecimal takingsFor(LocalDate date) {
        return billDao.sumTotalByDate(date);
    }

    /** Rounds an amount to two decimal places, the way money is written. */
    private BigDecimal money(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * The outcome of billing a visit: either the stored bill, or the problems
     * to show on the screen.
     */
    public static final class BillResult {

        private final boolean success;
        private final Bill bill;
        private final String ruleApplied;
        private final List<String> errors;

        private BillResult(boolean success, Bill bill, String ruleApplied, List<String> errors) {
            this.success = success;
            this.bill = bill;
            this.ruleApplied = ruleApplied;
            this.errors = errors;
        }

        static BillResult saved(Bill bill, String ruleApplied) {
            return new BillResult(true, bill, ruleApplied, List.of());
        }

        static BillResult failed(List<String> errors) {
            return new BillResult(false, null, "", errors);
        }

        public boolean isSuccess() {
            return success;
        }

        public Bill getBill() {
            return bill;
        }

        /** @return which pricing rule produced this bill */
        public String getRuleApplied() {
            return ruleApplied;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
