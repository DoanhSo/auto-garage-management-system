package com.Group13_65CNTT.AutoGarage.reception.controller;

import com.Group13_65CNTT.AutoGarage.reception.dto.CheckInVehicleRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.CheckInVehicleResponse;
import com.Group13_65CNTT.AutoGarage.reception.service.ReceptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reception")
public class ReceptionController {

    private final ReceptionService receptionService;

    public ReceptionController(ReceptionService receptionService) {
        this.receptionService = receptionService;
    }

    @PostMapping("/check-in")
    public ResponseEntity<CheckInVehicleResponse> checkInVehicle(@Valid @RequestBody CheckInVehicleRequest request) {
        CheckInVehicleResponse response = receptionService.checkInVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
