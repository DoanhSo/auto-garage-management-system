package com.Group13_65CNTT.AutoGarage.customer.service;

import com.Group13_65CNTT.AutoGarage.customer.dto.CreateCustomerRequest;
import com.Group13_65CNTT.AutoGarage.customer.dto.CustomerResponse;
import com.Group13_65CNTT.AutoGarage.customer.dto.UpdateCustomerRequest;

import java.util.List;

public interface CustomerService {

    CustomerResponse createCustomer(CreateCustomerRequest request);

    CustomerResponse getCustomerById(Long id);

    CustomerResponse getCurrentCustomer();

    CustomerResponse getCustomerByCode(String code);

    List<CustomerResponse> searchCustomers(String search, Boolean isActive, Boolean hasAccount);

    /** Front desk creates a sign-in account for an existing profile that has none. */
    CustomerResponse createAccount(Long id, com.Group13_65CNTT.AutoGarage.customer.dto.CreateCustomerAccountRequest request);

    /** Moves the account of a duplicate profile without business data onto this profile. */
    CustomerResponse linkAccount(Long id, com.Group13_65CNTT.AutoGarage.customer.dto.LinkCustomerAccountRequest request);

    CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request);

    void deactivateCustomer(Long id);

    CustomerResponse activateCustomer(Long id);
}
