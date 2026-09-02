package com.sunrise.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * The patient bill and receipt (Requirement 4).
 *
 * <pre>
 *   totalAmount = consultationFee + treatmentCost - discount + tax
 * </pre>
 *
 * <p>The fee and the cost are <b>copied</b> into the bill when it is
 * generated rather than read from the dentist and treatment each time it is
 * displayed. If a price changes next month, a receipt reprinted for an old
 * visit still shows what the patient actually paid.</p>
 *
 * <p>A bill cannot exist without its appointment. The database uses
 * {@code ON DELETE CASCADE}, which is the composition relationship shown in
 * the class diagram.</p>
 */
public class Bill implements Serializable {

    private static final long serialVersionUID = 1L;

    private int billId;
    private String billNo;
    private Appointment appointment;
    private BigDecimal consultationFee = BigDecimal.ZERO;
    private BigDecimal treatmentCost = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal tax = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private PaymentMethod paymentMethod = PaymentMethod.CASH;
    private PaymentStatus paymentStatus = PaymentStatus.PAID;
    private User billedBy;
    private LocalDateTime billedAt;

    public Bill() {
        // used when building the object from a ResultSet
    }

    public int getBillId() {
        return billId;
    }

    public void setBillId(int billId) {
        this.billId = billId;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public BigDecimal getTreatmentCost() {
        return treatmentCost;
    }

    public void setTreatmentCost(BigDecimal treatmentCost) {
        this.treatmentCost = treatmentCost;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public User getBilledBy() {
        return billedBy;
    }

    public void setBilledBy(User billedBy) {
        this.billedBy = billedBy;
    }

    public LocalDateTime getBilledAt() {
        return billedAt;
    }

    public void setBilledAt(LocalDateTime billedAt) {
        this.billedAt = billedAt;
    }

    /**
     * The amount charged before the discount was taken off, shown as a
     * subtotal line on the receipt.
     *
     * @return consultation fee plus treatment cost
     */
    public BigDecimal getSubtotal() {
        return nullSafe(consultationFee).add(nullSafe(treatmentCost));
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Bill)) {
            return false;
        }
        return Objects.equals(billNo, ((Bill) other).billNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(billNo);
    }

    @Override
    public String toString() {
        return "Bill{no='" + billNo + "', total=" + totalAmount
                + ", method=" + paymentMethod + '}';
    }
}
