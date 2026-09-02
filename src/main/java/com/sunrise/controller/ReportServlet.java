package com.sunrise.controller;

import com.sunrise.service.ReportService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Management reports for decision making.
 *
 * <p>{@code GET /admin/reports?from=2026-09-01&to=2026-09-30}. With no dates
 * the report covers the current month.</p>
 */
@WebServlet(name = "ReportServlet", urlPatterns = {"/admin/reports"})
public class ReportServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String VIEW = "/WEB-INF/views/reports.jsp";

    private transient ReportService reportService;

    @Override
    public void init() {
        this.reportService = new ReportService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        LocalDate from = parseDate(request.getParameter("from"),
                LocalDate.now().withDayOfMonth(1));
        LocalDate to = parseDate(request.getParameter("to"), LocalDate.now());

        request.setAttribute("from", from);
        request.setAttribute("to", to);
        request.setAttribute("summary", reportService.summary(from, to));
        request.setAttribute("dailyRows", reportService.appointmentsPerDay(from, to));
        request.setAttribute("doctorRows", reportService.workloadByDoctor(from, to));
        request.setAttribute("treatmentRows", reportService.revenueByTreatment(from, to));

        request.setAttribute("activePage", "reports");
        request.setAttribute("pageTitle", "Reports");
        request.getRequestDispatcher(VIEW).forward(request, response);
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
