package com.Group13_65CNTT.AutoGarage.catalog.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "services")
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_code", nullable = false, unique = true, length = 30)
    private String serviceCode;

    @Column(name = "service_name", nullable = false, length = 150)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 20)
    private ServiceType serviceType = ServiceType.SINGLE;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "unit", nullable = false, length = 30)
    private String unit = "job";

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "standard_minutes", nullable = false)
    private Integer standardMinutes;

    @Column(name = "required_bay_type", length = 30)
    private String requiredBayType;

    @Column(name = "required_skills_json", columnDefinition = "JSON")
    private String requiredSkillsJson;

    @Column(name = "interval_km")
    private Integer intervalKm;

    @Column(name = "interval_months")
    private Integer intervalMonths;

    @Column(name = "warranty_days", nullable = false)
    private Integer warrantyDays = 0;

    @Column(name = "warranty_km", nullable = false)
    private Integer warrantyKm = 0;

    @Column(name = "warranty_terms", columnDefinition = "TEXT")
    private String warrantyTerms;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.unit == null) {
            this.unit = "job";
        }
        if (this.unitPrice == null) {
            this.unitPrice = BigDecimal.ZERO;
        }
        if (this.warrantyDays == null) {
            this.warrantyDays = 0;
        }
        if (this.warrantyKm == null) {
            this.warrantyKm = 0;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.serviceType == null) {
            this.serviceType = ServiceType.SINGLE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public ServiceEntity() {
    }

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
