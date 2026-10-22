package com.Group13_65CNTT.AutoGarage.reception.controller;

import com.Group13_65CNTT.AutoGarage.reception.dto.CreateVehicleCheckRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.VehicleCheckResponse;
import com.Group13_65CNTT.AutoGarage.reception.service.VehicleCheckService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicle-checks")
public class VehicleCheckController {

    private final VehicleCheckService vehicleCheckService;

    public VehicleCheckController(VehicleCheckService vehicleCheckService) {
        this.vehicleCheckService = vehicleCheckService;
    }

    @PostMapping
    public ResponseEntity<VehicleCheckResponse> recordVehicleCheck(@Valid @RequestBody CreateVehicleCheckRequest request) {
        VehicleCheckResponse response = vehicleCheckService.recordVehicleCheck(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleCheckResponse> getVehicleCheckById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleCheckService.getVehicleCheckById(id));
    }

    @GetMapping("/order/{repairOrderId}")
    public ResponseEntity<List<VehicleCheckResponse>> getChecksByRepairOrder(@PathVariable Long repairOrderId) {
        return ResponseEntity.ok(vehicleCheckService.getChecksByRepairOrder(repairOrderId));
    }
}
