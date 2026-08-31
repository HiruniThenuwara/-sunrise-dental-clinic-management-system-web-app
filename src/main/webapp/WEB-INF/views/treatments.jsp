<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Treatment types and their prices.

    The appointment form reads this list when the receptionist chooses a
    treatment, and the billing screen reads the cost from it, so the two
    screens always agree on the price.
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
    <div>
        <h2 class="page-head__title">Treatments</h2>
        <p class="page-head__sub">Treatment types offered by the clinic, their duration and cost.</p>
    </div>
    <button class="btn btn--primary" type="button" onclick="openModal('treatmentModal')">+ Add Treatment</button>
</div>

<section class="stat-grid">
    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Treatment Types</p>
            <span class="stat-card__icon stat-card__icon--teal">&#9873;</span>
        </div>
        <p class="stat-card__value">10</p>
        <p class="stat-card__trend">9 active, 1 inactive</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Lowest Price</p>
            <span class="stat-card__icon stat-card__icon--blue">&#8377;</span>
        </div>
        <p class="stat-card__value">0</p>
        <p class="stat-card__trend">Consultation, charged at the dentist fee only</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Highest Price</p>
            <span class="stat-card__icon stat-card__icon--amber">&#8377;</span>
        </div>
        <p class="stat-card__value">85,000</p>
        <p class="stat-card__trend">Braces Fitting</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Longest Treatment</p>
            <span class="stat-card__icon stat-card__icon--violet">&#9200;</span>
        </div>
        <p class="stat-card__value">120</p>
        <p class="stat-card__trend">minutes, Braces Fitting</p>
    </article>
</section>

<section class="panel">
    <header class="panel__head">
        <h3>Treatment List</h3>
        <div class="filters">
            <input type="search" class="input input--search" placeholder="Search treatment name">
            <select class="input">
                <option>All statuses</option>
                <option>Active only</option>
                <option>Inactive only</option>
            </select>
        </div>
    </header>

    <div class="table-wrap">
        <table class="table">
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

            <tr>
                <td><strong>Consultation</strong></td>
                <td class="cell-sub">General dental check-up and advice</td>
                <td class="text-right">15 min</td>
                <td class="text-right mono">0.00</td>
                <td><span class="badge badge--success">Active</span></td>
                <td class="text-right"><a class="link" href="#" onclick="openModal('treatmentModal'); return false;">Edit</a></td>
            </tr>

            <tr>
                <td><strong>X-Ray</strong></td>
                <td class="cell-sub">Dental radiograph</td>
                <td class="text-right">15 min</td>
                <td class="text-right mono">2,000.00</td>
                <td><span class="badge badge--success">Active</span></td>
                <td class="text-right"><a class="link" href="#" onclick="openModal('treatmentModal'); return false;">Edit</a></td>
            </tr>

            <tr>
                <td><strong>Scaling</strong></td>
                <td class="cell-sub">Professional teeth cleaning and polishing</td>
                <td class="text-right">45 min</td>
                <td class="text-right mono">4,500.00</td>
                <td><span class="badge badge--success">Active</span></td>
                <td class="text-right"><a class="link" href="#" onclick="openModal('treatmentModal'); return false;">Edit</a></td>
            </tr>

            <tr>
                <td><strong>Tooth Extraction</strong></td>
                <td class="cell-sub">Simple or surgical tooth removal</td>
                <td class="text-right">30 min</td>
                <td class="text-right mono">5,000.00</td>
                <td><span class="badge badge--success">Active</span></td>
                <td class="text-right"><a class="link" href="#" onclick="openModal('treatmentModal'); return false;">Edit</a></td>
            </tr>

            <tr>
                <td><strong>Filling</strong></td>
                <td class="cell-sub">Composite or amalgam cavity filling</td>
                <td class="text-right">45 min</td>
                <td class="text-right mono">6,000.00</td>
                <td><span class="badge badge--success">Active</span></td>
                <td class="text-right"><a class="link" href="#" onclick="openModal('treatmentModal'); return false;">Edit</a></td>
            </tr>

            <tr>
                <td><strong>Teeth Whitening</strong></td>
                <td class="cell-sub">Cosmetic bleaching treatment</td>
                <td class="text-right">60 min</td>
                <td class="text-right mono">15,000.00</td>
                <td><span class="badge badge--success">Active</span></td>
                <td class="text-right"><a class="link" href="#" onclick="openModal('treatmentModal'); return false;">Edit</a></td>
            </tr>

            <tr>
                <td><strong>Root Canal</strong></td>
                <td class="cell-sub">Root canal treatment (endodontic)</td>
                <td class="text-right">90 min</td>
                <td class="text-right mono">25,000.00</td>
                <td><span class="badge badge--success">Active</span></td>
                <td class="text-right"><a class="link" href="#" onclick="openModal('treatmentModal'); return false;">Edit</a></td>
            </tr>

            <tr>
                <td><strong>Crown Fitting</strong></td>
                <td class="cell-sub">Porcelain or metal crown placement</td>
                <td class="text-right">60 min</td>
                <td class="text-right mono">35,000.00</td>
                <td><span class="badge badge--success">Active</span></td>
                <td class="text-right"><a class="link" href="#" onclick="openModal('treatmentModal'); return false;">Edit</a></td>
            </tr>

            <tr>
                <td><strong>Denture Fitting</strong></td>
                <td class="cell-sub">Partial or complete denture fitting</td>
                <td class="text-right">60 min</td>
                <td class="text-right mono">45,000.00</td>
                <td><span class="badge badge--muted">Inactive</span></td>
                <td class="text-right"><a class="link" href="#" onclick="openModal('treatmentModal'); return false;">Edit</a></td>
            </tr>

            <tr>
                <td><strong>Braces Fitting</strong></td>
                <td class="cell-sub">Orthodontic braces installation</td>
                <td class="text-right">120 min</td>
                <td class="text-right mono">85,000.00</td>
                <td><span class="badge badge--success">Active</span></td>
                <td class="text-right"><a class="link" href="#" onclick="openModal('treatmentModal'); return false;">Edit</a></td>
            </tr>

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

