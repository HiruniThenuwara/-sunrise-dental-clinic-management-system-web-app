package com.sunrise.service;

import com.sunrise.dao.AppointmentDao;
import com.sunrise.dao.DoctorScheduleDao;
import com.sunrise.model.DoctorSchedule;
import com.sunrise.model.TimeSlot;

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
 * <p>A time that is already taken stays in the list but is marked
 * unavailable, rather than disappearing. The receptionist can then see that
 * 09:00 exists and is simply taken, which is more useful than a gap.</p>
 *
 * <p>This is the <b>first</b> defence against double booking. The service
 * layer checks again before saving, and the {@code UNIQUE} constraint on
 * (dentist, date, time) is the final safety net at database level.</p>
 */
public class SlotService {

    private final DoctorScheduleDao scheduleDao;
    private final AppointmentDao appointmentDao;

    /**
     * Constructor used by the unit tests, so mock DAOs can be supplied.
     */
    public SlotService(DoctorScheduleDao scheduleDao, AppointmentDao appointmentDao) {
        this.scheduleDao = scheduleDao;
        this.appointmentDao = appointmentDao;
    }

    /**
     * Builds the list of times a dentist can be booked on a given date.
     *
     * @param doctorId the dentist
     * @param date     the chosen date
     * @return every slot for that day with booked times marked unavailable,
     *         or an empty list when the dentist does not work that day
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
        return slots;
    }

    /**
     * Checks one exact time, which is what the appointment service asks
     * before it saves a booking.
     *
     * @return {@code true} when the dentist works then and nobody has that
     *         time already
     */
    public boolean isSlotAvailable(int doctorId, LocalDate date, LocalTime time) {
        if (time == null) {
            return false;
        }
        return generateSlots(doctorId, date).stream()
                .anyMatch(slot -> time.equals(slot.getTime()) && slot.isAvailable());
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
}
