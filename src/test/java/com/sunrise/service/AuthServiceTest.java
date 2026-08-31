package com.sunrise.service;

import com.sunrise.dao.UserDao;
import com.sunrise.model.Role;
import com.sunrise.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService} - Requirement 1, User Authentication.
 *
 * <p><b>Test driven development:</b> this class was written and committed
 * <i>before</i> {@code AuthService} was implemented. The first run is
 * expected to fail (RED). The implementation is then written until every
 * test passes (GREEN).</p>
 *
 * <p>The {@link UserDao} is replaced by a <b>Mockito mock</b>, so these
 * tests never touch MySQL. That keeps them fast, repeatable, and able to
 * run on the GitHub Actions build server where no database exists.</p>
 *
 * <p><b>Test data.</b> The salt and hash below are the exact values seeded
 * into the {@code users} table by {@code database/schema.sql} for the
 * {@code admin} account, so these tests also prove that the Java hashing
 * code agrees with the data in the database.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - staff login rules")
class AuthServiceTest {

    /** Matches the seeded admin account in database/schema.sql. */
    private static final String VALID_USERNAME = "admin";
    private static final String VALID_PASSWORD = "admin123";
    private static final String SALT = "9f2c1a7b4e8d0356af61bc94d27e3081";
    private static final String PASSWORD_HASH =
            "1fc37713e585017314deed4f1a503ff7e5a59e61c17a6132963b5debd23499f4";

    @Mock
    private UserDao userDao;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userDao);
    }

    /** Builds the staff account the mock DAO will return. */
    private User activeAdmin() {
        return new User(1, VALID_USERNAME, PASSWORD_HASH, SALT,
                "System Administrator", Role.ADMIN, true);
    }

    // -----------------------------------------------------------------
    //  TC-01  happy path
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-01 correct username and password returns the user")
    void authenticateWithValidCredentialsReturnsUser() {
        when(userDao.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(activeAdmin()));

        Optional<User> result = authService.authenticate(VALID_USERNAME, VALID_PASSWORD);

        assertTrue(result.isPresent(), "A valid login must return the user");
        assertAll("logged in user",
                () -> assertEquals(VALID_USERNAME, result.get().getUsername()),
                () -> assertEquals(Role.ADMIN, result.get().getRole()),
                () -> assertEquals("System Administrator", result.get().getFullName()));
    }

    // -----------------------------------------------------------------
    //  TC-02  wrong password
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-02 wrong password is rejected")
    void authenticateWithWrongPasswordReturnsEmpty() {
        when(userDao.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(activeAdmin()));

        Optional<User> result = authService.authenticate(VALID_USERNAME, "wrongPassword");

        assertFalse(result.isPresent(), "A wrong password must not log the user in");
        verify(userDao, never()).updateLastLogin(anyInt());
    }

    // -----------------------------------------------------------------
    //  TC-03  unknown account
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-03 unknown username is rejected")
    void authenticateWithUnknownUsernameReturnsEmpty() {
        when(userDao.findByUsername("hacker")).thenReturn(Optional.empty());

        Optional<User> result = authService.authenticate("hacker", "anything");

        assertFalse(result.isPresent());
    }

    // -----------------------------------------------------------------
    //  TC-04  empty input must not even reach the database
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-04 blank username never queries the database")
    void authenticateWithBlankUsernameDoesNotQueryDatabase() {
        Optional<User> result = authService.authenticate("   ", VALID_PASSWORD);

        assertFalse(result.isPresent());
        verify(userDao, never()).findByUsername(anyString());
    }

    // -----------------------------------------------------------------
    //  TC-05  null password must not throw
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-05 null password is rejected without an exception")
    void authenticateWithNullPasswordReturnsEmpty() {
        Optional<User> result = authService.authenticate(VALID_USERNAME, null);

        assertFalse(result.isPresent());
        verify(userDao, never()).findByUsername(anyString());
    }

    // -----------------------------------------------------------------
    //  TC-06  disabled account
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-06 deactivated staff account cannot log in")
    void authenticateWithInactiveAccountReturnsEmpty() {
        User disabled = activeAdmin();
        disabled.setActive(false);
        when(userDao.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(disabled));

        Optional<User> result = authService.authenticate(VALID_USERNAME, VALID_PASSWORD);

        assertFalse(result.isPresent(), "A deactivated account must be refused");
    }

    // -----------------------------------------------------------------
    //  TC-07  audit trail
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-07 successful login records the last login time")
    void authenticateUpdatesLastLoginOnSuccess() {
        when(userDao.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(activeAdmin()));

        authService.authenticate(VALID_USERNAME, VALID_PASSWORD);

        verify(userDao, times(1)).updateLastLogin(1);
    }

    // -----------------------------------------------------------------
    //  TC-08  hashing rules
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-08 hashing is repeatable and depends on the salt")
    void hashPasswordIsRepeatableAndSaltDependent() {
        String first = AuthService.hashPassword(VALID_PASSWORD, SALT);
        String second = AuthService.hashPassword(VALID_PASSWORD, SALT);
        String otherSalt = AuthService.hashPassword(VALID_PASSWORD, "0000000000000000000000000000ffff");

        assertAll("SHA-256 hashing",
                () -> assertEquals(PASSWORD_HASH, first,
                        "Hash must match the value seeded in schema.sql"),
                () -> assertEquals(first, second,
                        "The same password and salt must always give the same hash"),
                () -> assertNotEquals(first, otherSalt,
                        "A different salt must produce a different hash"),
                () -> assertEquals(64, first.length(),
                        "SHA-256 in hex is always 64 characters"));
    }

    // -----------------------------------------------------------------
    //  TC-09  remember me cookie round trip
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-09 remember me token can be created and validated")
    void rememberTokenRoundTripReturnsSameUser() {
        User admin = activeAdmin();
        when(userDao.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(admin));

        String token = authService.createRememberToken(admin);
        Optional<User> result = authService.validateRememberToken(token);

        assertTrue(result.isPresent(), "A genuine token must be accepted");
        assertEquals(VALID_USERNAME, result.get().getUsername());
    }

    // -----------------------------------------------------------------
    //  TC-10  tampered cookie
    // -----------------------------------------------------------------
    @Test
    @DisplayName("TC-10 tampered remember me cookie is rejected")
    void validateRememberTokenWithTamperedValueReturnsEmpty() {
        Optional<User> result = authService.validateRememberToken("admin:notTheRealSignature");

        assertFalse(result.isPresent(), "A forged cookie must never log anyone in");
    }
}
