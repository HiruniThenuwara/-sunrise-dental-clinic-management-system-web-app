package com.sunrise.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Bill calculation and printable receipt.
 *
 * <p>Day 2 version: forwards to the view so the interface can be reviewed
 * and tested. The database work is added on Day 3.</p>
 */
@WebServlet(name = "BillingServlet", urlPatterns = {"/admin/billing"})
public class BillingServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setAttribute("activePage", "billing");
        request.setAttribute("pageTitle", "Billing");
        request.getRequestDispatcher("/WEB-INF/views/billing.jsp").forward(request, response);
    }
}
