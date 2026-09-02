package com.sunrise.model.report;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * One line of a management report.
 *
 * <p>The three reports all have the same shape: a label, a count, an amount
 * and a share of the total. Using one row type keeps the report screen and
 * the PDF simple, because both can render any report with the same loop.</p>
 */
public class ReportRow {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DecimalFormat WHOLE = new DecimalFormat("#,##0");

    private final String label;
    private final String subLabel;
    private final int count;
    private final int secondaryCount;
    private final BigDecimal amount;

    /** Percentage of the largest row, used to size the bars on screen. */
    private int sharePercent;

    public ReportRow(String label, String subLabel, int count,
                     int secondaryCount, BigDecimal amount) {
        this.label = label;
        this.subLabel = subLabel;
        this.count = count;
        this.secondaryCount = secondaryCount;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
    }

    public String getLabel() {
        return label;
    }

    public String getSubLabel() {
        return subLabel;
    }

    public int getCount() {
        return count;
    }

    /** A second figure, such as how many of the visits were completed. */
    public int getSecondaryCount() {
        return secondaryCount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public int getSharePercent() {
        return sharePercent;
    }

    public void setSharePercent(int sharePercent) {
        this.sharePercent = sharePercent;
    }

    /** @return the amount written for the screen, for example 1,420,000.00 */
    public String getFormattedAmount() {
        return MONEY.format(amount);
    }

    /** @return the count written with a thousands separator */
    public String getFormattedCount() {
        return WHOLE.format(count);
    }

    @Override
    public String toString() {
        return label + " count=" + count + " amount=" + amount;
    }
}
