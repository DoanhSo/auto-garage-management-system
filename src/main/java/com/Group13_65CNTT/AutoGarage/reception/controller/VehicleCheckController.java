package com.Group13_65CNTT.AutoGarage.reception.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.reception.dto.CreateVehicleCheckRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.VehicleCheckResponse;
import com.Group13_65CNTT.AutoGarage.reception.service.VehicleCheckService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.VEHICLE_CHECKS_BASE)
@RequiredArgsConstructor
@Tag(name = "Vehicle Checks")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class VehicleCheckController {

    private final VehicleCheckService vehicleCheckService;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_TECHNICAL_ACCESS)
    public ResponseEntity<VehicleCheckResponse> recordVehicleCheck(
            @Valid @RequestBody CreateVehicleCheckRequest request) {
        VehicleCheckResponse response = vehicleCheckService.recordVehicleCheck(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public ResponseEntity<VehicleCheckResponse> getVehicleCheckById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleCheckService.getVehicleCheckById(id));
    }

    @GetMapping("/order/{repairOrderId}")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public ResponseEntity<List<VehicleCheckResponse>> getChecksByRepairOrder(
            @PathVariable Long repairOrderId) {
        return ResponseEntity.ok(vehicleCheckService.getChecksByRepairOrder(repairOrderId));
    }
}
