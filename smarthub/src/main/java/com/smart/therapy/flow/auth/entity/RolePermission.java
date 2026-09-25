package com.smart.therapy.flow.auth.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * RolePermission - joins roles and permissions (many-to-many mapping).
 */
@Entity
@Table(name = "role_permissions", schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"role_id", "permission_id"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = {"role", "permission"})
public class RolePermission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;
}
