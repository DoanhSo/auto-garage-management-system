package com.Group13_65CNTT.AutoGarage.catalog.controller;

import com.Group13_65CNTT.AutoGarage.catalog.dto.CreateServiceRequest;
import com.Group13_65CNTT.AutoGarage.catalog.dto.ServiceResponse;
import com.Group13_65CNTT.AutoGarage.catalog.dto.UpdateServiceRequest;
import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceType;
import com.Group13_65CNTT.AutoGarage.catalog.service.CatalogService;
import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;

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
@RequestMapping(ApiPaths.SERVICES_BASE)
@RequiredArgsConstructor
@Tag(name = "Service Catalog")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class CatalogController {

    private final CatalogService catalogService;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ServiceResponse> createService(
            @Valid @RequestBody CreateServiceRequest request) {
        ServiceResponse response = catalogService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public ResponseEntity<List<ServiceResponse>> getServices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ServiceType type,
            @RequestParam(required = false) Boolean isActive) {
        List<ServiceResponse> list = catalogService.searchServices(search, type, isActive);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public ResponseEntity<ServiceResponse> getServiceById(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getServiceById(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public ResponseEntity<ServiceResponse> getServiceByCode(@PathVariable String code) {
        return ResponseEntity.ok(catalogService.getServiceByCode(code));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable Long id, @Valid @RequestBody UpdateServiceRequest request) {
        return ResponseEntity.ok(catalogService.updateService(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<Void> deactivateService(@PathVariable Long id) {
        catalogService.deactivateService(id);
        return ResponseEntity.noContent().build();
    }
}
