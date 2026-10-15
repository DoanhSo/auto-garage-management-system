package com.Group13_65CNTT.AutoGarage.notification.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.notification.dto.request.NotificationCreateRequest;
import com.Group13_65CNTT.AutoGarage.notification.dto.response.NotificationResponse;
import com.Group13_65CNTT.AutoGarage.notification.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.NOTIFICATIONS_BASE)
@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
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

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "List own web notifications")
    public ApiResponse<Page<NotificationResponse>> inbox(
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "${garage.pagination.default-size:20}") @Min(1) int size,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                notificationService.inbox(
                        unread, pagination.page(page, size, Sort.by("id").descending())),
                http.getRequestURI());
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Read own notification")
    public ApiResponse<NotificationResponse> get(
            @PathVariable @Min(1) Long id, HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                notificationService.getNotification(id),
                http.getRequestURI());
    }

    @PostMapping("/{id}/read")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Mark own notification read")
    public ApiResponse<NotificationResponse> markRead(
            @PathVariable @Min(1) Long id, HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.UPDATED, notificationService.markRead(id), http.getRequestURI());
    }
}
