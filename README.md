# Sunrise Dental Clinic — Appointment & Patient Management System

A web-based appointment and patient management system for **Sunrise Dental Clinic, Colombo**,
built with **core Java (Servlets + JSP + JDBC)** following a **simple MVC architecture**.
No application framework is used.

**Module:** CIS6003 Advanced Programming (WRIT1)
**Repository:** https://github.com/HiruniThenuwara/-sunrise-dental-clinic-management-system-web-app

---

## Problem

Sunrise Dental Clinic currently manages patient appointments and treatment records manually using
paper files and notebooks. This causes double bookings, lost patient records, long waiting times
and billing errors. This system replaces that manual process with a computerised solution.

---

## Features

| # | Feature | Description |
|---|---|---|
| 1 | **User Authentication** | Secure staff login with username and password. Session + "remember me" cookie. |
| 2 | **Register New Appointment** | Stores appointment number, patient name, address, contact number, dentist, treatment type, date and time. |
| 3 | **Display Appointment Details** | Search by appointment number and view complete patient and appointment information. |
| 4 | **Calculate & Print Bill** | Calculates total cost from treatment type + consultation fee and prints the patient receipt. |
| 5 | **Doctor Management** | Add, edit and deactivate dentists with their consultation fees. |
| 6 | **Schedule & Time Slots** | Define doctor working hours and auto-generate bookable time slots. Prevents double booking. |
| 7 | **Reports** | Daily appointments, revenue report and doctor workload report. |
| 8 | **Help Section** | Step-by-step instructions for new staff members. |
| 9 | **Exit / Logout** | Safely closes the session and clears cookies. |

---

## Technology Stack

| Layer | Technology |
|---|---|
| View | JSP, HTML5, CSS3, Vanilla JavaScript |
| Controller | Java Servlets (`javax.servlet`) |
| Model | Plain Java objects (POJO), DAO classes, JDBC |
| Web Services | REST servlets returning JSON (`/api/*`) |
| Database | MySQL / MariaDB (XAMPP) |
| Server | Apache Tomcat 9.0.x |
| Build tool | Apache Maven (`war` packaging) |
| Testing | JUnit 5, Mockito, H2 |
| CI/CD | GitHub Actions |
| IDE | Eclipse IDE for Enterprise Java and Web Developers |

> Maven is used only as a **build and dependency tool** so that automated tests and the CI
> workflow can run. It is not an application framework — all application code is plain Java.

---

## Architecture — Simple MVC (3 Tier)

```
                 Browser (JSP / HTML / CSS / JS)
                              |
                   ----------------------  Presentation Tier
                              |
     Servlets (controller/) + REST APIs (api/)
                              |
                   ----------------------  Business Tier
                              |
        Services (service/) --> DAO interfaces (dao/)
                              |
                   ----------------------  Data Tier
                              |
                    MySQL / MariaDB (JDBC)
```

### Package structure

```
com.sunrise
├── model/        MODEL      — User, Doctor, Patient, Appointment, Treatment, TimeSlot, Bill
├── dao/          MODEL      — DBConnection (Singleton), DAO interfaces + implementations
├── service/      MODEL      — AuthService, ValidationService, SlotService, BillingService
├── controller/   CONTROLLER — LoginServlet, AppointmentServlet, DoctorServlet, ...
├── api/          CONTROLLER — REST/JSON web services + JsonWriter
└── filter/                  — AuthFilter (session / cookie protection)
```

---

## Design Patterns Used

| Pattern | Location |
|---|---|
| MVC | Whole application |
| Singleton | `dao/DBConnection.java` |
| DAO | `dao/*Dao.java` + `dao/impl/*DaoImpl.java` |
| Factory | `dao/DaoFactory.java` |
| Front Controller | `filter/AuthFilter.java` |
| Strategy | `service/billing/*` (pricing per treatment type) |
| Builder | `model/Appointment` |

---

## How to Run

### Prerequisites

