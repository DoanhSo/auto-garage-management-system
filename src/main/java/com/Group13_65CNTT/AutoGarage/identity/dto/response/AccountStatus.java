package com.Group13_65CNTT.AutoGarage.identity.dto.response;

/** Whether a person can sign in: no account yet, created but never used, in use, or blocked. */
public enum AccountStatus {
    NONE,
    PENDING,
    ACTIVE,
    LOCKED
}
