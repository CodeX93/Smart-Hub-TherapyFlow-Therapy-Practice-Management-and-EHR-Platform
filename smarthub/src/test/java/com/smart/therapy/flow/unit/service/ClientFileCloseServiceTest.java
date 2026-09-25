package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.dto.ClientResponse;
import com.smart.therapy.flow.client.dto.UpdateClientRequest;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.enums.ClientEventType;
import com.smart.therapy.flow.client.enums.EventSource;
import com.smart.therapy.flow.client.repository.ClientHistoryRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientHistoryTrackingService;
import com.smart.therapy.flow.client.service.ClientMrnService;
import com.smart.therapy.flow.client.service.ClientPortalSettingsService;
import com.smart.therapy.flow.client.service.ClientService;
import com.smart.therapy.flow.client.validation.ClientRequestValidator;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CaseloadScope;
import com.smart.therapy.flow.common.security.CaseloadScopeService;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.service.BlindIndexService;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Client file close/reopen unit tests")
class ClientFileCloseServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private com.smart.therapy.flow.common.service.TimezoneService timezoneService;
    @Mock
    private ClientHistoryRepository clientHistoryRepository;
    @Mock
    private ClientHistoryTrackingService clientHistoryTrackingService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private ClientRequestValidator requestValidator;
    @Mock
    private CurrentUserService currentUserService;
    @Spy
    private PermissionChecker permissionChecker = new PermissionChecker();
    @Mock
    private ClientPortalSettingsService portalSettingsService;
    @Mock
    private SystemOptionResolverService systemOptionResolverService;
    @Mock
    private EntityManager entityManager;

    @Mock
    private CaseloadScopeService caseloadScopeService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ClientMrnService clientMrnService;

    @Mock
    private BlindIndexService blindIndexService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ClientService clientService;

    private User therapist;
    private User admin;
    private AuthPrincipal therapistPrincipal;
    private AuthPrincipal adminPrincipal;
    private Client activeClient;

    @BeforeEach
    void setUp() {
        lenient().when(timezoneService.getPracticeTimezone()).thenReturn(java.time.ZoneId.of("UTC"));
        therapist = TestDataFactory.createTestTherapist();
        therapist.setId(1L);
        admin = TestDataFactory.createTestAdmin();
        admin.setId(2L);

        therapistPrincipal = TestDataFactory.createAuthPrincipal(therapist, "ROLE_THERAPIST", "CLIENT_EDIT", "CLIENT_VIEW_ALL");
        adminPrincipal = TestDataFactory.createAuthPrincipal(admin, "ROLE_ADMIN", "CLIENT_EDIT", "CLIENT_VIEW_ALL");

        activeClient = TestDataFactory.createTestClientWithId(10L);
        activeClient.setStatus("active");
        activeClient.setStage("intake");
        activeClient.setAssignedTherapist(therapist);

        when(systemOptionResolverService.resolveOptionLabel(org.mockito.ArgumentMatchers.anyString(), any()))
                .thenAnswer(inv -> {
                    String key = inv.getArgument(1);
                    if (key == null) {
                        return null;
                    }
                    return key.substring(0, 1).toUpperCase() + key.substring(1).replace('_', ' ');
                });

        when(systemOptionResolverService.resolveOptionKey(eq("client_status"), any()))
                .thenAnswer(inv -> normalizeOptionKey(inv.getArgument(1)));
        when(systemOptionResolverService.requireOptionKey(eq("client_status"), any()))
                .thenAnswer(inv -> normalizeOptionKey(inv.getArgument(1)));

        ReflectionTestUtils.setField(clientService, "entityManager", entityManager);
        when(caseloadScopeService.resolve(any())).thenReturn(new CaseloadScopeService.ResolvedCaseloadScope(CaseloadScope.ALL, java.util.List.of(), 1L));
    }

    @Test
    @DisplayName("Should close client file and auto-set stage to closed")
    void shouldCloseClientFile() {
        when(systemOptionResolverService.requireOptionKey(eq("client_stage"), any())).thenReturn("closed");
        when(currentUserService.getCurrentUserId(therapistPrincipal)).thenReturn(1L);

        when(blindIndexService.getSearchMode()).thenReturn(BlindIndexService.SearchMode.LEGACY);
        when(clientRepository.findById(10L)).thenReturn(Optional.of(activeClient));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateClientRequest request = new UpdateClientRequest();
        request.markFieldPresent("status");
        request.setStatus("inactive");

        ClientResponse response = clientService.updateClient(10L, request, therapistPrincipal, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(activeClient.getStatus()).isEqualTo("inactive");
        assertThat(activeClient.getStage()).isEqualTo("closed");

        ArgumentCaptor<ClientEventType> eventTypeCaptor = ArgumentCaptor.forClass(ClientEventType.class);
        verify(clientHistoryTrackingService, org.mockito.Mockito.atLeastOnce()).persistHistory(
                eq(10L),
                eventTypeCaptor.capture(),
                eq(EventSource.API),
                any(),
                any(),
                any(),
                any(),
                any());
        assertThat(eventTypeCaptor.getAllValues())
                .contains(ClientEventType.FILE_CLOSED, ClientEventType.STAGE_CHANGED);
    }

    @Test
    @DisplayName("Should reopen client file as admin without changing stage")
    void shouldReopenClientFileAsAdmin() {
        when(currentUserService.getCurrentUserId(adminPrincipal)).thenReturn(2L);
        when(blindIndexService.getSearchMode()).thenReturn(BlindIndexService.SearchMode.LEGACY);
        activeClient.setStatus("inactive");
        activeClient.setStage("closed");

        when(clientRepository.findById(10L)).thenReturn(Optional.of(activeClient));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateClientRequest request = new UpdateClientRequest();
        request.markFieldPresent("status");
        request.setStatus("active");

        clientService.updateClient(10L, request, adminPrincipal, "127.0.0.1");

        assertThat(activeClient.getStatus()).isEqualTo("active");
        assertThat(activeClient.getStage()).isEqualTo("closed");

        verify(clientHistoryTrackingService).persistHistory(
                eq(10L),
                eq(ClientEventType.FILE_REOPENED),
                eq(EventSource.API),
                any(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    @DisplayName("Should reject reopen by non-admin therapist")
    void shouldRejectReopenByTherapist() {
        activeClient.setStatus("inactive");
        activeClient.setStage("closed");

        when(clientRepository.findById(10L)).thenReturn(Optional.of(activeClient));

        UpdateClientRequest request = new UpdateClientRequest();
        request.markFieldPresent("status");
        request.setStatus("active");

        assertThatThrownBy(() -> clientService.updateClient(10L, request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only administrators can reopen");
    }

    private static String normalizeOptionKey(String input) {
        if (input == null) {
            return null;
        }
        return input.trim().toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
    }
}
