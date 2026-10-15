package com.Group13_65CNTT.AutoGarage.audit.controller;

import com.Group13_65CNTT.AutoGarage.audit.dto.response.AuditEventResponse;
import com.Group13_65CNTT.AutoGarage.audit.service.AuditQueryService;
import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.AUDIT_BASE)
@Tag(name = "Audit Events")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {
    private final PaginationPolicy pagination;
    private final AuditQueryService auditService;

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Search audit events without credential or replay payload disclosure")
    public ApiResponse<Page<AuditEventResponse>> search(
            @RequestParam(required = false) @Positive Long actorId,
            @RequestParam(required = false) @Size(max = 100) String action,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "${garage.pagination.default-size:20}") @Min(1) int size,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                auditService.searchEvents(
                        actorId, action, pagination.page(page, size, Sort.by("id").descending())),
                http.getRequestURI());
    }
}
