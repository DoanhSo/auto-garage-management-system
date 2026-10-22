package com.Group13_65CNTT.AutoGarage.common.security;

import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;

import java.io.IOException;

@RequiredArgsConstructor
public class JsonCorsProcessor extends DefaultCorsProcessor {
    private final SecurityErrorWriter errors;

    @Override
    protected void rejectRequest(ServerHttpResponse response) {
        // Delay writing until the request path is available; never commit a plain-text body.
        response.setStatusCode(HttpStatus.FORBIDDEN);
    }

    @Override
    public boolean processRequest(
            CorsConfiguration configuration,
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {
        boolean accepted = super.processRequest(configuration, request, response);
        if (!accepted) errors.write(request, response, ResponseCode.FORBIDDEN);
        return accepted;
    }
}
