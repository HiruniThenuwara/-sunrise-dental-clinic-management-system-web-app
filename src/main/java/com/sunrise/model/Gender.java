package com.sunrise.model;

/**
 * Patient gender, matching the {@code gender} ENUM column.
 */
public enum Gender {

    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * @param value the text stored in the database, may be {@code null}
     * @return the matching gender, or {@code null} when nothing was recorded
     */
    public static Gender fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Gender.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
