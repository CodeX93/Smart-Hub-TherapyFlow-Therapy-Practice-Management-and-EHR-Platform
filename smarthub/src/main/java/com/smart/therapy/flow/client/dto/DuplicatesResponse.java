package com.smart.therapy.flow.client.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DuplicatesResponse {
    private List<DuplicateGroupResponse> duplicateGroups;
    private Integer totalDuplicates;
}

