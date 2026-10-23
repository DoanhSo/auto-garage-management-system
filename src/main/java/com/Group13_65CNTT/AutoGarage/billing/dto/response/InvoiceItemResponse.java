package com.Group13_65CNTT.AutoGarage.billing.dto.response;

import com.Group13_65CNTT.AutoGarage.billing.entity.InvoiceItemType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemResponse {
    private Long id;
    private Long invoiceId;
    private Integer lineNo;
    private InvoiceItemType itemType;
    private Long repairTaskId;
    private Long repairOrderPartId;
    private String itemName;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal unitCost;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal lineTotal;
    private Integer warrantyDays;
    private Integer warrantyKm;
    private String warrantyTerms;
}
