package com.smart.therapy.flow.admin.service;

import com.smart.therapy.flow.admin.dto.AdminDashboardSummaryResponse;
import com.smart.therapy.flow.billing.repository.SessionBillingRepository;
import com.smart.therapy.flow.client.dto.ClientStatsResponse;
import com.smart.therapy.flow.client.service.ClientService;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.session.service.SessionService;
import com.smart.therapy.flow.task.dto.TaskStatsResponse;
import com.smart.therapy.flow.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {

    private static final ZoneId FALLBACK_PRACTICE_ZONE = ZoneId.of("UTC");

    private final ClientService clientService;
    private final SessionService sessionService;
    private final TaskService taskService;
    private final SessionBillingRepository sessionBillingRepository;
    private final TimezoneService timezoneService;

    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse getDashboardSummary(AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        ClientStatsResponse clientStats = clientService.getClientStats(requester);
        TaskStatsResponse taskStats = taskService.getTaskStats(requester, null, null, null, null);

        ZoneId practiceZone = resolvePracticeZone();
        LocalDate today = LocalDate.now(practiceZone);
        long scheduledToday = sessionService.countScheduledSessionsForDay(
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                requester);

        YearMonth currentMonth = YearMonth.from(today);
        Instant monthStart = currentMonth.atDay(1).atStartOfDay(practiceZone).toInstant();
        Instant monthEndExclusive = currentMonth.plusMonths(1).atDay(1).atStartOfDay(practiceZone).toInstant();

        // Match Billings date filters: DATE(session_date) in practice timezone for "this month".
        BigDecimal estimatedThisMonth = nvl(sessionBillingRepository
                .sumTotalAmountForSessionDateRange(monthStart, monthEndExclusive));
        BigDecimal totalCollected = nvl(sessionBillingRepository
                .sumPaidAmountForSessionDateRange(monthStart, monthEndExclusive));
        BigDecimal outstandingBalance = nvl(sessionBillingRepository
                .sumPositiveOutstandingAmountForSessionDateRange(monthStart, monthEndExclusive));

        return AdminDashboardSummaryResponse.builder()
                .client(AdminDashboardSummaryResponse.ClientCard.builder()
                        .active(valueOrZero(clientStats.getActiveClients()))
                        .total(valueOrZero(clientStats.getTotalClients()))
                        .build())
                .session(AdminDashboardSummaryResponse.SessionCard.builder()
                        .scheduledToday(scheduledToday)
                        .build())
                .task(AdminDashboardSummaryResponse.TaskCard.builder()
                        .pending(valueOrZero(taskStats.getPendingTasks()))
                        .urgent(valueOrZero(taskStats.getUrgentTasks()))
                        .total(valueOrZero(taskStats.getTotalTasks()))
                        .build())
                .billing(AdminDashboardSummaryResponse.BillingCard.builder()
                        .estimatedThisMonth(estimatedThisMonth.setScale(2, RoundingMode.HALF_UP))
                        .totalCollected(totalCollected.setScale(2, RoundingMode.HALF_UP))
                        .outstandingBalance(outstandingBalance.setScale(2, RoundingMode.HALF_UP))
                        .build())
                .build();
    }

    private ZoneId resolvePracticeZone() {
        return timezoneService.findPracticeTimezoneId()
                .map(id -> {
                    try {
                        return ZoneId.of(id);
                    } catch (Exception ex) {
                        log.debug("Invalid practice timezone {}, falling back to {}: {}",
                                id, FALLBACK_PRACTICE_ZONE, ex.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .orElse(FALLBACK_PRACTICE_ZONE);
    }

    private long valueOrZero(Long value) {
        return value != null ? value : 0L;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
