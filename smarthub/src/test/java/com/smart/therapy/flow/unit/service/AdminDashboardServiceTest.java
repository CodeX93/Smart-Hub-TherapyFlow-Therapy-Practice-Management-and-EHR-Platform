package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.admin.dto.AdminDashboardSummaryResponse;
import com.smart.therapy.flow.admin.service.AdminDashboardService;
import com.smart.therapy.flow.billing.repository.SessionBillingRepository;
import com.smart.therapy.flow.client.dto.ClientStatsResponse;
import com.smart.therapy.flow.client.service.ClientService;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.session.service.SessionService;
import com.smart.therapy.flow.task.dto.TaskStatsResponse;
import com.smart.therapy.flow.task.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminDashboardService Unit Tests")
class AdminDashboardServiceTest {

    private static final ZoneId TORONTO = ZoneId.of("America/Toronto");

    @Mock
    private ClientService clientService;
    @Mock
    private SessionService sessionService;
    @Mock
    private TaskService taskService;
    @Mock
    private SessionBillingRepository sessionBillingRepository;
    @Mock
    private TimezoneService timezoneService;
    @Mock
    private AuthPrincipal requester;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    @DisplayName("Billing card uses practice-timezone month and outstanding/collected aggregates")
    void billingCardUsesPracticeTimezoneMonth() {
        when(timezoneService.findPracticeTimezoneId()).thenReturn(Optional.of("America/Toronto"));
        when(clientService.getClientStats(requester)).thenReturn(ClientStatsResponse.builder()
                .activeClients(10L)
                .totalClients(20L)
                .build());
        when(taskService.getTaskStats(eq(requester), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(TaskStatsResponse.builder()
                        .pendingTasks(3L)
                        .urgentTasks(1L)
                        .totalTasks(8L)
                        .build());
        when(sessionService.countScheduledSessionsForDay(anyInt(), anyInt(), anyInt(), eq(requester)))
                .thenReturn(5L);
        when(sessionBillingRepository.sumTotalAmountForSessionDateRange(any(), any()))
                .thenReturn(new BigDecimal("1000.00"));
        when(sessionBillingRepository.sumPaidAmountForSessionDateRange(any(), any()))
                .thenReturn(new BigDecimal("160.00"));
        when(sessionBillingRepository.sumPositiveOutstandingAmountForSessionDateRange(any(), any()))
                .thenReturn(new BigDecimal("840.00"));

        AdminDashboardSummaryResponse response = adminDashboardService.getDashboardSummary(requester);

        LocalDate today = LocalDate.now(TORONTO);
        YearMonth month = YearMonth.from(today);
        Instant expectedStart = month.atDay(1).atStartOfDay(TORONTO).toInstant();
        Instant expectedEnd = month.plusMonths(1).atDay(1).atStartOfDay(TORONTO).toInstant();

        ArgumentCaptor<Instant> startCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> endCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(sessionBillingRepository)
                .sumPositiveOutstandingAmountForSessionDateRange(startCaptor.capture(), endCaptor.capture());
        assertThat(startCaptor.getValue()).isEqualTo(expectedStart);
        assertThat(endCaptor.getValue()).isEqualTo(expectedEnd);

        verify(sessionService).countScheduledSessionsForDay(
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(), requester);

        assertThat(response.getBilling().getOutstandingBalance()).isEqualByComparingTo("840.00");
        assertThat(response.getBilling().getTotalCollected()).isEqualByComparingTo("160.00");
        assertThat(response.getBilling().getEstimatedThisMonth()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Falls back to America/New_York when practice timezone is unset")
    void fallsBackToAmericaNewYork() {
        when(timezoneService.findPracticeTimezoneId()).thenReturn(Optional.empty());
        when(clientService.getClientStats(requester)).thenReturn(ClientStatsResponse.builder()
                .activeClients(0L)
                .totalClients(0L)
                .build());
        when(taskService.getTaskStats(eq(requester), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(TaskStatsResponse.builder()
                        .pendingTasks(0L)
                        .urgentTasks(0L)
                        .totalTasks(0L)
                        .build());
        when(sessionService.countScheduledSessionsForDay(anyInt(), anyInt(), anyInt(), eq(requester)))
                .thenReturn(0L);
        when(sessionBillingRepository.sumTotalAmountForSessionDateRange(any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(sessionBillingRepository.sumPaidAmountForSessionDateRange(any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(sessionBillingRepository.sumPositiveOutstandingAmountForSessionDateRange(any(), any()))
                .thenReturn(BigDecimal.ZERO);

        adminDashboardService.getDashboardSummary(requester);

        ZoneId ny = ZoneId.of("America/New_York");
        LocalDate today = LocalDate.now(ny);
        verify(sessionService).countScheduledSessionsForDay(
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(), requester);
    }
}
