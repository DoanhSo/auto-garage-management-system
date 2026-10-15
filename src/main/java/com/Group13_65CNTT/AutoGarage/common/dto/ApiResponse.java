package com.Group13_65CNTT.AutoGarage.common.dto;

import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private final int code;
    private final String message;
    private final boolean success;
    private final T data;
    private final Instant timestamp;
    private final String path;

    public static <T> ApiResponse<T> success(ResponseCode code, T data, String path) {
        return new ApiResponse<>(
                code.getCode(), code.getMessage(), true, data, Instant.now(), path);
    }

    public static <T> ApiResponse<T> error(ResponseCode code, T data, String path) {
        return new ApiResponse<>(
                code.getCode(), code.getMessage(), false, data, Instant.now(), path);
    }
}
