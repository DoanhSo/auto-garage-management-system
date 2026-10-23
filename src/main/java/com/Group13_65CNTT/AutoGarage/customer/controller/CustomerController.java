package com.Group13_65CNTT.AutoGarage.customer.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.customer.dto.CreateCustomerRequest;
import com.Group13_65CNTT.AutoGarage.customer.dto.CustomerResponse;
import com.Group13_65CNTT.AutoGarage.customer.dto.UpdateCustomerRequest;
import com.Group13_65CNTT.AutoGarage.customer.service.CustomerService;

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
@RequestMapping(ApiPaths.CUSTOMERS_BASE)
@RequiredArgsConstructor
@Tag(name = "Customers")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    public ResponseEntity<List<CustomerResponse>> getCustomers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive) {
        List<CustomerResponse> customers = customerService.searchCustomers(search, isActive);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_BOOKING_ACCESS)
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    public ResponseEntity<CustomerResponse> getCustomerByCode(@PathVariable String code) {
        return ResponseEntity.ok(customerService.getCustomerByCode(code));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_BOOKING_ACCESS)
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id, @Valid @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    public ResponseEntity<Void> deactivateCustomer(@PathVariable Long id) {
        customerService.deactivateCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
