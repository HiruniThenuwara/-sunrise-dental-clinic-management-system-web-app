package com.sunrise.service;

import com.sunrise.dao.UserDao;
import com.sunrise.dao.impl.UserDaoImpl;
import com.sunrise.model.User;

import java.util.Optional;

/**
 * Staff login rules (Requirement 1) - <b>TDD RED STAGE</b>.
 *
 * <p>This is the empty skeleton written so that {@code AuthServiceTest}
 * compiles and can be executed. Every method still throws
 * {@link UnsupportedOperationException}, so all ten tests fail on purpose.
 * That failing run is the RED stage of test driven development and is
 * captured as a screenshot for the report.</p>
 *
 * <p>The real implementation is added in the next commit (GREEN stage).</p>
 */
public class AuthService {

    /** Session attribute that holds the logged in {@link User}. */
    public static final String SESSION_USER_KEY = "user";

    /** Name of the "remember me" cookie stored in the browser. */
    public static final String REMEMBER_COOKIE_NAME = "sdc_remember";

    /** The "remember me" cookie lives for 30 days. */
    public static final int REMEMBER_COOKIE_MAX_AGE = 30 * 24 * 60 * 60;

    private final UserDao userDao;

    /** Production constructor - uses the real JDBC DAO. */
    public AuthService() {
        this(new UserDaoImpl());
    }

    /**
     * Constructor used by the unit tests so a Mockito mock can be injected
     * in place of the real database.
     *
     * @param userDao the data access object to use
     */
    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Checks a username and password typed on the login screen.
     *
     * @param username the login name
     * @param password the plain text password
     * @return the logged in user, or {@link Optional#empty()} if refused
     */
    public Optional<User> authenticate(String username, String password) {
        throw new UnsupportedOperationException("Not implemented yet - TDD red stage");
    }

    /**
     * Builds the signed value stored in the "remember me" cookie.
     *
     * @param user the user who ticked remember me
     * @return the cookie value
     */
    public String createRememberToken(User user) {
        throw new UnsupportedOperationException("Not implemented yet - TDD red stage");
    }

    /**
     * Checks a "remember me" cookie value and logs the user back in.
     *
     * @param token the cookie value sent by the browser
     * @return the user, or {@link Optional#empty()} if the cookie is invalid
     */
    public Optional<User> validateRememberToken(String token) {
        throw new UnsupportedOperationException("Not implemented yet - TDD red stage");
    }

    /**
     * Hashes a password with SHA-256 using the given salt.
     *
     * @param password the plain text password
     * @param salt     the random salt stored with the user
     * @return the 64 character hexadecimal hash
     */
    public static String hashPassword(String password, String salt) {
        throw new UnsupportedOperationException("Not implemented yet - TDD red stage");
    }

    /**
     * Generates a new random 32 character salt for a new staff account.
     *
     * @return the salt
     */
    public static String generateSalt() {
        throw new UnsupportedOperationException("Not implemented yet - TDD red stage");
    }
}
