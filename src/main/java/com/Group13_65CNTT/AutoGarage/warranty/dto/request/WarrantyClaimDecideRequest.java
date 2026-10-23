package com.Group13_65CNTT.AutoGarage.warranty.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyClaimDecideRequest {

    @NotNull(message = "Quyết định phê duyệt (true/false) không được để trống")
    private Boolean approved;

    @NotBlank(message = "Lý do phê duyệt/từ chối không được để trống")
    private String reason;

    private String requestKey;
}
