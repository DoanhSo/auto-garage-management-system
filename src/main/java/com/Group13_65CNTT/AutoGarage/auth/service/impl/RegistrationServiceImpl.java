package com.Group13_65CNTT.AutoGarage.auth.service.impl;

import com.Group13_65CNTT.AutoGarage.auth.dto.request.RegistrationRequest;
import com.Group13_65CNTT.AutoGarage.auth.service.RegistrationService;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.util.PasswordPolicy;
import com.Group13_65CNTT.AutoGarage.identity.service.KeycloakIdentityService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final KeycloakIdentityService keycloakIdentityService;
    private final PasswordPolicy passwordPolicy;

    @Override
    @PreAuthorize(SecurityConstants.PERMIT_ALL)
    public void register(RegistrationRequest request) {
        passwordPolicy.validateNewPassword(request.getPassword());
        String email = request.getEmail().strip().toLowerCase(Locale.ROOT);
        // Keycloak owns credentials; the existing authenticated /me flow provisions
        // the garage CUSTOMER and customer profile after the first successful login.
        keycloakIdentityService.createOrLinkUser(
                email, email, request.getFullName().strip(), Role.CUSTOMER, request.getPassword())
                .orElseThrow(() -> new BusinessException(ResponseCode.AUTHENTICATION_UNAVAILABLE));
    }
}
