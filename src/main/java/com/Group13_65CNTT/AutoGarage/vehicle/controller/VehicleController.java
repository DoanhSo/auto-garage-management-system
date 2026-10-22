package com.Group13_65CNTT.AutoGarage.vehicle.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.CreateVehicleRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.TransferOwnershipRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.UpdateVehicleRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleResponse;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleTimelineResponse;
import com.Group13_65CNTT.AutoGarage.vehicle.service.VehicleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.VEHICLES_BASE)
@RequiredArgsConstructor
@Tag(name = "Vehicle Management", description = "Vehicle profiles, odometer history and ownership transfers")
@SecurityRequirement(name = "bearerAuth")
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_RECEPTIONIST_OR_ADMIN)
    @Operation(summary = "Register vehicle")
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(
            @Valid @RequestBody CreateVehicleRequest request,
            HttpServletRequest httpRequest) {
        VehicleResponse response = vehicleService.createVehicle(request);
        return ResponseEntity.created(URI.create(ApiPaths.VEHICLES_BASE + "/" + response.getId()))
                .body(ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Search vehicles")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getVehicles(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            HttpServletRequest httpRequest) {
        List<VehicleResponse> list = vehicleService.searchVehicles(customerId, search, isActive);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, list, httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Fetch vehicle by ID")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicleById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        VehicleResponse response = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/plate/{plate}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Fetch vehicle by license plate")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicleByPlate(
            @PathVariable String plate,
            HttpServletRequest httpRequest) {
        VehicleResponse response = vehicleService.getVehicleByPlate(plate);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_RECEPTIONIST_OR_ADMIN)
    @Operation(summary = "Update vehicle information")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleRequest request,
            HttpServletRequest httpRequest) {
        VehicleResponse response = vehicleService.updateVehicle(id, request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize(SecurityConstants.HAS_ROLE_RECEPTIONIST_OR_ADMIN)
    @Operation(summary = "Transfer vehicle ownership")
    public ResponseEntity<ApiResponse<VehicleResponse>> transferOwnership(
            @PathVariable Long id,
            @Valid @RequestBody TransferOwnershipRequest request,
            HttpServletRequest httpRequest) {
        VehicleResponse response = vehicleService.transferOwnership(id, request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_RECEPTIONIST_OR_ADMIN)
    @Operation(summary = "Deactivate vehicle")
    public ResponseEntity<ApiResponse<Void>> deactivateVehicle(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        vehicleService.deactivateVehicle(id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.DELETED, null, httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Get vehicle service and ownership timeline")
    public ResponseEntity<ApiResponse<VehicleTimelineResponse>> getVehicleTimeline(
            @PathVariable Long id,
            @RequestParam(required = false) Long requestingCustomerId,
            @RequestParam(required = false, defaultValue = "STAFF") String role,
            HttpServletRequest httpRequest) {
        VehicleTimelineResponse response = vehicleService.getVehicleTimeline(id, requestingCustomerId, role);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/plate/{plate}/timeline")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Get vehicle service timeline by license plate")
    public ResponseEntity<ApiResponse<VehicleTimelineResponse>> getVehicleTimelineByPlate(
            @PathVariable String plate,
            @RequestParam(required = false) Long requestingCustomerId,
            @RequestParam(required = false, defaultValue = "STAFF") String role,
            HttpServletRequest httpRequest) {
        VehicleTimelineResponse response = vehicleService.getVehicleTimelineByPlate(plate, requestingCustomerId, role);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }
}
