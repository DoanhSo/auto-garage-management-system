package com.Group13_65CNTT.AutoGarage.customer.dto;

import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sign-in account the front desk creates for an existing customer profile. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerAccountRequest {
    @NotBlank(message = "Tên đăng nhập là bắt buộc")
    @Pattern(regexp = SecurityConstants.USERNAME_PATTERN,
            message = "Tên đăng nhập 3–100 ký tự, chỉ gồm chữ, số và . _ -")
    private String username;

    /** Optional; the customer's profile email is used when left empty. */
    @Email(message = "Email không hợp lệ")
    @Size(max = SecurityConstants.EMAIL_MAX_LENGTH, message = "Email quá dài")
    private String email;

    @NotBlank(message = "Mật khẩu ban đầu là bắt buộc")
    @Size(max = 72, message = "Mật khẩu không được vượt quá 72 ký tự")
    private String password;
}
