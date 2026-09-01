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

### Alternative: run from the terminal

If you prefer not to use the Eclipse server view, build the WAR and drop it into Tomcat:

```bash
mvn clean package                 # produces target/sunrise-dental-clinic.war
copy target\sunrise-dental-clinic.war C:\tomcat9\webapps\
C:\tomcat9\bin\startup.bat        # start Tomcat  (shutdown.bat to stop)
```

Then open `http://localhost:8080/sunrise-dental-clinic/`.

### Default login

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | Administrator |

> Change this password before any real deployment. It exists only for demonstration.

---

## Running the Tests

```bash
mvn clean test
```

Test reports are generated in `target/surefire-reports/`.
Unit tests use **Mockito** mock DAOs, so no database is required to run them.

---

## Project Documentation

The system design (use case, class and sequence diagrams), the test plan and the
requirement traceability matrix are provided in the submitted assessment report
rather than in this repository.

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

Continuous integration runs `mvn clean test` on every push and pull request through
GitHub Actions (`.github/workflows/ci.yml`).

---

## Author

Developed as an individual coursework submission for
**CIS6003 Advanced Programming**, Cardiff Metropolitan University / ICBT Campus.
