package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTaskCommentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTaskInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTaskRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TaskTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubTaskMigrationPlanner.TaskMigrationPlan;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHubTaskMigrationPlannerTest {

    private final ClientHubTaskMigrationPlanner planner = new ClientHubTaskMigrationPlanner();

    @Test
    void countsCreatesAndMappedUpdates() {
        TaskMigrationPlan plan = planner.buildPlan(
                new SourceTaskInventory(2, 0, 0, 1, 0),
                List.of(
                        task("1", "10", "20"),
                        task("2", "11", null)),
                List.of(comment("100", "1", "20")),
                new TaskTargetState(Set.of("10", "11"), Set.of("20"), Set.of("2"), Set.of()));

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.tasksWouldCreate()).isEqualTo(1);
        assertThat(plan.tasksWouldUpdateMapped()).isEqualTo(1);
        assertThat(plan.commentsWouldCreate()).isEqualTo(1);
    }

    @Test
    void blocksUnmappedClientsAssigneesAndComments() {
        TaskMigrationPlan plan = planner.buildPlan(
                new SourceTaskInventory(2, 1, 0, 2, 1),
                List.of(
                        new SourceTaskRecord("1", "missing", "20", "Title", null, "pending", "medium",
                                null, null, Instant.now(), Instant.now()),
                        task("2", "10", "missing-user")),
                List.of(
                        comment("100", "missing-task", "20"),
                        new SourceTaskCommentRecord("101", "2", "20", "", false, Instant.now(), Instant.now())),
                new TaskTargetState(Set.of("10"), Set.of("20"), Set.of(), Set.of()));

        assertThat(plan.blocked()).isTrue();
        assertThat(plan.tasksBlocked()).isEqualTo(2);
        assertThat(plan.commentsBlocked()).isEqualTo(2);
        assertThat(plan.blockers())
                .contains("Tasks missing title: 1")
                .contains("Tasks reference unmapped clients: 1")
                .contains("Tasks reference unmapped assignees: 1")
                .contains("Task comments reference unmapped tasks: 1")
                .contains("Task comments missing content: 1");
    }

    private SourceTaskRecord task(String id, String clientId, String assigneeId) {
        return new SourceTaskRecord(
                id, clientId, assigneeId, "Task " + id, null, "pending", "medium",
                null, null, Instant.now(), Instant.now());
    }

    private SourceTaskCommentRecord comment(String id, String taskId, String authorId) {
        return new SourceTaskCommentRecord(
                id, taskId, authorId, "Comment " + id, false, Instant.now(), Instant.now());
    }
}
