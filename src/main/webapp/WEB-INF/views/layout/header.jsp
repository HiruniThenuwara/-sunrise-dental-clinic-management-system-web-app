<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%--
    Shared top of every admin page.

    Each view includes this file, then writes its own content, then
    includes footer.jsp. Keeping the sidebar and top bar in one place
    means a menu change is made once instead of on every page.
--%>
<jsp:useBean id="today" class="java.util.Date" scope="page"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${pageTitle}"/> | Sunrise Dental Clinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin.css?v=11">
</head>
<body>

<div class="layout">

    <%-- ================= sidebar ================= --%>
    <jsp:include page="/WEB-INF/views/layout/sidebar.jsp"/>

    <%-- ================= main column ================= --%>
    <div class="main">

        <header class="topbar">
            <div class="topbar__left">
                <button class="menu-toggle" type="button"
                        onclick="document.body.classList.toggle('sidebar-open')"
                        aria-label="Toggle menu">&#9776;</button>
                <div>
                    <h1 class="topbar__title"><c:out value="${pageTitle}"/></h1>
                    <p class="topbar__date">
                        <fmt:formatDate value="${today}" pattern="EEEE, d MMMM yyyy"/>
                    </p>
                </div>
            </div>

            <div class="topbar__right">
                <div class="user-chip">
                    <span class="user-chip__avatar">
                        <c:out value="${sessionScope.user.initials}"/>
                    </span>
                    <span class="user-chip__text">
                        <strong><c:out value="${sessionScope.user.fullName}"/></strong>
                        <small><c:out value="${sessionScope.user.role.displayName}"/></small>
                    </span>
                </div>

                <a class="btn btn--logout"
                   href="${pageContext.request.contextPath}/logout"
                   onclick="return confirm('Sign out of the system?');">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor"
                         stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M14.5 16.5v2a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-13a2 2 0 0 1 2-2h6.5a2 2 0 0 1 2 2v2"/>
                        <path d="M9.5 12h11"/>
                        <path d="m17 8.5 3.5 3.5-3.5 3.5"/>
                    </svg>
                    <span class="btn__text">Sign Out</span>
                </a>
            </div>
        </header>

        <main class="content">
