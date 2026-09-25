package com.smart.therapy.flow.organisation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.assessment.entity.AssessmentQuestion;
import com.smart.therapy.flow.assessment.entity.AssessmentQuestionOption;
import com.smart.therapy.flow.assessment.entity.AssessmentQuestionRatingLabel;
import com.smart.therapy.flow.assessment.entity.AssessmentSection;
import com.smart.therapy.flow.assessment.entity.AssessmentTemplate;
import com.smart.therapy.flow.assessment.repository.AssessmentTemplateRepository;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.document.entity.FormField;
import com.smart.therapy.flow.document.entity.FormSection;
import com.smart.therapy.flow.document.entity.FormTemplate;
import com.smart.therapy.flow.document.entity.FormTemplateVersion;
import com.smart.therapy.flow.document.enums.FieldType;
import com.smart.therapy.flow.document.enums.FormTemplateVersionStatus;
import com.smart.therapy.flow.document.enums.FromCategory;
import com.smart.therapy.flow.document.repository.FormTemplateRepository;
import com.smart.therapy.flow.document.service.StorageService;
import com.smart.therapy.flow.organisation.seed.ClinicalTemplateSeedDefinitions;
import com.smart.therapy.flow.organisation.seed.ClinicalTemplateSeedDefinitions.SeedAssessmentOption;
import com.smart.therapy.flow.organisation.seed.ClinicalTemplateSeedDefinitions.SeedAssessmentQuestion;
import com.smart.therapy.flow.organisation.seed.ClinicalTemplateSeedDefinitions.SeedAssessmentSection;
import com.smart.therapy.flow.organisation.seed.ClinicalTemplateSeedDefinitions.SeedAssessmentTemplate;
import com.smart.therapy.flow.organisation.seed.ClinicalTemplateSeedDefinitions.SeedCatalog;
import com.smart.therapy.flow.organisation.seed.ClinicalTemplateSeedDefinitions.SeedChecklistItem;
import com.smart.therapy.flow.organisation.seed.ClinicalTemplateSeedDefinitions.SeedChecklistTemplate;
import com.smart.therapy.flow.organisation.seed.ClinicalTemplateSeedDefinitions.SeedFormField;
import com.smart.therapy.flow.organisation.seed.ClinicalTemplateSeedDefinitions.SeedFormTemplate;
import com.smart.therapy.flow.organisation.seed.ClinicalTemplateSeedDefinitions.SeedReportTemplate;
import com.smart.therapy.flow.report.entity.ReportTemplate;
import com.smart.therapy.flow.report.repository.ReportTemplateRepository;
import com.smart.therapy.flow.report.util.BytesMultipartFile;
import com.smart.therapy.flow.task.entity.ChecklistItem;
import com.smart.therapy.flow.task.entity.ChecklistTemplate;
import com.smart.therapy.flow.task.enums.CheckListCategory;
import com.smart.therapy.flow.task.repository.ChecklistTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Seeds tenant-scoped clinical templates (assessments, forms, checklists, report layouts)
 * from legacy ClientHub defaults. Idempotent: skips catalogs that already contain rows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantClinicalTemplateSeedService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final AssessmentTemplateRepository assessmentTemplateRepository;
    private final FormTemplateRepository formTemplateRepository;
    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final ReportTemplateRepository reportTemplateRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public int seedDefaults(Long organisationId, String schemaName) {
        if (organisationId == null || schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
            return 0;
        }

        Integer seeded = tenantTransactionExecutor.executeWrite(organisationId, schemaName, () -> {
            SeedCatalog catalog = ClinicalTemplateSeedDefinitions.catalog();
            boolean seedAssessments = assessmentTemplateRepository.count() == 0 && !catalog.assessments().isEmpty();
            boolean seedForms = formTemplateRepository.count() == 0 && !catalog.forms().isEmpty();
            boolean seedChecklists = checklistTemplateRepository.count() == 0 && !catalog.checklists().isEmpty();
            boolean seedReports = reportTemplateRepository.count() == 0 && !catalog.reportTemplates().isEmpty();
            boolean repairReportFiles = !seedReports && !catalog.reportTemplates().isEmpty();

            if (!seedAssessments && !seedForms && !seedChecklists && !seedReports && !repairReportFiles) {
                return 0;
            }

            Optional<User> seedUser = userRepository.findAll().stream().findFirst();
            if ((seedAssessments || seedForms || seedReports || repairReportFiles) && seedUser.isEmpty()) {
                log.warn(
                        "Clinical template seed deferred for org {} (schema {}): no tenant user yet",
                        organisationId,
                        schemaName);
                if (!seedChecklists) {
                    return 0;
                }
            }

            int created = 0;
            if (seedAssessments && seedUser.isPresent()) {
                created += seedAssessments(catalog.assessments(), seedUser.get());
            }
            if (seedForms && seedUser.isPresent()) {
                created += seedForms(catalog.forms(), seedUser.get());
            }
            if (seedChecklists) {
                created += seedChecklists(catalog.checklists(), seedUser.orElse(null));
            }
            if (seedReports && seedUser.isPresent()) {
                created += seedReports(catalog.reportTemplates(), seedUser.get());
            } else if (repairReportFiles && seedUser.isPresent()) {
                created += repairMissingReportFiles(catalog.reportTemplates());
            }
            return created;
        });

        if (seeded != null && seeded > 0) {
            log.info("Seeded {} default clinical template record(s) for org {} (schema {})", seeded, organisationId, schemaName);
        }
        return seeded != null ? seeded : 0;
    }

    private int seedAssessments(List<SeedAssessmentTemplate> templates, User seedUser) {
        int created = 0;
        for (SeedAssessmentTemplate seed : templates) {
            AssessmentTemplate template = assessmentTemplateRepository.save(AssessmentTemplate.builder()
                    .name(seed.name())
                    .description(seed.description())
                    .category(seed.category())
                    .isStandardized(seed.isStandardized())
                    .isActive(seed.isActive())
                    .versionNumber(Math.max(seed.versionNumber(), 1))
                    .createdByUser(seedUser)
                    .build());
            created++;

            for (SeedAssessmentSection sectionSeed : nullSafeList(seed.sections())) {
                AssessmentSection section = AssessmentSection.builder()
                        .template(template)
                        .title(sectionSeed.title())
                        .description(sectionSeed.description())
                        .accessLevel(defaultIfBlank(sectionSeed.accessLevel(), "therapist_only"))
                        .isScoring(sectionSeed.isScoring())
                        .reportMapping(sectionSeed.reportMapping())
                        .aiReportPrompt(sectionSeed.aiReportPrompt())
                        .sortOrder(sectionSeed.sortOrder())
                        .build();
                template.getSections().add(section);
                created++;

                for (SeedAssessmentQuestion questionSeed : nullSafeList(sectionSeed.questions())) {
                    AssessmentQuestion question = AssessmentQuestion.builder()
                            .section(section)
                            .questionText(questionSeed.questionText())
                            .questionType(questionSeed.questionType())
                            .isRequired(questionSeed.isRequired())
                            .sortOrder(questionSeed.sortOrder())
                            .ratingMin(questionSeed.ratingMin())
                            .ratingMax(questionSeed.ratingMax())
                            .contributesToScore(questionSeed.contributesToScore())
                            .build();
                    section.getQuestions().add(question);
                    created++;

                    for (String label : nullSafeList(questionSeed.ratingLabels())) {
                        if (label == null || label.isBlank()) {
                            continue;
                        }
                        question.getRatingLabels().add(AssessmentQuestionRatingLabel.builder()
                                .question(question)
                                .label(label.trim())
                                .active(true)
                                .build());
                        created++;
                    }

                    for (SeedAssessmentOption optionSeed : nullSafeList(questionSeed.options())) {
                        question.getOptions().add(AssessmentQuestionOption.builder()
                                .question(question)
                                .optionKey(optionSeed.optionKey())
                                .optionText(optionSeed.optionText())
                                .optionValue(optionSeed.optionValue())
                                .scoreValue(optionSeed.scoreValue())
                                .sortOrder(optionSeed.sortOrder())
                                .build());
                        created++;
                    }
                }
            }
            assessmentTemplateRepository.save(template);
        }
        return created;
    }

    private int seedForms(List<SeedFormTemplate> templates, User seedUser) {
        int created = 0;
        for (SeedFormTemplate seed : templates) {
            FormTemplate template = formTemplateRepository.save(FormTemplate.builder()
                    .name(seed.name())
                    .description(seed.description())
                    .category(parseFormCategory(seed.category()))
                    .instructions(seed.instructions())
                    .requiresSignature(seed.requiresSignature())
                    .isActive(seed.isActive())
                    .isSystemTemplate(seed.isSystemTemplate())
                    .sortOrder(seed.sortOrder())
                    .createdByUser(seedUser)
                    .build());
            created++;

            FormTemplateVersion version = FormTemplateVersion.builder()
                    .template(template)
                    .versionNumber(1L)
                    .name(seed.name())
                    .description(seed.description())
                    .instructions(seed.instructions())
                    .requiresSignature(seed.requiresSignature())
                    .status(FormTemplateVersionStatus.ACTIVE)
                    .createdByUser(seedUser)
                    .build();
            template.getVersions().add(version);
            created++;

            String sectionName = defaultIfBlank(seed.defaultSectionName(), "Main");
            FormSection section = FormSection.builder()
                    .templateVersion(version)
                    .name(sectionName)
                    .sortOrder(0)
                    .build();
            version.getSections().add(section);
            created++;

            for (SeedFormField fieldSeed : nullSafeList(seed.fields())) {
                section.getFields().add(FormField.builder()
                        .templateVersion(version)
                        .section(section)
                        .fieldName(fieldSeed.fieldName())
                        .fieldLabel(fieldSeed.fieldLabel())
                        .fieldType(parseFieldType(fieldSeed.fieldType()))
                        .isRequired(fieldSeed.isRequired())
                        .defaultValue(fieldSeed.defaultValue())
                        .validationRules(fieldSeed.validationRules())
                        .helpText(fieldSeed.helpText())
                        .placeholder(fieldSeed.placeholder())
                        .autoPopulate(fieldSeed.autoPopulate())
                        .conditionalDisplay(fieldSeed.conditionalDisplay())
                        .sortOrder(fieldSeed.sortOrder())
                        .build());
                created++;
            }
            formTemplateRepository.save(template);
        }
        return created;
    }

    private int seedChecklists(List<SeedChecklistTemplate> templates, User seedUser) {
        int created = 0;
        for (SeedChecklistTemplate seed : templates) {
            ChecklistTemplate template = ChecklistTemplate.builder()
                    .name(seed.name())
                    .description(seed.description())
                    .clientType(seed.clientType())
                    .category(seed.category())
                    .isActive(seed.isActive())
                    .isSystem(seed.isSystem())
                    .sortOrder(seed.sortOrder())
                    .createdByUser(seedUser)
                    .build();
            created++;

            for (SeedChecklistItem itemSeed : nullSafeList(seed.items())) {
                template.getItems().add(ChecklistItem.builder()
                        .template(template)
                        .itemText(itemSeed.itemText())
                        .title(itemSeed.title())
                        .description(itemSeed.description())
                        .category(parseChecklistCategory(itemSeed.category()))
                        .sortOrder(itemSeed.sortOrder())
                        .itemOrder(itemSeed.itemOrder())
                        .daysFromStart(itemSeed.daysFromStart())
                        .isRequired(itemSeed.isRequired())
                        .build());
                created++;
            }
            checklistTemplateRepository.save(template);
        }
        return created;
    }

    private int seedReports(List<SeedReportTemplate> templates, User seedUser) {
        int created = 0;
        for (SeedReportTemplate seed : templates) {
            ReportTemplate template = ReportTemplate.builder()
                    .name(seed.name())
                    .description(seed.description())
                    .aiInstructions(seed.aiInstructions())
                    .structureText(seed.structureText())
                    .defaultIncludeProfile(seed.defaultIncludeProfile())
                    .defaultIncludeNotes(seed.defaultIncludeNotes())
                    .defaultIncludeAssessments(seed.defaultIncludeAssessments())
                    .supportingFilesGuidance(seed.supportingFilesGuidance())
                    .supportingFilesExpected(seed.supportingFilesExpected())
                    .supportingFileTypesJson(toJsonArray(seed.supportingFileTypes()))
                    .isActive(seed.isActive())
                    .createdByUser(seedUser)
                    .build();
            template = reportTemplateRepository.save(template);
            created++;
            created += attachReportFile(template, seed);
        }
        return created;
    }

    private int repairMissingReportFiles(List<SeedReportTemplate> templates) {
        int repaired = 0;
        for (SeedReportTemplate seed : templates) {
            if (!StringUtils.hasText(seed.fileResource())) {
                continue;
            }
            Optional<ReportTemplate> existing = reportTemplateRepository.findFirstByNameIgnoreCase(seed.name());
            if (existing.isEmpty()) {
                continue;
            }
            ReportTemplate template = existing.get();
            if (StringUtils.hasText(template.getFileBlobName()) && storageService.fileExists(template.getFileBlobName())) {
                continue;
            }
            repaired += attachReportFile(template, seed);
        }
        return repaired;
    }

    private int attachReportFile(ReportTemplate template, SeedReportTemplate seed) {
        if (!StringUtils.hasText(seed.fileResource())) {
            return 0;
        }
        byte[] fileBytes;
        try {
            fileBytes = loadClasspathBytes(seed.fileResource());
        } catch (IOException ex) {
            log.warn("Could not load report template file {}: {}", seed.fileResource(), ex.getMessage());
            return 0;
        }
        if (fileBytes.length == 0) {
            return 0;
        }

        String originalName = StringUtils.hasText(seed.originalName())
                ? seed.originalName()
                : "report-template.docx";
        String mimeType = StringUtils.hasText(seed.mimeType())
                ? seed.mimeType()
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

        try {
            BytesMultipartFile multipart = new BytesMultipartFile(fileBytes, originalName, mimeType);
            String blobKey = storageService.uploadFile(multipart, "report-templates", originalName);
            template.setOriginalName(originalName);
            template.setMimeType(mimeType);
            template.setFileSize(fileBytes.length);
            template.setFileBlobName(blobKey);
            try {
                template.setFileUrl(storageService.getFileUrl(blobKey));
            } catch (Exception urlEx) {
                log.warn("Could not generate file URL for seeded report template {}: {}", template.getName(), urlEx.getMessage());
            }
            reportTemplateRepository.save(template);
            return 1;
        } catch (Exception ex) {
            log.warn("Failed to upload seeded report template file for {}: {}", template.getName(), ex.getMessage());
            return 0;
        }
    }

    private static byte[] loadClasspathBytes(String resourcePath) throws IOException {
        try (InputStream input = TenantClinicalTemplateSeedService.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IOException("Missing classpath resource: " + resourcePath);
            }
            return input.readAllBytes();
        }
    }

    private static FromCategory parseFormCategory(String value) {
        if (value == null || value.isBlank()) {
            return FromCategory.CUSTOM;
        }
        String normalized = value.trim();
        for (FromCategory category : FromCategory.values()) {
            if (category.name().equalsIgnoreCase(normalized)) {
                return category;
            }
        }
        return FromCategory.CUSTOM;
    }

    private static FieldType parseFieldType(String value) {
        if (value == null || value.isBlank()) {
            return FieldType.TEXT;
        }
        try {
            return FieldType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return FieldType.TEXT;
        }
    }

    private static CheckListCategory parseChecklistCategory(String value) {
        if (value == null || value.isBlank()) {
            return CheckListCategory.INTAKE;
        }
        try {
            return CheckListCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return CheckListCategory.INTAKE;
        }
    }

    private static String toJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static <T> List<T> nullSafeList(List<T> values) {
        return values != null ? values : List.of();
    }
}
