package com.Group13_65CNTT.AutoGarage.auth.service;

public interface BrowserSessionService {
    void validateOrigin(String origin);
    BrowserSessionTokens open(String refreshToken, String expectedSubject);
    BrowserSessionTokens refresh(String refreshToken);
    void close(String refreshToken);
}
