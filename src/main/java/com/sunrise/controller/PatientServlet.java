package com.sunrise.controller;

import com.sunrise.model.Page;
import com.sunrise.dao.PatientDao;
import com.sunrise.dao.DaoFactory;
import com.sunrise.model.Appointment;
import com.sunrise.model.PatientSummary;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Patient records.
 *
 * <ul>
 *   <li>{@code GET /admin/patients} - everyone who has been registered,
 *       with their visit history summarised</li>
 *   <li>{@code GET /admin/patients?id=7} - one patient and every visit
 *       they have made</li>
 * </ul>
 *
 * <p>This answers the "lost patient records" problem in the scenario
 * directly: the paper files are replaced by a list that can be searched by
 * name, telephone number or NIC, and every visit a patient has ever made is
 * on one screen.</p>
 *
 * <p>Both roles can see it. A receptionist needs the patient's history at
 * the front desk as much as an administrator does.</p>
 */
@WebServlet(name = "PatientServlet", urlPatterns = {"/admin/patients"})
public class PatientServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String LIST_VIEW = "/WEB-INF/views/patients.jsp";
    private static final String DETAILS_VIEW = "/WEB-INF/views/patient-details.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int patientId = parseId(request.getParameter("id"));

        if (patientId > 0) {
            showOnePatient(request, response, patientId);
            return;
        }
        showList(request, response);
    }

    /** The searchable list of everyone on file. */
    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String search = request.getParameter("search");
        PatientDao patientDao = DaoFactory.getPatientDao();

        // The search belongs on the SQL, not on the page of results:
        // otherwise page two of a search would show the wrong people.
        Page<PatientSummary> page = Page.of(
                request.getParameter("page"),
                patientDao.countWithHistory(search),
                (offset, limit) -> patientDao.findPageWithHistory(search, offset, limit));

        request.setAttribute("patients", page.getItems());
        request.setAttribute("pageInfo", page);
        request.setAttribute("search", search);
        request.setAttribute("totalCount", page.getTotalItems());
        request.setAttribute("withUpcoming", patientDao.countWithUpcoming());
        request.setAttribute("newPatients", patientDao.countFirstTime());
        request.setAttribute("totalBilled", formatMoney(patientDao.totalBilled()));

        request.setAttribute("activePage", "patients");
        request.setAttribute("pageTitle", "Patients Management");

        moveFlashMessagesToRequest(request);
        request.getRequestDispatcher(LIST_VIEW).forward(request, response);
    }

    /** One patient, with every appointment they have ever had. */
    private void showOnePatient(HttpServletRequest request, HttpServletResponse response,
                                int patientId) throws ServletException, IOException {

        Optional<PatientSummary> found = DaoFactory.getPatientDao().findSummaryById(patientId);

        if (found.isEmpty()) {
            request.getSession().setAttribute("flashError", "That patient record was not found.");
            response.sendRedirect(request.getContextPath() + "/admin/patients");
            return;
        }

        List<Appointment> visits = DaoFactory.getAppointmentDao().findByPatient(patientId);

        request.setAttribute("summary", found.get());
        request.setAttribute("visits", visits);
        request.setAttribute("activePage", "patients");
        request.setAttribute("pageTitle", "Patient Record");

        request.getRequestDispatcher(DETAILS_VIEW).forward(request, response);
    }

    private String formatMoney(BigDecimal amount) {
        return new java.text.DecimalFormat("#,##0.00").format(amount);
    }

    private int parseId(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
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
