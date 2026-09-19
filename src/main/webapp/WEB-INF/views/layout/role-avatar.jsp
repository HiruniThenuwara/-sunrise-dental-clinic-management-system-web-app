<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--
    The profile picture for a member of staff, drawn from their role.

    The clinic has no photographs of its staff and no upload screen, so a
    real picture would be a field nobody ever fills in. What is actually
    useful on screen is which kind of account this is: an administrator, who
    can change dentists, treatments and accounts, or a receptionist, who
    books patients in and takes payment. The drawing says that at a glance,
    and the colour repeats it for anyone who cannot make out the shape.

    Call it with the role, and optionally the wrapper class to use:

        <jsp:include page="/WEB-INF/views/layout/role-avatar.jsp">
            <jsp:param name="role" value="${sessionScope.user.role}"/>
        </jsp:include>
--%>

<c:set var="avatarRole" value="${empty param.role ? 'RECEPTIONIST' : param.role}"/>
<c:set var="avatarClass" value="${empty param.styleClass ? 'avatar' : param.styleClass}"/>

<c:choose>
    <c:when test="${avatarRole eq 'ADMIN'}">
        <span class="${avatarClass} avatar--admin" title="Administrator">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
                 stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <%-- A shield: this account guards the clinic's records. --%>
                <path d="M12 2.8 4.9 5.6v5.5c0 4.4 3 8.4 7.1 9.9 4.1-1.5 7.1-5.5 7.1-9.9V5.6L12 2.8z"/>
                <circle cx="12" cy="10" r="2"/>
                <path d="M8.9 16.1a3.4 3.4 0 0 1 6.2 0"/>
            </svg>
            <span class="visually-hidden">Administrator</span>
        </span>
    </c:when>

    <c:otherwise>
        <span class="${avatarClass} avatar--reception" title="Receptionist">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
                 stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <%-- A headset: this account answers the telephone and books
                     the patients in at the desk. --%>
                <path d="M4.6 13.6v-1.8a7.4 7.4 0 0 1 14.8 0v1.8"/>
                <rect x="2.7" y="13" width="3.3" height="5.2" rx="1.6"/>
                <rect x="18" y="13" width="3.3" height="5.2" rx="1.6"/>
                <path d="M19.6 18.2v.6a2.5 2.5 0 0 1-2.5 2.5h-3.4"/>
            </svg>
            <span class="visually-hidden">Receptionist</span>
        </span>
    </c:otherwise>
</c:choose>
