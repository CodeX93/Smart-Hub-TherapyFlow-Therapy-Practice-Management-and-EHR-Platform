package com.smart.therapy.flow.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smart.therapy.flow.billing.service.BillingService;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.config.AppProperties;
import com.smart.therapy.flow.common.exception.ConflictException;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.common.exception.GlobalExceptionHandler;
import com.smart.therapy.flow.common.logging.SensitiveDataMasker;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.session.controller.SessionController;
import com.smart.therapy.flow.session.dto.CancelRecurringSeriesResponse;
import com.smart.therapy.flow.session.dto.CreateRecurringSessionsResponse;
import com.smart.therapy.flow.session.dto.RecurrencePreviewOccurrence;
import com.smart.therapy.flow.session.dto.RecurrencePreviewResponse;
import com.smart.therapy.flow.session.dto.RecurrenceRuleRequest;
import com.smart.therapy.flow.session.dto.SessionResponse;
import com.smart.therapy.flow.session.dto.SkippedRecurringOccurrence;
import com.smart.therapy.flow.session.dto.UpdateRecurringFutureRequest;
import com.smart.therapy.flow.session.enums.RecurrenceEndMode;
import com.smart.therapy.flow.session.enums.RecurrenceType;
import com.smart.therapy.flow.session.enums.SessionType;
import com.smart.therapy.flow.session.service.RecurringSessionService;
import com.smart.therapy.flow.session.service.SessionService;
import com.smart.therapy.flow.session.service.SessionTranscriptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Recurring session controller contract tests")
class RecurringSessionControllerContractTest {

    @Mock private SessionService sessionService;
    @Mock private RecurringSessionService recurringSessionService;
    @Mock private SessionTranscriptService sessionTranscriptService;
    @Mock private BillingService billingService;
    @Mock private com.smart.therapy.flow.common.security.JwtTokenProvider jwtTokenProvider;
    @Mock private AppProperties appProperties;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AuthPrincipal authPrincipal;

    @BeforeEach
    void setUp() {
        SessionController controller = new SessionController(
                sessionService, recurringSessionService, sessionTranscriptService, billingService, jwtTokenProvider);
        ReflectionTestUtils.setField(controller, "appProperties", appProperties);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        authPrincipal = TestDataFactory.createAuthPrincipal(
                TestDataFactory.createTestTherapist(),
                "ROLE_THERAPIST",
                "SESSION_CREATE",
                "SESSION_VIEW",
                "SESSION_EDIT",
                "SESSION_DELETE");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(
                        org.mockito.Mockito.mock(AuditLogService.class),
                        new SensitiveDataMasker(new ObjectMapper()),
                        org.mockito.Mockito.mock(com.smart.therapy.flow.common.metrics.AuthAbuseMetrics.class)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /recurring/preview returns expanded dates")
    void previewRecurringSessions() throws Exception {
        when(recurringSessionService.previewRecurringSessions(any(), any())).thenReturn(
                RecurrencePreviewResponse.builder()
                        .sessions(List.of(
                                RecurrencePreviewOccurrence.builder()
                                        .localDate("2026-06-08")
                                        .sessionTime("14:00")
                                        .hasConflict(false)
                                        .reasons(List.of())
                                        .build()))
                        .totalRequested(1)
                        .freeCount(1)
                        .conflictCount(0)
                        .build());

        mockMvc.perform(post("/api/v1/sessions/recurring/preview")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRule())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequested").value(1))
                .andExpect(jsonPath("$.freeCount").value(1))
                .andExpect(jsonPath("$.sessions[0].hasConflict").value(false));
    }

    @Test
    @DisplayName("POST /recurring creates series")
    void createRecurringSessions() throws Exception {
        when(recurringSessionService.createRecurringSessions(any(), any(), anyString()))
                .thenReturn(CreateRecurringSessionsResponse.builder()
                        .groupId("rec-abc")
                        .created(List.of(SessionResponse.builder().id(1L).recurrenceGroupId("rec-abc").build()))
                        .createdCount(1)
                        .skipped(List.of())
                        .skippedCount(0)
                        .build());

        mockMvc.perform(post("/api/v1/sessions/recurring")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRule())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").value("rec-abc"))
                .andExpect(jsonPath("$.createdCount").value(1));
    }

    @Test
    @DisplayName("POST /recurring returns 409 when all dates conflict")
    void createRecurringSessionsAllConflict() throws Exception {
        when(recurringSessionService.createRecurringSessions(any(), any(), anyString()))
                .thenThrow(new ConflictException("All recurrence dates conflict with existing sessions",
                        List.of(SkippedRecurringOccurrence.builder()
                                .localDate("2026-06-08")
                                .reasons(List.of("Therapist is busy"))
                                .build())));

        mockMvc.perform(post("/api/v1/sessions/recurring")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRule())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /recurring/{groupId}/future updates future sessions")
    void updateFutureRecurringSessions() throws Exception {
        when(recurringSessionService.updateFutureRecurringSessions(eq("rec-abc"), any(), any(), anyString()))
                .thenReturn(List.of(SessionResponse.builder().id(10L).recurrenceGroupId("rec-abc").build()));

        UpdateRecurringFutureRequest request = new UpdateRecurringFutureRequest();
        request.setAnchorId(10L);
        request.setSessionDate(Instant.parse("2026-06-11T18:00:00Z"));

        mockMvc.perform(put("/api/v1/sessions/recurring/rec-abc/future")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recurrenceGroupId").value("rec-abc"));
    }

    @Test
    @DisplayName("DELETE /recurring/{groupId} cancels upcoming series sessions")
    void cancelRecurringSeries() throws Exception {
        when(recurringSessionService.cancelRecurringSeries(eq("rec-abc"), any(), anyString()))
                .thenReturn(CancelRecurringSeriesResponse.builder()
                        .groupId("rec-abc")
                        .cancelledCount(3)
                        .build());

        mockMvc.perform(delete("/api/v1/sessions/recurring/rec-abc")
                        .principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value("rec-abc"))
                .andExpect(jsonPath("$.cancelledCount").value(3));
    }

    @Test
    @DisplayName("POST /recurring/preview rejects invalid rule")
    void previewRejectsInvalidRule() throws Exception {
        RecurrenceRuleRequest invalid = validRule();
        invalid.setCount(null);

        mockMvc.perform(post("/api/v1/sessions/recurring/preview")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    private RecurrenceRuleRequest validRule() {
        LocalDate startDate = LocalDate.now(ZoneId.of("America/New_York")).plusWeeks(2);
        while (startDate.getDayOfWeek().getValue() % 7 != 1) {
            startDate = startDate.plusDays(1);
        }

        RecurrenceRuleRequest request = new RecurrenceRuleRequest();
        request.setClientId(10L);
        request.setTherapistId(1L);
        request.setServiceId(5L);
        request.setSessionMode("in_person");
        request.setSessionDate(ZonedDateTime.of(
                startDate,
                java.time.LocalTime.of(14, 0),
                ZoneId.of("America/New_York")).toInstant());
        request.setTimezone("America/New_York");
        request.setRecurrenceType(RecurrenceType.WEEKLY);
        request.setDaysOfWeek(List.of(1, 3));
        request.setInterval(1);
        request.setEndMode(RecurrenceEndMode.COUNT);
        request.setCount(4);
        return request;
    }

    private UsernamePasswordAuthenticationToken auth() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(authPrincipal, null, authPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }
}
