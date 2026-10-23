package com.Group13_65CNTT.AutoGarage.identity.service.impl;

import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.identity.service.KeycloakIdentityService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
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

    private final boolean enabled;
    private final String serverUrl;
    private final String realm;
    private final String adminUsername;
    private final String adminPassword;
    private final RestClient restClient;

    public KeycloakIdentityServiceImpl(
            @Value("${security.keycloak-admin.enabled:true}") boolean enabled,
            @Value("${security.keycloak-admin.server-url:http://localhost:8081}") String serverUrl,
            @Value("${security.keycloak-admin.realm:autogarage-realm}") String realm,
            @Value("${KEYCLOAK_ADMIN_USERNAME:admin}") String adminUsername,
            @Value("${KEYCLOAK_ADMIN_PASSWORD:}") String adminPassword) {
        this.enabled = enabled;
        this.serverUrl = serverUrl.replaceAll("/+$", "");
        this.realm = realm;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public Optional<String> createOrLinkUser(
            String username, String email, String fullName, Role role, String initialPassword) {
        if (!isConfigured()) return Optional.empty();
        try {
            String token = acquireAdminToken();
            if (token == null) return Optional.empty();
            String userId = findKeycloakUserIdByUsername(token, username);
            String[] names = splitFullName(fullName);
            if (userId == null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("username", username);
                payload.put("enabled", true);
                payload.put("emailVerified", true);
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
                assignSingleGarageRole(token, userId, role);
            }
            return Optional.ofNullable(userId);
        } catch (Exception ex) {
            log.warn("Keycloak Admin sync skipped during user creation for {}: {}", username, ex.getMessage());
            return Optional.empty();
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
        if (!isConfigured()) return;
        try {
            String token = acquireAdminToken();
            if (token == null) return;
            String userId =
                    identityId != null && !identityId.isBlank()
                            ? identityId
                            : findKeycloakUserIdByUsername(token, username);
            if (userId == null) return;
            String[] names = splitFullName(fullName);
            Map<String, Object> payload = new HashMap<>();
            payload.put("enabled", active);
            payload.put("firstName", names[0]);
            payload.put("lastName", names[1]);
            payload.put("email", email != null ? email : "");
            restClient
                    .put()
                    .uri(serverUrl + "/admin/realms/" + realm + "/users/" + userId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            assignSingleGarageRole(token, userId, role);
            if (!active) {
                logoutKeycloakUser(token, userId);
            }
        } catch (Exception ex) {
            log.warn("Keycloak Admin sync skipped during user update for {}: {}", username, ex.getMessage());
        }
    }

    @Override
    public void disableUser(String identityId, String username) {
        if (!isConfigured()) return;
        try {
            String token = acquireAdminToken();
            if (token == null) return;
            String userId =
                    identityId != null && !identityId.isBlank()
                            ? identityId
                            : findKeycloakUserIdByUsername(token, username);
            if (userId == null) return;
            restClient
                    .put()
                    .uri(serverUrl + "/admin/realms/" + realm + "/users/" + userId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("enabled", false))
                    .retrieve()
                    .toBodilessEntity();
            logoutKeycloakUser(token, userId);
        } catch (Exception ex) {
            log.warn("Keycloak Admin sync skipped during user disable for {}: {}", username, ex.getMessage());
        }
    }

    private boolean isConfigured() {
        return enabled
                && adminUsername != null
                && !adminUsername.isBlank()
                && adminPassword != null
                && !adminPassword.isBlank();
    }

    private String acquireAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUsername);
        form.add("password", adminPassword);
        Map<String, Object> res =
                restClient
                        .post()
                        .uri(serverUrl + "/realms/master/protocol/openid-connect/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form)
                        .retrieve()
                        .body(MAP_TYPE);
        return res != null && res.get("access_token") instanceof String token ? token : null;
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
                if (name instanceof String roleName && isGarageRole(roleName) && !roleName.equals(targetRole.name())) {
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
        if (roleRep != null) {
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
