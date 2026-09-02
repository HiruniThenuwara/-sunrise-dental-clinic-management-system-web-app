<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Treatment types and their prices, read from the treatments table.

    The appointment form reads the same list when the receptionist chooses a
    treatment, and the billing screen reads the cost from it, so the two
    screens can never disagree about a price.
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
    <div>
        <h2 class="page-head__title">Treatments</h2>
        <p class="page-head__sub">Treatment types offered by the clinic, their duration and cost.</p>
    </div>
    <button class="btn btn--primary" type="button" onclick="openTreatmentModal()">+ Add Treatment</button>
</div>

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
            <c:forEach var="error" items="${errors}"><li><c:out value="${error}"/></li></c:forEach>
        </ul>
    </div>
</c:if>

<section class="stat-grid">
    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Treatment Types</p>
            <span class="stat-card__icon stat-card__icon--teal">&#9873;</span>
        </div>
        <p class="stat-card__value">${totalCount}</p>
        <p class="stat-card__trend">${activeCount} offered for booking</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Inactive</p>
            <span class="stat-card__icon stat-card__icon--violet">&times;</span>
        </div>
        <p class="stat-card__value">${totalCount - activeCount}</p>
        <p class="stat-card__trend">hidden from the appointment form</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Highest Price</p>
            <span class="stat-card__icon stat-card__icon--amber">&#8377;</span>
        </div>
        <p class="stat-card__value">${highestCost}</p>
        <p class="stat-card__trend">LKR</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Longest Treatment</p>
            <span class="stat-card__icon stat-card__icon--blue">&#9200;</span>
        </div>
        <p class="stat-card__value">${longestMinutes}</p>
        <p class="stat-card__trend">minutes</p>
    </article>
</section>

<section class="panel">
    <header class="panel__head">
        <h3>Treatment List</h3>
        <div class="filters">
            <input type="search" class="input input--search" id="treatmentSearch"
                   placeholder="Search treatment name" onkeyup="filterTreatments()">
        </div>
    </header>

    <div class="table-wrap">
        <table class="table" id="treatmentTable">
            <thead>
            <tr>
                <th>Treatment</th>
                <th>Description</th>
                <th class="text-right">Duration</th>
                <th class="text-right">Cost (LKR)</th>
                <th>Status</th>
                <th></th>
            </tr>
            </thead>
            <tbody>

            <c:forEach var="treatment" items="${treatments}">
                <tr>
                    <td><strong><c:out value="${treatment.treatmentName}"/></strong></td>
                    <td class="cell-sub">
                        <c:out value="${empty treatment.description ? '-' : treatment.description}"/>
                    </td>
                    <td class="text-right">${treatment.estimatedMinutes} min</td>
                    <td class="text-right mono">${treatment.baseCost}</td>
                    <td>
                        <c:choose>
                            <c:when test="${treatment.active}">
                                <span class="badge badge--success">Active</span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge badge--muted">Inactive</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td class="text-right nowrap">
                        <a class="link" href="#"
                           onclick="editTreatment(${treatment.treatmentId},
                                   '<c:out value="${treatment.treatmentName}"/>',
                                   '<c:out value="${treatment.description}"/>',
                                   '${treatment.baseCost}',
                                   ${treatment.estimatedMinutes},
                                   ${treatment.active}); return false;">Edit</a>
                        <form method="post" action="${ctx}/admin/treatments" class="inline-form">
                            <input type="hidden" name="action" value="toggle">
                            <input type="hidden" name="treatmentId" value="${treatment.treatmentId}">
                            <input type="hidden" name="active" value="${treatment.active ? 0 : 1}">
                            <button type="submit" class="link link--button">
                                <c:out value="${treatment.active ? 'Deactivate' : 'Activate'}"/>
                            </button>
                        </form>
                    </td>
                </tr>
            </c:forEach>

            <c:if test="${empty treatments}">
                <tr>
                    <td colspan="6">
                        <div class="empty-state">
                            <p class="empty-state__title">No treatments yet</p>
                            <p class="empty-state__text">
                                Add the treatments the clinic offers so they can be chosen
                                on the appointment form.
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
            An inactive treatment stays on old appointments and old bills, but the
            receptionist can no longer choose it for a new appointment. Nothing is
            deleted, so past records and their totals never change.
        </p>
    </footer>