<!-- ================= add / edit treatment modal ================= -->
<div class="modal" id="treatmentModal" aria-hidden="true">
    <div class="modal__backdrop" onclick="closeModal('treatmentModal')"></div>

    <div class="modal__box" role="dialog" aria-modal="true" aria-labelledby="treatmentModalTitle">
        <header class="modal__head">
            <h3 id="treatmentModalTitle">Add Treatment</h3>
            <button class="modal__close" type="button" onclick="closeModal('treatmentModal')" aria-label="Close">&times;</button>
        </header>

        <form class="modal__body"
              onsubmit="showToast('This feature is not available in this version yet.', 'info'); return false;">

            <div class="form-field">
                <label for="trName">Treatment Name <span class="required">*</span></label>
                <input class="input" type="text" id="trName" placeholder="Root Canal"
                       minlength="3" maxlength="100" required>
                <p class="hint">Each treatment name must be unique.</p>
            </div>

            <div class="form-field">
                <label for="trDescription">Description</label>
                <textarea class="input" id="trDescription" rows="2"
                          maxlength="255" placeholder="Short description shown to the receptionist"></textarea>
            </div>

            <div class="form-row">
                <div class="form-field">
                    <label for="trCost">Base Cost (LKR) <span class="required">*</span></label>
                    <input class="input" type="number" id="trCost" min="0" step="100" placeholder="25000" required>
                    <p class="hint">The dentist consultation fee is added on top of this.</p>
                </div>
                <div class="form-field">
                    <label for="trMinutes">Duration (minutes) <span class="required">*</span></label>
                    <select class="input" id="trMinutes" required>
                        <option value="15">15 minutes</option>
                        <option value="30" selected>30 minutes</option>
                        <option value="45">45 minutes</option>
                        <option value="60">60 minutes</option>
                        <option value="90">90 minutes</option>
                        <option value="120">120 minutes</option>
                    </select>
                    <p class="hint">Used to work out how many time slots the visit needs.</p>
                </div>
            </div>

            <div class="form-field">
                <label for="trStatus">Status</label>
                <select class="input" id="trStatus">
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

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
