package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import org.mockito.Spy;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.notification.service.NotificationEventCatalog;
import com.smart.therapy.flow.notification.service.NotificationService;
import com.smart.therapy.flow.task.dto.*;
import com.smart.therapy.flow.task.entity.Task;
import com.smart.therapy.flow.task.entity.TaskComment;
import com.smart.therapy.flow.task.repository.TaskCommentRepository;
import com.smart.therapy.flow.task.repository.TaskRepository;
import com.smart.therapy.flow.task.service.TaskService;
import com.smart.therapy.flow.user.repository.SupervisorAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Unit Tests")
@SuppressWarnings({ "null", "unchecked" }) // Suppress null warnings from Mockito and unchecked specification
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskCommentRepository taskCommentRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SupervisorAssignmentRepository supervisorAssignmentRepository;

    @Mock
    private AuditLogService auditLogService;

    @Spy
    private PermissionChecker permissionChecker = new PermissionChecker();

    @Mock
    private SystemOptionResolverService systemOptionResolverService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private TaskService taskService;

    private AuthPrincipal therapistPrincipal;
    private User therapist;
    private Client client;
    private Task task;

    @BeforeEach
    void setUp() {
        // @InjectMocks fills the constructor only; the optional notification
        // service is an @Autowired field the mock has to reach by hand.
        ReflectionTestUtils.setField(taskService, "notificationService", notificationService);

        therapist = TestDataFactory.createTestTherapist();
        therapist.setId(1L);
        therapistPrincipal = TestDataFactory.createAuthPrincipal(therapist, "ROLE_THERAPIST", "CLIENT_VIEW_OWN");

        client = TestDataFactory.createTestClient();
        client.setId(1L);
        client.setAssignedTherapist(therapist);

        task = Task.builder()
                .title("Follow up with client")
                .description("Check on client progress")
                .status("pending")
                .priority("high")
                .client(client)
                .assignedTo(therapist)
                .build();
        task.setId(1L);
    }

    @Test
    @DisplayName("Should create task successfully")
    void shouldCreateTaskSuccessfully() {
        when(currentUserService.requireCurrentUser(therapistPrincipal)).thenReturn(therapist);
        // Arrange
        CreateTaskRequest request = new CreateTaskRequest();
        request.setClientId(1L);
        request.setTitle("Follow up with client");
        request.setDescription("Check on client progress");
        request.setPriority("high");
        request.setStatus("pending");
        when(systemOptionResolverService.parseOptionKey("task_status", "pending", "pending")).thenReturn("pending");
        when(systemOptionResolverService.parseOptionKey("task_priority", "high", "medium")).thenReturn("high");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Act
        TaskResponse response = taskService.createTask(request, therapistPrincipal, "127.0.0.1");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Follow up with client");
        verify(clientRepository).findById(1L);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should trigger task_assigned notification when created task has an assignee")
    void shouldTriggerTaskAssignedNotificationForAssignee() {
        when(currentUserService.requireCurrentUser(therapistPrincipal)).thenReturn(therapist);
        CreateTaskRequest request = new CreateTaskRequest();
        request.setClientId(1L);
        request.setTitle("Follow up with client");
        when(systemOptionResolverService.parseOptionKey(eq("task_status"), any(), eq("pending"))).thenReturn("pending");
        when(systemOptionResolverService.parseOptionKey(eq("task_priority"), any(), eq("medium"))).thenReturn("medium");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        taskService.createTask(request, therapistPrincipal, "127.0.0.1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationService).processEventInNewTransaction(
                eq(NotificationEventCatalog.TASK_ASSIGNED), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue())
                .containsEntry("title", "Follow up with client")
                .containsEntry("assignedToId", therapist.getId());
    }

    @Test
    @DisplayName("Should not trigger task_assigned notification when created task has no assignee")
    void shouldNotTriggerTaskAssignedNotificationWithoutAssignee() {
        when(currentUserService.requireCurrentUser(therapistPrincipal)).thenReturn(therapist);
        CreateTaskRequest request = new CreateTaskRequest();
        request.setClientId(1L);
        request.setTitle("Follow up with client");
        when(systemOptionResolverService.parseOptionKey(eq("task_status"), any(), eq("pending"))).thenReturn("pending");
        when(systemOptionResolverService.parseOptionKey(eq("task_priority"), any(), eq("medium"))).thenReturn("medium");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        task.setAssignedTo(null);
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        taskService.createTask(request, therapistPrincipal, "127.0.0.1");

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should not fail task creation when the notification service throws")
    void shouldNotFailTaskCreationWhenNotificationFails() {
        when(currentUserService.requireCurrentUser(therapistPrincipal)).thenReturn(therapist);
        CreateTaskRequest request = new CreateTaskRequest();
        request.setClientId(1L);
        request.setTitle("Follow up with client");
        when(systemOptionResolverService.parseOptionKey(eq("task_status"), any(), eq("pending"))).thenReturn("pending");
        when(systemOptionResolverService.parseOptionKey(eq("task_priority"), any(), eq("medium"))).thenReturn("medium");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        doThrow(new RuntimeException("notification down"))
                .when(notificationService).processEventInNewTransaction(eq(NotificationEventCatalog.TASK_ASSIGNED), any());

        TaskResponse response = taskService.createTask(request, therapistPrincipal, "127.0.0.1");

        assertThat(response).isNotNull();
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when client not found")
    void shouldThrowExceptionWhenClientNotFound() {
        // Arrange
        CreateTaskRequest request = new CreateTaskRequest();
        request.setClientId(999L);
        request.setTitle("Test task");

        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> taskService.createTask(request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Client not found");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Should get task by ID successfully")
    void shouldGetTaskByIdSuccessfully() {
        when(currentUserService.requireCurrentUser(therapistPrincipal)).thenReturn(therapist);
        // Arrange
        Long taskId = 1L;
        task.setId(taskId);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskCommentRepository.countByTaskId(taskId)).thenReturn(2L);

        // Act
        TaskResponse response = taskService.getTask(taskId, therapistPrincipal);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(taskId);
        assertThat(response.getTitle()).isEqualTo("Follow up with client");
        assertThat(response.getCommentCount()).isEqualTo(2L);
        verify(taskRepository).findById(taskId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when task not found")
    void shouldThrowExceptionWhenTaskNotFound() {
        // Arrange
        Long taskId = 999L;
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> taskService.getTask(taskId, therapistPrincipal))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found");

        verify(taskRepository).findById(taskId);
    }

    @Test
    @DisplayName("Should update task successfully")
    void shouldUpdateTaskSuccessfully() {
        when(currentUserService.requireCurrentUser(therapistPrincipal)).thenReturn(therapist);
        // Arrange
        Long taskId = 1L;
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Updated task title");
        request.markFieldPresent("title");
        request.markFieldPresent("status");
        request.setStatus("in_progress");
        when(systemOptionResolverService.requireOptionKey("task_status", "in_progress")).thenReturn("in_progress");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Act
        TaskResponse response = taskService.updateTask(taskId, request, therapistPrincipal, "127.0.0.1");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Updated task title");
        assertThat(response.getStatus()).isEqualTo("in_progress");
        assertThat(task.getTitle()).isEqualTo("Updated task title");
        assertThat(task.getStatus()).isEqualTo("in_progress");
        verify(taskRepository).findById(taskId);
        verify(taskRepository).save(task);
    }

    @Test
    @DisplayName("Should trigger task_assigned notification when task is reassigned to a different user")
    void shouldTriggerTaskAssignedNotificationOnReassignment() {
        when(currentUserService.requireCurrentUser(therapistPrincipal)).thenReturn(therapist);
        User newAssignee = TestDataFactory.createTestTherapist();
        newAssignee.setId(2L);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setAssignedToId(2L);
        request.markFieldPresent("assignedToId");

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newAssignee));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        taskService.updateTask(1L, request, therapistPrincipal, "127.0.0.1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationService).processEventInNewTransaction(
                eq(NotificationEventCatalog.TASK_ASSIGNED), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).containsEntry("assignedToId", newAssignee.getId());
    }

    @Test
    @DisplayName("Should not trigger task_assigned notification when assignee is unchanged")
    void shouldNotTriggerTaskAssignedNotificationWhenAssigneeUnchanged() {
        when(currentUserService.requireCurrentUser(therapistPrincipal)).thenReturn(therapist);
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setAssignedToId(therapist.getId());
        request.markFieldPresent("assignedToId");

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(therapist.getId())).thenReturn(Optional.of(therapist));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        taskService.updateTask(1L, request, therapistPrincipal, "127.0.0.1");

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should not trigger task_assigned notification when task is unassigned")
    void shouldNotTriggerTaskAssignedNotificationWhenUnassigned() {
        when(currentUserService.requireCurrentUser(therapistPrincipal)).thenReturn(therapist);
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setAssignedToId(null);
        request.markFieldPresent("assignedToId");

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        taskService.updateTask(1L, request, therapistPrincipal, "127.0.0.1");

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should not fail task update when the reassignment notification throws")
    void shouldNotFailTaskUpdateWhenNotificationFails() {
        when(currentUserService.requireCurrentUser(therapistPrincipal)).thenReturn(therapist);
        User newAssignee = TestDataFactory.createTestTherapist();
        newAssignee.setId(2L);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setAssignedToId(2L);
        request.markFieldPresent("assignedToId");

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newAssignee));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        doThrow(new RuntimeException("notification down"))
                .when(notificationService).processEventInNewTransaction(eq(NotificationEventCatalog.TASK_ASSIGNED), any());

        TaskResponse response = taskService.updateTask(1L, request, therapistPrincipal, "127.0.0.1");

        assertThat(response).isNotNull();
        verify(taskRepository).save(task);
    }

    @Test
    @DisplayName("Should add comment to task successfully")
    void shouldAddCommentToTaskSuccessfully() {
        when(currentUserService.requireCurrentUser(therapistPrincipal)).thenReturn(therapist);
        // Arrange
        Long taskId = 1L;
        CreateTaskCommentRequest request = new CreateTaskCommentRequest();
        request.setContent("This is a comment");

        TaskComment comment = TaskComment.builder()
                .createdByUser(therapist)
                .task(task)
                .commentText("This is a comment")
                .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findById(1L)).thenReturn(Optional.of(therapist));
        when(taskCommentRepository.save(any(TaskComment.class))).thenReturn(comment);

        // Act
        TaskCommentResponse response = taskService.createTaskComment(taskId, request, therapistPrincipal, "127.0.0.1");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("This is a comment");
        verify(taskRepository).findById(taskId);
        verify(taskCommentRepository).save(any(TaskComment.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when task not found for comment")
    void shouldThrowExceptionWhenTaskNotFoundForComment() {
        // Arrange
        Long taskId = 999L;
        CreateTaskCommentRequest request = new CreateTaskCommentRequest();
        request.setContent("Test comment");

        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> taskService.createTaskComment(taskId, request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found");

        verify(taskCommentRepository, never()).save(any(TaskComment.class));
    }

    @Test
    @DisplayName("Should return paginated tasks list")
    void shouldReturnPaginatedTasksList() {
        // Arrange
        int page = 1;
        int pageSize = 10;
        task.setId(1L);
        Page<Task> taskPage = new PageImpl<>(List.of(task), PageRequest.of(0, 10), 1);

        when(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(taskPage);
        TaskCommentRepository.TaskCommentCountView countView = mock(TaskCommentRepository.TaskCommentCountView.class);
        when(countView.getTaskId()).thenReturn(1L);
        when(countView.getCommentCount()).thenReturn(3L);
        when(taskCommentRepository.countByTaskIds(List.of(1L))).thenReturn(List.of(countView));

        // Act
        PaginatedResponse<TaskResponse> response = taskService.getTasks(
                page, pageSize, null, null, null, null, null, null, null, null, null, null, null, therapistPrincipal);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getCommentCount()).isEqualTo(3L);
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getPage()).isEqualTo(page);
        assertThat(response.getPageSize()).isEqualTo(pageSize);
    }

    @Test
    @DisplayName("Should delete task successfully")
    void shouldDeleteTaskSuccessfully() {
        when(currentUserService.requireCurrentUser(therapistPrincipal)).thenReturn(therapist);
        // Arrange
        Long taskId = 1L;
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        // Act
        taskService.deleteTask(taskId, therapistPrincipal, "127.0.0.1");

        // Assert
        verify(taskRepository).findById(taskId);
        verify(taskRepository).delete(task);
    }

    @Test
    void deniesTaskAccessWithoutClientPermission() {
        AuthPrincipal restricted = TestDataFactory.createAuthPrincipal(therapist, "ROLE_THERAPIST");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        assertThatThrownBy(() -> taskService.getTask(1L, restricted))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(taskCommentRepository);
    }

    @Test
    void deniesCommentOnAnotherTherapistsTask() {
        when(currentUserService.requireCurrentUser(therapistPrincipal)).thenReturn(therapist);
        User other = TestDataFactory.createTestTherapist();
        other.setId(2L);
        task.setAssignedTo(other);
        client.setAssignedTherapist(other);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        CreateTaskCommentRequest request = new CreateTaskCommentRequest();
        request.setContent("Unauthorized comment");
        assertThatThrownBy(() -> taskService.createTaskComment(1L, request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(taskCommentRepository, notificationService, auditLogService);
    }

    @Test
    @DisplayName("Should throw exception when request is null")
    void shouldThrowExceptionWhenRequestIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> taskService.createTask(null, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Request is required");
    }

    @Test
    @DisplayName("Should throw exception when requester is null")
    void shouldThrowExceptionWhenRequesterIsNull() {
        // Arrange
        CreateTaskRequest request = new CreateTaskRequest();
        request.setClientId(1L);
        request.setTitle("Test task");

        // Act & Assert
        assertThatThrownBy(() -> taskService.createTask(request, null, "127.0.0.1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Requester is required");
    }
}
