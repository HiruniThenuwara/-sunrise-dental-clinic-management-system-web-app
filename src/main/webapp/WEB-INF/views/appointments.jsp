<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Appointment list and search (Requirement 3).

    The search box looks up an appointment by its number, which is the exact
    lookup described in the scenario. The filters beside it help staff answer
    everyday questions such as "who is coming today?".
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
    <div>
        <h2 class="page-head__title">All Appointments</h2>
        <p class="page-head__sub">Search by appointment number, or filter by dentist, status and date.</p>
    </div>
    <%-- Booking is front desk work, so the button is for receptionists only. --%>
    <c:if test="${not sessionScope.user.admin}">
        <a class="btn btn--primary" href="${ctx}/admin/appointments/new">+ New Appointment</a>
    </c:if>
</div>


<!-- ================= search by appointment number ================= -->
<section class="panel search-panel">
    <div class="panel__body">
        <form class="search-row" onsubmit="showToast('This feature is not available in this version yet.', 'info'); return false;">
            <div class="form-field form-field--grow">
                <label for="searchNo">Search by Appointment Number</label>
                <input class="input" type="search" id="searchNo" placeholder="APT-20260901-001">
            </div>
            <div class="form-field">
                <label for="filterDentist">Dentist</label>
                <select class="input" id="filterDentist">
                    <option>All dentists</option>
                    <option>Dr. Anura Jayasinghe</option>
                    <option>Dr. Sanduni Fernando</option>
                    <option>Dr. Kasun Silva</option>
                    <option>Dr. Malsha Weerasinghe</option>
                </select>
            </div>
            <div class="form-field">
                <label for="filterStatus">Status</label>
                <select class="input" id="filterStatus">
                    <option>All statuses</option>
                    <option>Booked</option>
                    <option>Completed</option>
                    <option>Cancelled</option>
                    <option>No show</option>
                </select>
            </div>
            <div class="form-field">
                <label for="filterDate">Date</label>
                <input class="input" type="date" id="filterDate">
            </div>
            <button class="btn btn--primary" type="submit">Search</button>
        </form>
    </div>
</section>

<!-- ================= results ================= -->
<section class="panel">
    <header class="panel__head">
        <h3>6 Appointments</h3>
        <div class="filters">
            <span class="badge badge--warning">3 Booked</span>
            <span class="badge badge--success">2 Completed</span>
            <span class="badge badge--danger">1 Cancelled</span>
        </div>
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
                        <div><strong>Saman Kumara</strong><div class="cell-sub">071 234 5678</div></div>
                    </div>
                </td>
                <td>Dr. Anura Jayasinghe</td>
                <td>Scaling</td>
                <td>01 Sep 2026<div class="cell-sub">09:00 AM</div></td>
                <td><span class="badge badge--warning">Booked</span></td>
                <td class="text-right"><a class="link" href="${ctx}/admin/appointments/view">View</a></td>
            </tr>

            <tr>
                <td><span class="mono">APT-20260901-002</span></td>
                <td>
                    <div class="patient-cell">
                        <span class="avatar">DR</span>
                        <div><strong>Dilini Rathnayake</strong><div class="cell-sub">072 345 6789</div></div>
                    </div>
                </td>
                <td>Dr. Sanduni Fernando</td>
                <td>Braces Fitting</td>
                <td>01 Sep 2026<div class="cell-sub">10:45 AM</div></td>
                <td><span class="badge badge--warning">Booked</span></td>
                <td class="text-right"><a class="link" href="${ctx}/admin/appointments/view">View</a></td>
            </tr>

            <tr>
                <td><span class="mono">APT-20260902-001</span></td>
                <td>
                    <div class="patient-cell">
                        <span class="avatar">RP</span>
                        <div><strong>Ruwan Perera</strong><div class="cell-sub">076 123 4567</div></div>
                    </div>
                </td>
                <td>Dr. Kasun Silva</td>
                <td>Tooth Extraction</td>
                <td>02 Sep 2026<div class="cell-sub">08:00 AM</div></td>
                <td><span class="badge badge--success">Completed</span></td>
                <td class="text-right"><c:choose><c:when test="${not sessionScope.user.admin}"><a class="link" href="${ctx}/admin/billing">Bill</a></c:when><c:otherwise><a class="link" href="${ctx}/admin/appointments/view">View</a></c:otherwise></c:choose></td>
            </tr>

            <tr>
                <td><span class="mono">APT-20260902-002</span></td>
                <td>
                    <div class="patient-cell">
                        <span class="avatar">NW</span>
                        <div><strong>Nadeesha Wijeratne</strong><div class="cell-sub">077 987 6543</div></div>
                    </div>
                </td>
                <td>Dr. Anura Jayasinghe</td>
                <td>Filling</td>
                <td>02 Sep 2026<div class="cell-sub">11:30 AM</div></td>
                <td><span class="badge badge--success">Completed</span></td>
                <td class="text-right"><c:choose><c:when test="${not sessionScope.user.admin}"><a class="link" href="${ctx}/admin/billing">Bill</a></c:when><c:otherwise><a class="link" href="${ctx}/admin/appointments/view">View</a></c:otherwise></c:choose></td>
            </tr>

            <tr>
                <td><span class="mono">APT-20260903-001</span></td>
                <td>
                    <div class="patient-cell">
                        <span class="avatar">MB</span>
                        <div><strong>Malith Bandara</strong><div class="cell-sub">070 555 1212</div></div>
                    </div>
                </td>
                <td>Dr. Malsha Weerasinghe</td>
                <td>Consultation</td>
                <td>03 Sep 2026<div class="cell-sub">02:30 PM</div></td>
                <td><span class="badge badge--danger">Cancelled</span></td>
                <td class="text-right"><a class="link" href="${ctx}/admin/appointments/view">View</a></td>
            </tr>

            <tr>
                <td><span class="mono">APT-20260903-002</span></td>
                <td>
                    <div class="patient-cell">
                        <span class="avatar">IS</span>
                        <div><strong>Ishara Senanayake</strong><div class="cell-sub">075 444 8899</div></div>
                    </div>
                </td>
                <td>Dr. Kasun Silva</td>
                <td>Root Canal</td>
                <td>03 Sep 2026<div class="cell-sub">09:00 AM</div></td>
                <td><span class="badge badge--warning">Booked</span></td>
                <td class="text-right"><a class="link" href="${ctx}/admin/appointments/view">View</a></td>
            </tr>

            </tbody>
        </table>
    </div>

    <footer class="panel__foot">
        <p class="hint">Showing 6 of 6 appointments</p>
        <div class="pager">
            <button class="btn btn--ghost btn--sm" type="button" disabled>Previous</button>
            <span class="pager__page">1</span>
            <button class="btn btn--ghost btn--sm" type="button" disabled>Next</button>
        </div>
    </footer>
</section>

<script src="${ctx}/assets/js/ui.js"></script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
