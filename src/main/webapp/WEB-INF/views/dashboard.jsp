<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Day 1 version of the dashboard.

    The layout, cards and table styles are in place. The statistic values
    and the recent appointment rows are filled with live database data on
    Day 2 and Day 3.
--%>

<div class="welcome-card">
    <div>
        <h2>Welcome back, <c:out value="${sessionScope.user.firstName}"/>.</h2>
        <p>You are signed in as <strong><c:out value="${sessionScope.user.role.displayName}"/></strong>.
           Use the menu on the left to register an appointment or find a patient record.</p>
    </div>
    <a class="btn btn--primary" href="${pageContext.request.contextPath}/admin/appointments/new">
        + New Appointment
    </a>
</div>

<section class="stat-grid">

    <article class="stat-card">
        <p class="stat-card__label">Today's Appointments</p>
        <p class="stat-card__value">--</p>
        <p class="stat-card__note">Connected on Day 3</p>
    </article>

    <article class="stat-card">
        <p class="stat-card__label">Registered Patients</p>
        <p class="stat-card__value">--</p>
        <p class="stat-card__note">Connected on Day 3</p>
    </article>

    <article class="stat-card">
        <p class="stat-card__label">Dentists On Duty</p>
        <p class="stat-card__value">--</p>
        <p class="stat-card__note">Connected on Day 3</p>
    </article>

    <article class="stat-card">
        <p class="stat-card__label">Today's Revenue</p>
        <p class="stat-card__value">--</p>
        <p class="stat-card__note">Connected on Day 3</p>
    </article>

</section>

<section class="panel">
    <header class="panel__head">
        <h3>Recent Appointments</h3>
        <span class="badge badge--muted">Day 2</span>
    </header>

    <div class="panel__body">
        <div class="empty-state">
            <p class="empty-state__title">No data connected yet</p>
            <p class="empty-state__text">
                The appointment table is built on Day 2 and connected to the
                database on Day 3. Login, session handling and the protected
                admin layout are complete.
            </p>
        </div>
    </div>
</section>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
