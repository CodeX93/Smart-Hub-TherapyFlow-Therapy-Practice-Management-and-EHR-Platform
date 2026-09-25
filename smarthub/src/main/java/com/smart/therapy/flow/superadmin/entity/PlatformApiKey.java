package com.smart.therapy.flow.superadmin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "platform_api_keys", schema = "public", indexes = {
        @Index(name = "idx_platform_api_keys_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_name", nullable = false, length = 120)
    private String keyName;

    @Column(name = "key_prefix", nullable = false, unique = true, length = 24)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, length = 128)
    private String keyHash;

    @Column(name = "scopes_json", nullable = false, columnDefinition = "TEXT")
    private String scopesJson;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "created_by_auth_id")
    private Long createdByAuthId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;
}
