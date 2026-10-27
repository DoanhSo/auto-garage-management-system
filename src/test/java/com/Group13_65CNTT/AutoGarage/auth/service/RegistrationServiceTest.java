package com.Group13_65CNTT.AutoGarage.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.Group13_65CNTT.AutoGarage.auth.dto.request.RegistrationRequest;
import com.Group13_65CNTT.AutoGarage.auth.service.impl.RegistrationServiceImpl;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.util.PasswordPolicy;
import com.Group13_65CNTT.AutoGarage.identity.service.KeycloakIdentityService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {
    @Mock KeycloakIdentityService identities;
    @Mock PasswordPolicy passwordPolicy;
    @InjectMocks RegistrationServiceImpl service;

    @Test
    void normalizesEmailAndCreatesOnlyCustomerWithoutTransformingPassword() {
        RegistrationRequest request = request();
        request.setUsername(" Customer.One ");
        request.setEmail(" Customer@Gmail.com "); request.setFullName(" Customer Name "); request.setPassword("  test-password-123  ");
        when(identities.createOrLinkUser(anyString(), anyString(), anyString(), any(), anyString())).thenReturn(Optional.of("new-identity"));
        service.register(request);
        var order = inOrder(passwordPolicy, identities);
        order.verify(passwordPolicy).validateNewPassword("  test-password-123  ");
        order.verify(identities).createOrLinkUser("customer.one", "customer@gmail.com", "Customer Name", Role.CUSTOMER, "  test-password-123  ");
    }

    @Test
    void invalidPasswordNeverCreatesAnIdentity() {
        RegistrationRequest request = request();
        doThrow(new BusinessException(ResponseCode.INVALID_PASSWORD)).when(passwordPolicy).validateNewPassword(request.getPassword());
        assertThatThrownBy(() -> service.register(request)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(identities);
    }

    @Test
    void duplicateIdentityIsNotLinkedOrPromoted() {
        var duplicate = new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
        when(identities.createOrLinkUser(anyString(), anyString(), anyString(), any(), anyString())).thenThrow(duplicate);
        assertThatThrownBy(() -> service.register(request())).isSameAs(duplicate);
        verify(identities, never()).syncUserUpdate(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void disabledIdentityAdapterDoesNotReportRegistrationSuccess() {
        when(identities.createOrLinkUser(anyString(), anyString(), anyString(), any(), anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.register(request())).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getResponseCode()).isEqualTo(ResponseCode.AUTHENTICATION_UNAVAILABLE));
    }

    private RegistrationRequest request() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("customer.one"); request.setFullName("Customer Name"); request.setEmail("customer@gmail.com"); request.setPassword("test-password-123");
        return request;
    }
}
