package com.Group13_65CNTT.AutoGarage.common.dto;

import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private final int code;
    private final String message;

    /** Authored, user-safe explanation of a business-rule failure; absent for generic errors. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String detail;

    private final boolean success;
    private final T data;
    private final Instant timestamp;
    private final String path;

    public static <T> ApiResponse<T> success(ResponseCode code, T data, String path) {
        return new ApiResponse<>(
                code.getCode(), code.getMessage(), null, true, data, Instant.now(), path);
    }

    public static <T> ApiResponse<T> error(ResponseCode code, T data, String path) {
        return error(code, null, data, path);
    }

    public static <T> ApiResponse<T> error(ResponseCode code, String detail, T data, String path) {
        return new ApiResponse<>(
                code.getCode(), code.getMessage(), detail, false, data, Instant.now(), path);
    }
}
