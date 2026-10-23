package com.Group13_65CNTT.AutoGarage.scheduling.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.MaintenanceReservationRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ServiceBayCreateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ServiceBayUpdateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.BayReservationResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.ServiceBayResponse;
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
@RequestMapping(ApiPaths.SERVICE_BAYS_BASE)
@RequiredArgsConstructor
@Tag(name = "Service Bay Management", description = "Bay configuration, maintenance, and reservations")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class ServiceBayController {

    private static final Set<String> SORT_FIELDS =
            Set.of("id", "bayCode", "bayName", "bayType", "isActive", "createdAt");

    private final SchedulingService schedulingService;
    private final PaginationPolicy pagination;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Create a service bay")
    public ResponseEntity<ApiResponse<ServiceBayResponse>> createServiceBay(
            @Valid @RequestBody ServiceBayCreateRequest request, HttpServletRequest httpRequest) {
        ServiceBayResponse response = schedulingService.createServiceBay(request);
        return ResponseEntity.created(URI.create(ApiPaths.SERVICE_BAYS_BASE + "/" + response.getId()))
                .body(ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Get service bay by ID")
    public ResponseEntity<ApiResponse<ServiceBayResponse>> getServiceBayById(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        ServiceBayResponse response = schedulingService.getServiceBayById(id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "List service bays with pagination")
    public ResponseEntity<ApiResponse<Page<ServiceBayResponse>>> getServiceBays(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "bayCode") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            HttpServletRequest httpRequest) {
        Page<ServiceBayResponse> responses = schedulingService.getServiceBays(
                isActive, pagination.page(page, size, sortBy, sortDir, SORT_FIELDS));
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, responses, httpRequest.getRequestURI()));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Update service bay configuration")
    public ResponseEntity<ApiResponse<ServiceBayResponse>> updateServiceBay(
            @PathVariable Long id,
            @Valid @RequestBody ServiceBayUpdateRequest request,
            HttpServletRequest httpRequest) {
        ServiceBayResponse response = schedulingService.updateServiceBay(id, request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Deactivate service bay")
    public ResponseEntity<ApiResponse<Void>> deactivateServiceBay(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        schedulingService.deactivateServiceBay(id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.DELETED, null, httpRequest.getRequestURI()));
    }

    @PostMapping("/{id}/maintenance")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Schedule bay maintenance (UC37)")
    public ResponseEntity<ApiResponse<BayReservationResponse>> scheduleMaintenance(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceReservationRequest request,
            HttpServletRequest httpRequest) {
        request.setBayId(id);
        Long actorId = currentUserProvider.userIdOrNull();
        BayReservationResponse response = schedulingService.scheduleMaintenance(actorId, request);
        return ResponseEntity.created(URI.create(ApiPaths.SERVICE_BAYS_BASE + "/" + id + "/maintenance/" + response.getId()))
                .body(ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}/reservations")
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "List reservations of a service bay")
    public ResponseEntity<ApiResponse<Page<BayReservationResponse>>> getBayReservations(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        Page<BayReservationResponse> responses = schedulingService.getBayReservations(
                id, pagination.page(page, size, "startsAt", "asc", Set.of("startsAt", "endsAt", "status")));
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, responses, httpRequest.getRequestURI()));
    }
}
