package com.Group13_65CNTT.AutoGarage;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Group13_65CNTT.AutoGarage.auth.controller.BrowserSessionController;
import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:autogarage_browser_session;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "security.auth.allowed-origins=http://localhost:3000",
    "security.browser-session.secure-cookie=true"
})
@ActiveProfiles("h2")
@AutoConfigureMockMvc
@RequiredArgsConstructor
class BrowserSessionHttpIntegrationTest extends IntegrationTestSupport {
    private static final AtomicInteger PROVIDER_STATUS = new AtomicInteger(200);
    private static final AtomicInteger PROVIDER_CALLS = new AtomicInteger();
    private static final HttpServer PROVIDER;
    private static final String ORIGIN = "http://localhost:3000";
    static {
        try {
            PROVIDER = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            PROVIDER.createContext("/", exchange -> {
                PROVIDER_CALLS.incrementAndGet();
                exchange.getRequestBody().readAllBytes();
                int status = PROVIDER_STATUS.get();
                String body = status == 200
                    ? "{\"access_token\":\"test-access\",\"refresh_token\":\"rotated-refresh\",\"expires_in\":300,\"token_type\":\"Bearer\"}"
                    : "{\"error\":\"private-provider-details\"}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                try (var output = exchange.getResponseBody()) { output.write(bytes); }
            });
            PROVIDER.start();
        } catch (Exception failure) { throw new ExceptionInInitializerError(failure); }
    }
    @DynamicPropertySource
    static void providerProperties(DynamicPropertyRegistry registry) {
        registry.add("security.browser-session.token-uri", () -> "http://127.0.0.1:" + PROVIDER.getAddress().getPort() + "/token");
    }
    private final MockMvc mvc;
    @MockitoBean JwtDecoder decoder;

    @BeforeEach
    void fixtures() {
        PROVIDER_STATUS.set(200);
        PROVIDER_CALLS.set(0);
        when(decoder.decode("test-access")).thenReturn(Jwt.withTokenValue("test-access").header("alg", "RS256").subject("identity-one").build());
    }
    private Cookie fixtureCookie() { return new Cookie(BrowserSessionController.COOKIE_NAME, "fixture-refresh"); }

    @Test
    void restoresAndRotatesHttpOnlyCookieWithoutExposingRefreshToken() throws Exception {
        mvc.perform(post(ApiPaths.BROWSER_SESSION_REFRESH).header("Origin", ORIGIN).cookie(fixtureCookie()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.path").value(ApiPaths.BROWSER_SESSION_REFRESH))
            .andExpect(jsonPath("$.data.accessToken").value("test-access"))
            .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(cookie().value(BrowserSessionController.COOKIE_NAME, "rotated-refresh"))
            .andExpect(cookie().httpOnly(BrowserSessionController.COOKIE_NAME, true))
            .andExpect(cookie().secure(BrowserSessionController.COOKIE_NAME, true))
            .andExpect(cookie().path(BrowserSessionController.COOKIE_NAME, ApiPaths.BROWSER_SESSION))
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("SameSite=Strict")));
    }
    @Test
    void missingOrRejectedSessionReturnsStandard401AndClearsCookie() throws Exception {
        mvc.perform(post(ApiPaths.BROWSER_SESSION_REFRESH).header("Origin", ORIGIN))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(3003))
            .andExpect(cookie().maxAge(BrowserSessionController.COOKIE_NAME, 0));
        PROVIDER_STATUS.set(400);
        mvc.perform(post(ApiPaths.BROWSER_SESSION_REFRESH).header("Origin", ORIGIN).cookie(fixtureCookie()))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.success").value(false))
            .andExpect(cookie().maxAge(BrowserSessionController.COOKIE_NAME, 0))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-provider-details"))));
    }
    @Test
    void providerOutagePreservesCookieAndReturnsSafe503() throws Exception {
        PROVIDER_STATUS.set(503);
        mvc.perform(post(ApiPaths.BROWSER_SESSION_REFRESH).header("Origin", ORIGIN).cookie(fixtureCookie()))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(4002))
            .andExpect(header().doesNotExist("Set-Cookie"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-provider-details"))));
    }
    @Test
    void rejectsMissingAndUntrustedOriginsBeforeUsingCookie() throws Exception {
        for (String origin : new String[]{"", "http://evil.test", "null"}) {
            mvc.perform(post(ApiPaths.BROWSER_SESSION_REFRESH).header("Origin", origin).cookie(fixtureCookie()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(3002));
        }
        assertThat(PROVIDER_CALLS.get()).isZero();
    }
    @Test
    void onlySessionEndpointsAllowCredentialedCors() throws Exception {
        mvc.perform(options(ApiPaths.BROWSER_SESSION_REFRESH).header("Origin", ORIGIN).header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options(ApiPaths.AUTH_BASE + ApiPaths.ME).header("Origin", ORIGIN).header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk()).andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }
    @Test
    void sessionOpeningRequiresBearerAndBindsRefreshedIdentity() throws Exception {
        mvc.perform(post(ApiPaths.BROWSER_SESSION).header("Origin", ORIGIN).contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"fixture-refresh\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post(ApiPaths.BROWSER_SESSION).header("Origin", ORIGIN)
                .with(jwt().jwt(j -> j.subject("identity-one")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"fixture-refresh\"}"))
            .andExpect(status().isOk()).andExpect(cookie().httpOnly(BrowserSessionController.COOKIE_NAME, true));
        mvc.perform(post(ApiPaths.BROWSER_SESSION).header("Origin", ORIGIN)
                .with(jwt().jwt(j -> j.subject("different-identity")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"fixture-refresh\"}"))
            .andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Set-Cookie"));
    }
    @Test
    void revokedAccessTokenIsNotRestored() throws Exception {
        when(decoder.decode("test-access")).thenThrow(new JwtException("private-token-detail"));
        mvc.perform(post(ApiPaths.BROWSER_SESSION_REFRESH).header("Origin", ORIGIN).cookie(fixtureCookie()))
            .andExpect(status().isUnauthorized()).andExpect(cookie().maxAge(BrowserSessionController.COOKIE_NAME, 0));
    }
    @Test
    void logoutClearsCookieEvenWhenRevocationCannotBeConfirmed() throws Exception {
        PROVIDER_STATUS.set(503);
        mvc.perform(delete(ApiPaths.BROWSER_SESSION).header("Origin", ORIGIN).cookie(fixtureCookie()))
            .andExpect(status().isServiceUnavailable()).andExpect(cookie().maxAge(BrowserSessionController.COOKIE_NAME, 0));
    }
}
