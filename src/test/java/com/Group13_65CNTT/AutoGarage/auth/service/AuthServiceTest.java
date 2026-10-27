package com.Group13_65CNTT.AutoGarage.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.Group13_65CNTT.AutoGarage.auth.service.impl.AuthServiceImpl;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.mapper.UserMapper;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.identity.service.KeycloakIdentityService;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository userRepository;
    @Mock CustomerRepository customerRepository;
    @Mock UserMapper userMapper;
    @Mock CurrentUserProvider currentUser;
    @Mock EntityManager entityManager;
    @Mock Clock clock;
    @Mock KeycloakIdentityService keycloakIdentityService;
    @InjectMocks AuthServiceImpl service;

    @Test
    void logoutSynchronizesRevocationVersionBeforeReturningSuccess() {
        User user = user();
        when(currentUser.userId()).thenReturn(1L);
        when(userRepository.findLockedById(1L)).thenReturn(Optional.of(user));
        service.logout();
        assertThat(user.getAuthVersion()).isEqualTo(1);
        verify(keycloakIdentityService).syncAuthVersion("customer-subject", "customer.one", 1L);
    }

    @Test
    void logoutReportsUnavailableWhenIdentityProviderCannotRevokeSessions() {
        User user = user();
        when(currentUser.userId()).thenReturn(1L);
        when(userRepository.findLockedById(1L)).thenReturn(Optional.of(user));
        var unavailable = new BusinessException(ResponseCode.AUTHENTICATION_UNAVAILABLE);
        doThrow(unavailable).when(keycloakIdentityService).syncAuthVersion(anyString(), anyString(), anyLong());
        assertThatThrownBy(service::logout).isSameAs(unavailable);
    }

    @Test
    void verifiedDifferentEmailCannotLinkHistoricalCustomerProfile() {
        arrangeMe("attacker@example.test");
        service.me();
        verify(customerRepository, never()).findFirstByEmailIgnoreCaseAndUserIdIsNullOrderByIdAsc(anyString());
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void matchingVerifiedEmailLinksExistingCustomerProfile() {
        arrangeMe("CUSTOMER@example.test");
        Customer customer = new Customer();
        when(customerRepository.findFirstByEmailIgnoreCaseAndUserIdIsNullOrderByIdAsc("customer@example.test"))
                .thenReturn(Optional.of(customer));
        service.me();
        assertThat(customer.getUserId()).isEqualTo(1L);
        verify(customerRepository).saveAndFlush(customer);
    }

    private void arrangeMe(String tokenEmail) {
        when(currentUser.userIdOrNull()).thenReturn(1L);
        when(currentUser.subjectOrNull()).thenReturn("customer-subject");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        Jwt token = Jwt.withTokenValue("test").header("alg", "RS256")
                .claim("email_verified", true).claim("email", tokenEmail).build();
        when(currentUser.jwt()).thenReturn(Optional.of(token));
    }

    private User user() {
        User user = User.builder().identityId("customer-subject").username("customer.one")
                .email("customer@example.test").role(Role.CUSTOMER).active(true).build();
        user.setId(1L);
        return user;
    }
}
