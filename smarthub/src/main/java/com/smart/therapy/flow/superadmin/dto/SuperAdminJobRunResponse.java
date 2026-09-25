package com.smart.therapy.flow.superadmin.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class SuperAdminJobRunResponse {
    private String key;
    private String status;
    private Instant ranAt;
}
