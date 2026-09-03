-- =====================================================================
--  SUNRISE DENTAL CLINIC - MANAGEMENT SYSTEM
--  Database schema and seed data
--
--  Target : MySQL 8 / MariaDB 10.4+ (XAMPP)
--  Import : phpMyAdmin -> Import -> choose this file -> Go
--           or:  mysql -u root < database/schema.sql
-- =====================================================================

DROP DATABASE IF EXISTS sunrise_dental;
CREATE DATABASE sunrise_dental
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;
USE sunrise_dental;


-- =====================================================================
--  1. users  -  staff accounts for the login screen (Requirement 1)
--     Passwords are never stored in plain text. We store a SHA-256 hash
--     of (salt + password) and a unique random salt for every user.
-- =====================================================================
CREATE TABLE users (
    user_id        INT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL,
    password_hash  CHAR(64)     NOT NULL,
    salt           CHAR(32)     NOT NULL,
    full_name      VARCHAR(100) NOT NULL,
    role           ENUM('ADMIN','RECEPTIONIST') NOT NULL DEFAULT 'RECEPTIONIST',
    is_active      TINYINT(1)   NOT NULL DEFAULT 1,
    last_login     DATETIME     NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_username UNIQUE (username)
) ENGINE=InnoDB;


-- =====================================================================
--  2. doctors  -  dentists working at the clinic
-- =====================================================================
CREATE TABLE doctors (
    doctor_id        INT AUTO_INCREMENT PRIMARY KEY,
    doctor_name      VARCHAR(100)  NOT NULL,
    specialization   VARCHAR(100)  NOT NULL,
    contact_number   VARCHAR(15)   NULL,
    email            VARCHAR(100)  NULL,
    consultation_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    is_active        TINYINT(1)    NOT NULL DEFAULT 1,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_doctor_active (is_active)
) ENGINE=InnoDB;


-- =====================================================================
--  3. doctor_schedule  -  working hours used to generate time slots
--     One row per doctor per working day.
--     slot_duration_minutes decides how many slots that day produces.
-- =====================================================================
CREATE TABLE doctor_schedule (
    schedule_id           INT AUTO_INCREMENT PRIMARY KEY,
    doctor_id             INT        NOT NULL,
    day_of_week           ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY',
                               'FRIDAY','SATURDAY','SUNDAY') NOT NULL,
    start_time            TIME       NOT NULL,
    end_time              TIME       NOT NULL,
    slot_duration_minutes INT        NOT NULL DEFAULT 30,
    is_active             TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_schedule_doctor FOREIGN KEY (doctor_id)
        REFERENCES doctors(doctor_id) ON DELETE CASCADE,
    CONSTRAINT uq_doctor_day UNIQUE (doctor_id, day_of_week)
) ENGINE=InnoDB;


-- =====================================================================
--  4. treatments  -  treatment types and their base cost (Requirement 4)
-- =====================================================================
CREATE TABLE treatments (
    treatment_id      INT AUTO_INCREMENT PRIMARY KEY,
    treatment_name    VARCHAR(100)  NOT NULL,
    description       VARCHAR(255)  NULL,
    base_cost         DECIMAL(10,2) NOT NULL,
    estimated_minutes INT           NOT NULL DEFAULT 30,
    is_active         TINYINT(1)    NOT NULL DEFAULT 1,
    CONSTRAINT uq_treatment_name UNIQUE (treatment_name)
) ENGINE=InnoDB;


-- =====================================================================
--  5. patients  -  patient personal details (Requirement 2)
-- =====================================================================
CREATE TABLE patients (
    patient_id     INT AUTO_INCREMENT PRIMARY KEY,
    patient_name   VARCHAR(100) NOT NULL,
    address        VARCHAR(255) NOT NULL,
    contact_number VARCHAR(15)  NOT NULL,
    email          VARCHAR(100) NULL,
    nic            VARCHAR(20)  NULL,
    date_of_birth  DATE         NULL,
    gender         ENUM('MALE','FEMALE','OTHER') NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_patient_contact (contact_number),
    INDEX idx_patient_name (patient_name)
) ENGINE=InnoDB;


