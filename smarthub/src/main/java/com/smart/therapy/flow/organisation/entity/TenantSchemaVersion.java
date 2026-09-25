package com.smart.therapy.flow.organisation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "tenant_schema_versions", schema = "public", uniqueConstraints = {
    @UniqueConstraint(name = "uq_tenant_schema_versions_org", columnNames = "organisation_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "organisationId")
public class TenantSchemaVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false, unique = true)
    private Long organisationId;

    @Column(name = "version", nullable = false, length = 50)
    private String version;

    @Column(name = "migrated_at")
    private Instant migratedAt;

    /** PENDING, IN_PROGRESS, SUCCESS, FAILED. */
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "SUCCESS";

    /** Last migration error when status = FAILED. */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
