package com.smart.therapy.flow.unit.migration.clienthub;

import com.smart.therapy.flow.assessment.entity.AssessmentAssignment;
import com.smart.therapy.flow.assessment.entity.AssessmentTemplate;
import com.smart.therapy.flow.assessment.repository.AssessmentAssignmentRepository;
import com.smart.therapy.flow.assessment.repository.AssessmentQuestionOptionRepository;
import com.smart.therapy.flow.assessment.repository.AssessmentQuestionRepository;
import com.smart.therapy.flow.assessment.repository.AssessmentReportRepository;
import com.smart.therapy.flow.assessment.repository.AssessmentResponseRepository;
import com.smart.therapy.flow.assessment.repository.AssessmentSectionRepository;
import com.smart.therapy.flow.assessment.repository.AssessmentTemplateRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.migration.clienthub.ClientHubAssessmentsExecuteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientHubAssessmentsExecuteServiceDedupeTest {

    @Mock private TenantTransactionExecutor tenantTransactionExecutor;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private AssessmentTemplateRepository assessmentTemplateRepository;
    @Mock private AssessmentSectionRepository assessmentSectionRepository;
    @Mock private AssessmentQuestionRepository assessmentQuestionRepository;
    @Mock private AssessmentQuestionOptionRepository assessmentQuestionOptionRepository;
    @Mock private AssessmentAssignmentRepository assessmentAssignmentRepository;
    @Mock private AssessmentResponseRepository assessmentResponseRepository;
    @Mock private AssessmentReportRepository assessmentReportRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ClientHubAssessmentsExecuteService service;

    @Test
    void repairSoftDeletesUnmappedSeedWhenMappedSiblingExists() {
        AssessmentTemplate seed = template(1L, "Mental Health Assessment", 1);
        AssessmentTemplate migrated = template(2L, "Mental Health Assessment", 2);
        AssessmentAssignment assignment = new AssessmentAssignment();
        assignment.setId(10L);
        assignment.setTemplate(seed);

        when(assessmentTemplateRepository.findAll()).thenReturn(List.of(seed, migrated));
        when(assessmentAssignmentRepository.findByTemplateId(1L)).thenReturn(List.of(assignment));
        when(assessmentTemplateRepository.getReferenceById(2L)).thenReturn(migrated);
        when(assessmentTemplateRepository.save(any(AssessmentTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assessmentAssignmentRepository.save(any(AssessmentAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int removed = service.repairUnmappedSeedTemplateDuplicates(Map.of("12", 2L));

        assertThat(removed).isEqualTo(1);
        assertThat(seed.getIsDeleted()).isTrue();
        assertThat(seed.getIsActive()).isFalse();
        assertThat(assignment.getTemplate()).isSameAs(migrated);
        verify(assessmentTemplateRepository).save(seed);
        verify(assessmentAssignmentRepository).save(assignment);
        verify(assessmentTemplateRepository, never()).save(eq(migrated));
    }

    @Test
    void repairLeavesIntentionalLocalVersionsAlone() {
        AssessmentTemplate v1 = template(1L, "Custom Assessment", 1);
        AssessmentTemplate v2 = template(2L, "Custom Assessment", 2);
        // Unrelated mapped id only — neither local version is treated as a seed duplicate.
        when(assessmentTemplateRepository.findAll()).thenReturn(List.of(v1, v2));

        int removed = service.repairUnmappedSeedTemplateDuplicates(Map.of("99", 99L));

        assertThat(removed).isZero();
        assertThat(v1.getIsDeleted()).isFalse();
        assertThat(v2.getIsDeleted()).isFalse();
        verify(assessmentTemplateRepository, never()).save(any());
    }

    private static AssessmentTemplate template(Long id, String name, int versionNumber) {
        AssessmentTemplate template = new AssessmentTemplate();
        template.setId(id);
        template.setName(name);
        template.setVersionNumber(versionNumber);
        template.setIsActive(true);
        template.setIsDeleted(false);
        return template;
    }
}
