package com.smart.therapy.flow.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUserActivityLogRequest {
    @NotNull
    private String activityType; // Updated to match entity field name
    
    private String description; // Updated to match entity field name
    
    private String ipAddress;
    
    private String userAgent;
}

