package com.smart.therapy.flow.superadmin.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class SuperAdminBackupJobResponse {
    private Long jobId;
    private Long organisationId;
    private Long requestedByAuthId;
    private String status;
    private String storageLocation;
    private String errorMessage;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
}
