package com.Group13_65CNTT.AutoGarage.auth.dto.request;

import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.util.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationRequest {
    @NotBlank(message = "Full name is required")
    @Size(max = SecurityConstants.FULL_NAME_MAX_LENGTH, message = "Full name is too long")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = SecurityConstants.EMAIL_MAX_LENGTH, message = "Email is too long")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(max = PasswordPolicy.BCRYPT_MAX_BYTES, message = "Password is too long")
    private String password;
}
