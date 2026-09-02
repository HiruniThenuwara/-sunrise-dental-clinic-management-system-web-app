package com.sunrise.model;

/**
 * Whether the bill has been settled.
 */
public enum PaymentStatus {

    PAID("Paid", "success"),
    PENDING("Pending", "warning");

    private final String displayName;
    private final String badgeStyle;

    PaymentStatus(String displayName, String badgeStyle) {
        this.displayName = displayName;
        this.badgeStyle = badgeStyle;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeStyle() {
        return badgeStyle;
    }

    public static PaymentStatus fromString(String value) {
        if (value == null) {
            return PAID;
        }
        try {
            return PaymentStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PAID;
        }
    }
}
