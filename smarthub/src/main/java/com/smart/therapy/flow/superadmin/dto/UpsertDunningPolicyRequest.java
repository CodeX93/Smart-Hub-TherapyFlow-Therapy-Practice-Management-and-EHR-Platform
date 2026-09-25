package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "Update dunning policy request")
@Data
public class UpsertDunningPolicyRequest {
    @NotEmpty
    @Valid
    private List<DunningStepRequest> steps;

    @NotNull
    @Min(1)
    private Integer gracePeriodDays;

    @NotNull
    @Min(1)
    private Integer trialNoticeDays;

    @Data
    public static class DunningStepRequest {
        @NotNull
        @Min(1)
        private Integer day;

        @NotNull
        private Action action;
    }

    public enum Action {
        email_reminder,
        auto_suspend,
        cancel
    }
}
