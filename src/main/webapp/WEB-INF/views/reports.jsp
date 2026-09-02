<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Management reports.

    Every figure comes from a GROUP BY query in ReportDao, so the totals are
    calculated by the database rather than by counting rows in Java.
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
    <div>
        <h2 class="page-head__title">Reports</h2>
        <p class="page-head__sub">
            ${from} to ${to} &middot; appointment volume, dentist workload and revenue.
        </p>
    </div>
    <div class="page-head__actions no-print">
        <button class="btn btn--ghost" type="button" onclick="window.print()">Print Report</button>
        <a class="btn btn--primary" href="${ctx}/admin/reports/pdf?from=${from}&to=${to}">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M12 3.5v11"/><path d="m7.8 10.3 4.2 4.2 4.2-4.2"/><path d="M4.5 19.5h15"/>
            </svg>
            Download PDF
        </a>
    </div>
</div>

<%-- ---------- period ---------- --%>
<section class="panel search-panel no-print">
    <div class="panel__body">
        <form class="search-row" method="get" action="${ctx}/admin/reports">
            <div class="form-field">
                <label for="from">From</label>
                <input class="input" type="date" id="from" name="from" value="${from}">
            </div>
            <div class="form-field">
                <label for="to">To</label>
                <input class="input" type="date" id="to" name="to" value="${to}">
            </div>
            <button class="btn btn--primary" type="submit">Apply</button>
        </form>
    </div>
</section>

<%-- ---------- headline figures ---------- --%>
<section class="stat-grid">
    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Appointments</p>
            <span class="stat-card__icon stat-card__icon--teal">&#9200;</span>
        </div>
        <p class="stat-card__value">${summary.formattedAppointments}</p>
        <p class="stat-card__trend">${summary.completed} completed</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Total Revenue</p>
            <span class="stat-card__icon stat-card__icon--amber">&#8377;</span>
        </div>
        <p class="stat-card__value">${summary.formattedRevenue}</p>
        <p class="stat-card__trend">LKR from bills issued</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Cancellations</p>
            <span class="stat-card__icon stat-card__icon--violet">&times;</span>
        </div>
        <p class="stat-card__value">${summary.cancelled}</p>
        <p class="stat-card__trend">${summary.cancellationRate}% of all bookings</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">New Patients</p>
            <span class="stat-card__icon stat-card__icon--blue">&#9787;</span>
        </div>
        <p class="stat-card__value">${summary.newPatients}</p>
        <p class="stat-card__trend">registered in this period</p>
    </article>
</section>

<div class="grid-2">

    <%-- ---------- appointments per day ---------- --%>
    <section class="panel">
        <header class="panel__head">
            <h3>Daily Appointments</h3>
            <span class="badge badge--muted">${from} to ${to}</span>
        </header>
        <div class="panel__body">
            <c:choose>
                <c:when test="${empty dailyRows}">
                    <div class="empty-state">
                        <p class="empty-state__title">No appointments in this period</p>
                        <p class="empty-state__text">Choose a different date range above.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="bar-chart">
                        <c:forEach var="row" items="${dailyRows}">
                            <div class="bar" style="--value: ${row.sharePercent}%"
                                 title="${row.label}: ${row.count} appointments">
                                <span class="bar__value">${row.count}</span>
                                <span class="bar__label"><c:out value="${row.subLabel}"/></span>
                            </div>
                        </c:forEach>
                    </div>
                    <p class="hint">
                        The tallest bar is the clinic's busiest day. Adding a second
                        dentist on that weekday is the most direct way to reduce the
                        waiting times described in the scenario.
                    </p>
                </c:otherwise>
            </c:choose>
        </div>
    </section>

    <%-- ---------- dentist workload ---------- --%>
    <section class="panel">
        <header class="panel__head">
            <h3>Dentist Workload</h3>
            <span class="badge badge--muted">${from} to ${to}</span>
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
                <c:forEach var="row" items="${doctorRows}">
                    <tr>
                        <td><strong><c:out value="${row.label}"/></strong>
                            <div class="cell-sub"><c:out value="${row.subLabel}"/></div></td>
                        <td class="text-right">${row.count}</td>
                        <td class="text-right">${row.secondaryCount}</td>
                        <td class="text-right mono">${row.formattedAmount}</td>
                    </tr>
                </c:forEach>
                <c:if test="${empty doctorRows}">
                    <tr><td colspan="4" class="muted">No dentists to report on.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </section>

</div>

<%-- ---------- revenue by treatment ---------- --%>
<section class="panel">
    <header class="panel__head">
        <h3>Revenue by Treatment Type</h3>
        <span class="badge badge--muted">${from} to ${to}</span>
    </header>
    <div class="table-wrap">
        <table class="table">
            <thead>
            <tr>
                <th>Treatment</th>
                <th class="text-right">Count</th>
                <th class="text-right">Revenue (LKR)</th>
                <th>Share</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="row" items="${treatmentRows}">
                <tr>
                    <td><strong><c:out value="${row.label}"/></strong>
                        <div class="cell-sub"><c:out value="${row.subLabel}"/></div></td>
                    <td class="text-right">${row.count}</td>
                    <td class="text-right mono">${row.formattedAmount}</td>
                    <td><div class="meter"><span style="width: ${row.sharePercent}%"></span></div></td>
                </tr>
            </c:forEach>
            <c:if test="${empty treatmentRows}">
                <tr><td colspan="4" class="muted">No billed treatments in this period.</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
    <footer class="panel__foot">
        <p class="hint">
            Revenue is taken from the bills that were actually issued, including any
            discount given, so it reflects money taken rather than list prices.
        </p>
    </footer>
</section>

<script src="${ctx}/assets/js/ui.js"></script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
