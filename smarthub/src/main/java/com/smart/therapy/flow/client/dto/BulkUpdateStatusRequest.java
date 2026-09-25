package com.smart.therapy.flow.client.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkUpdateStatusRequest {
    @NotEmpty
    private List<Long> clientIds;
    
    @NotNull
    private String status; // active, inactive, pending, discharged
}

