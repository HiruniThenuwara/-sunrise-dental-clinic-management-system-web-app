<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Patient records.

    This is the screen that replaces the paper files described in the
    scenario. Every patient who has ever been registered appears here with
    their visit history summarised, and can be found by name, telephone
    number or NIC.
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
    <div>
        <h2 class="page-head__title">Patients Management</h2>
        <p class="page-head__sub">Everyone registered at the clinic, with their visit history.</p>
    </div>
    <c:if test="${not sessionScope.user.admin}">
        <a class="btn btn--primary" href="${ctx}/admin/appointments/new">+ New Appointment</a>
    </c:if>
</div>

<c:if test="${not empty flashError}">
    <div class="alert-bar alert-bar--error"><c:out value="${flashError}"/></div>
</c:if>

<section class="stat-grid">
    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Registered Patients</p>
            <span class="stat-card__icon stat-card__icon--teal">&#9787;</span>
        </div>
        <p class="stat-card__value">${totalCount}</p>
        <p class="stat-card__trend">on file at the clinic</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">With Upcoming Visits</p>
            <span class="stat-card__icon stat-card__icon--blue">&#9200;</span>
        </div>
        <p class="stat-card__value">${withUpcoming}</p>
        <p class="stat-card__trend">have an appointment booked</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">First Time Patients</p>
            <span class="stat-card__icon stat-card__icon--violet">&#10022;</span>
        </div>
        <p class="stat-card__value">${newPatients}</p>
        <p class="stat-card__trend">one visit or fewer</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Total Billed</p>
            <span class="stat-card__icon stat-card__icon--amber">&#8377;</span>
        </div>
        <p class="stat-card__value">${totalBilled}</p>
        <p class="stat-card__trend">LKR across all patients</p>
    </article>
</section>

<%-- ---------- search ---------- --%>
<section class="panel search-panel">
    <div class="panel__body">
        <form class="search-row" method="get" action="${ctx}/admin/patients">
            <div class="form-field form-field--grow">
                <label for="patientSearch">Find a patient</label>
                <input class="input" type="search" id="patientSearch" name="search"
                       value="<c:out value='${search}'/>"
                       placeholder="Name, telephone number or NIC">
            </div>
            <button class="btn btn--primary" type="submit">Search</button>
            <c:if test="${not empty search}">
                <a class="btn btn--ghost" href="${ctx}/admin/patients">Clear</a>
            </c:if>
        </form>
    </div>
</section>

<%-- ---------- the list ---------- --%>
<section class="panel">
    <header class="panel__head">
        <h3>
            <c:choose>
                <c:when test="${not empty search}">Search Results</c:when>
                <c:otherwise>All Patients</c:otherwise>
            </c:choose>
        </h3>
        <span class="badge badge--muted">${totalCount} shown</span>
    </header>

    <div class="table-wrap">
        <table class="table">
            <thead>
            <tr>
                <th>Patient</th>
                <th>Contact</th>
                <th>Address</th>
                <th class="text-right">Visits</th>
                <th>Last Visit</th>
                <th>Next Visit</th>
                <th class="text-right">Billed (LKR)</th>
                <th></th>
            </tr>
            </thead>
            <tbody>

            <c:forEach var="row" items="${patients}">
                <tr>
                    <td>
                        <div class="patient-cell">
                            <span class="avatar"><c:out value="${row.patient.initials}"/></span>
                            <div>
                                <strong><c:out value="${row.patient.patientName}"/></strong>
                                <div class="cell-sub">
                                    <c:choose>
                                        <c:when test="${empty row.patient.nic}">No NIC recorded</c:when>
                                        <c:otherwise><c:out value="${row.patient.nic}"/></c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>
                    </td>
                    <td>
                        <c:out value="${row.patient.formattedContactNumber}"/>
                        <div class="cell-sub">
                            <c:out value="${empty row.patient.email ? 'No email' : row.patient.email}"/>
                        </div>
                    </td>
                    <td class="cell-sub"><c:out value="${row.patient.address}"/></td>
                    <td class="text-right">
                        <strong>${row.visitCount}</strong>
                        <c:if test="${row.cancelledCount gt 0}">
                            <div class="cell-sub">${row.cancelledCount} cancelled</div>
                        </c:if>
                    </td>
                    <td><c:out value="${row.formattedLastVisit}"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${row.upcoming}">
                                <span class="badge badge--warning">
                                    <c:out value="${row.formattedNextVisit}"/>
                                </span>
                            </c:when>
                            <c:otherwise><span class="muted">None booked</span></c:otherwise>
                        </c:choose>
                    </td>
                    <td class="text-right mono">${row.formattedTotal}</td>
                    <td class="text-right">
                        <a class="link" href="${ctx}/admin/patients?id=${row.patient.patientId}">
                            Open Record
                        </a>
                    </td>
                </tr>
            </c:forEach>

            <c:if test="${empty patients}">
                <tr>
                    <td colspan="8">
                        <div class="empty-state">
                            <p class="empty-state__title">
                                <c:choose>
                                    <c:when test="${not empty search}">No patient matched</c:when>
                                    <c:otherwise>No patients registered yet</c:otherwise>
                                </c:choose>
                            </p>
                            <p class="empty-state__text">
                                <c:choose>
                                    <c:when test="${not empty search}">
                                        Try part of the name, or the telephone number.
                                    </c:when>
                                    <c:otherwise>
                                        A patient record is created automatically the first time
                                        an appointment is registered for them.
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

    <footer class="panel__foot">
        <p class="hint">
            A returning patient is matched on their telephone number when a new
            appointment is registered, so the same person is never stored twice.
        </p>
    </footer>
</section>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
