package com.Group13_65CNTT.AutoGarage.notification.service.impl;

import com.Group13_65CNTT.AutoGarage.audit.annotation.Audited;
import com.Group13_65CNTT.AutoGarage.common.constant.AuditConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.NotificationConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.notification.dto.request.NotificationBroadcastRequest;
import com.Group13_65CNTT.AutoGarage.notification.dto.request.NotificationCreateRequest;
import com.Group13_65CNTT.AutoGarage.notification.dto.response.NotificationBroadcastResponse;
import com.Group13_65CNTT.AutoGarage.notification.dto.response.NotificationResponse;
import com.Group13_65CNTT.AutoGarage.notification.entity.Notification;
import com.Group13_65CNTT.AutoGarage.notification.mapper.NotificationMapper;
import com.Group13_65CNTT.AutoGarage.notification.repository.NotificationRepository;
import com.Group13_65CNTT.AutoGarage.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final PaginationPolicy pagination;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final CurrentUserProvider currentUser;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    @Transactional
    @Audited(
            action = AuditConstants.ACTION_NOTIFICATION_ENQUEUED,
            entity = AuditConstants.ENTITY_NOTIFICATION)
    public NotificationResponse enqueue(NotificationCreateRequest request) {
        if (request == null) badRequest();
        if (request.getChannel() == null
                || !NotificationConstants.SUPPORTED_CHANNELS.contains(request.getChannel())
                || request.getEventKey() == null
                || request.getEventKey().isBlank()
                || request.getEventKey().length() > NotificationConstants.EVENT_KEY_MAX_LENGTH
                || request.getDestination() == null
                || request.getDestination().isBlank()
                || request.getDestination().length() > NotificationConstants.DESTINATION_MAX_LENGTH
                || request.getPayloadJson() == null
                || request.getPayloadJson().length() > NotificationConstants.PAYLOAD_MAX_LENGTH
                || (request.getSubject() != null
                        && request.getSubject().length()
                                > NotificationConstants.SUBJECT_MAX_LENGTH)
                || (request.getUserId() == null && request.getCustomerId() == null)) badRequest();
        if (request.getCustomerId() != null
                && notificationRepository.customerExists(request.getCustomerId()) == 0) notFound();
        Long linkedUser =
                request.getCustomerId() == null
                        ? null
                        : notificationRepository.customerUserId(request.getCustomerId());
        Long recipient = request.getUserId() == null ? linkedUser : request.getUserId();
        if (recipient != null && !userRepository.existsById(recipient)) notFound();
        if (request.getUserId() != null
                && linkedUser != null
                && !Objects.equals(request.getUserId(), linkedUser)) badRequest();
        if (NotificationConstants.CHANNEL_WEB.equals(request.getChannel()) && recipient == null)
            badRequest();
        if (NotificationConstants.CHANNEL_EMAIL.equals(request.getChannel())
                && !request.getDestination()
                        .matches(NotificationConstants.EMAIL_DESTINATION_PATTERN)) badRequest();
        if (NotificationConstants.CHANNEL_SMS.equals(request.getChannel())
                && !request.getDestination()
                        .matches(NotificationConstants.SMS_DESTINATION_PATTERN)) badRequest();
        if (request.getSubject() != null
                && (request.getSubject().contains("\r") || request.getSubject().contains("\n")))
            badRequest();
        String key =
                recipient != null
                        ? NotificationConstants.USER_RECIPIENT_PREFIX + recipient
                        : NotificationConstants.CUSTOMER_RECIPIENT_PREFIX + request.getCustomerId();
        String destination =
                NotificationConstants.CHANNEL_WEB.equals(request.getChannel())
                        ? key
                        : request.getDestination();
        String payload = validateAndNormalizePayload(request.getPayloadJson());
        notificationRepository.enqueueOnce(
                recipient,
                request.getCustomerId(),
                key,
                destination,
                request.getEventKey(),
                request.getChannel(),
                request.getSubject(),
                payload,
                currentUser.userIdOrNull());
        Notification notice =
                notificationRepository
                        .findByEventKeyAndChannelAndRecipientKey(
                                request.getEventKey(), request.getChannel(), key)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR));
        if (!objectMapper.readTree(notice.getPayloadJson()).equals(objectMapper.readTree(payload))
                || !Objects.equals(notice.getDestination(), destination)
                || !Objects.equals(notice.getSubject(), request.getSubject())) {
            throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
        }
        return notificationMapper.toResponse(notice);
    }

    @Override
    @Transactional
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Audited(
            action = AuditConstants.ACTION_NOTIFICATION_BROADCAST,
            entity = AuditConstants.ENTITY_NOTIFICATION)
    public NotificationBroadcastResponse broadcast(NotificationBroadcastRequest request) {
        if (request == null
                || (request.getEventKey() != null
                        && request.getEventKey().length()
                                > NotificationConstants.EVENT_KEY_MAX_LENGTH)
                || request.getSubject() == null
                || request.getSubject().isBlank()
                || request.getSubject().length() > NotificationConstants.SUBJECT_MAX_LENGTH
                || request.getSubject().contains("\r")
                || request.getSubject().contains("\n")
                || request.getPayloadJson() == null
                || request.getPayloadJson().length() > NotificationConstants.PAYLOAD_MAX_LENGTH) {
            badRequest();
        }
        String eventKey =
                request.getEventKey() == null || request.getEventKey().isBlank()
                        ? "broadcast." + UUID.randomUUID()
                        : request.getEventKey().strip();
        String payload = validateAndNormalizePayload(request.getPayloadJson());
        Long actorId = currentUser.userIdOrNull();
        Set<Role> selectedRoles = new LinkedHashSet<>();
        if (request.getTargetRoles() != null) {
            for (Role role : request.getTargetRoles()) {
                if (role != null) selectedRoles.add(role);
            }
        }
        if (selectedRoles.isEmpty() && request.getTargetRole() != null) {
            selectedRoles.add(request.getTargetRole());
        }
        List<User> recipients =
                selectedRoles.isEmpty()
                        ? userRepository.findActiveUsersForBroadcast(null)
                        : userRepository.findActiveUsersByRoles(selectedRoles);
        String cleanSubject = request.getSubject().strip();
        for (User recipient : recipients) {
            String key = NotificationConstants.USER_RECIPIENT_PREFIX + recipient.getId();
            notificationRepository.enqueueOnce(
                    recipient.getId(),
                    null,
                    key,
                    key,
                    eventKey,
                    NotificationConstants.CHANNEL_WEB,
                    cleanSubject,
                    payload,
                    actorId);
        }
        Role singleTargetRole =
                selectedRoles.size() == 1 ? selectedRoles.iterator().next() : request.getTargetRole();
        return NotificationBroadcastResponse.builder()
                .eventKey(eventKey)
                .targetRole(singleTargetRole)
                .targetRoles(selectedRoles.isEmpty() ? List.of() : List.copyOf(selectedRoles))
                .channel(NotificationConstants.CHANNEL_WEB)
                .subject(cleanSubject)
                .recipientCount(recipients.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public Page<NotificationResponse> inbox(boolean unread, Pageable pageable) {
        pagination.validate(pageable);
        return notificationRepository
                .searchInbox(currentUser.userId(), unread, pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    public Page<NotificationResponse> listAllForAdmin(
            String channel, String status, String keyword, Pageable pageable) {
        pagination.validate(pageable);
        String cleanChannel = channel == null || channel.isBlank() ? null : channel.strip();
        String cleanStatus = status == null || status.isBlank() ? null : status.strip();
        String cleanKeyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
        return notificationRepository
                .searchAdminNotifications(cleanChannel, cleanStatus, cleanKeyword, pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public NotificationResponse getNotification(Long id) {
        if (id == null || id <= 0) badRequest();
        return notificationMapper.toResponse(
                owned(
                        notificationRepository
                                .findById(id)
                                .orElseThrow(
                                        () ->
                                                new BusinessException(
                                                        ResponseCode.RESOURCE_NOT_FOUND))));
    }

    @Override
    @Transactional
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Audited(
            action = AuditConstants.ACTION_NOTIFICATION_READ,
            entity = AuditConstants.ENTITY_NOTIFICATION)
    public NotificationResponse markRead(Long id) {
        if (id == null || id <= 0) badRequest();
        Notification notice =
                owned(
                        notificationRepository
                                .findLockedById(id)
                                .orElseThrow(
                                        () ->
                                                new BusinessException(
                                                        ResponseCode.RESOURCE_NOT_FOUND)));
        if (notice.getReadAt() == null) {
            notice.setReadAt(clock.instant());
            notice.setUpdatedBy(currentUser.userId());
            notificationRepository.saveAndFlush(notice);
        }
        return notificationMapper.toResponse(notice);
    }

    @Override
    @Transactional
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Audited(
            action = AuditConstants.ACTION_NOTIFICATION_DELETED,
            entity = AuditConstants.ENTITY_NOTIFICATION,
            idArgument = 0)
    public void deleteNotification(Long id) {
        if (id == null || id <= 0) badRequest();
        Notification notice =
                notificationRepository
                        .findLockedById(id)
                        .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        notificationRepository.delete(notice);
        notificationRepository.flush();
    }

    private String validateAndNormalizePayload(String rawPayload) {
        try {
            var tree = objectMapper.readTree(rawPayload);
            if (tree == null || !tree.isObject()) badRequest();
            return objectMapper.writeValueAsString(tree);
        } catch (JacksonException malformed) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, malformed);
        }
    }

    private Notification owned(Notification notice) {
        if (!NotificationConstants.CHANNEL_WEB.equals(notice.getChannel())
                || !Objects.equals(notice.getUserId(), currentUser.userId())) {
            throw new BusinessException(ResponseCode.FORBIDDEN);
        }
        return notice;
    }

    private void badRequest() {
        throw new BusinessException(ResponseCode.BAD_REQUEST);
    }

    private void notFound() {
        throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND);
    }
}
