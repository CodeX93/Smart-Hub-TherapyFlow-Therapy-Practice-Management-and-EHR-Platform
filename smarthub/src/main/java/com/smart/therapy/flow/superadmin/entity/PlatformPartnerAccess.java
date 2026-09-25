package com.smart.therapy.flow.superadmin.entity;

import com.smart.therapy.flow.organisation.entity.Organisation;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "platform_partner_access", schema = "public", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"partner_id", "organisation_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformPartnerAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false, length = 100)
    private String partnerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @Column(name = "feature_flags_json", nullable = false, columnDefinition = "TEXT")
    private String featureFlagsJson;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
