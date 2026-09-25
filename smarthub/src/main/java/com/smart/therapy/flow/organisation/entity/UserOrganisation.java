package com.smart.therapy.flow.organisation.entity;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Links an auth identity (public.auth_identities) to organisations they can access.
 * Lives in public schema. Used for: which tenants can this user switch to?
 */
@Entity
@Table(name = "user_organisations", schema = "public", indexes = {
    @Index(name = "idx_user_organisations_auth", columnList = "auth_id"),
    @Index(name = "idx_user_organisations_org", columnList = "organisation_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_user_organisations_auth_org", columnNames = {"auth_id", "organisation_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"auth", "organisation"})
public class UserOrganisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auth_id", nullable = false)
    private AuthIdentity auth;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
