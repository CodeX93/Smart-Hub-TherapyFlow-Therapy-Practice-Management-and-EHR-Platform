package com.smart.therapy.flow.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TherapistDashboardSummaryResponse {

    private ClientCard client;
    private SessionCard session;
    private TaskCard task;
    private BillingCard billing;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientCard {
        private Long active;
        private Long total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionCard {
        private Long scheduledToday;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskCard {
        private Long pending;
        private Long urgent;
        private Long total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingCard {
        /** Total billed for sessions in the practice-timezone current month (legacy / secondary). */
        private BigDecimal estimatedThisMonth;
        /** Paid amount for sessions in the practice-timezone current month. */
        private BigDecimal totalCollected;
        /** Outstanding amount for sessions in the practice-timezone current month. */
        private BigDecimal outstandingBalance;
    }
}
