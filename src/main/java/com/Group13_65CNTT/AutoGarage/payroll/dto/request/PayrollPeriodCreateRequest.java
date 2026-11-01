package com.Group13_65CNTT.AutoGarage.payroll.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPeriodCreateRequest {

    @NotBlank(message = "Tháng tính lương là bắt buộc")
    @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "Tháng tính lương phải có dạng YYYY-MM")
    private String periodMonth;

    private LocalDate fromDate;

    private LocalDate toDate;

    @Size(max = 2000, message = "Ghi chú không vượt quá 2000 ký tự")
    private String notes;
}
