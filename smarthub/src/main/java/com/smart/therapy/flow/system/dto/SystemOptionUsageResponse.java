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
public class SystemOptionUsageResponse {
    private Long optionId;
    private String optionKey;
    private String categoryKey;
    private boolean inUse;
    private long totalReferences;
    private List<UsageReference> references;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageReference {
        private String table;
        private String column;
        private long count;
    }
}
