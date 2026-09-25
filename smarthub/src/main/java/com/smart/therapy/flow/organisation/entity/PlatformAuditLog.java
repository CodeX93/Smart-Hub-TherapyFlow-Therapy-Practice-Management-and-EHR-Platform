package com.smart.therapy.flow.organisation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "platform_audit_logs", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth_id")
    private Long authId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
