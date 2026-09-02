package com.sunrise.dao.impl;

import com.sunrise.dao.BillDao;
import com.sunrise.dao.DBConnection;
import com.sunrise.model.Appointment;
import com.sunrise.model.Bill;
import com.sunrise.model.PaymentMethod;
import com.sunrise.model.PaymentStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of {@link BillDao}.
 */
public class BillDaoImpl implements BillDao {

    private static final Logger LOGGER = Logger.getLogger(BillDaoImpl.class.getName());

    private static final String COLUMNS =
            "bill_id, bill_no, appointment_id, consultation_fee, treatment_cost, discount, "
            + "tax, total_amount, payment_method, payment_status, billed_at";

    private static final String INSERT =
            "INSERT INTO bills (bill_no, appointment_id, consultation_fee, treatment_cost, "
            + "discount, tax, total_amount, payment_method, payment_status, billed_by) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_NUMBER =
            "SELECT " + COLUMNS + " FROM bills WHERE bill_no = ?";

    private static final String SELECT_BY_APPOINTMENT =
            "SELECT " + COLUMNS + " FROM bills WHERE appointment_id = ?";

    private static final String SUM_BY_DATE =
            "SELECT COALESCE(SUM(total_amount), 0) FROM bills WHERE DATE(billed_at) = ?";

    /**
     * Counts the numbers already issued for a date by matching the number
     * itself, not the {@code billed_at} timestamp.
     *
     * <p>Counting by timestamp looked equivalent but was not: a bill entered
     * for an earlier date, or seeded data whose timestamp is the import time,
     * produced a count of zero and the next number collided with one that
     * already existed. Matching on the number prefix keeps the counter and
     * the number in step by construction.</p>
     */
    private static final String COUNT_BY_DATE =
            "SELECT COUNT(*) FROM bills WHERE bill_no LIKE ?";

    private final DBConnection dbConnection;

    public BillDaoImpl() {
        this(DBConnection.getInstance());
    }

    public BillDaoImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public Bill insert(Bill bill) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, bill.getBillNo());
            statement.setInt(2, bill.getAppointment().getAppointmentId());
            statement.setBigDecimal(3, bill.getConsultationFee());
            statement.setBigDecimal(4, bill.getTreatmentCost());
            statement.setBigDecimal(5, bill.getDiscount());
            statement.setBigDecimal(6, bill.getTax());
            statement.setBigDecimal(7, bill.getTotalAmount());
            statement.setString(8, bill.getPaymentMethod().name());
            statement.setString(9, bill.getPaymentStatus().name());

            if (bill.getBilledBy() == null) {
                statement.setNull(10, java.sql.Types.INTEGER);
            } else {
                statement.setInt(10, bill.getBilledBy().getUserId());
            }

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    bill.setBillId(keys.getInt(1));
                }
            }
            LOGGER.info("Bill stored: " + bill.getBillNo());
            return bill;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not save the bill", e);
            return null;
        }
    }

    @Override
    public Optional<Bill> findByNumber(String billNo) {
        if (billNo == null || billNo.isBlank()) {
            return Optional.empty();
        }
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_NUMBER)) {

            statement.setString(1, billNo.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read the bill", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Bill> findByAppointment(int appointmentId) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_APPOINTMENT)) {

            statement.setInt(1, appointmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not read the bill for the appointment", e);
        }
        return Optional.empty();
    }

    @Override
    public BigDecimal sumTotalByDate(LocalDate date) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SUM_BY_DATE)) {

            statement.setDate(1, Date.valueOf(date));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not total the takings for " + date, e);
            return BigDecimal.ZERO;
        }
    }

    @Override
    public int countByDate(LocalDate date) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT_BY_DATE)) {

            statement.setString(1, "BILL-"
                    + date.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-%");

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not count the bills for " + date, e);
            return 0;
        }
    }

    private Bill mapRow(ResultSet resultSet) throws SQLException {
        Bill bill = new Bill();
        bill.setBillId(resultSet.getInt("bill_id"));
        bill.setBillNo(resultSet.getString("bill_no"));
        bill.setConsultationFee(resultSet.getBigDecimal("consultation_fee"));
        bill.setTreatmentCost(resultSet.getBigDecimal("treatment_cost"));
        bill.setDiscount(resultSet.getBigDecimal("discount"));
        bill.setTax(resultSet.getBigDecimal("tax"));
        bill.setTotalAmount(resultSet.getBigDecimal("total_amount"));
        bill.setPaymentMethod(PaymentMethod.fromString(resultSet.getString("payment_method")));
        bill.setPaymentStatus(PaymentStatus.fromString(resultSet.getString("payment_status")));

        Timestamp billedAt = resultSet.getTimestamp("billed_at");
        if (billedAt != null) {
            bill.setBilledAt(billedAt.toLocalDateTime());
        }

        Appointment appointment = new Appointment();
        appointment.setAppointmentId(resultSet.getInt("appointment_id"));
        bill.setAppointment(appointment);

        return bill;
    }
}
