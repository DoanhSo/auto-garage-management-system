package com.Group13_65CNTT.AutoGarage.notification.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.Group13_65CNTT.AutoGarage.common.config.NotificationProperties;
import com.Group13_65CNTT.AutoGarage.notification.entity.Notification;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.*;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@DisplayName("Outbox worker sends outside the claim transaction")
class OutboxWorkerTest {
    @Mock OutboxService outbox;
    @Mock NotificationDeliveryService delivery;
    private final NotificationProperties properties = new NotificationProperties();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-15T01:00:00Z"), ZoneOffset.UTC);
    private OutboxWorker worker;

    @BeforeEach
    void initialize() {
        worker = new OutboxWorker(outbox, delivery, properties, clock);
    }

    @Test
    void completionDatabaseFailureIsNotMisclassifiedAsProviderFailure() {
        properties.setWorkerEnabled(true);
        Notification notice = notice(clock.instant().plusSeconds(60));
        when(outbox.claim(anyString())).thenReturn(List.of(notice));
        when(delivery.deliver(notice)).thenReturn("provider:1");
        doThrow(new DataAccessResourceFailureException("test database unavailable"))
                .when(outbox)
                .complete(eq(1L), anyString(), eq("provider:1"));
        worker.dispatch();
        verify(outbox, never()).fail(any(), anyString());
    }

    @Test
    void disabledWorkerDoesNotClaimOrSend() {
        worker.dispatch();
        verifyNoInteractions(outbox, delivery);
    }

    @Test
    void providerFailureIsRetriedWithoutLoggingPayload() {
        properties.setWorkerEnabled(true);
        Notification notice = notice(clock.instant().plusSeconds(60));
        when(outbox.claim(anyString())).thenReturn(List.of(notice));
        when(delivery.deliver(notice))
                .thenThrow(new IllegalStateException("test provider failure"));
        worker.dispatch();
        verify(outbox).fail(eq(1L), anyString());
        verify(outbox, never()).complete(any(), anyString(), anyString());
    }

    @Test
    void successfulDeliveryCompletesTheSameLease() {
        properties.setWorkerEnabled(true);
        Notification notice = notice(clock.instant().plusSeconds(60));
        when(outbox.claim(anyString())).thenReturn(List.of(notice));
        when(delivery.deliver(notice)).thenReturn("provider:1");
        worker.dispatch();
        var lease = ArgumentCaptor.forClass(String.class);
        verify(outbox).claim(lease.capture());
        verify(outbox).complete(1L, lease.getValue(), "provider:1");
    }

    @Test
    void aLeaseExpiredBeforeSendingCannotTriggerProviderIo() {
        properties.setWorkerEnabled(true);
        when(outbox.claim(anyString()))
                .thenReturn(List.of(notice(clock.instant().minusSeconds(1))));
        worker.dispatch();
        verifyNoInteractions(delivery);
    }

    private Notification notice(Instant until) {
        Notification notice = Notification.builder().channel("WEB").lockedUntil(until).build();
        notice.setId(1L);
        return notice;
    }
}
