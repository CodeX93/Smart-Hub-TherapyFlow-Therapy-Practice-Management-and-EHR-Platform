package com.smart.therapy.flow.organisation.entity;

import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * SSO configuration per organisation (tenant). Stored in public schema.
 * Super-admin configures client_id / client_secret per org; redirect_uri can be built from request.
 */
@Entity
@Table(name = "organisation_sso_configs", schema = "public", uniqueConstraints = {
    @UniqueConstraint(name = "uq_organisation_sso_configs_org_provider", columnNames = {"organisation_id", "provider"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganisationSsoConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "client_id", nullable = false, length = 255)
    private String clientId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "client_secret", length = 500)
    private String clientSecret;

    @Column(name = "redirect_uri", length = 500)
    private String redirectUri;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
