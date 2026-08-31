package com.sunrise.service;

import com.sunrise.dao.UserDao;
import com.sunrise.dao.impl.UserDaoImpl;
import com.sunrise.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * All staff login rules for the system (Requirement 1).
 *
 * <p>This class sits in the MODEL layer of MVC. It holds the decisions -
 * who may log in, how a password is hashed, whether a "remember me" cookie
 * is genuine - while the servlets only pass data in and choose a view. That
 * separation is what allows {@code AuthServiceTest} to test every rule with
 * a Mockito mock and no database, no Tomcat and no browser.</p>
 *
 * <p><b>Password storage.</b> The plain password is never stored anywhere.
 * The database keeps a SHA-256 hash of {@code salt + password} together with
 * a random salt for each user. Because the salt differs per account, two
 * staff members who happen to choose the same password still have different
 * hashes, so an attacker cannot recognise repeated passwords or use a
 * precomputed rainbow table.</p>
 */
public class AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());

    /** Session attribute that holds the logged in {@link User}. */
    public static final String SESSION_USER_KEY = "user";

    /** Name of the "remember me" cookie stored in the browser. */
    public static final String REMEMBER_COOKIE_NAME = "sdc_remember";

    /** The "remember me" cookie lives for 30 days. */
    public static final int REMEMBER_COOKIE_MAX_AGE = 30 * 24 * 60 * 60;

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_BYTES = 16;
    private static final char TOKEN_SEPARATOR = ':';

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
     * <p>The account is refused if the input is empty, the username is
     * unknown, the account has been deactivated, or the password does not
     * match. The caller is given no clue which of those it was - the login
     * screen shows one generic message either way, so an attacker cannot
     * use the error text to discover which usernames exist.</p>
     *
     * @param username the login name
     * @param password the plain text password
     * @return the logged in user, or {@link Optional#empty()} if refused
     */
    public Optional<User> authenticate(String username, String password) {

        // Empty input is rejected before the database is touched at all.
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return Optional.empty();
        }

        Optional<User> found = userDao.findByUsername(username.trim());
        if (found.isEmpty()) {
            return Optional.empty();
        }

        User user = found.get();

        if (!user.isActive()) {
            LOGGER.warning("Login refused - account is deactivated: " + user.getUsername());
            return Optional.empty();
        }

        String candidateHash = hashPassword(password, user.getSalt());
        if (!matches(candidateHash, user.getPasswordHash())) {
            return Optional.empty();
        }

        // Audit trail - the clinic can see who used the system and when.
        userDao.updateLastLogin(user.getUserId());
        return Optional.of(user);
    }

    /**
     * Builds the signed value stored in the "remember me" cookie.
     *
     * <p>The value is {@code username:signature}, where the signature is a
     * hash of the username, the user's salt and their stored password hash.
     * The server can recompute it, but nobody can forge it without already
     * having read the database. Changing a password changes the stored hash,
     * which silently invalidates every cookie signed with the old one.</p>
     *
     * @param user the user who ticked remember me
     * @return the cookie value
     */
    public String createRememberToken(User user) {
        if (user == null) {
            return "";
        }
        return user.getUsername() + TOKEN_SEPARATOR + signature(user);
    }

    /**
     * Checks a "remember me" cookie value and logs the user back in.
     *
     * @param token the cookie value sent by the browser
     * @return the user, or {@link Optional#empty()} if the cookie is invalid
     */
    public Optional<User> validateRememberToken(String token) {

        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        int separator = token.indexOf(TOKEN_SEPARATOR);
        if (separator <= 0 || separator == token.length() - 1) {
            return Optional.empty();
        }

        String username = token.substring(0, separator);
        String presentedSignature = token.substring(separator + 1);

        Optional<User> found = userDao.findByUsername(username);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        User user = found.get();
        if (!user.isActive()) {
            return Optional.empty();
        }

        if (!matches(presentedSignature, signature(user))) {
            LOGGER.warning("Rejected a tampered remember me cookie for: " + username);
            return Optional.empty();
        }
        return Optional.of(user);
    }

    /**
     * Hashes a password with SHA-256 using the given salt.
     *
     * @param password the plain text password
     * @param salt     the random salt stored with the user
     * @return the 64 character hexadecimal hash
     */
    public static String hashPassword(String password, String salt) {
        return sha256Hex((salt == null ? "" : salt) + (password == null ? "" : password));
    }

    /**
     * Generates a new random 32 character salt for a new staff account.
     *
     * <p>{@link SecureRandom} is used rather than {@code Math.random()},
     * because a predictable salt would defeat its purpose.</p>
     *
     * @return the salt
     */
    public static String generateSalt() {
        byte[] bytes = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(bytes);
        return toHex(bytes);
    }

    /** The signature that proves a remember me cookie came from this server. */
    private String signature(User user) {
        return sha256Hex(user.getUsername() + user.getSalt() + user.getPasswordHash());
    }

    /**
     * Compares two hashes in constant time.
     *
     * <p>A normal {@code equals} stops at the first different character, and
     * the tiny timing difference can leak information about the correct
     * value. {@link MessageDigest#isEqual} always compares every byte.</p>
     */
    private boolean matches(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        return MessageDigest.isEqual(
                first.getBytes(StandardCharsets.UTF_8),
                second.getBytes(StandardCharsets.UTF_8));
    }

    /** Runs SHA-256 over the text and returns the result as lower case hex. */
    private static String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            return toHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is part of every standard Java installation.
            throw new IllegalStateException(HASH_ALGORITHM + " is not available", e);
        }
    }

    /** Converts bytes into a lower case hexadecimal string. */
    private static String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16));
            hex.append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }
}
