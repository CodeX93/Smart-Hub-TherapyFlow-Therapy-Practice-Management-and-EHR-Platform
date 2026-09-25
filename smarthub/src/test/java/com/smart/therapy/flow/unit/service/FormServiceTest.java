package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.document.dto.*;
import com.smart.therapy.flow.document.entity.FormAssignment;
import com.smart.therapy.flow.document.entity.FormAssignmentField;
import com.smart.therapy.flow.document.entity.FormTemplate;
import com.smart.therapy.flow.document.entity.FormTemplateVersion;
import com.smart.therapy.flow.document.enums.FormTemplateVersionStatus;
import com.smart.therapy.flow.document.enums.FromCategory;
import com.smart.therapy.flow.document.repository.*;
import com.smart.therapy.flow.document.repository.FormAssignmentFieldRepository;
import com.smart.therapy.flow.document.repository.FormSectionRepository;
import com.smart.therapy.flow.document.repository.FormTemplateVersionRepository;
import com.smart.therapy.flow.document.service.FormService;
import com.smart.therapy.flow.notification.service.NotificationService;
import com.smart.therapy.flow.report.service.ClientReportAccessService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FormService Unit Tests")
@SuppressWarnings("null") // Suppress null warnings from Mockito mocks
class FormServiceTest {

    @Mock
    private FormTemplateRepository formTemplateRepository;

    @Mock
    private FormFieldRepository formFieldRepository;

    @Mock
    private FormAssignmentRepository formAssignmentRepository;

    @Mock
    private FormResponseRepository formResponseRepository;

    @Mock
    private FormSignatureRepository formSignatureRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CurrentUserService currentUserService;

    @Spy
    private PermissionChecker permissionChecker = new PermissionChecker();

    @Mock
    private ClientReportAccessService clientReportAccessService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private FormTemplateVersionRepository formTemplateVersionRepository;

    @Mock
    private FormSectionRepository formSectionRepository;

    @Mock
    private FormAssignmentFieldRepository formAssignmentFieldRepository;

    @InjectMocks
    private FormService formService;

    private AuthPrincipal therapistPrincipal;
    private User therapist;
    private Client client;
    private FormTemplate template;

    @BeforeEach
    void setUp() {
        therapist = TestDataFactory.createTestTherapist();
        therapist.setId(1L);
        therapistPrincipal = TestDataFactory.createAuthPrincipal(therapist, "ROLE_THERAPIST", "CLIENT_VIEW_OWN", "FORM_FILL");

        client = TestDataFactory.createTestClient();
        client.setId(1L);

        template = FormTemplate.builder()
                .name("Intake Form")
                .description("Initial client intake form")
                .isActive(true)
                .build();
        template.setId(1L);
    }

    @Test
    @DisplayName("Should get all active form templates")
    void shouldGetAllActiveFormTemplates() {
        // Arrange
        when(formTemplateRepository.findActiveTemplatesNewestFirst())
                .thenReturn(List.of(template));

        // Act
        List<FormTemplateResponse> templates = formService.getTemplates();

        // Assert
        assertThat(templates).isNotNull();
        assertThat(templates).hasSize(1);
        assertThat(templates.get(0).getName()).isEqualTo("Intake Form");
        verify(formTemplateRepository).findActiveTemplatesNewestFirst();
    }

