package com.Group13_65CNTT.AutoGarage.repair.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HandoverRequest {

    @NotNull(message = "Số km khi bàn giao không được để trống")
    @Min(value = 0, message = "Số km khi bàn giao phải >= 0")
    private Integer odometerOut;

    private String notes;

    private String requestKey;
}
