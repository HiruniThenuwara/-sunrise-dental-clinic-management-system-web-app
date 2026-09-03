-- =====================================================================
--  Migration 02 - how the appointment was made
--
--  Records whether the patient walked in or booked online. The clinic
--  needs the distinction for planning: a morning full of walk-ins is a
--  different staffing problem from a morning of booked slots.
--
--  Existing rows are set to WALK_IN, because every appointment made
--  before this column existed was taken at the front desk.
--
--  Run on a database created before this column existed. A fresh install
--  does not need it: the column is now part of schema.sql.
-- =====================================================================

USE sunrise_dental;

ALTER TABLE appointments
    ADD COLUMN booking_type VARCHAR(10) NOT NULL DEFAULT 'WALK_IN'
        AFTER treatment_id;

UPDATE appointments SET booking_type = 'WALK_IN' WHERE booking_type IS NULL;

SELECT 'booking_type added' AS result,
       SUM(booking_type = 'WALK_IN') AS walk_in_rows,
       SUM(booking_type = 'ONLINE')  AS online_rows
FROM appointments;