- JDK 17 (the project compiles with `--release 17`)
- Eclipse IDE for Enterprise Java and Web Developers
- Apache Tomcat 9.0.x
- XAMPP (MySQL / MariaDB running on port 3306)

### Steps

1. **Clone the repository**

   ```bash
   git clone https://github.com/HiruniThenuwara/-sunrise-dental-clinic-management-system-web-app.git
   ```

2. **Create the database**

   Start MySQL in the XAMPP control panel, open phpMyAdmin
   (`http://localhost/phpmyadmin`) and import:

   ```
   database/schema.sql
   ```

3. **Configure the database connection**

   Edit `src/main/resources/db.properties`:

   ```properties
   db.url=jdbc:mysql://localhost:3306/sunrise_dental
   db.username=root
   db.password=
   ```

4. **Import into Eclipse**

   `File > Import > Maven > Existing Maven Projects` and select the cloned folder.

5. **Add the Tomcat 9 server**

   `Window > Show View > Servers > New Server > Apache Tomcat v9.0`

6. **Run**

   Right click the project > `Run As > Run on Server`, then open:

   ```
   http://localhost:8080/sunrise-dental-clinic/login.jsp
   ```

### Running it on another computer with XAMPP only

XAMPP ships both MySQL and Tomcat, so a second machine needs nothing else except a
**JDK 17 or newer** with `JAVA_HOME` set. The application targets **Servlet 3.1**, which
XAMPP's Tomcat 8.5 supports, and it runs unchanged on Tomcat 9 and 10.0.

1. Start **MySQL** in the XAMPP Control Panel
2. Open **phpMyAdmin** → *Import* → choose `database/schema.sql` → *Go*
3. Copy `target/sunrise-dental-clinic.war` into `C:\xampp\tomcat\webapps\`
4. Start **Tomcat** in the XAMPP Control Panel
5. Open `http://localhost:8080/sunrise-dental-clinic/`

If that machine's MySQL has a root password, set it in
`C:\xampp\tomcat\webapps\sunrise-dental-clinic\WEB-INF\classes\db.properties`
after Tomcat has unpacked the WAR, then restart Tomcat.

### Alternative: run from the terminal

If you prefer not to use the Eclipse server view, build the WAR and drop it into Tomcat:

```bash
mvn clean package                 # produces target/sunrise-dental-clinic.war
copy target\sunrise-dental-clinic.war C:\tomcat9\webapps\
C:\tomcat9\bin\startup.bat        # start Tomcat  (shutdown.bat to stop)
```

Then open `http://localhost:8080/sunrise-dental-clinic/`.

### Default logins

| Username | Password | Role | Can do |
|---|---|---|---|
| `admin` | `admin123` | Administrator | Dentists, treatments, working hours, reports, staff accounts |
| `nimali` | `nimali123` | Receptionist | Register appointments, search, billing |

> These demonstration passwords must be changed before any real deployment.
> Passwords are stored as a SHA-256 hash of (salt + password) with a unique random
> salt per account; the plain password is never stored or logged.

---

## Web Services

