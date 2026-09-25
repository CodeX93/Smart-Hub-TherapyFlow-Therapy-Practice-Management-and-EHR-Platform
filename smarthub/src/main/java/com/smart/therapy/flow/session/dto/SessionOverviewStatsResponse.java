package com.smart.therapy.flow.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Session overview statistics")
public class SessionOverviewStatsResponse {

    @Schema(description = "Scope type of this stats response", example = "THERAPIST")
    private String scope;

    @Schema(description = "Therapist ID when therapist-scoped", example = "12")
    private Long therapistId;

    @Schema(description = "Client ID when client-scoped", example = "45")
    private Long clientId;

    @Schema(description = "Timezone used to calculate date buckets", example = "Asia/Karachi")
    private String timezone;

    @Schema(description = "Total sessions in selected scope/range", example = "120")
    private Long totalSessions;

    @Schema(description = "Sessions for current day in timezone", example = "3")
    private Long todaySessions;

    @Schema(description = "Sessions for current week in timezone", example = "14")
    private Long thisWeekSessions;

    @Schema(description = "Sessions for current month in timezone", example = "48")
    private Long thisMonthSessions;

    @Schema(description = "Upcoming sessions from now", example = "10")
    private Long upcomingSessions;

    @Schema(description = "Completed sessions count", example = "72")
    private Long completedSessions;

    @Schema(description = "Cancelled/no-show sessions count", example = "8")
    private Long cancelledSessions;

    @Schema(description = "Sessions grouped by status value")
    private Map<String, Long> sessionsByStatus;

    @Schema(description = "Optional date range start filter applied", example = "2026-04-01T00:00:00Z")
    private Instant startDate;

    @Schema(description = "Optional date range end filter applied", example = "2026-04-30T23:59:59Z")
    private Instant endDate;
}
