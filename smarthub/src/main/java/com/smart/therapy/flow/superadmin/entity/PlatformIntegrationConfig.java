package com.smart.therapy.flow.superadmin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "platform_integration_configs", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformIntegrationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "integration_key", nullable = false, unique = true, length = 100)
    private String integrationKey;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "masked_summary", columnDefinition = "TEXT")
    private String maskedSummary;

    @Column(name = "updated_by_auth_id")
    private Long updatedByAuthId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
