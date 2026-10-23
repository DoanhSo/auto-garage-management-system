package com.Group13_65CNTT.AutoGarage.inventory.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class SupplierUpdateRequest {
    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    @Size(max = 150, message = "Tên nhà cung cấp tối đa 150 ký tự")
    private String supplierName;

    @Size(max = 30, message = "Số điện thoại tối đa 30 ký tự")
    private String phone;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 254, message = "Email tối đa 254 ký tự")
    private String email;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String address;

    @Size(max = 30, message = "Mã số thuế tối đa 30 ký tự")
    private String taxCode;

    @Min(value = 0, message = "Hạn thanh toán không được âm")
    @Builder.Default
    private Integer paymentTermDays = 0;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    @Builder.Default
    private Boolean isActive = true;

    @NotNull(message = "Phiên bản dữ liệu (version) bắt buộc để kiểm tra xung đột")
    private Long version;
}
