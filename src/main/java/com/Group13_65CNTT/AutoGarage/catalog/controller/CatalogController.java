package com.Group13_65CNTT.AutoGarage.catalog.controller;

import com.Group13_65CNTT.AutoGarage.catalog.dto.CreateServiceRequest;
import com.Group13_65CNTT.AutoGarage.catalog.dto.ServiceResponse;
import com.Group13_65CNTT.AutoGarage.catalog.dto.UpdateServiceRequest;
import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceType;
import com.Group13_65CNTT.AutoGarage.catalog.service.CatalogService;
import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;

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
@RequestMapping(ApiPaths.SERVICES_BASE)
@RequiredArgsConstructor
@Tag(name = "Service Catalog", description = "Service packages, pricing, repair standard minutes and warranties")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class CatalogController {

    private final CatalogService catalogService;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Create service catalog item")
    public ResponseEntity<ApiResponse<ServiceResponse>> createService(
            @Valid @RequestBody CreateServiceRequest request,
            HttpServletRequest httpRequest) {
        ServiceResponse response = catalogService.createService(request);
        return ResponseEntity.created(URI.create(ApiPaths.SERVICES_BASE + "/" + response.getId()))
                .body(ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Search service catalog")
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getServices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ServiceType type,
            @RequestParam(required = false) Boolean isActive,
            HttpServletRequest httpRequest) {
        List<ServiceResponse> list = catalogService.searchServices(search, type, isActive);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, list, httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Fetch service catalog item by ID")
    public ResponseEntity<ApiResponse<ServiceResponse>> getServiceById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        ServiceResponse response = catalogService.getServiceById(id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Fetch service catalog item by service code")
    public ResponseEntity<ApiResponse<ServiceResponse>> getServiceByCode(
            @PathVariable String code,
            HttpServletRequest httpRequest) {
        ServiceResponse response = catalogService.getServiceByCode(code);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Update service catalog item")
    public ResponseEntity<ApiResponse<ServiceResponse>> updateService(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequest request,
            HttpServletRequest httpRequest) {
        ServiceResponse response = catalogService.updateService(id, request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Deactivate service catalog item")
    public ResponseEntity<ApiResponse<Void>> deactivateService(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        catalogService.deactivateService(id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.DELETED, null, httpRequest.getRequestURI()));
    }
}
