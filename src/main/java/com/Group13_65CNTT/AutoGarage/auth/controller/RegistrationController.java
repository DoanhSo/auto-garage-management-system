package com.Group13_65CNTT.AutoGarage.auth.controller;

import com.Group13_65CNTT.AutoGarage.auth.dto.request.RegistrationRequest;
import com.Group13_65CNTT.AutoGarage.auth.service.RegistrationService;
import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.AUTH_BASE)
@RequiredArgsConstructor
@Tag(name = "Customer registration")
public class RegistrationController {
    private final RegistrationService registrationService;

    @PostMapping(ApiPaths.REGISTER)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(SecurityConstants.PERMIT_ALL)
    @Operation(summary = "Register a customer account with username, email and password")
    public ApiResponse<Void> register(
            @Valid @RequestBody RegistrationRequest request, HttpServletRequest httpRequest) {
        registrationService.register(request);
        return ApiResponse.success(ResponseCode.CREATED, null, httpRequest.getRequestURI());
    }
}
