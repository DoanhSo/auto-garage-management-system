package com.Group13_65CNTT.AutoGarage.estimate.dto.response;

import com.Group13_65CNTT.AutoGarage.estimate.entity.ApprovalStatus;
import com.Group13_65CNTT.AutoGarage.estimate.entity.ItemType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstimateItemResponse {

    private Long id;
    private Long estimateId;
    private Integer lineNo;
    private ItemType itemType;
    private Long serviceId;
    private Long partId;
    private String itemName;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal unitCost;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal lineTotal;
    private ApprovalStatus approvalStatus;
    private Long approvedBy;
    private Instant decidedAt;
    private String decisionNote;
    private BigDecimal reservedQuantity;
    private Integer warrantyDays;
    private Integer warrantyKm;
    private String warrantyTerms;
    private Instant createdAt;
    private Long version;
}
