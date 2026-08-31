<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Staff Login | Sunrise Dental Clinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/login.css?v=2">
</head>
<body>

<main class="login-page">
    <div class="login-wrap">

        <!-- ================= clinic identity ================= -->
        <header class="brand">
            <span class="brand__badge" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="30" height="30" fill="currentColor">
                    <path d="M16.8 2.4c-1.5 0-2.5.8-3.4 1.2a3.4 3.4 0 0 1-2.8 0C9.7 3.2 8.7 2.4 7.2 2.4 4.6 2.4 2.6 4.6 2.6 7.9c0 2.3.6 4 1.3 5.8.5 1.3.8 2.5 1 3.7.2 1.1.3 2.1.6 2.9.3.8.9 1.3 1.7 1.3.9 0 1.4-.7 1.7-1.6.3-.9.5-2 .7-3.1.2-1.1.4-2.1.7-2.7.2-.4.4-.6.7-.6s.5.2.7.6c.3.6.5 1.6.7 2.7.2 1.1.4 2.2.7 3.1.3.9.8 1.6 1.7 1.6.8 0 1.4-.5 1.7-1.3.3-.8.4-1.8.6-2.9.2-1.2.5-2.4 1-3.7.7-1.8 1.3-3.5 1.3-5.8 0-3.3-2-5.5-4.6-5.5Z"/>
                </svg>
            </span>
            <h1 class="brand__name">Sunrise <span>Dental</span></h1>
            <p class="brand__tagline">Appointment &amp; Patient Management System</p>
        </header>

        <!-- ================= login card ================= -->
        <div class="card">

            <div class="card__head">
                <h2>Welcome back</h2>
                <p>Sign in to continue to the clinic dashboard.</p>
            </div>

            <c:if test="${not empty error}">
                <div class="alert alert--error" role="alert">
                    <svg class="alert__icon" viewBox="0 0 24 24" width="18" height="18" fill="none"
                         stroke="currentColor" stroke-width="2" stroke-linecap="round">
                        <circle cx="12" cy="12" r="9"/><path d="M12 7.5v5"/><path d="M12 16.2v.1"/>
                    </svg>
                    <span><c:out value="${error}"/></span>
                </div>
            </c:if>

            <c:if test="${param.logout eq '1'}">
                <div class="alert alert--success" role="status">
                    <svg class="alert__icon" viewBox="0 0 24 24" width="18" height="18" fill="none"
                         stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="9"/><path d="m8.5 12.2 2.4 2.4 4.6-4.9"/>
                    </svg>
                    <span>You have been signed out safely.</span>
                </div>
            </c:if>

            <c:if test="${param.timeout eq '1'}">
                <div class="alert alert--warning" role="status">
                    <svg class="alert__icon" viewBox="0 0 24 24" width="18" height="18" fill="none"
                         stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="9"/><path d="M12 7.6V12l2.8 1.8"/>
                    </svg>
                    <span>Your session has expired. Please sign in again.</span>
                </div>
            </c:if>

            <form method="post" action="${pageContext.request.contextPath}/login" novalidate>

                <div class="field">
                    <label for="username">Username</label>
                    <div class="control">
                        <svg class="control__icon" viewBox="0 0 24 24" width="18" height="18" fill="none"
                             stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                            <circle cx="12" cy="8" r="3.6"/>
                            <path d="M4.8 20a7.2 7.2 0 0 1 14.4 0"/>
                        </svg>
                        <input type="text"
                               id="username"
                               name="username"
                               value="<c:out value='${username}'/>"
                               placeholder="Enter your username"
                               autocomplete="username"
                               required
                               autofocus>
                    </div>
                </div>

                <div class="field">
                    <label for="password">Password</label>
                    <div class="control">
                        <svg class="control__icon" viewBox="0 0 24 24" width="18" height="18" fill="none"
                             stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="4.5" y="10.5" width="15" height="9.5" rx="2.2"/>
                            <path d="M8.2 10.5V7.8a3.8 3.8 0 0 1 7.6 0v2.7"/>
                        </svg>
                        <input type="password"
                               id="password"
                               name="password"
                               placeholder="Enter your password"
                               autocomplete="current-password"
                               required>
                        <button type="button" class="reveal" onclick="togglePassword()"
                                aria-label="Show or hide password">Show</button>
                    </div>
                </div>

                <div class="field-row">
                    <label class="checkbox">
                        <input type="checkbox" name="remember" id="remember">
                        <span>Keep me signed in</span>
                    </label>
                    <a class="help-link" href="${pageContext.request.contextPath}/login">Forgot password?</a>
                </div>

                <button type="submit" class="btn-signin">
                    Sign In
                    <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor"
                         stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M5 12h13"/><path d="m12.5 6 6 6-6 6"/>
                    </svg>
                </button>
            </form>

            <div class="card__foot">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor"
                     stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 3 5 6v5.4c0 4.3 2.9 8.3 7 9.6 4.1-1.3 7-5.3 7-9.6V6l-7-3Z"/>
                </svg>
                <span>Authorised clinic staff only. All logins are recorded.</span>
            </div>

        </div>

        <!-- ================= trust row ================= -->
        <ul class="trust">
            <li>Secure staff access</li>
            <li>Patient data protected</li>
            <li>Colombo &middot; Est. 2015</li>
        </ul>

    </div>
</main>

<script>
    function togglePassword() {
        var input = document.getElementById('password');
        var button = document.querySelector('.reveal');
        var hidden = input.type === 'password';
        input.type = hidden ? 'text' : 'password';
        button.textContent = hidden ? 'Hide' : 'Show';
    }
</script>

</body>
</html>
