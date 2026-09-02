package com.sunrise.controller;

import com.sunrise.dao.DaoFactory;
import com.sunrise.model.Doctor;
import com.sunrise.model.DoctorSchedule;
import com.sunrise.service.SlotService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Working hours and the time slots they produce.
 *
 * <p>This screen is the source of every bookable time in the system. The
 * hours entered here are what {@link SlotService} divides into slots on the
 * appointment form, which is why only an administrator may change them: a
 * mistake here would silently remove a dentist's availability for a whole
 * day.</p>
 */
@WebServlet(name = "ScheduleServlet", urlPatterns = {"/admin/schedule"})
public class ScheduleServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String VIEW = "/WEB-INF/views/schedule.jsp";

    private transient SlotService slotService;

    @Override
    public void init() {
        this.slotService = new SlotService(
                DaoFactory.getDoctorScheduleDao(), DaoFactory.getAppointmentDao());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        putScheduleOnRequest(request);
        moveFlashMessagesToRequest(request);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    /** Saves the hours for one weekday, or removes that day. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        request.setCharacterEncoding("UTF-8");

        int doctorId = parseId(request.getParameter("doctorId"));
        DayOfWeek day = parseDay(request.getParameter("dayOfWeek"));

        if (doctorId <= 0 || day == null) {
            request.getSession().setAttribute("flashError",
                    "Choose a dentist and a day before saving.");
            response.sendRedirect(request.getContextPath() + "/admin/schedule");
            return;
        }

        boolean working = "1".equals(request.getParameter("working"));

        if (!working) {
            DaoFactory.getDoctorScheduleDao().delete(doctorId, day);
            request.getSession().setAttribute("flashSuccess",
                    "The dentist no longer works on " + friendly(day) + ".");
            response.sendRedirect(request.getContextPath() + "/admin/schedule?doctorId=" + doctorId);
            return;
        }

        LocalTime start = parseTime(request.getParameter("startTime"));
        LocalTime end = parseTime(request.getParameter("endTime"));
        int slotMinutes = parseId(request.getParameter("slotDuration"));

        if (start == null || end == null || !end.isAfter(start) || slotMinutes <= 0) {
            request.getSession().setAttribute("flashError",
                    "The finish time must be later than the start time.");
            response.sendRedirect(request.getContextPath() + "/admin/schedule?doctorId=" + doctorId);
            return;
        }

        DoctorSchedule schedule = new DoctorSchedule(doctorId, day, start, end, slotMinutes);
        schedule.setActive(true);

        if (DaoFactory.getDoctorScheduleDao().save(schedule)) {
            request.getSession().setAttribute("flashSuccess",
                    friendly(day) + " updated. That day now offers "
                            + schedule.getSlotCount() + " appointment slots.");
        } else {
            request.getSession().setAttribute("flashError",
                    "The working hours could not be saved.");
        }

        response.sendRedirect(request.getContextPath() + "/admin/schedule?doctorId=" + doctorId);
    }

    /** Loads the dentist list, their week, and a preview of one day's slots. */
    private void putScheduleOnRequest(HttpServletRequest request) {

        List<Doctor> doctors = DaoFactory.getDoctorDao().findAll();

        int doctorId = parseId(request.getParameter("doctorId"));
        if (doctorId <= 0 && !doctors.isEmpty()) {
            doctorId = doctors.get(0).getDoctorId();
        }

        // One entry per weekday, so the view can render a full week whether
        // or not the dentist works that day.
        Map<String, DoctorSchedule> week = new LinkedHashMap<>();
        Map<String, String> dayLabels = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            week.put(day.name(), null);
            dayLabels.put(day.name(), friendly(day));
        }
        request.setAttribute("dayLabels", dayLabels);
        for (DoctorSchedule schedule : DaoFactory.getDoctorScheduleDao().findByDoctor(doctorId)) {
            week.put(schedule.getDayOfWeek().name(), schedule);
        }

        LocalDate previewDate = parseDate(request.getParameter("date"));

        request.setAttribute("doctors", doctors);
        request.setAttribute("chosenDoctorId", doctorId);
        request.setAttribute("week", week);
        request.setAttribute("previewDate", previewDate);
        request.setAttribute("today", LocalDate.now());
        request.setAttribute("slots", slotService.generateSlots(doctorId, previewDate));
        request.setAttribute("freeCount", slotService.countAvailable(doctorId, previewDate));
        request.setAttribute("activePage", "schedule");
        request.setAttribute("pageTitle", "Schedule & Slots");
    }

    private String friendly(DayOfWeek day) {
        String name = day.name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private DayOfWeek parseDay(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return DayOfWeek.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

    private int parseId(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void moveFlashMessagesToRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        for (String key : new String[]{"flashSuccess", "flashError"}) {
            Object message = session.getAttribute(key);
            if (message != null) {
                request.setAttribute(key, message);
                session.removeAttribute(key);
            }
        }
    }
}
