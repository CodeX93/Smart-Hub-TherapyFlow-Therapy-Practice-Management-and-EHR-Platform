package com.smart.therapy.flow.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectoryEntryResponse {
    private Long id;
    private String entityType;
    private String role;
    private Long roleId;
    private List<DirectoryRoleResponse> roles;

    private String name;
    private String email;
    private String username;
    private String phone;
    private Boolean active;
    private String status;

    private String profilePicture;
    private String clientId;
    private Long assignedTherapistId;
    private String assignedTherapistName;

    private Instant lastLogin;
    private Instant createdAt;
    private Instant updatedAt;

    private Map<String, Object> otherFields;
}
