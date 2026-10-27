package com.Group13_65CNTT.AutoGarage.identity.service.impl;

import com.Group13_65CNTT.AutoGarage.common.config.KeycloakAdminProperties;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.util.SafeFailureLog;
import com.Group13_65CNTT.AutoGarage.identity.service.KeycloakIdentityService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class KeycloakIdentityServiceImpl implements KeycloakIdentityService {
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final KeycloakAdminProperties properties;
    private final String serverUrl;
    private final String realm;
    private final RestClient restClient;

    public KeycloakIdentityServiceImpl(
            KeycloakAdminProperties properties) {
        this.properties = properties;
        this.serverUrl = properties.getServerUrl() == null ? "" : properties.getServerUrl().replaceAll("/+$", "");
        this.realm = properties.getRealm();
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build());
        factory.setReadTimeout(properties.getRequestTimeout());
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public Optional<String> createOrLinkUser(
            String username, String email, String fullName, Role role, String initialPassword) {
        if (!properties.isEnabled()) return Optional.empty();
        try {
            String token = acquireAdminToken();
            String userId = findKeycloakUserIdByUsername(token, username);
            // A public registration may already own this name. Never elevate it by name alone.
            if (userId != null) throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
            String[] names = splitFullName(fullName);
            if (userId == null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("username", username);
                payload.put("enabled", true);
                payload.put("emailVerified", false);
                payload.put("attributes", Map.of(SecurityConstants.AUTH_VERSION_CLAIM, List.of("0")));
                payload.put("firstName", names[0]);
                payload.put("lastName", names[1]);
                if (email != null && !email.isBlank()) {
                    payload.put("email", email);
                }
                if (initialPassword != null && !initialPassword.isBlank()) {
                    payload.put(
                            "credentials",
                            List.of(
                                    Map.of(
                                            "type",
                                            "password",
                                            "value",
                                            initialPassword,
                                            "temporary",
                                            false)));
                }
                var response =
                        restClient
                                .post()
                                .uri(serverUrl + "/admin/realms/" + realm + "/users")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(payload)
                                .retrieve()
                                .toBodilessEntity();
                URI location = response.getHeaders().getLocation();
                if (location != null) {
                    String path = location.getPath();
                    userId = path.substring(path.lastIndexOf('/') + 1);
                } else {
                    userId = findKeycloakUserIdByUsername(token, username);
                }
            }
            if (userId != null) {
                try {
                    assignSingleGarageRole(token, userId, role);
                } catch (RestClientException | BusinessException failure) {
                    // This invocation created the identity; never remove an existing account.
                    try {
                        restClient.delete()
                                .uri(serverUrl + "/admin/realms/" + realm + "/users/" + userId)
                                .header("Authorization", SecurityConstants.BEARER_PREFIX + token)
                                .retrieve().toBodilessEntity();
                    } catch (RestClientException cleanupFailure) {
                        failure.addSuppressed(cleanupFailure);
                        SafeFailureLog.error(log, "Unable to remove incomplete Keycloak registration", cleanupFailure);
                    }
                    throw failure;
                }
            }
            if (userId == null || userId.isBlank()) throw unavailable();
            return Optional.of(userId);
        } catch (RestClientResponseException failure) {
            if (failure.getStatusCode().value() == 409) {
                throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE, failure);
            }
            throw unavailable(failure);
        } catch (RestClientException failure) {
            throw unavailable(failure);
        }
    }

    @Override
    public void syncUserUpdate(
            String identityId,
            String username,
            String email,
            String fullName,
            Role role,
            boolean active) {
        if (!properties.isEnabled()) return;
        try {
            String token = acquireAdminToken();
            String userId =
                    identityId != null && !identityId.isBlank()
                            ? identityId
                            : findKeycloakUserIdByUsername(token, username);
            if (userId == null) throw unavailable();
            String[] names = splitFullName(fullName);
            String userUrl = serverUrl + "/admin/realms/" + realm + "/users/" + userId;
            Map<String, Object> existing = readUser(token, userUrl);
            Map<String, Object> payload = profileUpdate(existing);
            payload.put("enabled", active);
            payload.put("firstName", names[0]);
            payload.put("lastName", names[1]);
            payload.put("email", email != null ? email : "");
            if (!java.util.Objects.equals(existing.get("email"), email)) {
                payload.put("emailVerified", false);
            }
            restClient
                    .put()
                    .uri(userUrl)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            assignSingleGarageRole(token, userId, role);
            if (!active) {
                logoutKeycloakUser(token, userId);
            }
        } catch (RestClientException failure) {
            throw unavailable(failure);
        }
    }

    @Override
    public void disableUser(String identityId, String username) {
        if (!properties.isEnabled()) return;
        try {
            String token = acquireAdminToken();
            String userId =
                    identityId != null && !identityId.isBlank()
                            ? identityId
                            : findKeycloakUserIdByUsername(token, username);
            if (userId == null) throw unavailable();
            String userUrl = serverUrl + "/admin/realms/" + realm + "/users/" + userId;
            Map<String, Object> payload = profileUpdate(readUser(token, userUrl));
            payload.put("enabled", false);
            restClient
                    .put()
                    .uri(userUrl)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            logoutKeycloakUser(token, userId);
        } catch (RestClientException failure) {
            throw unavailable(failure);
        }
    }

    @Override
    public void syncAuthVersion(String identityId, String username, long authVersion) {
        if (!properties.isEnabled()) return;
        try {
            String token = acquireAdminToken();
            String userId = identityId != null && !identityId.isBlank()
                    ? identityId : findKeycloakUserIdByUsername(token, username);
            if (userId == null) throw unavailable();
            String userUrl = serverUrl + "/admin/realms/" + realm + "/users/" + userId;
            Map<String, Object> existing = readUser(token, userUrl);
            Map<String, Object> attributes = new HashMap<>();
            if (existing.get("attributes") instanceof Map<?, ?> previous) {
                previous.forEach((key, value) -> {
                    if (key instanceof String name) attributes.put(name, value);
                });
            }
            attributes.put(SecurityConstants.AUTH_VERSION_CLAIM, List.of(Long.toString(authVersion)));
            Map<String, Object> payload = profileUpdate(existing);
            payload.put("attributes", attributes);
            restClient.put().uri(userUrl)
                    .header("Authorization", SecurityConstants.BEARER_PREFIX + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload).retrieve().toBodilessEntity();
            logoutKeycloakUser(token, userId);
        } catch (RestClientException failure) {
            throw unavailable(failure);
        }
    }

    private Map<String, Object> readUser(String token, String userUrl) {
        Map<String, Object> user = restClient.get().uri(userUrl)
                .header("Authorization", SecurityConstants.BEARER_PREFIX + token)
                .retrieve().body(MAP_TYPE);
        if (user == null) throw unavailable();
        return user;
    }

    private Map<String, Object> profileUpdate(Map<String, Object> existing) {
        // Keycloak's PUT replaces profile fields. Omitted names/email would be cleared even
        // when only attributes or enabled are being changed; preserve the editable profile.
        Map<String, Object> payload = new HashMap<>();
        for (String field : List.of("username", "email", "emailVerified", "firstName", "lastName",
                "enabled", "attributes", "requiredActions")) {
            if (existing.containsKey(field)) payload.put(field, existing.get(field));
        }
        return payload;
    }

    private BusinessException unavailable(RestClientException failure) {
        SafeFailureLog.error(log, "Keycloak admin operation failed", failure);
        return new BusinessException(ResponseCode.AUTHENTICATION_UNAVAILABLE, failure);
    }

    private BusinessException unavailable() {
        return new BusinessException(ResponseCode.AUTHENTICATION_UNAVAILABLE);
    }

    private String acquireAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", properties.getUsername());
        form.add("password", properties.getPassword());
        Map<String, Object> res =
                restClient
                        .post()
                        .uri(serverUrl + "/realms/master/protocol/openid-connect/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form)
                        .retrieve()
                        .body(MAP_TYPE);
        if (res == null || !(res.get("access_token") instanceof String token) || token.isBlank()) {
            throw unavailable();
        }
        return token;
    }

    private String findKeycloakUserIdByUsername(String token, String username) {
        List<Map<String, Object>> users =
                restClient
                        .get()
                        .uri(
                                serverUrl
                                        + "/admin/realms/"
                                        + realm
                                        + "/users?exact=true&username={username}",
                                username)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .body(LIST_MAP_TYPE);
        if (users == null || users.isEmpty()) return null;
        Object id = users.getFirst().get("id");
        return id instanceof String str ? str : null;
    }

    private void assignSingleGarageRole(String token, String userId, Role targetRole) {
        List<Map<String, Object>> currentRoles =
                restClient
                        .get()
                        .uri(
                                serverUrl
                                        + "/admin/realms/"
                                        + realm
                                        + "/users/"
                                        + userId
                                        + "/role-mappings/realm")
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .body(LIST_MAP_TYPE);
        List<Map<String, Object>> toRemove = new ArrayList<>();
        if (currentRoles != null) {
            for (Map<String, Object> r : currentRoles) {
                Object name = r.get("name");
                if (name instanceof String roleName
                        && ((isGarageRole(roleName) && !roleName.equals(targetRole.name()))
                        || (targetRole != Role.CUSTOMER && roleName.equals("default-roles-" + realm)))) {
                    toRemove.add(r);
                }
            }
        }
        if (!toRemove.isEmpty()) {
            restClient
                    .method(org.springframework.http.HttpMethod.DELETE)
                    .uri(
                            serverUrl
                                    + "/admin/realms/"
                                    + realm
                                    + "/users/"
                                    + userId
                                    + "/role-mappings/realm")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(toRemove)
                    .retrieve()
                    .toBodilessEntity();
        }
        Map<String, Object> roleRep =
                restClient
                        .get()
                        .uri(
                                serverUrl
                                        + "/admin/realms/"
                                        + realm
                                        + "/roles/"
                                        + targetRole.name())
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .body(MAP_TYPE);
        if (roleRep == null) throw unavailable();
        restClient
                .post()
                .uri(
                        serverUrl
                                + "/admin/realms/"
                                + realm
                                + "/users/"
                                + userId
                                + "/role-mappings/realm")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(roleRep))
                .retrieve()
                .toBodilessEntity();
    }

    private void logoutKeycloakUser(String token, String userId) {
        restClient
                .post()
                .uri(serverUrl + "/admin/realms/" + realm + "/users/" + userId + "/logout")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .toBodilessEntity();
    }

    private boolean isGarageRole(String roleName) {
        try {
            Role.valueOf(roleName);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String[] splitFullName(String fullName) {
        String trimmed = fullName == null ? "" : fullName.strip();
        int idx = trimmed.lastIndexOf(' ');
        if (idx <= 0) return new String[] {trimmed, ""};
        return new String[] {trimmed.substring(0, idx).strip(), trimmed.substring(idx + 1).strip()};
    }
}
