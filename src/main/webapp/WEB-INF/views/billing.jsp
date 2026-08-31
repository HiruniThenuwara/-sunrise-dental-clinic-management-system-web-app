<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Calculate and print the bill (Requirement 4).

    Left side: the staff member adjusts discount and payment method.
    Right side: a printable receipt. The print stylesheet in admin.css hides
    the sidebar, top bar and buttons so only the receipt reaches the printer.
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head no-print">
    <div>
        <h2 class="page-head__title">Billing</h2>
        <p class="page-head__sub">Calculate the treatment cost and print the patient receipt.</p>
    </div>
    <div class="page-head__actions">
        <a class="btn btn--ghost" href="${ctx}/admin/appointments">Back to list</a>
        <button class="btn btn--primary" type="button" onclick="window.print()">Print Receipt</button>
    </div>
</div>

<div class="notice no-print">
    <span class="notice__tag">Sample data</span>
    <span>Totals are calculated in the browser today. BillingService and the Strategy pattern take over on Day 3.</span>
</div>

<div class="grid-2">

    <!-- ---------- bill builder ---------- -->
    <section class="panel no-print">
        <header class="panel__head"><h3>Bill Details</h3></header>
        <div class="panel__body">

            <div class="form-field">
                <label for="billAppointment">Appointment Number</label>
                <input class="input mono" type="search" id="billAppointment" value="APT-20260902-001">
            </div>

            <dl class="detail-grid">
                <dt>Patient</dt>
                <dd>Ruwan Perera</dd>
                <dt>Dentist</dt>
                <dd>Dr. Kasun Silva</dd>
                <dt>Treatment</dt>
                <dd>Tooth Extraction</dd>
                <dt>Visit Date</dt>
                <dd>02 September 2026, 08:00 AM</dd>
            </dl>

            <hr class="divider">

            <div class="form-row">
                <div class="form-field">
                    <label for="billConsult">Consultation Fee (LKR)</label>
                    <input class="input" type="number" id="billConsult" value="3000" step="100" oninput="recalculate()">
                </div>
                <div class="form-field">
                    <label for="billTreatment">Treatment Cost (LKR)</label>
                    <input class="input" type="number" id="billTreatment" value="5000" step="100" oninput="recalculate()">
                </div>
            </div>

            <div class="form-row">
                <div class="form-field">
                    <label for="billDiscount">Discount (LKR)</label>
                    <input class="input" type="number" id="billDiscount" value="0" min="0" step="100" oninput="recalculate()">
                </div>
                <div class="form-field">
                    <label for="billMethod">Payment Method</label>
                    <select class="input" id="billMethod" onchange="recalculate()">
                        <option value="CASH">Cash</option>
                        <option value="CARD">Card</option>
                        <option value="INSURANCE">Insurance</option>
                    </select>
                </div>
            </div>

            <div class="form-actions">
                <button class="btn btn--ghost" type="button" onclick="window.print()">Print Receipt</button>
                <button class="btn btn--primary" type="button"
                        onclick="showToast('Saving the bill is connected on Day 3.', 'info')">Save Bill</button>
            </div>

        </div>
    </section>

    <!-- ---------- printable receipt ---------- -->
    <section class="receipt" id="receipt">

        <header class="receipt__head">
            <h2 class="receipt__clinic">Sunrise Dental Clinic</h2>
            <p class="receipt__address">
                No 128, Galle Road, Colombo 03<br>
                Tel: 011 234 5678 &middot; info@sunrisedental.lk
            </p>
            <p class="receipt__title">PATIENT RECEIPT</p>
        </header>

        <div class="receipt__meta">
            <div><span>Bill No</span><strong class="mono" id="rcBillNo">BILL-20260902-001</strong></div>
            <div><span>Appointment No</span><strong class="mono">APT-20260902-001</strong></div>
            <div><span>Date</span><strong>02 September 2026</strong></div>
            <div><span>Cashier</span><strong><c:out value="${sessionScope.user.fullName}"/></strong></div>
        </div>

        <div class="receipt__patient">
            <p><strong>Ruwan Perera</strong></p>
            <p>No 8, Station Road, Dehiwala</p>
            <p>076 123 4567</p>
        </div>

        <table class="receipt__table">
            <thead>
            <tr>
                <th>Description</th>
                <th class="text-right">Amount (LKR)</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td>Consultation - Dr. Kasun Silva<br><small>Oral Surgery</small></td>
                <td class="text-right mono" id="rcConsult">3,000.00</td>
            </tr>
            <tr>
                <td>Tooth Extraction<br><small>Simple or surgical tooth removal</small></td>
                <td class="text-right mono" id="rcTreatment">5,000.00</td>
            </tr>
            <tr>
                <td>Discount</td>
                <td class="text-right mono" id="rcDiscount">0.00</td>
            </tr>
            </tbody>
            <tfoot>
            <tr>
                <th>TOTAL</th>
                <th class="text-right mono" id="rcTotal">8,000.00</th>
            </tr>
            </tfoot>
        </table>

        <div class="receipt__pay">
            <span>Payment method</span>
            <strong id="rcMethod">CASH</strong>
        </div>

        <footer class="receipt__foot">
            <p>Thank you for visiting Sunrise Dental Clinic.</p>
            <p class="muted">This is a computer generated receipt. Please keep it for your records.</p>
        </footer>
    </section>

</div>

<script src="${ctx}/assets/js/ui.js"></script>
<script>
    /*
       Day 2 preview of the calculation.
       On Day 3 the same arithmetic lives in BillingService on the server,
       where it is unit tested, and this page only displays the result.
    */
    function money(value) {
        return value.toLocaleString('en-LK', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function recalculate() {
        var consult = Number(document.getElementById('billConsult').value) || 0;
        var treatment = Number(document.getElementById('billTreatment').value) || 0;
        var discount = Number(document.getElementById('billDiscount').value) || 0;

        if (discount > consult + treatment) {
            discount = consult + treatment;
            document.getElementById('billDiscount').value = discount;
            showToast('Discount cannot be more than the total.', 'error');
        }

        document.getElementById('rcConsult').textContent = money(consult);
        document.getElementById('rcTreatment').textContent = money(treatment);
        document.getElementById('rcDiscount').textContent = money(discount);
        document.getElementById('rcTotal').textContent = money(consult + treatment - discount);
        document.getElementById('rcMethod').textContent = document.getElementById('billMethod').value;
    }

    recalculate();
</script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
