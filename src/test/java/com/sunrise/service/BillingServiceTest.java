package com.sunrise.service;

import com.sunrise.dao.BillDao;
import com.sunrise.model.Appointment;
import com.sunrise.model.Bill;
import com.sunrise.model.Doctor;
import com.sunrise.model.Patient;
import com.sunrise.model.PaymentMethod;
import com.sunrise.model.Role;
import com.sunrise.model.Treatment;
import com.sunrise.model.User;
import com.sunrise.service.billing.BillingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BillingService} - Requirement 4.
 *
 * <p><b>Test driven development.</b> Written before the implementation, so
 * the first run fails on purpose.</p>
 *
 * <p>The clinic's second complaint in the scenario is billing errors. These
 * tests pin the arithmetic down so it cannot drift, and they prove that the
 * <b>Strategy pattern</b> picks the right pricing rule for each treatment.</p>
 *
 * <p>Every amount is compared with {@code compareTo} rather than
 * {@code equals}, because {@code BigDecimal.equals} also compares the scale,
 * so 1500.0 and 1500.00 would be reported as different.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BillingService - calculating a patient bill")
class BillingServiceTest {

    private static final LocalDate VISIT_DATE = LocalDate.of(2026, 9, 7);

    @Mock
    private BillDao billDao;

    private BillingService billingService;
    private User cashier;

    @BeforeEach
    void setUp() {
        billingService = new BillingService(billDao);
        cashier = new User(2, "nimali", "hash", "salt",
                "Nimali Perera", Role.RECEPTIONIST, true);
    }

    /** Builds a completed visit ready to be billed. */
    private Appointment visit(String treatmentName, String treatmentCost, String consultationFee) {
        Doctor doctor = new Doctor();
        doctor.setDoctorId(1);
        doctor.setDoctorName("Dr. Anura Jayasinghe");
        doctor.setConsultationFee(new BigDecimal(consultationFee));

        Treatment treatment = new Treatment();
        treatment.setTreatmentId(2);
        treatment.setTreatmentName(treatmentName);
        treatment.setBaseCost(new BigDecimal(treatmentCost));

        Appointment appointment = new Appointment();
        appointment.setAppointmentId(10);
        appointment.setAppointmentNo("APT-20260907-001");
        appointment.setAppointmentDate(VISIT_DATE);
        appointment.setAppointmentTime(LocalTime.of(9, 0));
        appointment.setPatient(new Patient("Saman Kumara", "Colombo 03", "0712345678"));
        appointment.setDoctor(doctor);
        appointment.setTreatment(treatment);
        return appointment;
    }

    /** Makes the mock DAO hand the bill straight back, as MySQL would. */
    private void daoEchoesTheBill() {
        when(billDao.insert(any(Bill.class))).thenAnswer(call -> call.getArgument(0));
    }

    // =================================================================
    //  The Strategy pattern chooses a pricing rule
    // =================================================================
    @Nested
    @DisplayName("Choosing a pricing rule")
    class StrategySelection {

        @Test
        @DisplayName("TC-01 a treatment with no cost of its own uses the consultation rule")
        void picksConsultationOnlyForAFreeTreatment() {
            BillingStrategy strategy = billingService.selectStrategy(
                    visit("Consultation", "0", "1500").getTreatment());

            assertEquals("Consultation only", strategy.describe());
        }

        @Test
        @DisplayName("TC-02 an ordinary treatment uses the standard rule")
        void picksStandardForAnOrdinaryTreatment() {
            BillingStrategy strategy = billingService.selectStrategy(
                    visit("Scaling", "4500", "1500").getTreatment());

            assertEquals("Standard treatment", strategy.describe());
        }

        @Test
        @DisplayName("TC-03 an expensive procedure uses the major procedure rule")
        void picksMajorProcedureForAnExpensiveTreatment() {
            BillingStrategy strategy = billingService.selectStrategy(
                    visit("Root Canal", "25000", "3000").getTreatment());

            assertEquals("Major procedure", strategy.describe());
        }
    }

