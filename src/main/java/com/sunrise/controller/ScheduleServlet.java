package com.sunrise.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Doctor working hours and generated time slots.
 */
@WebServlet(name = "ScheduleServlet", urlPatterns = {"/admin/schedule"})
public class ScheduleServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setAttribute("activePage", "schedule");
        request.setAttribute("pageTitle", "Schedule & Slots");
        request.getRequestDispatcher("/WEB-INF/views/schedule.jsp").forward(request, response);
    }
}
