package com.smart.therapy.flow.unit.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.audit.dto.AuditLogFilterRequest;
import com.smart.therapy.flow.audit.dto.AuditLogResponse;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.AuditLog;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogClientDeletedResponseTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private EntityManager entityManager;
    @Mock private ObjectProvider<MeterRegistry> meterRegistryProvider;
    @Mock private ObjectProvider<AuditLogService> selfProvider;

    private AuditLogService newService() {
        AuditLogService service = new AuditLogService(
                auditLogRepository, userRepository, clientRepository, entityManager,
                new ObjectMapper(), meterRegistryProvider, selfProvider);
        lenient().when(selfProvider.getObject()).thenReturn(service);
        return service;
    }

    @Test
    void clientDeletedResponseExposesMrnAndDeviceDetailsFromPayload() {
        String details = "{"
                + "\"internal_client_id\":2256,"
                + "\"client_mrn\":\"CL-WEB-DF21B3B259\","
                + "\"deleted_at\":\"2026-09-09T13:26:49Z\","
                + "\"closed_sessions\":1,"
                + "\"status\":\"active\","
                + "\"stage\":\"therapy\","
                + "\"soft_delete\":true,"
                + "\"user_agent\":\"Mozilla/5.0\","
                + "\"device_label\":\"Chrome on Windows\","
                + "\"device_fingerprint\":\"abc123fingerprint\""
                + "}";

        User actor = new User();
        actor.setId(382L);
        actor.setEmail("amjadabu@hotmail.com");

        AuditLog log = AuditLog.builder()
                .timestamp(Instant.parse("2026-09-09T13:26:49Z"))
                .username("amjadabu@hotmail.com")
                .action("client_deleted")
                .resourceType("client")
                .resourceId("2256")
                .result("success")
                .riskLevel("high")
                .hipaaRelevant(true)
                .ipAddress("154.192.25.158")
                .userAgent("Mozilla/5.0")
                .details(details)
                .build();
        log.setUser(actor);

        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));
        when(auditLogRepository.count(any(Specification.class))).thenReturn(1L);
        when(userRepository.findAuditActorsByIdIn(any())).thenReturn(List.of(
                new UserRepository.AuditActorView() {
                    @Override public Long getId() { return 382L; }
                    @Override public String getEmail() { return "amjadabu@hotmail.com"; }
                    @Override public String getFullName() { return "Admin"; }
                    @Override public String getLoginIdentifier() { return "admin"; }
                }
        ));

        AuditLogService service = newService();
        List<AuditLogResponse> responses = service.getAuditLogs(
                new AuditLogFilterRequest(), PageRequest.of(0, 10)).getContent();

        assertThat(responses).hasSize(1);
        AuditLogResponse response = responses.get(0);
        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getClientId()).isEqualTo(2256L);
        assertThat(response.getClientMrn()).isEqualTo("CL-WEB-DF21B3B259");
        assertThat(response.getResourceId()).isEqualTo("2256");
        assertThat(response.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(response.getDetails()).contains("Client MRN: CL-WEB-DF21B3B259");
        assertThat(response.getDetails()).contains("Device Fingerprint: abc123fingerprint");
        assertThat(response.getDetails()).contains("Device Label: Chrome on Windows");
        assertThat(response.getDetails()).contains("Internal Client ID: 2256");
    }

    @Test
    void displayUsernamePrefersLoginIdentifierOverAuthIdAndEmail() {
        User actor = new User();
        actor.setId(10L);
        actor.setEmail("amjadabu@hotmail.com");

        AuditLog authIdRow = AuditLog.builder()
                .timestamp(Instant.now())
                .username("382")
                .action("client_reports_viewed")
                .resourceType("client_report")
                .result("success")
                .riskLevel("high")
                .hipaaRelevant(true)
                .build();
        authIdRow.setUser(actor);

        AuditLog emailRow = AuditLog.builder()
                .timestamp(Instant.now())
                .username("amjadabu@hotmail.com")
                .action("client_viewed")
                .resourceType("client")
                .result("success")
                .riskLevel("medium")
                .hipaaRelevant(true)
                .build();
        emailRow.setUser(actor);

        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(authIdRow, emailRow)));
        when(auditLogRepository.count(any(Specification.class))).thenReturn(2L);
        when(userRepository.findAuditActorsByIdIn(any())).thenReturn(List.of(
                new UserRepository.AuditActorView() {
                    @Override public Long getId() { return 10L; }
                    @Override public String getEmail() { return "amjadabu@hotmail.com"; }
                    @Override public String getFullName() { return "Admin User"; }
                    @Override public String getLoginIdentifier() { return "admin"; }
                }
        ));

        AuditLogService service = newService();
        List<AuditLogResponse> responses = service.getAuditLogs(
                new AuditLogFilterRequest(), PageRequest.of(0, 10)).getContent();

        assertThat(responses).extracting(AuditLogResponse::getUsername)
                .containsExactly("admin", "admin");
    }

    @Test
    void logActionNormalizesAuthIdUsernameToLoginIdentifier() {
        UserRepository.AuditActorView actor = new UserRepository.AuditActorView() {
            @Override public Long getId() { return 10L; }
            @Override public String getEmail() { return "amjadabu@hotmail.com"; }
            @Override public String getFullName() { return "Admin User"; }
            @Override public String getLoginIdentifier() { return "admin"; }
        };
        User userRef = new User();
        userRef.setId(10L);

        AuditLog incoming = AuditLog.builder()
                .username("382")
                .action("client_reports_viewed")
                .resourceType("client_report")
                .result("success")
                .riskLevel("high")
                .hipaaRelevant(true)
                .timestamp(Instant.now())
                .build();
        User stub = new User();
        stub.setId(10L);
        incoming.setUser(stub);

        when(userRepository.findAuditActorById(10L)).thenReturn(Optional.of(actor));
        when(entityManager.getReference(User.class, 10L)).thenReturn(userRef);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditLogService service = newService();
        service.logAction(incoming);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("admin");
    }

    @Test
    void logActionBindsSoftDeletedClientViaScalarLookup() {
        Client deleted = new Client();
        deleted.setId(2256L);
        deleted.setClientId("CL-WEB-DF21B3B259");
        deleted.setIsDeleted(true);

        AuditLog incoming = AuditLog.builder()
                .action("client_deleted")
                .resourceType("client")
                .resourceId("2256")
                .result("success")
                .riskLevel("high")
                .hipaaRelevant(true)
                .timestamp(Instant.now())
                .build();
        Client ref = new Client();
        ref.setId(2256L);
        incoming.setClient(ref);

        when(clientRepository.findExistingIdIncludingDeleted(2256L)).thenReturn(Optional.of(2256L));
        when(entityManager.getReference(Client.class, 2256L)).thenReturn(deleted);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditLogService service = newService();
        service.logAction(incoming);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getClient().getId()).isEqualTo(2256L);
    }
}
