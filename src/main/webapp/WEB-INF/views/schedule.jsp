<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Doctor working hours and the time slots generated from them.

    The slot grid is produced by SlotService, which divides the working
    hours by the slot duration and removes any slot that already has an
    appointment.
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
    <div>
        <h2 class="page-head__title">Schedule &amp; Time Slots</h2>
        <p class="page-head__sub">Set working hours for each dentist. Bookable time slots are generated from them.</p>
    </div>
</div>


<div class="grid-2">

    <!-- ---------- working hours ---------- -->
    <section class="panel">
        <header class="panel__head">
            <h3>Working Hours</h3>
            <span class="badge badge--muted">Dr. Anura Jayasinghe</span>
        </header>

        <div class="panel__body">

            <div class="form-row">
                <div class="form-field">
                    <label for="schDoctor">Dentist</label>
                    <select class="input" id="schDoctor">
                        <option>Dr. Anura Jayasinghe - General Dentistry</option>
                        <option>Dr. Sanduni Fernando - Orthodontics</option>
                        <option>Dr. Kasun Silva - Oral Surgery</option>
                        <option>Dr. Malsha Weerasinghe - Pediatric Dentistry</option>
                    </select>
                </div>
                <div class="form-field">
                    <label for="schSlotLength">Slot Length</label>
                    <select class="input" id="schSlotLength">
                        <option>15 minutes</option>
                        <option selected>30 minutes</option>
                        <option>45 minutes</option>
                        <option>60 minutes</option>
                    </select>
                </div>
            </div>

            <table class="table table--compact">
                <thead>
                <tr>
                    <th>Day</th>
                    <th>Working</th>
                    <th>Start</th>
                    <th>End</th>
                    <th class="text-right">Slots</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td><strong>Monday</strong></td>
                    <td><label class="switch"><input type="checkbox" checked><span></span></label></td>
                    <td><input class="input input--time" type="time" value="09:00"></td>
                    <td><input class="input input--time" type="time" value="17:00"></td>
                    <td class="text-right"><strong>16</strong></td>
                </tr>
                <tr>
                    <td><strong>Tuesday</strong></td>
                    <td><label class="switch"><input type="checkbox"><span></span></label></td>
                    <td><input class="input input--time" type="time" value="09:00" disabled></td>
                    <td><input class="input input--time" type="time" value="17:00" disabled></td>
                    <td class="text-right muted">0</td>
                </tr>
                <tr>
                    <td><strong>Wednesday</strong></td>
                    <td><label class="switch"><input type="checkbox" checked><span></span></label></td>
                    <td><input class="input input--time" type="time" value="09:00"></td>
                    <td><input class="input input--time" type="time" value="17:00"></td>
                    <td class="text-right"><strong>16</strong></td>
                </tr>
                <tr>
                    <td><strong>Thursday</strong></td>
                    <td><label class="switch"><input type="checkbox"><span></span></label></td>
                    <td><input class="input input--time" type="time" value="09:00" disabled></td>
                    <td><input class="input input--time" type="time" value="17:00" disabled></td>
                    <td class="text-right muted">0</td>
                </tr>
                <tr>
                    <td><strong>Friday</strong></td>
                    <td><label class="switch"><input type="checkbox" checked><span></span></label></td>
                    <td><input class="input input--time" type="time" value="09:00"></td>
                    <td><input class="input input--time" type="time" value="13:00"></td>
                    <td class="text-right"><strong>8</strong></td>
                </tr>
                <tr>
                    <td><strong>Saturday</strong></td>
                    <td><label class="switch"><input type="checkbox"><span></span></label></td>
                    <td><input class="input input--time" type="time" value="09:00" disabled></td>
                    <td><input class="input input--time" type="time" value="13:00" disabled></td>
                    <td class="text-right muted">0</td>
                </tr>
                <tr>
                    <td><strong>Sunday</strong></td>
                    <td><label class="switch"><input type="checkbox"><span></span></label></td>
                    <td><input class="input input--time" type="time" value="09:00" disabled></td>
                    <td><input class="input input--time" type="time" value="13:00" disabled></td>
                    <td class="text-right muted">Closed</td>
                </tr>
                </tbody>
            </table>

            <div class="form-actions">
                <button class="btn btn--ghost" type="button">Reset</button>
                <button class="btn btn--primary" type="button"
                        onclick="showToast('This feature is not available in this version yet.', 'info')">
                    Save Working Hours
                </button>
            </div>
        </div>
    </section>

    <!-- ---------- generated slots ---------- -->
    <div class="side-column">

        <section class="panel">
            <header class="panel__head">
                <h3>Generated Slots</h3>
                <input class="input input--date" type="date" value="2026-09-01">
            </header>

            <div class="panel__body">
                <p class="hint">Monday 01 September 2026 &middot; 09:00 to 17:00 &middot; 30 minute slots</p>

                <div class="slot-grid">
                    <button class="slot-chip slot-chip--booked" type="button" data-time="09:00" onclick="selectSlot(this)">09:00</button>
                    <button class="slot-chip" type="button" data-time="09:30" onclick="selectSlot(this)">09:30</button>
                    <button class="slot-chip slot-chip--booked" type="button" data-time="10:00" onclick="selectSlot(this)">10:00</button>
                    <button class="slot-chip" type="button" data-time="10:30" onclick="selectSlot(this)">10:30</button>
                    <button class="slot-chip slot-chip--booked" type="button" data-time="11:00" onclick="selectSlot(this)">11:00</button>
                    <button class="slot-chip" type="button" data-time="11:30" onclick="selectSlot(this)">11:30</button>
                    <button class="slot-chip" type="button" data-time="12:00" onclick="selectSlot(this)">12:00</button>
                    <button class="slot-chip" type="button" data-time="12:30" onclick="selectSlot(this)">12:30</button>
                    <button class="slot-chip slot-chip--booked" type="button" data-time="13:00" onclick="selectSlot(this)">13:00</button>
                    <button class="slot-chip" type="button" data-time="13:30" onclick="selectSlot(this)">13:30</button>
                    <button class="slot-chip" type="button" data-time="14:00" onclick="selectSlot(this)">14:00</button>
                    <button class="slot-chip" type="button" data-time="14:30" onclick="selectSlot(this)">14:30</button>
                    <button class="slot-chip slot-chip--booked" type="button" data-time="15:00" onclick="selectSlot(this)">15:00</button>
                    <button class="slot-chip" type="button" data-time="15:30" onclick="selectSlot(this)">15:30</button>
                    <button class="slot-chip" type="button" data-time="16:00" onclick="selectSlot(this)">16:00</button>
                    <button class="slot-chip" type="button" data-time="16:30" onclick="selectSlot(this)">16:30</button>
                </div>

                <div class="legend">
                    <span><i class="legend__dot legend__dot--free"></i> Available</span>
                    <span><i class="legend__dot legend__dot--booked"></i> Already booked</span>
                    <span><i class="legend__dot legend__dot--selected"></i> Selected</span>
                </div>

                <p class="hint">
                    A booked slot cannot be selected. This is the first defence against
                    double booking, and the database enforces it again with a unique
                    constraint on dentist, date and time.
                </p>
            </div>
        </section>

    </div>
</div>

<script src="${ctx}/assets/js/ui.js"></script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
