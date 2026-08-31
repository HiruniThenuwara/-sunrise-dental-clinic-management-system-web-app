<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%-- Dentist management - Day 2 interface with sample rows. --%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
    <div>
        <h2 class="page-head__title">Dentists</h2>
        <p class="page-head__sub">Add dentists, set their consultation fee and control who can accept bookings.</p>
    </div>
    <button class="btn btn--primary" type="button" onclick="openModal('doctorModal')">+ Add Dentist</button>
</div>

<div class="notice">
    <span class="notice__tag">Sample data</span>
    <span>The form does not save yet. It is connected to the database on Day 3.</span>
</div>

<section class="panel">
    <header class="panel__head">
        <h3>Registered Dentists</h3>
        <div class="filters">
            <input type="search" class="input input--search" placeholder="Search by name or specialization">
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
                <th>Dentist</th>
                <th>Specialization</th>
                <th>Contact</th>
                <th class="text-right">Consultation Fee</th>
                <th>Working Days</th>
                <th>Status</th>
                <th></th>
            </tr>
            </thead>
            <tbody>

            <tr>
                <td>
                    <div class="patient-cell">
                        <span class="avatar">AJ</span>
                        <div>
                            <strong>Dr. Anura Jayasinghe</strong>
                            <div class="cell-sub">anura@sunrisedental.lk</div>
                        </div>
                    </div>
                </td>
                <td>General Dentistry</td>
                <td>077 123 4567</td>
                <td class="text-right"><strong>LKR 1,500.00</strong></td>
                <td><span class="day-pill">Mon</span><span class="day-pill">Wed</span><span class="day-pill">Fri</span></td>
                <td><span class="badge badge--success">Active</span></td>
                <td class="text-right">
                    <a class="link" href="${ctx}/admin/schedule">Schedule</a>
                    <a class="link" href="#" onclick="openModal('doctorModal'); return false;">Edit</a>
                </td>
            </tr>

            <tr>
                <td>
                    <div class="patient-cell">
                        <span class="avatar">SF</span>
                        <div>
                            <strong>Dr. Sanduni Fernando</strong>
                            <div class="cell-sub">sanduni@sunrisedental.lk</div>
                        </div>
                    </div>
                </td>
                <td>Orthodontics</td>
                <td>077 234 5678</td>
                <td class="text-right"><strong>LKR 2,500.00</strong></td>
                <td><span class="day-pill">Tue</span><span class="day-pill">Thu</span><span class="day-pill">Sat</span></td>
                <td><span class="badge badge--success">Active</span></td>
                <td class="text-right">
                    <a class="link" href="${ctx}/admin/schedule">Schedule</a>
                    <a class="link" href="#" onclick="openModal('doctorModal'); return false;">Edit</a>
                </td>
            </tr>

            <tr>
                <td>
                    <div class="patient-cell">
                        <span class="avatar">KS</span>
                        <div>
                            <strong>Dr. Kasun Silva</strong>
                            <div class="cell-sub">kasun@sunrisedental.lk</div>
                        </div>
                    </div>
                </td>
                <td>Oral Surgery</td>
                <td>077 345 6789</td>
                <td class="text-right"><strong>LKR 3,000.00</strong></td>
                <td><span class="day-pill">Mon</span><span class="day-pill">Thu</span></td>
                <td><span class="badge badge--success">Active</span></td>
                <td class="text-right">
                    <a class="link" href="${ctx}/admin/schedule">Schedule</a>
                    <a class="link" href="#" onclick="openModal('doctorModal'); return false;">Edit</a>
                </td>
            </tr>

            <tr>
                <td>
                    <div class="patient-cell">
                        <span class="avatar">MW</span>
                        <div>
                            <strong>Dr. Malsha Weerasinghe</strong>
                            <div class="cell-sub">malsha@sunrisedental.lk</div>
                        </div>
                    </div>
                </td>
                <td>Pediatric Dentistry</td>
                <td>077 456 7890</td>
                <td class="text-right"><strong>LKR 2,000.00</strong></td>
                <td><span class="day-pill">Wed</span><span class="day-pill">Sat</span></td>
                <td><span class="badge badge--muted">On Leave</span></td>
                <td class="text-right">
                    <a class="link" href="${ctx}/admin/schedule">Schedule</a>
                    <a class="link" href="#" onclick="openModal('doctorModal'); return false;">Edit</a>
                </td>
            </tr>

            </tbody>
        </table>
    </div>
</section>

<!-- ================= add / edit dentist modal ================= -->
<div class="modal" id="doctorModal" aria-hidden="true">
    <div class="modal__backdrop" onclick="closeModal('doctorModal')"></div>

    <div class="modal__box" role="dialog" aria-modal="true" aria-labelledby="doctorModalTitle">
        <header class="modal__head">
            <h3 id="doctorModalTitle">Add Dentist</h3>
            <button class="modal__close" type="button" onclick="closeModal('doctorModal')" aria-label="Close">&times;</button>
        </header>

        <form class="modal__body" onsubmit="alert('Saving is connected to the database on Day 3.'); return false;">

            <div class="form-row">
                <div class="form-field">
                    <label for="docName">Dentist Name <span class="required">*</span></label>
                    <input class="input" type="text" id="docName" placeholder="Dr. Anura Jayasinghe" required>
                </div>
                <div class="form-field">
                    <label for="docSpec">Specialization <span class="required">*</span></label>
                    <select class="input" id="docSpec" required>
                        <option value="">Select specialization</option>
                        <option>General Dentistry</option>
                        <option>Orthodontics</option>
                        <option>Oral Surgery</option>
                        <option>Pediatric Dentistry</option>
                        <option>Periodontics</option>
                        <option>Prosthodontics</option>
                    </select>
                </div>
            </div>

            <div class="form-row">
                <div class="form-field">
                    <label for="docPhone">Contact Number <span class="required">*</span></label>
                    <input class="input" type="tel" id="docPhone" placeholder="0771234567"
                           pattern="0[0-9]{9}" maxlength="10" required>
                    <p class="hint">10 digits starting with 0</p>
                </div>
                <div class="form-field">
                    <label for="docEmail">Email</label>
                    <input class="input" type="email" id="docEmail" placeholder="name@sunrisedental.lk">
                </div>
            </div>

            <div class="form-row">
                <div class="form-field">
                    <label for="docFee">Consultation Fee (LKR) <span class="required">*</span></label>
                    <input class="input" type="number" id="docFee" min="0" step="100" placeholder="1500" required>
                </div>
                <div class="form-field">
                    <label for="docStatus">Status</label>
                    <select class="input" id="docStatus">
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

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
