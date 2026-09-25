package com.smart.therapy.flow.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.audit.dto.AuditLogFilterRequest;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.AuditLog;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceIntegrityTest {

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
        // write()/writeImmediate() call through the Spring proxy (selfProvider)
        lenient().when(selfProvider.getObject()).thenReturn(service);
        return service;
    }

    @Test
    void neutralizesSpreadsheetFormulasInCsvExports() {
        AuditLog log = AuditLog.builder()
                .timestamp(Instant.parse("2026-01-01T00:00:00Z"))
                .username("=HYPERLINK(\"https://example.invalid\")")
                .action("+cmd")
                .resourceType("@resource")
                .result("success")
                .riskLevel("high")
                .hipaaRelevant(true)
                .ipAddress("-1")
                .build();
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        AuditLogService service = newService();

        String csv = service.exportAuditLogsAsCsv(new AuditLogFilterRequest(), 10);

        assertThat(csv).contains("\"'=HYPERLINK(\"\"https://example.invalid\"\")\"");
        assertThat(csv).contains("\"'+cmd\"", "\"'@resource\"", "\"'-1\"");
    }

    @Test
    void incrementsMetricWhenAuditPersistenceFails() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(meterRegistryProvider.getIfAvailable()).thenReturn(registry);
        doThrow(new IllegalStateException("database unavailable"))
                .when(auditLogRepository).save(any(AuditLog.class));
        AuditLogService service = newService();

        service.logAction(AuditLog.builder().action("client_viewed").resourceType("client").build());

        assertThat(registry.get("clinical.audit.write.failures")
                .tag("component", "audit_log_service")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void exportAuditMetadataUsesCountsWithoutClientIdentifiers() {
        AuditLogService service = newService();

        service.logDataExport(10L, "actor", "client_csv", List.of(101L, 102L),
                "127.0.0.1", "test-agent", java.util.Map.of("column_count", 25));

        org.mockito.ArgumentCaptor<AuditLog> captor =
                org.mockito.ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getDetails())
                .contains("\"client_count\":2", "\"column_count\":25")
                .doesNotContain("101", "102", "client_ids");
    }
}
