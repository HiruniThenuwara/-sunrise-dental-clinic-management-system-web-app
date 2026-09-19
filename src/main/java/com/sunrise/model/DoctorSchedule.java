package com.sunrise.model;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;

/**
 * The hours a dentist works on one weekday.
 *
 * <p>This is the input to the time slot calculation. A Monday from 09:00 to
 * 17:00 with a 30 minute slot length produces sixteen bookable slots, which
 * is what {@link #getSlotCount()} returns.</p>
 *
 * <p>Working hours belong to one dentist only. In the database the foreign
 * key uses {@code ON DELETE CASCADE}, which is the composition relationship
 * shown in the class diagram.</p>
 */
public class DoctorSchedule implements Serializable {

    /** Hours are shown the way the clinic writes them, not as 17:00. */
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("hh:mm a");

    private static final long serialVersionUID = 1L;

    private int scheduleId;
    private int doctorId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private int slotDurationMinutes = 30;
    private boolean active = true;

    public DoctorSchedule() {
        // used when building the object from a ResultSet
    }

    public DoctorSchedule(int doctorId, DayOfWeek dayOfWeek, LocalTime startTime,
                          LocalTime endTime, int slotDurationMinutes) {
        this.doctorId = doctorId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public int getSlotDurationMinutes() {
        return slotDurationMinutes;
    }

    public void setSlotDurationMinutes(int slotDurationMinutes) {
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * How many appointments can be booked on this day.
     *
     * @return the number of whole slots between the start and end time, or
     *         zero if the schedule is incomplete, inactive or the end time is
     *         not after the start time
     */
    public int getSlotCount() {
        if (!active || startTime == null || endTime == null
                || slotDurationMinutes <= 0 || !endTime.isAfter(startTime)) {
            return 0;
        }
        long workingMinutes = Duration.between(startTime, endTime).toMinutes();
        return (int) (workingMinutes / slotDurationMinutes);
    }

    /**
     * @return the weekday shortened for a crowded line, for example "Mon"
     */
    public String getShortDayName() {
        String name = getDayName();
        return name.length() <= 3 ? name : name.substring(0, 3);
    }

    /**
     * @return the working hours as staff read them, for example
     *         "09:00 AM - 05:00 PM", or an empty string when the hours are
     *         not set
     */
    public String getFormattedHours() {
        if (startTime == null || endTime == null) {
            return "";
        }
        return startTime.format(CLOCK) + " - " + endTime.format(CLOCK);
    }

    /** @return the weekday name shown in the interface, for example "Monday" */
    public String getDayName() {
        if (dayOfWeek == null) {
            return "";
        }
        String name = dayOfWeek.name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    @Override
    public String toString() {
        return "DoctorSchedule{doctorId=" + doctorId + ", day=" + dayOfWeek
                + ", " + startTime + "-" + endTime
                + ", slot=" + slotDurationMinutes + "min, slots=" + getSlotCount() + '}';
    }
}
