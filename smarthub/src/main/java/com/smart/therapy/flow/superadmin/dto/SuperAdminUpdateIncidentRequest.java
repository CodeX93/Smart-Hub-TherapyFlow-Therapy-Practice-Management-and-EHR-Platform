package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class SuperAdminUpdateIncidentRequest {
    @Size(max = 30)
    private String status;
    @Size(max = 30)
    private String severity;
    @Size(max = 4000)
    private String description;
    private Instant resolvedAt;
}
