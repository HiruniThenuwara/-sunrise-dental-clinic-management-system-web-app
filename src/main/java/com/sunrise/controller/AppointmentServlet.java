package com.sunrise.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Appointment screens (Requirements 2 and 3).
 *
 * <p>One controller serves the three appointment views, choosing the right
 * one from the path after {@code /admin/appointments}:</p>
 *
 * <ul>
 *   <li>{@code /admin/appointments}       - the list and search screen</li>
 *   <li>{@code /admin/appointments/new}   - the registration form</li>
 *   <li>{@code /admin/appointments/view}  - full details of one appointment</li>
 * </ul>
 */
@WebServlet(name = "AppointmentServlet",
            urlPatterns = {"/admin/appointments", "/admin/appointments/*"})
public class AppointmentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String LIST_VIEW = "/WEB-INF/views/appointments.jsp";
    private static final String FORM_VIEW = "/WEB-INF/views/appointment-form.jsp";
    private static final String DETAILS_VIEW = "/WEB-INF/views/appointment-details.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getPathInfo();   // null, "/new" or "/view"
        String view;

        if ("/new".equals(action)) {
            request.setAttribute("activePage", "new-appointment");
            request.setAttribute("pageTitle", "New Appointment");
            view = FORM_VIEW;

        } else if ("/view".equals(action)) {
            request.setAttribute("activePage", "appointments");
            request.setAttribute("pageTitle", "Appointment Details");
            view = DETAILS_VIEW;

        } else {
            request.setAttribute("activePage", "appointments");
            request.setAttribute("pageTitle", "All Appointments");
            view = LIST_VIEW;
        }

        request.getRequestDispatcher(view).forward(request, response);
    }
}
