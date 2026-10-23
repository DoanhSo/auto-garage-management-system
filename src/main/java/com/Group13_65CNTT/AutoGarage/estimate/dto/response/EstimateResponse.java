package com.Group13_65CNTT.AutoGarage.estimate.dto.response;

import com.Group13_65CNTT.AutoGarage.estimate.entity.EstimateKind;
import com.Group13_65CNTT.AutoGarage.estimate.entity.EstimateStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstimateResponse {

    private Long id;
    private String estimateCode;
    private Long repairOrderId;
    private Integer versionNo;
    private EstimateKind estimateKind;
    private Long parentEstimateId;
    private EstimateStatus status;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String notes;
    private Long createdBy;
    private Instant sentAt;
    private Instant validUntil;
    private Instant createdAt;
    private Long version;
    private List<EstimateItemResponse> items;
}
