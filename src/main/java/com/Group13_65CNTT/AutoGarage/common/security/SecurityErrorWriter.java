package com.Group13_65CNTT.AutoGarage.common.security;

import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityErrorWriter {
    private final ObjectMapper objectMapper;

    public void write(HttpServletRequest request, HttpServletResponse response, ResponseCode code)
            throws IOException {
        response.setStatus(code.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(SecurityConstants.UTF_8_ENCODING);
        objectMapper.writeValue(
                response.getOutputStream(), ApiResponse.error(code, null, request.getRequestURI()));
    }
}
