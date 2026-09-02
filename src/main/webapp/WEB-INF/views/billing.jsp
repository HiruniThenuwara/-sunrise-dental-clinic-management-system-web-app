<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Calculate and print the bill (Requirement 4).

    Left: find the visit and set the discount and payment method.
    Right: the printable receipt. The print stylesheet hides the sidebar, top
    bar and buttons, so only the receipt reaches the printer.
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head no-print">
    <div>
        <h2 class="page-head__title">Billing</h2>
        <p class="page-head__sub">Find the visit by its appointment number, then produce the receipt.</p>
    </div>
    <div class="page-head__actions">
        <a class="btn btn--ghost" href="${ctx}/admin/appointments">Back to list</a>
        <c:if test="${not empty bill or not empty existingBill}">
            <button class="btn btn--primary" type="button" onclick="window.print()">Print Receipt</button>
        </c:if>
    </div>
</div>

<c:if test="${not empty flashSuccess}">
    <div class="alert-bar alert-bar--success no-print"><c:out value="${flashSuccess}"/></div>
</c:if>
<c:if test="${not empty flashError}">
    <div class="alert-bar alert-bar--error no-print"><c:out value="${flashError}"/></div>
</c:if>
<c:if test="${not empty errors}">
    <div class="alert-bar alert-bar--error no-print">
        <strong>The bill was not produced:</strong>
        <ul>
            <c:forEach var="error" items="${errors}">
                <li><c:out value="${error}"/></li>
            </c:forEach>
        </ul>
    </div>
</c:if>

<%-- ---------- find the visit ---------- --%>
<section class="panel search-panel no-print">
    <div class="panel__body">
        <form class="search-row" method="get" action="${ctx}/admin/billing">
            <div class="form-field form-field--grow">
                <label for="billSearch">Appointment Number</label>
                <input class="input mono" type="search" id="billSearch" name="no"
                       value="<c:out value='${searchNo}'/>"
                       placeholder="APT-20260907-001" required>
            </div>
            <button class="btn btn--primary" type="submit">Find Visit</button>
        </form>
    </div>
</section>

