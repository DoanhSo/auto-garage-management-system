package com.Group13_65CNTT.AutoGarage.auth.service;

import com.Group13_65CNTT.AutoGarage.auth.dto.response.BrowserSessionResponse;

/** Internal token exchange result. Never serialize this type in an API response. */
public record BrowserSessionTokens(String accessToken, String refreshToken, long expiresIn) {
    public BrowserSessionResponse response() {
        return new BrowserSessionResponse(accessToken, expiresIn, "Bearer");
    }
    @Override
    public String toString() { return "BrowserSessionTokens[redacted]"; }
}
