package com.smart.therapy.flow.auth.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.organisation.entity.Organisation;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "permissions", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, exclude = {"rolePermissions"})
public class Permission extends BaseEntity {

    /** Optional: tenant-specific permission; null = platform/global permission. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String category; // client_management, scheduling, etc.

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "permission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RolePermission> rolePermissions = new HashSet<>();
}
