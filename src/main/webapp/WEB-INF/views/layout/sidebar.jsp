<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
    Left navigation menu.

    Each servlet sets request attribute "activePage" so the current item is
    highlighted. Doctor management and reports are shown only to users with
    the ADMIN role - a receptionist does not need them.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<aside class="sidebar">

    <div class="sidebar__brand">
        <span class="sidebar__logo">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none"
                 stroke="currentColor" stroke-width="1.8" stroke-linecap="round"
                 stroke-linejoin="round" aria-hidden="true">
                <path d="M12 5.5c-1.6-1.3-3.4-1.9-5-1.3C4.9 5 4 7.2 4.4 9.9c.3 1.9.9 3.4 1.4 5.1.4 1.4.7 2.9.9 4.1.2 1 1.5 1.2 2 .3.6-1.1 1-2.4 1.3-3.6.4-1.5 1.5-1.5 1.9 0 .3 1.2.7 2.5 1.3 3.6.5.9 1.8.7 2-.3.2-1.2.5-2.7.9-4.1.5-1.7 1.1-3.2 1.4-5.1.4-2.7-.5-4.9-2.6-5.7-1.6-.6-3.4 0-5 1.3Z"/>
            </svg>
        </span>
        <span class="sidebar__name">Sunrise<strong>Dental</strong></span>
    </div>

    <nav class="sidebar__nav">

        <p class="nav-group">Overview</p>

        <a class="nav-link ${activePage eq 'dashboard' ? 'is-active' : ''}"
           href="${ctx}/admin/dashboard">
            <span class="nav-link__icon">&#9632;</span> Dashboard
        </a>

        <p class="nav-group">Appointments</p>

        <a class="nav-link ${activePage eq 'new-appointment' ? 'is-active' : ''}"
           href="${ctx}/admin/appointments/new">
            <span class="nav-link__icon">&#43;</span> New Appointment
        </a>

        <a class="nav-link ${activePage eq 'appointments' ? 'is-active' : ''}"
           href="${ctx}/admin/appointments">
            <span class="nav-link__icon">&#9776;</span> All Appointments
        </a>

        <a class="nav-link ${activePage eq 'schedule' ? 'is-active' : ''}"
           href="${ctx}/admin/schedule">
            <span class="nav-link__icon">&#9200;</span> Schedule &amp; Slots
        </a>

        <p class="nav-group">Clinic</p>

        <c:if test="${sessionScope.user.admin}">
            <a class="nav-link ${activePage eq 'doctors' ? 'is-active' : ''}"
               href="${ctx}/admin/doctors">
                <span class="nav-link__icon">&#9877;</span> Dentists
            </a>
        </c:if>

        <a class="nav-link ${activePage eq 'billing' ? 'is-active' : ''}"
           href="${ctx}/admin/billing">
            <span class="nav-link__icon">&#8377;</span> Billing
        </a>

        <c:if test="${sessionScope.user.admin}">
            <a class="nav-link ${activePage eq 'reports' ? 'is-active' : ''}"
               href="${ctx}/admin/reports">
                <span class="nav-link__icon">&#9650;</span> Reports
            </a>
        </c:if>

        <p class="nav-group">Support</p>

        <a class="nav-link ${activePage eq 'help' ? 'is-active' : ''}"
           href="${ctx}/admin/help">
            <span class="nav-link__icon">?</span> Help
        </a>

    </nav>

    <div class="sidebar__foot">
        <p>Version 0.1.0</p>
        <p class="muted">Sunrise Dental Clinic</p>
    </div>

</aside>
