package com.smart.therapy.flow.client.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkReassignTherapistRequest {
    @NotEmpty
    private List<Long> clientIds;
    
    @NotEmpty
    private List<Long> therapistIds;
    
    private String distribution; // 'even' or null (single therapist)
}

