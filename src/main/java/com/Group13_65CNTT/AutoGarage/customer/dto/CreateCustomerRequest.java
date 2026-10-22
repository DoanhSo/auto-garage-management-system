package com.Group13_65CNTT.AutoGarage.customer.dto;

import com.Group13_65CNTT.AutoGarage.customer.entity.CustomerType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateCustomerRequest {

    private Long userId;

    private String customerCode;

    @NotBlank(message = "Tên khách hàng không được để trống")
    @Size(max = 150, message = "Tên khách hàng tối đa 150 ký tự")
    private String fullName;

    private CustomerType customerType = CustomerType.INDIVIDUAL;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 30, message = "Số điện thoại tối đa 30 ký tự")
    private String phone;

    @Size(max = 254, message = "Email tối đa 254 ký tự")
    private String email;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String address;

    @Size(max = 255, message = "Tên công ty tối đa 255 ký tự")
    private String companyName;

    @Size(max = 30, message = "Mã số thuế tối đa 30 ký tự")
    private String taxCode;

    @DecimalMin(value = "0.0", message = "Hạn mức nợ không được âm")
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Min(value = 0, message = "Thời hạn nợ không được âm")
    private Integer paymentTermDays = 0;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    public void setCustomerType(CustomerType customerType) {
        this.customerType = customerType;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public Integer getPaymentTermDays() {
        return paymentTermDays;
    }

    public void setPaymentTermDays(Integer paymentTermDays) {
        this.paymentTermDays = paymentTermDays;
    }
}
