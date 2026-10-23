package com.Group13_65CNTT.AutoGarage.repair.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepairOrderCreateRequest {

    private Long bookingId;

    @NotNull(message = "ID khách hàng không được để trống")
    private Long customerId;

    @NotNull(message = "ID xe không được để trống")
    private Long vehicleId;

    @NotNull(message = "Số km vào xưởng không được để trống")
    @Min(value = 0, message = "Số km khi vào xưởng phải >= 0")
    private Integer odometerIn;

    @DecimalMin(value = "0.00", message = "Mức nhiên liệu phải từ 0% đến 100%")
    @DecimalMax(value = "100.00", message = "Mức nhiên liệu phải từ 0% đến 100%")
    private BigDecimal fuelLevelPercent;

    private String customerComplaint;
}
