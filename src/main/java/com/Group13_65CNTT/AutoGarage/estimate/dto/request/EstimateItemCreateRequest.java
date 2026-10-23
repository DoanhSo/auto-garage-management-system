package com.Group13_65CNTT.AutoGarage.estimate.dto.request;

import com.Group13_65CNTT.AutoGarage.estimate.entity.ItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstimateItemCreateRequest {

    @NotNull(message = "Loại hạng mục (SERVICE hoặc PART) không được để trống")
    private ItemType itemType;

    private Long serviceId;

    private Long partId;

    private String itemName;

    private String unit;

    @NotNull(message = "Số lượng không được để trống")
    @DecimalMin(value = "0.001", message = "Số lượng phải lớn hơn 0")
    private BigDecimal quantity;

    @DecimalMin(value = "0.00", message = "Đơn giá phải >= 0")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.00", message = "Số tiền giảm giá phải >= 0")
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Tiền thuế phải >= 0")
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;
}
