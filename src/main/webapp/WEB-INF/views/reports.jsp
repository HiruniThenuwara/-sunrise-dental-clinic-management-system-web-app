<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Reports that help the clinic make decisions.

    Three reports were chosen because each answers a question the clinic
    manager actually asks: how busy are we, which dentist carries the load,
    and where does the money come from.
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
    <div>
        <h2 class="page-head__title">Reports</h2>
        <p class="page-head__sub">Appointment volume, dentist workload and revenue by treatment.</p>
    </div>
    <div class="page-head__actions no-print">
        <button class="btn btn--ghost" type="button" onclick="window.print()">Print Report</button>
        <a class="btn btn--primary" href="${ctx}/admin/reports/pdf">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M12 3.5v11"/><path d="m7.8 10.3 4.2 4.2 4.2-4.2"/><path d="M4.5 19.5h15"/>
            </svg>
            Download PDF
        </a>
    </div>
</div>


<section class="panel no-print">
    <div class="panel__body">
        <form class="search-row" onsubmit="showToast('This feature is not available in this version yet.', 'info'); return false;">
            <div class="form-field">
                <label for="repFrom">From</label>
                <input class="input" type="date" id="repFrom" value="2026-08-01">
            </div>
            <div class="form-field">
                <label for="repTo">To</label>
                <input class="input" type="date" id="repTo" value="2026-08-31">
            </div>
            <div class="form-field form-field--grow">
                <label for="repDentist">Dentist</label>
                <select class="input" id="repDentist">
                    <option>All dentists</option>
                    <option>Dr. Anura Jayasinghe</option>
                    <option>Dr. Sanduni Fernando</option>
                    <option>Dr. Kasun Silva</option>
                    <option>Dr. Malsha Weerasinghe</option>
                </select>
            </div>
            <button class="btn btn--primary" type="submit">Apply</button>
        </form>
    </div>
</section>

<!-- ================= summary ================= -->
<section class="stat-grid">
    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Appointments</p>
            <span class="stat-card__icon stat-card__icon--teal">&#9200;</span>
        </div>
        <p class="stat-card__value">148</p>
        <p class="stat-card__trend trend--up">+12% against July</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Total Revenue</p>
            <span class="stat-card__icon stat-card__icon--amber">&#8377;</span>
        </div>
        <p class="stat-card__value">1.42M</p>
        <p class="stat-card__trend">LKR for August 2026</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Cancellations</p>
            <span class="stat-card__icon stat-card__icon--violet">&times;</span>
        </div>
        <p class="stat-card__value">9</p>
        <p class="stat-card__trend trend--down">6.1% of all bookings</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">New Patients</p>
            <span class="stat-card__icon stat-card__icon--blue">&#9787;</span>
        </div>
        <p class="stat-card__value">27</p>
        <p class="stat-card__trend trend--up">+4 against July</p>
    </article>
</section>

