<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Help section (Requirement 5).

    Written for a new receptionist on their first day, in plain language and
    in the order the tasks actually happen at the front desk.
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
    <div>
        <h2 class="page-head__title">Help &amp; User Guide</h2>
        <p class="page-head__sub">Step by step instructions for new staff members.</p>
    </div>
    <button class="btn btn--ghost" type="button" onclick="window.print()">Print Guide</button>
</div>

<div class="help-layout">

    <!-- ---------- contents ---------- -->
    <nav class="panel help-toc no-print">
        <header class="panel__head"><h3>Contents</h3></header>
        <div class="panel__body">
            <ol class="toc-list">
                <li><a href="#s1">1. Signing in</a></li>
                <li><a href="#s2">2. Registering an appointment</a></li>
                <li><a href="#s3">3. Finding an appointment</a></li>
                <li><a href="#s4">4. Printing a bill</a></li>
                <li><a href="#s5">5. Adding a dentist</a></li>
                <li><a href="#s6">6. Setting working hours</a></li>
                <li><a href="#s7">7. Signing out safely</a></li>
                <li><a href="#s8">8. Common problems</a></li>
            </ol>
        </div>
    </nav>

    <!-- ---------- steps ---------- -->
    <div class="help-body">

        <section class="panel" id="s1">
            <header class="panel__head"><h3>1. Signing in</h3></header>
            <div class="panel__body">
                <ol class="steps">
                    <li>Open <span class="mono">http://localhost:8080/sunrise-dental-clinic/</span> in the browser.</li>
                    <li>Type the username and password given to you by the administrator.</li>
                    <li>Tick <strong>Keep me signed in</strong> only on the clinic computer, never on a shared or public one.</li>
                    <li>Click <strong>Sign In</strong>. You will land on the dashboard.</li>
                </ol>
                <p class="callout callout--warn">
                    Never share your password. Every login is recorded with the date and time,
                    so anything done with your account is traced back to you.
                </p>
            </div>
        </section>

        <section class="panel" id="s2">
            <header class="panel__head"><h3>2. Registering a new appointment</h3></header>
            <div class="panel__body">
                <ol class="steps">
                    <li>Click <strong>New Appointment</strong> in the left menu.</li>
                    <li>The appointment number is filled in automatically. Do not change it.</li>
                    <li>Enter the patient name, address and contact number. All three are required.</li>
                    <li>Choose the dentist and the treatment type.</li>
                    <li>Pick the date, then click a green time slot on the right.</li>
                    <li>Grey slots are already taken, so the system will not let you book them twice.</li>
                    <li>Click <strong>Register Appointment</strong>. Write the appointment number on the patient's card.</li>
                </ol>
                <p class="callout">
                    The contact number must be 10 digits starting with 0, for example 0712345678.
                    A past date cannot be selected.
                </p>
            </div>
        </section>

        <section class="panel" id="s3">
            <header class="panel__head"><h3>3. Finding an appointment</h3></header>
            <div class="panel__body">
                <ol class="steps">
                    <li>Click <strong>All Appointments</strong> in the left menu.</li>
                    <li>Type the appointment number in the search box, for example
                        <span class="mono">APT-20260901-001</span>, and press <strong>Search</strong>.</li>
                    <li>Click <strong>View</strong> to see the full patient and appointment details.</li>
                </ol>
                <p class="callout">
                    If the patient forgot their appointment number, filter by dentist and date instead,
                    then find the patient by name in the list.
                </p>
            </div>
        </section>

        <section class="panel" id="s4">
            <header class="panel__head"><h3>4. Calculating and printing a bill</h3></header>
            <div class="panel__body">
                <ol class="steps">
                    <li>Open the appointment and click <strong>Generate Bill</strong>.</li>
                    <li>Check the consultation fee and treatment cost. They are filled in automatically.</li>
                    <li>Enter a discount only if the dentist has approved one.</li>
                    <li>Choose the payment method: cash, card or insurance.</li>
                    <li>Click <strong>Save Bill</strong>, then <strong>Print Receipt</strong>.</li>
                    <li>Give the printed receipt to the patient.</li>
                </ol>
            </div>
        </section>

        <section class="panel" id="s5">
            <header class="panel__head"><h3>5. Adding a dentist</h3></header>
            <div class="panel__body">
                <ol class="steps">
                    <li>Click <strong>Dentists</strong> in the left menu. Only administrators see this.</li>
                    <li>Click <strong>+ Add Dentist</strong>.</li>
                    <li>Enter the name, specialization, contact number and consultation fee.</li>
                    <li>Click <strong>Save Dentist</strong>.</li>
                    <li>Set the working hours next, otherwise no time slots will appear for that dentist.</li>
                </ol>
            </div>
        </section>

        <section class="panel" id="s6">
            <header class="panel__head"><h3>6. Setting working hours</h3></header>
            <div class="panel__body">
                <ol class="steps">
                    <li>Click <strong>Schedule &amp; Slots</strong> in the left menu.</li>
                    <li>Select the dentist at the top.</li>
                    <li>Turn on the days the dentist works and set the start and end times.</li>
                    <li>Choose the slot length, normally 30 minutes.</li>
                    <li>Click <strong>Save Working Hours</strong>. The slot list on the right updates.</li>
                </ol>
            </div>
        </section>

        <section class="panel" id="s7">
            <header class="panel__head"><h3>7. Signing out safely</h3></header>
            <div class="panel__body">
                <ol class="steps">
                    <li>Click <strong>Sign Out</strong> at the top right before leaving the desk.</li>
                    <li>Confirm when asked.</li>
                </ol>
                <p class="callout callout--warn">
                    Patient records are confidential. Always sign out, even for a short break.
                    The system signs you out automatically after 30 minutes without activity.
                </p>
            </div>
        </section>

        <section class="panel" id="s8">
            <header class="panel__head"><h3>8. Common problems</h3></header>
            <div class="panel__body">
                <dl class="detail-grid">
                    <dt>"Invalid username or password"</dt>
                    <dd>Check the caps lock key. If it still fails, ask the administrator to reset your account.</dd>

                    <dt>"Your session has expired"</dt>
                    <dd>You were away for more than 30 minutes. Sign in again. Nothing is lost.</dd>

                    <dt>The time slot I need is grey</dt>
                    <dd>Another patient already has that time with the same dentist. Choose a different
                        slot, or a different dentist at the same time.</dd>

                    <dt>The page does not load at all</dt>
                    <dd>Check that MySQL is running in the XAMPP control panel, then that Tomcat is started.</dd>

                    <dt>The receipt prints with the menu on it</dt>
                    <dd>Use the <strong>Print Receipt</strong> button on the page rather than the browser
                        print menu, so only the receipt is printed.</dd>
                </dl>
            </div>
        </section>

    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
