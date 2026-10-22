package com.Group13_65CNTT.AutoGarage.identity.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.UserQueryConstants;
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

@RestController
@RequestMapping(ApiPaths.USERS_BASE)
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Administrator account and role management")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class UserController {
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
            @RequestParam(required = false) @Size(max = SecurityConstants.EMAIL_MAX_LENGTH)
                    String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_SIZE_EXPRESSION) @Min(1)
                    int size,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = UserQueryConstants.SORT_ASC) String sortDir,
            HttpServletRequest httpRequest) {
        Page<UserResponse> response =
                userService.searchUsers(
                        keyword,
                        active,
                        pagination.page(
                                page, size, sortBy, sortDir, UserQueryConstants.SORT_FIELDS));
        return ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI());
    }

    @GetMapping(ApiPaths.BY_ID)
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Get user by ID")
    public ApiResponse<UserResponse> getUser(
            @PathVariable @Min(1) Long id, HttpServletRequest httpRequest) {
        return ApiResponse.success(
                ResponseCode.SUCCESS, userService.getUser(id), httpRequest.getRequestURI());
    }

    @PutMapping(ApiPaths.BY_ID)
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

    @DeleteMapping(ApiPaths.BY_ID)
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(
            summary = "Delete user",
            description =
                    "Removes from management without deleting history; use PUT active=false to lock")
    public ApiResponse<Void> deleteUser(
            @PathVariable @Min(1) Long id,
            @RequestParam @Min(0) long version,
            HttpServletRequest httpRequest) {
        userService.deleteUser(id, version);
        return ApiResponse.success(ResponseCode.DELETED, null, httpRequest.getRequestURI());
    }
}
