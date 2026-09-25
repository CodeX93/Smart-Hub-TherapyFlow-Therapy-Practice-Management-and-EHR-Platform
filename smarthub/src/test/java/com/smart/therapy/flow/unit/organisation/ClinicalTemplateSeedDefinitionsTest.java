package com.smart.therapy.flow.unit.organisation;

import com.smart.therapy.flow.organisation.seed.ClinicalTemplateSeedDefinitions;
import com.smart.therapy.flow.organisation.seed.ClinicalTemplateSeedDefinitions.SeedAssessmentTemplate;
import com.smart.therapy.flow.organisation.seed.ClinicalTemplateSeedDefinitions.SeedCatalog;
import com.smart.therapy.flow.organisation.seed.ClinicalTemplateSeedDefinitions.SeedChecklistTemplate;
import com.smart.therapy.flow.organisation.seed.ClinicalTemplateSeedDefinitions.SeedFormTemplate;
import com.smart.therapy.flow.organisation.seed.ClinicalTemplateSeedDefinitions.SeedReportTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClinicalTemplateSeedDefinitions")
class ClinicalTemplateSeedDefinitionsTest {

    @Test
    @DisplayName("loads default clinical template catalog from classpath")
    void loadsDefaultCatalog() {
        SeedCatalog catalog = ClinicalTemplateSeedDefinitions.catalog();

        assertThat(catalog.assessments()).hasSize(1);
        assertThat(catalog.forms()).hasSize(1);
        assertThat(catalog.checklists()).hasSize(1);
        assertThat(catalog.reportTemplates()).hasSize(1);

        SeedAssessmentTemplate assessment = catalog.assessments().get(0);
        assertThat(assessment.name()).isEqualTo("Mental Health Assessment");
        assertThat(assessment.sections()).isNotEmpty();
        assertThat(assessment.sections().get(0).questions()).isNotEmpty();

        SeedFormTemplate form = catalog.forms().get(0);
        assertThat(form.name()).isEqualTo("Informed Consent");
        assertThat(form.category()).isEqualToIgnoringCase("consent");
        assertThat(form.fields()).hasSize(10);

        SeedChecklistTemplate checklist = catalog.checklists().get(0);
        assertThat(checklist.name()).isEqualTo("Refugee clients");
        assertThat(checklist.items()).hasSize(11);

        SeedReportTemplate report = catalog.reportTemplates().get(0);
        assertThat(report.name()).isEqualTo("Progress Report");
        assertThat(report.aiInstructions()).isNotBlank();
        assertThat(report.structureText()).isNotBlank();
        assertThat(report.fileResource()).isEqualTo(
                "clinical-templates/files/Psychotherapy_Progress_Report_Template_and_AI_Guide.docx");
        assertThat(report.originalName()).isEqualTo("Psychotherapy_Progress_Report_Template_and_AI_Guide.docx");
        assertThat(report.mimeType()).contains("wordprocessingml");
        assertThat(ClinicalTemplateSeedDefinitions.class.getClassLoader()
                .getResourceAsStream(report.fileResource())).isNotNull();
    }
}
