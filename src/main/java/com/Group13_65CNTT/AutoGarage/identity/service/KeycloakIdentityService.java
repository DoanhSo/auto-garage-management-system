package com.Group13_65CNTT.AutoGarage.identity.service;

import com.Group13_65CNTT.AutoGarage.common.enums.Role;

import java.util.Optional;

public interface KeycloakIdentityService {
    Optional<String> createOrLinkUser(
            String username, String email, String fullName, Role role, String initialPassword);

    void syncUserUpdate(
            String identityId,
            String username,
            String email,
            String fullName,
            Role role,
            boolean active);

    void disableUser(String identityId, String username);

    void syncAuthVersion(String identityId, String username, long authVersion);
}
