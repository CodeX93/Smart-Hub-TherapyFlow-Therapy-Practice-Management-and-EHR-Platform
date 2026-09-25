package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SuperAdminSuspendRequest {
    @Size(min = 5, max = 500)
    private String reason;
}
