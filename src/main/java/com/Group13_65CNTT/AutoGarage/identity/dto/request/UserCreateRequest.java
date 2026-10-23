package com.Group13_65CNTT.AutoGarage.identity.dto.request;

import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class UserCreateRequest {

    @Pattern(
            regexp = SecurityConstants.IDENTITY_ID_PATTERN,
            message =
                    "Identity ID must be a 1-36 character ASCII alphanumeric or hyphen identifier")
    private String identityId;

    @NotBlank(message = "Username is required")
    @Pattern(
            regexp = SecurityConstants.USERNAME_PATTERN,
            message =
                    "Username requires 3-100 ASCII letters, digits, dots, underscores or hyphens")
    private String username;

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

    @Size(max = 72, message = "Initial password must not exceed 72 characters")
    private String password;

    @Override
    public String toString() {
        return "UserCreateRequest[redacted]";
    }
}
