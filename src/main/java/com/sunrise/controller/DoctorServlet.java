package com.sunrise.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Dentist management screen.
 */
@WebServlet(name = "DoctorServlet", urlPatterns = {"/admin/doctors"})
public class DoctorServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setAttribute("activePage", "doctors");
        request.setAttribute("pageTitle", "Dentists");
        request.getRequestDispatcher("/WEB-INF/views/doctors.jsp").forward(request, response);
    }
}
