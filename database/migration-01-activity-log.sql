-- =====================================================================
--  Migration 01 - activity log
--
--  Run this on a database that was created before the activity log
--  existed. A fresh install does not need it, because the table is now
--  part of schema.sql.
--
--  Import through phpMyAdmin, or:
--      mysql -u root sunrise_dental < database/migration-01-activity-log.sql
-- =====================================================================

USE sunrise_dental;

CREATE TABLE IF NOT EXISTS activity_log (
    log_id     INT AUTO_INCREMENT PRIMARY KEY,

    -- Who did it. The id may be NULL for a failed login, where nobody is
    -- signed in, so the username is stored as text as well. Keeping the
    -- name means the log still reads correctly even if the account is
    -- later renamed or withdrawn.
    user_id    INT          NULL,
    username   VARCHAR(50)  NULL,

    -- What they did, and to which record.
    action     VARCHAR(40)  NOT NULL,
    entity     VARCHAR(40)  NULL,
    entity_ref VARCHAR(50)  NULL,
    details    VARCHAR(255) NULL,

    -- Where from. Long enough for an IPv6 address.
    ip_address VARCHAR(45)  NULL,

    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_activity_created (created_at),
    INDEX idx_activity_user (user_id),
    INDEX idx_activity_action (action),

    -- ON DELETE SET NULL rather than CASCADE: if a staff row were ever
    -- removed, the history of what happened must not disappear with it.
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

SELECT 'activity_log created' AS result,
       COUNT(*) AS existing_rows
FROM activity_log;
