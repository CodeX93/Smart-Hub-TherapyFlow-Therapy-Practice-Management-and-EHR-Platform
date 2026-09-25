package com.smart.therapy.flow.integration.client;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.BaseTenantApiTest;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

class ClientCreatedEffectsIntegrationTest extends BaseTenantApiTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EmailService emailService;
    @Autowired private SystemOptionResolverService options;

    @Test
    void committedClientCreatesHistoryAuditWelcomeAndTherapistNotification() {
        User therapist = persistStaff(uniqueEmail("effects-therapist"), "password123", "THERAPIST");
        String email = uniqueEmail("effects-client");
        Client client = persistClient(therapist, email);

        await().pollInSameThread().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            enterFixtureTenant();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM tenant_api_test.client_history WHERE client_id=? AND event_type='CREATED'",
                    Integer.class, client.getId())).isEqualTo(1);
            var auditStates = jdbc.queryForList("SELECT after_state FROM tenant_api_test.audit_logs WHERE client_id=? AND action='client_created'",
                    String.class, client.getId());
            assertThat(auditStates).hasSize(1);
            String afterState = auditStates.get(0);
            assertThat(afterState).contains("\"status\":\"Active\"", client.getClientId());
            assertThat(jdbc.queryForObject("SELECT count(*) FROM tenant_api_test.notifications WHERE user_id=? AND related_entity_type='client' AND related_entity_id=?",
                    Integer.class, therapist.getId(), client.getId())).isEqualTo(1);
            verify(emailService).sendWelcomeEmail(email, client.getFullName(), therapist.getFullName());
        });
        String data = jdbc.queryForObject("SELECT data FROM tenant_api_test.notifications WHERE user_id=? AND related_entity_type='client' AND related_entity_id=?",
                String.class, therapist.getId(), client.getId());
        assertThat(data).contains(client.getClientId()).doesNotContain(client.getFullName(), email);
    }

    @Test
    void statusLabelsResolveWithoutCallerTransaction() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(options.resolveOptionLabel("client_status", "active")).isEqualTo("Active");
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
    }

    @Test
    void eventGraphCanBeReadAfterRepositorySessionCloses() {
        User therapist = persistStaff(uniqueEmail("graph-therapist"), "password123", "THERAPIST");
        String email = uniqueEmail("graph-client");
        Client created = persistClient(therapist, email);
        Client detached = clientRepository.findForCreatedEvent(created.getId()).orElseThrow();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(detached.getPrimaryEmail()).isEqualTo(email);
        assertThat(detached.getAssignedTherapist().getFullName()).isEqualTo(therapist.getFullName());
        if (detached.getReferral() != null) {
            assertThat(detached.getReferral().getId()).isNotNull();
        }
    }
}
