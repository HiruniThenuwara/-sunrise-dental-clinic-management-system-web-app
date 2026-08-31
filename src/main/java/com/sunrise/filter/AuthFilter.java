package com.sunrise.filter;

import com.sunrise.model.User;
import com.sunrise.service.AuthService;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Guards every page under {@code /admin/*} (Requirement 1, "only authorised
 * staff can use the system").
 *
 * <p><b>Design pattern: Front Controller.</b> Instead of repeating a login
 * check at the top of every servlet and every JSP, one filter sits in front
 * of all protected URLs. A new page added later is protected automatically
 * as soon as its URL starts with {@code /admin/}, so a developer cannot
 * forget the check.</p>
 *
 * <p>The filter also handles the "remember me" cookie: if the session has
 * expired but the browser still holds a valid signed cookie, the staff
 * member is logged back in silently instead of being sent to the login
 * screen.</p>
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = {"/admin/*"})
public class AuthFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(AuthFilter.class.getName());

    private AuthService authService;

    @Override
    public void init(FilterConfig filterConfig) {
        this.authService = new AuthService();
        LOGGER.info("AuthFilter started - /admin/* is now protected");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        preventCachedPages(response);

        // 1. Normal case - an active session already exists.
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(AuthService.SESSION_USER_KEY) != null) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Session gone, but the browser may still have a remember me cookie.
        Optional<User> remembered = loginFromCookie(request);
        if (remembered.isPresent()) {
            HttpSession newSession = request.getSession(true);
            newSession.setAttribute(AuthService.SESSION_USER_KEY, remembered.get());
            LOGGER.info("Session restored from remember me cookie for "
                    + remembered.get().getUsername());
            chain.doFilter(request, response);
            return;
        }

        // 3. Not logged in - send to the login page and remember where they
        //    were going, so they land on the right page after logging in.
        String target = request.getRequestURI();
        if (request.getQueryString() != null) {
            target = target + "?" + request.getQueryString();
        }
        request.getSession(true).setAttribute("redirectAfterLogin", target);

        response.sendRedirect(request.getContextPath() + "/login.jsp?timeout=1");
    }

    @Override
    public void destroy() {
        this.authService = null;
    }

    /** Reads and verifies the remember me cookie, if the browser sent one. */
    private Optional<User> loginFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (AuthService.REMEMBER_COOKIE_NAME.equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return authService.validateRememberToken(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    /** Protected pages must never be served from the browser cache. */
    private void preventCachedPages(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
