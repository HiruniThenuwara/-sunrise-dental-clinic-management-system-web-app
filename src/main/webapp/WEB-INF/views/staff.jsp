<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<%--
    Staff accounts (Requirement 1, "only authorised staff can use the system").

    Administrators create the receptionist accounts here. Passwords are hashed
    with a fresh salt by StaffService before they reach the database, and the
    plain password is never stored or displayed.
--%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
    <div>
        <h2 class="page-head__title">Staff Management</h2>
        <p class="page-head__sub">Create, activate and deactivate the accounts that can sign in to the system.</p>
    </div>
    <button class="btn btn--primary" type="button" onclick="openCreateStaff()">+ Add Staff Member</button>
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
            <p class="stat-card__label">Staff Accounts</p>
            <span class="stat-card__icon stat-card__icon--teal">&#9787;</span>
        </div>
        <p class="stat-card__value">${totalCount}</p>
        <p class="stat-card__trend">${activeCount} able to sign in</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Administrators</p>
            <span class="stat-card__icon stat-card__icon--violet">&#9881;</span>
        </div>
        <p class="stat-card__value">${adminCount}</p>
        <p class="stat-card__trend">full access to the clinic setup</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Receptionists</p>
            <span class="stat-card__icon stat-card__icon--blue">&#9998;</span>
        </div>
        <p class="stat-card__value">${receptionistCount}</p>
        <p class="stat-card__trend">appointments and billing</p>
    </article>

    <article class="stat-card">
        <div class="stat-card__top">
            <p class="stat-card__label">Deactivated</p>
            <span class="stat-card__icon stat-card__icon--amber">&times;</span>
        </div>
        <p class="stat-card__value">${totalCount - activeCount}</p>
        <p class="stat-card__trend">cannot sign in</p>
    </article>
</section>

<section class="panel">
    <header class="panel__head">
        <h3>Registered Staff</h3>
    </header>

    <div class="table-wrap">
        <table class="table">
            <thead>
            <tr>
                <th>Staff Member</th>
                <th>Username</th>
                <th>Role</th>
                <th>Last Signed In</th>
                <th>Status</th>
                <th></th>
            </tr>
            </thead>
            <tbody>

            <c:forEach var="member" items="${staff}">
                <tr>
                    <td>
                        <div class="patient-cell">
                            <span class="avatar"><c:out value="${member.initials}"/></span>
                            <div>
                                <strong><c:out value="${member.fullName}"/></strong>
                                <c:if test="${member.userId eq sessionScope.user.userId}">
                                    <div class="cell-sub">This is you</div>
                                </c:if>
                            </div>
                        </div>
                    </td>
                    <td><span class="mono"><c:out value="${member.username}"/></span></td>
                    <td>
                        <span class="badge ${member.admin ? 'badge--warning' : 'badge--muted'}">
                            <c:out value="${member.role.displayName}"/>
                        </span>
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${empty member.lastLogin}">
                                <span class="muted">Never signed in</span>
                            </c:when>
                            <c:otherwise><c:out value="${member.lastLogin}"/></c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${member.active}">
                                <span class="badge badge--success">Active</span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge badge--danger">Inactive</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td class="text-right nowrap">
                        <a class="link" href="#"
                           onclick="editStaff(${member.userId},
                                   '<c:out value="${member.fullName}"/>',
                                   '${member.role}',
                                   ${member.active}); return false;">Edit</a>

                        <a class="link" href="#"
                           onclick="resetPassword(${member.userId},
                                   '<c:out value="${member.fullName}"/>'); return false;">Password</a>

                        <c:if test="${member.userId ne sessionScope.user.userId}">
                            <form method="post" action="${ctx}/admin/staff" class="inline-form"
                                  onsubmit="return confirm('${member.active
                                          ? "Deactivate"
                                          : "Activate"} the account for ${member.fullName}?');">
                                <input type="hidden" name="action" value="toggle">
                                <input type="hidden" name="userId" value="${member.userId}">
                                <input type="hidden" name="active" value="${member.active ? 0 : 1}">
                                <button type="submit" class="link link--button">
                                    <c:out value="${member.active ? 'Deactivate' : 'Activate'}"/>
                                </button>
                            </form>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>

            </tbody>
        </table>
    </div>

</section>

