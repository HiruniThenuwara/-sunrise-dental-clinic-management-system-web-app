package com.sunrise.model;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * One bookable time on a dentist's day.
 *
 * <p>Time slots are never stored in the database. They are calculated from
 * the dentist's {@link DoctorSchedule} each time a date is chosen, and any
 * slot that already has an appointment is marked unavailable. Recalculating
 * means a change to the working hours takes effect immediately, with no
 * stored slot rows to keep in step.</p>
 *
 * <p>A slot can be closed for two different reasons, and the screen says
 * which: somebody already has it, or the clock has gone past it. Both are
 * unavailable, but only one of them is worth telephoning the patient
 * about.</p>
 */
public class TimeSlot implements Serializable, Comparable<TimeSlot> {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter VALUE = DateTimeFormatter.ofPattern("HH:mm");

    private LocalTime time;
    private boolean available = true;
    private boolean past;

    public TimeSlot() {
        // no-arg constructor
    }

    public TimeSlot(LocalTime time) {
        this.time = time;
    }

    public TimeSlot(LocalTime time, boolean available) {
        this.time = time;
        this.available = available;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    /**
     * @return {@code true} when this time is earlier today than the moment
     *         the page was drawn, so the visit could never happen
     */
    public boolean isPast() {
        return past;
    }

    /** Marks this slot as taken, so the receptionist cannot choose it. */
    public void markBooked() {
        this.available = false;
    }

    /**
     * Marks this slot as gone by. Only used for today's date: a 09:00 slot
     * cannot be booked at 14:00, even though nobody has taken it.
     */
    public void markPast() {
        this.available = false;
        this.past = true;
    }

    /** @return the 24 hour value sent to the server, for example 09:30 */
    public String getValue() {
        return time == null ? "" : time.format(VALUE);
    }

    /** @return the label shown to the staff member, for example 09:30 AM */
    public String getLabel() {
        return time == null ? "" : time.format(DISPLAY);
    }

    /**
     * @return the short explanation shown when the staff member hovers over
     *         a closed slot, or an empty string when the slot is free
     */
    public String getUnavailableReason() {
        if (available) {
            return "";
        }
        return past ? "This time has already passed today" : "Already booked";
    }

    @Override
    public int compareTo(TimeSlot other) {
        return this.time.compareTo(other.time);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TimeSlot)) {
            return false;
        }
        return Objects.equals(time, ((TimeSlot) other).time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time);
    }

    @Override
    public String toString() {
        if (available) {
            return getValue() + " (free)";
        }
        return getValue() + (past ? " (past)" : " (booked)");
    }
}
