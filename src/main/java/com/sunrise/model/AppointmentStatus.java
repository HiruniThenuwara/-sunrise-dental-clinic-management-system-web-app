package com.sunrise.model;

/**
 * The life cycle of an appointment.
 *
 * <p>A cancelled appointment keeps its row so the history stays complete,
 * but it releases its time slot, which is why {@link #releasesSlot()} exists
 * rather than deleting the record.</p>
 */
public enum AppointmentStatus {

    BOOKED("Booked", "warning"),
    COMPLETED("Completed", "success"),
    CANCELLED("Cancelled", "danger"),
    NO_SHOW("No Show", "muted");

    private final String displayName;
    private final String badgeStyle;

    AppointmentStatus(String displayName, String badgeStyle) {
        this.displayName = displayName;
        this.badgeStyle = badgeStyle;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** @return the CSS modifier used by the badge in the views */
    public String getBadgeStyle() {
        return badgeStyle;
    }

    /** @return {@code true} when the time slot becomes free again */
    public boolean releasesSlot() {
        return this == CANCELLED;
    }

    /** @return {@code true} when a bill may be produced for the visit */
    public boolean isBillable() {
        return this == COMPLETED || this == BOOKED;
    }

    public static AppointmentStatus fromString(String value) {
        if (value == null) {
            return BOOKED;
        }
        try {
            return AppointmentStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return BOOKED;
        }
    }
}
