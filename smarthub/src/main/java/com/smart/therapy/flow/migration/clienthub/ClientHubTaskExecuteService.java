package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTaskCommentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTaskRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import com.smart.therapy.flow.system.service.SystemOptionKeyMatcher;
import com.smart.therapy.flow.task.entity.Task;
import com.smart.therapy.flow.task.entity.TaskComment;
import com.smart.therapy.flow.task.repository.TaskCommentRepository;
import com.smart.therapy.flow.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ClientHubTaskExecuteService {

    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final JdbcTemplate jdbcTemplate;
    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    public TaskExecuteResult execute(
            List<SourceTaskRecord> sourceTasks,
            List<SourceTaskCommentRecord> sourceComments,
            TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required for task execution");
        }
        return tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(),
                () -> executeInTenant(sourceTasks, sourceComments, target));
    }

    private TaskExecuteResult executeInTenant(
            List<SourceTaskRecord> sourceTasks,
            List<SourceTaskCommentRecord> sourceComments,
            TargetInventory target) {
        Map<String, Long> clientMappings = loadMappings(target.organisationId(), "clients");
        Map<String, Long> userMappings = loadMappings(target.organisationId(), "users");
        Map<String, Long> taskMappings = loadMappings(target.organisationId(), "tasks");
        Map<String, Long> commentMappings = loadMappings(target.organisationId(), "task_comments");

        int tasksCreated = 0;
        int tasksUpdated = 0;
        int taskMappedReruns = 0;
        for (SourceTaskRecord source : sourceTasks) {
            Optional<Long> mappedTaskId = Optional.ofNullable(taskMappings.get(source.legacyTaskPk()));
            Task task = mappedTaskId.flatMap(taskRepository::findById).orElseGet(Task::new);
            boolean existing = task.getId() != null;

            applyTaskFields(task, source, clientMappings, userMappings);
            Task saved = taskRepository.save(task);
            taskMappings.put(source.legacyTaskPk(), saved.getId());
            upsertLegacyMapping(target, "tasks", "tasks", source.legacyTaskPk(), saved.getId(), taskChecksum(source));

            if (existing) {
                tasksUpdated++;
                if (mappedTaskId.isPresent()) {
                    taskMappedReruns++;
                }
            } else {
                tasksCreated++;
            }
        }

        int commentsCreated = 0;
        int commentsUpdated = 0;
        int commentMappedReruns = 0;
        for (SourceTaskCommentRecord source : sourceComments) {
            Long targetTaskId = requiredMapping(taskMappings, source.taskLegacyId(), "tasks");
            Optional<Long> mappedCommentId = Optional.ofNullable(commentMappings.get(source.legacyCommentPk()));
            TaskComment comment = mappedCommentId.flatMap(taskCommentRepository::findById).orElseGet(TaskComment::new);
            boolean existing = comment.getId() != null;

            applyCommentFields(comment, source, targetTaskId, userMappings);
            TaskComment saved = taskCommentRepository.save(comment);
            commentMappings.put(source.legacyCommentPk(), saved.getId());
            upsertLegacyMapping(target, "task_comments", "task_comments",
                    source.legacyCommentPk(), saved.getId(), commentChecksum(source));

            if (existing) {
                commentsUpdated++;
                if (mappedCommentId.isPresent()) {
                    commentMappedReruns++;
                }
            } else {
                commentsCreated++;
            }
        }

        return new TaskExecuteResult(
                sourceTasks.size(),
                tasksCreated,
                tasksUpdated,
                taskMappedReruns,
                sourceComments.size(),
                commentsCreated,
                commentsUpdated,
                commentMappedReruns);
    }

    private void applyTaskFields(
            Task task,
            SourceTaskRecord source,
            Map<String, Long> clientMappings,
            Map<String, Long> userMappings) {
        task.setTitle(source.title().trim());
        task.setDescription(trim(source.description()));
        task.setStatus(normaliseEnum(source.status(), "pending"));
        task.setPriority(normaliseEnum(source.priority(), "medium"));
        task.setDueDate(source.dueDate());
        task.setCompletedAt(source.completedAt());
        task.setClient(clientRepository.getReferenceById(
                requiredMapping(clientMappings, source.clientLegacyId(), "clients")));
        if (source.assignedToLegacyId() == null) {
            task.setAssignedTo(null);
        } else {
            task.setAssignedTo(userRepository.getReferenceById(
                    requiredMapping(userMappings, source.assignedToLegacyId(), "users")));
        }
        task.setIsDeleted(false);
        task.setDeletedAt(null);
        if (source.createdAt() != null) {
            task.setCreatedAt(source.createdAt());
        }
        if (source.updatedAt() != null) {
            task.setUpdatedAt(source.updatedAt());
        }
        task.setCreatedBy(0L);
        task.setUpdatedBy(0L);
    }

    private void applyCommentFields(
            TaskComment comment,
            SourceTaskCommentRecord source,
            Long targetTaskId,
            Map<String, Long> userMappings) {
        comment.setTask(taskRepository.getReferenceById(targetTaskId));
        comment.setCommentText(source.content().trim());
        comment.setIsInternal(source.internal());
        comment.setCreatedByUser(userRepository.getReferenceById(
                requiredMapping(userMappings, source.authorLegacyId(), "users")));
        comment.setIsDeleted(false);
        comment.setDeletedAt(null);
        if (source.createdAt() != null) {
            comment.setCreatedAt(source.createdAt());
        }
        if (source.updatedAt() != null) {
            comment.setUpdatedAt(source.updatedAt());
        }
        comment.setCreatedBy(0L);
        comment.setUpdatedBy(0L);
    }

    private Map<String, Long> loadMappings(Long organisationId, String entityName) {
        Map<String, Long> mappings = new HashMap<>();
        jdbcTemplate.query("""
                SELECT source_id, target_id
                FROM public.clienthub_legacy_id_mappings
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                  AND entity_name = ?
                """,
                (RowCallbackHandler) rs -> mappings.put(rs.getString("source_id"), rs.getLong("target_id")),
                organisationId,
                entityName);
        return mappings;
    }

    private void upsertLegacyMapping(
            TargetInventory target,
            String entityName,
            String targetTable,
            String sourceId,
            Long targetId,
            String checksum) {
        jdbcTemplate.update("""
                INSERT INTO public.clienthub_legacy_id_mappings (
                    organisation_id, source_system, entity_name, source_id,
                    target_schema, target_table, target_id, source_checksum_sha256,
                    first_seen_run_id, last_seen_run_id
                )
                VALUES (?, 'ClientHubAI', ?, ?, ?, ?, ?, ?, NULL, NULL)
                ON CONFLICT (organisation_id, source_system, entity_name, source_id)
                DO UPDATE SET
                    target_schema = EXCLUDED.target_schema,
                    target_table = EXCLUDED.target_table,
                    target_id = EXCLUDED.target_id,
                    source_checksum_sha256 = EXCLUDED.source_checksum_sha256,
                    updated_at = CURRENT_TIMESTAMP
                """,
                target.organisationId(),
                entityName,
                sourceId,
                target.schemaName(),
                targetTable,
                targetId,
                checksum);
    }

    private Long requiredMapping(Map<String, Long> mappings, String sourceId, String entityName) {
        Long targetId = mappings.get(sourceId);
        if (targetId == null) {
            throw new IllegalStateException("Missing ClientHubAI " + entityName + " mapping for source id " + sourceId);
        }
        return targetId;
    }

    private String taskChecksum(SourceTaskRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyTaskPk(),
                source.clientLegacyId(),
                String.valueOf(source.assignedToLegacyId()),
                source.title(),
                String.valueOf(source.status()),
                String.valueOf(source.priority()),
                String.valueOf(source.dueDate()),
                String.valueOf(source.completedAt())));
    }

    private String commentChecksum(SourceTaskCommentRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyCommentPk(),
                source.taskLegacyId(),
                source.authorLegacyId(),
                source.content(),
                Boolean.toString(source.internal())));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String normaliseEnum(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = SystemOptionKeyMatcher.normalize(value);
        // Defensive aliases seen in ClientHubAI exports
        if ("urgen".equals(normalized) || "urgency".equals(normalized)) {
            return "urgent";
        }
        if ("inprogress".equals(normalized)) {
            return "in_progress";
        }
        return normalized;
    }

    public record TaskExecuteResult(
            int sourceTasks,
            int tasksCreated,
            int tasksUpdated,
            int taskMappedReruns,
            int sourceComments,
            int commentsCreated,
            int commentsUpdated,
            int commentMappedReruns) {
    }
}
