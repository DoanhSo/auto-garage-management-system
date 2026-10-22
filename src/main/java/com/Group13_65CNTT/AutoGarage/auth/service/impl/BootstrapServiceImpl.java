package com.Group13_65CNTT.AutoGarage.auth.service.impl;

import com.Group13_65CNTT.AutoGarage.audit.annotation.Audited;
import com.Group13_65CNTT.AutoGarage.auth.service.BootstrapService;
import com.Group13_65CNTT.AutoGarage.common.config.BootstrapProperties;
import com.Group13_65CNTT.AutoGarage.common.constant.AuditConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.identity.dto.response.UserResponse;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.mapper.UserMapper;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class BootstrapServiceImpl implements BootstrapService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    @Audited(
            action = AuditConstants.ACTION_ADMIN_BOOTSTRAPPED,
            entity = AuditConstants.ENTITY_USER,
            skipNullResult = true)
    public UserResponse initialize(BootstrapProperties properties) {
        if (!properties.enabled()) return null;
        if (!properties.isConfigured()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
        var administrators = userRepository.lockActiveAdministrators(Role.ADMIN);
        String username = properties.username().toLowerCase(Locale.ROOT);
        String identityId =
                properties.identityId() == null || properties.identityId().isBlank()
                        ? null
                        : properties.identityId().strip();
        var existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            if (existing.get().getRole() != Role.ADMIN
                    || !existing.get().isActive()
                    || existing.get().getDeletedAt() != null) {
                throw new BusinessException(ResponseCode.BOOTSTRAP_ACCOUNT_CONFLICT);
            }
            return null;
        }
        if (identityId != null && userRepository.existsByIdentityId(identityId)) {
            throw new BusinessException(ResponseCode.BOOTSTRAP_ACCOUNT_CONFLICT);
        }
        if (!administrators.isEmpty()) return null;
        User user =
                User.builder()
                        .identityId(identityId)
                        .username(username)
                        .fullName(properties.fullName())
                        .role(Role.ADMIN)
                        .build();
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }
}
