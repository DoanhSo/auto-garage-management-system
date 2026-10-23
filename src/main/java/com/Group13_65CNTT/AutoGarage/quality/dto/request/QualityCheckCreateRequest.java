package com.Group13_65CNTT.AutoGarage.quality.dto.request;

import com.Group13_65CNTT.AutoGarage.reception.entity.CheckResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityCheckCreateRequest {

    @NotNull(message = "ID phiếu sửa chữa không được để trống")
    private Long repairOrderId;

    @NotNull(message = "Kết quả kiểm tra (PASSED hoặc FAILED) không được để trống")
    private CheckResult result;

    @NotBlank(message = "Nội dung checklist JSON không được để trống")
    @Builder.Default
    private String checklistJson = "{}";

    private Integer odometer;

    private String notes;

    private List<Long> reworkTaskIds;
}
