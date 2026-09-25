package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTaskCommentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTaskInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTaskRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TaskTargetState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClientHubTaskMigrationPlanner {

    TaskMigrationPlan buildPlan(
            SourceTaskInventory inventory,
            List<SourceTaskRecord> sourceTasks,
            List<SourceTaskCommentRecord> sourceComments,
            TaskTargetState targetState) {
        int tasksWouldCreate = 0;
        int tasksWouldUpdateMapped = 0;
        int tasksBlocked = 0;
        int tasksWithUnmappedClient = 0;
        int tasksWithUnmappedAssignee = 0;

        for (SourceTaskRecord source : sourceTasks) {
            if (source.missingRequiredFields()) {
                tasksBlocked++;
                continue;
            }
            if (!targetState.mappedClientIds().contains(source.clientLegacyId())) {
                tasksBlocked++;
                tasksWithUnmappedClient++;
                continue;
            }
            if (source.assignedToLegacyId() != null
                    && !targetState.mappedUserIds().contains(source.assignedToLegacyId())) {
                tasksBlocked++;
                tasksWithUnmappedAssignee++;
                continue;
            }
            if (targetState.mappedTaskIds().contains(source.legacyTaskPk())) {
                tasksWouldUpdateMapped++;
            } else {
                tasksWouldCreate++;
            }
        }

        int commentsWouldCreate = 0;
        int commentsWouldUpdateMapped = 0;
        int commentsBlocked = 0;
        int commentsWithUnmappedTask = 0;
        int commentsWithUnmappedAuthor = 0;
        for (SourceTaskCommentRecord source : sourceComments) {
            if (source.missingRequiredFields()) {
                commentsBlocked++;
                continue;
            }
            boolean taskMapped = targetState.mappedTaskIds().contains(source.taskLegacyId())
                    || sourceTasks.stream().anyMatch(task -> task.legacyTaskPk().equals(source.taskLegacyId())
                    && !task.missingRequiredFields()
                    && targetState.mappedClientIds().contains(task.clientLegacyId())
                    && (task.assignedToLegacyId() == null
                    || targetState.mappedUserIds().contains(task.assignedToLegacyId())));
            if (!taskMapped) {
                commentsBlocked++;
                commentsWithUnmappedTask++;
                continue;
            }
            if (!targetState.mappedUserIds().contains(source.authorLegacyId())) {
                commentsBlocked++;
                commentsWithUnmappedAuthor++;
                continue;
            }
            if (targetState.mappedTaskCommentIds().contains(source.legacyCommentPk())) {
                commentsWouldUpdateMapped++;
            } else {
                commentsWouldCreate++;
            }
        }

        List<String> blockers = new ArrayList<>();
        addBlocker(blockers, inventory.blankTitleRows(), "Tasks missing title");
        addBlocker(blockers, inventory.blankClientRows(), "Tasks missing client_id");
        addBlocker(blockers, tasksWithUnmappedClient, "Tasks reference unmapped clients");
        addBlocker(blockers, tasksWithUnmappedAssignee, "Tasks reference unmapped assignees");
        addBlocker(blockers, commentsWithUnmappedTask, "Task comments reference unmapped tasks");
        addBlocker(blockers, commentsWithUnmappedAuthor, "Task comments reference unmapped authors");
        addBlocker(blockers, inventory.blankCommentContentRows(), "Task comments missing content");

        return new TaskMigrationPlan(
                sourceTasks.size(),
                tasksWouldCreate,
                tasksWouldUpdateMapped,
                tasksBlocked,
                sourceComments.size(),
                commentsWouldCreate,
                commentsWouldUpdateMapped,
                commentsBlocked,
                blockers);
    }

    private void addBlocker(List<String> blockers, long count, String label) {
        if (count > 0) {
            blockers.add(label + ": " + count);
        }
    }

    record TaskMigrationPlan(
            int sourceTasks,
            int tasksWouldCreate,
            int tasksWouldUpdateMapped,
            int tasksBlocked,
            int sourceComments,
            int commentsWouldCreate,
            int commentsWouldUpdateMapped,
            int commentsBlocked,
            List<String> blockers) {

        boolean blocked() {
            return tasksBlocked > 0 || commentsBlocked > 0 || !blockers.isEmpty();
        }
    }
}
