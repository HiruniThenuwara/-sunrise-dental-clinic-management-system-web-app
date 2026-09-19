<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%--
    Shared bottom of every admin page. Closes the tags opened in header.jsp.
--%>
        </main><%-- .content --%>

        <%-- The footer strip was removed. It repeated the system name on
             every screen and said nothing the staff member needed. --%>

    </div><%-- .main --%>
</div><%-- .layout --%>

<%-- Pagination for every list table. Loaded here so that a new screen
     gets it without remembering to add anything. --%>
<script src="${pageContext.request.contextPath}/assets/js/table-pager.js?v=1"></script>

<script>
    // Close the mobile menu when a menu item is tapped.
    document.querySelectorAll('.nav-link').forEach(function (link) {
        link.addEventListener('click', function () {
            document.body.classList.remove('sidebar-open');
        });
    });
</script>

</body>
</html>