-- =====================================================================
--  6. appointments  -  the core table (Requirements 2 and 3)
--
--  BUSINESS RULE - NO DOUBLE BOOKING:
--  uq_doctor_slot makes it impossible for the same dentist to have two
--  appointments on the same date at the same time. The service layer
--  checks this first and shows a friendly message, and this constraint
--  is the final safety net at database level.
-- =====================================================================
CREATE TABLE appointments (
    appointment_id   INT AUTO_INCREMENT PRIMARY KEY,
    appointment_no   VARCHAR(20) NOT NULL,
    patient_id       INT         NOT NULL,
    doctor_id        INT         NOT NULL,
    treatment_id     INT         NOT NULL,
    booking_type     VARCHAR(10) NOT NULL DEFAULT 'WALK_IN',
    appointment_date DATE        NOT NULL,
    appointment_time TIME        NOT NULL,
    status           ENUM('BOOKED','COMPLETED','CANCELLED','NO_SHOW')
                     NOT NULL DEFAULT 'BOOKED',
    notes            VARCHAR(255) NULL,
    created_by       INT          NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_appointment_no UNIQUE (appointment_no),
    CONSTRAINT uq_doctor_slot    UNIQUE (doctor_id, appointment_date, appointment_time),

    CONSTRAINT fk_appointment_patient   FOREIGN KEY (patient_id)   REFERENCES patients(patient_id),
    CONSTRAINT fk_appointment_doctor    FOREIGN KEY (doctor_id)    REFERENCES doctors(doctor_id),
    CONSTRAINT fk_appointment_treatment FOREIGN KEY (treatment_id) REFERENCES treatments(treatment_id),
    CONSTRAINT fk_appointment_user      FOREIGN KEY (created_by)   REFERENCES users(user_id),

    INDEX idx_appointment_date (appointment_date),
    INDEX idx_appointment_status (status)
) ENGINE=InnoDB;


-- =====================================================================
--  7. bills  -  patient bill / receipt (Requirement 4)
--     total_amount = consultation_fee + treatment_cost - discount + tax
--     One bill per appointment (uq_bill_appointment).
-- =====================================================================
CREATE TABLE bills (
    bill_id          INT AUTO_INCREMENT PRIMARY KEY,
    bill_no          VARCHAR(20)   NOT NULL,
    appointment_id   INT           NOT NULL,
    consultation_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    treatment_cost   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount         DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    tax              DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_amount     DECIMAL(10,2) NOT NULL,
    payment_method   ENUM('CASH','CARD','INSURANCE') NOT NULL DEFAULT 'CASH',
    payment_status   ENUM('PAID','PENDING')          NOT NULL DEFAULT 'PAID',
    billed_by        INT           NULL,
    billed_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_bill_no          UNIQUE (bill_no),
    CONSTRAINT uq_bill_appointment UNIQUE (appointment_id),
    CONSTRAINT fk_bill_appointment FOREIGN KEY (appointment_id)
        REFERENCES appointments(appointment_id) ON DELETE CASCADE,
    CONSTRAINT fk_bill_user FOREIGN KEY (billed_by) REFERENCES users(user_id)
) ENGINE=InnoDB;


