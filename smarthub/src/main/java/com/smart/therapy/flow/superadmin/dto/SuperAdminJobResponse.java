package com.smart.therapy.flow.superadmin.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class SuperAdminJobResponse {
    private String key;
    private String description;
    private String schedule;
    private boolean enabled;
    private Boolean overrideEnabled;
    private Instant lastRunAt;
}
