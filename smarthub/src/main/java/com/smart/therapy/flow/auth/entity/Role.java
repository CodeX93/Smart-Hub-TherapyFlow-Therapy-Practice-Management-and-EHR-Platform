package com.smart.therapy.flow.auth.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.organisation.entity.Organisation;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, exclude = {"authIdentityRoles", "rolePermissions"})
public class Role extends BaseEntity {

    /** Optional: tenant-specific role; null = global/system role. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<AuthIdentityRole> authIdentityRoles = new HashSet<>();

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RolePermission> rolePermissions = new HashSet<>();

    public void setRolePermissions(Set<RolePermission> rolePermissions) {
        if (this.rolePermissions == null) {
            this.rolePermissions = new HashSet<>();
        }
        this.rolePermissions.clear();
        if (rolePermissions != null) {
            for (RolePermission rolePermission : rolePermissions) {
                if (rolePermission != null) {
                    rolePermission.setRole(this);
                    this.rolePermissions.add(rolePermission);
                }
            }
        }
    }
}
