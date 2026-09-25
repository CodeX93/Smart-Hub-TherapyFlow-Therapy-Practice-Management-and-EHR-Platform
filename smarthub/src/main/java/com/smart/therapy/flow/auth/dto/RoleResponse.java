package com.smart.therapy.flow.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {
    private Long id;
    private String name;
    private String displayName;
    private String description;
    private Boolean isSystem;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private List<PermissionResponse> permissions;
    private Integer permissionsAssignedCount;
    private Integer userCount;
}

