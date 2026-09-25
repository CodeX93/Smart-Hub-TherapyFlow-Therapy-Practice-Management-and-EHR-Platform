package com.smart.therapy.flow.unit.session;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.billing.entity.Service;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CaseloadScope;
import com.smart.therapy.flow.common.security.CaseloadScopeService;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.session.dto.SessionSummaryResponse;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.repository.SessionIntegrationRepository;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.repository.SessionTranscriptRepository;
import com.smart.therapy.flow.session.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Session list summary batch mapping")
class SessionSummaryBatchMappingTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private SessionTranscriptRepository sessionTranscriptRepository;
    @Mock
    private SessionIntegrationRepository sessionIntegrationRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private PermissionChecker permissionChecker;
    @Mock
    private CaseloadScopeService caseloadScopeService;
    @Mock
    private com.smart.therapy.flow.client.service.ClientSearchHelper clientSearchHelper;
    @Mock
    private com.smart.therapy.flow.system.service.SystemOptionResolverService systemOptionResolverService;
    @Mock
    private com.smart.therapy.flow.subscription.service.SubscriptionFeatureService subscriptionFeatureService;
    @Mock
    private com.smart.therapy.flow.audit.service.AuditLogService auditLogService;
    @Mock
    private com.smart.therapy.flow.common.service.TimezoneService timezoneService;
    @Mock
    private com.smart.therapy.flow.client.repository.ClientRepository clientRepository;
    @Mock
    private com.smart.therapy.flow.client.service.ClientMrnService clientMrnService;
    @Mock
    private com.smart.therapy.flow.auth.repository.UserRepository userRepository;
    @Mock
    private com.smart.therapy.flow.billing.repository.ServiceRepository serviceRepository;
    @Mock
    private com.smart.therapy.flow.session.repository.RoomRepository roomRepository;
    @Mock
    private com.smart.therapy.flow.user.service.TherapistAvailabilityService therapistAvailabilityService;

    @InjectMocks
    private SessionService sessionService;

    private AuthPrincipal principal;
    private User therapist;
    private Client client;
    private Service service;

    @BeforeEach
    void setUp() {
        therapist = TestDataFactory.createTestTherapist();
        therapist.setId(10L);
        client = TestDataFactory.createTestClient(therapist);
        client.setId(20L);
        service = Service.builder()
                .serviceName("Individual Therapy")
                .category("therapy")
                .build();
        service.setId(30L);

        principal = TestDataFactory.createAuthPrincipal(therapist);
        lenient().when(currentUserService.requireCurrentUser(any())).thenReturn(therapist);
        when(caseloadScopeService.resolve(any())).thenReturn(
                new CaseloadScopeService.ResolvedCaseloadScope(CaseloadScope.ALL, List.of(), 10L));
        lenient().when(systemOptionResolverService.parseOptionKey(any(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(1));
    }

    @Test
    @DisplayName("getSessions batches transcript lookup once for the whole page")
    void getSessionsBatchesTranscriptLookup() {
        Session s1 = session(1L, Instant.parse("2026-09-01T14:00:00Z"));
        Session s2 = session(2L, Instant.parse("2026-09-02T14:00:00Z"));
        Session s3 = session(3L, Instant.parse("2026-09-03T14:00:00Z"));
        List<Session> pageContent = List.of(s1, s2, s3);

        when(sessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(pageContent));
        when(sessionRepository.findByIdsForSummary(anyCollection())).thenReturn(pageContent);
        when(sessionIntegrationRepository.findBySessionIdIn(anyCollection())).thenReturn(List.of());
        when(sessionTranscriptRepository.findSessionIdsWithTranscripts(anyCollection()))
                .thenReturn(List.of(2L));

        PaginatedResponse<SessionSummaryResponse> response = sessionService.getSessions(
                1, 25, null, null, null, null, null, null, null, null, null, null, false, false, principal);

        assertThat(response.getItems()).hasSize(3);
        assertThat(response.getItems().get(0).getHasTranscript()).isFalse();
        assertThat(response.getItems().get(1).getHasTranscript()).isTrue();
        assertThat(response.getItems().get(2).getHasTranscript()).isFalse();

        verify(sessionTranscriptRepository, times(1)).findSessionIdsWithTranscripts(eq(List.of(1L, 2L, 3L)));
        verify(sessionRepository, times(1)).findByIdsForSummary(anyCollection());
        verify(sessionIntegrationRepository, times(1)).findBySessionIdIn(anyCollection());
    }

    private Session session(Long id, Instant date) {
        Session session = TestDataFactory.createTestSession(client, therapist);
        session.setId(id);
        session.setSessionDate(date);
        session.setDuration(50);
        session.setStatus("scheduled");
        session.setSessionType("in-person");
        session.setClinicalSessionType("therapy");
        session.setService(service);
        return session;
    }
}
