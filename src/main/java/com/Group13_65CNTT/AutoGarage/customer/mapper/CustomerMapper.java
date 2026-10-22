package com.Group13_65CNTT.AutoGarage.customer.mapper;

import com.Group13_65CNTT.AutoGarage.customer.dto.CreateCustomerRequest;
import com.Group13_65CNTT.AutoGarage.customer.dto.CustomerResponse;
import com.Group13_65CNTT.AutoGarage.customer.dto.UpdateCustomerRequest;
import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toEntity(CreateCustomerRequest request) {
        if (request == null) {
            return null;
        }
        Customer customer = new Customer();
        customer.setUserId(request.getUserId());
        customer.setCustomerCode(request.getCustomerCode());
        customer.setFullName(request.getFullName().trim());
        customer.setCustomerType(request.getCustomerType());
        customer.setPhone(request.getPhone().trim());
        customer.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        customer.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        customer.setCompanyName(request.getCompanyName() != null ? request.getCompanyName().trim() : null);
        customer.setTaxCode(request.getTaxCode() != null ? request.getTaxCode().trim() : null);
        customer.setCreditLimit(request.getCreditLimit());
        customer.setPaymentTermDays(request.getPaymentTermDays());
        return customer;
    }

    public void updateEntity(Customer customer, UpdateCustomerRequest request) {
        if (customer == null || request == null) {
            return;
        }
        customer.setFullName(request.getFullName().trim());
        if (request.getCustomerType() != null) {
            customer.setCustomerType(request.getCustomerType());
        }
        customer.setPhone(request.getPhone().trim());
        customer.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        customer.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        customer.setCompanyName(request.getCompanyName() != null ? request.getCompanyName().trim() : null);
        customer.setTaxCode(request.getTaxCode() != null ? request.getTaxCode().trim() : null);
        if (request.getCreditLimit() != null) {
            customer.setCreditLimit(request.getCreditLimit());
        }
        if (request.getPaymentTermDays() != null) {
            customer.setPaymentTermDays(request.getPaymentTermDays());
        }
        if (request.getIsActive() != null) {
            customer.setIsActive(request.getIsActive());
        }
    }

    public CustomerResponse toResponse(Customer customer) {
        if (customer == null) {
            return null;
        }
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setUserId(customer.getUserId());
        response.setCustomerCode(customer.getCustomerCode());
        response.setFullName(customer.getFullName());
        response.setCustomerType(customer.getCustomerType());
        response.setPhone(customer.getPhone());
        response.setEmail(customer.getEmail());
        response.setAddress(customer.getAddress());
        response.setCompanyName(customer.getCompanyName());
        response.setTaxCode(customer.getTaxCode());
        response.setCreditLimit(customer.getCreditLimit());
        response.setPaymentTermDays(customer.getPaymentTermDays());
        response.setIsActive(customer.getIsActive());
        response.setCreatedAt(customer.getCreatedAt());
        response.setUpdatedAt(customer.getUpdatedAt());
        response.setVersion(customer.getVersion());
        return response;
    }
}
