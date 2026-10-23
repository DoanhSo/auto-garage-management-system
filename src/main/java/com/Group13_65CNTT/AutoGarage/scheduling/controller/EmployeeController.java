package com.Group13_65CNTT.AutoGarage.scheduling.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.EmployeeCreateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.EmployeeUpdateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.EmployeeResponse;
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
@RequestMapping(ApiPaths.EMPLOYEES_BASE)
@RequiredArgsConstructor
@Tag(name = "Employee Management", description = "Technician and garage staff management")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {

    private static final Set<String> SORT_FIELDS =
            Set.of("id", "employeeCode", "hourlyCost", "canQc", "isActive", "createdAt");

    private final SchedulingService schedulingService;
    private final PaginationPolicy pagination;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Create an employee profile")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
            @Valid @RequestBody EmployeeCreateRequest request, HttpServletRequest httpRequest) {
        EmployeeResponse response = schedulingService.createEmployee(request);
        return ResponseEntity.created(URI.create(ApiPaths.EMPLOYEES_BASE + "/" + response.getId()))
                .body(ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Get employee profile by ID")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeById(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        EmployeeResponse response = schedulingService.getEmployeeById(id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Get employee profile by User ID")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeByUserId(
            @PathVariable Long userId, HttpServletRequest httpRequest) {
        EmployeeResponse response = schedulingService.getEmployeeByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "List employees with pagination")
    public ResponseEntity<ApiResponse<Page<EmployeeResponse>>> getEmployees(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest httpRequest) {
        Page<EmployeeResponse> responses = schedulingService.getEmployees(
                isActive, pagination.page(page, size, sortBy, sortDir, SORT_FIELDS));
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, responses, httpRequest.getRequestURI()));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Update employee profile")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequest request,
            HttpServletRequest httpRequest) {
        EmployeeResponse response = schedulingService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Deactivate employee")
    public ResponseEntity<ApiResponse<Void>> deactivateEmployee(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        schedulingService.deactivateEmployee(id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.DELETED, null, httpRequest.getRequestURI()));
    }
}
