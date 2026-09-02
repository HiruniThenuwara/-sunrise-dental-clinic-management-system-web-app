package com.sunrise.service;

import com.sunrise.dao.DaoFactory;
import com.sunrise.dao.UserDao;
import com.sunrise.model.Role;
import com.sunrise.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Managing staff accounts, which only an administrator may do.
 *
 * <p>Requirement 1 says the system is for authorised staff, so somebody has
 * to be able to create and withdraw those accounts. Doing it here rather than
 * by editing the database directly means the rules are applied every time:
 * the login name is checked, the password is hashed with its own salt, and an
 * administrator cannot lock themselves out.</p>
 *
 * <p>The plain password never leaves this class. It arrives from the form,
 * is hashed immediately by {@link AuthService}, and only the hash and salt
 * are passed to the DAO.</p>
 */
public class StaffService {

    private static final Logger LOGGER = Logger.getLogger(StaffService.class.getName());

    private final UserDao userDao;
    private final ValidationService validationService;

    /** Production constructor - takes the DAO from the factory. */
    public StaffService() {
        this(DaoFactory.getUserDao(), new ValidationService());
    }

    /** Constructor used by the unit tests, so a mock DAO can be supplied. */
    public StaffService(UserDao userDao, ValidationService validationService) {
        this.userDao = userDao;
        this.validationService = validationService;
    }

    /** @return every staff account, for the management screen */
    public List<User> findAll() {
        return userDao.findAll();
    }

    public Optional<User> findById(int userId) {
        return userDao.findById(userId);
    }

    /**
     * Creates a staff account.
     *
     * @param password the plain password typed on the form; it is hashed
     *                 here and never stored or logged
     * @return the outcome, carrying the new account or the problems to show
     */
    public StaffResult create(String username, String fullName, String roleText,
                              String password, String confirmPassword, boolean active) {

        List<String> errors = new ArrayList<>();

        if (!validationService.isValidUsername(username)) {
            errors.add("Username must be 3 to 20 characters, using letters, digits "
                    + "and underscores only.");
        } else if (userDao.usernameExists(username.trim(), 0)) {
            errors.add("The username \"" + username.trim() + "\" is already taken.");
        }

        if (!validationService.isValidName(fullName)) {
            errors.add("Full name must be 3 to 100 letters, with no digits or symbols.");
        }
        if (!validationService.isAcceptablePassword(password)) {
            errors.add("Password must be at least 8 characters and contain "
                    + "both letters and digits.");
        } else if (!password.equals(confirmPassword)) {
            errors.add("The two passwords do not match.");
        }

        if (!errors.isEmpty()) {
            return StaffResult.failed(errors);
        }

        String salt = AuthService.generateSalt();

        User user = new User();
        user.setUsername(username.trim());
        user.setFullName(fullName.trim());
        user.setRole(Role.fromString(roleText));
        user.setSalt(salt);
        user.setPasswordHash(AuthService.hashPassword(password, salt));
        user.setActive(active);

        User saved = userDao.insert(user);

        if (saved.getUserId() <= 0) {
            return StaffResult.failed(List.of(
                    "The account could not be created. Please try again."));
        }

        LOGGER.info("Staff account created for " + saved.getUsername()
                + " with role " + saved.getRole());
        return StaffResult.created(saved);
    }

    /**
     * Updates the name, role and status of an existing account.
     *
     * @param currentUserId the administrator making the change, so they
     *                      cannot remove their own access by accident
     */
    public StaffResult update(int userId, String fullName, String roleText,
                              boolean active, int currentUserId) {

        List<String> errors = new ArrayList<>();

        Optional<User> found = userDao.findById(userId);
        if (found.isEmpty()) {
            return StaffResult.failed(List.of("That staff account no longer exists."));
        }

        if (!validationService.isValidName(fullName)) {
            errors.add("Full name must be 3 to 100 letters, with no digits or symbols.");
        }

        Role role = Role.fromString(roleText);

        // Two rules that stop an administrator locking themselves out of the
        // system, which would need a developer and a database client to undo.
        if (userId == currentUserId && !active) {
            errors.add("You cannot deactivate your own account.");
        }
        if (userId == currentUserId && role != Role.ADMIN) {
            errors.add("You cannot remove your own administrator access.");
        }

        if (!errors.isEmpty()) {
            return StaffResult.failed(errors);
        }

        User user = found.get();
        user.setFullName(fullName.trim());
        user.setRole(role);
        user.setActive(active);

        return userDao.update(user)
                ? StaffResult.updated(user)
                : StaffResult.failed(List.of("The account could not be updated."));
    }

    /**
     * Sets a new password for an account, used when a staff member forgets
     * theirs.
     *
     * <p>A new salt is generated as well as a new hash. That means every old
     * "remember me" cookie stops working immediately, because those cookies
     * are signed with the previous hash.</p>
     */
    public StaffResult resetPassword(int userId, String password, String confirmPassword) {

        if (!validationService.isAcceptablePassword(password)) {
            return StaffResult.failed(List.of(
                    "Password must be at least 8 characters and contain both letters and digits."));
        }
        if (!password.equals(confirmPassword)) {
            return StaffResult.failed(List.of("The two passwords do not match."));
        }

        Optional<User> found = userDao.findById(userId);
        if (found.isEmpty()) {
            return StaffResult.failed(List.of("That staff account no longer exists."));
        }

        String salt = AuthService.generateSalt();
        boolean changed = userDao.updatePassword(
                userId, AuthService.hashPassword(password, salt), salt);

        return changed
                ? StaffResult.updated(found.get())
                : StaffResult.failed(List.of("The password could not be changed."));
    }

    /**
     * Enables or disables an account.
     *
     * @param currentUserId the administrator making the change
     */
    public StaffResult setActive(int userId, boolean active, int currentUserId) {
        if (userId == currentUserId && !active) {
            return StaffResult.failed(List.of("You cannot deactivate your own account."));
        }

        Optional<User> found = userDao.findById(userId);
        if (found.isEmpty()) {
            return StaffResult.failed(List.of("That staff account no longer exists."));
        }

        return userDao.setActive(userId, active)
                ? StaffResult.updated(found.get())
                : StaffResult.failed(List.of("The account status could not be changed."));
    }

    /**
     * The outcome of a staff account change: either the account, or the
     * problems to show on the form.
     */
    public static final class StaffResult {

        private final boolean success;
        private final boolean newRecord;
        private final User user;
        private final List<String> errors;

        private StaffResult(boolean success, boolean newRecord, User user, List<String> errors) {
            this.success = success;
            this.newRecord = newRecord;
            this.user = user;
            this.errors = errors;
        }

        static StaffResult created(User user) {
            return new StaffResult(true, true, user, List.of());
        }

        static StaffResult updated(User user) {
            return new StaffResult(true, false, user, List.of());
        }

        static StaffResult failed(List<String> errors) {
            return new StaffResult(false, false, null, errors);
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isNewRecord() {
            return newRecord;
        }

        public User getUser() {
            return user;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
