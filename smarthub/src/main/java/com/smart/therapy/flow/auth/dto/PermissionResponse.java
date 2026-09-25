package com.smart.therapy.flow.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {
    private Long id;
    private String name;
    private String displayName;
    private String description;
    private String category;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}

