package com.Group13_65CNTT.AutoGarage.notification.entity;

import com.Group13_65CNTT.AutoGarage.common.entity.BaseEntity;

import jakarta.persistence.*;

import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "recipient_key", nullable = false, length = 100)
    private String recipientKey;

    @Column(nullable = false, length = 254)
    private String destination;

    @Column(name = "event_key", nullable = false, length = 150)
    private String eventKey;

    @Column(nullable = false, length = 10)
    private String channel;

    @Column(length = 255)
    private String subject;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "json")
    private String payloadJson;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Builder.Default
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "provider_reference", length = 150)
    private String providerReference;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Version private long version;
}
