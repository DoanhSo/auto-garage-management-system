package com.Group13_65CNTT.AutoGarage.auth.dto.response;

import com.Group13_65CNTT.AutoGarage.identity.dto.response.UserResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private Instant expiresAt;
    private UserResponse user;

    @Override
    public String toString() {
        return "LoginResponse[token=redacted]";
    }
}
