package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.assessment.dto.*;
import com.smart.therapy.flow.assessment.entity.*;
import com.smart.therapy.flow.assessment.repository.*;
import com.smart.therapy.flow.assessment.service.AssessmentService;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.ConflictException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.notification.service.NotificationService;
import com.smart.therapy.flow.report.service.ClientReportAccessService;
import com.smart.therapy.flow.system.repository.OptionCategoryRepository;
import com.smart.therapy.flow.system.repository.SystemOptionRepository;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssessmentService Unit Tests")
class AssessmentServiceTest {

    @Mock
    private AssessmentTemplateRepository templateRepository;

    @Mock
    private AssessmentAssignmentRepository assignmentRepository;

    @Mock
    private AssessmentResponseRepository responseRepository;

    @Mock
    private AssessmentQuestionRepository questionRepository;

    @Mock
    private AssessmentSectionRepository sectionRepository;

    @Mock
    private AssessmentQuestionOptionRepository optionRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AssessmentReportRepository reportRepository;

    @Mock
    private OptionCategoryRepository optionCategoryRepository;

    @Mock
    private SystemOptionRepository systemOptionRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditLogService auditLogService;

    @Spy
    private PermissionChecker permissionChecker = new PermissionChecker();

    @Mock
    private ClientReportAccessService clientReportAccessService;

    @InjectMocks
    private AssessmentService assessmentService;

    private AuthPrincipal adminPrincipal;
    private AuthPrincipal therapistPrincipal;
    private User admin;
    private User therapist;
    private Client client;
    private AssessmentTemplate template;

    @BeforeEach
    void setUp() {
        admin = TestDataFactory.createTestAdmin();
        admin.setId(1L);
        adminPrincipal = TestDataFactory.createAuthPrincipal(admin, "ROLE_ADMIN", "CONSENT_ADMIN_VIEW");

        therapist = TestDataFactory.createTestTherapist();
        therapist.setId(2L);
        therapistPrincipal = TestDataFactory.createAuthPrincipal(therapist);

        client = TestDataFactory.createTestClient();
        client.setId(1L);

        template = AssessmentTemplate.builder()
                .name("PHQ-9")
                .description("Patient Health Questionnaire")
                .category("Depression")
                .isActive(true)
                .build();
        template.setId(1L);
    }

    @Test
    @DisplayName("Should get all active templates")
    void shouldGetAllActiveTemplates() {
        // Arrange
        when(templateRepository.findByIsActive(true)).thenReturn(List.of(template));

        // Act
        List<AssessmentTemplateResponse> templates = assessmentService.getTemplates();

        // Assert
        assertThat(templates).isNotNull();
        assertThat(templates).hasSize(1);
        assertThat(templates.get(0).getName()).isEqualTo("PHQ-9");
        verify(templateRepository).findByIsActive(true);
    }

