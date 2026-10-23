package com.Group13_65CNTT.AutoGarage.auth.service.impl;

import com.Group13_65CNTT.AutoGarage.audit.annotation.Audited;
import com.Group13_65CNTT.AutoGarage.auth.service.AuthService;
import com.Group13_65CNTT.AutoGarage.common.constant.AuditConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.identity.dto.response.UserResponse;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.mapper.UserMapper;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CurrentUserProvider currentUser;
    private final EntityManager entityManager;
    private final Clock clock;

    @Override
    @Transactional
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public UserResponse me() {
        long id = currentUser.userId();
        User user = activeUser(id);
        String subject = currentUser.subjectOrNull();
        if (subject != null
                && !subject.equals(Long.toString(id))
                && subject.length() <= SecurityConstants.IDENTITY_ID_MAX_LENGTH
                && user.getIdentityId() == null) {
            user = lockUser(id);
            if (!user.isActive() || user.getDeletedAt() != null) {
                throw new BusinessException(ResponseCode.UNAUTHORIZED);
            }
            if (user.getIdentityId() == null) {
                user.setIdentityId(subject);
                user.setLastLoginAt(clock.instant());
                user.setUpdatedBy(user.getId());
                user = userRepository.saveAndFlush(user);
            }
        }
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Audited(action = AuditConstants.ACTION_AUTH_LOGOUT_ALL, entity = AuditConstants.ENTITY_USER)
    public void logout() {
        User user = lockUser(currentUser.userId());
        if (!user.isActive() || user.getDeletedAt() != null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED);
        }
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
                        .orElseThrow(() -> new BusinessException(ResponseCode.UNAUTHORIZED));
        entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        return user;
    }
}
