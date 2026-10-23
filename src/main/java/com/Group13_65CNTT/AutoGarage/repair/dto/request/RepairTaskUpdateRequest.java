package com.Group13_65CNTT.AutoGarage.repair.dto.request;

import com.Group13_65CNTT.AutoGarage.repair.entity.TaskStatus;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepairTaskUpdateRequest {

    private String taskName;

    private String description;

    @DecimalMin(value = "0.001", message = "Số lượng phải lớn hơn 0")
    private BigDecimal quantity;

    @DecimalMin(value = "0.00", message = "Đơn giá phải >= 0")
    private BigDecimal unitPrice;

    private TaskStatus status;

    private String completionNote;
}
