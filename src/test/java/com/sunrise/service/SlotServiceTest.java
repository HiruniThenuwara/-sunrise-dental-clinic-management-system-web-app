package com.sunrise.service;

import com.sunrise.dao.AppointmentDao;
import com.sunrise.dao.DoctorScheduleDao;
import com.sunrise.model.DoctorSchedule;
import com.sunrise.model.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SlotService}.
 *
 * <p><b>Test driven development.</b> Written before the implementation, so
 * the first run fails on purpose.</p>
 *
 * <p>Time slots are the heart of the booking screen, and they are the first
 * defence against the double bookings described in the scenario. Both DAOs
 * are Mockito mocks, so these rules are proved without a database.</p>
 *
 * <p>All dates below are Mondays or the days that follow them, chosen so the
 * tests never depend on what day it happens to be when they run.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SlotService - generating bookable time slots")
class SlotServiceTest {

    /**
     * The next Monday, whenever these tests happen to run. A fixed calendar
     * date eventually becomes today, and the past-time rule below would then
     * close the morning slots and break the tests above.
     */
    private static final LocalDate MONDAY =
            LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    private static final LocalDate TUESDAY = MONDAY.plusDays(1);

    /** The day the fixed clock believes it is: 07 September 2026, a Monday. */
    private static final LocalDate CLOCK_DAY = LocalDate.of(2026, 9, 7);

    private static final int DOCTOR_ID = 1;

    @Mock
    private DoctorScheduleDao scheduleDao;

    @Mock
    private AppointmentDao appointmentDao;

    private SlotService slotService;

    @BeforeEach
    void setUp() {
        slotService = new SlotService(scheduleDao, appointmentDao);
    }

    /** Builds a working day for the mock DAO to return. */
    private DoctorSchedule schedule(String start, String end, int slotMinutes) {
        DoctorSchedule schedule = new DoctorSchedule(
                DOCTOR_ID, DayOfWeek.MONDAY,
                LocalTime.parse(start), LocalTime.parse(end), slotMinutes);
        schedule.setActive(true);
        return schedule;
    }

    // -----------------------------------------------------------------
    //  TC-01  the basic calculation
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-01 nine to five in thirty minute slots produces sixteen slots")
    void generatesSixteenSlotsForAnEightHourDay() {
        when(scheduleDao.findByDoctorAndDay(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule("09:00", "17:00", 30)));
        when(appointmentDao.findBookedTimes(DOCTOR_ID, MONDAY)).thenReturn(List.of());

        List<TimeSlot> slots = slotService.generateSlots(DOCTOR_ID, MONDAY);

        assertAll(
                () -> assertEquals(16, slots.size()),
                () -> assertEquals(LocalTime.of(9, 0), slots.get(0).getTime()),
                () -> assertEquals(LocalTime.of(16, 30), slots.get(15).getTime())
        );
    }

    // -----------------------------------------------------------------
    //  TC-02  a different slot length
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-02 forty five minute slots divide the day correctly")
    void respectsTheSlotLength() {
        when(scheduleDao.findByDoctorAndDay(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule("10:00", "13:00", 45)));
        when(appointmentDao.findBookedTimes(DOCTOR_ID, MONDAY)).thenReturn(List.of());

        List<TimeSlot> slots = slotService.generateSlots(DOCTOR_ID, MONDAY);

        assertAll(
                () -> assertEquals(4, slots.size()),
                () -> assertEquals(LocalTime.of(10, 0), slots.get(0).getTime()),
                () -> assertEquals(LocalTime.of(12, 15), slots.get(3).getTime())
        );
    }

    // -----------------------------------------------------------------
    //  TC-03  a visit must finish before closing time
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-03 no slot is offered that would run past closing time")
    void doesNotOfferASlotThatOverrunsTheEndTime() {
        when(scheduleDao.findByDoctorAndDay(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule("09:00", "10:00", 45)));
        when(appointmentDao.findBookedTimes(DOCTOR_ID, MONDAY)).thenReturn(List.of());

        List<TimeSlot> slots = slotService.generateSlots(DOCTOR_ID, MONDAY);

        assertAll(
                () -> assertEquals(1, slots.size(), "09:45 would finish at 10:30, after closing"),
                () -> assertEquals(LocalTime.of(9, 0), slots.get(0).getTime())
        );
    }

