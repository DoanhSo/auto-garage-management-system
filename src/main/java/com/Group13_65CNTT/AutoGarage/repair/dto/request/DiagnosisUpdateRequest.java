package com.Group13_65CNTT.AutoGarage.repair.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosisUpdateRequest {

    @NotBlank(message = "Kết quả chẩn đoán không được để trống")
    private String diagnosis;
}
