package com.smart.therapy.flow.task.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientSearchHelper;
import com.smart.therapy.flow.notification.service.NotificationEventCatalog;
import com.smart.therapy.flow.notification.service.NotificationPayloadFactory;
import com.smart.therapy.flow.notification.service.NotificationService;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.dto.PatchUpdates;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.client.enums.Priority;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.task.dto.*;
import com.smart.therapy.flow.system.service.SystemOptionCategories;
import com.smart.therapy.flow.system.service.SystemOptionKeyMatcher;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import org.springframework.data.domain.Sort;
import com.smart.therapy.flow.task.entity.Task;
import com.smart.therapy.flow.task.entity.TaskComment;
import com.smart.therapy.flow.task.repository.TaskCommentRepository;
import com.smart.therapy.flow.task.repository.TaskRepository;
import com.smart.therapy.flow.user.entity.SupervisorAssignment;
import com.smart.therapy.flow.user.repository.SupervisorAssignmentRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {

    private static final String RESOURCE_TYPE_TASK = "task";

    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final SupervisorAssignmentRepository supervisorAssignmentRepository;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final SystemOptionResolverService systemOptionResolverService;
    private final ClientSearchHelper clientSearchHelper;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Transactional(readOnly = true)
    public PaginatedResponse<TaskResponse> getTasks(
            int page,
            int pageSize,
            String status,
            String priority,
            Long assignedToId,
            Long clientId,
            String search,
            String dateFilter,
            LocalDate fromDate,
            LocalDate toDate,
            String dateField,
            String sortBy,
            String sortOrder,
            AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        Pageable pageable = PageRequest.of(
                page - 1,
                pageSize,
                Sort.by("desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC,
                        sortBy != null ? sortBy : "createdAt"));

        Specification<Task> specification = buildTaskSpecification(
                status, priority, assignedToId, clientId, search, dateFilter, fromDate, toDate, dateField, requester
        );
        Page<Task> results = taskRepository.findAll(specification, pageable);
        Map<Long, Long> commentCounts = getCommentCounts(results.getContent());

        List<TaskResponse> payload = results.getContent().stream()
                .map(task -> toTaskResponse(task, commentCounts.getOrDefault(task.getId(), 0L)))
                .collect(Collectors.toList());

        return PaginatedResponse.of(payload, results.getTotalElements(), page, pageSize);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tasks", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey(#taskId)")
    public TaskResponse getTask(Long taskId, AuthPrincipal requester) {
        Objects.requireNonNull(taskId, "Task id is required");
        Objects.requireNonNull(requester, "Requester is required");

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        validateTaskAccess(task, requester);
        return toTaskResponse(task, Optional.ofNullable(taskCommentRepository.countByTaskId(taskId)).orElse(0L));
    }

    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public TaskResponse createTask(CreateTaskRequest request, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null) {
            subscriptionFeatureService.consumeUsageOrThrow(
                    orgId,
                    SubscriptionFeatureService.FEATURE_TASK_LIMIT,
                    1L,
                    "Task creation");
        }

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        User assignedTo = request.getAssignedToId() != null
                ? userRepository.findById(request.getAssignedToId()).orElse(null)
                : null;

        // PBAC: Validate supervisor can assign task to this therapist
        // Admin bypasses all supervisor checks; self-assignment is always allowed for therapists
        boolean isAdmin = permissionChecker.hasConsentAdminModuleAccess(requester);
        boolean isSupervisor = hasRole(requester, "SUPERVISOR");

        if (!isAdmin && assignedTo != null) {
            Long currentUserId = currentUserService.requireCurrentUser(requester).getId();

            // Allow self-assignment without supervisor checks
            if (!assignedTo.getId().equals(currentUserId)) {
                // Only supervisors need to validate supervised therapists list
                if (isSupervisor) {
                    List<Long> supervisedTherapistIds = getSupervisedTherapistIds(currentUserId);

                    if (supervisedTherapistIds.isEmpty()) {
                        throw new ForbiddenException("You have no supervised therapists assigned. Cannot assign tasks.");
                    }

                    if (!supervisedTherapistIds.contains(assignedTo.getId())) {
                        throw new ForbiddenException(
                                "You can only assign tasks to therapists you supervise. Therapist ID " +
                                        assignedTo.getId() + " is not in your supervised therapists list.");
                    }
                }
                // Non-supervisor, non-admin users cannot assign tasks to others
                else if (permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM")) {
                    throw new ForbiddenException("You can only assign tasks to yourself.");
                }
            }
        }

        // PBAC: Validate client belongs to supervised therapist (if client is assigned to therapist)
        // Admin bypasses all checks; skip supervisor check when requester is the assigned therapist
        if (!isAdmin && client.getAssignedTherapist() != null) {
            Long currentUserId = currentUserService.requireCurrentUser(requester).getId();
            boolean isSelfAssignment = assignedTo != null && assignedTo.getId().equals(currentUserId)
                    || (assignedTo == null && client.getAssignedTherapist().getId().equals(currentUserId));

            if (!isSelfAssignment && isSupervisor) {
                List<Long> supervisedTherapistIds = getSupervisedTherapistIds(currentUserId);
                if (!supervisedTherapistIds.contains(client.getAssignedTherapist().getId())) {
                    throw new ForbiddenException(
                            "You can only create tasks for clients assigned to therapists you supervise. " +
                                    "Client is assigned to therapist ID " + client.getAssignedTherapist().getId() +
                                    " which is not in your supervised therapists list.");
                }
            }
        }

        TaskTitleResolution titleResolution = resolveTaskTitle(request.getTitle(), request.getTitleKey());

        Task task = Task.builder()
                .title(titleResolution.title())
                .titleKey(titleResolution.titleKey())
                .taskType(parseTaskType(request.getTaskType()))
                .description(request.getDescription())
                .status(systemOptionResolverService.parseOptionKey(
                        SystemOptionCategories.TASK_STATUS, request.getStatus(), "pending"))
                .priority(systemOptionResolverService.parseOptionKey(
                        SystemOptionCategories.TASK_PRIORITY, request.getPriority(), "medium"))
                .dueDate(request.getDueDate())
                .client(client)
                .assignedTo(assignedTo)
                .build();

        Task saved = Objects.requireNonNull(taskRepository.save(task), "Persisted task must not be null");
        Long savedId = requireTaskId(saved);

        // Trigger notification
        if (notificationService != null && saved.getAssignedTo() != null) {
            try {
                notificationService.processEventInNewTransaction(NotificationEventCatalog.TASK_ASSIGNED, buildTaskEventData(saved));
            } catch (Exception e) {
                log.error("Failed to trigger task_assigned notification", e);
            }
        }

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "task_created", savedId, ipAddress, false);

        return toTaskResponse(saved, Optional.ofNullable(taskCommentRepository.countByTaskId(savedId)).orElse(0L));
    }

    @Transactional
    @CacheEvict(value = "tasks", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey(#taskId)")
    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(taskId, "Task id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        validateTaskAccess(task, requester);

        if (request.isAnyFieldPresent("titleKey", "title")) {
            TaskTitleResolution titleResolution = resolveTaskTitle(request.getTitle(), request.getTitleKey());
            task.setTitle(titleResolution.title());
            task.setTitleKey(titleResolution.titleKey());
        }
        if (request.isFieldPresent("taskType")) {
            task.setTaskType(StringUtils.hasText(request.getTaskType())
                    ? parseTaskType(request.getTaskType())
                    : null);
        }
        PatchUpdates.apply(request, "description", request.getDescription(), task::setDescription);
        if (request.isFieldPresent("status")) {
            if (!StringUtils.hasText(request.getStatus())) {
                throw new BadRequestException("status cannot be cleared");
            }
            task.setStatus(systemOptionResolverService.requireOptionKey(
                    SystemOptionCategories.TASK_STATUS, request.getStatus()));
        }
        if (request.isFieldPresent("priority")) {
            if (!StringUtils.hasText(request.getPriority())) {
                task.setPriority(null);
            } else {
                task.setPriority(systemOptionResolverService.requireOptionKey(
                        SystemOptionCategories.TASK_PRIORITY, request.getPriority()));
            }
        }
        PatchUpdates.apply(request, "dueDate", request.getDueDate(), task::setDueDate);
        if (request.isFieldPresent("clientId")) {
            if (request.getClientId() == null) {
                task.setClient(null);
            } else {
                Client client = clientRepository.findById(request.getClientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
                task.setClient(client);
            }
        }
        boolean assigneeChanged = false;
        if (request.isFieldPresent("assignedToId")) {
            User assignedTo = request.getAssignedToId() != null
                    ? userRepository.findById(request.getAssignedToId()).orElse(null)
                    : null;

            // Validate supervisor can reassign to this therapist
            if (hasRole(requester, "SUPERVISOR") && assignedTo != null) {
                List<Long> supervisedTherapistIds = getSupervisedTherapistIds(currentUserService.requireCurrentUser(requester).getId());

                if (supervisedTherapistIds.isEmpty()) {
                    throw new ForbiddenException("You have no supervised therapists assigned. Cannot reassign tasks.");
                }

                if (!supervisedTherapistIds.contains(assignedTo.getId())) {
                    throw new ForbiddenException(
                            "You can only reassign tasks to therapists you supervise. Therapist ID " +
                                    assignedTo.getId() + " is not in your supervised therapists list.");
                }
            }

            Long previousAssigneeId = task.getAssignedTo() != null ? task.getAssignedTo().getId() : null;
            assigneeChanged = assignedTo != null && !assignedTo.getId().equals(previousAssigneeId);
            task.setAssignedTo(assignedTo);
        }

        Task updated = Objects.requireNonNull(taskRepository.save(task), "Persisted task must not be null");
        Long updatedId = requireTaskId(updated);

        // Check for overdue
        if (updated.getDueDate() != null && updated.getDueDate().isBefore(Instant.now()) &&
                !SystemOptionKeyMatcher.matchesAny(updated.getStatus(), "completed", "overdue")) {
            updated.setStatus("overdue");
            updated = taskRepository.save(updated);

            // Trigger overdue notification
            if (notificationService != null) {
                try {
                    notificationService.processEventInNewTransaction(NotificationEventCatalog.TASK_OVERDUE, buildTaskEventData(updated));
                } catch (Exception e) {
                    log.error("Failed to trigger task_overdue notification", e);
                }
            }
        }

        // Trigger notification for the new assignee
        if (assigneeChanged && notificationService != null && updated.getAssignedTo() != null) {
            try {
                notificationService.processEventInNewTransaction(NotificationEventCatalog.TASK_ASSIGNED, buildTaskEventData(updated));
            } catch (Exception e) {
                log.error("Failed to trigger task_assigned notification", e);
            }
        }

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "task_updated", updatedId, ipAddress, false);
        return toTaskResponse(updated, Optional.ofNullable(taskCommentRepository.countByTaskId(updatedId)).orElse(0L));
    }

    @Transactional
    @CacheEvict(value = "tasks", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey(#taskId)")
    public void deleteTask(Long taskId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(taskId, "Task id is required");
        Objects.requireNonNull(requester, "Requester is required");

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        validateTaskAccess(task, requester);
        taskRepository.delete(task);

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "task_deleted", taskId, ipAddress, false);
    }

    @Transactional(readOnly = true)
    public TaskStatsResponse getTaskStats(
            AuthPrincipal requester,
            Long assignedToId,
            LocalDate fromDate,
            LocalDate toDate,
            String dateField
    ) {
        Objects.requireNonNull(requester, "Requester is required");

        Specification<Task> baseSpec = buildTaskSpecification(
                null, null, assignedToId, null, null, null, fromDate, toDate, dateField, requester
        );

        long totalTasks = taskRepository.count(baseSpec);
        long pendingTasks = taskRepository
                .count(baseSpec.and(optionKeyEquals("status", "pending")));
        long inProgressTasks = taskRepository
                .count(baseSpec.and(optionKeyEquals("status", "in_progress")));
        long completedTasks = taskRepository
                .count(baseSpec.and(optionKeyEquals("status", "completed")));
        long overdueTasks = taskRepository
                .count(baseSpec.and(optionKeyEquals("status", "overdue")));
        long highPriorityTasks = taskRepository
                .count(baseSpec.and(optionKeyEquals("priority", "high")));
        long urgentTasks = taskRepository
                .count(baseSpec.and(optionKeyEquals("priority", "urgent")));
        long needsAttentionTasks = taskRepository.count(baseSpec.and((root, query, cb) -> cb.or(
                optionKeyEquals("status", "overdue").toPredicate(root, query, cb),
                optionKeyEquals("priority", "high").toPredicate(root, query, cb),
                optionKeyEquals("priority", "urgent").toPredicate(root, query, cb)
        )));

        return TaskStatsResponse.builder()
                .totalTasks(totalTasks)
                .pendingTasks(pendingTasks)
                .inProgressTasks(inProgressTasks)
                .completedTasks(completedTasks)
                .overdueTasks(overdueTasks)
                .needsAttentionTasks(needsAttentionTasks)
                .highPriorityTasks(highPriorityTasks)
                .urgentTasks(urgentTasks)
                .build();
    }

    /**
     * Task stats for therapist dashboard cards. Always scoped to tasks assigned to the
     * current therapist or linked to their assigned clients.
     */
    @Transactional(readOnly = true)
    public TaskStatsResponse getTaskStatsForTherapist(AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");
        Long therapistId = currentUserService.requireCurrentUser(requester).getId();

        Specification<Task> baseSpec = (root, query, cb) -> cb.or(
                cb.equal(root.get("assignedTo").get("id"), therapistId),
                cb.equal(root.get("client").get("assignedTherapist").get("id"), therapistId));

        long totalTasks = taskRepository.count(baseSpec);
        long pendingTasks = taskRepository
                .count(baseSpec.and(optionKeyEquals("status", "pending")));
        long inProgressTasks = taskRepository
                .count(baseSpec.and(optionKeyEquals("status", "in_progress")));
        long completedTasks = taskRepository
                .count(baseSpec.and(optionKeyEquals("status", "completed")));
        long overdueTasks = taskRepository
                .count(baseSpec.and(optionKeyEquals("status", "overdue")));
        long highPriorityTasks = taskRepository
                .count(baseSpec.and(optionKeyEquals("priority", "high")));
        long urgentTasks = taskRepository
                .count(baseSpec.and(optionKeyEquals("priority", "urgent")));
        long needsAttentionTasks = taskRepository.count(baseSpec.and((root, query, cb) -> cb.or(
                optionKeyEquals("status", "overdue").toPredicate(root, query, cb),
                optionKeyEquals("priority", "high").toPredicate(root, query, cb),
                optionKeyEquals("priority", "urgent").toPredicate(root, query, cb)
        )));

        return TaskStatsResponse.builder()
                .totalTasks(totalTasks)
                .pendingTasks(pendingTasks)
                .inProgressTasks(inProgressTasks)
                .completedTasks(completedTasks)
                .overdueTasks(overdueTasks)
                .needsAttentionTasks(needsAttentionTasks)
                .highPriorityTasks(highPriorityTasks)
                .urgentTasks(urgentTasks)
                .build();
    }

    @Transactional
    public void checkOverdueTasks(AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        Instant now = Instant.now();
        List<Task> overdueTasks = taskRepository.findOverdueTasks(now);

        for (Task task : overdueTasks) {
            if (!SystemOptionKeyMatcher.matchesAny(task.getStatus(), "overdue", "completed")) {
                task.setStatus("overdue");
                taskRepository.save(task);

                if (notificationService != null) {
                    try {
                        notificationService.processEventInNewTransaction(NotificationEventCatalog.TASK_OVERDUE, buildTaskEventData(task));
                    } catch (Exception e) {
                        log.error("Failed to trigger task_overdue notification for task {}", task.getId(), e);
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<TaskCommentResponse> getTaskComments(Long taskId, AuthPrincipal requester) {
        Objects.requireNonNull(taskId, "Task id is required");
        Objects.requireNonNull(requester, "Requester is required");

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        validateTaskAccess(task, requester);

        List<TaskComment> comments = taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
        long totalCommentsCount = comments.size();
        return comments.stream()
                .map(comment -> toCommentResponse(comment, totalCommentsCount))
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskCommentResponse createTaskComment(Long taskId, CreateTaskCommentRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(taskId, "Task id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        validateTaskAccess(task, requester);

        User author = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TaskComment comment = TaskComment.builder()
                .task(task)
                .createdByUser(author)
                .commentText(request.getContent().trim())
                .isInternal(request.getIsInternal() != null ? request.getIsInternal() : false)
                .build();

        TaskComment saved = Objects.requireNonNull(taskCommentRepository.save(comment),
                "Persisted comment must not be null");
        if (notificationService != null) {
            try {
                Map<String, Object> eventData = buildTaskEventData(task);
                eventData.put("commentId", saved.getId());
                eventData.put("commentText", saved.getCommentText());
                eventData.put("authorId", author.getId());
                eventData.put("authorName", author.getFullName());
                // task_comment_added is canonical; the legacy comment_added alias only reaches
                // recipients its own tenant-configured triggers add beyond the canonical event.
                notificationService.processEventsInNewTransaction(
                        List.of(NotificationEventCatalog.TASK_COMMENT_ADDED, NotificationEventCatalog.COMMENT_ADDED),
                        eventData);
            } catch (Exception e) {
                log.error("Failed to trigger task comment notifications for task {}", taskId, e);
            }
        }
        long totalCommentsCount = Optional.ofNullable(taskCommentRepository.countByTaskId(taskId)).orElse(0L);
        return toCommentResponse(saved, totalCommentsCount);
    }

    @Transactional
    public void deleteTaskComment(Long taskId, Long commentId, AuthPrincipal requester) {
        Objects.requireNonNull(taskId, "Task id is required");
        Objects.requireNonNull(commentId, "Comment id is required");
        Objects.requireNonNull(requester, "Requester is required");

        TaskComment comment = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!Objects.equals(comment.getTask().getId(), taskId)) {
            throw new BadRequestException("Comment does not belong to this task");
        }

        // PBAC: Only author or users with USER_MANAGE can delete
        if (!Objects.equals(comment.getCreatedByUser().getId(), currentUserService.requireCurrentUser(requester).getId()) &&
                !permissionChecker.hasPermission(requester, "USER_MANAGE")) {
            throw new ForbiddenException("You can only delete your own comments");
        }

        taskCommentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getClientTasks(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client id is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Verify client exists
        clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        Specification<Task> spec = buildTaskSpecification(
                null, null, null, clientId, null, null, null, null, null, requester
        );
        List<Task> tasks = taskRepository.findAll(spec);
        Map<Long, Long> commentCounts = getCommentCounts(tasks);

        return tasks.stream()
                .map(task -> toTaskResponse(task, commentCounts.getOrDefault(task.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getRecentTasks(int limit, AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        // PBAC: Permission-based filtering is handled in buildTaskSpecification
        // No additional filtering needed here - spec already enforces data scope

        Specification<Task> spec = buildTaskSpecification(
                null, null, null, null, null, null, null, null, null, requester
        );
        List<Task> tasks = taskRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Limit results
        if (limit > 0 && tasks.size() > limit) {
            tasks = tasks.subList(0, limit);
        }

        Map<Long, Long> commentCounts = getCommentCounts(tasks);
        return tasks.stream()
                .map(task -> toTaskResponse(task, commentCounts.getOrDefault(task.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getUpcomingTasks(int limit, AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        Instant now = Instant.now();
        Specification<Task> spec = buildTaskSpecification(
                null, null, null, null, null, null, null, null, null, requester
        );

        // Filter for upcoming tasks (due date in future, not completed)
        spec = spec.and((root, query, cb) -> {
            Predicate futureDue = cb.greaterThan(root.get("dueDate"), now);
            Predicate notCompleted = cb.notEqual(root.get("status"), "completed");
            Predicate notCancelled = cb.notEqual(root.get("status"), "cancelled");
            return cb.and(futureDue, notCompleted, notCancelled);
        });

        List<Task> tasks = taskRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "dueDate"));

        // Limit results
        if (limit > 0 && tasks.size() > limit) {
            tasks = tasks.subList(0, limit);
        }

        Map<Long, Long> commentCounts = getCommentCounts(tasks);
        return tasks.stream()
                .map(task -> toTaskResponse(task, commentCounts.getOrDefault(task.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Long getPendingTasksCount(AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        Specification<Task> spec = buildTaskSpecification(
                "pending", null, null, null, null, null, null, null, null, requester);
        return taskRepository.count(spec);
    }

    @Transactional
    public TaskCommentResponse updateTaskComment(Long taskId, Long commentId, UpdateTaskCommentRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(taskId, "Task id is required");
        Objects.requireNonNull(commentId, "Comment id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        TaskComment comment = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!Objects.equals(comment.getTask().getId(), taskId)) {
            throw new BadRequestException("Comment does not belong to this task");
        }

        // Only author can update
        if (!Objects.equals(comment.getCreatedByUser().getId(), currentUserService.requireCurrentUser(requester).getId())) {
            throw new ForbiddenException("You can only update your own comments");
        }

        if (StringUtils.hasText(request.getContent())) {
            comment.setCommentText(request.getContent().trim());
        }
        if (request.getIsInternal() != null) {
            comment.setIsInternal(request.getIsInternal());
        }

        TaskComment saved = taskCommentRepository.save(comment);
        long totalCommentsCount = Optional.ofNullable(taskCommentRepository.countByTaskId(taskId)).orElse(0L);
        return toCommentResponse(saved, totalCommentsCount);
    }

    // Private helper methods

    private Specification<Task> buildTaskSpecification(
            String status,
            String priority,
            Long assignedToId,
            Long clientId,
            String search,
            String dateFilter,
            LocalDate fromDate,
            LocalDate toDate,
            String dateField,
            AuthPrincipal requester) {
        Specification<Task> spec = (root, query, cb) -> cb.conjunction();

        if (StringUtils.hasText(status)) {
            String normalizedStatusFilter = normalizeFilterToken(status);
            if ("active".equals(normalizedStatusFilter)) {
                spec = spec.and((root1, query1, cb1) -> cb1.or(
                        optionKeyEquals("status", "pending").toPredicate(root1, query1, cb1),
                        optionKeyEquals("status", "in_progress").toPredicate(root1, query1, cb1),
                        optionKeyEquals("status", "overdue").toPredicate(root1, query1, cb1)));
            } else {
                String normalizedStatus = normalizeTaskStatus(status);
                spec = spec.and(optionKeyEquals("status", normalizedStatus));
            }
        }

        if (StringUtils.hasText(priority)) {
            String priorityKey = normalizeTaskPriority(priority);
            spec = spec.and(optionKeyEquals("priority", priorityKey));
        }

        if (assignedToId != null) {
            spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("assignedTo").get("id"), assignedToId));
        }

        if (clientId != null) {
            spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("client").get("id"), clientId));
        }

        if (StringUtils.hasText(search)) {
            String term = search.trim();
            String searchTerm = "%" + term.toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root1, query1, cb1) -> {
                Join<Task, Client> clientJoin = root1.join("client", JoinType.LEFT);
                Join<Task, User> assigneeJoin = root1.join("assignedTo", JoinType.LEFT);
                Predicate titleDescAssignee = cb1.or(
                        cb1.like(cb1.lower(root1.get("title")), searchTerm),
                        cb1.like(cb1.lower(root1.get("description")), searchTerm),
                        cb1.like(cb1.lower(assigneeJoin.get("fullName")), searchTerm));
                // Client PHI: exact match only (no LIKE on encrypted fullName)
                Predicate clientMatch = cb1.and(
                        cb1.isNotNull(clientJoin.get("id")),
                        clientSearchHelper.predicateForClientPath(clientJoin, query1, cb1, term));
                return cb1.or(titleDescAssignee, clientMatch);
            });
        }

        String resolvedDateField = resolveDateField(dateField);
        Instant fromInstant = toStartOfDayUtc(fromDate);
        Instant toInstant = toEndOfDayUtc(toDate);
        if (fromInstant != null) {
            spec = spec.and((root1, query1, cb1) ->
                    cb1.greaterThanOrEqualTo(root1.get(resolvedDateField), fromInstant));
        }
        if (toInstant != null) {
            spec = spec.and((root1, query1, cb1) ->
                    cb1.lessThanOrEqualTo(root1.get(resolvedDateField), toInstant));
        }
        if (StringUtils.hasText(dateFilter)) {
            spec = spec.and(buildDateFilterSpecification(dateFilter));
        }

        // PBAC: Permission-based filtering with data scope
        boolean canViewAll = permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL");
        boolean canViewTeam = permissionChecker.hasPermission(requester,"CLIENT_VIEW_TEAM");
        boolean canViewOwn = permissionChecker.hasPermission(requester, "CLIENT_VIEW_OWN");
        
        if (canViewAll) {
            // Can see all tasks - no filtering
        } else if (canViewOwn) {
            // Filter to own tasks or tasks for own clients
            spec = spec.and((root1, query1, cb1) -> {
                Predicate assignedToMe = cb1.equal(root1.get("assignedTo").get("id"), currentUserService.requireCurrentUser(requester).getId());
                Predicate myClients = cb1.equal(root1.get("client").get("assignedTherapist").get("id"),
                        currentUserService.requireCurrentUser(requester).getId());
                return cb1.or(assignedToMe, myClients);
            });
        } else if (canViewTeam) {
            // Filter to team's tasks
            List<Long> supervisedTherapistIds = getSupervisedTherapistIds(currentUserService.requireCurrentUser(requester).getId());
            if (!supervisedTherapistIds.isEmpty()) {
                spec = spec.and((root1, query1, cb1) -> root1.get("assignedTo").get("id").in(supervisedTherapistIds));
            } else {
                spec = spec.and((root1, query1, cb1) -> cb1.disjunction());
            }
        } else {
            // No permission - return empty result
            spec = spec.and((root1, query1, cb1) -> cb1.disjunction());
        }

        return spec;
    }

    private Specification<Task> buildDateFilterSpecification(String dateFilter) {
        String normalized = dateFilter.trim().toLowerCase(Locale.ROOT).replace(" ", "_");
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return switch (normalized) {
            case "due_date" -> (root, query, cb) -> cb.isNotNull(root.get("dueDate"));
            case "overdue" -> (root, query, cb) -> cb.and(
                    cb.isNotNull(root.get("dueDate")),
                    cb.lessThan(root.get("dueDate"), now),
                    cb.notEqual(root.get("status"), "completed")
            );
            case "today" -> {
                Instant dayStart = toStartOfDayUtc(today);
                Instant dayEnd = toEndOfDayUtc(today);
                yield (root, query, cb) -> cb.and(
                        cb.isNotNull(root.get("dueDate")),
                        cb.greaterThanOrEqualTo(root.get("dueDate"), dayStart),
                        cb.lessThanOrEqualTo(root.get("dueDate"), dayEnd)
                );
            }
            case "week", "this_week" -> {
                LocalDate start = today.minusDays(today.getDayOfWeek().getValue() - 1L);
                LocalDate end = start.plusDays(6);
                Instant weekStart = toStartOfDayUtc(start);
                Instant weekEnd = toEndOfDayUtc(end);
                yield (root, query, cb) -> cb.and(
                        cb.isNotNull(root.get("dueDate")),
                        cb.greaterThanOrEqualTo(root.get("dueDate"), weekStart),
                        cb.lessThanOrEqualTo(root.get("dueDate"), weekEnd)
                );
            }
            case "month", "this_month" -> {
                LocalDate start = today.withDayOfMonth(1);
                LocalDate end = today.withDayOfMonth(today.lengthOfMonth());
                Instant monthStart = toStartOfDayUtc(start);
                Instant monthEnd = toEndOfDayUtc(end);
                yield (root, query, cb) -> cb.and(
                        cb.isNotNull(root.get("dueDate")),
                        cb.greaterThanOrEqualTo(root.get("dueDate"), monthStart),
                        cb.lessThanOrEqualTo(root.get("dueDate"), monthEnd)
                );
            }
            default -> (root, query, cb) -> cb.conjunction();
        };
    }

    private String resolveDateField(String dateField) {
        if (!StringUtils.hasText(dateField)) {
            return "dueDate";
        }
        // Frontend sends camelCase (createdAt); also accept snake_case (created_at).
        // normalizeFilterToken lowercases only, so createdAt → createdat.
        String normalized = normalizeFilterToken(dateField);
        return switch (normalized) {
            case "created_at", "createdat", "created" -> "createdAt";
            case "updated_at", "updatedat", "updated" -> "updatedAt";
            case "due_date", "duedate", "due" -> "dueDate";
            default -> throw new BadRequestException(
                    "Invalid dateField: " + dateField + ". Allowed values: dueDate, createdAt, updatedAt.");
        };
    }

    private String normalizeTaskStatus(String status) {
        try {
            return systemOptionResolverService.requireOptionKey(SystemOptionCategories.TASK_STATUS, status);
        } catch (BadRequestException ex) {
            throw new BadRequestException(
                    "Invalid task status: " + status
                            + ". Allowed values: pending, in_progress, completed, cancelled, overdue, active.",
                    ex);
        }
    }

    private String normalizeFilterToken(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace(" ", "_");
    }

    private String normalizeTaskPriority(String priority) {
        try {
            return systemOptionResolverService.requireOptionKey(
                    SystemOptionCategories.TASK_PRIORITY, priority);
        } catch (BadRequestException ex) {
            throw new BadRequestException(
                    "Invalid task priority: " + priority + ". Use a value from the task_priority system options.",
                    ex);
        }
    }

    /**
     * Case/hyphen-insensitive equality against stored option keys.
     */
    private Specification<Task> optionKeyEquals(String field, String expectedKey) {
        String normalizedExpected = SystemOptionKeyMatcher.normalize(expectedKey);
        return (root, query, cb) -> cb.equal(cb.lower(root.get(field)), normalizedExpected);
    }

    private Instant toStartOfDayUtc(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant toEndOfDayUtc(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.plusDays(1).atStartOfDay().minusNanos(1).toInstant(ZoneOffset.UTC);
    }

    /**
     * PBAC: Validate task access based on permissions + data scope.
     */
    private void validateTaskAccess(Task task, AuthPrincipal requester) {
        // Check permissions
        boolean canViewAll = permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL");
        boolean canViewTeam = permissionChecker.hasPermission(requester,"CLIENT_VIEW_TEAM");
        boolean canViewOwn = permissionChecker.hasPermission(requester, "CLIENT_VIEW_OWN");
        
        if (canViewAll) {
            return; // Can access all tasks
        }
        
        if (canViewOwn) {
            boolean isAssigned = task.getAssignedTo() != null
                    && Objects.equals(task.getAssignedTo().getId(), currentUserService.requireCurrentUser(requester).getId());
            boolean isClientTherapist = task.getClient().getAssignedTherapist() != null &&
                    Objects.equals(task.getClient().getAssignedTherapist().getId(), currentUserService.requireCurrentUser(requester).getId());
            if (!isAssigned && !isClientTherapist) {
                throw new ForbiddenException("You can only access tasks assigned to you or for your clients");
            }
        } else if (canViewTeam) {
            // Can access team's tasks
            if (task.getAssignedTo() == null
                    || !getSupervisedTherapistIds(currentUserService.requireCurrentUser(requester).getId()).contains(task.getAssignedTo().getId())) {
                throw new ForbiddenException("You can only access tasks of therapists you supervise");
            }
        } else {
            throw new ForbiddenException("You do not have permission to access this task");
        }
    }

    private List<Long> getSupervisedTherapistIds(Long supervisorId) {
        return supervisorAssignmentRepository.findBySupervisorId(supervisorId).stream()
                .map(SupervisorAssignment::getTherapist)
                .filter(Objects::nonNull)
                .map(User::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildTaskEventData(Task task) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", task.getId());
        data.put("title", task.getTitle());
        data.put("status", task.getStatus());
        data.put("priority", task.getPriority());
        data.put("dueDate", task.getDueDate());
        data.put("clientId", task.getClient().getId());
        NotificationPayloadFactory.putClientIdentity(data, task.getClient());
        data.put("assignedToId", task.getAssignedTo() != null ? task.getAssignedTo().getId() : null);
        data.put("assignedToName", task.getAssignedTo() != null ? task.getAssignedTo().getFullName() : "Unassigned");
        if (task.getClient() != null && task.getClient().getAssignedTherapist() != null) {
            data.put("therapistId", task.getClient().getAssignedTherapist().getId());
            data.put("therapistName", task.getClient().getAssignedTherapist().getFullName());
        } else {
            data.put("therapistName", "Unassigned");
        }
        return data;
    }

    private void recordAuditEvent(Long actorId, String action, Long resourceId, String ipAddress,
                                    boolean hipaaRelevant) {
        try {
            auditLogService.recordStaffEvent(actorId, action, RESOURCE_TYPE_TASK, resourceId, null, ipAddress,
                    hipaaRelevant);
        } catch (Exception e) {
            log.error("Failed to record audit event for task: {}", resourceId, e);
        }
    }

    private Map<Long, Long> getCommentCounts(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> taskIds = tasks.stream()
                .map(Task::getId)
                .filter(Objects::nonNull)
                .toList();

        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return taskCommentRepository.countByTaskIds(taskIds).stream()
                .collect(Collectors.toMap(
                        TaskCommentRepository.TaskCommentCountView::getTaskId,
                        TaskCommentRepository.TaskCommentCountView::getCommentCount
                ));
    }

    private TaskResponse toTaskResponse(Task task, Long commentCount) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .titleKey(task.getTitleKey())
                .taskType(task.getTaskType())
                .description(task.getDescription())
                .commentCount(commentCount)
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .clientId(task.getClient().getId())
                .clientName(task.getClient().getFullName())
                .assignedToId(task.getAssignedTo() != null ? task.getAssignedTo().getId() : null)
                .assignedToName(task.getAssignedTo() != null ? task.getAssignedTo().getFullName() : null)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private TaskCommentResponse toCommentResponse(TaskComment comment, long totalCommentsCount) {
        return TaskCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getCommentText())
                .isInternal(comment.getIsInternal())
                .authorId(comment.getCreatedByUser().getId())
                .authorName(comment.getCreatedByUser().getFullName())
                .createdAt(comment.getCreatedAt())
                .totalCommentsCount(totalCommentsCount)
                .build();
    }

    private Long requireTaskId(Task task) {
        Objects.requireNonNull(task, "Task is required");
        return Objects.requireNonNull(task.getId(), "Task id must not be null");
    }

    private TaskTitleResolution resolveTaskTitle(String title, String titleKey) {
        if (StringUtils.hasText(titleKey)) {
            String key = systemOptionResolverService.requireOptionKey(
                    SystemOptionCategories.TASK_TITLES, titleKey);
            String label = systemOptionResolverService.resolveOptionLabel(
                    SystemOptionCategories.TASK_TITLES, key);
            String resolvedTitle = StringUtils.hasText(label)
                    ? label
                    : (StringUtils.hasText(title) ? title.trim() : key);
            return new TaskTitleResolution(key, resolvedTitle);
        }
        if (!StringUtils.hasText(title)) {
            throw new BadRequestException("Task title is required");
        }
        String trimmedTitle = title.trim();
        String key = systemOptionResolverService.resolveOptionKey(
                SystemOptionCategories.TASK_TITLES, trimmedTitle);
        if (key != null) {
            String label = systemOptionResolverService.resolveOptionLabel(
                    SystemOptionCategories.TASK_TITLES, key);
            return new TaskTitleResolution(key, StringUtils.hasText(label) ? label : trimmedTitle);
        }
        return new TaskTitleResolution(null, trimmedTitle);
    }

    private String parseTaskType(String taskType) {
        if (!StringUtils.hasText(taskType)) {
            return null;
        }
        return systemOptionResolverService.requireOptionKey(
                SystemOptionCategories.TASK_TYPES, taskType);
    }

    private record TaskTitleResolution(String titleKey, String title) {}

    // DEPRECATED: Use permission checks instead
    // This method is kept for backward compatibility in business logic validation
    // For access control, always use principal.hasPermission() instead
    @Deprecated
    private boolean hasRole(AuthPrincipal principal, String roleName) {
        return permissionChecker.hasRole(principal, roleName);
    }
}


