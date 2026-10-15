package com.Group13_65CNTT.AutoGarage.auth.service.impl;

import com.Group13_65CNTT.AutoGarage.audit.annotation.Audited;
import com.Group13_65CNTT.AutoGarage.auth.dto.request.LoginRequest;
import com.Group13_65CNTT.AutoGarage.auth.dto.request.PasswordChangeRequest;
import com.Group13_65CNTT.AutoGarage.auth.dto.response.LoginResponse;
import com.Group13_65CNTT.AutoGarage.auth.service.AuthService;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.common.security.JwtTokenProvider;
import com.Group13_65CNTT.AutoGarage.common.util.PasswordPolicy;
import com.Group13_65CNTT.AutoGarage.identity.dto.response.UserResponse;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.mapper.UserMapper;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final JwtTokenProvider tokenProvider;
    private final CurrentUserProvider currentUser;
    private final EntityManager entityManager;
    private final Clock clock;
    private String dummyHash;

    @PostConstruct
    void initializeDummyHash() {
        dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    @Transactional
    @Audited(action = "AUTH_LOGIN", entity = "USER")
    public LoginResponse login(LoginRequest request) {
        if (request == null
                || request.getIdentifier() == null
                || request.getIdentifier().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {
            throw new BusinessException(ResponseCode.BAD_CREDENTIALS);
        }
        User user =
                userRepository
                        .findByIdentifier(request.getIdentifier().strip().toLowerCase(Locale.ROOT))
                        .orElse(null);
        if (user != null) user = lockUser(user.getId());
        boolean matches =
                PasswordPolicy.withinByteLimit(request.getPassword())
                        && passwordEncoder.matches(
                                request.getPassword(),
                                user == null ? dummyHash : user.getPasswordHash());
        if (!matches || user == null || !user.isActive() || user.getDeletedAt() != null)
            throw new BusinessException(ResponseCode.BAD_CREDENTIALS);
        user.setLastLoginAt(clock.instant());
        user.setUpdatedBy(user.getId());
        userRepository.saveAndFlush(user);
        var token = tokenProvider.issue(user);
        return LoginResponse.builder()
                .accessToken(token.value())
                .tokenType(SecurityConstants.BEARER_TOKEN_TYPE)
                .expiresAt(token.expiresAt())
                .user(userMapper.toResponse(user))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public UserResponse me() {
        return userMapper.toResponse(activeUser(currentUser.userId()));
    }

    @Override
    @Transactional
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Audited(action = "AUTH_LOGOUT_ALL", entity = "USER")
    public void logout() {
        User user = lockUser(currentUser.userId());
        if (!user.isActive()) throw new BusinessException(ResponseCode.UNAUTHORIZED);
        user.setAuthVersion(user.getAuthVersion() + 1);
        user.setUpdatedBy(user.getId());
        userRepository.saveAndFlush(user);
    }

    @Override
    @Transactional
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Audited(action = "USER_PASSWORD_CHANGED", entity = "USER")
    public void changePassword(PasswordChangeRequest request) {
        if (request == null) throw new BusinessException(ResponseCode.BAD_REQUEST);
        User user = lockUser(currentUser.userId());
        if (!user.isActive()) throw new BusinessException(ResponseCode.UNAUTHORIZED);
        if (!PasswordPolicy.withinByteLimit(request.getCurrentPassword())
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResponseCode.BAD_CREDENTIALS);
        }
        passwordPolicy.validateNewPassword(request.getNewPassword());
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setAuthVersion(user.getAuthVersion() + 1);
        user.setUpdatedBy(user.getId());
        userRepository.saveAndFlush(user);
    }

    private User activeUser(long id) {
        return userRepository
                .findById(id)
                .filter(User::isActive)
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ResponseCode.UNAUTHORIZED));
    }

    private User lockUser(Long id) {
        User user =
                userRepository
                        .findLockedById(id)
                        .orElseThrow(() -> new BusinessException(ResponseCode.BAD_CREDENTIALS));
        entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        return user;
    }
}
