package com.Group13_65CNTT.AutoGarage.customer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Moves the sign-in account of a duplicate profile (usually created by self-registration) onto this one. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LinkCustomerAccountRequest {
    @NotNull(message = "Hãy chọn hồ sơ đang giữ tài khoản")
    @Positive
    private Long sourceCustomerId;
}
