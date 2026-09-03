package com.sunrise.controller;

import com.sunrise.model.ActivityAction;
import com.sunrise.model.User;
import com.sunrise.service.ActivityLogService;
import com.sunrise.service.AuthService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * The activity log, for administrators only.
 *
 * <p>Shows who did what and when, with filters for staff member, action and
 * date range. The clinic holds patient records, so being able to answer
 * "who changed this?" after the fact is part of handling that data
 * responsibly rather than an optional extra.</p>
 *
 * <p>The role is checked here, not merely by hiding the menu item. Hiding a
 * link stops someone stumbling onto the page; it does not stop anyone typing
 * the address.</p>
 */
@WebServlet(name = "ActivityLogServlet", urlPatterns = {"/admin/activity"})
public class ActivityLogServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String VIEW = "/WEB-INF/views/activity.jsp";

    private transient ActivityLogService activityLogService;

    @Override
    public void init() {
        this.activityLogService = new ActivityLogService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        if (user == null || !user.isAdmin()) {
            request.getSession().setAttribute("flashError",
                    "The activity log can only be viewed by an administrator.");
            response.sendRedirect(request.getContextPath() + "/admin/dashboard");
            return;
        }

        String username = request.getParameter("username");
        String action = request.getParameter("action");
        LocalDate from = parseDate(request.getParameter("from"), null);
        LocalDate to = parseDate(request.getParameter("to"), null);

        request.setAttribute("entries", activityLogService.search(username, action, from, to));
        request.setAttribute("knownUsernames", activityLogService.knownUsernames());
        request.setAttribute("allActions", ActivityAction.values());

        request.setAttribute("filterUsername", username);
        request.setAttribute("filterAction", action);
        request.setAttribute("filterFrom", from);
        request.setAttribute("filterTo", to);

        request.setAttribute("countToday", activityLogService.countToday());
        request.setAttribute("failedLogins", activityLogService.failedLoginsThisWeek());
        request.setAttribute("bookingsThisWeek", activityLogService.countAction(
                ActivityAction.APPOINTMENT_CREATED, LocalDate.now().minusDays(7)));
        request.setAttribute("refusedBookings", activityLogService.countAction(
                ActivityAction.APPOINTMENT_REFUSED, LocalDate.now().minusDays(7)));

        request.setAttribute("activePage", "activity");
        request.setAttribute("pageTitle", "Activity Log");

        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object attribute = session.getAttribute(AuthService.SESSION_USER_KEY);
        return (attribute instanceof User) ? (User) attribute : null;
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }
}
