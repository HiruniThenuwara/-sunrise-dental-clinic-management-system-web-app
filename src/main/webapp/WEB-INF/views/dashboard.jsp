<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>


<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="welcome-card">
    <div>
        <h2>Welcome back, <c:out value="${sessionScope.user.firstName}"/>.</h2>
        <p>Here is what is happening at the clinic today. Use the menu on the left
           to register an appointment or find a patient record.</p>
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


<!-- ================= statistic cards ================= -->
<section class="stat-grid">

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Today's Appointments</p>
            <span class="stat-card__icon stat-card__icon--teal">&#9200;</span>
        </div>
        <p class="stat-card__value">8</p>
        <p class="stat-card__trend trend--up">3 still waiting &middot; 5 completed</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Registered Patients</p>
            <span class="stat-card__icon stat-card__icon--blue">&#9787;</span>
        </div>
        <p class="stat-card__value">342</p>
        <p class="stat-card__trend trend--up">+6 registered this week</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Dentists On Duty</p>
            <span class="stat-card__icon stat-card__icon--violet">&#9877;</span>
        </div>
        <p class="stat-card__value">3</p>
        <p class="stat-card__trend">of 4 registered dentists</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Today's Revenue</p>
            <span class="stat-card__icon stat-card__icon--amber">&#8377;</span>
        </div>
        <p class="stat-card__value">47,500</p>
        <p class="stat-card__trend">LKR from 5 paid bills</p>
    </article>

</section>

