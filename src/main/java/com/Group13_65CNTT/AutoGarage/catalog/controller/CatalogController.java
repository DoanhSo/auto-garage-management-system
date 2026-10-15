package com.Group13_65CNTT.AutoGarage.catalog.controller;

import com.Group13_65CNTT.AutoGarage.catalog.dto.CreateServiceRequest;
import com.Group13_65CNTT.AutoGarage.catalog.dto.ServiceResponse;
import com.Group13_65CNTT.AutoGarage.catalog.dto.UpdateServiceRequest;
import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceType;
import com.Group13_65CNTT.AutoGarage.catalog.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping
    public ResponseEntity<ServiceResponse> createService(@Valid @RequestBody CreateServiceRequest request) {
        ServiceResponse response = catalogService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ServiceResponse>> getServices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ServiceType type,
            @RequestParam(required = false) Boolean isActive) {
        List<ServiceResponse> list = catalogService.searchServices(search, type, isActive);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> getServiceById(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getServiceById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ServiceResponse> getServiceByCode(@PathVariable String code) {
        return ResponseEntity.ok(catalogService.getServiceByCode(code));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequest request) {
        return ResponseEntity.ok(catalogService.updateService(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateService(@PathVariable Long id) {
        catalogService.deactivateService(id);
        return ResponseEntity.noContent().build();
    }
}
