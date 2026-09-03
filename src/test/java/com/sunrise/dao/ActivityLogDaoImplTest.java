package com.sunrise.dao;

import com.sunrise.dao.impl.ActivityLogDaoImpl;
import com.sunrise.model.ActivityAction;
import com.sunrise.model.ActivityLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link ActivityLogDaoImpl}.
 *
 * <p>The log is what lets the clinic answer "who changed this?" after the
 * fact, so these tests check the two things that would quietly ruin it: that
 * an entry with no signed in user is still recorded, and that the filters
 * return what they claim to.</p>
 */
@DisplayName("ActivityLogDaoImpl - the audit trail")
class ActivityLogDaoImplTest {

    private TestDatabase database;
    private ActivityLogDao activityLogDao;

    @BeforeEach
    void setUp() {
        database = new TestDatabase();
        activityLogDao = new ActivityLogDaoImpl(database.connectionSource());

        database.execute("INSERT INTO users (user_id, username, password_hash, salt, "
                + "full_name, role, is_active) VALUES "
                + "(1, 'admin', 'hash', 'salt', 'System Administrator', 'ADMIN', TRUE)");
        database.execute("INSERT INTO users (user_id, username, password_hash, salt, "
                + "full_name, role, is_active) VALUES "
                + "(2, 'nimali', 'hash', 'salt', 'Nimali Perera', 'RECEPTIONIST', TRUE)");
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    private ActivityLog entry(Integer userId, String username, ActivityAction action, String ref) {
        ActivityLog log = new ActivityLog(userId, username, action);
        log.setEntity("Appointment");
        log.setEntityRef(ref);
        log.setDetails("Recorded by a test");
        log.setIpAddress("127.0.0.1");
        return log;
    }

    // -----------------------------------------------------------------
    //  Writing
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-01 an entry is stored and given an id and a timestamp")
    void storesAnEntry() {
        ActivityLog saved = activityLogDao.insert(
                entry(2, "nimali", ActivityAction.APPOINTMENT_CREATED, "APT-20260914-001"));

        List<ActivityLog> recent = activityLogDao.findRecent(10);

        assertAll(
                () -> assertTrue(saved.getLogId() > 0),
                () -> assertEquals(1, recent.size()),
                () -> assertNotNull(recent.get(0).getCreatedAt(),
                        "The database must supply the time it happened"),
                () -> assertEquals("nimali", recent.get(0).getUsername()),
                () -> assertEquals(ActivityAction.APPOINTMENT_CREATED, recent.get(0).getAction()),
                () -> assertEquals("APT-20260914-001", recent.get(0).getEntityRef())
        );
    }

    @Test
    @DisplayName("TC-02 a failed sign in is recorded even though nobody is signed in")
    void storesAnEntryWithNoUserId() {
        ActivityLog failed = new ActivityLog(null, "hacker", ActivityAction.LOGIN_FAILED);
        failed.setDetails("Sign in refused");
        failed.setIpAddress("10.0.0.5");

        activityLogDao.insert(failed);
        ActivityLog stored = activityLogDao.findRecent(1).get(0);

        assertAll(
                () -> assertNull(stored.getUserId(), "There is no account to point at"),
                () -> assertEquals("hacker", stored.getUsername(),
                        "but the name that was tried must still be recorded"),
                () -> assertEquals(ActivityAction.LOGIN_FAILED, stored.getAction())
        );
    }

    @Test
    @DisplayName("TC-03 a very long detail line is shortened rather than losing the entry")
    void trimsAnOverlongDetailLine() {
        ActivityLog log = new ActivityLog(1, "admin", ActivityAction.STAFF_UPDATED);
        log.setDetails("x".repeat(400));

        activityLogDao.insert(log);

        assertAll(
                () -> assertEquals(1, activityLogDao.findRecent(10).size(),
                        "The entry must survive"),
                () -> assertTrue(activityLogDao.findRecent(1).get(0).getDetails().length() <= 255)
        );
    }

    // -----------------------------------------------------------------
    //  Reading and filtering
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-04 the newest entry is returned first")
    void returnsNewestFirst() {
        activityLogDao.insert(entry(2, "nimali", ActivityAction.LOGIN_SUCCESS, "nimali"));
        activityLogDao.insert(entry(2, "nimali", ActivityAction.APPOINTMENT_CREATED, "APT-002"));
        activityLogDao.insert(entry(1, "admin", ActivityAction.DOCTOR_CREATED, "Dr. New"));

        List<ActivityLog> recent = activityLogDao.findRecent(10);

        assertAll(
                () -> assertEquals(3, recent.size()),
                () -> assertEquals("Dr. New", recent.get(0).getEntityRef(),
                        "The most recent action must be at the top")
        );
    }

    @Test
    @DisplayName("TC-05 the limit is respected")
    void respectsTheLimit() {
        for (int i = 0; i < 8; i++) {
            activityLogDao.insert(entry(2, "nimali", ActivityAction.LOGIN_SUCCESS, "entry" + i));
        }

        assertEquals(3, activityLogDao.findRecent(3).size());
    }

    @Test
    @DisplayName("TC-06 filtering by staff member returns only their actions")
    void filtersByUsername() {
        activityLogDao.insert(entry(2, "nimali", ActivityAction.APPOINTMENT_CREATED, "APT-001"));
        activityLogDao.insert(entry(2, "nimali", ActivityAction.BILL_CREATED, "BILL-001"));
        activityLogDao.insert(entry(1, "admin", ActivityAction.DOCTOR_CREATED, "Dr. New"));

        List<ActivityLog> nimalisWork = activityLogDao.search("nimali", null, null, null, 50);

        assertAll(
                () -> assertEquals(2, nimalisWork.size()),
                () -> assertTrue(nimalisWork.stream()
                        .allMatch(e -> "nimali".equals(e.getUsername())))
        );
    }

    @Test
    @DisplayName("TC-07 filtering by action returns only that action")
    void filtersByAction() {
        activityLogDao.insert(entry(2, "nimali", ActivityAction.APPOINTMENT_CREATED, "APT-001"));
        activityLogDao.insert(entry(2, "nimali", ActivityAction.APPOINTMENT_CREATED, "APT-002"));
        activityLogDao.insert(entry(1, "admin", ActivityAction.LOGIN_FAILED, "admin"));

        List<ActivityLog> bookings = activityLogDao.search(
                null, ActivityAction.APPOINTMENT_CREATED.name(), null, null, 50);

        assertEquals(2, bookings.size());
    }

    @Test
    @DisplayName("TC-08 no filter returns everything")
    void returnsEverythingWithNoFilter() {
        activityLogDao.insert(entry(2, "nimali", ActivityAction.APPOINTMENT_CREATED, "APT-001"));
        activityLogDao.insert(entry(1, "admin", ActivityAction.DOCTOR_CREATED, "Dr. New"));

        assertEquals(2, activityLogDao.search(null, null, null, null, 50).size());
    }

    @Test
    @DisplayName("TC-09 today's entries are inside a date range that covers today")
    void filtersByDateRange() {
        activityLogDao.insert(entry(2, "nimali", ActivityAction.APPOINTMENT_CREATED, "APT-001"));

        LocalDate today = LocalDate.now();

        assertAll(
                () -> assertEquals(1, activityLogDao.search(
                        null, null, today, today, 50).size(),
                        "A range of today to today must include an entry made today"),
                () -> assertEquals(0, activityLogDao.search(
                        null, null, today.plusDays(1), today.plusDays(2), 50).size(),
                        "A future range must be empty")
        );
    }

    // -----------------------------------------------------------------
    //  Counting, for the statistic cards
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-10 today's actions are counted")
    void countsTodaysEntries() {
        activityLogDao.insert(entry(2, "nimali", ActivityAction.LOGIN_SUCCESS, "nimali"));
        activityLogDao.insert(entry(2, "nimali", ActivityAction.APPOINTMENT_CREATED, "APT-001"));

        assertAll(
                () -> assertEquals(2, activityLogDao.countByDate(LocalDate.now())),
                () -> assertEquals(0, activityLogDao.countByDate(LocalDate.now().minusDays(3)))
        );
    }

    @Test
    @DisplayName("TC-11 failed sign ins are counted on their own")
    void countsOneAction() {
        activityLogDao.insert(entry(null, "hacker", ActivityAction.LOGIN_FAILED, "hacker"));
        activityLogDao.insert(entry(null, "hacker", ActivityAction.LOGIN_FAILED, "hacker"));
        activityLogDao.insert(entry(2, "nimali", ActivityAction.LOGIN_SUCCESS, "nimali"));

        assertAll(
                () -> assertEquals(2, activityLogDao.countByAction(
                        ActivityAction.LOGIN_FAILED.name(), LocalDate.now().minusDays(7))),
                () -> assertEquals(1, activityLogDao.countByAction(
                        ActivityAction.LOGIN_SUCCESS.name(), null))
        );
    }

    @Test
    @DisplayName("TC-12 the filter dropdown lists each name once")
    void listsDistinctUsernames() {
        activityLogDao.insert(entry(2, "nimali", ActivityAction.LOGIN_SUCCESS, "nimali"));
        activityLogDao.insert(entry(2, "nimali", ActivityAction.BILL_CREATED, "BILL-001"));
        activityLogDao.insert(entry(1, "admin", ActivityAction.DOCTOR_CREATED, "Dr. New"));

        List<String> names = activityLogDao.distinctUsernames();

        assertAll(
                () -> assertEquals(2, names.size()),
                () -> assertTrue(names.contains("nimali")),
                () -> assertTrue(names.contains("admin"))
        );
    }
}
