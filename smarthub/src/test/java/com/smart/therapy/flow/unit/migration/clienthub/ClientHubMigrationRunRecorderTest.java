package com.smart.therapy.flow.migration.clienthub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.migration.clienthub.ClientHubMigrationReconciliationService.EntityReconciliation;
import com.smart.therapy.flow.migration.clienthub.ClientHubMigrationReconciliationService.ReconciliationReport;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TableInventory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ClientHubMigrationRunRecorderTest {

    @Test
    void skipsRunCreationWhenTargetOrganisationIsUnresolved() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ClientHubMigrationRunRecorder recorder = new ClientHubMigrationRunRecorder(jdbcTemplate, new ObjectMapper());

        Long runId = recorder.startRun(
                new SourceInventory("public", List.of(), "fingerprint"),
                new TargetInventory(0, 0, null, null),
                false);

        assertThat(runId).isNull();
        verify(jdbcTemplate, never()).queryForObject(any(String.class), eq(Long.class), any());
    }

    @Test
    void recordsStartedRunWithSerializedSourceCounts() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(
                any(String.class),
                eq(Long.class),
                anyLong(),
                any(String.class),
                any(String.class),
                any(String.class))).thenReturn(42L);
        ClientHubMigrationRunRecorder recorder = new ClientHubMigrationRunRecorder(jdbcTemplate, new ObjectMapper());

        Long runId = recorder.startRun(
                new SourceInventory("public", List.of(
                        new TableInventory("users", 2, List.of()),
                        new TableInventory("odd\"table", 3, List.of())
                ), "fingerprint"),
                new TargetInventory(1, 0, 7L, "tenant_real"),
                true);

        assertThat(runId).isEqualTo(42L);
        ArgumentCaptor<String> sourceCounts = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(
                any(String.class),
                eq(Long.class),
                eq(7L),
                eq("EXECUTE"),
                eq("fingerprint"),
                sourceCounts.capture());
        assertThat(sourceCounts.getValue()).isEqualTo("{\"odd\\\"table\":3,\"users\":2}");
    }

    @Test
    void marksRunSucceeded() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ClientHubMigrationRunRecorder recorder = new ClientHubMigrationRunRecorder(jdbcTemplate, new ObjectMapper());

        recorder.markSucceeded(42L);

        verify(jdbcTemplate).update(any(String.class), eq(42L));
    }

    @Test
    void marksRunSucceededWithFinalReconciliationCounts() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ClientHubMigrationRunRecorder recorder = new ClientHubMigrationRunRecorder(jdbcTemplate, new ObjectMapper());

        recorder.markSucceeded(42L, new ReconciliationReport(List.of(
                new EntityReconciliation("clients", "clients", 10, 8, 2, 3, 0, 1, 1),
                new EntityReconciliation("users", "users", 4, 4, 0, 0, 0, 0, 0)
        )));

        ArgumentCaptor<String> targetCounts = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> totals = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(any(String.class), targetCounts.capture(), totals.capture(), eq(42L));
        assertThat(targetCounts.getValue()).isEqualTo("{\"clients\":8,\"users\":4}");
        assertThat(totals.getValue()).isEqualTo(
                "{\"sourceRows\":14,\"mappedRows\":12,\"unmappedEstimate\":2,"
                        + "\"pendingSyncEvents\":3,\"failedOrDeadSyncEvents\":2}");
    }

    @Test
    void marksRunFailedWithTruncatedMessage() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ClientHubMigrationRunRecorder recorder = new ClientHubMigrationRunRecorder(jdbcTemplate, new ObjectMapper());

        recorder.markFailed(42L, new IllegalStateException("x".repeat(1001)));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(any(String.class), eq("IllegalStateException"), message.capture(), eq(42L));
        assertThat(message.getValue()).hasSize(1000);
    }

    @Test
    void recordsOutcomeWithTruncatedErrorMessage() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ClientHubMigrationRunRecorder recorder = new ClientHubMigrationRunRecorder(jdbcTemplate, new ObjectMapper());

        recorder.recordOutcome(
                42L,
                new TargetInventory(1, 0, 7L, "tenant_real"),
                "clients",
                "__readiness__",
                "BLOCKED",
                "BLOCKED",
                "READINESS_BLOCKED",
                "x".repeat(1001));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(
                any(String.class),
                eq(42L),
                eq(7L),
                eq("clients"),
                eq("__readiness__"),
                eq("BLOCKED"),
                eq("BLOCKED"),
                eq("READINESS_BLOCKED"),
                message.capture());
        assertThat(message.getValue()).hasSize(1000);
    }

    @Test
    void skipsOutcomeWhenRunOrTargetIsUnresolved() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ClientHubMigrationRunRecorder recorder = new ClientHubMigrationRunRecorder(jdbcTemplate, new ObjectMapper());

        recorder.recordOutcome(null, new TargetInventory(1, 0, 7L, "tenant_real"),
                "clients", "__readiness__", "NO_OP", "SUCCEEDED", null, null);
        recorder.recordOutcome(42L, new TargetInventory(0, 0, null, null),
                "clients", "__readiness__", "NO_OP", "SUCCEEDED", null, null);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void linksExistingMappingsToExecuteRun() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ClientHubMigrationRunRecorder recorder = new ClientHubMigrationRunRecorder(jdbcTemplate, new ObjectMapper());

        recorder.linkMappingsToRun(42L, new TargetInventory(1, 0, 7L, "tenant_real"));

        verify(jdbcTemplate).update(any(String.class), eq(42L), eq(42L), eq(7L));
    }

    @Test
    void skipsMappingLinkageWhenRunOrTargetIsUnresolved() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ClientHubMigrationRunRecorder recorder = new ClientHubMigrationRunRecorder(jdbcTemplate, new ObjectMapper());

        recorder.linkMappingsToRun(null, new TargetInventory(1, 0, 7L, "tenant_real"));
        recorder.linkMappingsToRun(42L, new TargetInventory(0, 0, null, null));

        verifyNoInteractions(jdbcTemplate);
    }
}
