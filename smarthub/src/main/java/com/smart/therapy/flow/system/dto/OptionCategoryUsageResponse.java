package com.smart.therapy.flow.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionCategoryUsageResponse {
    private Long categoryId;
    private String categoryKey;
    private String categoryName;
    private long optionCount;
    private boolean inUse;
    private long totalReferences;
    private List<SystemOptionUsageResponse> optionUsage;
}
