package com.Group13_65CNTT.AutoGarage.common.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.dto.UiPolicyResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.service.UiPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.UI_POLICY)
@RequiredArgsConstructor
@Tag(name = "UI Policy")
public class UiPolicyController {
    private final UiPolicyService service;

    @GetMapping
    @Operation(summary = "Read public UI validation limits from server configuration")
    public ApiResponse<UiPolicyResponse> getPolicy(HttpServletRequest request) {
        return ApiResponse.success(ResponseCode.SUCCESS, service.getPolicy(), request.getRequestURI());
    }
}
