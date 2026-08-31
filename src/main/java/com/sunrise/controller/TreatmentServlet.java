package com.sunrise.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Treatment type management.
 *
 * <p>Every appointment must name a treatment type, and the bill is calculated
 * from that treatment's cost plus the dentist's consultation fee. The clinic
 * therefore needs a screen to add treatments and change their prices without
 * a developer editing the database by hand.</p>
 */
@WebServlet(name = "TreatmentServlet", urlPatterns = {"/admin/treatments"})
public class TreatmentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setAttribute("activePage", "treatments");
        request.setAttribute("pageTitle", "Treatments");
        request.getRequestDispatcher("/WEB-INF/views/treatments.jsp").forward(request, response);
    }
}
