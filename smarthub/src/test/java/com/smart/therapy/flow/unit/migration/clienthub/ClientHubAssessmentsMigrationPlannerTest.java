package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubAssessmentsExecuteService;
import com.smart.therapy.flow.migration.clienthub.ClientHubAssessmentsMigrationPlanner.AssessmentsMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.AssessmentTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentAssignmentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentQuestionOptionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentQuestionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentReportRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentResponseRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentSectionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentTemplateRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentsInventory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHubAssessmentsMigrationPlannerTest {

    private final ClientHubAssessmentsMigrationPlanner planner = new ClientHubAssessmentsMigrationPlanner();

    @Test
    void countsCreatesAndMappedUpdates() {
        AssessmentsMigrationPlan plan = planner.buildPlan(
                emptyInventory(),
                List.of(template("1", "PHQ-9"), template("2", "GAD-7")),
                List.of(section("1", "1"), section("2", "2")),
                List.of(question("1", "1"), question("2", "2")),
                List.of(option("1", "1"), option("2", "2")),
                List.of(assignment("1", "1", "10", "20"), assignment("2", "2", "11", "21")),
                List.of(response("1", "1", "1", "20"), response("2", "2", "2", "missing-user")),
                List.of(report("1", "1", "20"), report("2", "2", "21")),
                new AssessmentTargetState(
                        Set.of("10", "11"),
                        Set.of("20", "21"),
                        Set.of("2"),
                        Set.of("2"),
                        Set.of("2"),
                        Set.of("2"),
                        Set.of("2"),
                        Set.of("2"),
                        Set.of("2"),
                        Set.of()));

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.templatesWouldCreate()).isEqualTo(1);
        assertThat(plan.templatesWouldUpdateMapped()).isEqualTo(1);
        assertThat(plan.sectionsWouldCreate()).isEqualTo(1);
        assertThat(plan.sectionsWouldUpdateMapped()).isEqualTo(1);
        assertThat(plan.questionsWouldCreate()).isEqualTo(1);
        assertThat(plan.optionsWouldCreate()).isEqualTo(1);
        assertThat(plan.assignmentsWouldCreate()).isEqualTo(1);
        assertThat(plan.assignmentsWouldUpdateMapped()).isEqualTo(1);
        assertThat(plan.responsesWouldCreate()).isEqualTo(1);
        assertThat(plan.responsesWouldUpdateMapped()).isEqualTo(1);
        assertThat(plan.responsesResponderFallbackOrSkip()).isEqualTo(1);
        assertThat(plan.reportsWouldCreate()).isEqualTo(1);
        assertThat(plan.reportsWouldUpdateMapped()).isEqualTo(1);
        assertThat(plan.warnings()).anyMatch(warning -> warning.contains("CLIENT responder fallback"));
    }

    @Test
    void blocksUnmappedDependenciesAndMissingStaff() {
        AssessmentsMigrationPlan plan = planner.buildPlan(
                new SourceAssessmentsInventory(
                        1, 1, 1, 1, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0),
                List.of(template("1", "PHQ-9")),
                List.of(section("1", "missing-template")),
                List.of(question("1", "missing-section")),
                List.of(option("1", "missing-question")),
                List.of(assignment("1", "missing-template", "10", null)),
                List.of(response("1", "missing-assignment", "missing-question", "20")),
                List.of(report("1", "missing-assignment", null)),
                new AssessmentTargetState(
                        Set.of("10"),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of()));

        assertThat(plan.blocked()).isTrue();
        assertThat(plan.templatesBlocked()).isEqualTo(1);
        assertThat(plan.sectionsBlocked()).isEqualTo(1);
        assertThat(plan.questionsBlocked()).isEqualTo(1);
        assertThat(plan.optionsBlocked()).isEqualTo(1);
        assertThat(plan.assignmentsBlocked()).isEqualTo(1);
        assertThat(plan.responsesBlocked()).isEqualTo(1);
        assertThat(plan.reportsBlocked()).isEqualTo(1);
        assertThat(plan.blockers())
                .anyMatch(blocker -> blocker.contains("no staff users"))
                .anyMatch(blocker -> blocker.contains("unmapped templates"))
                .anyMatch(blocker -> blocker.contains("unmapped clients or templates"));
    }

    @Test
    void parsesLeadingIntegerVersion() {
        assertThat(ClientHubAssessmentsExecuteService.parseVersionNumber("3")).isEqualTo(3);
        assertThat(ClientHubAssessmentsExecuteService.parseVersionNumber("v2.1")).isEqualTo(1);
        assertThat(ClientHubAssessmentsExecuteService.parseVersionNumber("12-beta")).isEqualTo(12);
        assertThat(ClientHubAssessmentsExecuteService.parseVersionNumber(null)).isEqualTo(1);
        assertThat(ClientHubAssessmentsExecuteService.parseVersionNumber("")).isEqualTo(1);
    }

    private SourceAssessmentsInventory emptyInventory() {
        return new SourceAssessmentsInventory(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private SourceAssessmentTemplateRecord template(String id, String name) {
        return new SourceAssessmentTemplateRecord(
                id, name, null, "clinical", false, true, "20", "1",
                Instant.now(), Instant.now());
    }

    private SourceAssessmentSectionRecord section(String id, String templateId) {
        return new SourceAssessmentSectionRecord(
                id, templateId, "Section " + id, null, "therapist_only", false, null, null, 0,
                Instant.now(), Instant.now());
    }

    private SourceAssessmentQuestionRecord question(String id, String sectionId) {
        return new SourceAssessmentQuestionRecord(
                id, sectionId, "Question " + id, "rating", true, 0, 1, 5,
                List.of("Poor", "Good"), true, Instant.now(), Instant.now());
    }

    private SourceAssessmentQuestionOptionRecord option(String id, String questionId) {
        return new SourceAssessmentQuestionOptionRecord(
                id, questionId, "Option " + id, BigDecimal.ONE, 0);
    }

    private SourceAssessmentAssignmentRecord assignment(
            String id, String templateId, String clientId, String assignedBy) {
        return new SourceAssessmentAssignmentRecord(
                id, templateId, clientId, assignedBy, "pending", LocalDate.now(),
                null, null, null, null, null, null, Instant.now(), Instant.now());
    }

    private SourceAssessmentResponseRecord response(
            String id, String assignmentId, String questionId, String responderId) {
        return new SourceAssessmentResponseRecord(
                id, assignmentId, questionId, responderId, null, List.of(), "3",
                BigDecimal.valueOf(3), Instant.now(), Instant.now());
    }

    private SourceAssessmentReportRecord report(String id, String assignmentId, String createdBy) {
        return new SourceAssessmentReportRecord(
                id, assignmentId, "gen", "draft", null, null, true, false,
                Instant.now(), null, null, null, createdBy, null, Instant.now(), Instant.now());
    }
}
