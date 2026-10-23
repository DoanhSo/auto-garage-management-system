package com.Group13_65CNTT.AutoGarage.identity.entity;

import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.entity.BaseEntity;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {
    @Column(name = "identity_id", length = SecurityConstants.IDENTITY_ID_MAX_LENGTH, unique = true)
    private String identityId;

    @Column(nullable = false, length = SecurityConstants.USERNAME_MAX_LENGTH, updatable = false)
    private String username;

    @Column(length = SecurityConstants.EMAIL_MAX_LENGTH)
    private String email;

    @Column(name = "password_hash", length = SecurityConstants.PASSWORD_HASH_MAX_LENGTH)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = SecurityConstants.FULL_NAME_MAX_LENGTH)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = SecurityConstants.ROLE_MAX_LENGTH)
    private Role role;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "auth_version", nullable = false)
    private long authVersion;

    @Version
    @Column(nullable = false)
    private long version;
}
