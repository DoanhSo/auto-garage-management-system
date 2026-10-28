package com.Group13_65CNTT.AutoGarage.identity.service.impl;

import com.Group13_65CNTT.AutoGarage.common.enums.AccessResource;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.AccessRepository;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.identity.repository.projection.ResourceOwnership;
import com.Group13_65CNTT.AutoGarage.identity.service.ObjectAccessService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ObjectAccessServiceImpl implements ObjectAccessService {
    private final UserRepository userRepository;
    private final AccessRepository accessRepository;
    private final CurrentUserProvider currentUser;

    @Override
    public void requireRole(long actorId, String... roles) {
        User actor = actor(actorId);
        if (!Arrays.asList(roles).contains(actor.getRole().name())) forbidden();
    }

    @Override
    public void requireVehicleAccess(long actorId, long vehicleId) {
        requireResourceAccess(actorId, AccessResource.VEHICLE, vehicleId);
    }

    @Override
    public void requireRepairOrderAccess(long actorId, long orderId) {
        requireResourceAccess(actorId, AccessResource.REPAIR_ORDER, orderId);
    }

    @Override
    public void requireResourceAccess(long actorId, AccessResource resource, long id) {
        User user = actor(actorId);
        if (id <= 0) throw new BusinessException(ResponseCode.BAD_REQUEST);
        ResourceOwnership ownership =
                ownership(resource, id)
                        .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        if (resource == AccessResource.NOTIFICATION) {
            if (Objects.equals(ownership.getOwnerId(), actorId)) return;
            forbidden();
        }
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.RECEPTIONIST) return;
        if (user.getRole() == Role.CUSTOMER && Objects.equals(ownership.getOwnerId(), actorId))
            return;
        if (user.getRole() == Role.WAREHOUSE_STAFF && resource == AccessResource.REPAIR_ORDER)
            return;
        if (user.getRole() == Role.TECHNICIAN) {
            boolean operational =
                    resource == AccessResource.REPAIR_ORDER
                            || resource == AccessResource.ESTIMATE
                            || resource == AccessResource.TASK
                            || resource == AccessResource.ATTACHMENT;
            if (operational
                    && ownership.getOrderId() != null
                    && accessRepository.assignedToOrder(actorId, ownership.getOrderId()) > 0)
                return;
            if (resource == AccessResource.VEHICLE
                    && accessRepository.assignedToVehicle(actorId, id) > 0) return;
        }
        forbidden();
    }

    @Override
    public void requireQualityCheckAccess(long actorId, long orderId) {
        User user = actor(actorId);
        if (user.getRole() == Role.ADMIN) return;
        requireRole(actorId, Role.TECHNICIAN.name());
        requireRepairOrderAccess(actorId, orderId);
        if (accessRepository.assignedToQc(actorId, orderId) == 0) forbidden();
    }

    private User actor(long actorId) {
        if (actorId <= 0 || currentUser.userId() != actorId) forbidden();
        return userRepository
                .findById(actorId)
                .filter(User::isActive)
                .orElseThrow(() -> new BusinessException(ResponseCode.UNAUTHORIZED));
    }

    @Override
    public void requireTaskExecutionAccess(long actorId, long taskId) {
        User user = actor(actorId);
        if (user.getRole() == Role.ADMIN) return;
        if (user.getRole() != Role.TECHNICIAN || accessRepository.assignedToTask(actorId, taskId) == 0) forbidden();
    }

    @Override
    public void requireAssignmentExecutionAccess(long actorId, long assignmentId) {
        User user = actor(actorId);
        if (user.getRole() == Role.ADMIN) return;
        if (user.getRole() != Role.TECHNICIAN || accessRepository.ownsAssignment(actorId, assignmentId) == 0) forbidden();
    }

    private Optional<ResourceOwnership> ownership(AccessResource resource, long id) {
        return switch (resource) {
            case CUSTOMER -> accessRepository.ownershipCustomer(id);
            case VEHICLE -> accessRepository.ownershipVehicle(id);
            case BOOKING -> accessRepository.ownershipBooking(id);
            case REPAIR_ORDER -> accessRepository.ownershipRepairOrder(id);
            case ESTIMATE -> accessRepository.ownershipEstimate(id);
            case TASK -> accessRepository.ownershipTask(id);
            case INVOICE -> accessRepository.ownershipInvoice(id);
            case WARRANTY -> accessRepository.ownershipWarranty(id);
            case WARRANTY_CLAIM -> accessRepository.ownershipWarrantyClaim(id);
            case NOTIFICATION -> accessRepository.ownershipNotification(id);
            case ATTACHMENT -> accessRepository.ownershipAttachment(id);
        };
    }

    private void forbidden() {
        throw new BusinessException(ResponseCode.FORBIDDEN);
    }
}
