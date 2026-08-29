package com.sunrise.dao;

import com.sunrise.model.User;

import java.util.Optional;

/**
 * Data access contract for the {@code users} table.
 *
 * <p><b>Design pattern: DAO (Data Access Object).</b> The business layer
 * ({@code AuthService}) talks only to this interface and never writes SQL
 * itself. Two benefits follow:</p>
 *
 * <ul>
 *   <li>the database technology can change without touching business logic;</li>
 *   <li>unit tests can supply a Mockito mock of this interface, so the
 *       login rules are tested with <b>no database running</b> - which is
 *       what allows the tests to run on the GitHub Actions server.</li>
 * </ul>
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
     * Records the moment of a successful login, so the clinic can audit
     * who used the system and when.
     *
     * @param userId the user who just logged in
     * @return {@code true} if the row was updated
     */
    boolean updateLastLogin(int userId);
}
