package com.smart.therapy.flow.organisation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Allowed email domains for organisation SSO login.
 */
@Entity
@Table(name = "organisation_sso_allowed_domains", schema = "public", uniqueConstraints = {
    @UniqueConstraint(name = "uq_org_sso_allowed_domains_org_domain", columnNames = {"organisation_id", "domain"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganisationSsoAllowedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "domain", nullable = false, length = 255)
    private String domain;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

