package com.Group13_65CNTT.AutoGarage.identity.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.Group13_65CNTT.AutoGarage.common.config.PaginationProperties;
import com.Group13_65CNTT.AutoGarage.common.config.PasswordProperties;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.common.util.PasswordPolicy;
import com.Group13_65CNTT.AutoGarage.identity.dto.request.UserCreateRequest;
import com.Group13_65CNTT.AutoGarage.identity.dto.request.UserUpdateRequest;
import com.Group13_65CNTT.AutoGarage.identity.dto.response.UserResponse;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.mapper.UserMapper;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.identity.service.impl.UserServiceImpl;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService business rules")
class UserServiceTest {
    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock CurrentUserProvider currentUser;
    @Mock EntityManager entityManager;
    @Mock KeycloakIdentityService keycloakIdentityService;
    @Spy PaginationPolicy pagination = new PaginationPolicy(new PaginationProperties());
    @Spy PasswordPolicy passwordPolicy = new PasswordPolicy(new PasswordProperties());
    @InjectMocks UserServiceImpl service;

    @BeforeEach
    void actor() {
        lenient().when(currentUser.userId()).thenReturn(1L);
    }

    @Test
    void duplicateRacePreservesCauseAndUsesConflict() {
        var cause =
                new DataIntegrityViolationException(
                        "private database detail",
                        new SQLException("private database detail", "23000", 1062));
        arrangePersistenceFailure(cause);
        assertThatThrownBy(() -> service.createUser(createRequest()))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        failure -> {
                            assertThat(failure.getResponseCode())
                                    .isEqualTo(ResponseCode.DUPLICATE_RESOURCE);
                            assertThat(failure.getCause()).isSameAs(cause);
                        });
    }

    @Test
    void unrelatedConstraintIsNotMisreportedAsDuplicateUsername() {
        var cause =
                new DataIntegrityViolationException(
                        "private database detail",
                        new SQLException("private database detail", "23000", 1452));
        arrangePersistenceFailure(cause);
        assertThatThrownBy(() -> service.createUser(createRequest())).isSameAs(cause);
    }

    private void arrangePersistenceFailure(DataIntegrityViolationException cause) {
        administrator();
        when(userMapper.toEntity(any())).thenReturn(user(2, Role.CUSTOMER));
        when(userRepository.saveAndFlush(any())).thenThrow(cause);
    }

    @Test
    void customerCannotCreateAccounts() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1, Role.CUSTOMER)));
        assertThatThrownBy(() -> service.createUser(createRequest()))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getResponseCode())
                                        .isEqualTo(ResponseCode.FORBIDDEN));
        verifyNoInteractions(userMapper);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void newIdentityMustSatisfyPasswordPolicyBeforeCallingKeycloak() {
        administrator();
        var request = createRequest();
        request.setIdentityId(null);
        request.setPassword("short");
        assertThatThrownBy(() -> service.createUser(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        failure -> assertThat(failure.getResponseCode()).isEqualTo(ResponseCode.INVALID_PASSWORD));
        verifyNoInteractions(keycloakIdentityService);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void missingKeycloakIdentityCannotBeReportedAsSuccessfullyCreatedAccount() {
        administrator();
        var request = createRequest();
        request.setIdentityId(null);
        request.setPassword("test-password-123");
        when(keycloakIdentityService.createOrLinkUser(anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createUser(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        failure -> assertThat(failure.getResponseCode()).isEqualTo(ResponseCode.AUTHENTICATION_UNAVAILABLE));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void invalidIdentityIdIsRejectedBeforeWrite() {
        administrator();
        UserCreateRequest request = createRequest();
        request.setIdentityId("invalid identity id with spaces!");
        assertThatThrownBy(() -> service.createUser(request))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getResponseCode())
                                        .isEqualTo(ResponseCode.BAD_REQUEST));
        verifyNoInteractions(userMapper);
    }

    @Test
    void createsCanonicalIdentityWithKeycloakSubjectAndAuditorFields() {
        administrator();
        UserCreateRequest request = createRequest();
        User entity = user(2, Role.CUSTOMER);
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(userRepository.saveAndFlush(entity)).thenReturn(entity);
        UserResponse response = UserResponse.builder().id(2L).role(Role.CUSTOMER).build();
        when(userMapper.toResponse(entity)).thenReturn(response);
        assertThat(service.createUser(request)).isSameAs(response);
        assertThat(entity.getIdentityId()).isEqualTo("550e8400-e29b-41d4-a716-446655440022");
        assertThat(entity.getUsername()).isEqualTo("new.user");
        assertThat(entity.getEmail()).isEqualTo("user@example.test");
        assertThat(entity.getPasswordHash()).isNull();
        assertThat(entity.getCreatedBy()).isEqualTo(1L);
        assertThat(entity.getUpdatedBy()).isEqualTo(1L);
    }

    @Test
    void missingRoleIsControlledBeforeMapping() {
        administrator();
        UserCreateRequest request = createRequest();
        request.setRole(null);
        assertThatThrownBy(() -> service.createUser(request)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(userMapper);
    }

    @Test
    void unauthorizedActorCannotAcquireMutationLocks() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1, Role.CUSTOMER)));
        assertThatThrownBy(() -> service.updateUser(2L, updateRequest()))
                .isInstanceOf(BusinessException.class);
        verify(userRepository, never()).lockActiveAdministrators(any());
        verify(userRepository, never()).findLockedById(any());
    }

    @Test
    void protectsLastActiveAdministrator() {
        administrator();
        User admin = user(1, Role.ADMIN);
        when(userRepository.lockActiveAdministrators(Role.ADMIN)).thenReturn(List.of(admin));
        when(userRepository.findLockedById(1L)).thenReturn(Optional.of(admin));
        assertThatThrownBy(() -> service.updateUser(1L, updateRequest()))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getResponseCode())
                                        .isEqualTo(ResponseCode.LAST_ADMIN));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void revocationWhileWaitingPreventsMutation() {
        administrator();
        when(userRepository.lockActiveAdministrators(Role.ADMIN))
                .thenReturn(List.of(user(2, Role.ADMIN)));
        when(userRepository.findLockedById(1L)).thenReturn(Optional.of(user(1, Role.CUSTOMER)));
        assertThatThrownBy(() -> service.updateUser(2L, updateRequest()))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getResponseCode())
                                        .isEqualTo(ResponseCode.FORBIDDEN));
        verify(userRepository, never()).findLockedById(2L);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void duplicateUsernameOrIdentityIdDoesNotPersist() {
        administrator();
        when(userRepository.existsByUsername("new.user")).thenReturn(true);
        assertThatThrownBy(() -> service.createUser(createRequest()))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getResponseCode())
                                        .isEqualTo(ResponseCode.DUPLICATE_RESOURCE));
        verifyNoInteractions(userMapper);
    }

    private void administrator() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1, Role.ADMIN)));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "passwordHash",
                "authVersion",
                "id desc; DROP TABLE users; --",
                "LENGTH(username)"
            })
    void rejectsUnapprovedSortEvenWhenServiceIsCalledWithoutController(String property) {
        administrator();
        var pageable = PageRequest.of(0, 10, Sort.by("username").and(Sort.by(property)));
        assertThatThrownBy(() -> service.searchUsers(null, null, pageable))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getResponseCode())
                                        .isEqualTo(ResponseCode.BAD_REQUEST));
        verify(userRepository, never()).searchUsers(any(), any(), any());
    }

    @Test
    void allowsMultipleApprovedSortFieldsWithoutChangingBoundKeyword() {
        administrator();
        var pageable = PageRequest.of(0, 10, Sort.by("username").and(Sort.by("id")));
        String keyword = "' OR 1=1 --";
        when(userRepository.searchUsers(keyword, true, pageable)).thenReturn(Page.empty(pageable));
        assertThat(service.searchUsers(keyword, true, pageable).isEmpty()).isTrue();
        verify(userRepository).searchUsers(keyword, true, pageable);
    }

    private User user(long id, Role role) {
        User user =
                User.builder()
                        .username("user." + id)
                        .fullName("User")
                        .role(role)
                        .build();
        user.setId(id);
        return user;
    }

    private UserCreateRequest createRequest() {
        return UserCreateRequest.builder()
                .identityId("550e8400-e29b-41d4-a716-446655440022")
                .username("New.User")
                .email(" USER@EXAMPLE.TEST ")
                .fullName("User")
                .role(Role.CUSTOMER)
                .build();
    }

    private UserUpdateRequest updateRequest() {
        return UserUpdateRequest.builder()
                .fullName("User")
                .role(Role.CUSTOMER)
                .active(true)
                .version(0L)
                .build();
    }
}
