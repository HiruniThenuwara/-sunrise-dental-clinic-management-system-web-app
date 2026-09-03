package com.sunrise.controller;

import com.sunrise.dao.DaoFactory;
import com.sunrise.model.Appointment;
import com.sunrise.model.AppointmentStatus;
import com.sunrise.model.User;
import com.sunrise.service.AppointmentService;
import com.sunrise.service.AuthService;
import com.sunrise.service.SlotService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Appointment screens (Requirements 2 and 3).
 *
 * <ul>
 *   <li>{@code GET  /admin/appointments}      - list, and search by number</li>
 *   <li>{@code GET  /admin/appointments/new}  - the registration form</li>
 *   <li>{@code POST /admin/appointments/new}  - register the visit</li>
 *   <li>{@code GET  /admin/appointments/view} - full details of one visit</li>
 * </ul>
 *
 * <p>The servlet reads the request and picks a view. Every rule - validation,
 * the double booking check, the appointment number - belongs to
 * {@link AppointmentService}, which is why those rules can be unit tested
 * without a browser.</p>
 */
@WebServlet(name = "AppointmentServlet",
            urlPatterns = {"/admin/appointments", "/admin/appointments/*"})
public class AppointmentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String LIST_VIEW = "/WEB-INF/views/appointments.jsp";
    private static final String FORM_VIEW = "/WEB-INF/views/appointment-form.jsp";
    private static final String DETAILS_VIEW = "/WEB-INF/views/appointment-details.jsp";

    private transient AppointmentService appointmentService;
    private transient SlotService slotService;
    private transient com.sunrise.service.ActivityLogService activityLog;

    @Override
    public void init() {
        this.appointmentService = new AppointmentService();
        this.slotService = new SlotService(
                DaoFactory.getDoctorScheduleDao(), DaoFactory.getAppointmentDao());
        this.activityLog = new com.sunrise.service.ActivityLogService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getPathInfo();   // null, "/new" or "/view"

        if ("/new".equals(action)) {
            showRegistrationForm(request, response);
        } else if ("/view".equals(action)) {
            showDetails(request, response);
        } else {
            showList(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        if ("/status".equals(request.getPathInfo())) {
            changeStatus(request, response);
            return;
        }
        register(request, response);
    }

    // -----------------------------------------------------------------
    //  Requirement 3 - list and search by appointment number
    // -----------------------------------------------------------------
    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String searchNo = request.getParameter("no");
        List<Appointment> appointments;

        if (searchNo != null && !searchNo.isBlank()) {
            Optional<Appointment> found = appointmentService.findByNumber(searchNo);
            appointments = found.map(List::of).orElseGet(List::of);

            request.setAttribute("searchNo", searchNo.trim());
            if (found.isEmpty()) {
                request.setAttribute("flashError",
                        "No appointment found with the number " + searchNo.trim() + ".");
            }
        } else {
            appointments = appointmentService.findRecent(50);
        }

        request.setAttribute("appointments", appointments);
        request.setAttribute("bookedCount", countWithStatus(appointments, AppointmentStatus.BOOKED));
        request.setAttribute("completedCount", countWithStatus(appointments, AppointmentStatus.COMPLETED));
        request.setAttribute("cancelledCount", countWithStatus(appointments, AppointmentStatus.CANCELLED));
        request.setAttribute("activePage", "appointments");
        request.setAttribute("pageTitle", "All Appointments");

        moveFlashMessagesToRequest(request);
        request.getRequestDispatcher(LIST_VIEW).forward(request, response);
    }

    // -----------------------------------------------------------------
    //  Requirement 2 - the registration form
    // -----------------------------------------------------------------
    private void showRegistrationForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        putFormDataOnRequest(request);
        moveFlashMessagesToRequest(request);
        request.getRequestDispatcher(FORM_VIEW).forward(request, response);
    }

    /** Loads the dropdown lists, the free slots and the next number. */
    private void putFormDataOnRequest(HttpServletRequest request) {

        LocalDate chosenDate = parseDate(request.getParameter("date"));
        int chosenDoctorId = parseId(request.getParameter("doctorId"));

        request.setAttribute("doctors", DaoFactory.getDoctorDao().findAllActive());
        request.setAttribute("treatments", DaoFactory.getTreatmentDao().findAllActive());
        request.setAttribute("chosenDoctorId", chosenDoctorId);
        request.setAttribute("chosenDate", chosenDate);
        request.setAttribute("today", LocalDate.now());
        request.setAttribute("nextNumber",
                appointmentService.generateAppointmentNumber(chosenDate));

        if (chosenDoctorId > 0) {
            request.setAttribute("slots", slotService.generateSlots(chosenDoctorId, chosenDate));
        }

        request.setAttribute("activePage", "new-appointment");
        request.setAttribute("pageTitle", "New Appointment");
    }

    /** Stores the visit, or shows the form again with the problems. */
    private void register(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        LocalDate appointmentDate = parseDate(request.getParameter("appointmentDate"));

        AppointmentService.RegistrationResult result = appointmentService.register(
                request.getParameter("patientName"),
                request.getParameter("address"),
                request.getParameter("contactNumber"),
                request.getParameter("email"),
                request.getParameter("nic"),
                request.getParameter("gender"),
                request.getParameter("doctorId"),
                request.getParameter("treatmentId"),
                appointmentDate,
                request.getParameter("appointmentTime"),
                request.getParameter("bookingType"),
                request.getParameter("notes"),
                currentUser(request));

        if (result.isSuccess()) {
            String number = result.getAppointment().getAppointmentNo();

            activityLog.record(request, com.sunrise.model.ActivityAction.APPOINTMENT_CREATED,
                    "Appointment", number,
                    result.getAppointment().getPatient().getPatientName()
                            + " with " + result.getAppointment().getDoctor().getDoctorName()
                            + " on " + result.getAppointment().getFormattedDate()
                            + " at " + result.getAppointment().getFormattedTime());

            request.getSession().setAttribute("flashSuccess",
                    "Appointment " + number + " registered successfully.");
            // Redirect after post, so a refresh cannot book the same visit twice.
            response.sendRedirect(request.getContextPath()
                    + "/admin/appointments/view?no=" + number);
            return;
        }

        // A refused double booking is worth recording: it is evidence the
        // protection is working, and it shows how often it happens.
        boolean slotTaken = result.getErrors().stream()
                .anyMatch(error -> error.toLowerCase().contains("already booked"));
        if (slotTaken) {
            activityLog.record(request, com.sunrise.model.ActivityAction.APPOINTMENT_REFUSED,
                    "Appointment", null,
                    "Attempted booking refused: the dentist is already booked at "
                            + request.getParameter("appointmentTime")
                            + " on " + appointmentDate);
        }

        putFormDataOnRequest(request);
        request.setAttribute("errors", result.getErrors());
        request.setAttribute("formPatientName", request.getParameter("patientName"));
        request.setAttribute("formAddress", request.getParameter("address"));
        request.setAttribute("formContact", request.getParameter("contactNumber"));
        request.setAttribute("formEmail", request.getParameter("email"));
        request.setAttribute("formNic", request.getParameter("nic"));
        request.setAttribute("formGender", request.getParameter("gender"));
        request.setAttribute("formBookingType", request.getParameter("bookingType"));
        request.setAttribute("formNotes", request.getParameter("notes"));
        request.setAttribute("formTreatmentId", parseId(request.getParameter("treatmentId")));
        request.setAttribute("formTime", request.getParameter("appointmentTime"));

        request.getRequestDispatcher(FORM_VIEW).forward(request, response);
    }

    // -----------------------------------------------------------------
    //  Requirement 3 - full details of one visit
    // -----------------------------------------------------------------
    private void showDetails(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Optional<Appointment> found = appointmentService.findByNumber(request.getParameter("no"));

        if (found.isEmpty()) {
            request.getSession().setAttribute("flashError",
                    "No appointment found with that number.");
            response.sendRedirect(request.getContextPath() + "/admin/appointments");
            return;
        }

        Appointment appointment = found.get();
        request.setAttribute("appointment", appointment);
        request.setAttribute("existingBill",
                DaoFactory.getBillDao().findByAppointment(appointment.getAppointmentId())
                        .orElse(null));
        request.setAttribute("activePage", "appointments");
        request.setAttribute("pageTitle", "Appointment Details");

        moveFlashMessagesToRequest(request);
        request.getRequestDispatcher(DETAILS_VIEW).forward(request, response);
    }

    /** Marks a visit completed or cancelled. */
    private void changeStatus(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int appointmentId = parseId(request.getParameter("appointmentId"));
        String number = request.getParameter("no");
        AppointmentStatus status =
                AppointmentStatus.fromString(request.getParameter("status"));

        if (appointmentService.updateStatus(appointmentId, status)) {
            activityLog.record(request,
                    status == AppointmentStatus.CANCELLED
                            ? com.sunrise.model.ActivityAction.APPOINTMENT_CANCELLED
                            : com.sunrise.model.ActivityAction.APPOINTMENT_COMPLETED,
                    "Appointment", number,
                    "Status changed to " + status.getDisplayName());

            request.getSession().setAttribute("flashSuccess",
                    status == AppointmentStatus.CANCELLED
                            ? "Appointment " + number + " was cancelled. "
                              + "The time slot is free for another patient."
                            : "Appointment " + number + " is now marked as "
                              + status.getDisplayName().toLowerCase() + ".");
        } else {
            request.getSession().setAttribute("flashError",
                    "The status could not be changed. Please try again.");
        }

        // Go back to wherever the staff member was. Cancelling from the list
        // returns to the list; cancelling from the details page stays there.
        String returnTo = request.getParameter("returnTo");

        response.sendRedirect("list".equals(returnTo)
                ? request.getContextPath() + "/admin/appointments"
                : request.getContextPath() + "/admin/appointments/view?no=" + number);
    }

    // -----------------------------------------------------------------
    //  helpers
    // -----------------------------------------------------------------

    private long countWithStatus(List<Appointment> appointments, AppointmentStatus status) {
        return appointments.stream().filter(a -> a.getStatus() == status).count();
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object attribute = session.getAttribute(AuthService.SESSION_USER_KEY);
        return (attribute instanceof User) ? (User) attribute : null;
    }

    /** Reads a date from the request, falling back to today. */
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
