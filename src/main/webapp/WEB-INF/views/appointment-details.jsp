<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Display appointment details (Requirement 3).

    Shows the complete patient and appointment information for the number the
    staff member searched for, which is what the scenario asks for.
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
    <div>
        <h2 class="page-head__title">
            Appointment <span class="mono"><c:out value="${appointment.appointmentNo}"/></span>
        </h2>
        <p class="page-head__sub">
            Registered by
            <c:out value="${empty appointment.createdBy ? 'clinic staff' : appointment.createdBy.fullName}"/>
        </p>
    </div>
    <div class="page-head__actions">
        <a class="btn btn--ghost" href="${ctx}/admin/appointments">Back to list</a>
        <c:if test="${not sessionScope.user.admin and appointment.status.billable and empty existingBill}">
            <a class="btn btn--primary"
               href="${ctx}/admin/billing?no=${appointment.appointmentNo}">Generate Bill</a>
        </c:if>
    </div>
</div>

<c:if test="${not empty flashSuccess}">
    <div class="alert-bar alert-bar--success"><c:out value="${flashSuccess}"/></div>
</c:if>
<c:if test="${not empty flashError}">
    <div class="alert-bar alert-bar--error"><c:out value="${flashError}"/></div>
</c:if>

<div class="status-strip">
    <div class="status-strip__item">
        <span class="status-strip__label">Status</span>
        <span class="badge badge--${appointment.status.badgeStyle}">
            <c:out value="${appointment.status.displayName}"/>
        </span>
    </div>
    <div class="status-strip__item">
        <span class="status-strip__label">Date &amp; Time</span>
        <strong><c:out value="${appointment.formattedDate}"/>,
                <c:out value="${appointment.formattedTime}"/></strong>
    </div>
    <div class="status-strip__item">
        <span class="status-strip__label">Dentist</span>
        <strong><c:out value="${appointment.doctor.doctorName}"/></strong>
    </div>
    <div class="status-strip__item">
        <span class="status-strip__label">
            <c:out value="${empty existingBill ? 'Estimated Total' : 'Billed Total'}"/>
        </span>
        <strong class="amount">
            LKR
            <c:choose>
                <c:when test="${empty existingBill}">${appointment.estimatedTotal}</c:when>
                <c:otherwise>${existingBill.totalAmount}</c:otherwise>
            </c:choose>
        </strong>
    </div>
</div>

<div class="grid-2">

    <div class="side-column">

        <section class="panel">
            <header class="panel__head"><h3>Patient Information</h3></header>
            <div class="panel__body">
                <dl class="detail-grid">
                    <dt>Patient Name</dt>
                    <dd><c:out value="${appointment.patient.patientName}"/></dd>

                    <dt>NIC Number</dt>
                    <dd class="mono">
                        <c:out value="${empty appointment.patient.nic ? '-' : appointment.patient.nic}"/>
                    </dd>

                    <dt>Address</dt>
                    <dd><c:out value="${appointment.patient.address}"/></dd>

                    <dt>Contact Number</dt>
                    <dd><c:out value="${appointment.patient.formattedContactNumber}"/></dd>

                    <dt>Email</dt>
                    <dd>
                        <c:out value="${empty appointment.patient.email ? '-' : appointment.patient.email}"/>
                    </dd>
                </dl>
            </div>
        </section>

        <section class="panel">
            <header class="panel__head"><h3>Appointment Information</h3></header>
            <div class="panel__body">
                <dl class="detail-grid">
                    <dt>Appointment Number</dt>
                    <dd class="mono"><c:out value="${appointment.appointmentNo}"/></dd>

                    <dt>Dentist</dt>
                    <dd><c:out value="${appointment.doctor.doctorName}"/>
                        <span class="muted">(<c:out value="${appointment.doctor.specialization}"/>)</span></dd>

                    <dt>Treatment Type</dt>
                    <dd><c:out value="${appointment.treatment.treatmentName}"/></dd>

                    <dt>Appointment Date</dt>
                    <dd><c:out value="${appointment.formattedDate}"/></dd>

                    <dt>Appointment Time</dt>
                    <dd><c:out value="${appointment.formattedTime}"/>
                        <span class="muted">(${appointment.treatment.estimatedMinutes} minutes)</span></dd>

                    <dt>Status</dt>
                    <dd><span class="badge badge--${appointment.status.badgeStyle}">
                        <c:out value="${appointment.status.displayName}"/></span></dd>

                    <dt>Notes</dt>
                    <dd><c:out value="${empty appointment.notes ? 'None recorded' : appointment.notes}"/></dd>
                </dl>
            </div>
        </section>

    </div>

    <div class="side-column">

        <section class="panel">
            <header class="panel__head"><h3>Cost Breakdown</h3></header>
            <div class="panel__body">
                <ul class="summary-list">
                    <li><span>Consultation fee</span>
                        <strong>LKR ${appointment.doctor.consultationFee}</strong></li>
                    <li><span><c:out value="${appointment.treatment.treatmentName}"/></span>
                        <strong>LKR ${appointment.treatment.baseCost}</strong></li>
                    <li class="summary-list__total"><span>Estimated total</span>
                        <strong>LKR ${appointment.estimatedTotal}</strong></li>
                </ul>

                <c:choose>
                    <c:when test="${not empty existingBill}">
                        <p class="hint">
                            Already billed as
                            <span class="mono"><c:out value="${existingBill.billNo}"/></span>
                            for LKR ${existingBill.totalAmount}.
                        </p>
                    </c:when>
                    <c:when test="${not sessionScope.user.admin and appointment.status.billable}">
                        <a class="btn btn--primary btn--block"
                           href="${ctx}/admin/billing?no=${appointment.appointmentNo}">Generate Bill</a>
                    </c:when>
                </c:choose>
            </div>
        </section>

        <section class="panel">
            <header class="panel__head"><h3>Actions</h3></header>
            <div class="panel__body">
                <div class="quick-actions">

                    <c:if test="${appointment.status.displayName eq 'Booked'}">
                        <form method="post" action="${ctx}/admin/appointments/status">
                            <input type="hidden" name="appointmentId" value="${appointment.appointmentId}">
                            <input type="hidden" name="no" value="${appointment.appointmentNo}">
                            <input type="hidden" name="status" value="COMPLETED">
                            <button class="quick-action quick-action--button" type="submit">
                                <span class="quick-action__icon">&#10003;</span>
                                <span><strong>Mark as Completed</strong>
                                      <small>After the patient has been treated</small></span>
                            </button>
                        </form>

                        <form method="post" action="${ctx}/admin/appointments/status"
                              onsubmit="return confirm('Cancel this appointment? The time slot becomes free again.');">
                            <input type="hidden" name="appointmentId" value="${appointment.appointmentId}">
                            <input type="hidden" name="no" value="${appointment.appointmentNo}">
                            <input type="hidden" name="status" value="CANCELLED">
                            <button class="quick-action quick-action--button" type="submit">
                                <span class="quick-action__icon">&times;</span>
                                <span><strong>Cancel Appointment</strong>
                                      <small>Frees the time slot again</small></span>
                            </button>
                        </form>
                    </c:if>

                    <a class="quick-action" href="${ctx}/admin/appointments">
                        <span class="quick-action__icon">&#128269;</span>
                        <span><strong>Find Another</strong>
                              <small>Search by appointment number</small></span>
                    </a>
                </div>
            </div>
        </section>

    </div>
</div>

<script src="${ctx}/assets/js/ui.js"></script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
