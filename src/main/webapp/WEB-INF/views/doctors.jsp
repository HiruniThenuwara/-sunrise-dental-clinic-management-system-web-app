<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Dentist management.

    The rows come from the doctors table through DoctorService and DoctorDao.
    One modal serves both adding and editing: the Edit link fills its fields
    from the row, and the hidden doctorId decides which of the two happens.
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
    <div>
        <h2 class="page-head__title">Dentists Management</h2>
        <p class="page-head__sub">Add dentists, set their consultation fee and control who can accept bookings.</p>
    </div>
    <button class="btn btn--primary" type="button" onclick="openDoctorModal()">+ Add Dentist</button>
</div>

<%-- ---------- messages ---------- --%>
<c:if test="${not empty flashSuccess}">
    <div class="alert-bar alert-bar--success"><c:out value="${flashSuccess}"/></div>
</c:if>
<c:if test="${not empty flashError}">
    <div class="alert-bar alert-bar--error"><c:out value="${flashError}"/></div>
</c:if>
<c:if test="${not empty errors}">
    <div class="alert-bar alert-bar--error">
        <strong>Please correct the following:</strong>
        <ul>
            <c:forEach var="error" items="${errors}">
                <li><c:out value="${error}"/></li>
            </c:forEach>
        </ul>
    </div>
</c:if>

<%-- ---------- statistic cards ---------- --%>
<section class="stat-grid">
    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Total Dentists</p>
            <span class="stat-card__icon stat-card__icon--teal">&#9877;</span>
        </div>
        <p class="stat-card__value">${totalCount}</p>
        <p class="stat-card__trend">${activeCount} accepting bookings</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Active</p>
            <span class="stat-card__icon stat-card__icon--blue">&#10003;</span>
        </div>
        <p class="stat-card__value">${activeCount}</p>
        <p class="stat-card__trend">available on the appointment form</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Inactive</p>
            <span class="stat-card__icon stat-card__icon--violet">&times;</span>
        </div>
        <p class="stat-card__value">${totalCount - activeCount}</p>
        <p class="stat-card__trend">hidden from new bookings</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Highest Fee</p>
            <span class="stat-card__icon stat-card__icon--amber">&#8377;</span>
        </div>
        <p class="stat-card__value">${highestFeeText}</p>
        <p class="stat-card__trend">LKR consultation fee</p>
    </article>
</section>

<%-- ---------- dentist table ---------- --%>
<section class="panel">
    <header class="panel__head">
        <h3>Registered Dentists</h3>
        <div class="filters">
            <input type="search" class="input input--search" id="doctorSearch"
                   placeholder="Search by name or specialization" onkeyup="filterDoctors()">
        </div>
    </header>

    <div class="table-wrap">
        <table class="table" id="doctorTable" data-no-pager>
            <thead>
            <tr>
                <th>Dentist</th>
                <th>Specialization</th>
                <th>Contact</th>
                <th class="text-right">Consultation Fee</th>
                <th>Status</th>
                <th></th>
            </tr>
            </thead>
            <tbody>

            <c:forEach var="doctor" items="${doctors}">
                <tr>
                    <td>
                        <div class="patient-cell">
                            <span class="avatar"><c:out value="${doctor.initials}"/></span>
                            <div>
                                <strong><c:out value="${doctor.doctorName}"/></strong>
                                <div class="cell-sub">
                                    <c:out value="${empty doctor.email ? 'No email recorded' : doctor.email}"/>
                                </div>
                            </div>
                        </div>
                    </td>
                    <td><c:out value="${doctor.specialization}"/></td>
                    <td><c:out value="${empty doctor.contactNumber ? '-' : doctor.contactNumber}"/></td>
                    <td class="text-right">
                        <strong>LKR ${doctor.formattedFee}</strong>
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${doctor.active}">
                                <span class="badge badge--success">Active</span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge badge--muted">Inactive</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td class="text-right nowrap">
                        <a class="link" href="${ctx}/admin/schedule?doctorId=${doctor.doctorId}">Schedule</a>
                        <a class="link" href="#"
                           onclick="editDoctor(${doctor.doctorId},
                                   '<c:out value="${doctor.doctorName}"/>',
                                   '<c:out value="${doctor.specialization}"/>',
                                   '<c:out value="${doctor.contactNumber}"/>',
                                   '<c:out value="${doctor.email}"/>',
                                   '${doctor.consultationFee}',
                                   ${doctor.active}); return false;">Edit</a>
                        <form method="post" action="${ctx}/admin/doctors" class="inline-form"
                              onsubmit="return confirm('Change the booking status of this dentist?');">
                            <input type="hidden" name="action" value="toggle">
                            <input type="hidden" name="doctorId" value="${doctor.doctorId}">
                            <input type="hidden" name="active" value="${doctor.active ? 0 : 1}">
                            <button type="submit" class="link link--button">
                                <c:out value="${doctor.active ? 'Deactivate' : 'Activate'}"/>
                            </button>
                        </form>
                    </td>
                </tr>
            </c:forEach>

            <c:if test="${empty doctors}">
                <tr>
                    <td colspan="6">
                        <div class="empty-state">
                            <p class="empty-state__title">No dentists yet</p>
                            <p class="empty-state__text">
                                Add the first dentist, then set their working hours so that
                                time slots appear on the appointment form.
                            </p>
                        </div>
                    </td>
                </tr>
            </c:if>

            </tbody>
        </table>
    </div>

    <%-- Page links. The servlet put the page on the request as pageInfo. --%>
    <jsp:include page="/WEB-INF/views/layout/pager.jsp"/>

