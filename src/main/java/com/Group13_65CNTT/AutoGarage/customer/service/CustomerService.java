package com.Group13_65CNTT.AutoGarage.customer.service;

import com.Group13_65CNTT.AutoGarage.customer.dto.CreateCustomerRequest;
import com.Group13_65CNTT.AutoGarage.customer.dto.CustomerResponse;
import com.Group13_65CNTT.AutoGarage.customer.dto.UpdateCustomerRequest;

import java.util.List;

public interface CustomerService {

    CustomerResponse createCustomer(CreateCustomerRequest request);

    CustomerResponse getCustomerById(Long id);

    CustomerResponse getCustomerByCode(String code);

    List<CustomerResponse> searchCustomers(String search, Boolean isActive);

    CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request);

    void deactivateCustomer(Long id);
}
