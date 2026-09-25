package com.smart.therapy.flow.organisation.seed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * Default clinical template catalog (assessments, forms, checklists, report templates)
 * derived from legacy ClientHub template content.
 */
public final class ClinicalTemplateSeedDefinitions {

    private static final String SEED_RESOURCE = "clinical-templates/default-tenant-templates.json";
    private static final SeedCatalog CATALOG = loadCatalog();

    private ClinicalTemplateSeedDefinitions() {
    }

    public record SeedCatalog(
            List<SeedAssessmentTemplate> assessments,
            List<SeedFormTemplate> forms,
            List<SeedChecklistTemplate> checklists,
            List<SeedReportTemplate> reportTemplates
    ) {
    }

    public record SeedAssessmentTemplate(
            String name,
            String description,
            String category,
            boolean isStandardized,
            boolean isActive,
            int versionNumber,
            List<SeedAssessmentSection> sections
    ) {
    }

    public record SeedAssessmentSection(
            String title,
            String description,
            String accessLevel,
            boolean isScoring,
            String reportMapping,
            String aiReportPrompt,
            int sortOrder,
            List<SeedAssessmentQuestion> questions
    ) {
    }

    public record SeedAssessmentQuestion(
            String questionText,
            String questionType,
            boolean isRequired,
            int sortOrder,
            Integer ratingMin,
            Integer ratingMax,
            List<String> ratingLabels,
            boolean contributesToScore,
            List<SeedAssessmentOption> options
    ) {
    }

    public record SeedAssessmentOption(
            String optionKey,
            String optionText,
            String optionValue,
            BigDecimal scoreValue,
            int sortOrder
    ) {
    }

    public record SeedFormTemplate(
            String name,
            String description,
            String category,
            String instructions,
            boolean requiresSignature,
            boolean isActive,
            boolean isSystemTemplate,
            int sortOrder,
            String defaultSectionName,
            List<SeedFormField> fields
    ) {
    }

    public record SeedFormField(
            String fieldName,
            String fieldLabel,
            String fieldType,
            boolean isRequired,
            String defaultValue,
            String validationRules,
            String helpText,
            String placeholder,
            String autoPopulate,
            String conditionalDisplay,
            int sortOrder
    ) {
    }

    public record SeedChecklistTemplate(
            String name,
            String description,
            String clientType,
            String category,
            boolean isActive,
            boolean isSystem,
            int sortOrder,
            List<SeedChecklistItem> items
    ) {
    }

    public record SeedChecklistItem(
            String itemText,
            String title,
            String description,
            String category,
            int sortOrder,
            int itemOrder,
            Integer daysFromStart,
            boolean isRequired
    ) {
    }

    public record SeedReportTemplate(
            String name,
            String description,
            String aiInstructions,
            String structureText,
            boolean defaultIncludeProfile,
            boolean defaultIncludeNotes,
            boolean defaultIncludeAssessments,
            String supportingFilesGuidance,
            boolean supportingFilesExpected,
            List<String> supportingFileTypes,
            boolean isActive,
            String fileResource,
            String originalName,
            String mimeType
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeedFile(
            List<SeedAssessmentTemplate> assessments,
            List<SeedFormTemplate> forms,
            List<SeedChecklistTemplate> checklists,
            List<SeedReportTemplate> reportTemplates
    ) {
    }

    public static SeedCatalog catalog() {
        return CATALOG;
    }

    private static SeedCatalog loadCatalog() {
        try (InputStream input = ClinicalTemplateSeedDefinitions.class.getClassLoader().getResourceAsStream(SEED_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing clinical template seed resource: " + SEED_RESOURCE);
            }
            SeedFile seedFile = new ObjectMapper().readValue(input, SeedFile.class);
            return new SeedCatalog(
                    nullToEmpty(seedFile.assessments()),
                    nullToEmpty(seedFile.forms()),
                    nullToEmpty(seedFile.checklists()),
                    nullToEmpty(seedFile.reportTemplates()));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load clinical template seed resource: " + SEED_RESOURCE, ex);
        }
    }

    private static <T> List<T> nullToEmpty(List<T> values) {
        return values != null ? List.copyOf(values) : List.of();
    }
}
