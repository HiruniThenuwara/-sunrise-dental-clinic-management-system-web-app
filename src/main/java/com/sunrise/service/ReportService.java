package com.sunrise.service;

import com.sunrise.dao.DaoFactory;
import com.sunrise.dao.ReportDao;
import com.sunrise.model.report.ClinicSummary;
import com.sunrise.model.report.ReportRow;

import java.time.LocalDate;
import java.util.List;

/**
 * The management reports (Task B, "reports that add value to the system").
 *
 * <p>Three reports were chosen because each answers a question the clinic
 * manager actually asks:</p>
 *
 * <ul>
 *   <li><b>Daily appointments</b> - how busy are we, and on which days? This
 *       is what tells the manager whether a second dentist is needed on a
 *       particular weekday, which is the direct answer to the long waiting
 *       times in the scenario.</li>
 *   <li><b>Dentist workload</b> - who is carrying the load, and what do they
 *       bring in? Useful when deciding hours and hiring.</li>
 *   <li><b>Revenue by treatment</b> - where does the income come from? The
 *       infrequent, expensive procedures usually dominate, which is not
 *       obvious from the appointment book alone.</li>
 * </ul>
 */
public class ReportService {

    private final ReportDao reportDao;

    public ReportService() {
        this(DaoFactory.getReportDao());
    }

    public ReportService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    /** @return the headline figures for the period */
    public ClinicSummary summary(LocalDate from, LocalDate to) {
        return reportDao.summaryBetween(safeFrom(from, to), safeTo(from, to));
    }

    /** @return one row per day, for the bar chart */
    public List<ReportRow> appointmentsPerDay(LocalDate from, LocalDate to) {
        return reportDao.appointmentsPerDay(safeFrom(from, to), safeTo(from, to));
    }

    /** @return one row per dentist */
    public List<ReportRow> workloadByDoctor(LocalDate from, LocalDate to) {
        return reportDao.workloadByDoctor(safeFrom(from, to), safeTo(from, to));
    }

    /** @return one row per treatment type */
    public List<ReportRow> revenueByTreatment(LocalDate from, LocalDate to) {
        return reportDao.revenueByTreatment(safeFrom(from, to), safeTo(from, to));
    }

    /**
     * A missing start date defaults to the first of the current month, and a
     * range entered backwards is swapped rather than returning nothing, which
     * is easier for the user than an error message.
     */
    private LocalDate safeFrom(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        return start.isAfter(end) ? end : start;
    }

    private LocalDate safeTo(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        return start.isAfter(end) ? start : end;
    }
}
