package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTranscriptRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTranscriptsInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TranscriptsTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubTranscriptsMigrationPlanner.TranscriptsMigrationPlan;
import com.smart.therapy.flow.session.enums.SessionTranscriptStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHubTranscriptsMigrationPlannerTest {

    private final ClientHubTranscriptsMigrationPlanner planner = new ClientHubTranscriptsMigrationPlanner();

    @Test
    void countsCreatesAndMappedUpdates() {
        TranscriptsMigrationPlan plan = planner.buildPlan(
                emptyInventory(),
                List.of(
                        transcript("1", "10", "20", "30"),
                        transcript("2", "11", "21", "31")),
                new TranscriptsTargetState(
                        Set.of("10", "11"),
                        Set.of("20", "21"),
                        Set.of("30", "31"),
                        Set.of("2")));

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.wouldCreate()).isEqualTo(1);
        assertThat(plan.wouldUpdateMapped()).isEqualTo(1);
        assertThat(plan.blockedRows()).isZero();
    }

    @Test
    void blocksUnmappedDependencies() {
        TranscriptsMigrationPlan plan = planner.buildPlan(
                new SourceTranscriptsInventory(1, 0, 0, 0),
                List.of(transcript("1", "missing-session", "20", "30")),
                new TranscriptsTargetState(Set.of(), Set.of("20"), Set.of("30"), Set.of()));

        assertThat(plan.blocked()).isTrue();
        assertThat(plan.blockedRows()).isEqualTo(1);
        assertThat(plan.blockers()).anyMatch(blocker -> blocker.contains("unmapped sessions"));
    }

    @Test
    void mapsStatusWithReadyFallback() {
        assertThat(ClientHubTranscriptsExecuteService.mapStatus("ready"))
                .isEqualTo(SessionTranscriptStatus.READY);
        assertThat(ClientHubTranscriptsExecuteService.mapStatus("completed"))
                .isEqualTo(SessionTranscriptStatus.READY);
        assertThat(ClientHubTranscriptsExecuteService.mapStatus("weird-ready-state"))
                .isEqualTo(SessionTranscriptStatus.READY);
        assertThat(ClientHubTranscriptsExecuteService.mapStatus("failed"))
                .isEqualTo(SessionTranscriptStatus.FAILED);
        assertThat(ClientHubTranscriptsExecuteService.resolveUploadId(
                transcript("9", "1", "2", "3"))).isEqualTo("ch-transcript-9");
    }

    private SourceTranscriptsInventory emptyInventory() {
        return new SourceTranscriptsInventory(0, 0, 0, 0);
    }

    private SourceTranscriptRecord transcript(
            String id, String sessionId, String clientId, String therapistId) {
        return new SourceTranscriptRecord(
                id, sessionId, clientId, therapistId,
                "hello", "raw", "en", true, 60, 3, 12, "ready", null,
                Instant.now(), Instant.now(), null);
    }
}
