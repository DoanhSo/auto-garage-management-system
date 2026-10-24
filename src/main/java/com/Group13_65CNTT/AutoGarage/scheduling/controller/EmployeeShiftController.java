package com.Group13_65CNTT.AutoGarage.scheduling.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ShiftCreateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.ShiftResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.service.SchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping(ApiPaths.SHIFTS_BASE)
@RequiredArgsConstructor
@Tag(name = "Shift Management", description = "Employee shift scheduling and dispatching")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class EmployeeShiftController {

    private static final Set<String> SORT_FIELDS =
            Set.of("id", "startsAt", "endsAt", "status", "createdAt");

    private final SchedulingService schedulingService;
    private final PaginationPolicy pagination;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Assign a shift to an employee")
    public ResponseEntity<ApiResponse<ShiftResponse>> createShift(
            @Valid @RequestBody ShiftCreateRequest request,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userId();
        ShiftResponse response = schedulingService.createShift(actorId, request);
        return ResponseEntity.created(URI.create(ApiPaths.SHIFTS_BASE + "/" + response.getId()))
                .body(ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Cancel an employee shift")
    public ResponseEntity<ApiResponse<Void>> cancelShift(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userId();
        schedulingService.cancelShift(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, null, httpRequest.getRequestURI()));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Get shift schedule of an employee")
    public ResponseEntity<ApiResponse<Page<ShiftResponse>>> getEmployeeShifts(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "startsAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            HttpServletRequest httpRequest) {
        Page<ShiftResponse> responses = schedulingService.getEmployeeShifts(
                employeeId, pagination.page(page, size != null ? size : pagination.defaultSize(), sortBy, sortDir, SORT_FIELDS));
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, responses, httpRequest.getRequestURI()));
    }
}
