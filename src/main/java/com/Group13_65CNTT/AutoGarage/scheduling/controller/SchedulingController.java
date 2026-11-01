package com.Group13_65CNTT.AutoGarage.scheduling.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.OnShiftTechnicianResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.SCHEDULING_BASE)
@RequiredArgsConstructor
@Tag(name = "Dispatch", description = "Điều phối kỹ thuật viên đang trực ca")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class SchedulingController {

    private final DispatchService dispatchService;

    @GetMapping(ApiPaths.SCHEDULING_ON_SHIFT_TECHNICIANS)
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Danh sách kỹ thuật viên đang trong ca trực tại thời điểm hiện tại")
    public ResponseEntity<ApiResponse<List<OnShiftTechnicianResponse>>> getOnShiftTechnicians(
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                ResponseCode.SUCCESS, dispatchService.getOnShiftTechnicians(), httpRequest.getRequestURI()));
    }
}
