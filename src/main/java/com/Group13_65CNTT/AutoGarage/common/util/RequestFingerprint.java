package com.Group13_65CNTT.AutoGarage.common.util;

import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Canonical payload fingerprint for the I08 SHA-256 replay contract. */
public final class RequestFingerprint {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private RequestFingerprint() {}

    public static String of(Object... fields) {
        try {
            byte[] payload = JSON.writeValueAsString(fields).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (NoSuchAlgorithmException failure) {
            throw new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR, failure);
        }
    }

    public static String resultField(String result, String field) {
        String value = JSON.readTree(result).path(field).asText();
        if (value == null || value.isBlank()) {
            throw new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR);
        }
        return value;
    }
}
