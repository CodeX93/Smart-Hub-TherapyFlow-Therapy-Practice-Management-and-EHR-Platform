package com.smart.therapy.flow.user.service;

import com.smart.therapy.flow.billing.repository.SessionBillingRepository;
import com.smart.therapy.flow.client.dto.ClientStatsResponse;
import com.smart.therapy.flow.client.service.ClientService;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.session.service.SessionService;
import com.smart.therapy.flow.task.dto.TaskStatsResponse;
import com.smart.therapy.flow.task.service.TaskService;
import com.smart.therapy.flow.user.dto.TherapistDashboardSummaryResponse;
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
public class TherapistDashboardService {

    private static final ZoneId FALLBACK_PRACTICE_ZONE = ZoneId.of("UTC");

    private final ClientService clientService;
    private final SessionService sessionService;
    private final TaskService taskService;
    private final SessionBillingRepository sessionBillingRepository;
    private final CurrentUserService currentUserService;
    private final TimezoneService timezoneService;

    @Transactional(readOnly = true)
    public TherapistDashboardSummaryResponse getDashboardSummary(AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        ClientStatsResponse clientStats = clientService.getClientStatsForAssignedTherapist(requester);
        TaskStatsResponse taskStats = taskService.getTaskStatsForTherapist(requester);

        ZoneId practiceZone = resolvePracticeZone();
        LocalDate today = LocalDate.now(practiceZone);
        long scheduledToday = sessionService.countScheduledSessionsForDayForAssignedTherapist(
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                requester);

        Long therapistId = currentUserService.requireCurrentUser(requester).getId();
        YearMonth currentMonth = YearMonth.from(today);
        Instant monthStart = currentMonth.atDay(1).atStartOfDay(practiceZone).toInstant();
        Instant monthEndExclusive = currentMonth.plusMonths(1).atDay(1).atStartOfDay(practiceZone).toInstant();

        // Match Billings date filters: DATE(session_date) in practice timezone for "this month".
        BigDecimal estimatedThisMonth = nvl(sessionBillingRepository
                .sumTotalAmountByTherapistIdForSessionDateRange(therapistId, monthStart, monthEndExclusive));
        BigDecimal totalCollected = nvl(sessionBillingRepository
                .sumPaidAmountByTherapistIdForSessionDateRange(therapistId, monthStart, monthEndExclusive));
        BigDecimal outstandingBalance = nvl(sessionBillingRepository
                .sumPositiveOutstandingAmountByTherapistIdForSessionDateRange(
                        therapistId, monthStart, monthEndExclusive));

        return TherapistDashboardSummaryResponse.builder()
                .client(TherapistDashboardSummaryResponse.ClientCard.builder()
                        .active(valueOrZero(clientStats.getActiveClients()))
                        .total(valueOrZero(clientStats.getTotalClients()))
                        .build())
                .session(TherapistDashboardSummaryResponse.SessionCard.builder()
                        .scheduledToday(scheduledToday)
                        .build())
                .task(TherapistDashboardSummaryResponse.TaskCard.builder()
                        .pending(valueOrZero(taskStats.getPendingTasks()))
                        .urgent(valueOrZero(taskStats.getUrgentTasks()))
                        .total(valueOrZero(taskStats.getTotalTasks()))
                        .build())
                .billing(TherapistDashboardSummaryResponse.BillingCard.builder()
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