</section>

<%-- ================= add / edit treatment modal ================= --%>
<div class="modal" id="treatmentModal" aria-hidden="true">
    <div class="modal__backdrop" onclick="closeModal('treatmentModal')"></div>

    <div class="modal__box" role="dialog" aria-modal="true" aria-labelledby="treatmentModalTitle">
        <header class="modal__head">
            <h3 id="treatmentModalTitle">Add Treatment</h3>
            <button class="modal__close" type="button" onclick="closeModal('treatmentModal')" aria-label="Close">&times;</button>
        </header>

        <form class="modal__body" method="post" action="${ctx}/admin/treatments">
            <input type="hidden" name="treatmentId" id="trId" value="${formTreatmentId}">

            <div class="form-field">
                <label for="trName">Treatment Name <span class="required">*</span></label>
                <input class="input" type="text" id="trName" name="treatmentName"
                       value="<c:out value='${formName}'/>"
                       placeholder="Root Canal" minlength="3" maxlength="100" required>
                <p class="hint">Each treatment name must be unique.</p>
            </div>

            <div class="form-field">
                <label for="trDescription">Description</label>
                <textarea class="input" id="trDescription" name="description" rows="2"
                          maxlength="255"
                          placeholder="Short description shown to the receptionist"><c:out value="${formDescription}"/></textarea>
            </div>

            <div class="form-row">
                <div class="form-field">
                    <label for="trCost">Base Cost (LKR) <span class="required">*</span></label>
                    <input class="input" type="number" id="trCost" name="baseCost"
                           value="<c:out value='${formCost}'/>"
                           min="0" step="100" placeholder="25000" required>
                    <p class="hint">The dentist's consultation fee is added on top of this.</p>
                </div>
                <div class="form-field">
                    <label for="trMinutes">Duration (minutes) <span class="required">*</span></label>
                    <select class="input" id="trMinutes" name="estimatedMinutes" required>
                        <option value="15">15 minutes</option>
                        <option value="30" selected>30 minutes</option>
                        <option value="45">45 minutes</option>
                        <option value="60">60 minutes</option>
                        <option value="90">90 minutes</option>
                        <option value="120">120 minutes</option>
                    </select>
                </div>
            </div>

            <div class="form-field">
                <label for="trStatus">Status</label>
                <select class="input" id="trStatus" name="status">
                    <option value="1">Active - can be chosen for new appointments</option>
                    <option value="0">Inactive - hidden from the appointment form</option>
                </select>
            </div>

            <footer class="form-actions">
                <button class="btn btn--ghost" type="button" onclick="closeModal('treatmentModal')">Cancel</button>
                <button class="btn btn--primary" type="submit">Save Treatment</button>
            </footer>
        </form>
    </div>
</div>

<script src="${ctx}/assets/js/ui.js"></script>
<script>
    function openTreatmentModal() {
        document.getElementById('treatmentModalTitle').textContent = 'Add Treatment';
        document.getElementById('trId').value = '';
        document.getElementById('trName').value = '';
        document.getElementById('trDescription').value = '';
        document.getElementById('trCost').value = '';
        document.getElementById('trMinutes').value = '30';
        document.getElementById('trStatus').value = '1';
        openModal('treatmentModal');
    }

    function editTreatment(id, name, description, cost, minutes, active) {
        document.getElementById('treatmentModalTitle').textContent = 'Edit Treatment';
        document.getElementById('trId').value = id;
        document.getElementById('trName').value = name;
        document.getElementById('trDescription').value = (description === 'null' ? '' : description);
        document.getElementById('trCost').value = Math.round(Number(cost));
        document.getElementById('trMinutes').value = minutes;
        document.getElementById('trStatus').value = active ? '1' : '0';
        openModal('treatmentModal');
    }

    function filterTreatments() {
        var term = document.getElementById('treatmentSearch').value.toLowerCase();
        document.querySelectorAll('#treatmentTable tbody tr').forEach(function (row) {
            row.style.display = row.textContent.toLowerCase().indexOf(term) > -1 ? '' : 'none';
        });
    }

    <c:if test="${not empty errors}">
    openModal('treatmentModal');
    </c:if>
</script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
