package com.smart.therapy.flow.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantPermissionPolicyResponse {
    private boolean tenantCustomPermissionsEnabled;
    private List<String> tenantAssignableSystemPermissions;
    private List<String> blockedSystemPermissions;
}
