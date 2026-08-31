<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Staff Login | Sunrise Dental Clinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/login.css">
</head>
<body>

<main class="login-page">

    <div class="login-wrap">

        <!-- ============ clinic identity ============ -->
        <header class="brand">
            <div class="brand__mark">
                <svg viewBox="0 0 24 24" width="32" height="32" fill="none"
                     stroke="currentColor" stroke-width="1.8" stroke-linecap="round"
                     stroke-linejoin="round" aria-hidden="true">
                    <path d="M12 5.5c-1.6-1.3-3.4-1.9-5-1.3C4.9 5 4 7.2 4.4 9.9c.3 1.9.9 3.4 1.4 5.1.4 1.4.7 2.9.9 4.1.2 1 1.5 1.2 2 .3.6-1.1 1-2.4 1.3-3.6.4-1.5 1.5-1.5 1.9 0 .3 1.2.7 2.5 1.3 3.6.5.9 1.8.7 2-.3.2-1.2.5-2.7.9-4.1.5-1.7 1.1-3.2 1.4-5.1.4-2.7-.5-4.9-2.6-5.7-1.6-.6-3.4 0-5 1.3Z"/>
                </svg>
            </div>
            <h1 class="brand__name">Sunrise<span>Dental</span></h1>
            <p class="brand__tagline">Appointment &amp; Patient Management System</p>
        </header>

        <!-- ============ login card ============ -->
        <div class="login-card">

            <div class="login-card__head">
                <h2>Staff Login</h2>
                <p>Sign in to manage appointments and patient records.</p>
            </div>

            <c:if test="${not empty error}">
                <div class="alert alert--error" role="alert">
                    <span class="alert__icon">!</span>
                    <span><c:out value="${error}"/></span>
                </div>
            </c:if>

            <c:if test="${param.logout eq '1'}">
                <div class="alert alert--success" role="status">
                    <span class="alert__icon">&#10003;</span>
                    <span>You have been signed out safely.</span>
                </div>
            </c:if>

            <c:if test="${param.timeout eq '1'}">
                <div class="alert alert--warning" role="status">
                    <span class="alert__icon">&#8635;</span>
                    <span>Your session has expired. Please sign in again.</span>
                </div>
            </c:if>

            <form method="post" action="${pageContext.request.contextPath}/login" novalidate>

                <div class="field">
                    <label for="username">Username</label>
                    <input type="text"
                           id="username"
                           name="username"
                           value="<c:out value='${username}'/>"
                           placeholder="Enter your username"
                           autocomplete="username"
                           required
                           autofocus>
                </div>

                <div class="field">
                    <label for="password">Password</label>
                    <div class="field__password">
                        <input type="password"
                               id="password"
                               name="password"
                               placeholder="Enter your password"
                               autocomplete="current-password"
                               required>
                        <button type="button" class="toggle-password"
                                onclick="togglePassword()"
                                aria-label="Show or hide password">Show</button>
                    </div>
                </div>

                <div class="field field--inline">
                    <label class="checkbox">
                        <input type="checkbox" name="remember" id="remember">
                        <span>Keep me signed in on this computer</span>
                    </label>
                </div>

                <button type="submit" class="btn-primary">Sign In</button>
            </form>

            <div class="login-card__foot">
                <p>Authorised clinic staff only. All logins are recorded.</p>
            </div>

        </div>

        <p class="page-note">Colombo &middot; Established 2015</p>

    </div>
</main>

<script>
    function togglePassword() {
        var input = document.getElementById('password');
        var button = document.querySelector('.toggle-password');
        var hidden = input.type === 'password';
        input.type = hidden ? 'text' : 'password';
        button.textContent = hidden ? 'Hide' : 'Show';
    }
</script>

</body>
</html>
