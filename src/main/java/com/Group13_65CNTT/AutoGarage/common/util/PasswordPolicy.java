package com.Group13_65CNTT.AutoGarage.common.util;

import com.Group13_65CNTT.AutoGarage.common.config.PasswordProperties;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class PasswordPolicy {
    // BCrypt's byte limit is an algorithm constraint, not an environment policy.
    public static final int BCRYPT_MAX_BYTES = 72;
    private final PasswordProperties properties;

    public void validateNewPassword(String password) {
        if (password == null
                || password.isBlank()
                || password.codePointCount(0, password.length()) < properties.getMinLength()
                || !withinByteLimit(password)) {
            throw new BusinessException(ResponseCode.INVALID_PASSWORD);
        }
    }

    public static boolean withinByteLimit(String password) {
        return password != null
                && password.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }
}