    @Test
    @DisplayName("Should return templates ordered by created date descending")
    void shouldReturnTemplatesOrderedByCreatedDateDescending() {
        FormTemplate olderTemplate = FormTemplate.builder()
                .name("Older Form")
                .isActive(true)
                .build();
        olderTemplate.setId(1L);
        olderTemplate.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));

        FormTemplate newerTemplate = FormTemplate.builder()
                .name("Newer Form")
                .isActive(true)
                .build();
        newerTemplate.setId(2L);
        newerTemplate.setCreatedAt(Instant.parse("2026-07-01T10:00:00Z"));

        when(formTemplateRepository.findActiveTemplatesNewestFirst())
                .thenReturn(List.of(newerTemplate, olderTemplate));

        List<FormTemplateResponse> templates = formService.getTemplates(1, 20, null, null);

        assertThat(templates).extracting(FormTemplateResponse::getName)
                .containsExactly("Newer Form", "Older Form");
    }

    @Test
    @DisplayName("Should apply combined search and category filters case-insensitively")
    void shouldApplyCombinedSearchAndCategoryFiltersCaseInsensitively() {
        // Arrange
        FormTemplate intakeTemplate = FormTemplate.builder()
                .name("Client Intake")
                .description("Initial onboarding form")
                .category(FromCategory.INTake)
                .isActive(true)
                .build();
        FormTemplate consentTemplate = FormTemplate.builder()
                .name("Consent Packet")
                .description("Client consent form")
                .category(FromCategory.CONSENT)
                .isActive(true)
                .build();
        FormTemplate dischargeTemplate = FormTemplate.builder()
                .name("Discharge Summary")
                .description("Closing notes")
                .category(FromCategory.DISCHARGE)
                .isActive(true)
                .build();

        when(formTemplateRepository.findActiveTemplatesNewestFirst())
                .thenReturn(List.of(intakeTemplate, consentTemplate, dischargeTemplate));

        // Act
        List<FormTemplateResponse> templates = formService.getTemplates(1, 20, "CLIENT", "intake");

        // Assert
        assertThat(templates).hasSize(1);
        assertThat(templates.get(0).getName()).isEqualTo("Client Intake");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"in-take", "in_take", "IN TAKE", "intake", " INTAKE "})
    @DisplayName("Should normalize category token while filtering templates")
    void shouldNormalizeCategoryTokenWhenFilteringTemplates(String category) {
        // Arrange
        FormTemplate intakeTemplate = FormTemplate.builder()
                .name("Intake")
                .description("Intake workflow")
                .category(FromCategory.INTake)
                .isActive(true)
                .build();
        FormTemplate safetyTemplate = FormTemplate.builder()
                .name("Safety Form")
                .description("Safety checklist")
                .category(FromCategory.SAFETY)
                .isActive(true)
                .build();

        when(formTemplateRepository.findActiveTemplatesNewestFirst())
                .thenReturn(List.of(intakeTemplate, safetyTemplate));

        // Act
        List<FormTemplateResponse> templates = formService.getTemplates(1, 20, null, category);

        // Assert
        assertThat(templates).hasSize(1);
        assertThat(templates.get(0).getCategory()).isEqualTo("INTake");
    }

    @Test
    @DisplayName("Should get form template by ID successfully")
    void shouldGetFormTemplateByIdSuccessfully() {
        // Arrange
        Long templateId = 1L;
        when(formTemplateRepository.findByIdWithVersions(templateId)).thenReturn(Optional.of(template));

        // Act
        FormTemplateResponse response = formService.getTemplate(templateId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(templateId);
        assertThat(response.getName()).isEqualTo("Intake Form");
        verify(formTemplateRepository).findByIdWithVersions(templateId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when template not found")
    void shouldThrowExceptionWhenTemplateNotFound() {
        // Arrange
        Long templateId = 999L;
        when(formTemplateRepository.findByIdWithVersions(templateId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> formService.getTemplate(templateId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Form template not found");

        verify(formTemplateRepository).findByIdWithVersions(templateId);
    }

    @Test
    @DisplayName("Should assign form to client successfully")
    void shouldAssignFormToClientSuccessfully() {
        when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
        // Arrange
        CreateFormAssignmentRequest request = new CreateFormAssignmentRequest();
        request.setTemplateId(1L);
        request.setClientId(1L);

        FormAssignment assignment = FormAssignment.builder()
                .client(client)
                .status(com.smart.therapy.flow.document.enums.Status.ASSIGNED)
                .build();

        FormTemplateVersion version = FormTemplateVersion.builder().template(template).versionNumber(1L).status(FormTemplateVersionStatus.ACTIVE).build();
        version.setId(11L);
        when(formTemplateVersionRepository.findByTemplateIdAndStatusAndIsDeletedFalse(1L, FormTemplateVersionStatus.ACTIVE)).thenReturn(Optional.of(version));
        when(formTemplateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(clientReportAccessService.requireClientAccess(1L, therapistPrincipal)).thenReturn(client);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(therapist));
        assignment.setId(1L);
        assignment.setTemplateVersion(version);
        when(formAssignmentRepository.save(any(FormAssignment.class))).thenReturn(assignment);

        // Act
        FormAssignmentResponse response = formService.createAssignment(request, therapistPrincipal, "127.0.0.1");

        // Assert
        assertThat(response).isNotNull();
        verify(formTemplateRepository).findById(1L);
        verify(clientReportAccessService).requireClientAccess(1L, therapistPrincipal);
        verify(formAssignmentRepository).save(any(FormAssignment.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when template not found for assignment")
    void shouldThrowExceptionWhenTemplateNotFoundForAssignment() {
        // Arrange
        CreateFormAssignmentRequest request = new CreateFormAssignmentRequest();
        request.setTemplateId(999L);
        request.setClientId(1L);

        when(formTemplateRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> formService.createAssignment(request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Form template not found");

        verify(formAssignmentRepository, never()).save(any(FormAssignment.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when client not found for assignment")
    void shouldThrowExceptionWhenClientNotFoundForAssignment() {
        // Arrange
        CreateFormAssignmentRequest request = new CreateFormAssignmentRequest();
        request.setTemplateId(1L);
        request.setClientId(999L);

        FormTemplateVersion version = FormTemplateVersion.builder().template(template).versionNumber(1L).status(FormTemplateVersionStatus.ACTIVE).build();
        version.setId(11L);
        when(formTemplateVersionRepository.findByTemplateIdAndStatusAndIsDeletedFalse(1L, FormTemplateVersionStatus.ACTIVE)).thenReturn(Optional.of(version));
        when(formTemplateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(clientReportAccessService.requireClientAccess(999L, therapistPrincipal)).thenThrow(new ResourceNotFoundException("Client not found"));

        // Act & Assert
        assertThatThrownBy(() -> formService.createAssignment(request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Client not found");

        verify(formAssignmentRepository, never()).save(any(FormAssignment.class));
    }

    @Test
    @DisplayName("Should submit form response successfully")
    void shouldSubmitFormResponseSuccessfully() {
        when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
        // Arrange
        Long assignmentId = 1L;
        SubmitFormResponseRequest request = new SubmitFormResponseRequest();
        request.setAssignmentId(assignmentId);
        request.setResponses(Map.of(10L, "Synthetic response"));
        FormAssignmentField field = FormAssignmentField.builder().fieldLabel("Test question").fieldType("TEXT").build();
        field.setId(10L);
        when(formAssignmentFieldRepository.findByAssignmentIdAndIsDeletedFalseOrderBySortOrderAsc(assignmentId)).thenReturn(List.of(field));

        FormAssignment assignment = FormAssignment.builder()
                .client(client)
                .status(com.smart.therapy.flow.document.enums.Status.ASSIGNED)
                .build();

        when(formAssignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(formResponseRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(formAssignmentRepository.save(any(FormAssignment.class))).thenReturn(assignment);

        // Act
        List<FormResponseDto> responses = formService.submitResponses(assignmentId, request, therapistPrincipal,
                "127.0.0.1");

        // Assert
        assertThat(responses).isNotNull();
        verify(formAssignmentRepository).findById(assignmentId);
        verify(formResponseRepository).saveAll(anyList());
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getValue()).isEqualTo("Synthetic response");
        assertThat(responses.get(0).getAssignmentFieldId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when assignment not found")
    void shouldThrowExceptionWhenAssignmentNotFound() {
        // Arrange
        Long assignmentId = 999L;
        SubmitFormResponseRequest request = new SubmitFormResponseRequest();

        when(formAssignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> formService.submitResponses(assignmentId, request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Form assignment not found");

        verify(formResponseRepository, never()).save(any());
    }
}
