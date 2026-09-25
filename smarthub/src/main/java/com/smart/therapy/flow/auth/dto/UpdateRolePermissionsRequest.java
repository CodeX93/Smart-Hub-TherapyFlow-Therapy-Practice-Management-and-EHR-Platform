package com.smart.therapy.flow.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateRolePermissionsRequest {
    @NotNull(message = "Permission IDs are required")
    private List<Long> permissionIds;
}

