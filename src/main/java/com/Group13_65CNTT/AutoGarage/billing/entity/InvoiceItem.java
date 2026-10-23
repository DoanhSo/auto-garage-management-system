package com.Group13_65CNTT.AutoGarage.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "item_type", nullable = false, length = 10)
    private String itemType;

    @Column(name = "repair_task_id")
    private Long repairTaskId;

    @Column(name = "repair_order_part_id")
    private Long repairOrderPartId;

    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    @Column(name = "unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "unit_cost", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "warranty_days", nullable = false)
    @Builder.Default
    private Integer warrantyDays = 0;

    @Column(name = "warranty_km", nullable = false)
    @Builder.Default
    private Integer warrantyKm = 0;

    @Column(name = "warranty_terms", columnDefinition = "TEXT")
    private String warrantyTerms;
}
