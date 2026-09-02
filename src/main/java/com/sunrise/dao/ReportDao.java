package com.sunrise.dao;

import com.sunrise.model.report.ClinicSummary;
import com.sunrise.model.report.ReportRow;

import java.time.LocalDate;
import java.util.List;

/**
 * Aggregate queries behind the management reports.
 *
 * <p>These are the only queries in the system that use {@code GROUP BY} and
 * {@code SUM}. Doing the totalling in SQL rather than in Java means the
 * database returns a handful of rows instead of thousands, which is what
 * keeps the report screen quick as the clinic's history grows.</p>
 */
public interface ReportDao {

    /**
     * @return the headline figures for the period
     */
    ClinicSummary summaryBetween(LocalDate from, LocalDate to);

    /**
     * @return one row per day, for the bar chart
     */
    List<ReportRow> appointmentsPerDay(LocalDate from, LocalDate to);

    /**
     * @return one row per dentist: appointments, completed and revenue
     */
    List<ReportRow> workloadByDoctor(LocalDate from, LocalDate to);

    /**
     * @return one row per treatment type: how many and how much they earned
     */
    List<ReportRow> revenueByTreatment(LocalDate from, LocalDate to);
}
