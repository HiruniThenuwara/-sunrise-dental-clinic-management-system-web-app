<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Display appointment details (Requirement 3).

    Shows the complete patient and appointment information for one
    appointment number, which is what the scenario asks for after a search.
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
    <div>
        <h2 class="page-head__title">Appointment <span class="mono">APT-20260901-001</span></h2>
        <p class="page-head__sub">Registered on 28 August 2026 by System Administrator</p>
    </div>
    <div class="page-head__actions">
        <a class="btn btn--ghost" href="${ctx}/admin/appointments">Back to list</a>
        <a class="btn btn--primary" href="${ctx}/admin/billing">Generate Bill</a>
    </div>
</div>

<div class="notice">
    <span class="notice__tag">Sample data</span>
    <span>This record is hardcoded. It is loaded by appointment number from the database on Day 3.</span>
</div>

<div class="status-strip">
    <div class="status-strip__item">
        <span class="status-strip__label">Status</span>
        <span class="badge badge--warning">Booked</span>
    </div>
    <div class="status-strip__item">
        <span class="status-strip__label">Date &amp; Time</span>
        <strong>01 September 2026, 09:00 AM</strong>
    </div>
    <div class="status-strip__item">
        <span class="status-strip__label">Dentist</span>
        <strong>Dr. Anura Jayasinghe</strong>
    </div>
    <div class="status-strip__item">
        <span class="status-strip__label">Estimated Total</span>
        <strong class="amount">LKR 6,000.00</strong>
    </div>
</div>

<div class="grid-2">

    <div class="side-column">

        <section class="panel">
            <header class="panel__head"><h3>Patient Information</h3></header>
            <div class="panel__body">
                <dl class="detail-grid">
                    <dt>Patient Name</dt>
                    <dd>Saman Kumara</dd>

                    <dt>NIC Number</dt>
                    <dd class="mono">199012345678</dd>

                    <dt>Date of Birth</dt>
                    <dd>12 April 1990 <span class="muted">(36 years)</span></dd>

                    <dt>Gender</dt>
                    <dd>Male</dd>

                    <dt>Address</dt>
                    <dd>No 45, Galle Road, Colombo 03</dd>

                    <dt>Contact Number</dt>
                    <dd>071 234 5678</dd>

                    <dt>Email</dt>
                    <dd>saman@gmail.com</dd>
                </dl>
            </div>
        </section>

        <section class="panel">
            <header class="panel__head"><h3>Appointment Information</h3></header>
            <div class="panel__body">
                <dl class="detail-grid">
                    <dt>Appointment Number</dt>
                    <dd class="mono">APT-20260901-001</dd>

                    <dt>Dentist</dt>
                    <dd>Dr. Anura Jayasinghe <span class="muted">(General Dentistry)</span></dd>

                    <dt>Treatment Type</dt>
                    <dd>Scaling</dd>

                    <dt>Appointment Date</dt>
                    <dd>Tuesday, 01 September 2026</dd>

                    <dt>Appointment Time</dt>
                    <dd>09:00 AM <span class="muted">(45 minutes)</span></dd>

                    <dt>Status</dt>
                    <dd><span class="badge badge--warning">Booked</span></dd>

                    <dt>Notes</dt>
                    <dd>Regular cleaning. Patient reports mild sensitivity on the lower left side.</dd>
                </dl>
            </div>
        </section>

    </div>

    <div class="side-column">

        <section class="panel">
            <header class="panel__head"><h3>Cost Breakdown</h3></header>
            <div class="panel__body">
                <ul class="summary-list">
                    <li><span>Consultation fee</span><strong>LKR 1,500.00</strong></li>
                    <li><span>Scaling</span><strong>LKR 4,500.00</strong></li>
                    <li><span>Discount</span><strong>LKR 0.00</strong></li>
                    <li class="summary-list__total"><span>Total</span><strong>LKR 6,000.00</strong></li>
                </ul>
                <a class="btn btn--primary btn--block" href="${ctx}/admin/billing">Generate Bill</a>
            </div>
        </section>

        <section class="panel">
            <header class="panel__head"><h3>Visit History</h3></header>
            <div class="panel__body">
                <ul class="history-list">
                    <li>
                        <span class="history-list__date">14 Mar 2026</span>
                        <span><strong>Filling</strong><br><small>Dr. Anura Jayasinghe &middot; LKR 7,500</small></span>
                    </li>
                    <li>
                        <span class="history-list__date">02 Nov 2025</span>
                        <span><strong>Consultation</strong><br><small>Dr. Anura Jayasinghe &middot; LKR 1,500</small></span>
                    </li>
                    <li>
                        <span class="history-list__date">18 Jun 2025</span>
                        <span><strong>Scaling</strong><br><small>Dr. Kasun Silva &middot; LKR 4,500</small></span>
                    </li>
                </ul>
            </div>
        </section>

        <section class="panel">
            <header class="panel__head"><h3>Actions</h3></header>
            <div class="panel__body">
                <div class="quick-actions">
                    <a class="quick-action" href="#" onclick="showToast('Editing is connected on Day 3.', 'info'); return false;">
                        <span class="quick-action__icon">&#9998;</span>
                        <span><strong>Edit Appointment</strong><small>Change dentist, date or time</small></span>
                    </a>
                    <a class="quick-action" href="#" onclick="showToast('Status change is connected on Day 3.', 'info'); return false;">
                        <span class="quick-action__icon">&#10003;</span>
                        <span><strong>Mark as Completed</strong><small>After the patient is treated</small></span>
                    </a>
                    <a class="quick-action" href="#" onclick="if (confirm('Cancel this appointment?')) { showToast('Cancellation is connected on Day 3.', 'info'); } return false;">
                        <span class="quick-action__icon">&times;</span>
                        <span><strong>Cancel Appointment</strong><small>Frees the time slot again</small></span>
                    </a>
                </div>
            </div>
        </section>

    </div>
</div>

<script src="${ctx}/assets/js/ui.js"></script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
