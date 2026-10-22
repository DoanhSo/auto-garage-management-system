package com.Group13_65CNTT.AutoGarage.audit.controller;

import com.Group13_65CNTT.AutoGarage.audit.dto.response.AuditEventResponse;
import com.Group13_65CNTT.AutoGarage.audit.service.AuditQueryService;
import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.AuditConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.UserQueryConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.AUDIT_BASE)
@Tag(name = "Audit Events")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class AuditController {
    private final PaginationPolicy pagination;
    private final AuditQueryService auditService;

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Search audit events without credential or replay payload disclosure")
    public ApiResponse<Page<AuditEventResponse>> search(
            @RequestParam(required = false) @Positive Long actorId,
            @RequestParam(required = false) @Size(max = AuditConstants.ACTION_MAX_LENGTH)
                    String action,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_SIZE_EXPRESSION) @Min(1)
                    int size,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                auditService.searchEvents(
                        actorId,
                        action,
                        pagination.page(
                                page,
                                size,
                                Sort.by(UserQueryConstants.DEFAULT_SORT_BY).descending())),
                http.getRequestURI());
    }
}
