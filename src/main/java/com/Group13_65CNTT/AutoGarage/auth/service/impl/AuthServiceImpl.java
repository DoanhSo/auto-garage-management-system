package com.Group13_65CNTT.AutoGarage.auth.service.impl;

import com.Group13_65CNTT.AutoGarage.audit.annotation.Audited;
import com.Group13_65CNTT.AutoGarage.auth.service.AuthService;
import com.Group13_65CNTT.AutoGarage.common.constant.AuditConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.common.security.KeycloakJwtAuthenticationConverter;
import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.entity.CustomerType;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import com.Group13_65CNTT.AutoGarage.identity.dto.response.UserResponse;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.mapper.UserMapper;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.identity.service.KeycloakIdentityService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final UserMapper userMapper;
    private final CurrentUserProvider currentUser;
    private final EntityManager entityManager;
    private final Clock clock;
    private final KeycloakIdentityService keycloakIdentityService;

    @Override
    @Transactional
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public UserResponse me() {
        Long resolvedId = currentUser.userIdOrNull();
        User user = resolvedId != null ? activeUser(resolvedId) : provisionCustomerFromJwt();
        String subject = currentUser.subjectOrNull();
        if (subject != null
                && !subject.equals(Long.toString(user.getId()))
                && subject.length() <= SecurityConstants.IDENTITY_ID_MAX_LENGTH
                && user.getIdentityId() == null) {
            user = lockUser(user.getId());
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
        if (user.getRole() == Role.CUSTOMER && customerRepository != null) {
            linkExistingCustomerByVerifiedEmail(user);
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
        keycloakIdentityService.syncAuthVersion(user.getIdentityId(), user.getUsername(), user.getAuthVersion());
    }

    private User provisionCustomerFromJwt() {
        Jwt jwt =
                currentUser
                        .jwt()
                        .orElseThrow(() -> new BusinessException(ResponseCode.UNAUTHORIZED));
        Role role = KeycloakJwtAuthenticationConverter.extractSingleRole(jwt).orElse(null);
        String subject = jwt.getSubject();
        String rawUsername = jwt.getClaimAsString(SecurityConstants.PREFERRED_USERNAME_CLAIM);
        if (role != Role.CUSTOMER
                || subject == null
                || !subject.matches(SecurityConstants.IDENTITY_ID_PATTERN)
                || rawUsername == null
                || rawUsername.isBlank()) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED);
        }
        String username = sanitizeUsername(rawUsername, subject);
        String email = normalizeEmail(jwt.getClaimAsString(SecurityConstants.EMAIL_CLAIM));
        if (email != null && userRepository.existsByEmail(email)) {
            email = null;
        }
        String fullName =
                sanitizeFullName(jwt.getClaimAsString(SecurityConstants.NAME_CLAIM), username);
        User created =
                User.builder()
                        .identityId(subject)
                        .username(username)
                        .email(email)
                        .fullName(fullName)
                        .role(Role.CUSTOMER)
                        .active(true)
                        .lastLoginAt(clock.instant())
                        .build();
        User saved = userRepository.saveAndFlush(created);
        if (customerRepository != null) {
            ensureCustomerProfileLinked(saved);
        }
        return saved;
    }

    private boolean linkExistingCustomerByVerifiedEmail(User user) {
        if (customerRepository.findByUserId(user.getId()).isPresent()) return true;
        Boolean emailVerified =
                currentUser
                        .jwt()
                        .map(t -> t.getClaimAsBoolean(SecurityConstants.EMAIL_VERIFIED_CLAIM))
                        .orElse(Boolean.FALSE);
        String verifiedTokenEmail = currentUser.jwt()
                .map(token -> normalizeEmail(token.getClaimAsString(SecurityConstants.EMAIL_CLAIM)))
                .orElse(null);
        if (user.getEmail() != null && Boolean.TRUE.equals(emailVerified)
                && user.getEmail().equalsIgnoreCase(verifiedTokenEmail)) {
            var existingByEmail =
                    customerRepository.findFirstByEmailIgnoreCaseAndUserIdIsNullOrderByIdAsc(
                            user.getEmail());
            if (existingByEmail.isPresent()) {
                Customer customer = existingByEmail.get();
                customer.setUserId(user.getId());
                customerRepository.saveAndFlush(customer);
                return true;
            }
        }
        return false;
    }

    private void ensureCustomerProfileLinked(User user) {
        if (linkExistingCustomerByVerifiedEmail(user)) return;
        Customer customer = new Customer();
        customer.setUserId(user.getId());
        customer.setCustomerCode(SecurityConstants.CUSTOMER_CODE_OIDC_PREFIX + user.getId());
        customer.setFullName(user.getFullName());
        customer.setEmail(user.getEmail());
        customer.setCustomerType(CustomerType.INDIVIDUAL);
        customer.setIsActive(true);
        customerRepository.saveAndFlush(customer);
    }

    private String sanitizeUsername(String rawUsername, String subject) {
        String candidate =
                rawUsername.strip().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", ".");
        if (candidate.length() < 3) {
            candidate = "cus." + subject.substring(0, Math.min(8, subject.length()));
        }
        if (candidate.length() > SecurityConstants.USERNAME_MAX_LENGTH) {
            candidate = candidate.substring(0, SecurityConstants.USERNAME_MAX_LENGTH);
        }
        if (!userRepository.existsByUsername(candidate)) return candidate;
        String suffix = "." + subject.substring(0, Math.min(8, subject.length()));
        int maxBase = SecurityConstants.USERNAME_MAX_LENGTH - suffix.length();
        String withSuffix =
                (candidate.length() > maxBase ? candidate.substring(0, maxBase) : candidate)
                        + suffix;
        return withSuffix.toLowerCase(Locale.ROOT);
    }

    private String sanitizeFullName(String rawName, String fallbackUsername) {
        if (rawName == null || rawName.isBlank()) {
            return fallbackUsername.isBlank()
                    ? SecurityConstants.DEFAULT_CUSTOMER_NAME
                    : fallbackUsername;
        }
        String stripped = rawName.strip();
        return stripped.length() > SecurityConstants.FULL_NAME_MAX_LENGTH
                ? stripped.substring(0, SecurityConstants.FULL_NAME_MAX_LENGTH)
                : stripped;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) return null;
        String stripped = email.strip().toLowerCase(Locale.ROOT);
        return stripped.length() <= SecurityConstants.EMAIL_MAX_LENGTH ? stripped : null;
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
