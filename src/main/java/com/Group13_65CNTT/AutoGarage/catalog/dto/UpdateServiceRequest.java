package com.Group13_65CNTT.AutoGarage.catalog.dto;

import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class UpdateServiceRequest {

    @NotBlank(message = "Tên dịch vụ không được để trống")
    @Size(max = 150, message = "Tên dịch vụ tối đa 150 ký tự")
    private String serviceName;

    private ServiceType serviceType;

    private String description;

    @Size(max = 30, message = "Đơn vị tính tối đa 30 ký tự")
    private String unit;

    @DecimalMin(value = "0.0", message = "Đơn giá không được âm")
    private BigDecimal unitPrice;

    @Min(value = 1, message = "Thời gian định mức phải lớn hơn 0 phút")
    private Integer standardMinutes;

    @Size(max = 30, message = "Loại khoang tối đa 30 ký tự")
    private String requiredBayType;

    private String requiredSkillsJson;

    @Min(value = 1, message = "Chu kỳ km phải lớn hơn 0")
    private Integer intervalKm;

    @Min(value = 1, message = "Chu kỳ tháng phải lớn hơn 0")
    private Integer intervalMonths;

    @Min(value = 0, message = "Số ngày bảo hành không được âm")
    private Integer warrantyDays;

    @Min(value = 0, message = "Số km bảo hành không được âm")
    private Integer warrantyKm;

    private String warrantyTerms;

    private Boolean isActive;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getStandardMinutes() {
        return standardMinutes;
    }

    public void setStandardMinutes(Integer standardMinutes) {
        this.standardMinutes = standardMinutes;
    }

    public String getRequiredBayType() {
        return requiredBayType;
    }

    public void setRequiredBayType(String requiredBayType) {
        this.requiredBayType = requiredBayType;
    }

    public String getRequiredSkillsJson() {
        return requiredSkillsJson;
    }

    public void setRequiredSkillsJson(String requiredSkillsJson) {
        this.requiredSkillsJson = requiredSkillsJson;
    }

    public Integer getIntervalKm() {
        return intervalKm;
    }

    public void setIntervalKm(Integer intervalKm) {
        this.intervalKm = intervalKm;
    }

    public Integer getIntervalMonths() {
        return intervalMonths;
    }

    public void setIntervalMonths(Integer intervalMonths) {
        this.intervalMonths = intervalMonths;
    }

    public Integer getWarrantyDays() {
        return warrantyDays;
    }

    public void setWarrantyDays(Integer warrantyDays) {
        this.warrantyDays = warrantyDays;
    }

    public Integer getWarrantyKm() {
        return warrantyKm;
    }

    public void setWarrantyKm(Integer warrantyKm) {
        this.warrantyKm = warrantyKm;
    }

    public String getWarrantyTerms() {
        return warrantyTerms;
    }

    public void setWarrantyTerms(String warrantyTerms) {
        this.warrantyTerms = warrantyTerms;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
