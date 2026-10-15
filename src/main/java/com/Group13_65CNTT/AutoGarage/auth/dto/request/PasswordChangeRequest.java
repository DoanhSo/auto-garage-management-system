package com.Group13_65CNTT.AutoGarage.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class PasswordChangeRequest {

    @NotBlank(message = "Current password is required")
    @Size(max = 72, message = "Current password must not exceed 72 characters")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(max = 72, message = "Password must not exceed 72 characters")
    private String newPassword;

    @Override
    public String toString() {
        return "PasswordChangeRequest[redacted]";
    }
}