</section>

<%-- ================= add / edit dentist modal ================= --%>
<div class="modal" id="doctorModal" aria-hidden="true">
    <div class="modal__backdrop" onclick="closeModal('doctorModal')"></div>

    <div class="modal__box" role="dialog" aria-modal="true" aria-labelledby="doctorModalTitle">
        <header class="modal__head">
            <h3 id="doctorModalTitle">Add Dentist</h3>
            <button class="modal__close" type="button" onclick="closeModal('doctorModal')" aria-label="Close">&times;</button>
        </header>

        <form class="modal__body" method="post" action="${ctx}/admin/doctors">
            <input type="hidden" name="doctorId" id="docId" value="${formDoctorId}">

            <div class="form-row">
                <div class="form-field">
                    <label for="docName">Dentist Name <span class="required">*</span></label>
                    <input class="input" type="text" id="docName" name="doctorName"
                           value="<c:out value='${formName}'/>"
                           placeholder="Dr. Anura Jayasinghe" minlength="3" maxlength="100" required>
                </div>
                <div class="form-field">
                    <label for="docSpec">Specialization <span class="required">*</span></label>
                    <select class="input" id="docSpec" name="specialization" required>
                        <option value="">Select specialization</option>
                        <option value="General Dentistry">General Dentistry</option>
                        <option value="Orthodontics">Orthodontics</option>
                        <option value="Oral Surgery">Oral Surgery</option>
                        <option value="Pediatric Dentistry">Pediatric Dentistry</option>
                        <option value="Periodontics">Periodontics</option>
                        <option value="Prosthodontics">Prosthodontics</option>
                        <option value="Endodontics">Endodontics</option>
                    </select>
                </div>
            </div>

            <div class="form-row">
                <div class="form-field">
                    <label for="docPhone">Contact Number</label>
                    <input class="input" type="tel" id="docPhone" name="contactNumber"
                           value="<c:out value='${formContact}'/>"
                           placeholder="0771234567" pattern="0[0-9]{9}" maxlength="10">
                    <p class="hint">10 digits starting with 0</p>
                </div>
                <div class="form-field">
                    <label for="docEmail">Email</label>
                    <input class="input" type="email" id="docEmail" name="email"
                           value="<c:out value='${formEmail}'/>"
                           placeholder="name@sunrisedental.lk">
                </div>
            </div>

            <div class="form-row">
                <div class="form-field">
                    <label for="docFee">Consultation Fee (LKR) <span class="required">*</span></label>
                    <input class="input" type="number" id="docFee" name="consultationFee"
                           value="<c:out value='${formFee}'/>"
                           min="0" step="100" placeholder="1500" required>
                </div>
                <div class="form-field">
                    <label for="docStatus">Status</label>
                    <select class="input" id="docStatus" name="status">
                        <option value="1">Active - can accept bookings</option>
                        <option value="0">Inactive - hidden from booking</option>
                    </select>
                </div>
            </div>

            <footer class="form-actions">
                <button class="btn btn--ghost" type="button" onclick="closeModal('doctorModal')">Cancel</button>
                <button class="btn btn--primary" type="submit">Save Dentist</button>
            </footer>
        </form>
    </div>
</div>

<script src="${ctx}/assets/js/ui.js"></script>
<script>
    /* Opens the modal ready for a new dentist. */
    function openDoctorModal() {
        document.getElementById('doctorModalTitle').textContent = 'Add Dentist';
        document.getElementById('docId').value = '';
        document.getElementById('docName').value = '';
        document.getElementById('docSpec').value = '';
        document.getElementById('docPhone').value = '';
        document.getElementById('docEmail').value = '';
        document.getElementById('docFee').value = '';
        document.getElementById('docStatus').value = '1';
        openModal('doctorModal');
    }

    /* Opens the same modal filled with an existing dentist. */
    function editDoctor(id, name, specialization, phone, email, fee, active) {
        document.getElementById('doctorModalTitle').textContent = 'Edit Dentist';
        document.getElementById('docId').value = id;
        document.getElementById('docName').value = name;
        document.getElementById('docSpec').value = specialization;
        document.getElementById('docPhone').value = (phone === 'null' ? '' : phone);
        document.getElementById('docEmail').value = (email === 'null' ? '' : email);
        document.getElementById('docFee').value = Math.round(Number(fee));
        document.getElementById('docStatus').value = active ? '1' : '0';
        openModal('doctorModal');
    }

    /* Filters the table in the browser, without contacting the server. */
    function filterDoctors() {
        var term = document.getElementById('doctorSearch').value.toLowerCase();
        document.querySelectorAll('#doctorTable tbody tr').forEach(function (row) {
            row.style.display = row.textContent.toLowerCase().indexOf(term) > -1 ? '' : 'none';
        });
    }

    <c:if test="${not empty errors}">
    // The form was rejected, so reopen it with the typed values still there.
    openModal('doctorModal');
    </c:if>
</script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
