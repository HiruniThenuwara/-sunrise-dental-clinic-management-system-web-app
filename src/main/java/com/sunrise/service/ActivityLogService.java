package com.sunrise.service;

import com.sunrise.dao.ActivityLogDao;
import com.sunrise.dao.DaoFactory;
import com.sunrise.model.ActivityAction;
import com.sunrise.model.ActivityLog;
import com.sunrise.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;

/**
 * Records what staff members do, and reads it back for the activity screen.
 *
 * <p>The clinic holds patient records, so it has to be possible to answer
 * "who changed this, and when?" afterwards. Requirement 1 restricts the
 * system to authorised staff; this is what makes that restriction
 * accountable rather than merely stated.</p>
 *
 * <p>Recording happens in the controller layer rather than inside the
 * business services. Two reasons: the controller is where the request lives,
 * so it knows who is signed in and from which address; and it keeps the
 * services free of a concern that has nothing to do with their rules, which
 * is why those services can still be unit tested with nothing but a mock
 * DAO.</p>
 *
 * <p>Writing an entry never throws. If the log cannot be written, the action
 * the staff member was performing still succeeds - an audit trail must not
 * be able to stop the clinic working.</p>
 */
public class ActivityLogService {

    private static final int DEFAULT_LIMIT = 200;

    private final ActivityLogDao activityLogDao;

    public ActivityLogService() {
        this(DaoFactory.getActivityLogDao());
    }

    public ActivityLogService(ActivityLogDao activityLogDao) {
        this.activityLogDao = activityLogDao;
    }

    // -----------------------------------------------------------------
    //  writing
    // -----------------------------------------------------------------

    /**
     * Records an action performed by the signed in user.
     *
     * @param request   the current request, used for the user and the address
     * @param action    what was done
     * @param entity    the kind of record, for example "Appointment"
     * @param entityRef which record, for example the appointment number
     * @param details   a short sentence for the log table
     */
    public void record(HttpServletRequest request, ActivityAction action,
                       String entity, String entityRef, String details) {

        User user = currentUser(request);

        ActivityLog entry = new ActivityLog(
                user == null ? null : user.getUserId(),
                user == null ? null : user.getUsername(),
                action);

        entry.setEntity(entity);
        entry.setEntityRef(entityRef);
        entry.setDetails(details);
        entry.setIpAddress(clientAddress(request));

        activityLogDao.insert(entry);
    }

    /**
     * Records a sign in attempt, successful or not.
     *
     * <p>A failed attempt has nobody signed in, so the username that was
     * typed is recorded on its own. Repeated failures against one username
     * are exactly what an administrator needs to be able to see.</p>
     */
    public void recordLoginAttempt(HttpServletRequest request, String username,
                                   User user, boolean successful) {

        ActivityLog entry = new ActivityLog(
                user == null ? null : user.getUserId(),
                user == null ? username : user.getUsername(),
                successful ? ActivityAction.LOGIN_SUCCESS : ActivityAction.LOGIN_FAILED);

        entry.setEntity("Account");
        entry.setEntityRef(user == null ? username : user.getUsername());
        entry.setDetails(successful
                ? "Signed in successfully"
                : "Sign in refused: wrong username or password");
        entry.setIpAddress(clientAddress(request));

        activityLogDao.insert(entry);
    }

    // -----------------------------------------------------------------
    //  reading
    // -----------------------------------------------------------------

    public List<ActivityLog> findRecent(int limit) {
        return activityLogDao.findRecent(limit);
    }

    public List<ActivityLog> search(String username, String action,
                                    LocalDate from, LocalDate to) {
        return activityLogDao.search(username, action, from, to, DEFAULT_LIMIT);
    }

    public int countToday() {
        return activityLogDao.countByDate(LocalDate.now());
    }

    /** @return failed sign in attempts in the last seven days */
    public int failedLoginsThisWeek() {
        return activityLogDao.countByAction(
                ActivityAction.LOGIN_FAILED.name(), LocalDate.now().minusDays(7));
    }

    public int countAction(ActivityAction action, LocalDate from) {
        return activityLogDao.countByAction(action.name(), from);
    }

    public List<String> knownUsernames() {
        return activityLogDao.distinctUsernames();
    }

    // -----------------------------------------------------------------
    //  helpers
    // -----------------------------------------------------------------

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object attribute = session.getAttribute(AuthService.SESSION_USER_KEY);
        return (attribute instanceof User) ? (User) attribute : null;
    }

    /**
     * The address the request came from.
     *
     * <p>{@code X-Forwarded-For} is checked first so the real address is
     * recorded if the clinic ever puts the system behind a proxy, rather
     * than logging the proxy's own address for every entry.</p>
     */
    private String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
