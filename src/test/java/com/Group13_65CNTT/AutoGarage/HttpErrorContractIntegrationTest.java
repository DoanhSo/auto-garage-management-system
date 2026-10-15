package com.Group13_65CNTT.AutoGarage;

import static org.assertj.core.api.Assertions.assertThat;

import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.config.NotificationProperties;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
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
class HttpErrorContractIntegrationTest extends IntegrationTestSupport {
    @LocalServerPort int port;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired ObjectMapper mapper;
    @Autowired NotificationProperties notificationProperties;

    @Test
    void realHttpServerUsesConfiguredPoliciesAndUnifiedErrors() throws Exception {
        assertThat(notificationProperties.isWorkerEnabled()).isFalse();
        assertThat(notificationProperties.isRemindersEnabled()).isFalse();
        assertThat(notificationProperties.isEmailEnabled()).isFalse();
        assertThat(notificationProperties.isSmsEnabled()).isFalse();
        byte[] random = new byte[24];
        new SecureRandom().nextBytes(random);
        String password = Base64.getEncoder().encodeToString(random);
        users.saveAndFlush(
                User.builder()
                        .username("http.errors.admin")
                        .fullName("HTTP Admin")
                        .passwordHash(passwords.encode(password))
                        .role(Role.ADMIN)
                        .build());

        try (HttpClient client = HttpClient.newHttpClient()) {
            envelope(exchange(client, "GET", "/api/v1/auth/me", null, null, Map.of()), 401, 3001);
            var accepted =
                    exchange(
                            client,
                            "OPTIONS",
                            "/api/v1/auth/login",
                            null,
                            null,
                            Map.of(
                                    "Origin",
                                    "http://localhost:5173",
                                    "Access-Control-Request-Method",
                                    "POST"));
            assertThat(accepted.statusCode()).isEqualTo(200);
            assertThat(accepted.headers().firstValue("Access-Control-Allow-Origin"))
                    .contains("http://localhost:5173");
            assertThat(accepted.headers().firstValue("Access-Control-Max-Age")).contains("11");

            envelope(
                    exchange(
                            client,
                            "OPTIONS",
                            "/api/v1/auth/login",
                            null,
                            null,
                            Map.of(
                                    "Origin",
                                    "https://untrusted.example",
                                    "Access-Control-Request-Method",
                                    "POST")),
                    403,
                    3002);
            envelope(
                    exchange(
                            client,
                            "POST",
                            "/api/v1/auth/login",
                            "{",
                            null,
                            Map.of("Content-Type", "application/json")),
                    400,
                    2004);
            envelope(
                    exchange(
                            client,
                            "POST",
                            "/api/v1/auth/login",
                            "text",
                            null,
                            Map.of("Content-Type", "text/plain")),
                    415,
                    2009);

            String login =
                    mapper.writeValueAsString(
                            Map.of("identifier", "http.errors.admin", "password", password));
            String token =
                    envelope(
                                    exchange(
                                            client,
                                            "POST",
                                            "/api/v1/auth/login",
                                            login,
                                            null,
                                            Map.of("Content-Type", "application/json")),
                                    200,
                                    1000)
                            .path("data")
                            .path("accessToken")
                            .asString();
            assertThat(token).isNotBlank();

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

            String invalidPassword =
                    mapper.writeValueAsString(
                            Map.of(
                                    "username",
                                    "http.errors.rejected",
                                    "fullName",
                                    "Rejected",
                                    "role",
                                    "CUSTOMER",
                                    "password",
                                    "SixteenChars!123"));
            envelope(
                    exchange(
                            client,
                            "POST",
                            "/api/v1/admin/users",
                            invalidPassword,
                            token,
                            Map.of("Content-Type", "application/json")),
                    400,
                    2007);
            assertThat(users.existsByUsername("http.errors.rejected")).isFalse();

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
