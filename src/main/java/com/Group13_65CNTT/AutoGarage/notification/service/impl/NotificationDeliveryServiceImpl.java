package com.Group13_65CNTT.AutoGarage.notification.service.impl;

import com.Group13_65CNTT.AutoGarage.common.config.NotificationProperties;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.exception.NotificationDeliveryException;
import com.Group13_65CNTT.AutoGarage.notification.entity.Notification;
import com.Group13_65CNTT.AutoGarage.notification.service.NotificationDeliveryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationDeliveryServiceImpl implements NotificationDeliveryService {
    private final NotificationProperties properties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ObjectMapper objectMapper;
    private final HttpClient smsHttpClient;

    @Override
    public String deliver(Notification notification) {
        return switch (notification.getChannel()) {
            case "WEB" -> "web:" + notification.getId();
            case "EMAIL" -> sendEmail(notification);
            case "SMS" -> sendSms(notification);
            default -> throw new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR);
        };
    }

    private String sendEmail(Notification notification) {
        if (!properties.isEmailEnabled()) throw new NotificationDeliveryException();
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) throw new NotificationDeliveryException();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getEmailFrom());
        message.setTo(notification.getDestination());
        message.setSubject(
                notification.getSubject() == null
                        ? properties.getDefaultSubject()
                        : notification.getSubject());
        message.setText(notification.getPayloadJson());
        try {
            sender.send(message);
        } catch (MailException failure) {
            throw new NotificationDeliveryException(failure);
        }
        return "smtp:" + notification.getId();
    }

    private String sendSms(Notification notification) {
        if (!properties.isSmsEnabled()) throw new NotificationDeliveryException();
        String idempotencyKey = "notification:" + notification.getId();
        String body =
                objectMapper.writeValueAsString(
                        Map.of(
                                "destination",
                                notification.getDestination(),
                                "payload",
                                objectMapper.readTree(notification.getPayloadJson()),
                                "idempotencyKey",
                                idempotencyKey));
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(properties.getSmsUrl()))
                        .timeout(properties.getRequestTimeout())
                        .header("Authorization", "Bearer " + properties.getSmsToken())
                        .header("Content-Type", "application/json")
                        .header("Idempotency-Key", idempotencyKey)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
        try {
            HttpResponse<Void> response =
                    smsHttpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new NotificationDeliveryException();
            return idempotencyKey;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new NotificationDeliveryException(interrupted);
        } catch (IOException unavailable) {
            throw new NotificationDeliveryException(unavailable);
        }
    }
}
