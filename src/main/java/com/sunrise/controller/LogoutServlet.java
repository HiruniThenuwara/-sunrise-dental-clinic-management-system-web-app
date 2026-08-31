package com.sunrise.controller;

import com.sunrise.model.User;
import com.sunrise.service.AuthService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Closes the staff session safely (Requirement 6, "Exit System").
 *
 * <p>Logging out does three things, and all three matter. The session is
 * destroyed on the server, the "remember me" cookie is deleted from the
 * browser, and the browser is told not to keep the admin pages in its cache.
 * Without the last step, pressing the Back button on a shared clinic
 * computer could still show the previous user's patient data.</p>
 */
@WebServlet(name = "LogoutServlet", urlPatterns = {"/logout"})
public class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(LogoutServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        if (session != null) {
            Object attribute = session.getAttribute(AuthService.SESSION_USER_KEY);
            if (attribute instanceof User) {
                LOGGER.info("Logout: " + ((User) attribute).getUsername());
            }
            session.invalidate();
        }

        deleteRememberCookie(request, response);
        preventCachedPages(response);

        response.sendRedirect(request.getContextPath() + "/login.jsp?logout=1");
    }

    /** POST is accepted too, so the logout link can be a form button. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        doGet(request, response);
    }

    /** Removes the remember me cookie by sending it back with age zero. */
    private void deleteRememberCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(AuthService.REMEMBER_COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    /** Stops the browser showing protected pages from its cache. */
    private void preventCachedPages(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
