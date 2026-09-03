package com.sunrise.service;

import com.sunrise.dao.AppointmentDao;
import com.sunrise.dao.DoctorScheduleDao;
import com.sunrise.model.DoctorSchedule;
import com.sunrise.model.TimeSlot;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Turns a dentist's working hours into the list of bookable times shown on
 * the appointment form.
 *
 * <p>Slots are never stored in the database. They are calculated from
 * {@link DoctorSchedule} every time a date is chosen, for two reasons:</p>
 *
 * <ul>
 *   <li>a change to the working hours takes effect immediately, with no
 *       stored slot rows left behind to contradict it;</li>
 *   <li>there is only one source of truth about when a dentist works, so the
 *       booking screen and the schedule screen can never disagree.</li>
 * </ul>
 *
 * <p>A time is closed for one of two reasons. Somebody has already taken it,
 * or, on today's date, the clock has gone past it: at 14:00 this morning's
 * 09:00 slot is no longer a thing that can happen. Closed times stay in the
 * list rather than disappearing, so the receptionist can see that 09:00
 * exists and why it cannot be used, which is more useful than a gap.</p>
 *
 * <p>This is the <b>first</b> defence against double booking. The service
 * layer checks again before saving, and the {@code UNIQUE} constraint on
 * (dentist, date, time) is the final safety net at database level.</p>
 */
public class SlotService {

    private final DoctorScheduleDao scheduleDao;
    private final AppointmentDao appointmentDao;

    /**
     * Where "now" comes from. Reading the clock through this field rather
     * than calling {@code LocalTime.now()} directly lets the tests pin the
     * time of day, so the past-time rule can be proved instead of being
     * taken on trust.
     */
    private final Clock clock;

    /**
     * Constructor used by the application and by the unit tests that do not
     * care what the time of day is.
     */
    public SlotService(DoctorScheduleDao scheduleDao, AppointmentDao appointmentDao) {
        this(scheduleDao, appointmentDao, Clock.systemDefaultZone());
    }

    /**
     * Constructor used by the tests of the past-time rule, so a fixed clock
     * can be supplied.
     */
    public SlotService(DoctorScheduleDao scheduleDao, AppointmentDao appointmentDao, Clock clock) {
        this.scheduleDao = scheduleDao;
        this.appointmentDao = appointmentDao;
        this.clock = clock;
    }

    /**
     * Builds the list of times a dentist can be booked on a given date.
     *
     * @param doctorId the dentist
     * @param date     the chosen date
     * @return every slot for that day, with booked times and times that have
     *         already gone by marked unavailable, or an empty list when the
     *         dentist does not work that day
     */
    public List<TimeSlot> generateSlots(int doctorId, LocalDate date) {

        if (date == null) {
            return List.of();
        }

        Optional<DoctorSchedule> found =
                scheduleDao.findByDoctorAndDay(doctorId, date.getDayOfWeek());

        if (found.isEmpty()) {
            return List.of();
        }

        DoctorSchedule schedule = found.get();
        List<TimeSlot> slots = buildSlots(schedule);

        if (slots.isEmpty()) {
            return slots;
        }

        markBooked(slots, appointmentDao.findBookedTimes(doctorId, date));
        markPast(slots, date);
        return slots;
    }

    /**
     * Checks one exact time, which is what the appointment service asks
     * before it saves a booking.
     *
     * @return {@code true} when the dentist works then, nobody has that time
     *         already, and the time has not gone by
     */
    public boolean isSlotAvailable(int doctorId, LocalDate date, LocalTime time) {
        if (time == null) {
            return false;
        }
        return generateSlots(doctorId, date).stream()
                .anyMatch(slot -> time.equals(slot.getTime()) && slot.isAvailable());
    }

    /**
     * Says whether a date and time are already behind us.
     *
     * <p>Kept separate from {@link #isSlotAvailable} so the appointment
     * service can tell the receptionist which of the two problems they
     * have. "That time is already booked" is misleading when the real
     * trouble is that it is now the afternoon.</p>
     *
     * @return {@code true} when the moment has passed
     */
    public boolean hasPassed(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            return false;
        }
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today)) {
            return true;
        }
        return date.isEqual(today) && !time.isAfter(LocalTime.now(clock));
    }

    /**
     * @return how many slots are still free on that date, used for the
     *         "6 slots left" figure on the schedule screen
     */
    public int countAvailable(int doctorId, LocalDate date) {
        return (int) generateSlots(doctorId, date).stream()
                .filter(TimeSlot::isAvailable)
                .count();
    }

    /**
     * Divides the working day into slots.
     *
     * <p>A slot is only offered when the whole visit finishes by closing
     * time. With a 45 minute slot length, a day ending at 10:00 offers 09:00
     * but not 09:45, because that visit would run until 10:30.</p>
     */
    private List<TimeSlot> buildSlots(DoctorSchedule schedule) {

        List<TimeSlot> slots = new ArrayList<>();

        if (!schedule.isActive()
                || schedule.getStartTime() == null
                || schedule.getEndTime() == null
                || schedule.getSlotDurationMinutes() <= 0
                || !schedule.getEndTime().isAfter(schedule.getStartTime())) {
            return slots;
        }

        LocalTime slotStart = schedule.getStartTime();
        LocalTime closing = schedule.getEndTime();
        int minutes = schedule.getSlotDurationMinutes();

        while (!slotStart.plusMinutes(minutes).isAfter(closing)) {
            slots.add(new TimeSlot(slotStart));
            slotStart = slotStart.plusMinutes(minutes);
        }
        return slots;
    }

    /** Marks every slot that already has an appointment as unavailable. */
    private void markBooked(List<TimeSlot> slots, List<LocalTime> bookedTimes) {
        if (bookedTimes == null || bookedTimes.isEmpty()) {
            return;
        }
        Set<LocalTime> taken = new HashSet<>(bookedTimes);
        for (TimeSlot slot : slots) {
            if (taken.contains(slot.getTime())) {
                slot.markBooked();
            }
        }
    }

    /**
     * Closes the times that have gone by.
     *
     * <p>On today's date that means the times earlier than now. A slot
     * exactly on the current minute counts as gone: the patient cannot be in
     * the chair at a time that is already starting.</p>
     *
     * <p>On a date before today, every time has gone. The date box on the
     * form will not offer such a date, but a typed web address can still ask
     * for one, and a grid full of green slots for last Thursday would be a
     * lie. Slots that are booked keep the booked marking, because "already
     * booked" is the more useful thing for the receptionist to know.</p>
     */
    private void markPast(List<TimeSlot> slots, LocalDate date) {

        LocalDate today = LocalDate.now(clock);

        if (date.isAfter(today)) {
            return;
        }

        boolean wholeDayGone = date.isBefore(today);
        LocalTime now = LocalTime.now(clock);

        for (TimeSlot slot : slots) {
            if (slot.isAvailable() && (wholeDayGone || !slot.getTime().isAfter(now))) {
                slot.markPast();
            }
        }
    }
}
