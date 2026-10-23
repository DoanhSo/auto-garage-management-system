package com.Group13_65CNTT.AutoGarage.identity.service.impl;

import com.Group13_65CNTT.AutoGarage.audit.annotation.Audited;
import com.Group13_65CNTT.AutoGarage.common.constant.AuditConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.UserQueryConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.common.util.DatabaseFailures;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.identity.dto.request.UserCreateRequest;
import com.Group13_65CNTT.AutoGarage.identity.dto.request.UserUpdateRequest;
import com.Group13_65CNTT.AutoGarage.identity.dto.response.UserResponse;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.mapper.UserMapper;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.identity.service.KeycloakIdentityService;
import com.Group13_65CNTT.AutoGarage.identity.service.UserService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
public class UserServiceImpl implements UserService {
    private final PaginationPolicy pagination;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CurrentUserProvider currentUser;
    private final EntityManager entityManager;
    private final Clock clock;
    private final KeycloakIdentityService keycloakIdentityService;

    @Override
    @Transactional
    @Audited(action = AuditConstants.ACTION_USER_CREATED, entity = AuditConstants.ENTITY_USER)
    public UserResponse createUser(UserCreateRequest request) {
        long actorId = requireAdministrator();
        if (request == null) throw new BusinessException(ResponseCode.BAD_REQUEST);
        validateProfile(request.getFullName(), request.getRole());
        if (request.getUsername() == null
                || !request.getUsername().matches(SecurityConstants.USERNAME_PATTERN)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
        String identityId = normalizeIdentityId(request.getIdentityId());
        String username = request.getUsername().toLowerCase(Locale.ROOT);
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByUsername(username)
                || (email != null && userRepository.existsByEmail(email))
                || (identityId != null && userRepository.existsByIdentityId(identityId))) {
            throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
        }
        if (identityId == null && keycloakIdentityService != null) {
            identityId =
                    keycloakIdentityService
                            .createOrLinkUser(
                                    username,
                                    email,
                                    request.getFullName().strip(),
                                    request.getRole(),
                                    request.getPassword())
                            .orElse(null);
        }
        User user = userMapper.toEntity(request);
        user.setIdentityId(identityId);
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(request.getRole());
        user.setActive(true);
        user.setCreatedBy(actorId);
        user.setUpdatedBy(actorId);
        return userMapper.toResponse(save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String keyword, Boolean active, Pageable pageable) {
        requireAdministrator();
        pagination.validate(pageable, UserQueryConstants.SORT_FIELDS);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
        return userRepository
                .searchUsers(normalizedKeyword, active, pageable)
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        requireAdministrator();
        validateId(id);
        return userMapper.toResponse(
                userRepository
                        .findById(id)
                        .filter(user -> user.getDeletedAt() == null)
                        .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND)));
    }

    @Override
    @Transactional
    @Audited(action = AuditConstants.ACTION_USER_UPDATED, entity = AuditConstants.ENTITY_USER)
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        validateId(id);
        if (request == null) throw new BusinessException(ResponseCode.BAD_REQUEST);
        validateProfile(request.getFullName(), request.getRole());
        if (request.getActive() == null
                || request.getVersion() == null
                || request.getVersion() < 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
        return applyUpdate(id, request);
    }

    @Override
    @Transactional
    @Audited(
            action = AuditConstants.ACTION_USER_DELETED,
            entity = AuditConstants.ENTITY_USER,
            idArgument = 0)
    public void deleteUser(Long id, long version) {
        validateId(id);
        if (version < 0) throw new BusinessException(ResponseCode.BAD_REQUEST);
        long actorId = requireAdministrator();
        var administrators = userRepository.lockActiveAdministrators(Role.ADMIN);
        requireLockedAdministrator(actorId);
        User user = lockUser(id);
        verifyMutation(user, user.getRole(), false, version, administrators.size());
        user.setAuthVersion(user.getAuthVersion() + 1);
        user.setActive(false);
        user.setDeletedAt(clock.instant());
        user.setUpdatedBy(actorId);
        User saved = save(user);
        if (keycloakIdentityService != null) {
            keycloakIdentityService.disableUser(saved.getIdentityId(), saved.getUsername());
        }
    }

    private UserResponse applyUpdate(Long id, UserUpdateRequest request) {
        long actorId = requireAdministrator();
        var administrators = userRepository.lockActiveAdministrators(Role.ADMIN);
        requireLockedAdministrator(actorId);
        User user = lockUser(id);
        verifyMutation(
                user,
                request.getRole(),
                request.getActive(),
                request.getVersion(),
                administrators.size());
        if (user.getRole() != request.getRole() || user.isActive() != request.getActive()) {
            user.setAuthVersion(user.getAuthVersion() + 1);
        }
        userMapper.updateEntityFromRequest(request, user);
        user.setEmail(normalizeEmail(request.getEmail()));
        user.setRole(request.getRole());
        user.setActive(request.getActive());
        user.setUpdatedBy(actorId);
        User saved = save(user);
        if (keycloakIdentityService != null) {
            keycloakIdentityService.syncUserUpdate(
                    saved.getIdentityId(),
                    saved.getUsername(),
                    saved.getEmail(),
                    saved.getFullName(),
                    saved.getRole(),
                    saved.isActive());
        }
        return userMapper.toResponse(saved);
    }

    private void verifyMutation(
            User user, Role role, boolean active, long version, int administratorCount) {
        if (user.getVersion() != version)
            throw new BusinessException(ResponseCode.STALE_USER_VERSION);
        if (user.getRole() == Role.ADMIN
                && user.isActive()
                && (!active || role != Role.ADMIN)
                && administratorCount <= 1) {
            throw new BusinessException(ResponseCode.LAST_ADMIN);
        }
    }

    private long requireAdministrator() {
        long actorId = currentUser.userId();
        User actor =
                userRepository
                        .findById(actorId)
                        .filter(User::isActive)
                        .filter(user -> user.getDeletedAt() == null)
                        .orElseThrow(() -> new BusinessException(ResponseCode.UNAUTHORIZED));
        if (actor.getRole() != Role.ADMIN) throw new BusinessException(ResponseCode.FORBIDDEN);
        return actorId;
    }

    private void requireLockedAdministrator(long actorId) {
        User actor = lockUser(actorId);
        if (!actor.isActive()) throw new BusinessException(ResponseCode.UNAUTHORIZED);
        if (actor.getRole() != Role.ADMIN) throw new BusinessException(ResponseCode.FORBIDDEN);
    }

    private User lockUser(Long id) {
        User user =
                userRepository
                        .findLockedById(id)
                        .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        // Refresh defeats a stale persistence-context/snapshot after waiting on a MySQL row lock.
        entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        if (user.getDeletedAt() != null)
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND);
        return user;
    }

    private User save(User user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException failure) {
            if (DatabaseFailures.isDuplicateKey(failure)) {
                throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE, failure);
            }
            throw failure;
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ResponseCode.BAD_REQUEST);
    }

    private void validateProfile(String fullName, Role role) {
        if (fullName == null
                || fullName.isBlank()
                || fullName.length() > SecurityConstants.FULL_NAME_MAX_LENGTH
                || role == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    private String normalizeIdentityId(String identityId) {
        if (identityId == null || identityId.isBlank()) return null;
        String stripped = identityId.strip();
        if (!stripped.matches(SecurityConstants.IDENTITY_ID_PATTERN)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
        return stripped;
    }
}
