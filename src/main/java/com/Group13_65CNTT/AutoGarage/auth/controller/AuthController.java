package com.Group13_65CNTT.AutoGarage.auth.controller;

import com.Group13_65CNTT.AutoGarage.auth.service.AuthService;
import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.identity.dto.response.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.AUTH_BASE)
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = "Keycloak OIDC resource server identity and session revocation")
public class AuthController {
    private final AuthService authService;

    @GetMapping(ApiPaths.ME)
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Get current authenticated user and synchronize Keycloak identity")
    @SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
    public ApiResponse<UserResponse> me(HttpServletRequest httpRequest) {
        return ApiResponse.success(
                ResponseCode.SUCCESS, authService.me(), httpRequest.getRequestURI());
    }

    @PostMapping(ApiPaths.LOGOUT)
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Revoke all active tokens of current user on the resource server")
    @SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
    public ApiResponse<Void> logout(HttpServletRequest httpRequest) {
        authService.logout();
        return ApiResponse.success(ResponseCode.SUCCESS, null, httpRequest.getRequestURI());
    }
}
