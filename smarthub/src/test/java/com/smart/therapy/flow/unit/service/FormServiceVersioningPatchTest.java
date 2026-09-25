package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.document.dto.FormSignatureResponse;
import com.smart.therapy.flow.document.dto.SubmitFormSignatureRequest;
import com.smart.therapy.flow.document.dto.UpdateFormTemplateRequest;
import com.smart.therapy.flow.document.entity.FormAssignment;
import com.smart.therapy.flow.document.entity.FormField;
import com.smart.therapy.flow.document.entity.FormSection;
import com.smart.therapy.flow.document.entity.FormSignature;
import com.smart.therapy.flow.document.entity.FormTemplate;
import com.smart.therapy.flow.document.entity.FormTemplateVersion;
import com.smart.therapy.flow.document.enums.FieldType;
import com.smart.therapy.flow.document.enums.FormTemplateVersionStatus;
import com.smart.therapy.flow.document.enums.FromCategory;
import com.smart.therapy.flow.document.enums.Status;
import com.smart.therapy.flow.document.repository.FormAssignmentFieldRepository;
import com.smart.therapy.flow.document.repository.FormAssignmentRepository;
import com.smart.therapy.flow.document.repository.FormFieldOptionRepository;
import com.smart.therapy.flow.document.repository.FormFieldRepository;
import com.smart.therapy.flow.document.repository.FormResponseRepository;
import com.smart.therapy.flow.document.repository.FormSectionRepository;
import com.smart.therapy.flow.document.repository.FormSignatureRepository;
import com.smart.therapy.flow.document.repository.FormTemplateRepository;
import com.smart.therapy.flow.document.repository.FormTemplateVersionRepository;
import com.smart.therapy.flow.document.service.FormService;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FormService PATCH Versioning Tests")
class FormServiceVersioningPatchTest {

    @Mock
    private FormTemplateRepository formTemplateRepository;
    @Mock
    private FormTemplateVersionRepository formTemplateVersionRepository;
    @Mock
    private FormSectionRepository formSectionRepository;
    @Mock
    private FormFieldRepository formFieldRepository;
    @Mock
    private FormFieldOptionRepository formFieldOptionRepository;
    @Mock
    private FormAssignmentRepository formAssignmentRepository;
    @Mock
    private FormAssignmentFieldRepository formAssignmentFieldRepository;
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
    private CurrentUserService currentUserService;
    @Mock
    private PermissionChecker permissionChecker;
    @Mock
    private SubscriptionFeatureService subscriptionFeatureService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private FormService formService;

    @Mock
    private AuthPrincipal principal;

    private User adminUser;
    private FormTemplate template;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(42L);

