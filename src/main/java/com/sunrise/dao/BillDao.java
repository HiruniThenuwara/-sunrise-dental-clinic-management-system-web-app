package com.sunrise.dao;

import com.sunrise.model.Bill;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Data access contract for the {@code bills} table (Requirement 4).
 */
public interface BillDao {

    /**
     * @param bill the bill to store, without an id
     * @return the same object with its generated id and bill number
     */
    Bill insert(Bill bill);

    /**
     * @param billNo for example {@code BILL-20260902-001}
     * @return the bill with its appointment loaded
     */
    Optional<Bill> findByNumber(String billNo);

    /**
     * A visit can only be billed once, so this is how the billing screen
     * knows whether a receipt already exists.
     *
     * @return the bill for that appointment, if one was produced
     */
    Optional<Bill> findByAppointment(int appointmentId);

    /**
     * @param date the day to total
     * @return the money taken that day, never {@code null}
     */
    BigDecimal sumTotalByDate(LocalDate date);

    /**
     * @param date the day being billed
     * @return how many bills already exist on that date, used to build the
     *         next bill number
     */
    int countByDate(LocalDate date);
}
