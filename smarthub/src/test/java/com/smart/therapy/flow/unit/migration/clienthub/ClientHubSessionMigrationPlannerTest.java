package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSessionMigrationPlanner.SessionMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SessionTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSessionInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSessionNoteRef;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSessionRef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHubSessionMigrationPlannerTest {

    private final ClientHubSessionMigrationPlanner planner = new ClientHubSessionMigrationPlanner();

    @Test
    void countsSessionAndNoteCreateUpdateWhenDependenciesExist() {
        SessionMigrationPlan plan = planner.buildPlan(
                inventory(2, 2),
                List.of(
                        session("1", "10", "20", "30"),
                        session("2", "10", "20", "30")),
                List.of(
                        note("100", "1", "10", "20"),
                        note("101", "2", "10", "20")),
                new SessionTargetState(
                        Set.of("10"),
                        Set.of("20"),
                        Set.of("30"),
                        Set.of("2"),
                        Set.of("101")));

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.sessionsWouldCreate()).isEqualTo(1);
        assertThat(plan.sessionsWouldUpdateMapped()).isEqualTo(1);
        assertThat(plan.notesWouldCreate()).isEqualTo(1);
        assertThat(plan.notesWouldUpdateMapped()).isEqualTo(1);
    }

    @Test
    void blocksSessionsWhenServiceMappingIsMissing() {
        SessionMigrationPlan plan = planner.buildPlan(
                inventory(1, 1),
                List.of(session("1", "10", "20", "missing-service")),
                List.of(note("100", "1", "10", "20")),
                new SessionTargetState(Set.of("10"), Set.of("20"), Set.of(), Set.of(), Set.of()));

        assertThat(plan.blocked()).isTrue();
        assertThat(plan.sessionsBlocked()).isEqualTo(1);
        assertThat(plan.notesBlocked()).isEqualTo(1);
        assertThat(plan.warnings())
                .contains("No ClientHubAI service mappings exist; session execution must wait for service catalog mapping");
        assertThat(plan.blockers())
                .contains("Sessions blocked by missing dependency mappings: 1")
                .contains("Session notes blocked by missing dependency mappings: 1");
    }

    @Test
    void blocksRowsWithMissingRequiredForeignKeysOrDates() {
        SessionMigrationPlan plan = planner.buildPlan(
                new SourceSessionInventory(1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1),
                List.of(new SourceSessionRef("1", null, "20", "30", false, true)),
                List.of(new SourceSessionNoteRef("100", null, "10", "20", false)),
                new SessionTargetState(Set.of("10"), Set.of("20"), Set.of("30"), Set.of(), Set.of()));

        assertThat(plan.blocked()).isTrue();
        assertThat(plan.blockers())
                .contains("Sessions missing client_id: 1")
                .contains("Sessions missing session_date: 1")
                .contains("Session notes missing session_id: 1")
                .contains("Session notes missing date: 1");
    }

    private SourceSessionInventory inventory(long sessions, long notes) {
        return new SourceSessionInventory(sessions, 0, 0, 0, 0, 0, notes, 0, 0, 0, 0);
    }

    private SourceSessionRef session(String id, String clientId, String therapistId, String serviceId) {
        return new SourceSessionRef(id, clientId, therapistId, serviceId, true, true);
    }

    private SourceSessionNoteRef note(String id, String sessionId, String clientId, String therapistId) {
        return new SourceSessionNoteRef(id, sessionId, clientId, therapistId, true);
    }
}