    // =================================================================
    //  The arithmetic
    // =================================================================
    @Nested
    @DisplayName("Calculating the total")
    class Calculation {

        @Test
        @DisplayName("TC-04 a consultation charges the dentist fee only")
        void chargesOnlyTheFeeForAConsultation() {
            daoEchoesTheBill();

            BillingService.BillResult result = billingService.generate(
                    visit("Consultation", "0", "1500"),
                    BigDecimal.ZERO, PaymentMethod.CASH, cashier);

            Bill bill = result.getBill();
            assertAll(
                    () -> assertTrue(result.isSuccess(), "Errors: " + result.getErrors()),
                    () -> assertEquals(0, bill.getConsultationFee().compareTo(new BigDecimal("1500"))),
                    () -> assertEquals(0, bill.getTreatmentCost().compareTo(BigDecimal.ZERO)),
                    () -> assertEquals(0, bill.getTotalAmount().compareTo(new BigDecimal("1500")))
            );
        }

        @Test
        @DisplayName("TC-05 an ordinary treatment adds the fee to the treatment cost")
        void addsTheFeeToTheTreatmentCost() {
            daoEchoesTheBill();

            BillingService.BillResult result = billingService.generate(
                    visit("Scaling", "4500", "1500"),
                    BigDecimal.ZERO, PaymentMethod.CASH, cashier);

            Bill bill = result.getBill();
            assertAll(
                    () -> assertEquals(0, bill.getConsultationFee().compareTo(new BigDecimal("1500"))),
                    () -> assertEquals(0, bill.getTreatmentCost().compareTo(new BigDecimal("4500"))),
                    () -> assertEquals(0, bill.getTotalAmount().compareTo(new BigDecimal("6000")))
            );
        }

        @Test
        @DisplayName("TC-06 a major procedure includes the consultation in its price")
        void waivesTheFeeOnAMajorProcedure() {
            daoEchoesTheBill();

            BillingService.BillResult result = billingService.generate(
                    visit("Root Canal", "25000", "3000"),
                    BigDecimal.ZERO, PaymentMethod.CARD, cashier);

            Bill bill = result.getBill();
            assertAll(
                    () -> assertEquals(0, bill.getConsultationFee().compareTo(BigDecimal.ZERO),
                            "The consultation is included in the procedure price"),
                    () -> assertEquals(0, bill.getTreatmentCost().compareTo(new BigDecimal("25000"))),
                    () -> assertEquals(0, bill.getTotalAmount().compareTo(new BigDecimal("25000")))
            );
        }

        @Test
        @DisplayName("TC-07 a discount is taken off the total")
        void subtractsTheDiscount() {
            daoEchoesTheBill();

            BillingService.BillResult result = billingService.generate(
                    visit("Scaling", "4500", "1500"),
                    new BigDecimal("500"), PaymentMethod.CASH, cashier);

            assertEquals(0, result.getBill().getTotalAmount().compareTo(new BigDecimal("5500")));
        }

        @Test
        @DisplayName("TC-08 a discount equal to the whole total leaves nothing to pay")
        void allowsADiscountOfTheWholeAmount() {
            daoEchoesTheBill();

            BillingService.BillResult result = billingService.generate(
                    visit("Scaling", "4500", "1500"),
                    new BigDecimal("6000"), PaymentMethod.CASH, cashier);

            assertAll(
                    () -> assertTrue(result.isSuccess()),
                    () -> assertEquals(0, result.getBill().getTotalAmount().compareTo(BigDecimal.ZERO))
            );
        }

        @Test
        @DisplayName("TC-09 amounts always carry two decimal places")
        void storesAmountsWithTwoDecimals() {
            daoEchoesTheBill();

            BillingService.BillResult result = billingService.generate(
                    visit("Scaling", "4500", "1500"),
                    BigDecimal.ZERO, PaymentMethod.CASH, cashier);

            assertEquals(2, result.getBill().getTotalAmount().scale(),
                    "Money on a receipt must read 6000.00, not 6000");
        }
    }

