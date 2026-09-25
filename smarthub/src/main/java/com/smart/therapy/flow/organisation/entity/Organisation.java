package com.smart.therapy.flow.organisation.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Tenant: parent of users, clients, forms, rooms, etc. Data isolation per org.
 * Lives in public schema. Tenant-scoped entities have no organisation_id (schema-per-tenant).
 */
@Entity
@AttributeOverrides({
    @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false, updatable = false)),
    @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false))
})
@Table(name = "organisations", schema = "public", indexes = {
    @Index(name = "idx_organisations_slug", columnList = "slug"),
    @Index(name = "idx_organisations_status", columnList = "status"),
    @Index(name = "idx_organisations_subdomain", columnList = "subdomain"),
    @Index(name = "idx_organisations_schema_name", columnList = "schema_name")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_organisations_slug", columnNames = "slug"),
    @UniqueConstraint(name = "uq_organisations_subdomain", columnNames = "subdomain"),
    @UniqueConstraint(name = "uq_organisations_schema_name", columnNames = "schema_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Organisation extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    /** Subdomain for this tenant (e.g. clinicA from clinicA.yourapp.com). Immutable after creation. */
    @Column(name = "subdomain", unique = true, length = 63, updatable = false)
    private String subdomain;

    /** Schema name for this tenant (e.g. tenant_42). Physical isolation: all tenant data lives here. */
    @Column(name = "schema_name", unique = true, nullable = false, length = 63)
    private String schemaName;

    /** Last successful backup timestamp (for super-admin visibility). */
    @Column(name = "last_backup_at")
    private java.time.Instant lastBackupAt;

    /** Backup status: PENDING, IN_PROGRESS, SUCCESS, FAILED. */
    @Column(name = "backup_status", length = 30)
    private String backupStatus;

    /** Backup storage location or path (e.g. s3 key). */
    @Column(name = "backup_location", length = 500)
    private String backupLocation;

    /** INDIVIDUAL, CLINIC, ENTERPRISE — plan/UI/SSO gating. */
    @Column(name = "organisation_type", length = 30, nullable = false)
    @Builder.Default
    private String organisationType = "CLINIC";

    /** Preferred IANA timezone for tenant operations (e.g. Asia/Karachi, Europe/Stockholm). */
    @Column(name = "timezone", length = 64)
    private String timezone;

    /** Cloud/operational region for tenant infrastructure (e.g. eu-north-1). */
    @Column(name = "region", length = 100)
    private String region;

    /** Data residency policy label (e.g. EU, US, PK). */
    @Column(name = "data_residency", length = 30)
    private String dataResidency;

    /** Locale for tenant UI (e.g. en-US). */
    @Column(name = "locale", length = 20)
    private String locale;

    /** Branding logo URL (HTTPS). */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /** Primary brand color (hex). */
    @Column(name = "brand_primary_color", length = 16)
    private String brandPrimaryColor;

    /** Secondary brand color (hex). */
    @Column(name = "brand_secondary_color", length = 16)
    private String brandSecondaryColor;

    /** Accent brand color (hex). */
    @Column(name = "brand_accent_color", length = 16)
    private String brandAccentColor;

    /** Support contact email (branding). */
    @Column(name = "support_email", length = 255)
    private String supportEmail;

    /** Support/address line (free text). */
    @Column(name = "support_address", length = 500)
    private String supportAddress;

    /** Tenant-specific Stripe publishable key (pk_*). */
    @Column(name = "stripe_publishable_key", length = 255)
    private String stripePublishableKey;

    /** Tenant-specific Stripe secret key (encrypted). */
    @Column(name = "stripe_secret_key_encrypted", columnDefinition = "TEXT")
    private String stripeSecretKeyEncrypted;

    /** Tenant-specific Stripe webhook signing secret (encrypted). */
    @Column(name = "stripe_webhook_secret_encrypted", columnDefinition = "TEXT")
    private String stripeWebhookSecretEncrypted;

    /** Tenant-specific Stripe webhook endpoint URL configured in Stripe dashboard. */
    @Column(name = "stripe_webhook_endpoint_url", length = 500)
    private String stripeWebhookEndpointUrl;

    /** Effective timestamp for scheduled termination (internal ARCHIVED lifecycle state). */
    @Column(name = "termination_effective_at")
    private java.time.Instant terminationEffectiveAt;

    /** If set, tenant is disabled (e.g. compromised). Checked first in TenantFilter. */
    @Column(name = "force_disabled_reason", columnDefinition = "TEXT")
    private String forceDisabledReason;
}
