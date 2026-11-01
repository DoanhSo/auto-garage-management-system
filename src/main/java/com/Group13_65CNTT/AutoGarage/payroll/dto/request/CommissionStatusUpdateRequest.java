package com.Group13_65CNTT.AutoGarage.payroll.dto.request;

import com.Group13_65CNTT.AutoGarage.payroll.entity.CommissionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionStatusUpdateRequest {

    @NotNull(message = "Trạng thái là bắt buộc")
    private CommissionStatus status;

    @Size(max = 2000, message = "Ghi chú không vượt quá 2000 ký tự")
    private String notes;
}
