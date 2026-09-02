package com.sunrise.controller;

import com.sunrise.dao.DaoFactory;
import com.sunrise.model.Appointment;
import com.sunrise.service.AppointmentService;
import com.sunrise.service.BillingService;
import com.sunrise.service.DoctorService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;

/**
 * The admin panel home page.
 *
 * <p>Every figure on this page is read from the database when the page is
 * requested. The JSP under {@code /WEB-INF/views/} cannot be reached
 * directly, so the only way in is through this servlet, and therefore through
 * {@code AuthFilter}.</p>
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/admin/dashboard", "/admin/"})
public class DashboardServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String VIEW = "/WEB-INF/views/dashboard.jsp";
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private transient AppointmentService appointmentService;
    private transient DoctorService doctorService;
    private transient BillingService billingService;

    @Override
    public void init() {
        this.appointmentService = new AppointmentService();
        this.doctorService = new DoctorService();
        this.billingService = new BillingService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        LocalDate today = LocalDate.now();

        List<Appointment> todaysVisits = appointmentService.findByDate(today);
        List<Appointment> recent = appointmentService.findRecent(6);

        request.setAttribute("todayCount", todaysVisits.size());
        request.setAttribute("todayCompleted", todaysVisits.stream()
                .filter(a -> a.getStatus().name().equals("COMPLETED")).count());
        request.setAttribute("patientCount", DaoFactory.getPatientDao().countAll());
        request.setAttribute("activeDoctors", doctorService.countActive());
        request.setAttribute("totalDoctors", doctorService.findAll().size());
        request.setAttribute("todayRevenue", MONEY.format(billingService.takingsFor(today)));
        request.setAttribute("recentAppointments", recent);
        request.setAttribute("todaysVisits", todaysVisits);

        request.setAttribute("activePage", "dashboard");
        request.setAttribute("pageTitle", "Dashboard");

        moveFlashMessagesToRequest(request);
        request.getRequestDispatcher(VIEW).forward(request, response);
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
