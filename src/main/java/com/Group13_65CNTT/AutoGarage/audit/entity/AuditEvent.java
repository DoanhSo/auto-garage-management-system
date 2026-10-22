package com.Group13_65CNTT.AutoGarage.audit.entity;

import com.Group13_65CNTT.AutoGarage.common.constant.AuditConstants;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** Append-only audit has its own lifecycle, rather than mutable BaseEntity metadata. */
@Entity
@Table(name = "audit_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(nullable = false, length = AuditConstants.ACTION_MAX_LENGTH)
    private String action;

    @Column(name = "entity_type", nullable = false, length = AuditConstants.ENTITY_TYPE_MAX_LENGTH)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_json", columnDefinition = "json")
    private String afterJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "request_key", length = AuditConstants.REQUEST_KEY_MAX_LENGTH)
    private String requestKey;

    @Column(name = "request_hash", length = AuditConstants.REQUEST_HASH_LENGTH)
    private String requestHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json", columnDefinition = "json")
    private String resultJson;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
