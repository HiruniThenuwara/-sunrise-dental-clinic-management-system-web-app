<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Admin panel home page. Every figure is read from the database by
    DashboardServlet when the page is requested.

    The icons are inline SVG rather than text characters: a character such as
    the watch symbol renders differently on every machine and shows as an
    empty box where the font lacks it, whereas an SVG looks the same
    everywhere and takes the colour of its card.
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
            <span class="stat-card__icon stat-card__icon--teal">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                     stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <rect x="3" y="4.5" width="18" height="16.5" rx="2.5"/>
                    <path d="M3 9.5h18M8 2.5v4M16 2.5v4"/>
                    <path d="m8.5 14.5 2 2 4-4"/>
                </svg>
            </span>
        </div>
        <p class="stat-card__value">${todayCount}</p>
        <p class="stat-card__trend">${todayCompleted} completed so far</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Registered Patients</p>
            <span class="stat-card__icon stat-card__icon--blue">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                     stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <circle cx="9" cy="8" r="3.4"/>
                    <path d="M2.5 20a6.5 6.5 0 0 1 13 0"/>
                    <path d="M16.5 5.2a3.4 3.4 0 0 1 0 5.6M18 20a6.4 6.4 0 0 0-2.2-4.8"/>
                </svg>
            </span>
        </div>
        <p class="stat-card__value">${patientCount}</p>
        <p class="stat-card__trend">on file at the clinic</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Dentists On Duty</p>
            <span class="stat-card__icon stat-card__icon--violet">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                     stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M6 3v5.5a4.5 4.5 0 0 0 9 0V3"/>
                    <path d="M4 3h3M13.5 3h3"/>
                    <path d="M10.5 13v3a4.5 4.5 0 0 0 9 0v-1.5"/>
                    <circle cx="19.5" cy="12.5" r="2"/>
                </svg>
            </span>
        </div>
        <p class="stat-card__value">${activeDoctors}</p>
        <p class="stat-card__trend">of ${totalDoctors} registered dentists</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Today's Revenue</p>
            <span class="stat-card__icon stat-card__icon--amber">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                     stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M5 3.5h14v17l-2.3-1.6-2.3 1.6-2.4-1.6L9.6 20.5 7.3 19 5 20.5z"/>
                    <path d="M9 8.5h6M9 12h6"/>
                </svg>
            </span>
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
                            <span class="quick-action__icon">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                     stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"
                                     aria-hidden="true">
                                    <rect x="3" y="4.5" width="18" height="16.5" rx="2.5"/>
                                    <path d="M3 9.5h18M8 2.5v4M16 2.5v4"/>
                                    <path d="M12 12.5v5M9.5 15h5"/>
                                </svg>
                            </span>
                            <span><strong>Register Appointment</strong>
                                  <small>Add a new patient visit</small></span>
                        </a>
                    </c:if>

                    <a class="quick-action" href="${ctx}/admin/appointments">
                        <span class="quick-action__icon">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                 stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"
                                 aria-hidden="true">
                                <circle cx="10.5" cy="10.5" r="6.5"/>
                                <path d="m15.5 15.5 5 5"/>
                            </svg>
                        </span>
                        <span><strong>Find Appointment</strong>
                              <small>Search by appointment number</small></span>
                    </a>

                    <a class="quick-action" href="${ctx}/admin/patients">
                        <span class="quick-action__icon">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                 stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"
                                 aria-hidden="true">
                                <circle cx="9" cy="8" r="3.4"/>
                                <path d="M2.5 20a6.5 6.5 0 0 1 13 0"/>
                                <path d="M16.5 5.2a3.4 3.4 0 0 1 0 5.6M18 20a6.4 6.4 0 0 0-2.2-4.8"/>
                            </svg>
                        </span>
                        <span><strong>Patient Records</strong>
                              <small>Find a patient and their history</small></span>
                    </a>

                    <c:if test="${not sessionScope.user.admin}">
                        <a class="quick-action" href="${ctx}/admin/billing">
                            <span class="quick-action__icon">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                     stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"
                                     aria-hidden="true">
                                    <path d="M5 3.5h14v17l-2.3-1.6-2.3 1.6-2.4-1.6L9.6 20.5 7.3 19 5 20.5z"/>
                                    <path d="M9 8.5h6M9 12h6"/>
                                </svg>
                            </span>
                            <span><strong>Print a Bill</strong>
                                  <small>Calculate and print a receipt</small></span>
                        </a>
                    </c:if>

                    <c:if test="${sessionScope.user.admin}">
                        <a class="quick-action" href="${ctx}/admin/doctors">
                            <span class="quick-action__icon">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                     stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"
                                     aria-hidden="true">
                                    <path d="M6 3v5.5a4.5 4.5 0 0 0 9 0V3"/>
                                    <path d="M4 3h3M13.5 3h3"/>
                                    <path d="M10.5 13v3a4.5 4.5 0 0 0 9 0v-1.5"/>
                                    <circle cx="19.5" cy="12.5" r="2"/>
                                </svg>
                            </span>
                            <span><strong>Manage Dentists</strong>
                                  <small>Add a dentist or change a fee</small></span>
                        </a>

                        <a class="quick-action" href="${ctx}/admin/reports">
                            <span class="quick-action__icon">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                     stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"
                                     aria-hidden="true">
                                    <path d="M3.5 20.5h17"/>
                                    <rect x="5" y="12" width="3.6" height="8.5" rx="1"/>
                                    <rect x="10.2" y="7" width="3.6" height="13.5" rx="1"/>
                                    <rect x="15.4" y="3.5" width="3.6" height="17" rx="1"/>
                                </svg>
                            </span>
                            <span><strong>View Reports</strong>
                                  <small>Workload and revenue</small></span>
                        </a>
                    </c:if>

                    <a class="quick-action" href="${ctx}/admin/help">
                        <span class="quick-action__icon">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                 stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"
                                 aria-hidden="true">
                                <circle cx="12" cy="12" r="9"/>
                                <path d="M9.6 9.4a2.5 2.5 0 1 1 3.3 2.4c-.6.2-.9.8-.9 1.4v.4"/>
                                <path d="M12 16.8v.1"/>
                            </svg>
                        </span>
                        <span><strong>Help Guide</strong>
                              <small>Step by step instructions</small></span>
                    </a>
                </div>
            </div>
        </section>

    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
