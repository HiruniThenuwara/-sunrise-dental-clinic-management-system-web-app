package com.sunrise.model;

/**
 * How the appointment came to be made.
 *
 * <p>Two cases: the patient walked up to the desk, or the booking was taken
 * remotely, by telephone or through the website. The clinic needs the
 * distinction for planning, because a morning full of walk-ins is a
 * different staffing problem from a morning of slots booked in advance.</p>
 */
public enum BookingType {

    /** The patient came to the desk in person. */
    WALK_IN("Walk in", "muted"),

    /** Booked without the patient being present: by telephone or online. */
    ONLINE("Online", "success");

    private final String displayName;
    private final String badgeStyle;

    BookingType(String displayName, String badgeStyle) {
        this.displayName = displayName;
        this.badgeStyle = badgeStyle;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** @return the CSS modifier for the badge shown beside the appointment */
    public String getBadgeStyle() {
        return badgeStyle;
    }

    /**
     * Walk-in is the fallback, because that is how appointments were taken
     * before the system recorded the difference.
     *
     * @param value the text stored in the database or posted by the form
     * @return the matching type, never {@code null}
     */
    public static BookingType fromString(String value) {
        if (value == null) {
            return WALK_IN;
        }
        try {
            return BookingType.valueOf(value.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return WALK_IN;
        }
    }
}
