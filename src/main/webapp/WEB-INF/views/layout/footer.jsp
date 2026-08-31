<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%--
    Shared bottom of every admin page. Closes the tags opened in header.jsp.
--%>
        </main><%-- .content --%>

        <footer class="page-footer">
            <p>Sunrise Dental Clinic Management System &middot; CIS6003 Advanced Programming</p>
            <p class="muted">Handle patient data with care. Do not leave this screen unattended.</p>
        </footer>

    </div><%-- .main --%>
</div><%-- .layout --%>

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
