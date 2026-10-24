package com.Group13_65CNTT.AutoGarage.reception.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.reception.dto.CheckInVehicleRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.CheckInVehicleResponse;
import com.Group13_65CNTT.AutoGarage.reception.service.ReceptionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping(ApiPaths.RECEPTION_BASE)
@RequiredArgsConstructor
@Tag(name = "Reception", description = "Vehicle arrival check-in and repair order initialization")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class ReceptionController {

    private final ReceptionService receptionService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/check-in")
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    @Operation(summary = "Check in vehicle at garage reception")
    public ResponseEntity<ApiResponse<CheckInVehicleResponse>> checkInVehicle(
            @Valid @RequestBody CheckInVehicleRequest request,
            HttpServletRequest httpRequest) {
        request.setActorUserId(currentUserProvider.userId());
        CheckInVehicleResponse response = receptionService.checkInVehicle(request);
        return ResponseEntity.created(URI.create(ApiPaths.RECEPTION_BASE + "/check-in/" + response.getRepairOrderId()))
                .body(ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }
}
