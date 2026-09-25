package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.AssessmentTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentAssignmentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentQuestionOptionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentQuestionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentReportRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentResponseRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentSectionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentTemplateRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentsInventory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ClientHubAssessmentsMigrationPlanner {

    AssessmentsMigrationPlan buildPlan(
            SourceAssessmentsInventory inventory,
            List<SourceAssessmentTemplateRecord> templates,
            List<SourceAssessmentSectionRecord> sections,
            List<SourceAssessmentQuestionRecord> questions,
            List<SourceAssessmentQuestionOptionRecord> options,
            List<SourceAssessmentAssignmentRecord> assignments,
            List<SourceAssessmentResponseRecord> responses,
            List<SourceAssessmentReportRecord> reports,
            AssessmentTargetState targetState) {

        boolean hasMappedStaff = !targetState.mappedUserIds().isEmpty();

        int templatesWouldCreate = 0;
        int templatesWouldUpdateMapped = 0;
        int templatesBlocked = 0;
        int templatesNoStaff = 0;
        for (SourceAssessmentTemplateRecord source : templates) {
            if (source.missingRequiredFields()) {
                templatesBlocked++;
                continue;
            }
            if (!hasMappedStaff) {
                templatesBlocked++;
                templatesNoStaff++;
                continue;
            }
            if (targetState.mappedTemplateIds().contains(source.legacyTemplatePk())) {
                templatesWouldUpdateMapped++;
            } else {
                templatesWouldCreate++;
            }
        }

        Set<String> projectedTemplateIds = new HashSet<>(targetState.mappedTemplateIds());
        if (hasMappedStaff) {
            for (SourceAssessmentTemplateRecord source : templates) {
                if (!source.missingRequiredFields()) {
                    projectedTemplateIds.add(source.legacyTemplatePk());
                }
            }
        }

        int sectionsWouldCreate = 0;
        int sectionsWouldUpdateMapped = 0;
        int sectionsBlocked = 0;
        int sectionsUnmappedTemplate = 0;
        for (SourceAssessmentSectionRecord source : sections) {
            if (source.missingRequiredFields()) {
                sectionsBlocked++;
                continue;
            }
            if (!projectedTemplateIds.contains(source.templateLegacyId())) {
                sectionsBlocked++;
                sectionsUnmappedTemplate++;
                continue;
            }
            if (targetState.mappedSectionIds().contains(source.legacySectionPk())) {
                sectionsWouldUpdateMapped++;
            } else {
                sectionsWouldCreate++;
            }
        }

        Set<String> projectedSectionIds = new HashSet<>(targetState.mappedSectionIds());
        for (SourceAssessmentSectionRecord source : sections) {
            if (!source.missingRequiredFields() && projectedTemplateIds.contains(source.templateLegacyId())) {
                projectedSectionIds.add(source.legacySectionPk());
            }
        }

        int questionsWouldCreate = 0;
        int questionsWouldUpdateMapped = 0;
        int questionsBlocked = 0;
        int questionsUnmappedSection = 0;
        for (SourceAssessmentQuestionRecord source : questions) {
            if (source.missingRequiredFields()) {
                questionsBlocked++;
                continue;
            }
            if (!projectedSectionIds.contains(source.sectionLegacyId())) {
                questionsBlocked++;
                questionsUnmappedSection++;
                continue;
            }
            if (targetState.mappedQuestionIds().contains(source.legacyQuestionPk())) {
                questionsWouldUpdateMapped++;
            } else {
                questionsWouldCreate++;
            }
        }

        Set<String> projectedQuestionIds = new HashSet<>(targetState.mappedQuestionIds());
        for (SourceAssessmentQuestionRecord source : questions) {
            if (!source.missingRequiredFields() && projectedSectionIds.contains(source.sectionLegacyId())) {
                projectedQuestionIds.add(source.legacyQuestionPk());
            }
        }

        int optionsWouldCreate = 0;
        int optionsWouldUpdateMapped = 0;
        int optionsBlocked = 0;
        int optionsUnmappedQuestion = 0;
        for (SourceAssessmentQuestionOptionRecord source : options) {
            if (source.missingRequiredFields()) {
                optionsBlocked++;
                continue;
            }
            if (!projectedQuestionIds.contains(source.questionLegacyId())) {
                optionsBlocked++;
                optionsUnmappedQuestion++;
                continue;
            }
            if (targetState.mappedOptionIds().contains(source.legacyOptionPk())) {
                optionsWouldUpdateMapped++;
            } else {
                optionsWouldCreate++;
            }
        }

        Set<String> projectedOptionIds = new HashSet<>(targetState.mappedOptionIds());
        for (SourceAssessmentQuestionOptionRecord source : options) {
            if (!source.missingRequiredFields() && projectedQuestionIds.contains(source.questionLegacyId())) {
                projectedOptionIds.add(source.legacyOptionPk());
            }
        }

        int assignmentsWouldCreate = 0;
        int assignmentsWouldUpdateMapped = 0;
        int assignmentsBlocked = 0;
        int assignmentsUnmapped = 0;
        int assignmentsNoStaff = 0;
        for (SourceAssessmentAssignmentRecord source : assignments) {
            if (source.missingRequiredFields()) {
                assignmentsBlocked++;
                continue;
            }
            boolean clientMapped = targetState.mappedClientIds().contains(source.clientLegacyId());
            boolean templateMapped = projectedTemplateIds.contains(source.templateLegacyId());
            boolean assignedByMapped = source.assignedByLegacyId() != null
                    && !source.assignedByLegacyId().isBlank()
                    && targetState.mappedUserIds().contains(source.assignedByLegacyId());
            if (!clientMapped || !templateMapped) {
                assignmentsBlocked++;
                assignmentsUnmapped++;
                continue;
            }
            if (!assignedByMapped && !hasMappedStaff) {
                assignmentsBlocked++;
                assignmentsNoStaff++;
                continue;
            }
            if (targetState.mappedAssignmentIds().contains(source.legacyAssignmentPk())) {
                assignmentsWouldUpdateMapped++;
            } else {
                assignmentsWouldCreate++;
            }
        }

        Set<String> projectedAssignmentIds = new HashSet<>(targetState.mappedAssignmentIds());
        for (SourceAssessmentAssignmentRecord source : assignments) {
            if (source.missingRequiredFields()) {
                continue;
            }
            boolean clientMapped = targetState.mappedClientIds().contains(source.clientLegacyId());
            boolean templateMapped = projectedTemplateIds.contains(source.templateLegacyId());
            boolean assignedByMapped = source.assignedByLegacyId() != null
                    && !source.assignedByLegacyId().isBlank()
                    && targetState.mappedUserIds().contains(source.assignedByLegacyId());
            if (clientMapped && templateMapped && (assignedByMapped || hasMappedStaff)) {
                projectedAssignmentIds.add(source.legacyAssignmentPk());
            }
        }

        int responsesWouldCreate = 0;
        int responsesWouldUpdateMapped = 0;
        int responsesBlocked = 0;
        int responsesUnmapped = 0;
        int responsesResponderFallbackOrSkip = 0;
        for (SourceAssessmentResponseRecord source : responses) {
            if (source.missingRequiredFields()) {
                responsesBlocked++;
                continue;
            }
            if (!projectedAssignmentIds.contains(source.assignmentLegacyId())
                    || !projectedQuestionIds.contains(source.questionLegacyId())) {
                responsesBlocked++;
                responsesUnmapped++;
                continue;
            }
            boolean responderAsUser = source.responderLegacyId() != null
                    && !source.responderLegacyId().isBlank()
                    && targetState.mappedUserIds().contains(source.responderLegacyId());
            // Unmapped responders use CLIENT fallback at execute (assignment client) or are skipped.
            if (!responderAsUser) {
                responsesResponderFallbackOrSkip++;
            }
            if (targetState.mappedResponseIds().contains(source.legacyResponsePk())) {
                responsesWouldUpdateMapped++;
            } else {
                responsesWouldCreate++;
            }
        }

        int reportsWouldCreate = 0;
        int reportsWouldUpdateMapped = 0;
        int reportsBlocked = 0;
        int reportsUnmapped = 0;
        int reportsNoStaff = 0;
        for (SourceAssessmentReportRecord source : reports) {
            if (source.missingRequiredFields()) {
                reportsBlocked++;
                continue;
            }
            if (!projectedAssignmentIds.contains(source.assignmentLegacyId())) {
                reportsBlocked++;
                reportsUnmapped++;
                continue;
            }
            boolean createdByMapped = source.createdByLegacyId() != null
                    && !source.createdByLegacyId().isBlank()
                    && targetState.mappedUserIds().contains(source.createdByLegacyId());
            if (!createdByMapped && !hasMappedStaff) {
                reportsBlocked++;
                reportsNoStaff++;
                continue;
            }
            if (targetState.mappedReportIds().contains(source.legacyReportPk())) {
                reportsWouldUpdateMapped++;
            } else {
                reportsWouldCreate++;
            }
        }

        List<String> blockers = new ArrayList<>();
        addBlocker(blockers, inventory.blankTemplateNameRows(), "Assessment templates missing name");
        addBlocker(blockers, templatesNoStaff, "Assessment templates blocked because no staff users are mapped");
        addBlocker(blockers, inventory.blankSectionTemplateRows(), "Assessment sections missing template_id");
        addBlocker(blockers, inventory.blankSectionTitleRows(), "Assessment sections missing title");
        addBlocker(blockers, sectionsUnmappedTemplate, "Assessment sections reference unmapped templates");
        addBlocker(blockers, inventory.blankQuestionSectionRows(), "Assessment questions missing section_id");
        addBlocker(blockers, inventory.blankQuestionTextRows(), "Assessment questions missing question_text");
        addBlocker(blockers, inventory.blankQuestionTypeRows(), "Assessment questions missing question_type");
        addBlocker(blockers, questionsUnmappedSection, "Assessment questions reference unmapped sections");
        addBlocker(blockers, inventory.blankOptionQuestionRows(), "Assessment question options missing question_id");
        addBlocker(blockers, inventory.blankOptionTextRows(), "Assessment question options missing option_text");
        addBlocker(blockers, optionsUnmappedQuestion, "Assessment question options reference unmapped questions");
        addBlocker(blockers, inventory.blankAssignmentTemplateRows(), "Assessment assignments missing template_id");
        addBlocker(blockers, inventory.blankAssignmentClientRows(), "Assessment assignments missing client_id");
        addBlocker(blockers, assignmentsUnmapped, "Assessment assignments reference unmapped clients or templates");
        addBlocker(blockers, assignmentsNoStaff, "Assessment assignments blocked because no staff users are mapped");
        addBlocker(blockers, inventory.blankResponseAssignmentRows(), "Assessment responses missing assignment_id");
        addBlocker(blockers, inventory.blankResponseQuestionRows(), "Assessment responses missing question_id");
        addBlocker(blockers, responsesUnmapped, "Assessment responses reference unmapped assignments or questions");
        addBlocker(blockers, inventory.blankReportAssignmentRows(), "Assessment reports missing assignment_id");
        addBlocker(blockers, reportsUnmapped, "Assessment reports reference unmapped assignments");
        addBlocker(blockers, reportsNoStaff, "Assessment reports blocked because no staff users are mapped");

        List<String> warnings = new ArrayList<>();
        addWarning(warnings, responsesResponderFallbackOrSkip,
                "Assessment responses may use CLIENT responder fallback or be skipped (unmapped responder_id)");

        return new AssessmentsMigrationPlan(
                templates.size(),
                templatesWouldCreate,
                templatesWouldUpdateMapped,
                templatesBlocked,
                sections.size(),
                sectionsWouldCreate,
                sectionsWouldUpdateMapped,
                sectionsBlocked,
                questions.size(),
                questionsWouldCreate,
                questionsWouldUpdateMapped,
                questionsBlocked,
                options.size(),
                optionsWouldCreate,
                optionsWouldUpdateMapped,
                optionsBlocked,
                assignments.size(),
                assignmentsWouldCreate,
                assignmentsWouldUpdateMapped,
                assignmentsBlocked,
                responses.size(),
                responsesWouldCreate,
                responsesWouldUpdateMapped,
                responsesBlocked,
                responsesResponderFallbackOrSkip,
                reports.size(),
                reportsWouldCreate,
                reportsWouldUpdateMapped,
                reportsBlocked,
                blockers,
                warnings);
    }

    private void addBlocker(List<String> blockers, long count, String label) {
        if (count > 0) {
            blockers.add(label + ": " + count);
        }
    }

    private void addWarning(List<String> warnings, long count, String label) {
        if (count > 0) {
            warnings.add(label + ": " + count);
        }
    }

    record AssessmentsMigrationPlan(
            int sourceTemplates,
            int templatesWouldCreate,
            int templatesWouldUpdateMapped,
            int templatesBlocked,
            int sourceSections,
            int sectionsWouldCreate,
            int sectionsWouldUpdateMapped,
            int sectionsBlocked,
            int sourceQuestions,
            int questionsWouldCreate,
            int questionsWouldUpdateMapped,
            int questionsBlocked,
            int sourceOptions,
            int optionsWouldCreate,
            int optionsWouldUpdateMapped,
            int optionsBlocked,
            int sourceAssignments,
            int assignmentsWouldCreate,
            int assignmentsWouldUpdateMapped,
            int assignmentsBlocked,
            int sourceResponses,
            int responsesWouldCreate,
            int responsesWouldUpdateMapped,
            int responsesBlocked,
            int responsesResponderFallbackOrSkip,
            int sourceReports,
            int reportsWouldCreate,
            int reportsWouldUpdateMapped,
            int reportsBlocked,
            List<String> blockers,
            List<String> warnings) {

        boolean blocked() {
            return !blockers.isEmpty();
        }
    }
}
