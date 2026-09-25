package com.smart.therapy.flow.superadmin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "platform_tenant_routing_settings", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformTenantRoutingSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_auto_routing", nullable = false)
    private Boolean emailAutoRouting;

    @Column(name = "path_based_routing", nullable = false)
    private Boolean pathBasedRouting;

    @Column(name = "path_prefix", nullable = false, length = 100)
    private String pathPrefix;

    @Column(name = "org_identifier", nullable = false, length = 20)
    private String orgIdentifier;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by_auth_id")
    private Long updatedByAuthId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

