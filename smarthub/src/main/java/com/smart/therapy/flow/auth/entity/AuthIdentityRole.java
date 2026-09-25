package com.smart.therapy.flow.auth.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.organisation.entity.Organisation;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Roles attached to identity, optionally scoped to an organisation (public schema).
 * organisation_id NULL = platform-level role.
 */
@Entity
@AttributeOverrides({
    @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false, updatable = false)),
    @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false))
})
@Table(name = "auth_identity_roles", schema = "public", indexes = {
    @Index(name = "idx_auth_roles_auth", columnList = "auth_id"),
    @Index(name = "idx_auth_identity_roles_organisation", columnList = "organisation_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AuthIdentityRole extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auth_id", nullable = false)
    private AuthIdentity authIdentity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
}