    // -----------------------------------------------------------------
    //  TC-04  booked times are shown but cannot be chosen
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-04 a time that is already booked is marked unavailable")
    void marksBookedTimesAsUnavailable() {
        when(scheduleDao.findByDoctorAndDay(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule("09:00", "11:00", 30)));
        when(appointmentDao.findBookedTimes(DOCTOR_ID, MONDAY))
                .thenReturn(List.of(LocalTime.of(9, 0), LocalTime.of(10, 0)));

        List<TimeSlot> slots = slotService.generateSlots(DOCTOR_ID, MONDAY);

        assertAll(
                () -> assertEquals(4, slots.size(), "Booked slots stay in the list, greyed out"),
                () -> assertFalse(slots.get(0).isAvailable(), "09:00 is taken"),
                () -> assertTrue(slots.get(1).isAvailable(), "09:30 is free"),
                () -> assertFalse(slots.get(2).isAvailable(), "10:00 is taken"),
                () -> assertTrue(slots.get(3).isAvailable(), "10:30 is free")
        );
    }

    // -----------------------------------------------------------------
    //  TC-05  the dentist does not work that day
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-05 a day the dentist does not work produces no slots")
    void returnsNothingWhenTheDentistDoesNotWorkThatDay() {
        when(scheduleDao.findByDoctorAndDay(DOCTOR_ID, DayOfWeek.TUESDAY))
                .thenReturn(Optional.empty());

        List<TimeSlot> slots = slotService.generateSlots(DOCTOR_ID, TUESDAY);

        assertAll(
                () -> assertTrue(slots.isEmpty()),
                () -> verify(appointmentDao, never()).findBookedTimes(anyInt(), any())
        );
    }

