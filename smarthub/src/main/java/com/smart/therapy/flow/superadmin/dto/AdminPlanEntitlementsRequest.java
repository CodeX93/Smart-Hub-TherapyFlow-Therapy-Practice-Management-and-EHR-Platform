package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AdminPlanEntitlementsRequest {
    @NotEmpty
    @Valid
    private List<PlanFeatureItem> features;

    @Data
    public static class PlanFeatureItem {
        @jakarta.validation.constraints.NotBlank
        private String key;
        @jakarta.validation.constraints.NotNull
        private Boolean enabled;
        private Integer limit;
    }
}
