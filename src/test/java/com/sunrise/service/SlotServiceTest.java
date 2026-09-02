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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
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

    /** 07 September 2026 is a Monday. */
    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 7);
    private static final LocalDate TUESDAY = LocalDate.of(2026, 9, 8);
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
}
