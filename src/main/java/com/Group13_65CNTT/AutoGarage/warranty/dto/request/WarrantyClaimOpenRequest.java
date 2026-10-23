package com.Group13_65CNTT.AutoGarage.warranty.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyClaimOpenRequest {

    @NotNull(message = "ID bảo hành không được để trống")
    private Long warrantyId;

    @NotNull(message = "Số km hiện tại không được để trống")
    @Min(value = 0, message = "Số km hiện tại phải >= 0")
    private Integer currentOdometer;

    @NotBlank(message = "Mô tả hiện tượng hư hỏng không được để trống")
    private String description;

    private String requestKey;
}