    @Test
    @DisplayName("Should get template by ID successfully")
    void shouldGetTemplateByIdSuccessfully() {
        // Arrange
        Long templateId = 1L;
        when(templateRepository.findByIdWithSections(templateId)).thenReturn(Optional.of(template));

        // Act
        AssessmentTemplateResponse response = assessmentService.getTemplate(templateId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(templateId);
        assertThat(response.getName()).isEqualTo("PHQ-9");
        verify(templateRepository).findByIdWithSections(templateId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when template not found")
    void shouldThrowExceptionWhenTemplateNotFound() {
        // Arrange
        Long templateId = 999L;
        when(templateRepository.findByIdWithSections(templateId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> assessmentService.getTemplate(templateId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Assessment template not found");

        verify(templateRepository).findByIdWithSections(templateId);
    }

    @Test
    @DisplayName("Should create template successfully when admin")
    void shouldCreateTemplateSuccessfullyWhenAdmin() {
        when(currentUserService.requireCurrentUser(adminPrincipal)).thenReturn(admin);
        // Arrange
        CreateAssessmentTemplateRequest request = new CreateAssessmentTemplateRequest();
        request.setName("GAD-7");
        request.setDescription("Generalized Anxiety Disorder");
        request.setCategory("Anxiety");

        AssessmentTemplate savedTemplate = AssessmentTemplate.builder()
                .name("GAD-7")
                .description("Generalized Anxiety Disorder")
                .category("Anxiety")
                .isActive(true)
                .build();

        when(templateRepository.save(any(AssessmentTemplate.class))).thenReturn(savedTemplate);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        // Act
        AssessmentTemplateResponse response = assessmentService.createTemplate(request, adminPrincipal);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("GAD-7");
        verify(templateRepository).save(any(AssessmentTemplate.class));
    }

    @Test
    @DisplayName("Should throw ForbiddenException when non-admin creates template")
    void shouldThrowExceptionWhenNonAdminCreatesTemplate() {
        // Arrange
        CreateAssessmentTemplateRequest request = new CreateAssessmentTemplateRequest();
        request.setName("GAD-7");

        // Act & Assert
        assertThatThrownBy(() -> assessmentService.createTemplate(request, therapistPrincipal))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only administrators can create assessment templates");

        verify(templateRepository, never()).save(any(AssessmentTemplate.class));
    }

    @Test
    @DisplayName("Should assign assessment to client successfully")
    void shouldAssignAssessmentToClientSuccessfully() {
        when(currentUserService.requireCurrentUser(therapistPrincipal)).thenReturn(therapist);
        // Arrange
        CreateAssessmentAssignmentRequest request = new CreateAssessmentAssignmentRequest();
        request.setTemplateId(1L);
        request.setClientId(1L);
        request.setDueDate(java.time.Instant.now().plusSeconds(86400));

        AssessmentAssignment assignment = AssessmentAssignment.builder()
                .template(template)
                .client(client)
                .status("assigned")
                .assignedDate(java.time.Instant.now())
                .build();

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(clientReportAccessService.requireClientAccess(eq(1L), any())).thenReturn(client);
        assignment.setId(1L);
        assignment.setDueDate(request.getDueDate().atZone(java.time.ZoneOffset.UTC).toLocalDate());
        when(assignmentRepository.save(any(AssessmentAssignment.class))).thenReturn(assignment);

        when(userRepository.findById(therapist.getId())).thenReturn(Optional.of(therapist));
        // Act
        AssessmentAssignmentResponse response = assessmentService.assignAssessment(request, therapistPrincipal, "127.0.0.1");

        // Assert
        assertThat(response).isNotNull();
        verify(templateRepository).findById(1L);
        verify(clientReportAccessService).requireClientAccess(eq(1L), any());
        verify(assignmentRepository).save(any(AssessmentAssignment.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when template not found for assignment")
    void shouldThrowExceptionWhenTemplateNotFoundForAssignment() {
        // Arrange
        CreateAssessmentAssignmentRequest request = new CreateAssessmentAssignmentRequest();
        request.setTemplateId(999L);
        request.setClientId(1L);

        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> assessmentService.assignAssessment(request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Assessment template not found");

        verify(assignmentRepository, never()).save(any(AssessmentAssignment.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when client not found for assignment")
    void shouldThrowExceptionWhenClientNotFoundForAssignment() {
        // Arrange
        CreateAssessmentAssignmentRequest request = new CreateAssessmentAssignmentRequest();
        request.setTemplateId(1L);
        request.setClientId(999L);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(clientReportAccessService.requireClientAccess(eq(999L), any())).thenThrow(new ResourceNotFoundException("Client not found"));

        // Act & Assert
        assertThatThrownBy(() -> assessmentService.assignAssessment(request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Client not found");

        verify(assignmentRepository, never()).save(any(AssessmentAssignment.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when template is already assigned to client")
    void shouldThrowConflictExceptionWhenTemplateAlreadyAssignedToClient() {
        CreateAssessmentAssignmentRequest request = new CreateAssessmentAssignmentRequest();
        request.setTemplateId(1L);
        request.setClientId(1L);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));

        when(assignmentRepository.existsByClientIdAndTemplateIdAndIsDeletedFalse(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> assessmentService.assignAssessment(request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already assigned");

        verify(assignmentRepository, never()).save(any(AssessmentAssignment.class));
    }

    @Test
    @DisplayName("Should throw exception when request is null")
    void shouldThrowExceptionWhenRequestIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> assessmentService.createTemplate(null, adminPrincipal))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Request is required");
    }

    @Test
    @DisplayName("Should throw exception when requester is null")
    void shouldThrowExceptionWhenRequesterIsNull() {
        // Arrange
        CreateAssessmentTemplateRequest request = new CreateAssessmentTemplateRequest();
        request.setName("Test");

        // Act & Assert
        assertThatThrownBy(() -> assessmentService.createTemplate(request, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Requester is required");
    }

    @Test
    @DisplayName("Should assign assessment with assignedDate")
    void shouldAssignAssessmentWithAssignedDate() {
        when(currentUserService.requireCurrentUser(therapistPrincipal)).thenReturn(therapist);
        // Arrange
        CreateAssessmentAssignmentRequest request = new CreateAssessmentAssignmentRequest();
        request.setTemplateId(1L);
        request.setClientId(1L);
        request.setDueDate(java.time.Instant.now().plusSeconds(86400));

        java.time.Instant assignedDate = java.time.Instant.now();
        AssessmentAssignment assignment = AssessmentAssignment.builder()
                .template(template)
                .client(client)
                .status("assigned")
                .assignedDate(assignedDate)
                .build();

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(clientReportAccessService.requireClientAccess(eq(1L), any())).thenReturn(client);
        when(assignmentRepository.save(any(AssessmentAssignment.class))).thenAnswer(invocation -> {
            AssessmentAssignment ass = invocation.getArgument(0);
            ass.setId(1L);
            ass.setAssignedDate(assignedDate);
            return ass;
        });

        when(userRepository.findById(therapist.getId())).thenReturn(Optional.of(therapist));
        // Act
        AssessmentAssignmentResponse response = assessmentService.assignAssessment(request, therapistPrincipal, "127.0.0.1");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAssignedDate()).isNotNull();
        assertThat(response.getAssignedDate()).isEqualTo(assignedDate);
        verify(assignmentRepository).save(any(AssessmentAssignment.class));
    }

    @Test
    @DisplayName("Should map assignedDate in response")
    void shouldMapAssignedDateInResponse() {
        // Arrange
        java.time.Instant assignedDate = java.time.Instant.now().minusSeconds(3600);
        AssessmentAssignment assignment = AssessmentAssignment.builder()
                .template(template)
                .client(client)
                .status("assigned")
                .assignedDate(assignedDate)
                .build();
        assignment.setId(1L);

        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        AssessmentAssignmentResponse response = assessmentService.getAssignment(1L, therapistPrincipal);
        assertThat(response.getAssignedDate()).isEqualTo(assignedDate);
        verify(clientReportAccessService).validateClientAccess(client, therapistPrincipal);
    }
}
