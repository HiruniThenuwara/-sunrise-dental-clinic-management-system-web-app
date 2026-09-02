package com.sunrise.controller;

import com.sunrise.dao.DaoFactory;
import com.sunrise.dao.TreatmentDao;
import com.sunrise.model.Treatment;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Treatment type management.
 *
 * <p>Every appointment names a treatment, and the bill is built from that
 * treatment's cost plus the dentist's fee, so the clinic needs to be able to
 * add treatments and change prices without a developer editing the
 * database.</p>
 *
 * <p>As with dentists, a treatment is deactivated rather than deleted. Old
 * appointments and bills keep showing what was really done and charged.</p>
 */
@WebServlet(name = "TreatmentServlet", urlPatterns = {"/admin/treatments"})
public class TreatmentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String VIEW = "/WEB-INF/views/treatments.jsp";

    private transient TreatmentDao treatmentDao;

    @Override
    public void init() {
        this.treatmentDao = DaoFactory.getTreatmentDao();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        putListOnRequest(request);
        moveFlashMessagesToRequest(request);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        if ("toggle".equals(request.getParameter("action"))) {
            int treatmentId = parseId(request.getParameter("treatmentId"));
            boolean makeActive = "1".equals(request.getParameter("active"));

            // Called once and the result reused. Calling it inside a ternary
            // would run the update twice.
            boolean changed = treatmentDao.setActive(treatmentId, makeActive);

            request.getSession().setAttribute(changed ? "flashSuccess" : "flashError",
                    changed
                            ? (makeActive
                                ? "The treatment can be chosen for new appointments again."
                                : "The treatment is no longer offered for new appointments.")
                            : "The treatment could not be updated.");

            response.sendRedirect(request.getContextPath() + "/admin/treatments");
            return;
        }

        save(request, response);
    }

    /** Adds a new treatment or updates an existing one. */
    private void save(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int treatmentId = parseId(request.getParameter("treatmentId"));
        String name = request.getParameter("treatmentName");
        String description = request.getParameter("description");
        BigDecimal cost = parseAmount(request.getParameter("baseCost"));
        int minutes = parseId(request.getParameter("estimatedMinutes"));
        boolean active = !"0".equals(request.getParameter("status"));

        List<String> errors = validate(name, cost, minutes, treatmentId);

        if (errors.isEmpty()) {
            Treatment treatment = new Treatment();
            treatment.setTreatmentId(treatmentId);
            treatment.setTreatmentName(name.trim());
            treatment.setDescription(description == null || description.isBlank()
                    ? null : description.trim());
            treatment.setBaseCost(cost);
            treatment.setEstimatedMinutes(minutes);
            treatment.setActive(active);

            boolean saved = treatmentId > 0
                    ? treatmentDao.update(treatment)
                    : treatmentDao.insert(treatment).getTreatmentId() > 0;

            if (saved) {
                request.getSession().setAttribute("flashSuccess",
                        treatment.getTreatmentName()
                                + (treatmentId > 0 ? " was updated." : " was added."));
                response.sendRedirect(request.getContextPath() + "/admin/treatments");
                return;
            }
            errors.add("The treatment could not be saved. Please try again.");
        }

        putListOnRequest(request);
        request.setAttribute("errors", errors);
        request.setAttribute("formTreatmentId", treatmentId);
        request.setAttribute("formName", name);
        request.setAttribute("formDescription", description);
        request.setAttribute("formCost", request.getParameter("baseCost"));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    /**
     * Checks the treatment form. The name must be unique, because the
     * appointment form shows treatments by name and two identical entries
     * would be impossible for the receptionist to tell apart.
     */
    private List<String> validate(String name, BigDecimal cost, int minutes, int treatmentId) {
        List<String> errors = new ArrayList<>();

        if (name == null || name.trim().length() < 3 || name.trim().length() > 100) {
            errors.add("Treatment name must be between 3 and 100 characters.");
        } else {
            treatmentDao.findByName(name.trim())
                    .filter(existing -> existing.getTreatmentId() != treatmentId)
                    .ifPresent(existing ->
                            errors.add("A treatment called \"" + name.trim() + "\" already exists."));
        }

        if (cost == null || cost.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Cost must be zero or more.");
        }
        if (minutes <= 0) {
            errors.add("Please choose how long the treatment takes.");
        }
        return errors;
    }

    /** Loads the treatment list and the statistic card values. */
    private void putListOnRequest(HttpServletRequest request) {
        List<Treatment> treatments = treatmentDao.findAll();

        long active = treatments.stream().filter(Treatment::isActive).count();

        BigDecimal highest = treatments.stream()
                .map(Treatment::getBaseCost)
                .filter(cost -> cost != null)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        int longest = treatments.stream()
                .mapToInt(Treatment::getEstimatedMinutes)
                .max()
                .orElse(0);

        request.setAttribute("treatments", treatments);
        request.setAttribute("totalCount", treatments.size());
        request.setAttribute("activeCount", active);
        request.setAttribute("highestCost", new java.text.DecimalFormat("#,##0").format(highest));
        request.setAttribute("longestMinutes", longest);
        request.setAttribute("activePage", "treatments");
        request.setAttribute("pageTitle", "Treatments Management");
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
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
