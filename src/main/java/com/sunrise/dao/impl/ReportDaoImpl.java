package com.sunrise.dao.impl;

import com.sunrise.dao.DBConnection;
import com.sunrise.dao.ReportDao;
import com.sunrise.model.report.ClinicSummary;
import com.sunrise.model.report.ReportRow;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of {@link ReportDao}.
 *
 * <p>Revenue is joined from the {@code bills} table rather than calculated
 * from the treatment prices. A bill records what the patient actually paid,
 * including any discount, and it keeps the price that applied on the day, so
 * a later price change cannot rewrite last month's takings.</p>
 */
public class ReportDaoImpl implements ReportDao {

    private static final Logger LOGGER = Logger.getLogger(ReportDaoImpl.class.getName());

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd MMM");

    private static final String SUMMARY =
            "SELECT COUNT(*) AS total, "
            + "SUM(status = 'COMPLETED') AS completed, "
            + "SUM(status = 'CANCELLED') AS cancelled "
            + "FROM appointments WHERE appointment_date BETWEEN ? AND ?";

    private static final String REVENUE_TOTAL =
            "SELECT COALESCE(SUM(b.total_amount), 0) FROM bills b "
            + "JOIN appointments a ON a.appointment_id = b.appointment_id "
            + "WHERE a.appointment_date BETWEEN ? AND ?";

    private static final String NEW_PATIENTS =
            "SELECT COUNT(*) FROM patients WHERE DATE(created_at) BETWEEN ? AND ?";

    private static final String PER_DAY =
            "SELECT appointment_date, COUNT(*) AS total, "
            + "SUM(status = 'COMPLETED') AS completed "
            + "FROM appointments WHERE appointment_date BETWEEN ? AND ? "
            + "GROUP BY appointment_date ORDER BY appointment_date";

    private static final String BY_DOCTOR =
            "SELECT d.doctor_name, d.specialization, COUNT(a.appointment_id) AS total, "
            + "SUM(a.status = 'COMPLETED') AS completed, "
            + "COALESCE(SUM(b.total_amount), 0) AS revenue "
            + "FROM doctors d "
            + "LEFT JOIN appointments a ON a.doctor_id = d.doctor_id "
            + "     AND a.appointment_date BETWEEN ? AND ? "
            + "LEFT JOIN bills b ON b.appointment_id = a.appointment_id "
            + "GROUP BY d.doctor_id, d.doctor_name, d.specialization "
            + "ORDER BY revenue DESC, total DESC";

    private static final String BY_TREATMENT =
            "SELECT t.treatment_name, t.base_cost, COUNT(a.appointment_id) AS total, "
            + "COALESCE(SUM(b.total_amount), 0) AS revenue "
            + "FROM treatments t "
            + "JOIN appointments a ON a.treatment_id = t.treatment_id "
            + "     AND a.appointment_date BETWEEN ? AND ? "
            + "LEFT JOIN bills b ON b.appointment_id = a.appointment_id "
            + "GROUP BY t.treatment_id, t.treatment_name, t.base_cost "
            + "HAVING total > 0 "
            + "ORDER BY revenue DESC, total DESC";

    private final DBConnection dbConnection;

    public ReportDaoImpl() {
        this(DBConnection.getInstance());
    }

    public ReportDaoImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public ClinicSummary summaryBetween(LocalDate from, LocalDate to) {
        ClinicSummary summary = new ClinicSummary();

        try (Connection connection = dbConnection.getConnection()) {

            try (PreparedStatement statement = connection.prepareStatement(SUMMARY)) {
                statement.setDate(1, Date.valueOf(from));
                statement.setDate(2, Date.valueOf(to));
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        summary.setTotalAppointments(resultSet.getInt("total"));
                        summary.setCompleted(resultSet.getInt("completed"));
                        summary.setCancelled(resultSet.getInt("cancelled"));
                    }
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(REVENUE_TOTAL)) {
                statement.setDate(1, Date.valueOf(from));
                statement.setDate(2, Date.valueOf(to));
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        summary.setTotalRevenue(resultSet.getBigDecimal(1));
                    }
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(NEW_PATIENTS)) {
                statement.setDate(1, Date.valueOf(from));
                statement.setDate(2, Date.valueOf(to));
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        summary.setNewPatients(resultSet.getInt(1));
                    }
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not build the report summary", e);
        }
        return summary;
    }

    @Override
    public List<ReportRow> appointmentsPerDay(LocalDate from, LocalDate to) {
        List<ReportRow> rows = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(PER_DAY)) {

            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    LocalDate day = resultSet.getDate("appointment_date").toLocalDate();
                    rows.add(new ReportRow(
                            day.format(DAY_LABEL),
                            day.getDayOfWeek().name().substring(0, 3),
                            resultSet.getInt("total"),
                            resultSet.getInt("completed"),
                            BigDecimal.ZERO));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not build the daily appointment report", e);
        }
        return withShares(rows, true);
    }

    @Override
    public List<ReportRow> workloadByDoctor(LocalDate from, LocalDate to) {
        List<ReportRow> rows = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(BY_DOCTOR)) {

            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new ReportRow(
                            resultSet.getString("doctor_name"),
                            resultSet.getString("specialization"),
                            resultSet.getInt("total"),
                            resultSet.getInt("completed"),
                            resultSet.getBigDecimal("revenue")));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not build the dentist workload report", e);
        }
        return withShares(rows, false);
    }

    @Override
    public List<ReportRow> revenueByTreatment(LocalDate from, LocalDate to) {
        List<ReportRow> rows = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(BY_TREATMENT)) {

            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new ReportRow(
                            resultSet.getString("treatment_name"),
                            "Unit price " + resultSet.getBigDecimal("base_cost").toPlainString(),
                            resultSet.getInt("total"),
                            0,
                            resultSet.getBigDecimal("revenue")));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not build the treatment revenue report", e);
        }
        return withShares(rows, false);
    }

    /**
     * Works out each row's share of the largest row, so the screen can size
     * the bars without doing arithmetic in the JSP.
     *
     * @param byCount {@code true} to compare counts, {@code false} to compare
     *                money
     */
    private List<ReportRow> withShares(List<ReportRow> rows, boolean byCount) {
        if (rows.isEmpty()) {
            return rows;
        }

        double largest = rows.stream()
                .mapToDouble(row -> byCount ? row.getCount() : row.getAmount().doubleValue())
                .max()
                .orElse(0);

        if (largest <= 0) {
            return rows;
        }

        for (ReportRow row : rows) {
            double value = byCount ? row.getCount() : row.getAmount().doubleValue();
            row.setSharePercent((int) Math.round((value / largest) * 100));
        }
        return rows;
    }
}
