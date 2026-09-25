package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SuperAdminUpsertNotificationTemplateRequest {
    @NotBlank
    @Size(max = 250)
    private String subjectTemplate;

    @NotBlank
    @Size(max = 20000)
    private String bodyTemplate;

    private Boolean active;
}
