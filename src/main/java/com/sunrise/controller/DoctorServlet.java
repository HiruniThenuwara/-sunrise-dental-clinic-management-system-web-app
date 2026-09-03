package com.sunrise.controller;

import com.sunrise.model.Doctor;
import com.sunrise.service.DoctorService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Dentist management screen (add, edit, activate and deactivate).
 *
 * <p>The servlet contains no SQL and no business rules. It reads the request,
 * hands the values to {@link DoctorService} and chooses what to show next.
 * After a successful save it redirects rather than forwarding, so refreshing
 * the browser cannot store the same dentist twice.</p>
 */
@WebServlet(name = "DoctorServlet", urlPatterns = {"/admin/doctors"})
public class DoctorServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String VIEW = "/WEB-INF/views/doctors.jsp";

    private transient DoctorService doctorService;

    @Override
    public void init() {
        this.doctorService = new DoctorService();
    }

    /** Lists the dentists. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        putListOnRequest(request);
        moveFlashMessagesToRequest(request);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    /** Handles the add, edit and status change actions. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        if ("toggle".equals(request.getParameter("action"))) {
            handleStatusChange(request);
            response.sendRedirect(request.getContextPath() + "/admin/doctors");
            return;
        }
        handleSave(request, response);
    }

    /** Adds a new dentist or updates an existing one. */
    private void handleSave(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int doctorId = parseId(request.getParameter("doctorId"));

        DoctorService.SaveResult result = doctorService.save(
                doctorId,
                request.getParameter("doctorName"),
                request.getParameter("specialization"),
                request.getParameter("contactNumber"),
                request.getParameter("email"),
                request.getParameter("consultationFee"),
                !"0".equals(request.getParameter("status")));

        if (result.isSuccess()) {
            new com.sunrise.service.ActivityLogService().record(request,
                    result.isNewRecord()
                            ? com.sunrise.model.ActivityAction.DOCTOR_CREATED
                            : com.sunrise.model.ActivityAction.DOCTOR_UPDATED,
                    "Dentist", result.getDoctor().getDoctorName(),
                    result.getSuccessMessage() + " Consultation fee LKR "
                            + result.getDoctor().getFormattedFee());

            // Redirect after post, so a browser refresh cannot save twice.
            request.getSession().setAttribute("flashSuccess", result.getSuccessMessage());
            response.sendRedirect(request.getContextPath() + "/admin/doctors");
            return;
        }

        // Validation failed. Show the list again with the errors and the
        // values the staff member typed, so nothing has to be retyped.
        putListOnRequest(request);
        request.setAttribute("errors", result.getErrors());
        request.setAttribute("formDoctorId", doctorId);
        request.setAttribute("formName", request.getParameter("doctorName"));
        request.setAttribute("formSpecialization", request.getParameter("specialization"));
        request.setAttribute("formContact", request.getParameter("contactNumber"));
        request.setAttribute("formEmail", request.getParameter("email"));
        request.setAttribute("formFee", request.getParameter("consultationFee"));

        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    /** Activates or deactivates a dentist. */
    private void handleStatusChange(HttpServletRequest request) {
        int doctorId = parseId(request.getParameter("doctorId"));
        boolean makeActive = "1".equals(request.getParameter("active"));

        if (doctorService.setActive(doctorId, makeActive)) {
            request.getSession().setAttribute("flashSuccess",
                    makeActive
                            ? "The dentist can accept bookings again."
                            : "The dentist is no longer offered for new bookings.");
        } else {
            request.getSession().setAttribute("flashError",
                    "The status could not be changed. Please try again.");
        }
    }

    /** Loads the dentist list and the statistic card values. */
    private void putListOnRequest(HttpServletRequest request) {
        List<Doctor> doctors = doctorService.findAll();

        request.setAttribute("doctors", doctors);
        request.setAttribute("activeCount", doctorService.countActive());
        request.setAttribute("totalCount", doctors.size());
        request.setAttribute("highestFeeText",
                new java.text.DecimalFormat("#,##0").format(highestFee(doctors)));
        request.setAttribute("activePage", "doctors");
        request.setAttribute("pageTitle", "Dentists Management");
    }

    /** @return the highest consultation fee, for the statistic card */
    private BigDecimal highestFee(List<Doctor> doctors) {
        return doctors.stream()
                .map(Doctor::getConsultationFee)
                .filter(fee -> fee != null)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Moves a message left by the previous request into this one, so it is
     * shown exactly once and does not reappear when the page is refreshed.
     */
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
}
