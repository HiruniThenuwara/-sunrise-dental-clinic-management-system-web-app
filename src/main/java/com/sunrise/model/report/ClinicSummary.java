package com.sunrise.model.report;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * The headline figures for a reporting period.
 */
public class ClinicSummary {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DecimalFormat WHOLE = new DecimalFormat("#,##0");

    private int totalAppointments;
    private int completed;
    private int cancelled;
    private int newPatients;
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    public int getTotalAppointments() {
        return totalAppointments;
    }

    public void setTotalAppointments(int totalAppointments) {
        this.totalAppointments = totalAppointments;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }

    public int getCancelled() {
        return cancelled;
    }

    public void setCancelled(int cancelled) {
        this.cancelled = cancelled;
    }

    public int getNewPatients() {
        return newPatients;
    }

    public void setNewPatients(int newPatients) {
        this.newPatients = newPatients;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue == null ? BigDecimal.ZERO : totalRevenue;
    }

    /**
     * @return cancellations as a percentage of all bookings, which is the
     *         figure the clinic manager watches
     */
    public String getCancellationRate() {
        if (totalAppointments == 0) {
            return "0.0";
        }
        double rate = (cancelled * 100.0) / totalAppointments;
        return String.format("%.1f", rate);
    }

    public String getFormattedRevenue() {
        return MONEY.format(totalRevenue);
    }

    public String getFormattedAppointments() {
        return WHOLE.format(totalAppointments);
    }
}
