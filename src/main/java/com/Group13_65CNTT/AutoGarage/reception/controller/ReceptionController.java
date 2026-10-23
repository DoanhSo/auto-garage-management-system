package com.Group13_65CNTT.AutoGarage.reception.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.reception.dto.CheckInVehicleRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.CheckInVehicleResponse;
import com.Group13_65CNTT.AutoGarage.reception.service.ReceptionService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.RECEPTION_BASE)
@RequiredArgsConstructor
@Tag(name = "Reception")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class ReceptionController {

    private final ReceptionService receptionService;

    @PostMapping("/check-in")
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    public ResponseEntity<CheckInVehicleResponse> checkInVehicle(
            @Valid @RequestBody CheckInVehicleRequest request) {
        CheckInVehicleResponse response = receptionService.checkInVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
