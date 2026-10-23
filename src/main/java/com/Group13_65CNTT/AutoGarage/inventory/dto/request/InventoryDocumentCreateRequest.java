package com.Group13_65CNTT.AutoGarage.inventory.dto.request;

import com.Group13_65CNTT.AutoGarage.inventory.entity.DocumentType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDocumentCreateRequest {
    @NotNull(message = "Loại chứng từ không được để trống")
    private DocumentType documentType;

    private Long supplierId;

    private Long repairOrderId;

    private Long sourceDocumentId;

    @Size(max = 100, message = "Số hóa đơn NCC tối đa 100 ký tự")
    private String supplierInvoiceNumber;

    private LocalDate supplierInvoiceDate;

    private LocalDate dueDate;

    @DecimalMin(value = "0.0", inclusive = true, message = "Tiền thuế không được âm")
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    private String reason;

    @NotEmpty(message = "Chứng từ kho phải có ít nhất một dòng chi tiết")
    @Valid
    private List<InventoryDocumentItemRequest> items;
}
