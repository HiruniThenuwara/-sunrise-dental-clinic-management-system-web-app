package com.sunrise.model;

/**
 * The access levels a staff member can have.
 *
 * <p>Matches the {@code role} ENUM column in the {@code users} table.
 * Using an enum instead of a plain String means an invalid role can never
 * be created by mistake - the compiler checks it.</p>
 */
public enum Role {

    /** Full access, including doctor management and reports. */
    ADMIN("Administrator"),

    /** Can register appointments, search them and print bills. */
    RECEPTIONIST("Receptionist");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    /** @return the label shown in the user interface */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converts the database value into a {@code Role}, falling back to
     * {@code RECEPTIONIST} (the lowest privilege) if the value is unknown.
     *
     * @param value the text stored in the database
     * @return the matching role, never {@code null}
     */
    public static Role fromString(String value) {
        if (value == null) {
            return RECEPTIONIST;
        }
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RECEPTIONIST;
        }
    }
}
