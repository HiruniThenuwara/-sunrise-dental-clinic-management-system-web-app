package com.sunrise.dao;

import com.sunrise.model.ActivityLog;

import java.time.LocalDate;
import java.util.List;

/**
 * Data access contract for the {@code activity_log} table.
 *
 * <p>There is no update and no delete. An audit trail that can be edited is
 * not an audit trail, so entries are written once and only ever read back.</p>
 */
public interface ActivityLogDao {

    /**
     * Records one action.
     *
     * @param entry what happened
     * @return the same object with its generated id filled in
     */
    ActivityLog insert(ActivityLog entry);

    /**
     * @param limit how many entries to return, newest first
     * @return the most recent activity
     */
    List<ActivityLog> findRecent(int limit);

    /**
     * The filtered view used by the activity screen. Any argument may be
     * left out, in which case that filter is not applied.
     *
     * @param username only this staff member, or {@code null} for everyone
     * @param action   only this action, or {@code null} for all actions
     * @param from     earliest date, or {@code null}
     * @param to       latest date, or {@code null}
     * @param limit    how many entries to return
     * @return matching entries, newest first
     */
    List<ActivityLog> search(String username, String action,
                             LocalDate from, LocalDate to, int limit);

    /**
     * @return how many entries were recorded on a date
     */
    int countByDate(LocalDate date);

    /**
     * @param action the action to count
     * @param from   earliest date, or {@code null} for all time
     * @return how many times that action was recorded
     */
    int countByAction(String action, LocalDate from);

    /**
     * @return the distinct usernames that appear in the log, for the filter
     *         dropdown
     */
    List<String> distinctUsernames();

    /**
     * One page of the log, newest first, with the same filters the screen
     * offers. The filters belong on the SQL rather than on the page of
     * results, otherwise page two of a filtered list would be wrong.
     */
    List<ActivityLog> searchPage(String username, String action,
                                 LocalDate from, LocalDate to,
                                 int offset, int limit);

    /** @return how many entries match those filters, for the page count */
    int countSearch(String username, String action, LocalDate from, LocalDate to);
}
