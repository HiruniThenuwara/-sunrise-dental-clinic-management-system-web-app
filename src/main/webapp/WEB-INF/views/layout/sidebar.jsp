<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
    Left navigation menu.

    Each servlet sets request attribute "activePage" so the current item is
    highlighted. Items that grant access to clinic setup or to other people's
    records are shown to administrators only; the servlets check the role
    again themselves, because hiding a link is not access control.

    The icons are inline SVG rather than font characters or images: they stay
    sharp at any size, they take the colour of the text automatically, and
    they need no extra file to download.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<aside class="sidebar">

    <div class="sidebar__brand">
        <span class="sidebar__logo">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor" aria-hidden="true">
                <path d="M16.8 2.4c-1.5 0-2.5.8-3.4 1.2a3.4 3.4 0 0 1-2.8 0C9.7 3.2 8.7 2.4 7.2 2.4 4.6 2.4 2.6 4.6 2.6 7.9c0 2.3.6 4 1.3 5.8.5 1.3.8 2.5 1 3.7.2 1.1.3 2.1.6 2.9.3.8.9 1.3 1.7 1.3.9 0 1.4-.7 1.7-1.6.3-.9.5-2 .7-3.1.2-1.1.4-2.1.7-2.7.2-.4.4-.6.7-.6s.5.2.7.6c.3.6.5 1.6.7 2.7.2 1.1.4 2.2.7 3.1.3.9.8 1.6 1.7 1.6.8 0 1.4-.5 1.7-1.3.3-.8.4-1.8.6-2.9.2-1.2.5-2.4 1-3.7.7-1.8 1.3-3.5 1.3-5.8 0-3.3-2-5.5-4.6-5.5Z"/>
            </svg>
        </span>
        <span class="sidebar__name">Sunrise<strong>Dental</strong></span>
    </div>

    <nav class="sidebar__nav">

        <p class="nav-group">Overview</p>

        <a class="nav-link ${activePage eq 'dashboard' ? 'is-active' : ''}"
           href="${ctx}/admin/dashboard">
            <span class="nav-link__icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
                     stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <rect x="3" y="3" width="7.5" height="8.5" rx="1.6"/>
                    <rect x="13.5" y="3" width="7.5" height="5" rx="1.6"/>
                    <rect x="13.5" y="10.5" width="7.5" height="10.5" rx="1.6"/>
                    <rect x="3" y="14" width="7.5" height="7" rx="1.6"/>
                </svg>
            </span> Dashboard
        </a>

        <p class="nav-group">Appointments</p>

        <%-- Front desk work. The administrator manages the clinic and does
             not take bookings, so this is shown to receptionists only. --%>
        <c:if test="${not sessionScope.user.admin}">
            <a class="nav-link ${activePage eq 'new-appointment' ? 'is-active' : ''}"
               href="${ctx}/admin/appointments/new">
                <span class="nav-link__icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
                         stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <rect x="3" y="4.5" width="18" height="16.5" rx="2.5"/>
                        <path d="M3 9.5h18M8 2.5v4M16 2.5v4"/>
                        <path d="M12 12.5v5M9.5 15h5"/>
                    </svg>
                </span> New Appointment
            </a>
        </c:if>

        <a class="nav-link ${activePage eq 'appointments' ? 'is-active' : ''}"
           href="${ctx}/admin/appointments">
            <span class="nav-link__icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
                     stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <rect x="3" y="4.5" width="18" height="16.5" rx="2.5"/>
                    <path d="M3 9.5h18M8 2.5v4M16 2.5v4"/>
                    <path d="m8.5 14.5 2 2 4-4"/>
                </svg>
            </span> All Appointments
        </a>

        <%-- Patient records replace the paper files. Both roles need them:
             the receptionist at the desk, the administrator for reporting. --%>
        <a class="nav-link ${activePage eq 'patients' ? 'is-active' : ''}"
           href="${ctx}/admin/patients">
            <span class="nav-link__icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
                     stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <circle cx="9" cy="8" r="3.4"/>
                    <path d="M2.5 20a6.5 6.5 0 0 1 13 0"/>
                    <path d="M16.5 5.2a3.4 3.4 0 0 1 0 5.6M18 20a6.4 6.4 0 0 0-2.2-4.8"/>
                </svg>
            </span> Patients Management
        </a>

        <%-- Working hours decide which slots exist, so only an administrator
             may change them. --%>
        <c:if test="${sessionScope.user.admin}">
            <a class="nav-link ${activePage eq 'schedule' ? 'is-active' : ''}"
               href="${ctx}/admin/schedule">
                <span class="nav-link__icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
                         stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <circle cx="12" cy="12" r="9"/>
                        <path d="M12 6.8V12l3.4 2.1"/>
                    </svg>
                </span> Schedule &amp; Slots
            </a>
        </c:if>

        <p class="nav-group">Clinic</p>

        <c:if test="${sessionScope.user.admin}">
            <a class="nav-link ${activePage eq 'doctors' ? 'is-active' : ''}"
               href="${ctx}/admin/doctors">
                <span class="nav-link__icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
                         stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M6 3v5.5a4.5 4.5 0 0 0 9 0V3"/>
                        <path d="M4 3h3M13.5 3h3"/>
                        <path d="M10.5 13v3a4.5 4.5 0 0 0 9 0v-1.5"/>
                        <circle cx="19.5" cy="12.5" r="2"/>
                    </svg>
                </span> Dentists Management
            </a>
        </c:if>

        <c:if test="${sessionScope.user.admin}">
            <a class="nav-link ${activePage eq 'treatments' ? 'is-active' : ''}"
               href="${ctx}/admin/treatments">
                <span class="nav-link__icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
                         stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <rect x="4.5" y="3.5" width="15" height="17" rx="2.4"/>
                        <path d="M9 3.5h6v3H9z"/>
                        <path d="M12 10.5v5M9.5 13h5"/>
                    </svg>
                </span> Treatments Management
            </a>
        </c:if>

        <%-- Staff accounts grant access to patient records, so only an
             administrator may create or withdraw them. --%>
        <c:if test="${sessionScope.user.admin}">
            <a class="nav-link ${activePage eq 'staff' ? 'is-active' : ''}"
               href="${ctx}/admin/staff">
                <span class="nav-link__icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
                         stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <rect x="2.5" y="4.5" width="19" height="15" rx="2.4"/>
                        <circle cx="8.5" cy="11" r="2.4"/>
                        <path d="M4.8 16.6a4 4 0 0 1 7.4 0"/>
                        <path d="M15 10h4M15 13.5h4"/>
                    </svg>
                </span> Staff Management
            </a>
        </c:if>

        <%-- Taking payment is front desk work, so receptionists only. --%>
        <c:if test="${not sessionScope.user.admin}">
            <a class="nav-link ${activePage eq 'billing' ? 'is-active' : ''}"
               href="${ctx}/admin/billing">
                <span class="nav-link__icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
                         stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M5 3.5h14v17l-2.3-1.6-2.3 1.6-2.4-1.6L9.6 20.5 7.3 19 5 20.5z"/>
                        <path d="M9 8.5h6M9 12h6"/>
                    </svg>
                </span> Billing
            </a>
        </c:if>

        <c:if test="${sessionScope.user.admin}">
            <a class="nav-link ${activePage eq 'reports' ? 'is-active' : ''}"
               href="${ctx}/admin/reports">
                <span class="nav-link__icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
                         stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M3.5 20.5h17"/>
                        <rect x="5" y="12" width="3.6" height="8.5" rx="1"/>
                        <rect x="10.2" y="7" width="3.6" height="13.5" rx="1"/>
                        <rect x="15.4" y="3.5" width="3.6" height="17" rx="1"/>
                    </svg>
                </span> Reports
            </a>
        </c:if>

        <%-- The audit trail. Only an administrator should be able to see
             who did what, and it must not be editable from the interface. --%>
        <c:if test="${sessionScope.user.admin}">
            <a class="nav-link ${activePage eq 'activity' ? 'is-active' : ''}"
               href="${ctx}/admin/activity">
                <span class="nav-link__icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
                         stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M2.5 12h4l2.5-7 4.5 14 2.5-7h5.5"/>
                    </svg>
                </span> Activity Log
            </a>
        </c:if>

        <p class="nav-group">Support</p>

        <a class="nav-link ${activePage eq 'help' ? 'is-active' : ''}"
           href="${ctx}/admin/help">
            <span class="nav-link__icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
                     stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <circle cx="12" cy="12" r="9"/>
                    <path d="M9.6 9.4a2.5 2.5 0 1 1 3.3 2.4c-.6.2-.9.8-.9 1.4v.4"/>
                    <path d="M12 16.8v.1"/>
                </svg>
            </span> Help
        </a>

    </nav>

    <div class="sidebar__foot">

        <%-- Requirement 6, Exit System - always reachable from any page --%>
        <a class="signout-link"
           href="${ctx}/logout"
           onclick="return confirm('Sign out of the system?');">
            <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M14.5 16.5v2a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-13a2 2 0 0 1 2-2h6.5a2 2 0 0 1 2 2v2"/>
                <path d="M9.5 12h11"/>
                <path d="m17 8.5 3.5 3.5-3.5 3.5"/>
            </svg>
            Sign Out
        </a>

        <p class="sidebar__version">Sunrise Dental Clinic</p>
    </div>

</aside>
