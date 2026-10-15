package com.Group13_65CNTT.AutoGarage.auth.controller;

import com.Group13_65CNTT.AutoGarage.auth.dto.request.LoginRequest;
import com.Group13_65CNTT.AutoGarage.auth.dto.request.PasswordChangeRequest;
import com.Group13_65CNTT.AutoGarage.auth.dto.response.LoginResponse;
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
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.AUTH_BASE)
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Bearer authentication and own-account security")
public class AuthController {
    private final AuthService authService;

    @PostMapping(ApiPaths.LOGIN)
    @Operation(summary = "Login with username or email")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success(
                ResponseCode.SUCCESS, authService.login(request), httpRequest.getRequestURI());
    }

    @GetMapping(ApiPaths.ME)
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Get current user")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<UserResponse> me(HttpServletRequest httpRequest) {
        return ApiResponse.success(
                ResponseCode.SUCCESS, authService.me(), httpRequest.getRequestURI());
    }

    @PostMapping(ApiPaths.LOGOUT)
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Revoke all tokens of current user")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest) {
        authService.logout();
        return ApiResponse.success(ResponseCode.SUCCESS, null, httpRequest.getRequestURI());
    }

    @PostMapping(ApiPaths.PASSWORD)
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Change own password and revoke tokens")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody PasswordChangeRequest request, HttpServletRequest httpRequest) {
        authService.changePassword(request);
        return ApiResponse.success(ResponseCode.UPDATED, null, httpRequest.getRequestURI());
    }
}
