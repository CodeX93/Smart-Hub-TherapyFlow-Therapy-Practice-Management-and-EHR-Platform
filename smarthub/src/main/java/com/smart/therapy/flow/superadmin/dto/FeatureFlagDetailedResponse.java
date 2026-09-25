package com.smart.therapy.flow.superadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlagDetailedResponse {
    private String key;
    private String module;
    private List<String> accessControlList;
    private boolean enabled;
    private Integer usageLimit;
    private Map<String, Object> usageLimitations;
}

