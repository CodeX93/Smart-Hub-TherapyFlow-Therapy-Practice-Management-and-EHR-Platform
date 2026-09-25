package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SuperAdminCreateIncidentRequest {
    @NotBlank
    @Size(max = 200)
    private String title;
    @Size(max = 4000)
    private String description;
    @Size(max = 120)
    private String serviceName;
    @Size(max = 30)
    private String severity;
}
