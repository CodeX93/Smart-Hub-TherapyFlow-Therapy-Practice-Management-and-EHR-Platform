package com.smart.therapy.flow.session.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ConflictCheckResponse {

    private Boolean hasConflicts;
    private List<ConflictInfo> therapistConflicts;
    private List<ConflictInfo> roomConflicts;
    private List<ConflictInfo> clientConflicts;
}

