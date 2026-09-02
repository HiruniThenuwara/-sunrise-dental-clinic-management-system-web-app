<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Admin panel home page. Every figure is read from the database by
    DashboardServlet when the page is requested.
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="welcome-card">
    <div>
        <h2>Welcome back, <c:out value="${sessionScope.user.firstName}"/>.</h2>
        <p>
            <c:choose>
                <c:when test="${todayCount eq 0}">
                    There are no appointments booked for today.
                </c:when>
                <c:otherwise>
                    ${todayCount} appointment<c:if test="${todayCount ne 1}">s</c:if>
                    booked for today, ${todayCompleted} already completed.
                </c:otherwise>
            </c:choose>
        </p>
    </div>
    <c:choose>
        <c:when test="${sessionScope.user.admin}">
            <a class="btn btn--primary" href="${ctx}/admin/reports">View Reports</a>
        </c:when>
        <c:otherwise>
            <a class="btn btn--primary" href="${ctx}/admin/appointments/new">+ New Appointment</a>
        </c:otherwise>
    </c:choose>
</div>

<c:if test="${not empty flashSuccess}">
    <div class="alert-bar alert-bar--success"><c:out value="${flashSuccess}"/></div>
</c:if>
<c:if test="${not empty flashError}">
    <div class="alert-bar alert-bar--error"><c:out value="${flashError}"/></div>
</c:if>

<!-- ================= statistic cards ================= -->
<section class="stat-grid">

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Today's Appointments</p>
            <span class="stat-card__icon stat-card__icon--teal">&#9200;</span>
        </div>
        <p class="stat-card__value">${todayCount}</p>
        <p class="stat-card__trend">${todayCompleted} completed so far</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Registered Patients</p>
            <span class="stat-card__icon stat-card__icon--blue">&#9787;</span>
        </div>
        <p class="stat-card__value">${patientCount}</p>
        <p class="stat-card__trend">on file at the clinic</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Dentists On Duty</p>
            <span class="stat-card__icon stat-card__icon--violet">&#9877;</span>
        </div>
        <p class="stat-card__value">${activeDoctors}</p>
        <p class="stat-card__trend">of ${totalDoctors} registered dentists</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Today's Revenue</p>
            <span class="stat-card__icon stat-card__icon--amber">&#8377;</span>
        </div>
        <p class="stat-card__value">${todayRevenue}</p>
        <p class="stat-card__trend">LKR taken today</p>
    </article>

</section>

<div class="grid-2">

    <!-- ---------- recent appointments ---------- -->
    <section class="panel">
        <header class="panel__head">
            <h3>Recent Appointments</h3>
            <a class="link" href="${ctx}/admin/appointments">View all</a>
        </header>

        <div class="table-wrap">
            <table class="table">
                <thead>
                <tr>
                    <th>Appointment No</th>
                    <th>Patient</th>
                    <th>Dentist</th>
                    <th>Date &amp; Time</th>
                    <th>Status</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>

                <c:forEach var="appointment" items="${recentAppointments}">
                    <tr>
                        <td><span class="mono"><c:out value="${appointment.appointmentNo}"/></span></td>
                        <td>
                            <div class="patient-cell">
                                <span class="avatar"><c:out value="${appointment.patient.initials}"/></span>
                                <div>
                                    <strong><c:out value="${appointment.patient.patientName}"/></strong>
                                    <div class="cell-sub">
                                        <c:out value="${appointment.treatment.treatmentName}"/>
                                    </div>
                                </div>
                            </div>
                        </td>
                        <td><c:out value="${appointment.doctor.doctorName}"/></td>
                        <td><c:out value="${appointment.formattedDate}"/>
                            <div class="cell-sub"><c:out value="${appointment.formattedTime}"/></div></td>
                        <td><span class="badge badge--${appointment.status.badgeStyle}">
                            <c:out value="${appointment.status.displayName}"/></span></td>
                        <td class="text-right">
                            <a class="link"
                               href="${ctx}/admin/appointments/view?no=${appointment.appointmentNo}">View</a>
                        </td>
                    </tr>
                </c:forEach>

                <c:if test="${empty recentAppointments}">
                    <tr>
                        <td colspan="6">
                            <div class="empty-state">
                                <p class="empty-state__title">No appointments yet</p>
                                <p class="empty-state__text">
                                    Once visits are registered they appear here, newest first.
                                </p>
                            </div>
                        </td>
                    </tr>
                </c:if>

                </tbody>
            </table>
        </div>
    </section>

    <!-- ---------- side column ---------- -->
    <div class="side-column">

        <section class="panel">
            <header class="panel__head">
                <h3>Today's Schedule</h3>
                <span class="badge badge--muted">${todayCount} booked</span>
            </header>
            <div class="panel__body">
                <c:choose>
                    <c:when test="${empty todaysVisits}">
                        <div class="empty-state">
                            <p class="empty-state__title">Nothing booked today</p>
                            <p class="empty-state__text">The clinic has a clear day.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <ul class="slot-list">
                            <c:forEach var="visit" items="${todaysVisits}">
                                <li class="slot ${visit.status.displayName eq 'Completed' ? 'slot--done' : ''}">
                                    <span class="slot__time">
                                        <c:out value="${visit.formattedTime}"/>
                                    </span>
                                    <span class="slot__text">
                                        <strong><c:out value="${visit.patient.patientName}"/></strong><br>
                                        <small><c:out value="${visit.treatment.treatmentName}"/> &middot;
                                               <c:out value="${visit.doctor.doctorName}"/></small>
                                    </span>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>

        <section class="panel">
            <header class="panel__head">
                <h3>Quick Actions</h3>
            </header>
            <div class="panel__body">
                <div class="quick-actions">

                    <c:if test="${not sessionScope.user.admin}">
                        <a class="quick-action" href="${ctx}/admin/appointments/new">
                            <span class="quick-action__icon">&#43;</span>
                            <span><strong>Register Appointment</strong>
                                  <small>Add a new patient visit</small></span>
                        </a>
                    </c:if>

                    <a class="quick-action" href="${ctx}/admin/appointments">
                        <span class="quick-action__icon">&#128269;</span>
                        <span><strong>Find Appointment</strong>
                              <small>Search by appointment number</small></span>
                    </a>

                    <c:if test="${not sessionScope.user.admin}">
                        <a class="quick-action" href="${ctx}/admin/billing">
                            <span class="quick-action__icon">&#8377;</span>
                            <span><strong>Print a Bill</strong>
                                  <small>Calculate and print a receipt</small></span>
                        </a>
                    </c:if>

                    <c:if test="${sessionScope.user.admin}">
                        <a class="quick-action" href="${ctx}/admin/doctors">
                            <span class="quick-action__icon">&#9877;</span>
                            <span><strong>Manage Dentists</strong>
                                  <small>Add a dentist or change a fee</small></span>
                        </a>
                        <a class="quick-action" href="${ctx}/admin/reports">
                            <span class="quick-action__icon">&#9650;</span>
                            <span><strong>View Reports</strong>
                                  <small>Workload and revenue</small></span>
                        </a>
                    </c:if>

                    <a class="quick-action" href="${ctx}/admin/help">
                        <span class="quick-action__icon">?</span>
                        <span><strong>Help Guide</strong>
                              <small>Step by step instructions</small></span>
                    </a>
                </div>
            </div>
        </section>

    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
