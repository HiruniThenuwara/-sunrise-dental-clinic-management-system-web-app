package com.sunrise.model;

/**
 * The things a staff member can do that are worth recording.
 *
 * <p>Using an enum rather than free text means the log can be filtered
 * reliably and a typo cannot create a category of its own. Each value also
 * carries how it should be shown, so the view does not need a lookup table
 * of its own.</p>
 */
public enum ActivityAction {

    // ---------- signing in and out ----------
    LOGIN_SUCCESS("Signed in", "Access", "success"),
    LOGIN_FAILED("Failed sign in", "Access", "danger"),
    LOGOUT("Signed out", "Access", "muted"),

    // ---------- appointments ----------
    APPOINTMENT_CREATED("Registered appointment", "Appointments", "success"),
    APPOINTMENT_COMPLETED("Marked appointment completed", "Appointments", "success"),
    APPOINTMENT_CANCELLED("Cancelled appointment", "Appointments", "warning"),
    APPOINTMENT_REFUSED("Double booking prevented", "Appointments", "danger"),

    // ---------- money ----------
    BILL_CREATED("Produced bill", "Billing", "success"),

    // ---------- clinic setup ----------
    DOCTOR_CREATED("Added dentist", "Clinic", "success"),
    DOCTOR_UPDATED("Updated dentist", "Clinic", "muted"),
    DOCTOR_STATUS("Changed dentist status", "Clinic", "warning"),
    TREATMENT_SAVED("Saved treatment", "Clinic", "muted"),
    TREATMENT_STATUS("Changed treatment status", "Clinic", "warning"),
    SCHEDULE_SAVED("Changed working hours", "Clinic", "muted"),
    SCHEDULE_REMOVED("Removed working day", "Clinic", "warning"),

    // ---------- staff accounts ----------
    STAFF_CREATED("Created staff account", "Staff", "success"),
    STAFF_UPDATED("Updated staff account", "Staff", "muted"),
    STAFF_PASSWORD_RESET("Reset a password", "Staff", "warning"),
    STAFF_STATUS("Changed account access", "Staff", "danger");

    private final String displayName;
    private final String category;
    private final String badgeStyle;

    ActivityAction(String displayName, String category, String badgeStyle) {
        this.displayName = displayName;
        this.category = category;
        this.badgeStyle = badgeStyle;
    }

    /** @return the wording shown in the log table */
    public String getDisplayName() {
        return displayName;
    }

    /** @return the group used by the filter dropdown */
    public String getCategory() {
        return category;
    }

    /** @return the CSS modifier for the badge */
    public String getBadgeStyle() {
        return badgeStyle;
    }

    /**
     * Security relevant entries are highlighted, because those are the ones
     * an administrator is usually looking for.
     *
     * @return {@code true} for failed logins, access changes and prevented
     *         double bookings
     */
    public boolean isNoteworthy() {
        return this == LOGIN_FAILED
                || this == STAFF_STATUS
                || this == STAFF_PASSWORD_RESET
                || this == APPOINTMENT_REFUSED;
    }

    public static ActivityAction fromString(String value) {
        if (value == null) {
            return LOGIN_SUCCESS;
        }
        try {
            return ActivityAction.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return LOGIN_SUCCESS;
        }
    }
}
