package com.Group13_65CNTT.AutoGarage.notification.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.UserQueryConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.notification.dto.request.NotificationBroadcastRequest;
import com.Group13_65CNTT.AutoGarage.notification.dto.request.NotificationCreateRequest;
import com.Group13_65CNTT.AutoGarage.notification.dto.response.NotificationBroadcastResponse;
import com.Group13_65CNTT.AutoGarage.notification.dto.response.NotificationResponse;
import com.Group13_65CNTT.AutoGarage.notification.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.NOTIFICATIONS_BASE)
@Tag(name = "Notifications")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class NotificationController {
    private final PaginationPolicy pagination;
    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Enqueue notification after transaction commit")
    public ApiResponse<NotificationResponse> enqueue(
            @Valid @RequestBody NotificationCreateRequest request, HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.CREATED, notificationService.enqueue(request), http.getRequestURI());
    }

    @PostMapping(ApiPaths.NOTIFICATIONS_BROADCAST)
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Broadcast web notification by role or to all active users")
    public ApiResponse<NotificationBroadcastResponse> broadcast(
            @Valid @RequestBody NotificationBroadcastRequest request, HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.CREATED,
                notificationService.broadcast(request),
                http.getRequestURI());
    }

    @GetMapping(ApiPaths.NOTIFICATIONS_ADMIN)
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "List all notifications for administrator management")
    public ApiResponse<Page<NotificationResponse>> listForAdmin(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_SIZE_EXPRESSION) @Min(1)
                    int size,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                notificationService.listAllForAdmin(
                        channel,
                        status,
                        keyword,
                        pagination.page(
                                page,
                                size,
                                Sort.by(UserQueryConstants.DEFAULT_SORT_BY).descending())),
                http.getRequestURI());
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "List own web notifications")
    public ApiResponse<Page<NotificationResponse>> inbox(
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_SIZE_EXPRESSION) @Min(1)
                    int size,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                notificationService.inbox(
                        unread,
                        pagination.page(
                                page,
                                size,
                                Sort.by(UserQueryConstants.DEFAULT_SORT_BY).descending())),
                http.getRequestURI());
    }

    @GetMapping(ApiPaths.BY_ID)
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Read own notification")
    public ApiResponse<NotificationResponse> get(
            @PathVariable @Min(1) Long id, HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                notificationService.getNotification(id),
                http.getRequestURI());
    }

    @PostMapping(ApiPaths.NOTIFICATION_READ)
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Mark own notification read")
    public ApiResponse<NotificationResponse> markRead(
            @PathVariable @Min(1) Long id, HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.UPDATED, notificationService.markRead(id), http.getRequestURI());
    }

    @DeleteMapping(ApiPaths.BY_ID)
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Delete / revoke notification")
    public ApiResponse<Void> delete(@PathVariable @Min(1) Long id, HttpServletRequest http) {
        notificationService.deleteNotification(id);
        return ApiResponse.success(ResponseCode.DELETED, null, http.getRequestURI());
    }
}
