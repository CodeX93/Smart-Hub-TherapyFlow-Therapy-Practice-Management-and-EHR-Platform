package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class SuperAdminNotificationRequest {
    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 8000)
    private String message;

    @Size(max = 40)
    private String channel;

    private Instant scheduledAt;
    private List<Long> organisationIds;
}
