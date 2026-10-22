package com.Group13_65CNTT.AutoGarage.catalog.dto;

import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceType;

import java.math.BigDecimal;
import java.time.Instant;

public class ServiceResponse {

    private Long id;
    private String serviceCode;
    private String serviceName;
    private ServiceType serviceType;
    private String description;
    private String unit;
    private BigDecimal unitPrice;
    private Integer standardMinutes;
    private String requiredBayType;
    private String requiredSkillsJson;
    private Integer intervalKm;
    private Integer intervalMonths;
    private Integer warrantyDays;
    private Integer warrantyKm;
    private String warrantyTerms;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
