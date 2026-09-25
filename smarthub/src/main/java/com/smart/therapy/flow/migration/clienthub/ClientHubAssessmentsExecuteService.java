package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.assessment.entity.AssessmentAssignment;
import com.smart.therapy.flow.assessment.entity.AssessmentQuestion;
import com.smart.therapy.flow.assessment.entity.AssessmentQuestionOption;
import com.smart.therapy.flow.assessment.entity.AssessmentQuestionRatingLabel;
import com.smart.therapy.flow.assessment.entity.AssessmentReport;
import com.smart.therapy.flow.assessment.entity.AssessmentResponse;
import com.smart.therapy.flow.assessment.entity.AssessmentResponseOption;
import com.smart.therapy.flow.assessment.entity.AssessmentSection;
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
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentAssignmentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentQuestionOptionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentQuestionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentReportRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentResponseRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentSectionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentTemplateRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientHubAssessmentsExecuteService {

    private static final int BATCH_SIZE = 25;
    private static final Pattern LEADING_INTEGER = Pattern.compile("^\\s*(\\d+)");

    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final JdbcTemplate jdbcTemplate;
    private final AssessmentTemplateRepository assessmentTemplateRepository;
    private final AssessmentSectionRepository assessmentSectionRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final AssessmentQuestionOptionRepository assessmentQuestionOptionRepository;
    private final AssessmentAssignmentRepository assessmentAssignmentRepository;
    private final AssessmentResponseRepository assessmentResponseRepository;
    private final AssessmentReportRepository assessmentReportRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    public AssessmentsExecuteResult execute(
            List<SourceAssessmentTemplateRecord> templates,
            List<SourceAssessmentSectionRecord> sections,
            List<SourceAssessmentQuestionRecord> questions,
            List<SourceAssessmentQuestionOptionRecord> options,
            List<SourceAssessmentAssignmentRecord> assignments,
            List<SourceAssessmentResponseRecord> responses,
            List<SourceAssessmentReportRecord> reports,
            TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException(
                    "Exactly one target organisation is required for assessments execution");
        }

        Map<String, Long> clientMappings = loadMappingsOutsideTenant(target.organisationId(), "clients");
        Map<String, Long> userMappings = loadMappingsOutsideTenant(target.organisationId(), "users");
        Long defaultStaffUserId = resolveLowestMappedStaffUserId(userMappings);

        // Soft-delete placeholder seed templates that share a name with a migrated
        // ClientHub template (seed + migrate previously created Version 1 + Version 2).
        tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(), () -> {
            Map<String, Long> templateMappings = loadMappings(target.organisationId(), "assessment_templates");
            int removed = repairUnmappedSeedTemplateDuplicates(templateMappings);
            if (removed > 0) {
                log.info("Removed {} seed assessment template duplicate(s) for org {}",
                        removed, target.organisationId());
            }
            return removed;
        });

        Counts templateCounts = runBatched(
                "assessment_templates",
                templates,
                batch -> tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(), () -> {
                    Map<String, Long> templateMappings = loadMappings(target.organisationId(), "assessment_templates");
                    Set<String> occupiedNameVersions = loadOccupiedTemplateNameVersions();
                    return upsertTemplates(batch, target, userMappings, templateMappings,
                            occupiedNameVersions, defaultStaffUserId);
                }));

        Counts sectionCounts = runBatched(
                "assessment_sections",
                sections,
                batch -> tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(), () -> {
                    Map<String, Long> templateMappings = loadMappings(target.organisationId(), "assessment_templates");
                    Map<String, Long> sectionMappings = loadMappings(target.organisationId(), "assessment_sections");
                    return upsertSections(batch, target, templateMappings, sectionMappings);
                }));

        Counts questionCounts = runBatched(
                "assessment_questions",
                questions,
                batch -> tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(), () -> {
                    Map<String, Long> sectionMappings = loadMappings(target.organisationId(), "assessment_sections");
                    Map<String, Long> questionMappings = loadMappings(target.organisationId(), "assessment_questions");
                    return upsertQuestions(batch, target, sectionMappings, questionMappings);
                }));

        Counts optionCounts = runBatched(
                "assessment_question_options",
                options,
                batch -> tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(), () -> {
                    Map<String, Long> questionMappings = loadMappings(target.organisationId(), "assessment_questions");
                    Map<String, Long> optionMappings =
                            loadMappings(target.organisationId(), "assessment_question_options");
                    return upsertOptions(batch, target, questionMappings, optionMappings);
                }));

        Counts assignmentCounts = runBatched(
                "assessment_assignments",
                assignments,
                batch -> tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(), () -> {
                    Map<String, Long> templateMappings = loadMappings(target.organisationId(), "assessment_templates");
                    Map<String, Long> assignmentMappings =
                            loadMappings(target.organisationId(), "assessment_assignments");
                    return upsertAssignments(
                            batch, target, clientMappings, userMappings, templateMappings,
                            assignmentMappings, defaultStaffUserId);
                }));

        Counts responseCounts = runBatched(
                "assessment_responses",
                responses,
                batch -> tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(), () -> {
                    Map<String, Long> assignmentMappings =
                            loadMappings(target.organisationId(), "assessment_assignments");
                    Map<String, Long> questionMappings = loadMappings(target.organisationId(), "assessment_questions");
                    Map<String, Long> optionMappings =
                            loadMappings(target.organisationId(), "assessment_question_options");
                    Map<String, Long> responseMappings = loadMappings(target.organisationId(), "assessment_responses");
                    return upsertResponses(
                            batch, target, userMappings, assignmentMappings, questionMappings,
                            optionMappings, responseMappings);
                }));

        Counts reportCounts = runBatched(
                "assessment_reports",
                reports,
                batch -> tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(), () -> {
                    Map<String, Long> assignmentMappings =
                            loadMappings(target.organisationId(), "assessment_assignments");
                    Map<String, Long> reportMappings = loadMappings(target.organisationId(), "assessment_reports");
                    return upsertReports(
                            batch, target, userMappings, assignmentMappings, reportMappings, defaultStaffUserId);
                }));

        return new AssessmentsExecuteResult(
                templates.size(), templateCounts.created, templateCounts.updated, templateCounts.mappedReruns,
                sections.size(), sectionCounts.created, sectionCounts.updated, sectionCounts.mappedReruns,
                questions.size(), questionCounts.created, questionCounts.updated, questionCounts.mappedReruns,
                options.size(), optionCounts.created, optionCounts.updated, optionCounts.mappedReruns,
                assignments.size(), assignmentCounts.created, assignmentCounts.updated, assignmentCounts.mappedReruns,
                responses.size(), responseCounts.created, responseCounts.updated, responseCounts.mappedReruns,
                responseCounts.skipped,
                reports.size(), reportCounts.created, reportCounts.updated, reportCounts.mappedReruns);
    }

    private <T> Counts runBatched(String stage, List<T> rows, java.util.function.Function<List<T>, Counts> worker) {
        Counts total = new Counts();
        if (rows.isEmpty()) {
            return total;
        }
        int batchNumber = 0;
        for (int start = 0; start < rows.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, rows.size());
            int currentBatch = ++batchNumber;
            List<T> batch = rows.subList(start, end);
            Counts batchCounts = runBatchWithRetry(stage, currentBatch, batch, worker);
            total.add(batchCounts);
            log.info("ClientHubAI assessments batch complete: stage={} batch={} batch_size={} processed={}/{}",
                    stage, currentBatch, batch.size(), end, rows.size());
        }
        return total;
    }

    private <T> Counts runBatchWithRetry(
            String stage,
            int batchNumber,
            List<T> batch,
            java.util.function.Function<List<T>, Counts> worker) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return worker.apply(batch);
            } catch (RuntimeException ex) {
                lastFailure = ex;
                if (attempt >= 3 || !isTransientDbFailure(ex)) {
                    throw ex;
                }
                log.warn("ClientHubAI assessments batch failed (stage={} batch={} attempt={}/3); retrying. cause={}",
                        stage, batchNumber, attempt, ex.getMessage());
                try {
                    Thread.sleep(3_000L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
        throw lastFailure;
    }

    private boolean isTransientDbFailure(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof org.springframework.dao.DataAccessResourceFailureException
                    || current instanceof org.springframework.dao.TransientDataAccessException
                    || current instanceof java.net.SocketException
                    || current instanceof java.net.SocketTimeoutException
                    || current instanceof java.io.IOException
                    || (current.getMessage() != null && (
                    current.getMessage().contains("I/O error")
                            || current.getMessage().contains("Connection is closed")
                            || current.getMessage().contains("This connection has been closed")))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Map<String, Long> loadMappingsOutsideTenant(Long organisationId, String entityName) {
        return loadMappings(organisationId, entityName);
    }

    private Counts upsertTemplates(
            List<SourceAssessmentTemplateRecord> templates,
            TargetInventory target,
            Map<String, Long> userMappings,
            Map<String, Long> templateMappings,
            Set<String> occupiedNameVersions,
            Long defaultStaffUserId) {
        if (defaultStaffUserId == null && !templates.isEmpty()) {
            throw new IllegalStateException("Cannot migrate assessment templates without at least one mapped staff user");
        }
        Counts counts = new Counts();
        for (SourceAssessmentTemplateRecord source : templates) {
            if (templateMappings.containsKey(source.legacyTemplatePk())) {
                counts.record(true, true);
                continue;
            }
            Long createdById = resolveUserOrDefault(userMappings, source.createdByLegacyId(), defaultStaffUserId);
            String name = source.name().trim();
            int versionNumber = parseVersionNumber(source.version());

            // Replace unmapped same-name seeds (e.g. default-tenant-templates.json)
            // instead of bumping version and leaving two active "Mental Health Assessment" cards.
            List<AssessmentTemplate> seedDuplicates = findUnmappedTemplatesWithName(name, templateMappings);
            for (AssessmentTemplate seed : seedDuplicates) {
                occupiedNameVersions.remove(nameVersionKey(seed.getName(), seed.getVersionNumber()));
            }

            while (occupiedNameVersions.contains(nameVersionKey(name, versionNumber))) {
                versionNumber++;
            }

            AssessmentTemplate template = new AssessmentTemplate();
            template.setName(name);
            template.setDescription(trim(source.description()));
            template.setCategory(trimTo(source.category(), 100));
            template.setIsStandardized(source.standardized());
            template.setIsActive(source.active());
            template.setCreatedByUser(userRepository.getReferenceById(createdById));
            template.setVersionNumber(versionNumber);
            template.setIsDeleted(false);
            template.setDeletedAt(null);
            template.setCreatedBy(0L);
            template.setUpdatedBy(0L);

            AssessmentTemplate saved = assessmentTemplateRepository.save(template);
            occupiedNameVersions.add(nameVersionKey(name, versionNumber));
            templateMappings.put(source.legacyTemplatePk(), saved.getId());
            upsertLegacyMapping(target, "assessment_templates", "assessment_templates",
                    source.legacyTemplatePk(), saved.getId(), templateChecksum(source));

            for (AssessmentTemplate seed : seedDuplicates) {
                reassignAssignmentsToTemplate(seed.getId(), saved.getId());
                softDeleteAssessmentTemplate(seed);
            }
            counts.record(false, false);
        }
        return counts;
    }

    /**
     * Soft-deletes active templates that share a name with a ClientHub-mapped template
     * but themselves have no legacy mapping (typically the seeded placeholder).
     */
    public int repairUnmappedSeedTemplateDuplicates(Map<String, Long> templateMappings) {
        if (templateMappings == null || templateMappings.isEmpty()) {
            return 0;
        }
        Set<Long> mappedIds = new HashSet<>(templateMappings.values());
        Map<String, List<AssessmentTemplate>> byName = new HashMap<>();
        for (AssessmentTemplate template : assessmentTemplateRepository.findAll()) {
            if (template.getName() == null || template.getName().isBlank()) {
                continue;
            }
            byName.computeIfAbsent(template.getName().trim(), ignored -> new ArrayList<>()).add(template);
        }

        int removed = 0;
        for (List<AssessmentTemplate> group : byName.values()) {
            if (group.size() < 2) {
                continue;
            }
            List<AssessmentTemplate> mapped = group.stream()
                    .filter(template -> mappedIds.contains(template.getId()))
                    .sorted(Comparator
                            .comparing((AssessmentTemplate t) -> t.getVersionNumber() == null ? 0 : t.getVersionNumber())
                            .reversed()
                            .thenComparing(AssessmentTemplate::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            if (mapped.isEmpty()) {
                continue;
            }
            Long keepId = mapped.get(0).getId();
            for (AssessmentTemplate template : group) {
                if (mappedIds.contains(template.getId())) {
                    continue;
                }
                reassignAssignmentsToTemplate(template.getId(), keepId);
                softDeleteAssessmentTemplate(template);
                removed++;
            }
        }
        return removed;
    }

    private List<AssessmentTemplate> findUnmappedTemplatesWithName(
            String name,
            Map<String, Long> templateMappings) {
        Set<Long> mappedIds = new HashSet<>(templateMappings.values());
        List<AssessmentTemplate> matches = new ArrayList<>();
        for (AssessmentTemplate template : assessmentTemplateRepository.findAll()) {
            if (template.getName() != null
                    && name.equals(template.getName().trim())
                    && !mappedIds.contains(template.getId())) {
                matches.add(template);
            }
        }
        return matches;
    }

    private void reassignAssignmentsToTemplate(Long fromTemplateId, Long toTemplateId) {
        if (fromTemplateId == null || toTemplateId == null || fromTemplateId.equals(toTemplateId)) {
            return;
        }
        AssessmentTemplate targetTemplate = assessmentTemplateRepository.getReferenceById(toTemplateId);
        for (AssessmentAssignment assignment : assessmentAssignmentRepository.findByTemplateId(fromTemplateId)) {
            assignment.setTemplate(targetTemplate);
            assignment.setUpdatedBy(0L);
            assessmentAssignmentRepository.save(assignment);
        }
    }

    private void softDeleteAssessmentTemplate(AssessmentTemplate template) {
        template.softDelete();
        template.setIsActive(false);
        template.setUpdatedBy(0L);
        assessmentTemplateRepository.save(template);
    }

    private Counts upsertSections(
            List<SourceAssessmentSectionRecord> sections,
            TargetInventory target,
            Map<String, Long> templateMappings,
            Map<String, Long> sectionMappings) {
        Counts counts = new Counts();
        for (SourceAssessmentSectionRecord source : sections) {
            if (sectionMappings.containsKey(source.legacySectionPk())) {
                counts.record(true, true);
                continue;
            }
            AssessmentSection section = new AssessmentSection();
            section.setTemplate(assessmentTemplateRepository.getReferenceById(
                    requiredMapping(templateMappings, source.templateLegacyId(), "assessment_templates")));
            section.setTitle(source.title().trim());
            section.setDescription(trim(source.description()));
            section.setAccessLevel(nonBlank(source.accessLevel(), "therapist_only"));
            if (section.getAccessLevel().length() > 20) {
                section.setAccessLevel(section.getAccessLevel().substring(0, 20));
            }
            section.setIsScoring(source.scoring());
            section.setReportMapping(trimTo(source.reportMapping(), 50));
            section.setAiReportPrompt(trim(source.aiReportPrompt()));
            section.setSortOrder(source.sortOrder() != null ? source.sortOrder() : 0);
            section.setIsDeleted(false);
            section.setDeletedAt(null);
            section.setCreatedBy(0L);
            section.setUpdatedBy(0L);

            AssessmentSection saved = assessmentSectionRepository.save(section);
            sectionMappings.put(source.legacySectionPk(), saved.getId());
            upsertLegacyMapping(target, "assessment_sections", "assessment_sections",
                    source.legacySectionPk(), saved.getId(), sectionChecksum(source));
            counts.record(false, false);
        }
        return counts;
    }

    private Counts upsertQuestions(
            List<SourceAssessmentQuestionRecord> questions,
            TargetInventory target,
            Map<String, Long> sectionMappings,
            Map<String, Long> questionMappings) {
        Counts counts = new Counts();
        for (SourceAssessmentQuestionRecord source : questions) {
            if (questionMappings.containsKey(source.legacyQuestionPk())) {
                counts.record(true, true);
                continue;
            }
            AssessmentQuestion question = new AssessmentQuestion();
            question.setSection(assessmentSectionRepository.getReferenceById(
                    requiredMapping(sectionMappings, source.sectionLegacyId(), "assessment_sections")));
            question.setQuestionText(source.questionText().trim());
            question.setQuestionType(trimTo(source.questionType().trim(), 50));
            question.setIsRequired(source.required());
            question.setSortOrder(source.sortOrder() != null ? source.sortOrder() : 0);
            question.setRatingMin(source.ratingMin());
            question.setRatingMax(source.ratingMax());
            question.setContributesToScore(source.contributesToScore());
            question.setIsDeleted(false);
            question.setDeletedAt(null);
            question.setCreatedBy(0L);
            question.setUpdatedBy(0L);

            List<String> labels = source.ratingLabels() == null ? List.of() : source.ratingLabels();
            for (int i = 0; i < labels.size(); i++) {
                String label = labels.get(i);
                if (label == null || label.isBlank()) {
                    continue;
                }
                AssessmentQuestionRatingLabel ratingLabel = AssessmentQuestionRatingLabel.builder()
                        .question(question)
                        .label(label.trim())
                        .score(i)
                        .active(true)
                        .build();
                ratingLabel.setCreatedBy(0L);
                ratingLabel.setUpdatedBy(0L);
                ratingLabel.setIsDeleted(false);
                question.getRatingLabels().add(ratingLabel);
            }

            AssessmentQuestion saved = assessmentQuestionRepository.save(question);
            questionMappings.put(source.legacyQuestionPk(), saved.getId());
            upsertLegacyMapping(target, "assessment_questions", "assessment_questions",
                    source.legacyQuestionPk(), saved.getId(), questionChecksum(source));
            counts.record(false, false);
        }
        return counts;
    }

    private Counts upsertOptions(
            List<SourceAssessmentQuestionOptionRecord> options,
            TargetInventory target,
            Map<String, Long> questionMappings,
            Map<String, Long> optionMappings) {
        Counts counts = new Counts();
        for (SourceAssessmentQuestionOptionRecord source : options) {
            if (optionMappings.containsKey(source.legacyOptionPk())) {
                counts.record(true, true);
                continue;
            }
            AssessmentQuestionOption option = new AssessmentQuestionOption();
            option.setQuestion(assessmentQuestionRepository.getReferenceById(
                    requiredMapping(questionMappings, source.questionLegacyId(), "assessment_questions")));
            option.setOptionKey("opt-" + source.legacyOptionPk());
            option.setOptionText(source.optionText().trim());
            option.setScoreValue(source.optionValue());
            option.setOptionValue(source.optionValue() == null ? null : source.optionValue().toPlainString());
            option.setSortOrder(source.sortOrder());
            option.setIsDefault(false);
            option.setIsDeleted(false);
            option.setDeletedAt(null);
            option.setCreatedBy(0L);
            option.setUpdatedBy(0L);

            AssessmentQuestionOption saved = assessmentQuestionOptionRepository.save(option);
            optionMappings.put(source.legacyOptionPk(), saved.getId());
            upsertLegacyMapping(target, "assessment_question_options", "assessment_question_options",
                    source.legacyOptionPk(), saved.getId(), optionChecksum(source));
            counts.record(false, false);
        }
        return counts;
    }

    private Counts upsertAssignments(
            List<SourceAssessmentAssignmentRecord> assignments,
            TargetInventory target,
            Map<String, Long> clientMappings,
            Map<String, Long> userMappings,
            Map<String, Long> templateMappings,
            Map<String, Long> assignmentMappings,
            Long defaultStaffUserId) {
        Counts counts = new Counts();
        for (SourceAssessmentAssignmentRecord source : assignments) {
            if (assignmentMappings.containsKey(source.legacyAssignmentPk())) {
                counts.record(true, true);
                continue;
            }
            Long assignedById = resolveUserOrDefault(userMappings, source.assignedByLegacyId(), defaultStaffUserId);
            if (assignedById == null) {
                throw new IllegalStateException(
                        "Cannot migrate assessment assignment " + source.legacyAssignmentPk()
                                + " without assignedBy mapping");
            }

            AssessmentAssignment assignment = new AssessmentAssignment();
            assignment.setTemplate(assessmentTemplateRepository.getReferenceById(
                    requiredMapping(templateMappings, source.templateLegacyId(), "assessment_templates")));
            assignment.setClient(clientRepository.getReferenceById(
                    requiredMapping(clientMappings, source.clientLegacyId(), "clients")));
            assignment.setAssignedBy(userRepository.getReferenceById(assignedById));
            assignment.setAssignedDate(source.createdAt() != null ? source.createdAt() : Instant.now());
            assignment.setStatus(nonBlank(source.status(), "pending"));
            if (assignment.getStatus().length() > 30) {
                assignment.setStatus(assignment.getStatus().substring(0, 30));
            }
            assignment.setDueDate(source.dueDate());
            assignment.setCompletedAt(source.completedAt());
            assignment.setFinalizedAt(source.finalizedAt());
            assignment.setClientSubmittedAt(source.clientSubmittedAt());
            assignment.setTherapistCompletedAt(source.therapistCompletedAt());
            assignment.setTotalScore(source.totalScore());
            assignment.setNotes(trim(source.notes()));
            assignment.setIsDeleted(false);
            assignment.setDeletedAt(null);
            assignment.setCreatedBy(0L);
            assignment.setUpdatedBy(0L);

            AssessmentAssignment saved = assessmentAssignmentRepository.save(assignment);
            assignmentMappings.put(source.legacyAssignmentPk(), saved.getId());
            upsertLegacyMapping(target, "assessment_assignments", "assessment_assignments",
                    source.legacyAssignmentPk(), saved.getId(), assignmentChecksum(source));
            counts.record(false, false);
        }
        return counts;
    }

    private Counts upsertResponses(
            List<SourceAssessmentResponseRecord> responses,
            TargetInventory target,
            Map<String, Long> userMappings,
            Map<String, Long> assignmentMappings,
            Map<String, Long> questionMappings,
            Map<String, Long> optionMappings,
            Map<String, Long> responseMappings) {
        Counts counts = new Counts();
        for (SourceAssessmentResponseRecord source : responses) {
            if (responseMappings.containsKey(source.legacyResponsePk())) {
                counts.record(true, true);
                continue;
            }
            Long assignmentId = requiredMapping(
                    assignmentMappings, source.assignmentLegacyId(), "assessment_assignments");
            Long questionId = requiredMapping(questionMappings, source.questionLegacyId(), "assessment_questions");
            AssessmentAssignment assignment = assessmentAssignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Missing assessment assignment target id " + assignmentId));

            Long responderUserId = null;
            Long responderClientId = null;
            String responderType;
            if (source.responderLegacyId() != null && !source.responderLegacyId().isBlank()
                    && userMappings.containsKey(source.responderLegacyId())) {
                responderType = "USER";
                responderUserId = userMappings.get(source.responderLegacyId());
            } else if (assignment.getClient() != null && assignment.getClient().getId() != null) {
                responderType = "CLIENT";
                responderClientId = assignment.getClient().getId();
            } else {
                counts.skipped++;
                log.warn("ClientHubAI assessments response skipped (unmapped responder): source_id={} responder_id={}",
                        source.legacyResponsePk(), source.responderLegacyId());
                continue;
            }

            AssessmentResponse response = new AssessmentResponse();
            response.setAssignment(assignment);
            response.setQuestion(assessmentQuestionRepository.getReferenceById(questionId));
            response.setResponderType(responderType);
            response.setResponderUserId(responderUserId);
            response.setResponderClientId(responderClientId);
            response.setResponseText(trim(source.responseText()));
            response.setResponseValue(trimTo(source.ratingValue(), 255));
            response.setScore(source.scoreValue());
            response.setAnsweredAt(source.createdAt());
            response.setIsDeleted(false);
            response.setDeletedAt(null);
            response.setCreatedBy(0L);
            response.setUpdatedBy(0L);

            List<String> selected = source.selectedOptionLegacyIds() == null
                    ? List.of()
                    : source.selectedOptionLegacyIds();
            for (String optionLegacyId : selected) {
                Long optionId = optionMappings.get(optionLegacyId);
                if (optionId == null) {
                    continue;
                }
                AssessmentQuestionOption optionRef =
                        assessmentQuestionOptionRepository.getReferenceById(optionId);
                AssessmentResponseOption selectedOption = AssessmentResponseOption.builder()
                        .response(response)
                        .option(optionRef)
                        .score(optionRef.getScoreValue())
                        .build();
                selectedOption.setCreatedBy(0L);
                selectedOption.setUpdatedBy(0L);
                selectedOption.setIsDeleted(false);
                response.getSelectedOptions().add(selectedOption);
            }

            AssessmentResponse saved = assessmentResponseRepository.save(response);
            responseMappings.put(source.legacyResponsePk(), saved.getId());
            upsertLegacyMapping(target, "assessment_responses", "assessment_responses",
                    source.legacyResponsePk(), saved.getId(), responseChecksum(source));
            counts.record(false, false);
        }
        return counts;
    }

    private Counts upsertReports(
            List<SourceAssessmentReportRecord> reports,
            TargetInventory target,
            Map<String, Long> userMappings,
            Map<String, Long> assignmentMappings,
            Map<String, Long> reportMappings,
            Long defaultStaffUserId) {
        if (defaultStaffUserId == null && !reports.isEmpty()) {
            throw new IllegalStateException("Cannot migrate assessment reports without at least one mapped staff user");
        }
        Counts counts = new Counts();
        for (SourceAssessmentReportRecord source : reports) {
            if (reportMappings.containsKey(source.legacyReportPk())) {
                counts.record(true, true);
                continue;
            }
            Long createdById = resolveUserOrDefault(userMappings, source.createdByLegacyId(), defaultStaffUserId);
            AssessmentReport report = new AssessmentReport();
            report.setAssignment(assessmentAssignmentRepository.getReferenceById(
                    requiredMapping(assignmentMappings, source.assignmentLegacyId(), "assessment_assignments")));
            report.setGeneratedContent(trim(source.generatedContent()));
            report.setDraftContent(trim(source.draftContent()));
            report.setFinalContent(trim(source.finalContent()));
            report.setReportData(trim(source.reportData()));
            report.setTemplateSnapshot(null);
            report.setIsDraft(source.draft());
            report.setIsFinalized(source.finalized());
            report.setGeneratedAt(source.generatedAt());
            report.setEditedAt(source.editedAt());
            report.setFinalizedAt(source.finalizedAt());
            report.setExportedAt(source.exportedAt());
            report.setCreatedByUser(userRepository.getReferenceById(createdById));
            if (source.finalizedByLegacyId() != null && !source.finalizedByLegacyId().isBlank()
                    && userMappings.containsKey(source.finalizedByLegacyId())) {
                report.setFinalizedByUser(userRepository.getReferenceById(
                        userMappings.get(source.finalizedByLegacyId())));
            }
            report.setIsDeleted(false);
            report.setDeletedAt(null);
            report.setCreatedBy(0L);
            report.setUpdatedBy(0L);

            AssessmentReport saved = assessmentReportRepository.save(report);
            reportMappings.put(source.legacyReportPk(), saved.getId());
            upsertLegacyMapping(target, "assessment_reports", "assessment_reports",
                    source.legacyReportPk(), saved.getId(), reportChecksum(source));
            counts.record(false, false);
        }
        return counts;
    }

    static int parseVersionNumber(String version) {
        if (version == null || version.isBlank()) {
            return 1;
        }
        Matcher matcher = LEADING_INTEGER.matcher(version);
        if (!matcher.find()) {
            return 1;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    private Set<String> loadOccupiedTemplateNameVersions() {
        Set<String> occupied = new HashSet<>();
        for (AssessmentTemplate template : assessmentTemplateRepository.findAll()) {
            if (template.getName() != null && template.getVersionNumber() != null) {
                occupied.add(nameVersionKey(template.getName(), template.getVersionNumber()));
            }
        }
        return occupied;
    }

    private static String nameVersionKey(String name, int versionNumber) {
        return name + "\u0000" + versionNumber;
    }

    private Long resolveUserOrDefault(Map<String, Long> userMappings, String legacyUserId, Long defaultStaffUserId) {
        if (legacyUserId != null && !legacyUserId.isBlank() && userMappings.containsKey(legacyUserId)) {
            return userMappings.get(legacyUserId);
        }
        return defaultStaffUserId;
    }

    private Long resolveLowestMappedStaffUserId(Map<String, Long> userMappings) {
        return userMappings.entrySet().stream()
                .min(Comparator.comparingLong(entry -> {
                    try {
                        return Long.parseLong(entry.getKey());
                    } catch (NumberFormatException ex) {
                        return Long.MAX_VALUE;
                    }
                }))
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    private Map<String, Long> loadMappings(Long organisationId, String entityName) {
        Map<String, Long> mappings = new HashMap<>();
        jdbcTemplate.query("""
                SELECT source_id, target_id
                FROM public.clienthub_legacy_id_mappings
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                  AND entity_name = ?
                """,
                (RowCallbackHandler) rs -> mappings.put(rs.getString("source_id"), rs.getLong("target_id")),
                organisationId,
                entityName);
        return mappings;
    }

    private void upsertLegacyMapping(
            TargetInventory target,
            String entityName,
            String targetTable,
            String sourceId,
            Long targetId,
            String checksum) {
        jdbcTemplate.update("""
                INSERT INTO public.clienthub_legacy_id_mappings (
                    organisation_id, source_system, entity_name, source_id,
                    target_schema, target_table, target_id, source_checksum_sha256,
                    first_seen_run_id, last_seen_run_id
                )
                VALUES (?, 'ClientHubAI', ?, ?, ?, ?, ?, ?, NULL, NULL)
                ON CONFLICT (organisation_id, source_system, entity_name, source_id)
                DO UPDATE SET
                    target_schema = EXCLUDED.target_schema,
                    target_table = EXCLUDED.target_table,
                    target_id = EXCLUDED.target_id,
                    source_checksum_sha256 = EXCLUDED.source_checksum_sha256,
                    updated_at = CURRENT_TIMESTAMP
                """,
                target.organisationId(),
                entityName,
                sourceId,
                target.schemaName(),
                targetTable,
                targetId,
                checksum);
    }

    private Long requiredMapping(Map<String, Long> mappings, String sourceId, String entityName) {
        Long targetId = mappings.get(sourceId);
        if (targetId == null) {
            throw new IllegalStateException("Missing ClientHubAI " + entityName + " mapping for source id " + sourceId);
        }
        return targetId;
    }

    private String templateChecksum(SourceAssessmentTemplateRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyTemplatePk(),
                nullToEmpty(source.name()),
                nullToEmpty(source.version()),
                Boolean.toString(source.active()),
                Boolean.toString(source.standardized())));
    }

    private String sectionChecksum(SourceAssessmentSectionRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacySectionPk(),
                source.templateLegacyId(),
                nullToEmpty(source.title()),
                String.valueOf(source.sortOrder())));
    }

    private String questionChecksum(SourceAssessmentQuestionRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyQuestionPk(),
                source.sectionLegacyId(),
                nullToEmpty(source.questionText()),
                nullToEmpty(source.questionType()),
                String.valueOf(source.sortOrder())));
    }

    private String optionChecksum(SourceAssessmentQuestionOptionRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyOptionPk(),
                source.questionLegacyId(),
                nullToEmpty(source.optionText()),
                source.optionValue() == null ? "" : source.optionValue().toPlainString(),
                String.valueOf(source.sortOrder())));
    }

    private String assignmentChecksum(SourceAssessmentAssignmentRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyAssignmentPk(),
                source.templateLegacyId(),
                source.clientLegacyId(),
                nullToEmpty(source.status()),
                String.valueOf(source.createdAt())));
    }

    private String responseChecksum(SourceAssessmentResponseRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyResponsePk(),
                source.assignmentLegacyId(),
                source.questionLegacyId(),
                nullToEmpty(source.responderLegacyId()),
                nullToEmpty(source.ratingValue())));
    }

    private String reportChecksum(SourceAssessmentReportRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyReportPk(),
                source.assignmentLegacyId(),
                Boolean.toString(source.draft()),
                Boolean.toString(source.finalized())));
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String trimTo(String value, int maxLength) {
        String trimmed = trim(value);
        if (trimmed == null) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private static String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class Counts {
        int created;
        int updated;
        int mappedReruns;
        int skipped;

        void record(boolean existing, boolean mappedRerun) {
            if (mappedRerun) {
                mappedReruns++;
            } else if (existing) {
                updated++;
            } else {
                created++;
            }
        }

        void add(Counts other) {
            created += other.created;
            updated += other.updated;
            mappedReruns += other.mappedReruns;
            skipped += other.skipped;
        }
    }

    record AssessmentsExecuteResult(
            int sourceTemplates,
            int templatesCreated,
            int templatesUpdated,
            int templateMappedReruns,
            int sourceSections,
            int sectionsCreated,
            int sectionsUpdated,
            int sectionMappedReruns,
            int sourceQuestions,
            int questionsCreated,
            int questionsUpdated,
            int questionMappedReruns,
            int sourceOptions,
            int optionsCreated,
            int optionsUpdated,
            int optionMappedReruns,
            int sourceAssignments,
            int assignmentsCreated,
            int assignmentsUpdated,
            int assignmentMappedReruns,
            int sourceResponses,
            int responsesCreated,
            int responsesUpdated,
            int responseMappedReruns,
            int responsesSkipped,
            int sourceReports,
            int reportsCreated,
            int reportsUpdated,
            int reportMappedReruns) {
    }
}
