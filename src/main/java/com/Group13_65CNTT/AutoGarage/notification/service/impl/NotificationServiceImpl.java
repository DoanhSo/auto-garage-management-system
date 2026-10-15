package com.Group13_65CNTT.AutoGarage.notification.service.impl;

import com.Group13_65CNTT.AutoGarage.audit.annotation.Audited;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.notification.dto.request.NotificationCreateRequest;
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
import java.util.Objects;
import java.util.Set;

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
    @Audited(action = "NOTIFICATION_ENQUEUED", entity = "NOTIFICATION")
    public NotificationResponse enqueue(NotificationCreateRequest request) {
        if (request == null) badRequest();
        if (request.getChannel() == null
                || !Set.of("WEB", "EMAIL", "SMS").contains(request.getChannel())
                || request.getEventKey() == null
                || request.getEventKey().isBlank()
                || request.getEventKey().length() > 150
                || request.getDestination() == null
                || request.getDestination().isBlank()
                || request.getDestination().length() > 254
                || request.getPayloadJson() == null
                || request.getPayloadJson().length() > 16384
                || (request.getSubject() != null && request.getSubject().length() > 255)
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
        if ("WEB".equals(request.getChannel()) && recipient == null) badRequest();
        if ("EMAIL".equals(request.getChannel())
                && !request.getDestination().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) badRequest();
        if ("SMS".equals(request.getChannel())
                && !request.getDestination().matches("\\+[1-9][0-9]{7,14}")) badRequest();
        if (request.getSubject() != null
                && (request.getSubject().contains("\r") || request.getSubject().contains("\n")))
            badRequest();
        String key =
                recipient != null ? "user:" + recipient : "customer:" + request.getCustomerId();
        String destination = "WEB".equals(request.getChannel()) ? key : request.getDestination();
        String payload;
        try {
            var tree = objectMapper.readTree(request.getPayloadJson());
            if (tree == null || !tree.isObject()) badRequest();
            payload = objectMapper.writeValueAsString(tree);
        } catch (JacksonException malformed) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, malformed);
        }
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
    @Audited(action = "NOTIFICATION_READ", entity = "NOTIFICATION")
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

    private Notification owned(Notification notice) {
        if (!"WEB".equals(notice.getChannel())
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
