package com.Group13_65CNTT.AutoGarage.identity.service;

import static org.assertj.core.api.Assertions.*;

import com.Group13_65CNTT.AutoGarage.common.config.KeycloakAdminProperties;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.identity.service.impl.KeycloakIdentityServiceImpl;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

class KeycloakIdentityServiceTest {
    private final List<Request> requests = new ArrayList<>();
    private HttpServer server;
    private KeycloakAdminProperties properties;
    private KeycloakIdentityService service;
    private boolean unavailable;
    private boolean existingUsername;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void startFakeIdentityProvider() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::respond);
        server.start();
        properties = new KeycloakAdminProperties();
        properties.setServerUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setRealm("garage");
        properties.setUsername("test-admin");
        properties.setPassword("test-only-password");
        service = new KeycloakIdentityServiceImpl(properties);
    }

    @AfterEach
    void stop() { server.stop(0); }

    @Test
    void revocationPreservesExistingAttributesAndEndsOldRefreshSessions() throws Exception {
        service.syncAuthVersion("identity-1", "customer.one", 3);
        Request update = requests.stream().filter(r -> r.method().equals("PUT")).findFirst().orElseThrow();
        var body = json.readTree(update.body());
        assertThat(body.path("attributes").path("auth_version").get(0).asString()).isEqualTo("3");
        assertThat(body.path("attributes").path("other").get(0).asString()).isEqualTo("keep");
        assertThat(body.path("email").asString()).isEqualTo("customer@example.test");
        assertThat(body.path("firstName").asString()).isEqualTo("Customer");
        assertThat(body.path("lastName").asString()).isEqualTo("One");
        assertThat(body.path("emailVerified").asBoolean()).isTrue();
        assertThat(body.path("enabled").asBoolean()).isTrue();
        assertThat(requests).anyMatch(r -> r.method().equals("POST") && r.path().endsWith("/identity-1/logout"));
    }

    @Test
    void disablingAccountPreservesProfileAndRevocationAttributes() throws Exception {
        service.disableUser("identity-1", "customer.one");
        Request update = requests.stream().filter(r -> r.method().equals("PUT")).findFirst().orElseThrow();
        var body = json.readTree(update.body());
        assertThat(body.path("enabled").asBoolean()).isFalse();
        assertThat(body.path("email").asString()).isEqualTo("customer@example.test");
        assertThat(body.path("firstName").asString()).isEqualTo("Customer");
        assertThat(body.path("attributes").path("auth_version").get(0).asString()).isEqualTo("1");
    }

    @Test
    void profileUpdatePreservesAttributesAndDoesNotVerifyAChangedEmail() throws Exception {
        service.syncUserUpdate("identity-1", "customer.one", "updated@example.test", "Updated Person", Role.TECHNICIAN, true);
        Request update = requests.stream().filter(r -> r.method().equals("PUT")).findFirst().orElseThrow();
        var body = json.readTree(update.body());
        assertThat(body.path("email").asString()).isEqualTo("updated@example.test");
        assertThat(body.path("emailVerified").asBoolean()).isFalse();
        assertThat(body.path("firstName").asString()).isEqualTo("Updated");
        assertThat(body.path("lastName").asString()).isEqualTo("Person");
        assertThat(body.path("attributes").path("other").get(0).asString()).isEqualTo("keep");
        assertThat(body.path("attributes").path("auth_version").get(0).asString()).isEqualTo("1");
    }

    @Test
    void unavailableProviderDoesNotPretendAccountWasCreatedUpdatedOrDisabled() {
        unavailable = true;
        assertUnavailable(() -> service.createOrLinkUser("staff.one", "staff@example.test", "Staff One", Role.TECHNICIAN, "test-password"));
        assertUnavailable(() -> service.syncUserUpdate("identity-1", "staff.one", null, "Staff One", Role.TECHNICIAN, true));
        assertUnavailable(() -> service.disableUser("identity-1", "staff.one"));
        assertUnavailable(() -> service.syncAuthVersion("identity-1", "staff.one", 1));
        assertThat(requests).allMatch(r -> r.path().endsWith("/token"));
    }

    @Test
    void publicRegistrationCannotBePromotedToStaffByUsernameCollision() {
        existingUsername = true;
        assertThatThrownBy(() -> service.createOrLinkUser("staff.one", null, "Staff", Role.ADMIN, "test-password"))
                .isInstanceOfSatisfying(BusinessException.class,
                        failure -> assertThat(failure.getResponseCode()).isEqualTo(ResponseCode.DUPLICATE_RESOURCE));
        assertThat(requests).noneMatch(r -> r.path().contains("role-mappings"));
        assertThat(requests).noneMatch(r -> r.method().equals("PUT"));
    }

    @Test
    void createdStaffHasOneGarageRoleAndDoesNotTrustAnUnverifiedEmail() throws Exception {
        assertThat(service.createOrLinkUser("staff.one", "staff@example.test", "Staff One", Role.TECHNICIAN, "test-password"))
                .contains("identity-1");
        Request create = requests.stream().filter(r -> r.method().equals("POST") && r.path().endsWith("/users")).findFirst().orElseThrow();
        assertThat(json.readTree(create.body()).path("emailVerified").asBoolean()).isFalse();
        Request removal = requests.stream().filter(r -> r.method().equals("DELETE")).findFirst().orElseThrow();
        assertThat(removal.body()).contains("default-roles-garage");
        assertThat(requests).anyMatch(r -> r.method().equals("POST") && r.path().endsWith("/role-mappings/realm") && r.body().contains("TECHNICIAN"));
    }

    private void assertUnavailable(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation) {
        assertThatThrownBy(operation).isInstanceOfSatisfying(BusinessException.class, failure -> {
            assertThat(failure.getResponseCode()).isEqualTo(ResponseCode.AUTHENTICATION_UNAVAILABLE);
            assertThat(failure.getCause()).isNotNull();
            assertThat(failure.getMessage()).doesNotContain("private-provider-detail");
        });
    }

    private void respond(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(new Request(method, path, requestBody));
        int status = 200;
        String response;
        if (unavailable) { status = 503; response = "private-provider-detail"; }
        else if (path.endsWith("/token")) response = "{\"access_token\":\"test-admin-token\"}";
        else if (path.endsWith("/users") && method.equals("GET")) response = existingUsername ? "[{\"id\":\"identity-1\"}]" : "[]";
        else if (path.endsWith("/users") && method.equals("POST")) {
            status = 201; response = "";
            exchange.getResponseHeaders().set("Location", properties.getServerUrl() + "/admin/realms/garage/users/identity-1");
        }
        else if (path.endsWith("/role-mappings/realm") && method.equals("GET")) response = "[{\"id\":\"default-role\",\"name\":\"default-roles-garage\"}]";
        else if (path.endsWith("/roles/TECHNICIAN")) response = "{\"id\":\"technician-role\",\"name\":\"TECHNICIAN\"}";
        else if (path.endsWith("/users/identity-1") && method.equals("GET")) response = "{\"username\":\"customer.one\",\"email\":\"customer@example.test\",\"emailVerified\":true,\"firstName\":\"Customer\",\"lastName\":\"One\",\"enabled\":true,\"attributes\":{\"other\":[\"keep\"],\"auth_version\":[\"1\"]}}";
        else { status = 204; response = ""; }
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private record Request(String method, String path, String body) {}
}
