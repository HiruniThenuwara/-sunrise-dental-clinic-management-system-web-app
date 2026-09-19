<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    One patient's complete record: their details and every visit they have
    made, newest first. This is what the paper file used to be.
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="patient" value="${summary.patient}"/>

<div class="page-head">
    <div>
        <h2 class="page-head__title"><c:out value="${patient.patientName}"/></h2>
        <p class="page-head__sub">
            Patient record &middot; ${summary.visitCount}
            visit<c:if test="${summary.visitCount ne 1}">s</c:if> at the clinic
        </p>
    </div>
    <div class="page-head__actions">
        <a class="btn btn--ghost" href="${ctx}/admin/patients">Back to patients</a>
        <c:if test="${not sessionScope.user.admin}">
            <a class="btn btn--primary" href="${ctx}/admin/appointments/new">+ New Appointment</a>
        </c:if>
    </div>
</div>

<div class="status-strip">
    <div class="status-strip__item">
        <span class="status-strip__label">Total Visits</span>
        <strong>${summary.visitCount}</strong>
    </div>
    <div class="status-strip__item">
        <span class="status-strip__label">Completed</span>
        <strong>${summary.completedCount}</strong>
    </div>
    <div class="status-strip__item">
        <span class="status-strip__label">Next Appointment</span>
        <strong><c:out value="${summary.formattedNextVisit}"/></strong>
    </div>
    <div class="status-strip__item">
        <span class="status-strip__label">Total Billed</span>
        <strong class="amount">LKR ${summary.formattedTotal}</strong>
    </div>
</div>

<div class="grid-2">

    <%-- ---------- the visit history ---------- --%>
    <section class="panel">
        <header class="panel__head">
            <h3>Visit History</h3>
            <span class="badge badge--muted">Newest first</span>
        </header>

        <div class="table-wrap">
            <table class="table">
                <thead>
                <tr>
                    <th>Appointment No</th>
                    <th>Date &amp; Time</th>
                    <th>Dentist</th>
                    <th>Treatment</th>
                    <th>Status</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>

                <c:forEach var="visit" items="${visits}">
                    <tr>
                        <td><span class="mono"><c:out value="${visit.appointmentNo}"/></span></td>
                        <td>
                            <c:out value="${visit.formattedDate}"/>
                            <div class="cell-sub"><c:out value="${visit.formattedTime}"/></div>
                        </td>
                        <td><c:out value="${visit.doctor.doctorName}"/></td>
                        <td>
                            <c:out value="${visit.treatment.treatmentName}"/>
                            <div class="cell-sub">LKR ${visit.treatment.baseCost}</div>
                        </td>
                        <td>
                            <span class="badge badge--${visit.status.badgeStyle}">
                                <c:out value="${visit.status.displayName}"/>
                            </span>
                        </td>
                        <td class="text-right">
                            <a class="link"
                               href="${ctx}/admin/appointments/view?no=${visit.appointmentNo}">View</a>
                        </td>
                    </tr>
                </c:forEach>

                <c:if test="${empty visits}">
                    <tr>
                        <td colspan="6">
                            <div class="empty-state">
                                <p class="empty-state__title">No visits recorded</p>
                                <p class="empty-state__text">
                                    This patient has no appointments on file.
                                </p>
                            </div>
                        </td>
                    </tr>
                </c:if>

                </tbody>
            </table>
        </div>
    </section>

    <%-- ---------- the personal details ---------- --%>
    <div class="side-column">

        <section class="panel">
            <header class="panel__head"><h3>Patient Details</h3></header>
            <div class="panel__body">
                <dl class="detail-grid">
                    <dt>Full Name</dt>
                    <dd><c:out value="${patient.patientName}"/></dd>

                    <dt>Contact Number</dt>
                    <dd><c:out value="${patient.formattedContactNumber}"/></dd>

                    <dt>Email</dt>
                    <dd><c:out value="${empty patient.email ? '-' : patient.email}"/></dd>

                    <dt>NIC Number</dt>
                    <dd class="mono"><c:out value="${empty patient.nic ? '-' : patient.nic}"/></dd>

                    <%-- Date of birth is not shown. The appointment form does
                         not collect it, so the row was always empty. The
                         column is kept in the database for a later release
                         that asks for it. --%>

                    <dt>Gender</dt>
                    <dd>
                        <c:out value="${empty patient.gender ? '-' : patient.gender.displayName}"/>
                    </dd>

                    <dt>Address</dt>
                    <dd><c:out value="${patient.address}"/></dd>
                </dl>
            </div>
        </section>

        <section class="panel">
            <header class="panel__head"><h3>Summary</h3></header>
            <div class="panel__body">
                <ul class="summary-list">
                    <li><span>Total visits</span><strong>${summary.visitCount}</strong></li>
                    <li><span>Completed</span><strong>${summary.completedCount}</strong></li>
                    <li><span>Cancelled</span><strong>${summary.cancelledCount}</strong></li>
                    <li><span>Last seen</span>
                        <strong><c:out value="${summary.formattedLastVisit}"/></strong></li>
                    <li class="summary-list__total"><span>Total billed</span>
                        <strong>LKR ${summary.formattedTotal}</strong></li>
                </ul>
                <p class="hint">
                    Billed totals come from the receipts that were actually issued,
                    including any discount given.
                </p>
            </div>
        </section>

    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
