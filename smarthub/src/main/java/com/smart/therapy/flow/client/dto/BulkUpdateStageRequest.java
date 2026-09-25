package com.smart.therapy.flow.client.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkUpdateStageRequest {
    @NotEmpty
    private List<Long> clientIds;
    
    @NotNull
    private String stage; // intake, assessment, psychotherapy, maintenance, discharged
}