-- =====================================================================
--  8. activity_log  -  who did what, and when
--
--  The clinic handles patient records, so it must be possible to answer
--  "who changed this?" after the fact. The username is stored as text as
--  well as by id, so the log still reads correctly if an account is later
--  renamed or withdrawn, and user_id is nullable because a failed login
--  has nobody signed in.
-- =====================================================================
CREATE TABLE activity_log (
    log_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT          NULL,
    username   VARCHAR(50)  NULL,
    action     VARCHAR(40)  NOT NULL,
    entity     VARCHAR(40)  NULL,
    entity_ref VARCHAR(50)  NULL,
    details    VARCHAR(255) NULL,
    ip_address VARCHAR(45)  NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_activity_created (created_at),
    INDEX idx_activity_user (user_id),
    INDEX idx_activity_action (action),

    CONSTRAINT fk_activity_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;


-- =====================================================================
--  SEED DATA
-- =====================================================================

-- ---------------------------------------------------------------------
--  Staff logins
--  admin  / admin123     (Administrator)
--  nimali / nimali123    (Receptionist)
--
--  password_hash = SHA-256( salt + plain password ), stored as hex.
--  These demo passwords must be changed before any real deployment.
-- ---------------------------------------------------------------------
INSERT INTO users (username, password_hash, salt, full_name, role) VALUES
('admin',
 '1fc37713e585017314deed4f1a503ff7e5a59e61c17a6132963b5debd23499f4',
 '9f2c1a7b4e8d0356af61bc94d27e3081',
 'System Administrator', 'ADMIN'),
('nimali',
 '2a1febbecb2a553012d1b72e64bc6d779ba0cf44dcee7f9799f16f559d262f09',
 '3d81ea5c0b7f42196ac8de5307b1f6a2',
 'Nimali Perera', 'RECEPTIONIST');


-- ---------------------------------------------------------------------
--  Dentists
-- ---------------------------------------------------------------------
INSERT INTO doctors (doctor_name, specialization, contact_number, email, consultation_fee) VALUES
('Dr. Anura Jayasinghe', 'General Dentistry',   '0771234567', 'anura@sunrisedental.lk',  1500.00),
('Dr. Sanduni Fernando', 'Orthodontics',        '0772345678', 'sanduni@sunrisedental.lk', 2500.00),
('Dr. Kasun Silva',      'Oral Surgery',        '0773456789', 'kasun@sunrisedental.lk',   3000.00),
('Dr. Malsha Weerasinghe','Pediatric Dentistry','0774567890', 'malsha@sunrisedental.lk',  2000.00);


-- ---------------------------------------------------------------------
--  Working hours  ->  used by SlotService to generate time slots
--  Example: Dr. Anura on Monday 09:00-17:00 with 30 minute slots = 16 slots
-- ---------------------------------------------------------------------
INSERT INTO doctor_schedule (doctor_id, day_of_week, start_time, end_time, slot_duration_minutes) VALUES
(1, 'MONDAY',    '09:00:00', '17:00:00', 30),
(1, 'WEDNESDAY', '09:00:00', '17:00:00', 30),
(1, 'FRIDAY',    '09:00:00', '13:00:00', 30),
(2, 'TUESDAY',   '10:00:00', '18:00:00', 45),
(2, 'THURSDAY',  '10:00:00', '18:00:00', 45),
(2, 'SATURDAY',  '09:00:00', '13:00:00', 45),
(3, 'MONDAY',    '08:00:00', '14:00:00', 60),
(3, 'THURSDAY',  '08:00:00', '14:00:00', 60),
(4, 'WEDNESDAY', '14:00:00', '19:00:00', 30),
(4, 'SATURDAY',  '09:00:00', '15:00:00', 30);


-- ---------------------------------------------------------------------
--  Treatment types and base cost (LKR)
-- ---------------------------------------------------------------------
INSERT INTO treatments (treatment_name, description, base_cost, estimated_minutes) VALUES
('Consultation',    'General dental check-up and advice',        0.00,    15),
('Scaling',         'Professional teeth cleaning and polishing', 4500.00, 45),
('Filling',         'Composite or amalgam cavity filling',       6000.00, 45),
('Tooth Extraction','Simple or surgical tooth removal',          5000.00, 30),
('Root Canal',      'Root canal treatment (endodontic)',        25000.00, 90),
('Crown Fitting',   'Porcelain or metal crown placement',       35000.00, 60),
('Teeth Whitening', 'Cosmetic bleaching treatment',            15000.00,  60),
('Braces Fitting',  'Orthodontic braces installation',         85000.00, 120),
('Denture Fitting', 'Partial or complete denture fitting',     45000.00,  60),
('X-Ray',           'Dental radiograph',                        2000.00,  15);


-- ---------------------------------------------------------------------
--  Sample patients and appointments
--  (used to test "Display Appointment Details" and billing)
-- ---------------------------------------------------------------------
INSERT INTO patients (patient_name, address, contact_number, email, nic, date_of_birth, gender) VALUES
('Saman Kumara',    'No 45, Galle Road, Colombo 03',   '0712345678', 'saman@gmail.com',  '199012345678', '1990-04-12', 'MALE'),
('Dilini Rathnayake','12/A, Temple Lane, Nugegoda',    '0723456789', 'dilini@gmail.com', '199523456789', '1995-09-25', 'FEMALE'),
('Ruwan Perera',    'No 8, Station Road, Dehiwala',    '0761234567', NULL,               '198734567890', '1987-01-30', 'MALE');

INSERT INTO appointments
    (appointment_no, patient_id, doctor_id, treatment_id, appointment_date, appointment_time, status, notes, created_by) VALUES
('APT-20260901-001', 1, 1, 2, '2026-09-01', '09:00:00', 'BOOKED',    'Regular cleaning',            1),
('APT-20260901-002', 2, 2, 8, '2026-09-01', '10:45:00', 'BOOKED',    'Braces consultation follow-up', 1),
('APT-20260902-001', 3, 3, 4, '2026-09-02', '08:00:00', 'COMPLETED', 'Lower left molar extraction',  2);

INSERT INTO bills
    (bill_no, appointment_id, consultation_fee, treatment_cost, discount, tax, total_amount, payment_method, payment_status, billed_by) VALUES
('BILL-20260902-001', 3, 3000.00, 5000.00, 0.00, 0.00, 8000.00, 'CASH', 'PAID', 2);


-- =====================================================================
--  Quick verification
-- =====================================================================
SELECT 'users'        AS table_name, COUNT(*) AS rows_inserted FROM users
UNION ALL SELECT 'doctors',          COUNT(*) FROM doctors
UNION ALL SELECT 'doctor_schedule',  COUNT(*) FROM doctor_schedule
UNION ALL SELECT 'treatments',       COUNT(*) FROM treatments
UNION ALL SELECT 'patients',         COUNT(*) FROM patients
UNION ALL SELECT 'appointments',     COUNT(*) FROM appointments
UNION ALL SELECT 'bills',            COUNT(*) FROM bills;
