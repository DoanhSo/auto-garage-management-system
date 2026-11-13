package com.Group13_65CNTT.AutoGarage.identity.dto.response;

import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import java.time.Instant;

/** Sign-in state of the account behind a customer or an employee, shown next to the person. */
public record AccountSummary(AccountStatus status, String username, Instant lastLoginAt) {

    public static AccountSummary none() {
        return new AccountSummary(AccountStatus.NONE, null, null);
    }

    /** Summary of {@code user}; {@link #none()} when there is no account. */
    public static AccountSummary of(User user) {
        if (user == null) {
            return none();
        }
        return new AccountSummary(statusOf(user), user.getUsername(), user.getLastLoginAt());
    }

    /**
     * Blocked first, then signed in at least once, otherwise waiting. An identity-provider link alone does not
     * count: accounts created by staff are linked before their owner ever signs in.
     */
    public static AccountStatus statusOf(User user) {
        if (!user.isActive() || user.getDeletedAt() != null) {
            return AccountStatus.LOCKED;
        }
        if (user.getLastLoginAt() != null) {
            return AccountStatus.ACTIVE;
        }
        return AccountStatus.PENDING;
    }
}
