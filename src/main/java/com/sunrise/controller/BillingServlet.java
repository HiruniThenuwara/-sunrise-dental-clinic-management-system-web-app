package com.sunrise.controller;

import com.sunrise.model.Appointment;
import com.sunrise.model.Bill;
import com.sunrise.model.PaymentMethod;
import com.sunrise.model.User;
import com.sunrise.service.AppointmentService;
import com.sunrise.service.AuthService;
import com.sunrise.service.BillingService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Calculating and printing the patient bill (Requirement 4).
 *
 * <ul>
 *   <li>{@code GET  /admin/billing?no=APT-...} - find the visit and show the
 *       amounts that will be charged</li>
 *   <li>{@code POST /admin/billing} - produce and store the bill</li>
 * </ul>
 *
 * <p>The servlet never calculates anything. {@link BillingService} chooses
 * the pricing rule and does the arithmetic, which is why the totals are
 * covered by unit tests rather than being retyped at the front desk.</p>
 */
@WebServlet(name = "BillingServlet", urlPatterns = {"/admin/billing"})
public class BillingServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String VIEW = "/WEB-INF/views/billing.jsp";

    private transient AppointmentService appointmentService;
    private transient BillingService billingService;

    @Override
    public void init() {
        this.appointmentService = new AppointmentService();
        this.billingService = new BillingService();
    }

    /** Finds the visit and shows what it will cost. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String appointmentNo = request.getParameter("no");

        if (appointmentNo != null && !appointmentNo.isBlank()) {
            Optional<Appointment> found = appointmentService.findByNumber(appointmentNo);

            if (found.isPresent()) {
                Appointment appointment = found.get();
                request.setAttribute("appointment", appointment);
                request.setAttribute("ruleApplied",
                        billingService.selectStrategy(appointment.getTreatment()).describe());
                request.setAttribute("existingBill",
                        billingService.findByAppointment(appointment.getAppointmentId())
                                .orElse(null));
            } else {
                request.setAttribute("flashError",
                        "No appointment found with the number " + appointmentNo.trim() + ".");
            }
            request.setAttribute("searchNo", appointmentNo.trim());
        }

        showPage(request, response);
    }

    /** Produces and stores the bill. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String appointmentNo = request.getParameter("appointmentNo");
        Optional<Appointment> found = appointmentService.findByNumber(appointmentNo);

        if (found.isEmpty()) {
            request.setAttribute("flashError", "That appointment could not be found.");
            showPage(request, response);
            return;
        }

        Appointment appointment = found.get();

        BillingService.BillResult result = billingService.generate(
                appointment,
                parseAmount(request.getParameter("discount")),
                PaymentMethod.fromString(request.getParameter("paymentMethod")),
                currentUser(request));

        if (result.isSuccess()) {
            Bill bill = result.getBill();

            new com.sunrise.service.ActivityLogService().record(request,
                    com.sunrise.model.ActivityAction.BILL_CREATED,
                    "Bill", bill.getBillNo(),
                    "LKR " + bill.getTotalAmount() + " for " + appointment.getAppointmentNo()
                            + " (" + result.getRuleApplied() + ", paid by "
                            + bill.getPaymentMethod().getDisplayName() + ")");

            request.setAttribute("bill", bill);
            request.setAttribute("appointment", appointment);
            request.setAttribute("ruleApplied", result.getRuleApplied());
            request.setAttribute("flashSuccess",
                    "Bill " + bill.getBillNo() + " saved. You can print the receipt now.");
        } else {
            request.setAttribute("appointment", appointment);
            request.setAttribute("ruleApplied",
                    billingService.selectStrategy(appointment.getTreatment()).describe());
            request.setAttribute("existingBill",
                    billingService.findByAppointment(appointment.getAppointmentId()).orElse(null));
            request.setAttribute("errors", result.getErrors());
        }

        request.setAttribute("searchNo", appointment.getAppointmentNo());
        showPage(request, response);
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setAttribute("activePage", "billing");
        request.setAttribute("pageTitle", "Billing");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    /** Reads a money value from the form, treating blanks as zero. */
    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            // Not a number. Returning -1 makes the service refuse it with a
            // proper message rather than silently treating it as zero.
            return BigDecimal.valueOf(-1);
        }
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object attribute = session.getAttribute(AuthService.SESSION_USER_KEY);
        return (attribute instanceof User) ? (User) attribute : null;
    }
}
