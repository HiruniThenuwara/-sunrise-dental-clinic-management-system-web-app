package com.sunrise.controller;

import com.sunrise.model.Role;
import com.sunrise.model.User;
import com.sunrise.service.AuthService;
import com.sunrise.service.StaffService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * Staff account management, for administrators only.
 *
 * <p>This is where receptionist accounts are created and withdrawn. Because
 * it grants access to patient records, the servlet checks the role itself
 * rather than relying on the menu being hidden: hiding a link stops a staff
 * member stumbling onto the page, but it does not stop anyone typing the
 * address.</p>
 */
@WebServlet(name = "StaffServlet", urlPatterns = {"/admin/staff"})
public class StaffServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String VIEW = "/WEB-INF/views/staff.jsp";

    private transient StaffService staffService;

    @Override
    public void init() {
        this.staffService = new StaffService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (refuseNonAdmin(request, response)) {
            return;
        }
        putListOnRequest(request);
        moveFlashMessagesToRequest(request);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (refuseNonAdmin(request, response)) {
            return;
        }
        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");
        int currentUserId = currentUser(request) == null ? 0 : currentUser(request).getUserId();

        StaffService.StaffResult result;

        switch (action == null ? "" : action) {
            case "toggle" -> result = staffService.setActive(
                    parseId(request.getParameter("userId")),
                    "1".equals(request.getParameter("active")),
                    currentUserId);

            case "password" -> result = staffService.resetPassword(
                    parseId(request.getParameter("userId")),
                    request.getParameter("password"),
                    request.getParameter("confirmPassword"));

            case "edit" -> result = staffService.update(
                    parseId(request.getParameter("userId")),
                    request.getParameter("fullName"),
                    request.getParameter("role"),
                    !"0".equals(request.getParameter("status")),
                    currentUserId);

            default -> result = staffService.create(
                    request.getParameter("username"),
                    request.getParameter("fullName"),
                    request.getParameter("role"),
                    request.getParameter("password"),
                    request.getParameter("confirmPassword"),
                    !"0".equals(request.getParameter("status")));
        }

        if (result.isSuccess()) {
            // Anything that changes who can reach patient records is
            // recorded, with the name of the administrator who did it.
            new com.sunrise.service.ActivityLogService().record(request,
                    switch (action == null ? "" : action) {
                        case "toggle" -> com.sunrise.model.ActivityAction.STAFF_STATUS;
                        case "password" -> com.sunrise.model.ActivityAction.STAFF_PASSWORD_RESET;
                        case "edit" -> com.sunrise.model.ActivityAction.STAFF_UPDATED;
                        default -> com.sunrise.model.ActivityAction.STAFF_CREATED;
                    },
                    "Staff account", result.getUser().getUsername(),
                    successMessage(action, result));

            request.getSession().setAttribute("flashSuccess", successMessage(action, result));
            // Redirect after post, so a refresh cannot create the account twice.
            response.sendRedirect(request.getContextPath() + "/admin/staff");
            return;
        }

        putListOnRequest(request);
        request.setAttribute("errors", result.getErrors());
        request.setAttribute("formUsername", request.getParameter("username"));
        request.setAttribute("formFullName", request.getParameter("fullName"));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private String successMessage(String action, StaffService.StaffResult result) {
        String name = result.getUser() == null ? "The account" : result.getUser().getFullName();

        return switch (action == null ? "" : action) {
            case "toggle" -> result.getUser().isActive()
                    ? name + " can sign in again."
                    : name + " can no longer sign in.";
            case "password" -> "The password for " + name + " has been changed.";
            case "edit" -> name + " was updated.";
            default -> name + " was added as a "
                    + result.getUser().getRole().getDisplayName().toLowerCase() + ".";
        };
    }

    /**
     * Sends a non administrator back to the dashboard.
     *
     * @return {@code true} when the request was refused
     */
    private boolean refuseNonAdmin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        User user = currentUser(request);
        if (user != null && user.isAdmin()) {
            return false;
        }
        request.getSession().setAttribute("flashError",
                "Staff accounts can only be managed by an administrator.");
        response.sendRedirect(request.getContextPath() + "/admin/dashboard");
        return true;
    }

    private void putListOnRequest(HttpServletRequest request) {
        List<User> staff = staffService.findAll();

        request.setAttribute("staff", staff);
        request.setAttribute("totalCount", staff.size());
        request.setAttribute("adminCount",
                staff.stream().filter(User::isAdmin).count());
        request.setAttribute("receptionistCount",
                staff.stream().filter(u -> u.getRole() == Role.RECEPTIONIST).count());
        request.setAttribute("activeCount",
                staff.stream().filter(User::isActive).count());
        request.setAttribute("activePage", "staff");
        request.setAttribute("pageTitle", "Staff Accounts");
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object attribute = session.getAttribute(AuthService.SESSION_USER_KEY);
        return (attribute instanceof User) ? (User) attribute : null;
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
