package com.sunrise.controller;

import com.sunrise.model.User;
import com.sunrise.service.AuthService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Handles the staff login screen (Requirement 1).
 *
 * <p>This is the CONTROLLER part of MVC. It contains no business rules and
 * no SQL: it only reads the request, asks {@link AuthService} to decide,
 * and then chooses the next view. The login rules themselves live in the
 * service class, which is why they can be unit tested without a browser.</p>
 *
 * <ul>
 *   <li>{@code GET  /login} - shows the login form</li>
 *   <li>{@code POST /login} - checks the credentials</li>
 * </ul>
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

    private static final String LOGIN_VIEW = "/login.jsp";
    private static final String DASHBOARD = "/admin/dashboard";

    private transient AuthService authService;

    @Override
    public void init() {
        this.authService = new AuthService();
    }

    /** Shows the login form, or skips it if the user is already logged in. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(AuthService.SESSION_USER_KEY) != null) {
            response.sendRedirect(request.getContextPath() + DASHBOARD);
            return;
        }
        request.getRequestDispatcher(LOGIN_VIEW).forward(request, response);
    }

    /** Validates the submitted credentials. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        boolean rememberMe = "on".equalsIgnoreCase(request.getParameter("remember"));

        Optional<User> authenticated = authService.authenticate(username, password);

        if (authenticated.isEmpty()) {
            // One generic message for every failure reason. Telling the user
            // "no such username" would let an attacker discover valid accounts.
            LOGGER.warning("Failed login attempt for username: " + username);
            request.setAttribute("error", "Invalid username or password. Please try again.");
            request.setAttribute("username", username);
            request.getRequestDispatcher(LOGIN_VIEW).forward(request, response);
            return;
        }

        User user = authenticated.get();

        // A new session id after login prevents session fixation attacks.
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(AuthService.SESSION_USER_KEY, user);
        session.setMaxInactiveInterval(30 * 60);

        addOrClearRememberCookie(request, response, user, rememberMe);

        LOGGER.info("Login successful for user: " + user.getUsername());
        response.sendRedirect(request.getContextPath() + DASHBOARD);
    }

    /**
     * Writes the "remember me" cookie when the box is ticked, or removes any
     * previous one when it is not.
     */
    private void addOrClearRememberCookie(HttpServletRequest request,
                                          HttpServletResponse response,
                                          User user,
                                          boolean rememberMe) {

        Cookie cookie = new Cookie(AuthService.REMEMBER_COOKIE_NAME,
                rememberMe ? authService.createRememberToken(user) : "");

        cookie.setMaxAge(rememberMe ? AuthService.REMEMBER_COOKIE_MAX_AGE : 0);
        cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        // JavaScript cannot read the cookie, which protects it from XSS theft.
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }
}