    // =================================================================
    //  Refusing a bill that would be wrong
    // =================================================================
    @Nested
    @DisplayName("Refusing an invalid bill")
    class Refusals {

        @Test
        @DisplayName("TC-10 a discount larger than the total is refused")
        void refusesADiscountLargerThanTheTotal() {
            BillingService.BillResult result = billingService.generate(
                    visit("Scaling", "4500", "1500"),
                    new BigDecimal("9999"), PaymentMethod.CASH, cashier);

            assertAll(
                    () -> assertFalse(result.isSuccess()),
                    () -> assertTrue(result.getErrors().stream()
                            .anyMatch(e -> e.toLowerCase().contains("discount"))),
                    () -> verify(billDao, never()).insert(any())
            );
        }

        @Test
        @DisplayName("TC-11 a negative discount is refused")
        void refusesANegativeDiscount() {
            BillingService.BillResult result = billingService.generate(
                    visit("Scaling", "4500", "1500"),
                    new BigDecimal("-100"), PaymentMethod.CASH, cashier);

            assertAll(
                    () -> assertFalse(result.isSuccess()),
                    () -> verify(billDao, never()).insert(any())
            );
        }

        @Test
        @DisplayName("TC-12 a missing appointment is refused without an exception")
        void refusesANullAppointment() {
            BillingService.BillResult result = billingService.generate(
                    null, BigDecimal.ZERO, PaymentMethod.CASH, cashier);

            assertAll(
                    () -> assertFalse(result.isSuccess()),
                    () -> verify(billDao, never()).insert(any())
            );
        }

        @Test
        @DisplayName("TC-13 a visit that was already billed is not billed twice")
        void refusesToBillTheSameVisitTwice() {
            when(billDao.findByAppointment(10)).thenReturn(Optional.of(new Bill()));

            BillingService.BillResult result = billingService.generate(
                    visit("Scaling", "4500", "1500"),
                    BigDecimal.ZERO, PaymentMethod.CASH, cashier);

            assertAll(
                    () -> assertFalse(result.isSuccess()),
                    () -> assertTrue(result.getErrors().stream()
                            .anyMatch(e -> e.toLowerCase().contains("already"))),
                    () -> verify(billDao, never()).insert(any())
            );
        }
    }

    // =================================================================
    //  Bill numbers
    // =================================================================
    @Nested
    @DisplayName("Bill numbers")
    class BillNumbers {

        @Test
        @DisplayName("TC-14 the first bill of a day is numbered 001")
        void numbersTheFirstBillOfADay() {
            when(billDao.countByDate(VISIT_DATE)).thenReturn(0);

            assertEquals("BILL-20260907-001", billingService.generateBillNumber(VISIT_DATE));
        }

        @Test
        @DisplayName("TC-15 the counter continues within the day and is padded")
        void continuesTheNumberingWithinTheDay() {
            when(billDao.countByDate(VISIT_DATE)).thenReturn(7);

            assertEquals("BILL-20260907-008", billingService.generateBillNumber(VISIT_DATE));
        }
    }

    // =================================================================
    //  What is stored
    // =================================================================
    @Test
    @DisplayName("TC-16 the bill records who took the payment and how")
    void recordsTheCashierAndThePaymentMethod() {
        daoEchoesTheBill();

        BillingService.BillResult result = billingService.generate(
                visit("Scaling", "4500", "1500"),
                BigDecimal.ZERO, PaymentMethod.INSURANCE, cashier);

        Bill bill = result.getBill();
        assertAll(
                () -> assertEquals(PaymentMethod.INSURANCE, bill.getPaymentMethod()),
                () -> assertEquals("Nimali Perera", bill.getBilledBy().getFullName()),
                () -> assertEquals("APT-20260907-001", bill.getAppointment().getAppointmentNo())
        );
    }
}
