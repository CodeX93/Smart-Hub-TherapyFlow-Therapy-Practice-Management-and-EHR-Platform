package com.smart.therapy.flow.superadmin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "platform_api_key_scopes", schema = "public",
        uniqueConstraints = @UniqueConstraint(name = "uq_platform_api_key_scopes_key_scope", columnNames = {"api_key_id", "scope"}),
        indexes = {
                @Index(name = "idx_platform_api_key_scopes_key", columnList = "api_key_id"),
                @Index(name = "idx_platform_api_key_scopes_scope", columnList = "scope")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformApiKeyScope {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "api_key_id", nullable = false)
    private PlatformApiKey apiKey;

    @Column(name = "scope", nullable = false, length = 120)
    private String scope;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

