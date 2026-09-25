package com.smart.therapy.flow.unit.session;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.portal.enums.SessionHistoryScope;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.session.dto.SessionOverviewStatsResponse;
import com.smart.therapy.flow.session.dto.SessionSummaryResponse;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.enums.SessionStatus;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.repository.SessionIntegrationRepository;
import com.smart.therapy.flow.session.repository.SessionTranscriptRepository;
import com.smart.therapy.flow.session.service.SessionService;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService portal session history scope")
class SessionHistoryScopeServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private SessionIntegrationRepository sessionIntegrationRepository;
    @Mock
    private SessionTranscriptRepository sessionTranscriptRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private TimezoneService timezoneService;
    @Mock
    private PermissionChecker permissionChecker;
    @Mock
    private SubscriptionFeatureService subscriptionFeatureService;
    @Mock
    private SystemOptionResolverService systemOptionResolverService;

    @InjectMocks
    private SessionService sessionService;

    private AuthPrincipal clientPrincipal;
    private Client client;
    private User therapist;

    @BeforeEach
    void setUp() {
        clientPrincipal = TestDataFactory.createAuthPrincipalForClient(10L);
        therapist = TestDataFactory.createTestTherapist();
        client = TestDataFactory.createTestClient();
        client.setId(10L);
        when(currentUserService.getCurrentClientId(clientPrincipal)).thenReturn(10L);
    }

    @Test
    @DisplayName("Returns only sessions for logged-in client filtered by scope")
    void returnsScopedSessionsForClient() {
        when(timezoneService.getClientTimezone(10L)).thenReturn(Optional.of(ZoneId.of("America/New_York")));
        Instant now = Instant.now();
        Session upcoming = TestDataFactory.createTestSession(client, therapist);
        upcoming.setId(28L);
        upcoming.setStatus(SessionStatus.RESCHEDULING.getValue());
        upcoming.setSessionDate(now.plus(5, ChronoUnit.DAYS));
        upcoming.setDuration(60);

        Session past = TestDataFactory.createTestSession(client, therapist);
        past.setId(27L);
        past.setStatus(SessionStatus.CONFIRMED.getValue());
        past.setSessionDate(now.minus(2, ChronoUnit.DAYS));
        past.setDuration(60);

        when(sessionRepository.findByClientId(10L)).thenReturn(List.of(upcoming, past));

        List<SessionSummaryResponse> upcomingResults = sessionService.getCurrentClientSessionHistory(
                clientPrincipal, SessionHistoryScope.UPCOMING, null);
        List<SessionSummaryResponse> pastResults = sessionService.getCurrentClientSessionHistory(
                clientPrincipal, SessionHistoryScope.PAST, null);

        assertThat(upcomingResults).hasSize(1);
        assertThat(upcomingResults.get(0).getId()).isEqualTo(28L);
        assertThat(pastResults).hasSize(1);
        assertThat(pastResults.get(0).getId()).isEqualTo(27L);
    }

    @Test
    @DisplayName("Rejects invalid timezone")
    void rejectsInvalidTimezone() {
        when(timezoneService.normalizeTimezoneId("Not/AZone")).thenReturn("Not/AZone");
        assertThatThrownBy(() -> sessionService.getCurrentClientSessionHistory(
                clientPrincipal, SessionHistoryScope.UPCOMING, "Not/AZone"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid timezone.");
    }

    @Test
    @DisplayName("Session stats use practice timezone when param omitted")
    void statsDefaultToPracticeTimezone() {
        when(timezoneService.findPracticeTimezoneId()).thenReturn(Optional.of("America/Toronto"));
        Instant now = Instant.now();
        Session upcoming = TestDataFactory.createTestSession(client, therapist);
        upcoming.setStatus(SessionStatus.CONFIRMED.getValue());
        upcoming.setSessionDate(now.plus(1, ChronoUnit.DAYS));
        upcoming.setDuration(60);

        when(sessionRepository.findByClientId(10L)).thenReturn(List.of(upcoming));

        SessionOverviewStatsResponse stats =
                sessionService.getCurrentClientSessionOverview(null, clientPrincipal);

        assertThat(stats.getTimezone()).isEqualTo("America/Toronto");
        assertThat(stats.getUpcomingSessions()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Rejects non-client principal")
    void rejectsNonClientPrincipal() {
        when(currentUserService.getCurrentClientId(clientPrincipal)).thenReturn(null);

        assertThatThrownBy(() -> sessionService.getCurrentClientSessionHistory(
                clientPrincipal, SessionHistoryScope.PAST, null))
                .isInstanceOf(ForbiddenException.class);
    }
}
