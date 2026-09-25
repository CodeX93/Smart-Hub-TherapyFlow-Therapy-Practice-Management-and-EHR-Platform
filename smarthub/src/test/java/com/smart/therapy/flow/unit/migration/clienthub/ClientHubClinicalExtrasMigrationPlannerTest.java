package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.client.enums.ConsentType;
import com.smart.therapy.flow.migration.clienthub.ClientHubClinicalExtrasExecuteService;
import com.smart.therapy.flow.migration.clienthub.ClientHubClinicalExtrasMigrationPlanner.ClinicalExtrasMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.ClinicalExtrasTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceChecklistItemRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceChecklistTemplateRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientChecklistItemRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientChecklistRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientPortalRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClinicalExtrasInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourcePatientConsentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSupervisorAssignmentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceUserZoomIntegrationRecord;
import com.smart.therapy.flow.task.enums.CheckListCategory;
import com.smart.therapy.flow.user.dto.RequiredMeetingFrequency;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHubClinicalExtrasMigrationPlannerTest {

    private final ClientHubClinicalExtrasMigrationPlanner planner = new ClientHubClinicalExtrasMigrationPlanner();

    @Test
    void countsCreatesAndMappedUpdates() {
        ClinicalExtrasMigrationPlan plan = planner.buildPlan(
                emptyInventory(),
                List.of(consent("1", "10"), consent("2", "11")),
                List.of(supervisor("1", "20", "21")),
                List.of(portal("10", "a@example.com", true), portal("11", "b@example.com", false)),
                List.of(template("1", "Intake"), template("2", "Discharge")),
                List.of(item("1", "1"), item("2", "2")),
                List.of(clientChecklist("1", "10", "1")),
                List.of(clientChecklistItem("1", "1", "1")),
                List.of(zoom("20"), zoom("21")),
                new ClinicalExtrasTargetState(
                        Set.of("10", "11"),
                        Set.of("20", "21"),
                        Set.of("2"),
                        Set.of(),
                        Set.of("11"),
                        Set.of("2"),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of("21"),
                        Set.of("Intake")));

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.consentsWouldCreate()).isEqualTo(1);
        assertThat(plan.consentsWouldUpdateMapped()).isEqualTo(1);
        assertThat(plan.supervisorsWouldCreate()).isEqualTo(1);
        assertThat(plan.portalsWouldCreate()).isEqualTo(1);
        assertThat(plan.portalsWouldUpdateMapped()).isEqualTo(1);
        assertThat(plan.portalsPasswordResetRequired()).isEqualTo(1);
        assertThat(plan.templatesWouldUpdateExistingName()).isEqualTo(1);
        assertThat(plan.templatesWouldUpdateMapped()).isEqualTo(1);
        assertThat(plan.checklistItemsWouldCreate()).isEqualTo(2);
        assertThat(plan.clientChecklistsWouldCreate()).isEqualTo(1);
        assertThat(plan.clientChecklistItemsWouldCreate()).isEqualTo(1);
        assertThat(plan.zoomWouldCreate()).isEqualTo(1);
        assertThat(plan.zoomWouldUpdateMapped()).isEqualTo(1);
    }

    @Test
    void missingPortalEmailIsWarningNotBlocker() {
        ClinicalExtrasMigrationPlan plan = planner.buildPlan(
                new SourceClinicalExtrasInventory(
                        0, 0, 0, 0, 0, 0, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                List.of(),
                List.of(),
                List.of(
                        portal("10", "a@example.com", true),
                        new SourceClientPortalRecord(
                                "11", "No Email", null, true, null, null, false, null, null, Instant.now())),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new ClinicalExtrasTargetState(
                        Set.of("10", "11"),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of()));

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.portalsWouldCreate()).isEqualTo(1);
        assertThat(plan.portalsBlocked()).isEqualTo(1);
        assertThat(plan.warnings()).anyMatch(warning -> warning.contains("missing portal_email"));
        assertThat(plan.blockers()).isEmpty();
    }

    @Test
    void blocksUnmappedDependenciesAndMissingStaffForConsents() {
        ClinicalExtrasMigrationPlan plan = planner.buildPlan(
                new SourceClinicalExtrasInventory(
                        1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1),
                List.of(consent("1", "10")),
                List.of(supervisor("1", "missing-sup", "20")),
                List.of(portal("missing-client", "a@example.com", true)),
                List.of(),
                List.of(item("1", "missing-template")),
                List.of(clientChecklist("1", "10", "missing-template")),
                List.of(clientChecklistItem("1", "missing-checklist", "missing-item")),
                List.of(zoom("missing-user")),
                new ClinicalExtrasTargetState(
                        Set.of("10"),
                        Set.of(),
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
        assertThat(plan.consentsBlocked()).isEqualTo(1);
        assertThat(plan.supervisorsBlocked()).isEqualTo(1);
        assertThat(plan.portalsBlocked()).isEqualTo(1);
        assertThat(plan.checklistItemsBlocked()).isEqualTo(1);
        assertThat(plan.clientChecklistsBlocked()).isEqualTo(1);
        assertThat(plan.clientChecklistItemsBlocked()).isEqualTo(1);
        assertThat(plan.zoomBlocked()).isEqualTo(1);
        assertThat(plan.blockers())
                .anyMatch(blocker -> blocker.contains("no staff users"))
                .anyMatch(blocker -> blocker.contains("Portal rows reference unmapped"))
                .anyMatch(blocker -> blocker.contains("Zoom integrations"));
    }

    @Test
    void mapsConsentTypesFrequenciesAndCategories() {
        assertThat(ClientHubClinicalExtrasExecuteService.mapConsentType("ai_processing"))
                .isEqualTo(ConsentType.AI_PROCESSING);
        assertThat(ClientHubClinicalExtrasExecuteService.mapConsentType("hipaa_privacy"))
                .isEqualTo(ConsentType.HIPAA_PRIVACY);
        assertThat(ClientHubClinicalExtrasExecuteService.mapConsentType("unknown_thing"))
                .isEqualTo(ConsentType.OTHER);
        assertThat(ClientHubClinicalExtrasExecuteService.mapMeetingFrequency("bi-weekly"))
                .isEqualTo(RequiredMeetingFrequency.BIWEEKLY);
        assertThat(ClientHubClinicalExtrasExecuteService.mapMeetingFrequency("weekly"))
                .isEqualTo(RequiredMeetingFrequency.WEEKLY);
        assertThat(ClientHubClinicalExtrasExecuteService.mapChecklistCategory("discharge"))
                .isEqualTo(CheckListCategory.DISCHARGE);
        assertThat(ClientHubClinicalExtrasExecuteService.mapChecklistCategory("weird"))
                .isEqualTo(CheckListCategory.INTAKE);
    }

    private SourceClinicalExtrasInventory emptyInventory() {
        return new SourceClinicalExtrasInventory(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private SourcePatientConsentRecord consent(String id, String clientId) {
        return new SourcePatientConsentRecord(
                id, clientId, "treatment", "1.0", true, Instant.parse("2024-01-01T00:00:00Z"),
                null, null, null, null, Instant.now(), Instant.now());
    }

    private SourceSupervisorAssignmentRecord supervisor(String id, String supervisorId, String therapistId) {
        return new SourceSupervisorAssignmentRecord(
                id, supervisorId, therapistId, Instant.parse("2024-01-01T00:00:00Z"), true, null,
                "weekly", null, null, Instant.now(), Instant.now());
    }

    private SourceClientPortalRecord portal(String clientId, String email, boolean bcrypt) {
        return new SourceClientPortalRecord(
                clientId, "Client " + clientId, email, true, email,
                bcrypt ? "$2a$10$abcdefghijklmnopqrstuv" : "plain",
                bcrypt, null, null, Instant.now());
    }

    private SourceChecklistTemplateRecord template(String id, String name) {
        return new SourceChecklistTemplateRecord(
                id, name, null, null, true, 0, Instant.now(), Instant.now());
    }

    private SourceChecklistItemRecord item(String id, String templateId) {
        return new SourceChecklistItemRecord(
                id, templateId, "Item " + id, null, "intake", false, 1, 0, Instant.now());
    }

    private SourceClientChecklistRecord clientChecklist(String id, String clientId, String templateId) {
        return new SourceClientChecklistRecord(
                id, clientId, templateId, false, null, null, null, LocalDate.now(), Instant.now());
    }

    private SourceClientChecklistItemRecord clientChecklistItem(String id, String checklistId, String itemId) {
        return new SourceClientChecklistItemRecord(
                id, checklistId, itemId, false, null, null, null, Instant.now());
    }

    private SourceUserZoomIntegrationRecord zoom(String userId) {
        return new SourceUserZoomIntegrationRecord(
                userId, "acct-" + userId, "client-" + userId, "secret", "token",
                Instant.now().plusSeconds(3600), Instant.now());
    }
}