        template = FormTemplate.builder()
                .name("Template A")
                .description("desc")
                .category(FromCategory.CUSTOM)
                .instructions("old instructions")
                .requiresSignature(true)
                .isActive(true)
                .sortOrder(1)
                .createdByUser(adminUser)
                .build();
        template.setId(100L);
        template.setIsDeleted(false);

    }

    @Test
    @DisplayName("Metadata-only PATCH updates template without creating new version")
    void metadataOnlyPatchDoesNotCreateVersion() {
        when(currentUserService.requireCurrentUser(principal)).thenReturn(adminUser);
        when(formTemplateRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(template));
        when(formTemplateRepository.findByIdWithVersions(100L)).thenReturn(Optional.of(template));
        when(formTemplateRepository.save(any(FormTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        UpdateFormTemplateRequest request = new UpdateFormTemplateRequest();
        request.markFieldPresent("name");
        request.setName("Renamed");
        request.markFieldPresent("sortOrder");
        request.setSortOrder(7);

        formService.updateTemplate(100L, request, principal, "127.0.0.1");

        assertThat(template.getName()).isEqualTo("Renamed");
        assertThat(template.getSortOrder()).isEqualTo(7);
        verify(formTemplateVersionRepository, never()).findMaxVersionNumberByTemplateId(anyLong());
        verify(formTemplateVersionRepository, never()).archiveActiveVersions(anyLong(), anyLong(), any(), any());
        verify(formTemplateVersionRepository, never()).save(any(FormTemplateVersion.class));
    }

    @Test
    @DisplayName("Structural PATCH with fields omitted clones prior structure")
    void structuralPatchWithOmittedFieldsClonesPreviousStructure() {
        when(currentUserService.requireCurrentUser(principal)).thenReturn(adminUser);
        when(formTemplateRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(template));
        when(formTemplateRepository.findByIdWithVersions(100L)).thenReturn(Optional.of(template));
        when(formTemplateRepository.save(any(FormTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        UpdateFormTemplateRequest request = new UpdateFormTemplateRequest();
        request.markFieldPresent("instructions");
        request.setInstructions("new instructions");
        // Omitted fields preserve the previous structure.

        FormTemplateVersion active = buildActiveVersion(11L, 3L, true, "old version instructions");
        when(formTemplateVersionRepository.findByTemplateIdAndStatusAndIsDeletedFalse(100L, FormTemplateVersionStatus.ACTIVE))
                .thenReturn(Optional.of(active));
        when(formTemplateVersionRepository.findMaxVersionNumberByTemplateId(100L)).thenReturn(3L);

        AtomicLong versionIds = new AtomicLong(200L);
        when(formTemplateVersionRepository.save(any(FormTemplateVersion.class))).thenAnswer(invocation -> {
            FormTemplateVersion v = invocation.getArgument(0);
            if (v.getId() == null) {
                v.setId(versionIds.getAndIncrement());
            }
            return v;
        });

        FormSection oldSection = FormSection.builder()
                .templateVersion(active)
                .name("Old Section")
                .description("old")
                .sortOrder(0)
                .build();
        oldSection.setId(301L);
        oldSection.setIsDeleted(false);

        FormField oldField = FormField.builder()
                .templateVersion(active)
                .section(oldSection)
                .fieldName("full_name")
                .fieldLabel("Full Name")
                .fieldType(FieldType.TEXT)
                .isRequired(true)
                .sortOrder(0)
                .build();
        oldField.setId(401L);
        oldField.setIsDeleted(false);

        when(formSectionRepository.findByTemplateVersionIdAndIsDeletedFalseOrderBySortOrderAsc(active.getId()))
                .thenReturn(List.of(oldSection));
        when(formFieldRepository.findBySectionIdAndIsDeletedFalseOrderBySortOrderAsc(oldSection.getId()))
                .thenReturn(List.of(oldField));
        when(formFieldOptionRepository.findByFieldIdAndIsDeletedFalseOrderBySortOrderAsc(oldField.getId()))
                .thenReturn(List.of());
        when(formSectionRepository.save(any(FormSection.class))).thenAnswer(invocation -> {
            FormSection s = invocation.getArgument(0);
            if (s.getId() == null) {
                s.setId(500L);
            }
            return s;
        });
        when(formFieldRepository.save(any(FormField.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(formTemplateVersionRepository.archiveActiveVersions(eq(100L), anyLong(),
                eq(FormTemplateVersionStatus.ACTIVE), eq(FormTemplateVersionStatus.ARCHIVED))).thenReturn(1);

        formService.updateTemplate(100L, request, principal, "127.0.0.1");

        verify(formSectionRepository).findByTemplateVersionIdAndIsDeletedFalseOrderBySortOrderAsc(active.getId());
        verify(formFieldRepository).findBySectionIdAndIsDeletedFalseOrderBySortOrderAsc(oldSection.getId());
        verify(formTemplateVersionRepository).archiveActiveVersions(eq(100L), anyLong(),
                eq(FormTemplateVersionStatus.ACTIVE), eq(FormTemplateVersionStatus.ARCHIVED));
    }

    @Test
    @DisplayName("Structural PATCH with fields empty list replaces with zero fields and no clone")
    void structuralPatchWithEmptyFieldsDoesNotClone() {
        when(currentUserService.requireCurrentUser(principal)).thenReturn(adminUser);
        when(formTemplateRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(template));
        when(formTemplateRepository.findByIdWithVersions(100L)).thenReturn(Optional.of(template));
        when(formTemplateRepository.save(any(FormTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        UpdateFormTemplateRequest request = new UpdateFormTemplateRequest();
        request.markFieldPresent("fields");
        request.setFields(List.of());

        FormTemplateVersion active = buildActiveVersion(21L, 5L, true, "v5");
        when(formTemplateVersionRepository.findByTemplateIdAndStatusAndIsDeletedFalse(100L, FormTemplateVersionStatus.ACTIVE)).thenReturn(Optional.of(active));
        when(formTemplateVersionRepository.findMaxVersionNumberByTemplateId(100L)).thenReturn(5L);
        when(formTemplateVersionRepository.save(any(FormTemplateVersion.class))).thenAnswer(inv -> {
            FormTemplateVersion version = inv.getArgument(0);
            version.setId(22L);
            return version;
        });

        formService.updateTemplate(100L, request, principal, "127.0.0.1");

        verify(formSectionRepository, never()).findByTemplateVersionIdAndIsDeletedFalseOrderBySortOrderAsc(anyLong());
        verify(formFieldRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Structural PATCH with no active version and fields omitted creates default section")
    void structuralPatchWithNoActiveVersionAndOmittedFieldsCreatesDefaultSection() {
        when(currentUserService.requireCurrentUser(principal)).thenReturn(adminUser);
        when(formTemplateRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(template));
        when(formTemplateRepository.findByIdWithVersions(100L)).thenReturn(Optional.of(template));
        when(formTemplateRepository.save(any(FormTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        UpdateFormTemplateRequest request = new UpdateFormTemplateRequest();
        request.markFieldPresent("requiresSignature");
        request.setRequiresSignature(false);
        // Omitted fields preserve the previous structure.

        when(formTemplateVersionRepository.findByTemplateIdAndStatusAndIsDeletedFalse(100L, FormTemplateVersionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(formTemplateVersionRepository.findMaxVersionNumberByTemplateId(100L)).thenReturn(0L);
        when(formTemplateVersionRepository.save(any(FormTemplateVersion.class))).thenAnswer(invocation -> {
            FormTemplateVersion v = invocation.getArgument(0);
            if (v.getId() == null) {
                v.setId(1200L);
            }
            return v;
        });
        when(formSectionRepository.save(any(FormSection.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(formTemplateVersionRepository.archiveActiveVersions(eq(100L), anyLong(),
                eq(FormTemplateVersionStatus.ACTIVE), eq(FormTemplateVersionStatus.ARCHIVED))).thenReturn(0);

        formService.updateTemplate(100L, request, principal, "127.0.0.1");

        verify(formSectionRepository, times(1)).save(any(FormSection.class));
        verify(formTemplateVersionRepository).archiveActiveVersions(eq(100L), anyLong(),
                eq(FormTemplateVersionStatus.ACTIVE), eq(FormTemplateVersionStatus.ARCHIVED));
    }

    @Test
    @DisplayName("Signature submission uses version requiresSignature first then template fallback")
    void submitSignatureUsesVersionRequiresSignatureThenTemplateFallback() {
        when(currentUserService.requireCurrentUser(principal)).thenReturn(adminUser);
        FormTemplate signatureTemplate = FormTemplate.builder()
                .name("Consent")
                .category(FromCategory.CONSENT)
                .requiresSignature(true)
                .isActive(true)
                .createdByUser(adminUser)
                .build();
        signatureTemplate.setId(888L);
        signatureTemplate.setIsDeleted(false);

        FormTemplateVersion version = FormTemplateVersion.builder()
                .template(signatureTemplate)
                .versionNumber(1L)
                .status(FormTemplateVersionStatus.ACTIVE)
                .createdByUser(adminUser)
                .requiresSignature(null)
                .build();
        version.setId(889L);
        version.setIsDeleted(false);

        FormAssignment assignment = FormAssignment.builder()
                .templateVersion(version)
                .assignedBy(adminUser)
                .status(Status.IN_PROGRESS)
                .build();
        assignment.setId(777L);

        SubmitFormSignatureRequest request = new SubmitFormSignatureRequest();
        request.setSignatureData("base64sig");
        request.setSignerName("Client A");
        request.setSignerRole("client");
        request.setAgreedToTerms(true);

        when(formAssignmentRepository.findById(777L)).thenReturn(Optional.of(assignment));
        when(formSignatureRepository.save(any(FormSignature.class))).thenAnswer(invocation -> {
            FormSignature sig = invocation.getArgument(0);
            sig.setId(3000L);
            return sig;
        });
        when(formAssignmentRepository.save(any(FormAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FormSignatureResponse response = formService.submitSignature(777L, request, principal, "127.0.0.1", "JUnit");

        assertThat(response).isNotNull();
        verify(formSignatureRepository).save(any(FormSignature.class));
    }

    @Test
    @DisplayName("Signature submission fails when both version and template do not require signature")
    void submitSignatureFailsWhenNoSignatureRequired() {
        FormTemplate signatureTemplate = FormTemplate.builder()
                .name("No Signature Form")
                .category(FromCategory.CUSTOM)
                .requiresSignature(false)
                .isActive(true)
                .createdByUser(adminUser)
                .build();
        signatureTemplate.setIsDeleted(false);

        FormTemplateVersion version = FormTemplateVersion.builder()
                .template(signatureTemplate)
                .versionNumber(1L)
                .status(FormTemplateVersionStatus.ACTIVE)
                .createdByUser(adminUser)
                .requiresSignature(false)
                .build();
        version.setIsDeleted(false);

        FormAssignment assignment = FormAssignment.builder()
                .templateVersion(version)
                .assignedBy(adminUser)
                .status(Status.IN_PROGRESS)
                .build();
        assignment.setId(778L);

        SubmitFormSignatureRequest request = new SubmitFormSignatureRequest();
        request.setSignatureData("base64sig");
        request.setSignerName("Client B");

        when(formAssignmentRepository.findById(778L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> formService.submitSignature(778L, request, principal, "127.0.0.1", "JUnit"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not require a signature");
    }

    @Test
    @DisplayName("Update template throws when template is missing")
    void updateTemplateThrowsWhenTemplateMissing() {
        when(formTemplateRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());
        UpdateFormTemplateRequest request = new UpdateFormTemplateRequest();
        request.markFieldPresent("name");
        request.setName("new");

        assertThatThrownBy(() -> formService.updateTemplate(999L, request, principal, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Form template not found");
    }

    private FormTemplateVersion buildActiveVersion(Long id, Long versionNumber, Boolean requiresSignature, String instructions) {
        FormTemplateVersion version = FormTemplateVersion.builder()
                .template(template)
                .versionNumber(versionNumber)
                .name(template.getName())
                .description(template.getDescription())
                .instructions(instructions)
                .requiresSignature(requiresSignature)
                .status(FormTemplateVersionStatus.ACTIVE)
                .createdByUser(adminUser)
                .build();
        version.setId(id);
        version.setIsDeleted(false);
        return version;
    }
}