The system is a distributed application: the browser and any other client read and
write through JSON endpoints. All of them sit behind the same login and return
`401` with a JSON body when called without a session.

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/doctors` | GET | Dentists available for booking |
| `/api/slots?doctorId=1&date=2026-09-14` | GET | Bookable times, with taken ones marked |
| `/api/appointments?no=APT-20260914-001` | GET | One visit |
| `/api/appointments?date=2026-09-14` | GET | A day's list |
| `/api/appointments` | POST | Register a visit |

`POST /api/appointments` uses the status code to say what happened: **201** created,
**400** invalid input, **409** the slot is already taken. That distinction lets another
system tell "you typed something wrong" apart from "somebody else just took that time".

```bash
curl -i "http://localhost:8080/sunrise-dental-clinic/api/slots?doctorId=1&date=2026-09-14"
```

---

## Testing

```bash
mvn clean test
```

**139 automated tests**, all passing. The report is written to `target/surefire-reports/`.

| Test class | Cases | Covers |
|---|---|---|
| `AuthServiceTest` | 10 | Login, hashing, remember-me |
| `ValidationServiceTest` | 62 | Every input rule, including injection strings |
| `SlotServiceTest` | 12 | Slot generation and availability |
| `AppointmentServiceTest` | 13 | Numbering and double booking |
| `BillingServiceTest` | 16 | Pricing rules and refusals |
| `DoctorDaoImplTest` | 10 | Real SQL against H2 |
| `AppointmentDaoImplTest` | 11 | Transactions and the unique constraint |
| `BookingEndToEndTest` | 5 | The whole journey, nothing mocked |

Unit tests use **Mockito** mocks in place of the DAOs and integration tests use an
in-memory **H2** database, so the whole suite runs with no MySQL server. That is why
it can run unattended on the build server.

**Test driven development** was used for every service class. The git history shows a
commit named `(TDD red)` immediately before each matching `(TDD green)` commit.

---

## Continuous Integration

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on every push and pull
request:

1. Check out and set up JDK 17 with a cached Maven repository
2. `mvn clean compile`
3. `mvn test` — all 139 tests
4. `mvn package` — builds the deployable WAR
5. Uploads the Surefire test report as an artifact (**even when the build fails**,
   because that is when the report matters most)
6. Uploads the WAR
7. Writes a pass/fail summary onto the run page

---

## Project Structure

```
src/main/java/com/sunrise/
├── model/        Entities and enums (User, Doctor, Patient, Appointment, Bill, ...)
├── dao/          DAO interfaces, DaoFactory, DBConnection (Singleton)
│   └── impl/     JDBC implementations
├── service/      Business rules (Auth, Validation, Slot, Appointment, Billing, Report, Staff)
│   └── billing/  BillingStrategy and its three implementations
├── controller/   Servlets
├── api/          REST/JSON web services + JsonWriter
└── filter/       AuthFilter (Front Controller)

src/main/webapp/
├── WEB-INF/views/   JSP views, unreachable by direct URL
└── assets/          CSS and JavaScript

src/test/java/       139 tests
database/schema.sql  MySQL schema and seed data
```

---

## Version Control Workflow

```
main      -> stable, tagged releases
develop   -> daily integration branch
feature/* -> one branch per feature, merged through Pull Requests
```

| Tag | Milestone |
|---|---|
| `v0.1.0` | Login, authentication and admin layout |
| `v0.2.0` | All user interface pages |
| `v0.3.0` | Database, business logic and web services |
| `v1.0.0` | Tests, CI/CD pipeline and documentation |

Commit messages follow a consistent style, and the test driven pairs are visible in
the log:

```
test: add failing appointment and double booking tests (TDD red)
feat(appointment): implement appointment service (TDD green)
```

---

## Security Notes

| Concern | How it is handled |
|---|---|
| SQL injection | Every query uses `PreparedStatement` with `?` placeholders; proved by a test that stores `Robert'); DROP TABLE doctors; --` and checks the table survives |
| Password storage | SHA-256 over (unique salt + password); never stored or logged in plain text |
| Session fixation | The session is invalidated and a new id issued after a successful login |
| Cross site scripting | Output is escaped with `<c:out>`; the JSON writer escapes quotes, backslashes and control characters |
| Cookie theft | The remember-me cookie is `HttpOnly` and signed, so it cannot be read by JavaScript or forged |
| Unauthorised access | `AuthFilter` guards `/admin/*` and `/api/*`; role checks are enforced in the servlet, not just by hiding menu items |
| Cached pages | Protected pages send `no-store`, so the Back button cannot show the previous user's data |
| Deletion of history | Dentists, treatments and staff accounts are deactivated, never deleted, so past records stay intact |

Continuous integration runs `mvn clean test` on every push and pull request through
GitHub Actions (`.github/workflows/ci.yml`).

---

## Author

Developed as an individual coursework submission for
**CIS6003 Advanced Programming**, Cardiff Metropolitan University / ICBT Campus.
