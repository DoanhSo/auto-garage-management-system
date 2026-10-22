package com.Group13_65CNTT.AutoGarage.reception.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.reception.dto.CreateVehicleCheckRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.VehicleCheckResponse;
import com.Group13_65CNTT.AutoGarage.reception.service.VehicleCheckService;

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
@RequestMapping(ApiPaths.VEHICLE_CHECKS_BASE)
@RequiredArgsConstructor
@Tag(name = "Vehicle Inspection", description = "Vehicle check sheets, reception inspection and technical evaluations")
@SecurityRequirement(name = "bearerAuth")
public class VehicleCheckController {

    private final VehicleCheckService vehicleCheckService;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_RECEPTIONIST_OR_TECHNICIAN_OR_ADMIN)
    @Operation(summary = "Record vehicle check result")
    public ResponseEntity<ApiResponse<VehicleCheckResponse>> recordVehicleCheck(
            @Valid @RequestBody CreateVehicleCheckRequest request,
            HttpServletRequest httpRequest) {
        VehicleCheckResponse response = vehicleCheckService.recordVehicleCheck(request);
        return ResponseEntity.created(URI.create(ApiPaths.VEHICLE_CHECKS_BASE + "/" + response.getId()))
                .body(ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Fetch vehicle check by ID")
    public ResponseEntity<ApiResponse<VehicleCheckResponse>> getVehicleCheckById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        VehicleCheckResponse response = vehicleCheckService.getVehicleCheckById(id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/order/{repairOrderId}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Fetch checks by repair order ID")
    public ResponseEntity<ApiResponse<List<VehicleCheckResponse>>> getChecksByRepairOrder(
            @PathVariable Long repairOrderId,
            HttpServletRequest httpRequest) {
        List<VehicleCheckResponse> list = vehicleCheckService.getChecksByRepairOrder(repairOrderId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, list, httpRequest.getRequestURI()));
    }
}
