package com.smart.therapy.flow.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectoryRoleResponse {
    private Long roleId;
    private String role;
    private String displayName;
}
