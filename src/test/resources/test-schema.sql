-- =====================================================================
--  Schema for the in-memory H2 database used by the integration tests.
--
--  It mirrors database/schema.sql, with two deliberate differences:
--    * ENUM columns become VARCHAR with a CHECK constraint, because ENUM is
--      a MySQL extension. The values and the rules they enforce are the same.
--    * No seed data. Each test inserts exactly the rows it needs, so tests
--      cannot depend on one another or on rows left behind by an earlier run.
--
--  The constraints that matter are kept exactly as they are in production,
--  in particular uq_doctor_slot, which is what makes double booking
--  impossible at database level.
-- =====================================================================

CREATE TABLE users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(64)  NOT NULL,
    salt          VARCHAR(32)  NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'RECEPTIONIST'
                  CHECK (role IN ('ADMIN','RECEPTIONIST')),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login    TIMESTAMP    NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE doctors (
    doctor_id        INT AUTO_INCREMENT PRIMARY KEY,
    doctor_name      VARCHAR(100)  NOT NULL,
    specialization   VARCHAR(100)  NOT NULL,
    contact_number   VARCHAR(15)   NULL,
    email            VARCHAR(100)  NULL,
    consultation_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE doctor_schedule (
    schedule_id           INT AUTO_INCREMENT PRIMARY KEY,
    doctor_id             INT         NOT NULL,
    day_of_week           VARCHAR(10) NOT NULL,
    start_time            TIME        NOT NULL,
    end_time              TIME        NOT NULL,
    slot_duration_minutes INT         NOT NULL DEFAULT 30,
    is_active             BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_schedule_doctor FOREIGN KEY (doctor_id)
        REFERENCES doctors(doctor_id) ON DELETE CASCADE,
    CONSTRAINT uq_doctor_day UNIQUE (doctor_id, day_of_week)
);

CREATE TABLE treatments (
    treatment_id      INT AUTO_INCREMENT PRIMARY KEY,
    treatment_name    VARCHAR(100)  NOT NULL UNIQUE,
    description       VARCHAR(255)  NULL,
    base_cost         DECIMAL(10,2) NOT NULL,
    estimated_minutes INT           NOT NULL DEFAULT 30,
    is_active         BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE patients (
    patient_id     INT AUTO_INCREMENT PRIMARY KEY,
    patient_name   VARCHAR(100) NOT NULL,
    address        VARCHAR(255) NOT NULL,
    contact_number VARCHAR(15)  NOT NULL,
    email          VARCHAR(100) NULL,
    nic            VARCHAR(20)  NULL,
    date_of_birth  DATE         NULL,
    gender         VARCHAR(10)  NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE appointments (
    appointment_id   INT AUTO_INCREMENT PRIMARY KEY,
    appointment_no   VARCHAR(20) NOT NULL,
    patient_id       INT         NOT NULL,
    doctor_id        INT         NOT NULL,
    treatment_id     INT         NOT NULL,
    appointment_date DATE        NOT NULL,
    appointment_time TIME        NOT NULL,
    status           VARCHAR(15) NOT NULL DEFAULT 'BOOKED'
                     CHECK (status IN ('BOOKED','COMPLETED','CANCELLED','NO_SHOW')),
    notes            VARCHAR(255) NULL,
    created_by       INT          NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_appointment_no UNIQUE (appointment_no),

    -- The rule that makes double booking impossible.
    CONSTRAINT uq_doctor_slot UNIQUE (doctor_id, appointment_date, appointment_time),

    CONSTRAINT fk_appointment_patient   FOREIGN KEY (patient_id)   REFERENCES patients(patient_id),
    CONSTRAINT fk_appointment_doctor    FOREIGN KEY (doctor_id)    REFERENCES doctors(doctor_id),
    CONSTRAINT fk_appointment_treatment FOREIGN KEY (treatment_id) REFERENCES treatments(treatment_id),
    CONSTRAINT fk_appointment_user      FOREIGN KEY (created_by)   REFERENCES users(user_id)
);

CREATE TABLE bills (
    bill_id          INT AUTO_INCREMENT PRIMARY KEY,
    bill_no          VARCHAR(20)   NOT NULL UNIQUE,
    appointment_id   INT           NOT NULL UNIQUE,
    consultation_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    treatment_cost   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount         DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    tax              DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_amount     DECIMAL(10,2) NOT NULL,
    payment_method   VARCHAR(12)   NOT NULL DEFAULT 'CASH'
                     CHECK (payment_method IN ('CASH','CARD','INSURANCE')),
    payment_status   VARCHAR(10)   NOT NULL DEFAULT 'PAID'
                     CHECK (payment_status IN ('PAID','PENDING')),
    billed_by        INT           NULL,
    billed_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bill_appointment FOREIGN KEY (appointment_id)
        REFERENCES appointments(appointment_id) ON DELETE CASCADE,
    CONSTRAINT fk_bill_user FOREIGN KEY (billed_by) REFERENCES users(user_id)
);
