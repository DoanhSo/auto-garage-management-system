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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService business rules")
class UserServiceTest {
    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CurrentUserProvider currentUser;
    @Mock EntityManager entityManager;
    @Spy PasswordPolicy passwordPolicy = new PasswordPolicy(new PasswordProperties());
    @Spy PaginationPolicy pagination = new PaginationPolicy(new PaginationProperties());
    @InjectMocks UserServiceImpl service;

    @BeforeEach
    void actor() {
        when(currentUser.userId()).thenReturn(1L);
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
    void customerCannotCreateOrHashCredentials() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1, Role.CUSTOMER)));
        assertThatThrownBy(() -> service.createUser(createRequest()))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getResponseCode())
                                        .isEqualTo(ResponseCode.FORBIDDEN));
        verifyNoInteractions(passwordEncoder, userMapper);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void invalidPasswordIsRejectedBeforeHashOrWrite() {
        administrator();
        UserCreateRequest request = createRequest();
        request.setPassword("short");
        assertThatThrownBy(() -> service.createUser(request))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getResponseCode())
                                        .isEqualTo(ResponseCode.INVALID_PASSWORD));
        verifyNoInteractions(passwordEncoder, userMapper);
    }

    @Test
    void createsCanonicalIdentityWithEncodedPasswordAndAuditorFields() {
        administrator();
        UserCreateRequest request = createRequest();
        User entity = user(2, Role.CUSTOMER);
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(entity)).thenReturn(entity);
        UserResponse response = UserResponse.builder().id(2L).role(Role.CUSTOMER).build();
        when(userMapper.toResponse(entity)).thenReturn(response);
        assertThat(service.createUser(request)).isSameAs(response);
        assertThat(entity.getUsername()).isEqualTo("new.user");
        assertThat(entity.getEmail()).isEqualTo("user@example.test");
        assertThat(entity.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(entity.getCreatedBy()).isEqualTo(1L);
        assertThat(entity.getUpdatedBy()).isEqualTo(1L);
    }

    @Test
    void missingRoleIsControlledBeforeMapping() {
        administrator();
        UserCreateRequest request = createRequest();
        request.setRole(null);
        assertThatThrownBy(() -> service.createUser(request)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(passwordEncoder, userMapper);
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
    void duplicateUsernameDoesNotHashOrPersist() {
        administrator();
        when(userRepository.existsByUsername("new.user")).thenReturn(true);
        assertThatThrownBy(() -> service.createUser(createRequest()))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getResponseCode())
                                        .isEqualTo(ResponseCode.DUPLICATE_RESOURCE));
        verifyNoInteractions(passwordEncoder, userMapper);
    }

    private void administrator() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1, Role.ADMIN)));
    }

    private User user(long id, Role role) {
        User user =
                User.builder()
                        .username("user." + id)
                        .passwordHash("internal-hash")
                        .fullName("User")
                        .role(role)
                        .build();
        user.setId(id);
        return user;
    }

    private UserCreateRequest createRequest() {
        return UserCreateRequest.builder()
                .username("New.User")
                .email(" USER@EXAMPLE.TEST ")
                .fullName("User")
                .role(Role.CUSTOMER)
                .password("ValidPassword!123")
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
