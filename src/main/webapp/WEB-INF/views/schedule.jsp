<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Working hours and the slots they produce.

    The week comes from doctor_schedule. The slot list on the right is built
    by SlotService from those hours, with the times that already have an
    appointment marked as taken.
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
    <div>
        <h2 class="page-head__title">Schedule &amp; Time Slots</h2>
        <p class="page-head__sub">
            Set the working hours for each dentist. The bookable times on the
            appointment form are generated from them.
        </p>
    </div>
</div>

<c:if test="${not empty flashSuccess}">
    <div class="alert-bar alert-bar--success"><c:out value="${flashSuccess}"/></div>
</c:if>
<c:if test="${not empty flashError}">
    <div class="alert-bar alert-bar--error"><c:out value="${flashError}"/></div>
</c:if>

<%-- ---------- choose the dentist ---------- --%>
<section class="panel search-panel">
    <div class="panel__body">
        <form class="search-row" method="get" action="${ctx}/admin/schedule">
            <div class="form-field form-field--grow">
                <label for="pickDoctor">Dentist</label>
                <select class="input" id="pickDoctor" name="doctorId" onchange="this.form.submit()">
                    <c:forEach var="doctor" items="${doctors}">
                        <option value="${doctor.doctorId}"
                                ${chosenDoctorId eq doctor.doctorId ? 'selected' : ''}>
                            <c:out value="${doctor.doctorName}"/> -
                            <c:out value="${doctor.specialization}"/>
                            <c:if test="${not doctor.active}"> (inactive)</c:if>
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="form-field">
                <label for="previewDate">Preview slots for</label>
                <input class="input" type="date" id="previewDate" name="date"
                       value="${previewDate}" onchange="this.form.submit()">
            </div>
            <button class="btn btn--ghost" type="submit">Show</button>
        </form>
    </div>
</section>

<div class="grid-2">

    <%-- ---------- the week ---------- --%>
    <section class="panel">
        <header class="panel__head">
            <h3>Working Hours</h3>
            <span class="badge badge--muted">One row per weekday</span>
        </header>

        <div class="table-wrap">
            <table class="table table--compact">
                <thead>
                <tr>
                    <th>Day</th>
                    <th>Start</th>
                    <th>Finish</th>
                    <th>Slot Length</th>
                    <th class="text-right">Slots</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>

                <c:forEach var="entry" items="${week}">
                    <c:set var="day" value="${entry.key}"/>
                    <c:set var="schedule" value="${entry.value}"/>

                    <tr>
                        <form method="post" action="${ctx}/admin/schedule">
                            <input type="hidden" name="doctorId" value="${chosenDoctorId}">
                            <input type="hidden" name="dayOfWeek" value="${day}">

                            <td><strong><c:out value="${dayLabels[day]}"/></strong></td>
                            <td>
                                <input class="input input--time" type="time" name="startTime"
                                       value="${empty schedule ? '09:00' : schedule.startTime}">
                            </td>
                            <td>
                                <input class="input input--time" type="time" name="endTime"
                                       value="${empty schedule ? '17:00' : schedule.endTime}">
                            </td>
                            <td>
                                <select class="input" name="slotDuration">
                                    <c:forEach var="length" items="${[15, 30, 45, 60]}">
                                        <option value="${length}"
                                            ${(empty schedule ? 30 : schedule.slotDurationMinutes) eq length
                                              ? 'selected' : ''}>${length} min</option>
                                    </c:forEach>
                                </select>
                            </td>
                            <td class="text-right">
                                <c:choose>
                                    <c:when test="${empty schedule}"><span class="muted">Closed</span></c:when>
                                    <c:otherwise><strong>${schedule.slotCount}</strong></c:otherwise>
                                </c:choose>
                            </td>
                            <td class="text-right nowrap">
                                <button class="link link--button" type="submit" name="working" value="1">
                                    Save
                                </button>
                                <c:if test="${not empty schedule}">
                                    <button class="link link--button" type="submit" name="working" value="0"
                                            onclick="return confirm('Remove this working day? Existing appointments are not affected.');">
                                        Remove
                                    </button>
                                </c:if>
                            </td>
                        </form>
                    </tr>
                </c:forEach>

                </tbody>
            </table>
        </div>

        <footer class="panel__foot">
            <p class="hint">
                A slot is only offered when the whole visit finishes by the closing
                time. With 45 minute slots a day ending at 17:00 offers 16:15 but
                not 16:30.
            </p>
        </footer>
    </section>

    <%-- ---------- generated slots ---------- --%>
    <div class="side-column">
        <section class="panel">
            <header class="panel__head">
                <h3>Generated Slots</h3>
                <span class="badge badge--muted">${freeCount} free</span>
            </header>
            <div class="panel__body">
                <p class="hint">${previewDate}</p>

                <c:choose>
                    <c:when test="${empty slots}">
                        <div class="empty-state">
                            <p class="empty-state__title">No slots on this day</p>
                            <p class="empty-state__text">
                                This dentist has no working hours saved for the weekday
                                of the chosen date. Add hours on the left and the slots
                                appear here.
                            </p>
                        </div>
                    </c:when>

                    <c:otherwise>
                        <div class="slot-grid">
                            <c:forEach var="slot" items="${slots}">
                                <span class="slot-chip ${slot.available ? '' : 'slot-chip--booked'}">
                                    ${slot.value}
                                </span>
                            </c:forEach>
                        </div>

                        <div class="legend">
                            <span><i class="legend__dot legend__dot--free"></i> Available</span>
                            <span><i class="legend__dot legend__dot--booked"></i> Already booked</span>
                        </div>

                        <p class="hint">
                            These are the exact times the receptionist will see on the
                            appointment form. A booked time cannot be chosen, which is
                            the first defence against double booking.
                        </p>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>
    </div>
</div>

<script src="${ctx}/assets/js/ui.js"></script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
