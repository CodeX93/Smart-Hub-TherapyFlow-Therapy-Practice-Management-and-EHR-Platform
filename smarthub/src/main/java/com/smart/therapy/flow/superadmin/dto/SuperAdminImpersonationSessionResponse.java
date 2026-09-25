package com.smart.therapy.flow.superadmin.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class SuperAdminImpersonationSessionResponse {
    private Long sessionId;
    private Long superAdminAuthId;
    private Long targetAuthId;
    private Long organisationId;
    private String status;
    private Instant startedAt;
    private Instant expiresAt;
    private Instant endedAt;
    private String reason;
}
