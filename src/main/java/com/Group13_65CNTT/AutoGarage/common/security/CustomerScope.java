package com.Group13_65CNTT.AutoGarage.common.security;

import com.Group13_65CNTT.AutoGarage.common.enums.AccessResource;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import com.Group13_65CNTT.AutoGarage.identity.service.ObjectAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Applies authenticated customer ownership to reception queries and mutations. */
@Component
@RequiredArgsConstructor
public class CustomerScope {
    private final CurrentUserProvider currentUser;
    private final CustomerRepository customers;
    private final ObjectAccessService access;

    public void requireFrontDesk() {
        access.requireRole(currentUser.userId(), Role.ADMIN.name(), Role.RECEPTIONIST.name());
    }

    public void requireResource(AccessResource resource, Long id) {
        if (id == null || id <= 0) throw new BusinessException(ResponseCode.BAD_REQUEST);
        access.requireResourceAccess(currentUser.userId(), resource, id);
    }

    public Long customerIdForQuery(Long requestedCustomerId) {
        if (currentUser.role() != Role.CUSTOMER) {
            requireFrontDesk();
            return requestedCustomerId;
        }
        long ownId = currentCustomerId();
        if (requestedCustomerId != null && requestedCustomerId != ownId) {
            throw new BusinessException(ResponseCode.FORBIDDEN);
        }
        return ownId;
    }

    public long currentCustomerId() {
        return customers.findByUserId(currentUser.userId())
                .map(Customer::getId)
                .orElseThrow(() -> new BusinessException(ResponseCode.FORBIDDEN));
    }
}
