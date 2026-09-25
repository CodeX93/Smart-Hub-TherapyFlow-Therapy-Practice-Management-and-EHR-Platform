package com.smart.therapy.flow.unit.organisation;

import com.smart.therapy.flow.assessment.entity.AssessmentTemplate;
import com.smart.therapy.flow.assessment.repository.AssessmentTemplateRepository;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.document.entity.FormTemplate;
import com.smart.therapy.flow.document.repository.FormTemplateRepository;
import com.smart.therapy.flow.document.service.StorageService;
import com.smart.therapy.flow.organisation.service.TenantClinicalTemplateSeedService;
import com.smart.therapy.flow.report.repository.ReportTemplateRepository;
import com.smart.therapy.flow.task.repository.ChecklistTemplateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantClinicalTemplateSeedService")
class TenantClinicalTemplateSeedServiceTest {

    @Mock
    private TenantTransactionExecutor tenantTransactionExecutor;

    @Mock
    private AssessmentTemplateRepository assessmentTemplateRepository;

    @Mock
    private FormTemplateRepository formTemplateRepository;

    @Mock
    private ChecklistTemplateRepository checklistTemplateRepository;

    @Mock
    private ReportTemplateRepository reportTemplateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private TenantClinicalTemplateSeedService seedService;

    @Test
    @DisplayName("seeds all clinical catalogs when tenant tables are empty")
    void seedsWhenEmpty() throws Exception {
        when(tenantTransactionExecutor.executeWrite(eq(5L), eq("tenant_test"), any()))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
        when(assessmentTemplateRepository.count()).thenReturn(0L);
        when(formTemplateRepository.count()).thenReturn(0L);
        when(checklistTemplateRepository.count()).thenReturn(0L);
        when(reportTemplateRepository.count()).thenReturn(0L);
        when(assessmentTemplateRepository.save(any(AssessmentTemplate.class))).thenAnswer(invocation -> {
            AssessmentTemplate template = invocation.getArgument(0);
            template.setId(1L);
            return template;
        });
        when(formTemplateRepository.save(any(FormTemplate.class))).thenAnswer(invocation -> {
            FormTemplate template = invocation.getArgument(0);
            template.setId(2L);
            return template;
        });
        when(reportTemplateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(storageService.uploadFile(any(), anyString(), anyString())).thenReturn("clients/report-templates/seed.docx");
        when(storageService.getFileUrl(anyString())).thenReturn("/api/documents/file/seed.docx");
        User seedUser = User.builder().build();
        seedUser.setId(1L);
        when(userRepository.findAll()).thenReturn(List.of(seedUser));

        int created = seedService.seedDefaults(5L, "tenant_test");

        assertThat(created).isPositive();
        verify(assessmentTemplateRepository, atLeastOnce()).save(any(AssessmentTemplate.class));
        verify(formTemplateRepository, atLeastOnce()).save(any(FormTemplate.class));
        verify(checklistTemplateRepository, atLeastOnce()).save(any());
        verify(reportTemplateRepository, atLeastOnce()).save(any());
        verify(storageService, atLeastOnce()).uploadFile(any(), eq("report-templates"), anyString());
    }

    @Test
    @DisplayName("skips when all clinical catalogs already exist with report files")
    void skipsWhenAlreadySeeded() throws Exception {
        when(tenantTransactionExecutor.executeWrite(eq(5L), eq("tenant_test"), any()))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
        when(assessmentTemplateRepository.count()).thenReturn(1L);
        when(formTemplateRepository.count()).thenReturn(1L);
        when(checklistTemplateRepository.count()).thenReturn(1L);
        when(reportTemplateRepository.count()).thenReturn(1L);
        User seedUser = User.builder().build();
        seedUser.setId(1L);
        when(userRepository.findAll()).thenReturn(List.of(seedUser));
        com.smart.therapy.flow.report.entity.ReportTemplate existing =
                com.smart.therapy.flow.report.entity.ReportTemplate.builder()
                        .name("Progress Report")
                        .fileBlobName("clients/report-templates/existing.docx")
                        .build();
        when(reportTemplateRepository.findFirstByNameIgnoreCase("Progress Report"))
                .thenReturn(java.util.Optional.of(existing));
        when(storageService.fileExists("clients/report-templates/existing.docx")).thenReturn(true);

        assertThat(seedService.seedDefaults(5L, "tenant_test")).isZero();
        verify(assessmentTemplateRepository, never()).save(any());
        verify(formTemplateRepository, never()).save(any());
        verify(checklistTemplateRepository, never()).save(any());
        verify(storageService, never()).uploadFile(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("skips invalid schema inputs")
    void skipsInvalidSchemaInputs() {
        assertThat(seedService.seedDefaults(null, "tenant_test")).isZero();
        assertThat(seedService.seedDefaults(1L, null)).isZero();
        assertThat(seedService.seedDefaults(1L, "public")).isZero();
    }
}
