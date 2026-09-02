package com.sunrise.model;

/**
 * How the patient paid the bill.
 */
public enum PaymentMethod {

    CASH("Cash"),
    CARD("Card"),
    INSURANCE("Insurance");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PaymentMethod fromString(String value) {
        if (value == null) {
            return CASH;
        }
        try {
            return PaymentMethod.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CASH;
        }
    }
}
