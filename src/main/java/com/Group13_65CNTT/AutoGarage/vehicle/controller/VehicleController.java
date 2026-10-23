package com.Group13_65CNTT.AutoGarage.vehicle.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.CreateVehicleRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.TransferOwnershipRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.UpdateVehicleRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleResponse;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleTimelineResponse;
import com.Group13_65CNTT.AutoGarage.vehicle.service.VehicleService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
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

import java.util.List;

@RestController
@RequestMapping(ApiPaths.VEHICLES_BASE)
@RequiredArgsConstructor
@Tag(name = "Vehicles")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class VehicleController {

    private final VehicleService vehicleService;
    private final CurrentUserProvider currentUserProvider;
    private final CustomerRepository customerRepository;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    public ResponseEntity<VehicleResponse> createVehicle(
            @Valid @RequestBody CreateVehicleRequest request) {
        VehicleResponse response = vehicleService.createVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public ResponseEntity<List<VehicleResponse>> getVehicles(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive) {
        List<VehicleResponse> list = vehicleService.searchVehicles(customerId, search, isActive);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }

    @GetMapping("/plate/{plate}")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public ResponseEntity<VehicleResponse> getVehicleByPlate(@PathVariable String plate) {
        return ResponseEntity.ok(vehicleService.getVehicleByPlate(plate));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable Long id, @Valid @RequestBody UpdateVehicleRequest request) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, request));
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    public ResponseEntity<VehicleResponse> transferOwnership(
            @PathVariable Long id, @Valid @RequestBody TransferOwnershipRequest request) {
        return ResponseEntity.ok(vehicleService.transferOwnership(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    public ResponseEntity<Void> deactivateVehicle(@PathVariable Long id) {
        vehicleService.deactivateVehicle(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public ResponseEntity<VehicleTimelineResponse> getVehicleTimeline(
            @PathVariable Long id,
            @RequestParam(required = false) Long requestingCustomerId,
            @RequestParam(required = false) String role) {
        String effectiveRole = resolveEffectiveRole(role);
        Long effectiveCustomerId = resolveEffectiveCustomerId(effectiveRole, requestingCustomerId);
        return ResponseEntity.ok(
                vehicleService.getVehicleTimeline(id, effectiveCustomerId, effectiveRole));
    }

    @GetMapping("/plate/{plate}/timeline")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public ResponseEntity<VehicleTimelineResponse> getVehicleTimelineByPlate(
            @PathVariable String plate,
            @RequestParam(required = false) Long requestingCustomerId,
            @RequestParam(required = false) String role) {
        String effectiveRole = resolveEffectiveRole(role);
        Long effectiveCustomerId = resolveEffectiveCustomerId(effectiveRole, requestingCustomerId);
        return ResponseEntity.ok(
                vehicleService.getVehicleTimelineByPlate(
                        plate, effectiveCustomerId, effectiveRole));
    }

    private String resolveEffectiveRole(String fallbackRole) {
        try {
            return currentUserProvider.role().name();
        } catch (Exception ignored) {
            return fallbackRole != null ? fallbackRole : "STAFF";
        }
    }

    private Long resolveEffectiveCustomerId(String effectiveRole, Long fallbackCustomerId) {
        if (Role.CUSTOMER.name().equalsIgnoreCase(effectiveRole) && customerRepository != null) {
            Long userId = currentUserProvider.userIdOrNull();
            if (userId != null) {
                return customerRepository
                        .findByUserId(userId)
                        .map(Customer::getId)
                        .orElse(fallbackCustomerId);
            }
        }
        return fallbackCustomerId;
    }
}
