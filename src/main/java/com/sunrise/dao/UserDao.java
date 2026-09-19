package com.sunrise.dao;

import com.sunrise.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Data access contract for the {@code users} table.
 *
 * <p><b>Design pattern: DAO (Data Access Object).</b> The business layer
 * ({@code AuthService}, {@code StaffService}) talks only to this interface
 * and never writes SQL itself. Two benefits follow:</p>
 *
 * <ul>
 *   <li>the database technology can change without touching business logic;</li>
 *   <li>unit tests can supply a Mockito mock of this interface, so the
 *       login rules are tested with <b>no database running</b> - which is
 *       what allows the tests to run on the GitHub Actions server.</li>
 * </ul>
 *
 * <p>There is no delete method. A staff member who leaves is deactivated,
 * because their name is recorded against the appointments they registered
 * and the bills they took payment for.</p>
 */
public interface UserDao {

    /**
     * Finds a staff account by its login name.
     *
     * @param username the login name typed on the login screen
     * @return the user wrapped in an {@link Optional}, or
     *         {@link Optional#empty()} if no such account exists
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a staff account by primary key.
     *
     * @param userId the primary key
     * @return the user, or {@link Optional#empty()} if not found
     */
    Optional<User> findById(int userId);

    /**
     * @return every staff account, administrators first then by name
     */
    List<User> findAll();

    /**
     * Records the moment of a successful login, so the clinic can audit
     * who used the system and when.
     *
     * @param userId the user who just logged in
     * @return {@code true} if the row was updated
     */
    boolean updateLastLogin(int userId);

    /**
     * Creates a staff account.
     *
     * @param user the account to store, carrying an already hashed password
     *             and its salt
     * @return the same object with its generated id filled in
     */
    User insert(User user);

    /**
     * Updates the name, role and status of an account. The password is not
     * touched here; it has its own method so that a routine edit can never
     * change someone's password by accident.
     *
     * @return {@code true} if one row was changed
     */
    boolean update(User user);

    /**
     * Replaces the password with a new hash and salt.
     *
     * @return {@code true} if one row was changed
     */
    boolean updatePassword(int userId, String passwordHash, String salt);

    /**
     * Enables or disables an account for logging in.
     *
     * @return {@code true} if one row was changed
     */
    boolean setActive(int userId, boolean active);

    /**
     * Checks whether a login name is already taken.
     *
     * @param username      the name being chosen
     * @param excludeUserId the account being edited, so it does not clash
     *                      with itself; pass 0 when creating
     * @return {@code true} when the name is already in use
     */
    boolean usernameExists(String username, int excludeUserId);

    /** One page of staff accounts, administrators first. */
    List<User> findPage(int offset, int limit);

    /** @return how many staff accounts exist, for the page count */
    int countAll();

    /** @return how many accounts hold that role */
    int countByRole(com.sunrise.model.Role role);

    /** @return how many accounts can still sign in */
    int countActive();
}