<%-- ================= create / edit staff modal ================= --%>
<div class="modal" id="staffModal" aria-hidden="true">
    <div class="modal__backdrop" onclick="closeModal('staffModal')"></div>

    <div class="modal__box" role="dialog" aria-modal="true" aria-labelledby="staffModalTitle">
        <header class="modal__head">
            <h3 id="staffModalTitle">Add Staff Member</h3>
            <button class="modal__close" type="button" onclick="closeModal('staffModal')" aria-label="Close">&times;</button>
        </header>

        <form class="modal__body" method="post" action="${ctx}/admin/staff" id="staffForm">
            <input type="hidden" name="action" id="staffAction" value="create">
            <input type="hidden" name="userId" id="staffUserId" value="">

            <div class="form-row">
                <div class="form-field">
                    <label for="staffFullName">Full Name <span class="required">*</span></label>
                    <input class="input" type="text" id="staffFullName" name="fullName"
                           value="<c:out value='${formFullName}'/>"
                           placeholder="Nimali Perera" minlength="3" maxlength="100" required>
                </div>
                <div class="form-field" id="usernameField">
                    <label for="staffUsername">Username <span class="required">*</span></label>
                    <input class="input mono" type="text" id="staffUsername" name="username"
                           value="<c:out value='${formUsername}'/>"
                           placeholder="nimali" minlength="3" maxlength="20"
                           pattern="[A-Za-z0-9_]+">
                    <p class="hint">Letters, digits and underscores only. It cannot be changed later.</p>
                </div>
            </div>

            <div class="form-row" id="passwordFields">
                <div class="form-field">
                    <label for="staffPassword">Password <span class="required">*</span></label>
                    <input class="input" type="password" id="staffPassword" name="password"
                           minlength="8" placeholder="At least 8 characters">
                    <p class="hint">At least 8 characters, with letters and digits.</p>
                </div>
                <div class="form-field">
                    <label for="staffConfirm">Confirm Password <span class="required">*</span></label>
                    <input class="input" type="password" id="staffConfirm" name="confirmPassword"
                           minlength="8" placeholder="Type it again">
                </div>
            </div>

            <div class="form-row" id="roleFields">
                <div class="form-field">
                    <label for="staffRole">Role <span class="required">*</span></label>
                    <select class="input" id="staffRole" name="role" required>
                        <option value="RECEPTIONIST">Receptionist - appointments and billing</option>
                        <option value="ADMIN">Administrator - full access</option>
                    </select>
                </div>
                <div class="form-field">
                    <label for="staffStatus">Status</label>
                    <select class="input" id="staffStatus" name="status">
                        <option value="1">Active - can sign in</option>
                        <option value="0">Inactive - cannot sign in</option>
                    </select>
                </div>
            </div>

            <footer class="form-actions">
                <button class="btn btn--ghost" type="button" onclick="closeModal('staffModal')">Cancel</button>
                <button class="btn btn--primary" type="submit">Save Account</button>
            </footer>
        </form>
    </div>
</div>

<script src="${ctx}/assets/js/ui.js"></script>
<script>
    /* Adding a new account: username and password are required. */
    function openCreateStaff() {
        document.getElementById('staffModalTitle').textContent = 'Add Staff Member';
        document.getElementById('staffAction').value = 'create';
        document.getElementById('staffUserId').value = '';
        document.getElementById('staffFullName').value = '';
        document.getElementById('staffUsername').value = '';
        document.getElementById('staffUsername').required = true;
        document.getElementById('staffPassword').value = '';
        document.getElementById('staffPassword').required = true;
        document.getElementById('staffConfirm').value = '';
        document.getElementById('staffConfirm').required = true;
        document.getElementById('staffRole').value = 'RECEPTIONIST';
        document.getElementById('staffStatus').value = '1';
        show('usernameField', true);
        show('passwordFields', true);
        show('roleFields', true);
        openModal('staffModal');
    }

    /* Editing: the username is fixed and the password is left alone. */
    function editStaff(id, fullName, role, active) {
        document.getElementById('staffModalTitle').textContent = 'Edit Staff Member';
        document.getElementById('staffAction').value = 'edit';
        document.getElementById('staffUserId').value = id;
        document.getElementById('staffFullName').value = fullName;
        document.getElementById('staffUsername').required = false;
        document.getElementById('staffPassword').required = false;
        document.getElementById('staffConfirm').required = false;
        document.getElementById('staffRole').value = role;
        document.getElementById('staffStatus').value = active ? '1' : '0';
        show('usernameField', false);
        show('passwordFields', false);
        show('roleFields', true);
        openModal('staffModal');
    }

    /* Setting a new password for somebody who has forgotten theirs. */
    function resetPassword(id, fullName) {
        document.getElementById('staffModalTitle').textContent = 'New Password for ' + fullName;
        document.getElementById('staffAction').value = 'password';
        document.getElementById('staffUserId').value = id;
        document.getElementById('staffFullName').value = fullName;
        document.getElementById('staffUsername').required = false;
        document.getElementById('staffPassword').value = '';
        document.getElementById('staffPassword').required = true;
        document.getElementById('staffConfirm').value = '';
        document.getElementById('staffConfirm').required = true;
        show('usernameField', false);
        show('passwordFields', true);
        show('roleFields', false);
        openModal('staffModal');
    }

    function show(id, visible) {
        document.getElementById(id).style.display = visible ? '' : 'none';
    }

    /* The server checks this too; this is only immediate feedback. */
    document.getElementById('staffForm').addEventListener('submit', function (event) {
        var password = document.getElementById('staffPassword');
        var confirm = document.getElementById('staffConfirm');
        if (password.required && password.value !== confirm.value) {
            event.preventDefault();
            showToast('The two passwords do not match.', 'error');
        }
    });

    <c:if test="${not empty errors}">
    openCreateStaff();
    document.getElementById('staffFullName').value = '<c:out value="${formFullName}"/>';
    document.getElementById('staffUsername').value = '<c:out value="${formUsername}"/>';
    </c:if>
</script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
