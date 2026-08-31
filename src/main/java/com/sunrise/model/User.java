package com.sunrise.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A staff member who can log into the system (Requirement 1).
 *
 * <p>This is a plain Java object in the MODEL layer. All attributes are
 * {@code private} and are reached only through {@code public} getters and
 * setters, which is the encapsulation shown in the class diagram.</p>
 *
 * <p>The plain text password is never stored anywhere in this class. Only
 * the SHA-256 {@code passwordHash} and the random {@code salt} used to
 * produce it are kept, and {@link #toString()} deliberately leaves both
 * out so they can never appear in a log file.</p>
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private int userId;
    private String username;
    private String passwordHash;
    private String salt;
    private String fullName;
    private Role role;
    private boolean active;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;

    /** Empty constructor required when building the object from a ResultSet. */
    public User() {
        // no-arg constructor
    }

    /**
     * Convenience constructor used by the DAO and by the unit tests.
     *
     * @param userId       primary key
     * @param username     login name, unique
     * @param passwordHash SHA-256 hash of (salt + password)
     * @param salt         random value used when hashing
     * @param fullName     name shown in the admin panel
     * @param role         access level
     * @param active       {@code false} disables the account
     */
    public User(int userId, String username, String passwordHash, String salt,
                String fullName, Role role, boolean active) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Convenience method used by the JSP views to show or hide
     * administrator-only menu items.
     *
     * @return {@code true} if this user is an administrator
     */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /**
     * @return the first name only, used for the "Hello, Nimali" greeting
     */
    public String getFirstName() {
        if (fullName == null || fullName.isBlank()) {
            return username;
        }
        return fullName.trim().split("\\s+")[0];
    }

    /**
     * @return up to two capital letters used for the round avatar in the
     *         top bar, for example "SA" for "System Administrator"
     */
    public String getInitials() {
        if (fullName == null || fullName.isBlank()) {
            return username == null || username.isEmpty()
                    ? "?"
                    : username.substring(0, 1).toUpperCase();
        }
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < parts.length && initials.length() < 2; i++) {
            if (!parts[i].isEmpty()) {
                initials.append(Character.toUpperCase(parts[i].charAt(0)));
            }
        }
        return initials.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User)) {
            return false;
        }
        User user = (User) other;
        return userId == user.userId && Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, username);
    }

    /** Password hash and salt are intentionally excluded. */
    @Override
    public String toString() {
        return "User{userId=" + userId
                + ", username='" + username + '\''
                + ", fullName='" + fullName + '\''
                + ", role=" + role
                + ", active=" + active + '}';
    }
}
