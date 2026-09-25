package com.smart.therapy.flow.api;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.audit.support.AuditEventDraft;
import com.smart.therapy.flow.auth.entity.AuditLog;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.BaseTenantApiTest;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Real audit persistence must retain references without reading unrelated clinical graphs. */
class AuditReferenceApiTest extends BaseTenantApiTest {
    @Autowired AuditLogService auditLogs;
    @Autowired com.smart.therapy.flow.auth.repository.AuthIdentityRepository authIdentities;
    @Autowired JdbcTemplate jdbc;

    private Client auditClient(User actor) {
        return clientRepository.saveAndFlush(Client.builder().clientId("QA-AUDIT-" + UUID.randomUUID())
                .fullName("Synthetic audit client").status("active").assignedTherapist(actor).build());
    }

    @Test
    void persistsAuditReferencesWithoutLoadingClinicalGraph() {
        User actor = persistStaff(uniqueEmail("audit"), "password123", "THERAPIST");
        Client client = auditClient(actor);
        AuditLog audit = AuditEventDraft.of("client_viewed", "client")
                .actorId(actor.getId()).clientId(client.getId()).resourceId(client.getId())
                .hipaaRelevant(true).toAuditLog();
        auditLogs.writeImmediate(audit);

        var row = jdbc.queryForMap("SELECT user_id, client_id, username, action, hipaa_relevant FROM tenant_api_test.audit_logs WHERE id = ?", audit.getId());
        assertThat(row).containsEntry("user_id", actor.getId()).containsEntry("client_id", client.getId())
                .containsEntry("username", actor.getEmail()).containsEntry("action", "client_viewed")
                .containsEntry("hipaa_relevant", true);
        assertThat(Hibernate.isInitialized(audit.getUser())).as("Audit actor reference stays unloaded").isFalse();
        assertThat(Hibernate.isInitialized(audit.getClient())).as("Audit client reference stays unloaded").isFalse();
    }

    @Test
    void retainsSoftDeletedClientReferenceAndSuppliedUsername() {
        User actor = persistStaff(uniqueEmail("audit"), "password123", "THERAPIST");
        Client client = auditClient(actor);
        client.setIsDeleted(true);
        clientRepository.saveAndFlush(client);
        AuditLog audit = AuditEventDraft.of("client_deleted", "client")
                .actorId(actor.getId()).clientId(client.getId()).resourceId(client.getId()).toAuditLog();
        audit.setUsername("Recorded actor");

        auditLogs.writeImmediate(audit);

        assertThat(jdbc.queryForMap("SELECT client_id, username FROM tenant_api_test.audit_logs WHERE id = ?", audit.getId()))
                .containsEntry("client_id", client.getId()).containsEntry("username", "Recorded actor");
    }

    @Test
    void retainsAuditWhenActorAndClientNoLongerExist() {
        AuditLog audit = AuditEventDraft.of("client_viewed", "client")
                .actorId(Long.MAX_VALUE).clientId(Long.MAX_VALUE).resourceId(Long.MAX_VALUE).toAuditLog();
        audit.setUsername("Historical actor");

        auditLogs.writeImmediate(audit);

        assertThat(jdbc.queryForMap("SELECT user_id, client_id, username FROM tenant_api_test.audit_logs WHERE id = ?", audit.getId()))
                .containsEntry("user_id", null).containsEntry("client_id", null)
                .containsEntry("username", "Historical actor");
    }

    @Test
    void fallsBackToActorNameWhenLoginIdentifierAndEmailAreBlank() {
        // Actor labels prefer loginIdentifier, then email, then full name. Clear the first
        // two so this still covers the last rung rather than the one it used to be.
        User actor = persistStaff(uniqueEmail("audit"), "password123", "THERAPIST");
        // login_identifier is NOT NULL, but the resolver uses hasText, so blank clears it.
        actor.getAuthIdentity().setLoginIdentifier(" ");
        authIdentities.saveAndFlush(actor.getAuthIdentity());
        actor.setEmail(" ");
        actor.setFullName("  Recorded name  ");
        userRepository.saveAndFlush(actor);
        AuditLog audit = AuditEventDraft.of("client_viewed", "client").actorId(actor.getId()).toAuditLog();

        auditLogs.writeImmediate(audit);

        assertThat(jdbc.queryForObject("SELECT username FROM tenant_api_test.audit_logs WHERE id = ?", String.class, audit.getId()))
                .isEqualTo("Recorded name");
    }

    @Test
    void doesNotReattachSoftDeletedStaff() {
        User actor = persistStaff(uniqueEmail("audit"), "password123", "THERAPIST");
        actor.setIsDeleted(true);
        userRepository.saveAndFlush(actor);
        AuditLog audit = AuditEventDraft.of("client_viewed", "client").actorId(actor.getId()).toAuditLog();
        audit.setUsername("Historical actor");

        auditLogs.writeImmediate(audit);

        assertThat(jdbc.queryForMap("SELECT user_id, username FROM tenant_api_test.audit_logs WHERE id = ?", audit.getId()))
                .containsEntry("user_id", null).containsEntry("username", "Historical actor");
    }
}
