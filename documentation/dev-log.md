# Development Log

Sunrise Dental Clinic Management System — CIS6003 Advanced Programming

---

## Day 1 — Project Setup, Authentication and Admin Shell

**Version tag:** `v0.1.0`
**Branch:** `feature/day1-login` → `develop` → `main`

### Objective

Get a working, secured foundation: a Maven web project that runs on Tomcat, a real
database, and a login that only lets authorised staff into the admin panel. The login
rules were built with test driven development so the Day 1 history already shows a
RED commit followed by a GREEN commit.

### What was built

| Commit | Description |
|---|---|
| 1 | `.gitignore` and project README |
| 2 | Maven `war` project, package structure, `web.xml`, JUnit 5 + Mockito + H2 |
| 3 | `database/schema.sql` — 7 tables, constraints and seed data |
| 4 | `DBConnection` (Singleton) and `db.properties` |
| 5 | `User`, `Role`, `UserDao`, `UserDaoImpl` |
| 6 | `AuthServiceTest` — 10 test cases written **before** the implementation (RED) |
| 7 | `AuthService` implemented until all tests pass (GREEN) |
| 8 | `LoginServlet` — session creation, session fixation protection |
| 9 | Remember-me cookie and `LogoutServlet` |
| 10 | `login.jsp` and `login.css` |
| 11 | Admin layout: `header.jsp`, `sidebar.jsp`, `footer.jsp`, `admin.css`, `DashboardServlet` |
| 12 | `AuthFilter` — protects `/admin/*` |
| 13 | This development log and the Day 1 screenshots |

### Design decisions and the reasons for them

**MVC boundaries were fixed on Day 1, not later.**
`LoginServlet` contains no SQL and no password logic. It reads the request, asks
`AuthService` for a decision, and picks the next view. This is why the login rules
could be unit tested with no browser and no database.

**Passwords are salted, not stored in plain text.**
The `users` table stores a SHA-256 hash of (salt + password) plus a random 32
character salt per user. Two staff members with the same password therefore have
different hashes, so one leaked hash cannot be reused. `User.toString()` deliberately
omits the hash and salt so they can never reach a log file.

**One generic error message.**
A failed login always shows "Invalid username or password", never "no such user".
Distinguishing the two would let an attacker discover which usernames exist.

**The session id is regenerated after login.**
The old session is invalidated and a new one created, which defeats session fixation
(an attacker planting a known session id before the victim logs in).

**Protected views live under `/WEB-INF/views/`.**
Tomcat never serves anything under `WEB-INF` directly, so a user cannot reach
`dashboard.jsp` by typing its path. The only way in is through `DashboardServlet`,
and therefore through `AuthFilter`.

**Remember-me does not add a database column.**
The cookie holds `username:signature`, where the signature is a SHA-256 hash of the
username, the user's salt and their stored password hash. The server can recompute
and verify it, but nobody can forge it without reading the database. Changing the
password automatically invalidates every old cookie, because the hash it was signed
with no longer exists. The cookie is marked `HttpOnly`, so JavaScript cannot steal it.

**Enum instead of String for the role.**
`Role` is an enum, so an invalid access level cannot be created by mistake and the
compiler catches typos that a `String` would not.

### Test driven development record

`AuthServiceTest` was committed **before** `AuthService` had any working code. The
skeleton class threw `UnsupportedOperationException` from every method so the test
class compiled and produced a genuine failing run.

| Test | What it proves |
|---|---|
| TC-01 | Correct credentials return the user |
| TC-02 | A wrong password is refused and no login is recorded |
| TC-03 | An unknown username is refused |
| TC-04 | A blank username never even reaches the database |
| TC-05 | A null password is refused without throwing |
| TC-06 | A deactivated staff account cannot log in |
| TC-07 | A successful login updates `last_login` (audit trail) |
| TC-08 | Hashing is repeatable, salt dependent, and matches `schema.sql` |
| TC-09 | A genuine remember-me cookie is accepted |
| TC-10 | A forged remember-me cookie is rejected |

TC-08 is worth noting: it asserts the hash produced by the Java code equals the exact
value seeded into the database by `schema.sql`. If either side is ever changed without
the other, this test fails immediately.

**Evidence to attach:**

| Screenshot | File |
|---|---|
| RED — 10 tests failing before the implementation | `screenshots/day1-tdd-red.png` |
| GREEN — 10 tests passing after the implementation | `screenshots/day1-tdd-green.png` |
| Login screen | `screenshots/day1-login-page.png` |
| Admin dashboard after login | `screenshots/day1-dashboard.png` |
| Session expiry message | `screenshots/day1-session-timeout.png` |

### Database verification

`database/schema.sql` was imported into XAMPP (MariaDB 10.4.32) and reported:

```
users 2 · doctors 4 · doctor_schedule 10 · treatments 10
patients 3 · appointments 3 · bills 1
```

The double booking rule was then tested directly in SQL by trying to insert a second
appointment for the same dentist, date and time:

```
ERROR 1062 (23000): Duplicate entry '1-2026-09-01-09:00:00' for key 'uq_doctor_slot'
```

This is the database-level defence against the double bookings described in the
scenario. The service layer adds a friendly message on top of it on Day 3.

### Assumptions made

1. Staff accounts are created by the administrator directly in the database. Public
   self registration would be wrong for a clinic system where only employees may
   have access.
2. Two roles are enough: `ADMIN` (full access, including dentists and reports) and
   `RECEPTIONIST` (appointments, search and billing).
3. XAMPP's default `root` account with an empty password is used for development
   only. This is documented rather than hidden, and a restricted database user would
   be created for a real deployment.
4. Sessions expire after 30 minutes of inactivity, which suits a shared reception
   computer.

### Problems encountered

| Problem | Solution |
|---|---|
| Maven CLI is not installed on the development machine | Tests are run from Eclipse with `Run As > JUnit Test`; the GitHub Actions runner provides Maven for CI |
| JDK 25 is installed, but Tomcat 9 projects normally target an LTS release | `pom.xml` compiles with `--release 17`, which JDK 25 supports |
| A single shared JDBC `Connection` would break under concurrent requests | The Singleton shares only the configuration; `getConnection()` returns a new connection each time and callers close it with try-with-resources |

### Day 1 result

Login, logout, remember-me, session protection and the admin layout are complete and
tested. Day 2 builds the remaining screens.
