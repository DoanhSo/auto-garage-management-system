package com.Group13_65CNTT.AutoGarage.auth.dto.response;

public record BrowserSessionResponse(String accessToken, long expiresIn, String tokenType) {
    @Override
    public String toString() { return "BrowserSessionResponse[redacted]"; }
}
