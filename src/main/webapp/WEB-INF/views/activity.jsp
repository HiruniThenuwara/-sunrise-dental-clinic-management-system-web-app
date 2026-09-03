<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Activity log - who did what, and when.

    Entries are written by the servlets as staff work. Nothing on this page
    can change or remove an entry: a log that can be edited is not an audit
    trail.
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
    <div>
        <h2 class="page-head__title">Activity Log</h2>
        <p class="page-head__sub">Every sign in, booking, bill and account change, newest first.</p>
    </div>
    <button class="btn btn--ghost no-print" type="button" onclick="window.print()">Print Log</button>
</div>

<%-- ---------- headline figures ---------- --%>
<section class="stat-grid">
    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Actions Today</p>
            <span class="stat-card__icon stat-card__icon--teal">&#9998;</span>
        </div>
        <p class="stat-card__value">${countToday}</p>
        <p class="stat-card__trend">recorded since midnight</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Failed Sign Ins</p>
            <span class="stat-card__icon stat-card__icon--amber">&#9888;</span>
        </div>
        <p class="stat-card__value">${failedLogins}</p>
        <p class="stat-card__trend">in the last 7 days</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Appointments Booked</p>
            <span class="stat-card__icon stat-card__icon--blue">&#9200;</span>
        </div>
        <p class="stat-card__value">${bookingsThisWeek}</p>
        <p class="stat-card__trend">in the last 7 days</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Double Bookings Stopped</p>
            <span class="stat-card__icon stat-card__icon--violet">&#128274;</span>
        </div>
        <p class="stat-card__value">${refusedBookings}</p>
        <p class="stat-card__trend">refused in the last 7 days</p>
    </article>
</section>

<%-- ---------- filters ---------- --%>
<section class="panel search-panel no-print">
    <div class="panel__body">
        <form class="search-row" method="get" action="${ctx}/admin/activity">
            <div class="form-field">
                <label for="filterUser">Staff member</label>
                <select class="input" id="filterUser" name="username">
                    <option value="">Everyone</option>
                    <c:forEach var="name" items="${knownUsernames}">
                        <option value="${name}" ${filterUsername eq name ? 'selected' : ''}>
                            <c:out value="${name}"/>
                        </option>
                    </c:forEach>
                </select>
            </div>

            <div class="form-field form-field--grow">
                <label for="filterAction">Action</label>
                <select class="input" id="filterAction" name="action">
                    <option value="">All actions</option>
                    <c:forEach var="a" items="${allActions}">
                        <option value="${a}" ${filterAction eq a.toString() ? 'selected' : ''}>
                            <c:out value="${a.category}"/> &mdash; <c:out value="${a.displayName}"/>
                        </option>
                    </c:forEach>
                </select>
            </div>

            <div class="form-field">
                <label for="filterFrom">From</label>
                <input class="input" type="date" id="filterFrom" name="from" value="${filterFrom}">
            </div>
            <div class="form-field">
                <label for="filterTo">To</label>
                <input class="input" type="date" id="filterTo" name="to" value="${filterTo}">
            </div>

            <button class="btn btn--primary" type="submit">Filter</button>
            <a class="btn btn--ghost" href="${ctx}/admin/activity">Clear</a>
        </form>
    </div>
</section>

<%-- ---------- the log ---------- --%>
<section class="panel">
    <header class="panel__head">
        <h3>
            <c:choose>
                <c:when test="${empty filterUsername and empty filterAction
                                and empty filterFrom and empty filterTo}">Recent Activity</c:when>
                <c:otherwise>Filtered Activity</c:otherwise>
            </c:choose>
        </h3>
        <span class="badge badge--muted">${fn:length(entries)} entries shown</span>
    </header>

    <div class="table-wrap">
        <table class="table">
            <thead>
            <tr>
                <th>When</th>
                <th>Who</th>
                <th>Action</th>
                <th>Record</th>
                <th>Details</th>
                <th>From</th>
            </tr>
            </thead>
            <tbody>

            <c:forEach var="entry" items="${entries}">
                <tr class="${entry.action.noteworthy ? 'row--flagged' : ''}">
                    <td>
                        <strong><c:out value="${entry.relativeTime}"/></strong>
                        <div class="cell-sub"><c:out value="${entry.formattedTime}"/></div>
                    </td>
                    <td>
                        <div class="patient-cell">
                            <span class="avatar"><c:out value="${entry.initials}"/></span>
                            <strong class="mono"><c:out value="${entry.who}"/></strong>
                        </div>
                    </td>
                    <td>
                        <span class="badge badge--${entry.action.badgeStyle}">
                            <c:out value="${entry.action.displayName}"/>
                        </span>
                        <div class="cell-sub"><c:out value="${entry.action.category}"/></div>
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${empty entry.entityRef}"><span class="muted">-</span></c:when>
                            <c:otherwise>
                                <span class="mono"><c:out value="${entry.entityRef}"/></span>
                                <div class="cell-sub"><c:out value="${entry.entity}"/></div>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td class="cell-sub"><c:out value="${entry.details}"/></td>
                    <td class="cell-sub mono"><c:out value="${entry.ipAddress}"/></td>
                </tr>
            </c:forEach>

            <c:if test="${empty entries}">
                <tr>
                    <td colspan="6">
                        <div class="empty-state">
                            <p class="empty-state__title">Nothing recorded yet</p>
                            <p class="empty-state__text">
                                Entries appear here as staff sign in, register appointments
                                and produce bills. If you have just added the log to an
                                existing database, activity from before that point was not
                                recorded.
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
            The log is append only. Entries cannot be edited or deleted from this
            screen, which is what makes it usable as evidence. Failed sign ins and
            changes to who can reach patient records are highlighted.
        </p>
    </footer>
</section>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
