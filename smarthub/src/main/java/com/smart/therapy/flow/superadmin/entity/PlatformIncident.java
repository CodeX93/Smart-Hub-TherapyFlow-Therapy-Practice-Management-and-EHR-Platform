package com.smart.therapy.flow.superadmin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "platform_incidents", schema = "public", indexes = {
        @Index(name = "idx_platform_incidents_status", columnList = "status"),
        @Index(name = "idx_platform_incidents_severity", columnList = "severity")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "service_name", length = 100)
    private String serviceName;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
