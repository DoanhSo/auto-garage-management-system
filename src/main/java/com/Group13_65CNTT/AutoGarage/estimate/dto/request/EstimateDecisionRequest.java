package com.Group13_65CNTT.AutoGarage.estimate.dto.request;

import com.Group13_65CNTT.AutoGarage.shared.contracts.EstimateApproval;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstimateDecisionRequest {

    @NotEmpty(message = "Danh sách quyết định hạng mục không được để trống")
    private List<EstimateApproval.LineDecision> lines;

    private String requestKey;
}
