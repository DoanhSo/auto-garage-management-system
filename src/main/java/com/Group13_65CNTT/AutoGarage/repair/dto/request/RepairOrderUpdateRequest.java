package com.Group13_65CNTT.AutoGarage.repair.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepairOrderUpdateRequest {

    @DecimalMin(value = "0.00", message = "Mức nhiên liệu phải từ 0% đến 100%")
    @DecimalMax(value = "100.00", message = "Mức nhiên liệu phải từ 0% đến 100%")
    private BigDecimal fuelLevelPercent;

    private String customerComplaint;

    private String diagnosis;
}
