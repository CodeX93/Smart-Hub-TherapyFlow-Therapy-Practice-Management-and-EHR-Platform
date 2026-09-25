package com.smart.therapy.flow.client.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Portal preferences for a client. Auth (login) is in AuthIdentity; this table holds only portal settings.
 * Table: client_portal_settings (after V31 migration from client_credentials).
 */
@Entity
@Table(name = "client_portal_settings", indexes = {
    @Index(name = "idx_client_portal_settings_client", columnList = "client_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ClientPortalSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false, unique = true)
    @JsonIgnore
    private Client client;

    @Column(name = "has_portal_access")
    @Builder.Default
    private Boolean hasPortalAccess = false;

    @Column(name = "is_activated")
    @Builder.Default
    private Boolean isActivated = false;

    @Column(name = "activated_at")
    private java.time.Instant activatedAt;

    @Column(name = "email_notifications")
    @Builder.Default
    private Boolean emailNotifications = true;

    @Column(name = "sms_notifications")
    @Builder.Default
    private Boolean smsNotifications = false;

    @Column(name = "push_notifications")
    @Builder.Default
    private Boolean pushNotifications = false;

    @Column(name = "portal_notes", columnDefinition = "TEXT")
    private String portalNotes;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    /** When the client last asked their therapist to enable online sessions. Drives the cooldown. */
    @Column(name = "online_booking_requested_at")
    private java.time.Instant onlineBookingRequestedAt;

    /** When that request last went out in a therapist digest. Older than the request means pending. */
    @Column(name = "online_booking_notified_at")
    private java.time.Instant onlineBookingNotifiedAt;

    public boolean isPortalAccessEnabled() {
        return Boolean.TRUE.equals(this.hasPortalAccess)
            && Boolean.TRUE.equals(this.isActivated);
    }
}
