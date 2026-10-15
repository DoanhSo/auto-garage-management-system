package com.Group13_65CNTT.AutoGarage.notification.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.Group13_65CNTT.AutoGarage.common.config.NotificationProperties;
import com.Group13_65CNTT.AutoGarage.common.exception.NotificationDeliveryException;
import com.Group13_65CNTT.AutoGarage.notification.entity.Notification;
import com.Group13_65CNTT.AutoGarage.notification.service.impl.NotificationDeliveryServiceImpl;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification delivery respects configured channels")
class NotificationDeliveryServiceTest {
    @Mock ObjectProvider<JavaMailSender> senderProvider;
    @Mock JavaMailSender sender;
    @Mock HttpClient smsClient;
    @Mock HttpResponse<Void> smsResponse;
    private final NotificationProperties properties = new NotificationProperties();
    private NotificationDeliveryServiceImpl delivery;

    @BeforeEach
    void initialize() {
        delivery =
                new NotificationDeliveryServiceImpl(
                        properties, senderProvider, new ObjectMapper(), smsClient);
    }

    @Test
    void emailFailurePreservesCauseAndReturnsSafeMessage() {
        properties.setEmailEnabled(true);
        properties.setEmailFrom("garage@example.test");
        when(senderProvider.getIfAvailable()).thenReturn(sender);
        var cause = new MailSendException("smtp-password=private");
        doThrow(cause).when(sender).send(any(SimpleMailMessage.class));
        assertThatThrownBy(() -> delivery.deliver(notice("EMAIL")))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasCause(cause)
                .hasMessage("Notification provider is unavailable");
    }

    @Test
    void networkFailurePreservesCauseWithoutReturningGatewayDetails() throws Exception {
        properties.setSmsEnabled(true);
        properties.setSmsUrl("https://gateway.example.test/messages");
        properties.setSmsToken("test-only-key");
        var cause = new IOException("Authorization=Bearer private");
        when(smsClient.send(
                        any(HttpRequest.class),
                        ArgumentMatchers.<HttpResponse.BodyHandler<Void>>any()))
                .thenThrow(cause);
        assertThatThrownBy(() -> delivery.deliver(notice("SMS")))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasCause(cause)
                .hasMessage("Notification provider is unavailable");
    }

    @Test
    void interruptionIsPreservedSoTheWorkerCanStop() throws Exception {
        properties.setSmsEnabled(true);
        properties.setSmsUrl("https://gateway.example.test/messages");
        properties.setSmsToken("test-only-key");
        var cause = new InterruptedException("test interruption");
        when(smsClient.send(
                        any(HttpRequest.class),
                        ArgumentMatchers.<HttpResponse.BodyHandler<Void>>any()))
                .thenThrow(cause);
        try {
            assertThatThrownBy(() -> delivery.deliver(notice("SMS"))).hasCause(cause);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void webDeliveryNeedsNoExternalProvider() {
        assertThat(delivery.deliver(notice("WEB"))).isEqualTo("web:1");
        verifyNoInteractions(senderProvider, sender);
    }

    @Test
    void disabledEmailNeverSendsOrPretendsSuccess() {
        assertThatThrownBy(() -> delivery.deliver(notice("EMAIL")))
                .isInstanceOf(NotificationDeliveryException.class);
        verifyNoInteractions(sender);
    }

    @Test
    void configuredEmailUsesOnlyItsExplicitSenderDestinationAndPayload() {
        properties.setEmailEnabled(true);
        properties.setEmailFrom("garage@example.test");
        when(senderProvider.getIfAvailable()).thenReturn(sender);
        assertThat(delivery.deliver(notice("EMAIL"))).isEqualTo("smtp:1");
        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertThat(message.getValue().getFrom()).isEqualTo("garage@example.test");
        assertThat(message.getValue().getTo()).containsExactly("customer@example.test");
        assertThat(message.getValue().getText()).isEqualTo("{\"message\":\"hello\"}");
    }

    @Test
    void disabledSmsDoesNotCallAnyGateway() {
        assertThatThrownBy(() -> delivery.deliver(notice("SMS")))
                .isInstanceOf(NotificationDeliveryException.class);
    }

    @Test
    void enablingSmsRequiresHttpsAndAnExplicitSecret() {
        properties.setSmsEnabled(true);
        properties.setSmsUrl("http://example.test/sms");
        properties.setSmsToken("test-only");
        assertThat(properties.isPolicyValid()).isFalse();
        properties.setSmsUrl("https://example.test/sms");
        properties.setSmsToken("");
        assertThat(properties.isPolicyValid()).isFalse();
        properties.setSmsToken("test-only");
        assertThat(properties.isPolicyValid()).isTrue();
    }

    @Test
    void configuredSmsSendsStableIdempotencyAndAuthorizationHeaders() throws Exception {
        properties.setRequestTimeout(Duration.ofSeconds(17));
        properties.setSmsEnabled(true);
        properties.setSmsUrl("https://gateway.example.test/messages");
        properties.setSmsToken("test-only-key");
        when(smsResponse.statusCode()).thenReturn(202);
        when(smsClient.send(
                        any(HttpRequest.class),
                        ArgumentMatchers.<HttpResponse.BodyHandler<Void>>any()))
                .thenReturn(smsResponse);
        assertThat(delivery.deliver(notice("SMS"))).isEqualTo("notification:1");
        var request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(smsClient)
                .send(request.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<Void>>any());
        assertThat(request.getValue().uri().toString())
                .isEqualTo("https://gateway.example.test/messages");
        assertThat(request.getValue().headers().firstValue("Authorization"))
                .contains("Bearer test-only-key");
        assertThat(request.getValue().headers().firstValue("Idempotency-Key"))
                .contains("notification:1");
        assertThat(request.getValue().timeout()).contains(Duration.ofSeconds(17));
    }

    @Test
    void rejectedSmsCannotBeMarkedSuccessfullyDelivered() throws Exception {
        properties.setSmsEnabled(true);
        properties.setSmsUrl("https://gateway.example.test/messages");
        properties.setSmsToken("test-only-key");
        when(smsResponse.statusCode()).thenReturn(503);
        when(smsClient.send(
                        any(HttpRequest.class),
                        ArgumentMatchers.<HttpResponse.BodyHandler<Void>>any()))
                .thenReturn(smsResponse);
        assertThatThrownBy(() -> delivery.deliver(notice("SMS")))
                .isInstanceOf(NotificationDeliveryException.class);
    }

    private Notification notice(String channel) {
        Notification notice =
                Notification.builder()
                        .channel(channel)
                        .destination("customer@example.test")
                        .subject("Reminder")
                        .payloadJson("{\"message\":\"hello\"}")
                        .build();
        notice.setId(1L);
        return notice;
    }
}
