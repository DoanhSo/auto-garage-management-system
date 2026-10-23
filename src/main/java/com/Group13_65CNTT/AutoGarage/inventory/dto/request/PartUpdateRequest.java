package com.Group13_65CNTT.AutoGarage.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
public class PartUpdateRequest {
    @NotBlank(message = "Tên phụ tùng không được để trống")
    @Size(max = 150, message = "Tên phụ tùng tối đa 150 ký tự")
    private String partName;

    @NotBlank(message = "Đơn vị tính không được để trống")
    @Size(max = 30, message = "Đơn vị tính tối đa 30 ký tự")
    private String unit;

    private String description;

    private Long preferredSupplierId;

    @NotNull(message = "Giá mua không được null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá mua không được âm")
    @Builder.Default
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @NotNull(message = "Giá bán không được null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá bán không được âm")
    @Builder.Default
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    @NotNull(message = "Mức tồn tối thiểu không được null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Mức tồn tối thiểu không được âm")
    @Builder.Default
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Min(value = 0, message = "Thời hạn bảo hành (ngày) không được âm")
    @Builder.Default
    private Integer warrantyDays = 0;

    @Min(value = 0, message = "Thời hạn bảo hành (km) không được âm")
    @Builder.Default
    private Integer warrantyKm = 0;

    private String warrantyTerms;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    @Builder.Default
    private Boolean isActive = true;

    @NotNull(message = "Phiên bản dữ liệu (version) bắt buộc để kiểm tra xung đột")
    private Long version;
}
