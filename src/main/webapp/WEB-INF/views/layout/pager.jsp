<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--
    Page links for a list screen.

    Include this straight after a table whose servlet put a Page object on
    the request as "pageInfo". Nothing is drawn when everything fits on one
    page, so a short list stays clean.

    Every link repeats the filters that are already in the address, which is
    why the links are built from the request's own parameters rather than
    hard coded: page two of a search must still be that search, and page two
    of a filtered activity log must keep the filter.
--%>

<c:if test="${not empty pageInfo and pageInfo.paged}">

    <%-- The servlet forwards here, so pageContext.request.servletPath would
         give /WEB-INF/views/..., an address the browser is never allowed to
         ask for. The container keeps the original path in the forward
         attributes, and that is what the links must point at. --%>
    <c:set var="pagerServlet"
           value="${empty requestScope['javax.servlet.forward.servlet_path']
                    ? pageContext.request.servletPath
                    : requestScope['javax.servlet.forward.servlet_path']}"/>
    <c:set var="pagerExtra"
           value="${empty requestScope['javax.servlet.forward.path_info']
                    ? '' : requestScope['javax.servlet.forward.path_info']}"/>
    <c:set var="pagerPath" value="${pagerServlet}${pagerExtra}"/>

    <nav class="pager" aria-label="Table pages">

        <p class="pager__count">
            Showing ${pageInfo.firstItem} to ${pageInfo.lastItem}
            of ${pageInfo.totalItems}
        </p>

        <div class="pager__controls">

            <%-- Previous --%>
            <c:choose>
                <c:when test="${pageInfo.hasPrevious}">
                    <c:url var="previousLink" value="${pagerPath}">
                        <c:forEach var="entry" items="${param}">
                            <c:if test="${entry.key ne 'page'}">
                                <c:param name="${entry.key}" value="${entry.value}"/>
                            </c:if>
                        </c:forEach>
                        <c:param name="page" value="${pageInfo.previousPage}"/>
                    </c:url>
                    <a class="pager__btn" href="${previousLink}"
                       aria-label="Previous page">Previous</a>
                </c:when>
                <c:otherwise>
                    <span class="pager__btn is-disabled" aria-disabled="true">Previous</span>
                </c:otherwise>
            </c:choose>

            <%-- The page numbers, with a gap where numbers were left out --%>
            <c:forEach var="number" items="${pageInfo.numbers}">
                <c:choose>
                    <c:when test="${number lt 0}">
                        <span class="pager__gap">&hellip;</span>
                    </c:when>
                    <c:when test="${number eq pageInfo.pageNumber}">
                        <span class="pager__page is-current" aria-current="page">${number}</span>
                    </c:when>
                    <c:otherwise>
                        <c:url var="numberLink" value="${pagerPath}">
                            <c:forEach var="entry" items="${param}">
                                <c:if test="${entry.key ne 'page'}">
                                    <c:param name="${entry.key}" value="${entry.value}"/>
                                </c:if>
                            </c:forEach>
                            <c:param name="page" value="${number}"/>
                        </c:url>
                        <a class="pager__page" href="${numberLink}"
                           aria-label="Page ${number}">${number}</a>
                    </c:otherwise>
                </c:choose>
            </c:forEach>

            <%-- Next --%>
            <c:choose>
                <c:when test="${pageInfo.hasNext}">
                    <c:url var="nextLink" value="${pagerPath}">
                        <c:forEach var="entry" items="${param}">
                            <c:if test="${entry.key ne 'page'}">
                                <c:param name="${entry.key}" value="${entry.value}"/>
                            </c:if>
                        </c:forEach>
                        <c:param name="page" value="${pageInfo.nextPage}"/>
                    </c:url>
                    <a class="pager__btn" href="${nextLink}" aria-label="Next page">Next</a>
                </c:when>
                <c:otherwise>
                    <span class="pager__btn is-disabled" aria-disabled="true">Next</span>
                </c:otherwise>
            </c:choose>

        </div>
    </nav>
</c:if>
