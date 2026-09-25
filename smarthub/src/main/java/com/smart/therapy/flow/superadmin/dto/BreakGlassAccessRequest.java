package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BreakGlassAccessRequest {

    @NotBlank
    @Size(min = 10, max = 2000)
    private String reason;

    private String resourceType;
    private String resourceId;
}
