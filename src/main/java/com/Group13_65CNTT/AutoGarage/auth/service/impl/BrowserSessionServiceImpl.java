package com.Group13_65CNTT.AutoGarage.auth.service.impl;

import com.Group13_65CNTT.AutoGarage.auth.service.BrowserSessionService;
import com.Group13_65CNTT.AutoGarage.auth.service.BrowserSessionTokens;
import com.Group13_65CNTT.AutoGarage.common.config.AuthProperties;
import com.Group13_65CNTT.AutoGarage.common.config.BrowserSessionProperties;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.net.http.HttpClient;
import java.util.Map;

@Service
@Slf4j
public class BrowserSessionServiceImpl implements BrowserSessionService {
    private static final ParameterizedTypeReference<Map<String, Object>> TOKEN_TYPE = new ParameterizedTypeReference<>() {};
    private final BrowserSessionProperties properties;
    private final AuthProperties authProperties;
    private final JwtDecoder decoder;
    private final RestClient client;

    public BrowserSessionServiceImpl(BrowserSessionProperties properties, AuthProperties authProperties, JwtDecoder decoder) {
        this.properties = properties;
        this.authProperties = authProperties;
        this.decoder = decoder;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(properties.requestTimeout()).build());
        factory.setReadTimeout(properties.requestTimeout());
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    @PreAuthorize(SecurityConstants.PERMIT_ALL)
    public void validateOrigin(String origin) {
        // Required even for same-origin requests: CORS alone does not prevent cookie CSRF.
        if (origin == null || authProperties.allowedOrigins() == null || !authProperties.allowedOrigins().contains(origin)) {
            throw new BusinessException(ResponseCode.FORBIDDEN);
        }
    }

    @Override
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public BrowserSessionTokens open(String refreshToken, String expectedSubject) {
        if (expectedSubject == null) throw new BusinessException(ResponseCode.INVALID_TOKEN);
        BrowserSessionTokens tokens = refresh(refreshToken);
        if (!expectedSubject.equals(subject(tokens.accessToken()))) {
            close(tokens.refreshToken());
            throw new BusinessException(ResponseCode.INVALID_TOKEN);
        }
        return tokens;
    }

    @Override
    @PreAuthorize(SecurityConstants.PERMIT_ALL)
    public BrowserSessionTokens refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) throw new BusinessException(ResponseCode.INVALID_TOKEN);
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", properties.clientId());
        form.add("refresh_token", refreshToken);
        try {
            var payload = client.post().uri(properties.tokenUri()).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(TOKEN_TYPE);
            if (payload == null || !(payload.get("access_token") instanceof String access) || access.isBlank()
                    || !(payload.get("refresh_token") instanceof String nextRefresh) || nextRefresh.isBlank()
                    || !(payload.get("expires_in") instanceof Number seconds) || seconds.longValue() <= 0
                    || !"Bearer".equalsIgnoreCase(String.valueOf(payload.get("token_type")))) {
                throw new BusinessException(ResponseCode.AUTHENTICATION_UNAVAILABLE);
            }
            subject(access); // Includes issuer, audience, expiry and account revocation validation.
            return new BrowserSessionTokens(access, nextRefresh, seconds.longValue());
        } catch (RestClientResponseException failure) {
            if (failure.getStatusCode().value() == 400 || failure.getStatusCode().value() == 401)
                throw new BusinessException(ResponseCode.INVALID_TOKEN, failure);
            throw new BusinessException(ResponseCode.AUTHENTICATION_UNAVAILABLE, failure);
        } catch (RestClientException failure) {
            throw new BusinessException(ResponseCode.AUTHENTICATION_UNAVAILABLE, failure);
        }
    }

    private String subject(String accessToken) {
        try {
            String subject = decoder.decode(accessToken).getSubject();
            if (subject == null || subject.isBlank()) throw new BusinessException(ResponseCode.INVALID_TOKEN);
            return subject;
        } catch (JwtException invalid) {
            throw new BusinessException(ResponseCode.INVALID_TOKEN, invalid);
        }
    }

    @Override
    @PreAuthorize(SecurityConstants.PERMIT_ALL)
    public void close(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        var form = new LinkedMultiValueMap<String, String>();
        form.add("client_id", properties.clientId());
        form.add("refresh_token", refreshToken);
        try {
            client.post().uri(properties.tokenUri().resolve("logout")).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().toBodilessEntity();
        } catch (RestClientResponseException failure) {
            if (failure.getStatusCode().value() != 400 && failure.getStatusCode().value() != 401)
                throw new BusinessException(ResponseCode.AUTHENTICATION_UNAVAILABLE, failure);
        } catch (RestClientException failure) {
            throw new BusinessException(ResponseCode.AUTHENTICATION_UNAVAILABLE, failure);
        }
    }
}
