<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Appointment list and search (Requirement 3).

    The search box looks a visit up by the unique number printed on the
    patient's card, which is exactly the lookup described in the scenario.
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
    <div>
        <h2 class="page-head__title">All Appointments</h2>
        <p class="page-head__sub">Search by appointment number to bring up a patient's visit.</p>
    </div>
    <c:if test="${not sessionScope.user.admin}">
        <a class="btn btn--primary" href="${ctx}/admin/appointments/new">+ New Appointment</a>
    </c:if>
</div>

<c:if test="${not empty flashSuccess}">
    <div class="alert-bar alert-bar--success"><c:out value="${flashSuccess}"/></div>
</c:if>
<c:if test="${not empty flashError}">
    <div class="alert-bar alert-bar--error"><c:out value="${flashError}"/></div>
</c:if>

<%-- ---------- search by appointment number ---------- --%>
<section class="panel search-panel">
    <div class="panel__body">
        <form class="search-row" method="get" action="${ctx}/admin/appointments">
            <div class="form-field form-field--grow">
                <label for="searchNo">Search by Appointment Number</label>
                <input class="input mono" type="search" id="searchNo" name="no"
                       value="<c:out value='${searchNo}'/>"
                       placeholder="APT-20260907-001">
            </div>
            <button class="btn btn--primary" type="submit">Search</button>
            <c:if test="${not empty searchNo}">
                <a class="btn btn--ghost" href="${ctx}/admin/appointments">Clear</a>
            </c:if>
        </form>
    </div>
</section>

<%-- ---------- results ---------- --%>
<section class="panel">
    <header class="panel__head">
        <h3>
            <c:choose>
                <c:when test="${not empty searchNo}">Search Result</c:when>
                <c:otherwise>Recent Appointments</c:otherwise>
            </c:choose>
        </h3>
        <div class="filters">
            <span class="badge badge--warning">${bookedCount} Booked</span>
            <span class="badge badge--success">${completedCount} Completed</span>
            <span class="badge badge--danger">${cancelledCount} Cancelled</span>
        </div>
    </header>

    <div class="table-wrap">
        <table class="table" data-no-pager>
            <thead>
            <tr>
                <th>Appointment No</th>
                <th>Patient</th>
                <th>Dentist</th>
                <th>Treatment</th>
                <th>Date &amp; Time</th>
                <th>Status</th>
                <th></th>
            </tr>
            </thead>
            <tbody>

            <c:forEach var="appointment" items="${appointments}">
                <tr>
                    <td><span class="mono"><c:out value="${appointment.appointmentNo}"/></span></td>
                    <td>
                        <div class="patient-cell">
                            <span class="avatar"><c:out value="${appointment.patient.initials}"/></span>
                            <div>
                                <strong><c:out value="${appointment.patient.patientName}"/></strong>
                                <div class="cell-sub">
                                    <c:out value="${appointment.patient.formattedContactNumber}"/>
                                </div>
                            </div>
                        </div>
                    </td>
                    <td><c:out value="${appointment.doctor.doctorName}"/></td>
                    <td><c:out value="${appointment.treatment.treatmentName}"/></td>
                    <td>
                        <c:out value="${appointment.formattedDate}"/>
                        <div class="cell-sub"><c:out value="${appointment.formattedTime}"/></div>
                    </td>
                    <td>
                        <span class="badge badge--${appointment.status.badgeStyle}">
                            <c:out value="${appointment.status.displayName}"/>
                        </span>
                    </td>
                    <td class="text-right nowrap">
                        <a class="link"
                           href="${ctx}/admin/appointments/view?no=${appointment.appointmentNo}">View</a>
                        <c:if test="${not sessionScope.user.admin and appointment.status.billable}">
                            <a class="link"
                               href="${ctx}/admin/billing?no=${appointment.appointmentNo}">Bill</a>
                        </c:if>

                        <%-- Cancel is offered only while the visit is still
                             booked and the date has not passed, which is what
                             Appointment.isCancellable() decides. Cancelling
                             frees the time slot for another patient. --%>
                        <c:if test="${appointment.cancellable}">
                            <form method="post" action="${ctx}/admin/appointments/status"
                                  class="inline-form"
                                  onsubmit="return confirm('Cancel appointment ${appointment.appointmentNo} for ${appointment.patient.patientName}?\n\nThe time slot becomes free for another patient.');">
                                <input type="hidden" name="appointmentId"
                                       value="${appointment.appointmentId}">
                                <input type="hidden" name="no" value="${appointment.appointmentNo}">
                                <input type="hidden" name="status" value="CANCELLED">
                                <input type="hidden" name="returnTo" value="list">
                                <button type="submit" class="link link--button link--danger">
                                    Cancel
                                </button>
                            </form>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>

            <c:if test="${empty appointments}">
                <tr>
                    <td colspan="7">
                        <div class="empty-state">
                            <p class="empty-state__title">
                                <c:choose>
                                    <c:when test="${not empty searchNo}">No appointment found</c:when>
                                    <c:otherwise>No appointments yet</c:otherwise>
                                </c:choose>
                            </p>
                            <p class="empty-state__text">
                                <c:choose>
                                    <c:when test="${not empty searchNo}">
                                        Check the number on the patient's card and try again.
                                    </c:when>
                                    <c:otherwise>
                                        Register the first appointment from the New Appointment screen.
                                    </c:otherwise>
                                </c:choose>
                            </p>
                        </div>
                    </td>
                </tr>
            </c:if>

            </tbody>
        </table>
    </div>

    <%-- Page links. The servlet put the page on the request as pageInfo. --%>
    <jsp:include page="/WEB-INF/views/layout/pager.jsp"/>
</section>

<script src="${ctx}/assets/js/ui.js"></script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
