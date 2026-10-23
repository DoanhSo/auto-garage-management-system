package com.Group13_65CNTT.AutoGarage.repair.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepairTaskCreateRequest {

    private Long serviceId;

    @NotBlank(message = "Tên công việc không được để trống")
    private String taskName;

    private String description;

    @NotNull(message = "Số lượng không được để trống")
    @DecimalMin(value = "0.001", message = "Số lượng phải lớn hơn 0")
    private BigDecimal quantity;

    @NotNull(message = "Đơn giá không được để trống")
    @DecimalMin(value = "0.00", message = "Đơn giá phải >= 0")
    private BigDecimal unitPrice;
}
