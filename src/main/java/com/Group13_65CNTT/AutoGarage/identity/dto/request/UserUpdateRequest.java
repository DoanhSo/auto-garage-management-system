package com.Group13_65CNTT.AutoGarage.identity.dto.request;

import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;

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
public class UserUpdateRequest {

    @Email(message = "Email must be valid")
    @Size(
            max = SecurityConstants.EMAIL_MAX_LENGTH,
            message = "Email must not exceed 254 characters")
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(
            max = SecurityConstants.FULL_NAME_MAX_LENGTH,
            message = "Full name must not exceed 150 characters")
    private String fullName;

    @NotNull(message = "Role is required")
    private Role role;

    @NotNull(message = "Active status is required")
    private Boolean active;

    @NotNull(message = "Version is required")
    @Min(value = 0, message = "Version must be non-negative")
    private Long version;

    @Override
    public String toString() {
        return "UserUpdateRequest[redacted]";
    }
}
