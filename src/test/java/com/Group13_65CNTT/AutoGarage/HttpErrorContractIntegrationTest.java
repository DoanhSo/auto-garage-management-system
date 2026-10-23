package com.Group13_65CNTT.AutoGarage;

import static org.assertj.core.api.Assertions.assertThat;

import com.Group13_65CNTT.AutoGarage.common.config.AuthProperties;
import com.Group13_65CNTT.AutoGarage.common.config.NotificationProperties;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "server.address=127.0.0.1",
            "spring.datasource.url=jdbc:h2:mem:autogarage_http_errors;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "security.password.min-length=20",
            "security.auth.allowed-origins=http://localhost:5173",
            "security.auth.cors-max-age=11s",
            "garage.pagination.default-size=10",
            "garage.pagination.max-size=25",
            "garage.report.max-period=30d"
        })
@ActiveProfiles("h2")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@RequiredArgsConstructor
class HttpErrorContractIntegrationTest extends IntegrationTestSupport {
    @LocalServerPort int port;
    private final UserRepository users;
    private final AuthProperties authProperties;
    private final ObjectMapper mapper;
    private final NotificationProperties notificationProperties;

    @Test
    void realHttpServerUsesConfiguredPoliciesAndUnifiedErrors() throws Exception {
        assertThat(notificationProperties.isWorkerEnabled()).isFalse();
        assertThat(notificationProperties.isRemindersEnabled()).isFalse();
        assertThat(notificationProperties.isEmailEnabled()).isFalse();
        assertThat(notificationProperties.isSmsEnabled()).isFalse();
        User admin =
                users.saveAndFlush(
                        User.builder()
                                .identityId("550e8400-e29b-41d4-a716-446655000001")
                                .username("http.errors.admin")
                                .fullName("HTTP Admin")
                                .role(Role.ADMIN)
                                .build());

        try (HttpClient client = HttpClient.newHttpClient()) {
            var policy =
                    envelope(
                                    exchange(
                                            client,
                                            "GET",
                                            "/api/v1/public/ui-policy",
                                            null,
                                            null,
                                            Map.of()),
                                    200,
                                    1000)
                            .path("data");
            assertThat(policy.path("passwordMinLength").asInt()).isEqualTo(20);
            assertThat(policy.path("passwordMaxUtf8Bytes").asInt()).isEqualTo(72);
            assertThat(policy.path("paginationDefaultSize").asInt()).isEqualTo(10);
            assertThat(policy.path("paginationMaxSize").asInt()).isEqualTo(25);
            assertThat(policy.path("reportMaxPeriodSeconds").asLong())
                    .isEqualTo(Duration.ofDays(30).toSeconds());
            assertThat(policy.path("garageTimeZone").asString())
                    .isEqualTo(notificationProperties.getZone());
            assertThat(policy.size()).isEqualTo(7);
            envelope(
                    exchange(client, "POST", "/api/v1/public/ui-policy", null, null, Map.of()),
                    401,
                    3001);
            envelope(exchange(client, "GET", "/api/v1/auth/me", null, null, Map.of()), 401, 3001);
            var accepted =
                    exchange(
                            client,
                            "OPTIONS",
                            "/api/v1/auth/me",
                            null,
                            null,
                            Map.of(
                                    "Origin",
                                    "http://localhost:5173",
                                    "Access-Control-Request-Method",
                                    "GET"));
            assertThat(accepted.statusCode()).isEqualTo(200);
            assertThat(accepted.headers().firstValue("Access-Control-Allow-Origin"))
                    .contains("http://localhost:5173");
            assertThat(accepted.headers().firstValue("Access-Control-Max-Age")).contains("11");

            envelope(
                    exchange(
                            client,
                            "OPTIONS",
                            "/api/v1/auth/me",
                            null,
                            null,
                            Map.of(
                                    "Origin",
                                    "https://untrusted.example",
                                    "Access-Control-Request-Method",
                                    "GET")),
                    403,
                    3002);

            String token = issueKeycloakToken(authProperties, admin);
            assertThat(token).isNotBlank();

            envelope(
                    exchange(
                            client,
                            "POST",
                            "/api/v1/admin/users",
                            "{",
                            token,
                            Map.of("Content-Type", "application/json")),
                    400,
                    2004);
            envelope(
                    exchange(
                            client,
                            "POST",
                            "/api/v1/admin/users",
                            "text",
                            token,
                            Map.of("Content-Type", "text/plain")),
                    415,
                    2009);

            envelope(
                    exchange(client, "GET", "/api/v1/admin/users?size=26", null, token, Map.of()),
                    400,
                    2004);
            var page =
                    envelope(
                            exchange(client, "GET", "/api/v1/admin/users", null, token, Map.of()),
                            200,
                            1000);
            assertThat(page.path("data").path("size").asInt()).isEqualTo(10);

            String invalidUser =
                    mapper.writeValueAsString(
                            Map.of(
                                    "username",
                                    "bad username",
                                    "fullName",
                                    "Rejected",
                                    "role",
                                    "CUSTOMER"));
            envelope(
                    exchange(
                            client,
                            "POST",
                            "/api/v1/admin/users",
                            invalidUser,
                            token,
                            Map.of("Content-Type", "application/json")),
                    400,
                    2001);
            assertThat(users.existsByUsername("bad username")).isFalse();

            envelope(
                    exchange(
                            client,
                            "GET",
                            "/api/v1/reports/dashboard?from=2026-01-01T00:00:00Z&until=2026-02-01T00:00:00Z",
                            null,
                            token,
                            Map.of()),
                    400,
                    2004);
            envelope(
                    exchange(client, "POST", "/api/v1/auth/logout", null, token, Map.of()),
                    200,
                    1000);
            envelope(exchange(client, "GET", "/api/v1/auth/me", null, token, Map.of()), 401, 3003);
        }
    }

    private HttpResponse<String> exchange(
            HttpClient client,
            String method,
            String path,
            String body,
            String token,
            Map<String, String> headers)
            throws Exception {
        var builder =
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .timeout(Duration.ofSeconds(15));
        headers.forEach(builder::header);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        builder.method(
                method,
                body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode envelope(HttpResponse<String> response, int status, int code) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type"))
                .hasValueSatisfying(value -> assertThat(value).contains("application/json"));
        var json = mapper.readTree(response.body());
        assertThat(json.path("code").asInt()).isEqualTo(code);
        assertThat(json.path("success").asBoolean()).isEqualTo(status < 400);
        assertThat(json.path("timestamp").asString()).isNotBlank();
        assertThat(json.path("path").asString()).startsWith("/api/v1/");
        return json;
    }
}
