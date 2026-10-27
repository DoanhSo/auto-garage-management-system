package com.Group13_65CNTT.AutoGarage.common.security;

import com.Group13_65CNTT.AutoGarage.common.enums.AccessResource;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import com.Group13_65CNTT.AutoGarage.identity.service.ObjectAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerScopeTest {
    @Mock CurrentUserProvider currentUser;
    @Mock CustomerRepository customers;
    @Mock ObjectAccessService access;
    @InjectMocks CustomerScope scope;

    @Test
    void customerWithoutFilterQueriesOnlyLinkedCustomer() {
        when(currentUser.role()).thenReturn(Role.CUSTOMER);
        when(currentUser.userId()).thenReturn(42L);
        Customer customer = new Customer();
        customer.setId(7L);
        when(customers.findByUserId(42L)).thenReturn(Optional.of(customer));

        assertThat(scope.customerIdForQuery(null)).isEqualTo(7L);
        assertThat(scope.customerIdForQuery(7L)).isEqualTo(7L);
        assertThatThrownBy(() -> scope.customerIdForQuery(8L))
                .isInstanceOfSatisfying(BusinessException.class,
                        failure -> assertThat(failure.getResponseCode()).isEqualTo(ResponseCode.FORBIDDEN));
    }

    @Test
    void missingCustomerProfileNeverFallsBackToCallerId() {
        when(currentUser.role()).thenReturn(Role.CUSTOMER);
        when(currentUser.userId()).thenReturn(42L);
        when(customers.findByUserId(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scope.customerIdForQuery(7L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void staffDirectoryRequiresFrontDeskRole() {
        when(currentUser.role()).thenReturn(Role.TECHNICIAN);
        when(currentUser.userId()).thenReturn(42L);
        doThrow(new BusinessException(ResponseCode.FORBIDDEN)).when(access)
                .requireRole(42L, "ADMIN", "RECEPTIONIST");

        assertThatThrownBy(() -> scope.customerIdForQuery(null)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(customers);
    }

    @Test
    void resourceAuthorizationUsesAuthenticatedActor() {
        when(currentUser.userId()).thenReturn(42L);
        scope.requireResource(AccessResource.VEHICLE, 7L);
        verify(access).requireResourceAccess(42L, AccessResource.VEHICLE, 7L);
    }
}
