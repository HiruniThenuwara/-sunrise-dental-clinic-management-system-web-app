package com.sunrise.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One recorded action: who did what, to which record, and when.
 *
 * <p>The username is stored on the entry itself rather than being read from
 * the {@code users} table each time. An audit trail has to stay readable
 * even after an account is renamed or withdrawn, and it has to say what was
 * true at the moment the action happened.</p>
 */
public class ActivityLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter FULL =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final DateTimeFormatter TIME_ONLY =
            DateTimeFormatter.ofPattern("hh:mm a");

    private int logId;
    private Integer userId;
    private String username;
    private ActivityAction action;
    private String entity;
    private String entityRef;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;

    public ActivityLog() {
        // used when building the object from a ResultSet
    }

    public ActivityLog(Integer userId, String username, ActivityAction action) {
        this.userId = userId;
        this.username = username;
        this.action = action;
    }

    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ActivityAction getAction() {
        return action;
    }

    public void setAction(ActivityAction action) {
        this.action = action;
    }

    /** @return what kind of record was touched, for example "Appointment" */
    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    /** @return which record, for example an appointment number */
    public String getEntityRef() {
        return entityRef;
    }

    public void setEntityRef(String entityRef) {
        this.entityRef = entityRef;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** @return the name to show, falling back for entries with no account */
    public String getWho() {
        if (username != null && !username.isBlank()) {
            return username;
        }
        return "unknown";
    }

    /** @return initials for the avatar in the log table */
    public String getInitials() {
        String who = getWho();
        return who.substring(0, 1).toUpperCase();
    }

    /** @return the moment written in full, for example 02 Sep 2026, 03:45 PM */
    public String getFormattedTime() {
        return createdAt == null ? "" : createdAt.format(FULL);
    }

    /**
     * A short, human way of saying when it happened, which is what an
     * administrator scanning the page actually wants to read.
     *
     * @return for example "just now", "12 minutes ago", "today at 09:15 AM"
     */
    public String getRelativeTime() {
        if (createdAt == null) {
            return "";
        }

        Duration since = Duration.between(createdAt, LocalDateTime.now());
        long minutes = since.toMinutes();

        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        }
        if (createdAt.toLocalDate().isEqual(java.time.LocalDate.now())) {
            return "today at " + createdAt.format(TIME_ONLY);
        }
        if (createdAt.toLocalDate().isEqual(java.time.LocalDate.now().minusDays(1))) {
            return "yesterday at " + createdAt.format(TIME_ONLY);
        }
        return getFormattedTime();
    }

    @Override
    public String toString() {
        return "ActivityLog{" + getWho() + " " + action + " " + entityRef + " at " + createdAt + '}';
    }
}