<c:choose>

    <%-- ================= nothing found yet ================= --%>
    <c:when test="${empty appointment}">
        <section class="panel no-print">
            <div class="panel__body">
                <div class="empty-state">
                    <p class="empty-state__title">Find a visit to bill</p>
                    <p class="empty-state__text">
                        Type the appointment number from the patient's card above and
                        press Find Visit. The consultation fee and treatment cost are
                        filled in automatically from the record.
                    </p>
                </div>
            </div>
        </section>
    </c:when>

    <%-- ================= the visit was found ================= --%>
    <c:otherwise>
        <div class="grid-2">

            <%-- ---------- bill builder ---------- --%>
            <section class="panel no-print">
                <header class="panel__head">
                    <h3>Bill Details</h3>
                    <span class="badge badge--muted"><c:out value="${ruleApplied}"/></span>
                </header>
                <div class="panel__body">

                    <dl class="detail-grid">
                        <dt>Patient</dt>
                        <dd><c:out value="${appointment.patient.patientName}"/></dd>
                        <dt>Dentist</dt>
                        <dd><c:out value="${appointment.doctor.doctorName}"/></dd>
                        <dt>Treatment</dt>
                        <dd><c:out value="${appointment.treatment.treatmentName}"/></dd>
                        <dt>Visit Date</dt>
                        <dd><c:out value="${appointment.formattedDate}"/>,
                            <c:out value="${appointment.formattedTime}"/></dd>
                        <dt>Pricing Rule</dt>
                        <dd><c:out value="${ruleApplied}"/></dd>
                    </dl>

                    <hr class="divider">

                    <c:choose>
                        <c:when test="${not empty bill or not empty existingBill}">
                            <p class="hint">
                                This visit has been billed. The receipt is shown on the right
                                and can be printed. A visit is billed only once, so the totals
                                cannot be changed afterwards.
                            </p>
                            <button class="btn btn--primary btn--block" type="button"
                                    onclick="window.print()">Print Receipt</button>
                        </c:when>

                        <c:otherwise>
                            <form method="post" action="${ctx}/admin/billing">
                                <input type="hidden" name="appointmentNo"
                                       value="${appointment.appointmentNo}">

                                <ul class="summary-list">
                                    <li><span>Consultation fee</span>
                                        <strong>LKR ${appointment.doctor.consultationFee}</strong></li>
                                    <li><span>Treatment cost</span>
                                        <strong>LKR ${appointment.treatment.baseCost}</strong></li>
                                </ul>

                                <div class="form-row">
                                    <div class="form-field">
                                        <label for="discount">Discount (LKR)</label>
                                        <input class="input" type="number" id="discount" name="discount"
                                               value="0" min="0" step="100">
                                        <p class="hint">Only with the dentist's approval.</p>
                                    </div>
                                    <div class="form-field">
                                        <label for="paymentMethod">Payment Method</label>
                                        <select class="input" id="paymentMethod" name="paymentMethod">
                                            <option value="CASH">Cash</option>
                                            <option value="CARD">Card</option>
                                            <option value="INSURANCE">Insurance</option>
                                        </select>
                                    </div>
                                </div>

                                <div class="form-actions">
                                    <button class="btn btn--primary" type="submit">
                                        Generate &amp; Save Bill
                                    </button>
                                </div>
                            </form>
                        </c:otherwise>
                    </c:choose>

                </div>
            </section>

            <%-- ---------- printable receipt ---------- --%>
            <c:set var="shown" value="${not empty bill ? bill : existingBill}"/>

            <c:choose>
                <c:when test="${empty shown}">
                    <section class="panel no-print">
                        <div class="panel__body">
                            <div class="empty-state">
                                <p class="empty-state__title">Receipt not produced yet</p>
                                <p class="empty-state__text">
                                    Set the discount and payment method, then press
                                    Generate and Save Bill. The receipt appears here ready
                                    to print.
                                </p>
                            </div>
                        </div>
                    </section>
                </c:when>

                <c:otherwise>
                    <section class="receipt" id="receipt">

                        <header class="receipt__head">
                            <h2 class="receipt__clinic">Sunrise Dental Clinic</h2>
                            <p class="receipt__address">
                                No 128, Galle Road, Colombo 03<br>
                                Tel: 011 234 5678 &middot; info@sunrisedental.lk
                            </p>
                            <p class="receipt__title">PATIENT RECEIPT</p>
                        </header>

                        <div class="receipt__meta">
                            <div><span>Bill No</span>
                                <strong class="mono"><c:out value="${shown.billNo}"/></strong></div>
                            <div><span>Appointment No</span>
                                <strong class="mono"><c:out value="${appointment.appointmentNo}"/></strong></div>
                            <div><span>Visit Date</span>
                                <strong><c:out value="${appointment.formattedDate}"/></strong></div>
                            <div><span>Cashier</span>
                                <strong><c:out value="${sessionScope.user.fullName}"/></strong></div>
                        </div>

                        <div class="receipt__patient">
                            <p><strong><c:out value="${appointment.patient.patientName}"/></strong></p>
                            <p><c:out value="${appointment.patient.address}"/></p>
                            <p><c:out value="${appointment.patient.formattedContactNumber}"/></p>
                        </div>

                        <table class="receipt__table">
                            <thead>
                            <tr>
                                <th>Description</th>
                                <th class="text-right">Amount (LKR)</th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr>
                                <td>Consultation - <c:out value="${appointment.doctor.doctorName}"/>
                                    <br><small><c:out value="${appointment.doctor.specialization}"/></small></td>
                                <td class="text-right mono">${shown.consultationFee}</td>
                            </tr>
                            <tr>
                                <td><c:out value="${appointment.treatment.treatmentName}"/>
                                    <br><small><c:out value="${appointment.treatment.description}"/></small></td>
                                <td class="text-right mono">${shown.treatmentCost}</td>
                            </tr>
                            <tr>
                                <td>Discount</td>
                                <td class="text-right mono">${shown.discount}</td>
                            </tr>
                            </tbody>
                            <tfoot>
                            <tr>
                                <th>TOTAL</th>
                                <th class="text-right mono">${shown.totalAmount}</th>
                            </tr>
                            </tfoot>
                        </table>

                        <div class="receipt__pay">
                            <span>Payment method</span>
                            <strong><c:out value="${shown.paymentMethod.displayName}"/></strong>
                        </div>

                        <footer class="receipt__foot">
                            <p>Thank you for visiting Sunrise Dental Clinic.</p>
                            <p class="muted">
                                This is a computer generated receipt. Please keep it for your records.
                            </p>
                        </footer>
                    </section>
                </c:otherwise>
            </c:choose>

        </div>
    </c:otherwise>
</c:choose>

<script src="${ctx}/assets/js/ui.js"></script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
