package com.smart.therapy.flow.subscription.entity;

import com.smart.therapy.flow.organisation.entity.Organisation;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "billing_contacts", schema = "public", indexes = {
        @Index(name = "idx_billing_contacts_org_active", columnList = "organisation_id, is_active, is_primary")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class BillingContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
