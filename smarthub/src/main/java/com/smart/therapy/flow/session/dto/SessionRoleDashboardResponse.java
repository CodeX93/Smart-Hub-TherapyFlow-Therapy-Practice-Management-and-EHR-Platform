package com.smart.therapy.flow.session.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SessionRoleDashboardResponse {
    private long totalSessions;
    private long todayCount;
    private long weekCount;
    private long monthCount;
    private long upcomingCount;
    private long cancelledCount;
    private long completedCount;
    private long recentCount;

    private List<SessionSummaryResponse> todaySessions;
    private List<SessionSummaryResponse> weekSessions;
    private List<SessionSummaryResponse> monthSessions;
    private List<SessionSummaryResponse> allSessions;
    private List<SessionSummaryResponse> upcomingSessions;
    private List<SessionSummaryResponse> cancelledSessions;
    private List<SessionSummaryResponse> completedSessions;
    private List<SessionSummaryResponse> recentSessions;
}

