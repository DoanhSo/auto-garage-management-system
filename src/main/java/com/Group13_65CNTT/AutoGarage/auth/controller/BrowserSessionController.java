package com.Group13_65CNTT.AutoGarage.auth.controller;

import com.Group13_65CNTT.AutoGarage.auth.dto.request.BrowserSessionRequest;
import com.Group13_65CNTT.AutoGarage.auth.dto.response.BrowserSessionResponse;
import com.Group13_65CNTT.AutoGarage.auth.service.BrowserSessionService;
import com.Group13_65CNTT.AutoGarage.auth.service.BrowserSessionTokens;
import com.Group13_65CNTT.AutoGarage.common.config.BrowserSessionProperties;
import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.AUTH_BASE)
@RequiredArgsConstructor
@Tag(name = "Browser session", description = "HttpOnly refresh cookie; API access remains Bearer-only")
public class BrowserSessionController {
    public static final String COOKIE_NAME = "AG_REFRESH";
    private final BrowserSessionService sessions;
    private final BrowserSessionProperties properties;
    private final CurrentUserProvider currentUser;

    @PostMapping(ApiPaths.SESSION)
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Establish browser refresh session for the authenticated identity")
    public ApiResponse<BrowserSessionResponse> open(@Valid @RequestBody BrowserSessionRequest body, HttpServletRequest request, HttpServletResponse response) {
        prepare(request, response);
        return complete(sessions.open(body.getRefreshToken(), currentUser.subjectOrNull()), request, response);
    }

    @PostMapping(ApiPaths.SESSION_REFRESH)
    @PreAuthorize(SecurityConstants.PERMIT_ALL)
    @Operation(summary = "Restore access token from HttpOnly refresh cookie")
    public ApiResponse<BrowserSessionResponse> refresh(@CookieValue(name = COOKIE_NAME, required = false) String cookie, HttpServletRequest request, HttpServletResponse response) {
        prepare(request, response);
        try {
            return complete(sessions.refresh(cookie), request, response);
        } catch (BusinessException failure) {
            if (failure.getResponseCode() == ResponseCode.INVALID_TOKEN || failure.getResponseCode() == ResponseCode.UNAUTHORIZED) clear(response);
            throw failure;
        }
    }

    @DeleteMapping(ApiPaths.SESSION)
    @PreAuthorize(SecurityConstants.PERMIT_ALL)
    @Operation(summary = "Clear browser refresh cookie and revoke its Keycloak session")
    public ApiResponse<Void> close(@CookieValue(name = COOKIE_NAME, required = false) String cookie, HttpServletRequest request, HttpServletResponse response) {
        prepare(request, response);
        clear(response); // Clear even when the provider cannot confirm revocation.
        sessions.close(cookie);
        return ApiResponse.success(ResponseCode.SUCCESS, null, request.getRequestURI());
    }

    private void prepare(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        sessions.validateOrigin(request.getHeader(HttpHeaders.ORIGIN));
    }

    private ApiResponse<BrowserSessionResponse> complete(BrowserSessionTokens tokens, HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(tokens.refreshToken()).build().toString());
        return ApiResponse.success(ResponseCode.SUCCESS, tokens.response(), request.getRequestURI());
    }

    private ResponseCookie.ResponseCookieBuilder cookie(String value) {
        return ResponseCookie.from(COOKIE_NAME, value).httpOnly(true).secure(properties.secureCookie()).sameSite("Strict").path(ApiPaths.BROWSER_SESSION);
    }

    private void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("").maxAge(0).build().toString());
    }
}
