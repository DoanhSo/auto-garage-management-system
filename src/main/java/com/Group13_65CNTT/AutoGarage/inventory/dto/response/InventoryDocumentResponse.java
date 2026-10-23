package com.Group13_65CNTT.AutoGarage.inventory.dto.response;

import com.Group13_65CNTT.AutoGarage.inventory.entity.DocumentStatus;
import com.Group13_65CNTT.AutoGarage.inventory.entity.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDocumentResponse {
    private Long id;
    private String documentCode;
    private DocumentType documentType;
    private DocumentStatus status;
    private Long supplierId;
    private String supplierName;
    private Long repairOrderId;
    private Long sourceDocumentId;
    private String sourceDocumentCode;
    private String supplierInvoiceNumber;
    private LocalDate supplierInvoiceDate;
    private LocalDate dueDate;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String reason;
    private Long createdBy;
    private Long postedBy;
    private Instant postedAt;
    private Instant createdAt;
    private Long version;
    private List<InventoryDocumentItemResponse> items;
}