<div class="grid-2">

    <!-- ---------- daily appointments ---------- -->
    <section class="panel">
        <header class="panel__head">
            <h3>Daily Appointments</h3>
            <span class="badge badge--muted">Last 7 days</span>
        </header>
        <div class="panel__body">
            <div class="bar-chart">
                <div class="bar" style="--value: 55%"><span class="bar__value">11</span><span class="bar__label">Mon</span></div>
                <div class="bar" style="--value: 80%"><span class="bar__value">16</span><span class="bar__label">Tue</span></div>
                <div class="bar" style="--value: 70%"><span class="bar__value">14</span><span class="bar__label">Wed</span></div>
                <div class="bar" style="--value: 95%"><span class="bar__value">19</span><span class="bar__label">Thu</span></div>
                <div class="bar" style="--value: 60%"><span class="bar__value">12</span><span class="bar__label">Fri</span></div>
                <div class="bar" style="--value: 45%"><span class="bar__value">9</span><span class="bar__label">Sat</span></div>
                <div class="bar bar--muted" style="--value: 5%"><span class="bar__value">0</span><span class="bar__label">Sun</span></div>
            </div>
            <p class="hint">Thursday is the busiest day. Adding a second dentist on Thursday
               would reduce waiting time, which is one of the problems in the scenario.</p>
        </div>
    </section>

    <!-- ---------- dentist workload ---------- -->
    <section class="panel">
        <header class="panel__head">
            <h3>Dentist Workload</h3>
            <span class="badge badge--muted">August 2026</span>
        </header>
        <div class="table-wrap">
            <table class="table table--compact">
                <thead>
                <tr>
                    <th>Dentist</th>
                    <th class="text-right">Appointments</th>
                    <th class="text-right">Completed</th>
                    <th class="text-right">Revenue (LKR)</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td><strong>Dr. Anura Jayasinghe</strong><div class="cell-sub">General Dentistry</div></td>
                    <td class="text-right">52</td>
                    <td class="text-right">48</td>
                    <td class="text-right mono">386,000.00</td>
                </tr>
                <tr>
                    <td><strong>Dr. Sanduni Fernando</strong><div class="cell-sub">Orthodontics</div></td>
                    <td class="text-right">38</td>
                    <td class="text-right">35</td>
                    <td class="text-right mono">612,500.00</td>
                </tr>
                <tr>
                    <td><strong>Dr. Kasun Silva</strong><div class="cell-sub">Oral Surgery</div></td>
                    <td class="text-right">34</td>
                    <td class="text-right">31</td>
                    <td class="text-right mono">341,000.00</td>
                </tr>
                <tr>
                    <td><strong>Dr. Malsha Weerasinghe</strong><div class="cell-sub">Pediatric Dentistry</div></td>
                    <td class="text-right">24</td>
                    <td class="text-right">22</td>
                    <td class="text-right mono">84,500.00</td>
                </tr>
                </tbody>
            </table>
        </div>
    </section>

</div>

<!-- ---------- revenue by treatment ---------- -->
<section class="panel">
    <header class="panel__head">
        <h3>Revenue by Treatment Type</h3>
        <span class="badge badge--muted">August 2026</span>
    </header>
    <div class="table-wrap">
        <table class="table">
            <thead>
            <tr>
                <th>Treatment</th>
                <th class="text-right">Count</th>
                <th class="text-right">Unit Price (LKR)</th>
                <th class="text-right">Revenue (LKR)</th>
                <th>Share</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td><strong>Braces Fitting</strong></td>
                <td class="text-right">6</td>
                <td class="text-right mono">85,000.00</td>
                <td class="text-right mono">510,000.00</td>
                <td><div class="meter"><span style="width: 100%"></span></div></td>
            </tr>
            <tr>
                <td><strong>Root Canal</strong></td>
                <td class="text-right">14</td>
                <td class="text-right mono">25,000.00</td>
                <td class="text-right mono">350,000.00</td>
                <td><div class="meter"><span style="width: 69%"></span></div></td>
            </tr>
            <tr>
                <td><strong>Crown Fitting</strong></td>
                <td class="text-right">7</td>
                <td class="text-right mono">35,000.00</td>
                <td class="text-right mono">245,000.00</td>
                <td><div class="meter"><span style="width: 48%"></span></div></td>
            </tr>
            <tr>
                <td><strong>Filling</strong></td>
                <td class="text-right">29</td>
                <td class="text-right mono">6,000.00</td>
                <td class="text-right mono">174,000.00</td>
                <td><div class="meter"><span style="width: 34%"></span></div></td>
            </tr>
            <tr>
                <td><strong>Scaling</strong></td>
                <td class="text-right">31</td>
                <td class="text-right mono">4,500.00</td>
                <td class="text-right mono">139,500.00</td>
                <td><div class="meter"><span style="width: 27%"></span></div></td>
            </tr>
            </tbody>
        </table>
    </div>
    <footer class="panel__foot">
        <p class="hint">Braces and root canal treatments produce most of the income even
           though they are the least frequent. That is useful when planning which
           specialisations to expand.</p>
    </footer>
</section>

<script src="${ctx}/assets/js/ui.js"></script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
