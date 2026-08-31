package com.sunrise.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Shows the admin panel home page after a successful login.
 *
 * <p>The JSP lives under {@code /WEB-INF/views/}, which the servlet
 * container never serves directly. A user cannot reach it by typing the
 * path in the address bar - the only way in is through this servlet, and
 * therefore through {@code AuthFilter}.</p>
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/admin/dashboard", "/admin/"})
public class DashboardServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String DASHBOARD_VIEW = "/WEB-INF/views/dashboard.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setAttribute("activePage", "dashboard");
        request.setAttribute("pageTitle", "Dashboard");
        request.getRequestDispatcher(DASHBOARD_VIEW).forward(request, response);
    }
}
