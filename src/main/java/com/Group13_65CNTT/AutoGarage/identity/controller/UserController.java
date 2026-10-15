package com.Group13_65CNTT.AutoGarage.identity.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.identity.dto.request.UserCreateRequest;
import com.Group13_65CNTT.AutoGarage.identity.dto.request.UserUpdateRequest;
import com.Group13_65CNTT.AutoGarage.identity.dto.response.UserResponse;
import com.Group13_65CNTT.AutoGarage.identity.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping(ApiPaths.USERS_BASE)
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Administrator account and role management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private static final Set<String> SORT_FIELDS =
            Set.of("id", "username", "fullName", "role", "active", "createdAt");
    private final PaginationPolicy pagination;
    private final UserService userService;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Create user")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest request, HttpServletRequest httpRequest) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.created(URI.create(ApiPaths.USERS_BASE + "/" + response.getId()))
                .body(
                        ApiResponse.success(
                                ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Search and paginate users")
    public ApiResponse<Page<UserResponse>> searchUsers(
            @RequestParam(required = false) @Size(max = 254) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "${garage.pagination.default-size:20}") @Min(1) int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            HttpServletRequest httpRequest) {
        Page<UserResponse> response =
                userService.searchUsers(
                        keyword, active, pagination.page(page, size, sortBy, sortDir, SORT_FIELDS));
        return ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI());
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Get user by ID")
    public ApiResponse<UserResponse> getUser(
            @PathVariable @Min(1) Long id, HttpServletRequest httpRequest) {
        return ApiResponse.success(
                ResponseCode.SUCCESS, userService.getUser(id), httpRequest.getRequestURI());
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(
            summary = "Update user profile and role",
            description = "Requires the current version; email null clears email")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody UserUpdateRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(
                ResponseCode.UPDATED,
                userService.updateUser(id, request),
                httpRequest.getRequestURI());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Deactivate user", description = "Soft delete; preserves account history")
    public ApiResponse<Void> deactivateUser(
            @PathVariable @Min(1) Long id,
            @RequestParam @Min(0) long version,
            HttpServletRequest httpRequest) {
        userService.deactivateUser(id, version);
        return ApiResponse.success(ResponseCode.DELETED, null, httpRequest.getRequestURI());
    }
}