    // -----------------------------------------------------------------
    //  TC-06  the working day has been switched off
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-06 an inactive working day produces no slots")
    void returnsNothingWhenTheWorkingDayIsInactive() {
        DoctorSchedule inactive = schedule("09:00", "17:00", 30);
        inactive.setActive(false);
        when(scheduleDao.findByDoctorAndDay(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(inactive));

        assertTrue(slotService.generateSlots(DOCTOR_ID, MONDAY).isEmpty());
    }

    // -----------------------------------------------------------------
    //  TC-07  bad data must not crash the booking screen
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-07 an end time before the start time produces no slots")
    void returnsNothingWhenTheHoursAreBackToFront() {
        when(scheduleDao.findByDoctorAndDay(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule("17:00", "09:00", 30)));

        assertTrue(slotService.generateSlots(DOCTOR_ID, MONDAY).isEmpty());
    }

    @Test
    @DisplayName("TC-08 a null date is handled without an exception")
    void returnsNothingForANullDate() {
        List<TimeSlot> slots = slotService.generateSlots(DOCTOR_ID, null);

        assertAll(
                () -> assertTrue(slots.isEmpty()),
                () -> verify(scheduleDao, never()).findByDoctorAndDay(anyInt(), any())
        );
    }

    // -----------------------------------------------------------------
    //  TC-09  the check used before saving an appointment
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-09 a free time is reported as available")
    void reportsAFreeTimeAsAvailable() {
        when(scheduleDao.findByDoctorAndDay(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule("09:00", "11:00", 30)));
        when(appointmentDao.findBookedTimes(DOCTOR_ID, MONDAY))
                .thenReturn(List.of(LocalTime.of(9, 0)));

        assertTrue(slotService.isSlotAvailable(DOCTOR_ID, MONDAY, LocalTime.of(9, 30)));
    }

    @Test
    @DisplayName("TC-10 a booked time is not reported as available")
    void reportsABookedTimeAsUnavailable() {
        when(scheduleDao.findByDoctorAndDay(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule("09:00", "11:00", 30)));
        when(appointmentDao.findBookedTimes(DOCTOR_ID, MONDAY))
                .thenReturn(List.of(LocalTime.of(9, 0)));

        assertFalse(slotService.isSlotAvailable(DOCTOR_ID, MONDAY, LocalTime.of(9, 0)));
    }

    @Test
    @DisplayName("TC-11 a time outside the working hours is not available")
    void reportsATimeOutsideWorkingHoursAsUnavailable() {
        when(scheduleDao.findByDoctorAndDay(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule("09:00", "11:00", 30)));
        when(appointmentDao.findBookedTimes(DOCTOR_ID, MONDAY)).thenReturn(List.of());

        assertFalse(slotService.isSlotAvailable(DOCTOR_ID, MONDAY, LocalTime.of(18, 0)));
    }

    // -----------------------------------------------------------------
    //  TC-12  the count shown on the schedule screen
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-12 counts only the slots that are still free")
    void countsOnlyFreeSlots() {
        when(scheduleDao.findByDoctorAndDay(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule("09:00", "11:00", 30)));
        when(appointmentDao.findBookedTimes(DOCTOR_ID, MONDAY))
                .thenReturn(List.of(LocalTime.of(9, 0), LocalTime.of(10, 30)));

        assertEquals(2, slotService.countAvailable(DOCTOR_ID, MONDAY));
    }

    // =================================================================
    //  Times that have already gone by on today's date.
    //
    //  The receptionist opens the booking screen in the afternoon and
    //  chooses today. This morning's slots are still part of the dentist's
    //  working day, but nobody can be seen at a time that is over. The
    //  clock is fixed below so these rules are proved, rather than
    //  depending on the hour the tests happen to run.
    // =================================================================

    /** A service whose idea of "now" is pinned to CLOCK_DAY at the given time. */
    private SlotService serviceAt(String time) {
        ZoneId zone = ZoneId.systemDefault();
        Clock fixed = Clock.fixed(
                CLOCK_DAY.atTime(LocalTime.parse(time)).atZone(zone).toInstant(), zone);
        return new SlotService(scheduleDao, appointmentDao, fixed);
    }

    // -----------------------------------------------------------------
    //  TC-13  this morning's slots are closed by the afternoon
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-13 at 11:15 today, the morning slots are marked as past")
    void marksTodaysEarlierSlotsAsPast() {
        when(scheduleDao.findByDoctorAndDay(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule("09:00", "13:00", 60)));
        when(appointmentDao.findBookedTimes(DOCTOR_ID, CLOCK_DAY)).thenReturn(List.of());

        List<TimeSlot> slots = serviceAt("11:15").generateSlots(DOCTOR_ID, CLOCK_DAY);

        assertAll(
                () -> assertEquals(4, slots.size()),
                () -> assertTrue(slots.get(0).isPast(), "09:00 has gone"),
                () -> assertTrue(slots.get(1).isPast(), "10:00 has gone"),
                () -> assertTrue(slots.get(2).isPast(), "11:00 has gone"),
                () -> assertFalse(slots.get(0).isAvailable(), "a past slot is not bookable"),
                () -> assertFalse(slots.get(3).isPast(), "12:00 is still to come"),
                () -> assertTrue(slots.get(3).isAvailable(), "12:00 can still be booked")
        );
    }

    // -----------------------------------------------------------------
    //  TC-14  the slot standing on the current minute
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-14 the slot on the current minute counts as gone")
    void treatsTheCurrentMinuteAsPast() {
        when(scheduleDao.findByDoctorAndDay(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule("09:00", "11:00", 60)));
        when(appointmentDao.findBookedTimes(DOCTOR_ID, CLOCK_DAY)).thenReturn(List.of());

        List<TimeSlot> slots = serviceAt("10:00").generateSlots(DOCTOR_ID, CLOCK_DAY);

        assertAll(
                () -> assertTrue(slots.get(0).isPast(), "09:00 has gone"),
                () -> assertTrue(slots.get(1).isPast(), "10:00 is starting now, so it has gone")
        );
    }

    // -----------------------------------------------------------------
    //  TC-15  booked beats past in the explanation
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-15 a booked morning slot still reads as booked, not as past")
    void keepsTheBookedReasonForAnEarlierSlot() {
        when(scheduleDao.findByDoctorAndDay(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule("09:00", "12:00", 60)));
        when(appointmentDao.findBookedTimes(DOCTOR_ID, CLOCK_DAY))
                .thenReturn(List.of(LocalTime.of(9, 0)));

        List<TimeSlot> slots = serviceAt("11:30").generateSlots(DOCTOR_ID, CLOCK_DAY);

        assertAll(
                () -> assertFalse(slots.get(0).isAvailable()),
                () -> assertFalse(slots.get(0).isPast(),
                        "somebody had it, which is the more useful reason"),
                () -> assertEquals("Already booked", slots.get(0).getUnavailableReason()),
                () -> assertEquals("This time has already passed today",
                        slots.get(1).getUnavailableReason())
        );
    }

    // -----------------------------------------------------------------
    //  TC-16  a past time cannot be booked, and is not counted as free
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-16 a time earlier than now today is neither available nor counted")
    void refusesAndDoesNotCountAPastTime() {
        when(scheduleDao.findByDoctorAndDay(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule("09:00", "13:00", 60)));
        when(appointmentDao.findBookedTimes(DOCTOR_ID, CLOCK_DAY)).thenReturn(List.of());

        SlotService afternoon = serviceAt("11:15");

        assertAll(
                () -> assertFalse(afternoon.isSlotAvailable(DOCTOR_ID, CLOCK_DAY, LocalTime.of(9, 0)),
                        "this morning cannot be booked this afternoon"),
                () -> assertTrue(afternoon.isSlotAvailable(DOCTOR_ID, CLOCK_DAY, LocalTime.of(12, 0))),
                () -> assertEquals(1, afternoon.countAvailable(DOCTOR_ID, CLOCK_DAY))
        );
    }

    // -----------------------------------------------------------------
    //  TC-17  only today is affected
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-17 a later date keeps every slot, however late in the day it is")
    void leavesFutureDatesAlone() {
        LocalDate nextWeek = CLOCK_DAY.plusWeeks(1);
        when(scheduleDao.findByDoctorAndDay(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule("09:00", "13:00", 60)));
        when(appointmentDao.findBookedTimes(DOCTOR_ID, nextWeek)).thenReturn(List.of());

        List<TimeSlot> slots = serviceAt("23:30").generateSlots(DOCTOR_ID, nextWeek);

        assertAll(
                () -> assertEquals(4, slots.size()),
                () -> assertTrue(slots.stream().allMatch(TimeSlot::isAvailable),
                        "next week's morning has not happened yet")
        );
    }

    // -----------------------------------------------------------------
    //  TC-18  a date that has been and gone
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-18 every slot on an earlier date is closed, whatever the time of day")
    void closesEverySlotOnAnEarlierDate() {
        LocalDate lastWeek = CLOCK_DAY.minusWeeks(1);
        when(scheduleDao.findByDoctorAndDay(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule("09:00", "13:00", 60)));
        when(appointmentDao.findBookedTimes(DOCTOR_ID, lastWeek)).thenReturn(List.of());

        SlotService earlyMorning = serviceAt("06:00");
        List<TimeSlot> slots = earlyMorning.generateSlots(DOCTOR_ID, lastWeek);

        assertAll(
                () -> assertEquals(4, slots.size(), "the times are still shown"),
                () -> assertTrue(slots.stream().allMatch(TimeSlot::isPast),
                        "last week has gone, even the slots later than 06:00"),
                () -> assertTrue(slots.stream().noneMatch(TimeSlot::isAvailable)),
                () -> assertEquals(0, earlyMorning.countAvailable(DOCTOR_ID, lastWeek))
        );
    }

    // -----------------------------------------------------------------
    //  TC-19  the question the appointment service asks before saving
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-19 hasPassed answers for yesterday, earlier today, later today and tomorrow")
    void answersWhetherAMomentHasPassed() {
        SlotService afternoon = serviceAt("11:15");

        assertAll(
                () -> assertTrue(afternoon.hasPassed(CLOCK_DAY.minusDays(1), LocalTime.of(23, 0)),
                        "any time yesterday has gone"),
                () -> assertTrue(afternoon.hasPassed(CLOCK_DAY, LocalTime.of(9, 0))),
                () -> assertFalse(afternoon.hasPassed(CLOCK_DAY, LocalTime.of(12, 0))),
                () -> assertFalse(afternoon.hasPassed(CLOCK_DAY.plusDays(1), LocalTime.of(9, 0))),
                () -> assertFalse(afternoon.hasPassed(null, LocalTime.of(9, 0))),
                () -> assertFalse(afternoon.hasPassed(CLOCK_DAY, null))
        );
    }
}
