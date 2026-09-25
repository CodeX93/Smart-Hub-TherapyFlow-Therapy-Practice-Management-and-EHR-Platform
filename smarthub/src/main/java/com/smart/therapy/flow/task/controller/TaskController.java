package com.smart.therapy.flow.task.controller;

import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.task.dto.*;
import com.smart.therapy.flow.task.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;
    
    @org.springframework.beans.factory.annotation.Autowired
    private com.smart.therapy.flow.common.config.AppProperties appProperties;

    @GetMapping
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<PaginatedResponse<TaskResponse>> getTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String dateFilter,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String dateField,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        int defaultPage = appProperties.getPagination().getDefaultPage();
        int defaultPageSize = appProperties.getPagination().getDefaultPageSize();
        int maxPageSize = appProperties.getPagination().getMaxPageSize();
        int minPageSize = appProperties.getPagination().getMinPageSize();
        
        int safePage = Math.max(defaultPage, page);
        if (pageSize == null) {
            pageSize = defaultPageSize;
        }
        int safePageSize = Math.min(Math.max(pageSize, minPageSize), maxPageSize);
        PaginatedResponse<TaskResponse> response = taskService.getTasks(
                safePage, safePageSize, status, priority, assignedToId, clientId, search,
                dateFilter, fromDate, toDate, dateField, sortBy, sortOrder, principal
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<PaginatedResponse<TaskResponse>> getTaskHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String dateFilter,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String dateField,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        int defaultPage = appProperties.getPagination().getDefaultPage();
        int defaultPageSize = appProperties.getPagination().getDefaultPageSize();
        int maxPageSize = appProperties.getPagination().getMaxPageSize();
        int minPageSize = appProperties.getPagination().getMinPageSize();

        int safePage = Math.max(defaultPage, page);
        if (pageSize == null) {
            pageSize = defaultPageSize;
        }
        int safePageSize = Math.min(Math.max(pageSize, minPageSize), maxPageSize);

        PaginatedResponse<TaskResponse> response = taskService.getTasks(
                safePage, safePageSize, status, priority, assignedToId, clientId, search,
                dateFilter, fromDate, toDate, dateField, sortBy, sortOrder, principal
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<TaskStatsResponse> getTaskStats(
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String dateField,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(taskService.getTaskStats(principal, assignedToId, fromDate, toDate, dateField));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<TaskResponse> getTask(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        TaskResponse task = taskService.getTask(id, principal);
        return ResponseEntity.ok(task);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create new task",
            description = """
                    Create a new task.
                    
                    **Required Fields:**
                    - `title` (REQUIRED): Task title
                    - `priority` (REQUIRED): Task priority (low, medium, high, urgent)
                    - `status` (REQUIRED): Task status (pending, in_progress, completed, overdue)
                    - `clientId` (REQUIRED): ID of the client this task is for
                    
                    **Optional Fields:**
                    - `description` (optional): Task description
                    - `assignedToId` (optional): ID of the user assigned to this task
                    - `dueDate` (optional): Due date for the task (ISO 8601 format)
                    
                    **Requires:** THERAPIST, ADMIN, or SUPERVISOR role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Task information to create",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CreateTaskRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Create Task",
                                    value = """
                                            {
                                              "title": "Follow up with client",
                                              "description": "Check in on progress and schedule next session",
                                              "priority": "medium",
                                              "status": "pending",
                                              "clientId": 123,
                                              "assignedToId": 1,
                                              "dueDate": "2025-12-31T23:59:59Z"
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Task created successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = TaskResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        TaskResponse task = taskService.createTask(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(task);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        TaskResponse task = taskService.updateTask(id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(task);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<Void> deleteTask(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        taskService.deleteTask(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/check-overdue")
    @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE')")
    public ResponseEntity<Void> checkOverdueTasks(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        taskService.checkOverdueTasks(principal);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{taskId}/comments")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<List<TaskCommentResponse>> getTaskComments(
            @PathVariable("taskId") Long taskId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<TaskCommentResponse> comments = taskService.getTaskComments(taskId, principal);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{taskId}/comments")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<TaskCommentResponse> createTaskComment(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody CreateTaskCommentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        TaskCommentResponse comment = taskService.createTaskComment(taskId, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(comment);
    }

    @PutMapping("/{taskId}/comments/{commentId}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<TaskCommentResponse> updateTaskComment(
            @PathVariable("taskId") Long taskId,
            @PathVariable("commentId") Long commentId,
            @Valid @RequestBody UpdateTaskCommentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        TaskCommentResponse comment = taskService.updateTaskComment(taskId, commentId, request, principal);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/{taskId}/comments/{commentId}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<Void> deleteTaskComment(
            @PathVariable("taskId") Long taskId,
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        taskService.deleteTaskComment(taskId, commentId, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<List<TaskResponse>> getRecentTasks(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<TaskResponse> tasks = taskService.getRecentTasks(limit, principal);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<List<TaskResponse>> getUpcomingTasks(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<TaskResponse> tasks = taskService.getUpcomingTasks(limit, principal);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/pending/count")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<Object> getPendingTasksCount(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long taskCount = taskService.getPendingTasksCount(principal);
        return ResponseEntity.ok(new Object() {
            public final Long count = taskCount;
        });
    }

    @GetMapping("/clients/{clientId}/tasks")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<List<TaskResponse>> getClientTasks(
            @PathVariable("clientId") Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<TaskResponse> tasks = taskService.getClientTasks(clientId, principal);
        return ResponseEntity.ok(tasks);
    }

}


