package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "Dunning policy response")
@Data
public class DunningPolicyResponse {
    private List<DunningStep> steps;
    private Integer gracePeriodDays;
    private Integer trialNoticeDays;

    @Data
    public static class DunningStep {
        private Integer day;
        private String action;
    }
}