<!-- ================= main grid ================= -->
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
                    <th>Treatment</th>
                    <th>Date &amp; Time</th>
                    <th>Status</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>

                <tr>
                    <td><span class="mono">APT-20260901-001</span></td>
                    <td>
                        <div class="patient-cell">
                            <span class="avatar">SK</span>
                            <div>
                                <strong>Saman Kumara</strong>
                                <div class="cell-sub">071 234 5678</div>
                            </div>
                        </div>
                    </td>
                    <td>Dr. Anura Jayasinghe</td>
                    <td>Scaling</td>
                    <td>01 Sep 2026<div class="cell-sub">09:00 AM</div></td>
                    <td><span class="badge badge--warning">Booked</span></td>
                    <td class="text-right"><a class="link" href="${ctx}/admin/appointments">View</a></td>
                </tr>

                <tr>
                    <td><span class="mono">APT-20260901-002</span></td>
                    <td>
                        <div class="patient-cell">
                            <span class="avatar">DR</span>
                            <div>
                                <strong>Dilini Rathnayake</strong>
                                <div class="cell-sub">072 345 6789</div>
                            </div>
                        </div>
                    </td>
                    <td>Dr. Sanduni Fernando</td>
                    <td>Braces Fitting</td>
                    <td>01 Sep 2026<div class="cell-sub">10:45 AM</div></td>
                    <td><span class="badge badge--warning">Booked</span></td>
                    <td class="text-right"><a class="link" href="${ctx}/admin/appointments">View</a></td>
                </tr>

                <tr>
                    <td><span class="mono">APT-20260902-001</span></td>
                    <td>
                        <div class="patient-cell">
                            <span class="avatar">RP</span>
                            <div>
                                <strong>Ruwan Perera</strong>
                                <div class="cell-sub">076 123 4567</div>
                            </div>
                        </div>
                    </td>
                    <td>Dr. Kasun Silva</td>
                    <td>Tooth Extraction</td>
                    <td>02 Sep 2026<div class="cell-sub">08:00 AM</div></td>
                    <td><span class="badge badge--success">Completed</span></td>
                    <td class="text-right"><a class="link" href="${ctx}/admin/billing">Bill</a></td>
                </tr>

                <tr>
                    <td><span class="mono">APT-20260902-002</span></td>
                    <td>
                        <div class="patient-cell">
                            <span class="avatar">NW</span>
                            <div>
                                <strong>Nadeesha Wijeratne</strong>
                                <div class="cell-sub">077 987 6543</div>
                            </div>
                        </div>
                    </td>
                    <td>Dr. Anura Jayasinghe</td>
                    <td>Filling</td>
                    <td>02 Sep 2026<div class="cell-sub">11:30 AM</div></td>
                    <td><span class="badge badge--success">Completed</span></td>
                    <td class="text-right"><a class="link" href="${ctx}/admin/billing">Bill</a></td>
                </tr>

                <tr>
                    <td><span class="mono">APT-20260903-001</span></td>
                    <td>
                        <div class="patient-cell">
                            <span class="avatar">MB</span>
                            <div>
                                <strong>Malith Bandara</strong>
                                <div class="cell-sub">070 555 1212</div>
                            </div>
                        </div>
                    </td>
                    <td>Dr. Malsha Weerasinghe</td>
                    <td>Consultation</td>
                    <td>03 Sep 2026<div class="cell-sub">02:30 PM</div></td>
                    <td><span class="badge badge--danger">Cancelled</span></td>
                    <td class="text-right"><a class="link" href="${ctx}/admin/appointments">View</a></td>
                </tr>

                <tr>
                    <td><span class="mono">APT-20260903-002</span></td>
                    <td>
                        <div class="patient-cell">
                            <span class="avatar">IS</span>
                            <div>
                                <strong>Ishara Senanayake</strong>
                                <div class="cell-sub">075 444 8899</div>
                            </div>
                        </div>
                    </td>
                    <td>Dr. Kasun Silva</td>
                    <td>Root Canal</td>
                    <td>03 Sep 2026<div class="cell-sub">09:00 AM</div></td>
                    <td><span class="badge badge--warning">Booked</span></td>
                    <td class="text-right"><a class="link" href="${ctx}/admin/appointments">View</a></td>
                </tr>

                </tbody>
            </table>
        </div>
    </section>

    <!-- ---------- side column ---------- -->
    <div class="side-column">

        <section class="panel">
            <header class="panel__head">
                <h3>Today's Schedule</h3>
                <span class="badge badge--muted">Dr. Anura</span>
            </header>
            <div class="panel__body">
                <ul class="slot-list">
                    <li class="slot slot--done">
                        <span class="slot__time">09:00</span>
                        <span class="slot__text"><strong>Saman Kumara</strong><br><small>Scaling</small></span>
                    </li>
                    <li class="slot slot--done">
                        <span class="slot__time">09:30</span>
                        <span class="slot__text"><strong>Kavindu Alwis</strong><br><small>Consultation</small></span>
                    </li>
                    <li class="slot slot--now">
                        <span class="slot__time">10:00</span>
                        <span class="slot__text"><strong>Hasini Gamage</strong><br><small>Filling &middot; in progress</small></span>
                    </li>
                    <li class="slot slot--free">
                        <span class="slot__time">10:30</span>
                        <span class="slot__text">Available</span>
                    </li>
                    <li class="slot">
                        <span class="slot__time">11:00</span>
                        <span class="slot__text"><strong>Ruwan Perera</strong><br><small>X-Ray</small></span>
                    </li>
                    <li class="slot slot--free">
                        <span class="slot__time">11:30</span>
                        <span class="slot__text">Available</span>
                    </li>
                </ul>
            </div>
        </section>

        <section class="panel">
            <header class="panel__head">
                <h3>Quick Actions</h3>
            </header>
            <div class="panel__body">
                <div class="quick-actions">

                    <%-- Front desk actions, shown to receptionists only. --%>
                    <c:if test="${not sessionScope.user.admin}">
                        <a class="quick-action" href="${ctx}/admin/appointments/new">
                            <span class="quick-action__icon">&#43;</span>
                            <span>
                                <strong>Register Appointment</strong>
                                <small>Add a new patient visit</small>
                            </span>
                        </a>
                    </c:if>

                    <a class="quick-action" href="${ctx}/admin/appointments">
                        <span class="quick-action__icon">&#128269;</span>
                        <span>
                            <strong>Find Appointment</strong>
                            <small>Search by appointment number</small>
                        </span>
                    </a>

                    <c:if test="${not sessionScope.user.admin}">
                        <a class="quick-action" href="${ctx}/admin/billing">
                            <span class="quick-action__icon">&#8377;</span>
                            <span>
                                <strong>Print a Bill</strong>
                                <small>Calculate and print a receipt</small>
                            </span>
                        </a>
                    </c:if>

                    <%-- Clinic management actions, shown to administrators only. --%>
                    <c:if test="${sessionScope.user.admin}">
                        <a class="quick-action" href="${ctx}/admin/doctors">
                            <span class="quick-action__icon">&#9877;</span>
                            <span>
                                <strong>Manage Dentists</strong>
                                <small>Add a dentist or change a fee</small>
                            </span>
                        </a>
                        <a class="quick-action" href="${ctx}/admin/reports">
                            <span class="quick-action__icon">&#9650;</span>
                            <span>
                                <strong>View Reports</strong>
                                <small>Workload and revenue</small>
                            </span>
                        </a>
                    </c:if>

                    <a class="quick-action" href="${ctx}/admin/help">
                        <span class="quick-action__icon">?</span>
                        <span>
                            <strong>Help Guide</strong>
                            <small>Step by step instructions</small>
                        </span>
                    </a>
                </div>
            </div>
        </section>

    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
