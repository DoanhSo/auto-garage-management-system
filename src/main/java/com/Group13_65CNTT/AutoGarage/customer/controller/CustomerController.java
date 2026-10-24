package com.Group13_65CNTT.AutoGarage.customer.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.customer.dto.CreateCustomerRequest;
import com.Group13_65CNTT.AutoGarage.customer.dto.CustomerResponse;
import com.Group13_65CNTT.AutoGarage.customer.dto.UpdateCustomerRequest;
import com.Group13_65CNTT.AutoGarage.customer.service.CustomerService;

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
@RequestMapping(ApiPaths.CUSTOMERS_BASE)
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Customer profiles, contact details and credit limits")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_RECEPTIONIST_OR_ADMIN)
    @Operation(summary = "Create customer profile")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request,
            HttpServletRequest httpRequest) {
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.created(URI.create(ApiPaths.CUSTOMERS_BASE + "/" + response.getId()))
                .body(ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Search customers with optional filters")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getCustomers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            HttpServletRequest httpRequest) {
        List<CustomerResponse> customers = customerService.searchCustomers(search, isActive);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, customers, httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Fetch customer by ID")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Fetch customer by customer code")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerByCode(
            @PathVariable String code,
            HttpServletRequest httpRequest) {
        CustomerResponse response = customerService.getCustomerByCode(code);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_RECEPTIONIST_OR_ADMIN)
    @Operation(summary = "Update customer profile")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request,
            HttpServletRequest httpRequest) {
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_RECEPTIONIST_OR_ADMIN)
    @Operation(summary = "Deactivate customer")
    public ResponseEntity<ApiResponse<Void>> deactivateCustomer(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        customerService.deactivateCustomer(id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.DELETED, null, httpRequest.getRequestURI()));
    }
}
