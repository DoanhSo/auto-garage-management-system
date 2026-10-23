package com.Group13_65CNTT.AutoGarage.repair.dto.request;

import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepairOrderStatusUpdateRequest {

    @NotNull(message = "Trạng thái mới không được để trống")
    private RepairOrderStatus newStatus;

    private String note;
}
