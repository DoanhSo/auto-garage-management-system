package com.Group13_65CNTT.AutoGarage.estimate.dto.request;

import com.Group13_65CNTT.AutoGarage.estimate.entity.EstimateKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstimateCreateRequest {

    @NotNull(message = "ID phiếu sửa chữa không được để trống")
    private Long repairOrderId;

    @Builder.Default
    private EstimateKind estimateKind = EstimateKind.INITIAL;

    private Long parentEstimateId;

    private Instant validUntil;

    private String notes;

    @NotEmpty(message = "Báo giá phải có ít nhất một hạng mục")
    @Valid
    private List<EstimateItemCreateRequest> items;
}
