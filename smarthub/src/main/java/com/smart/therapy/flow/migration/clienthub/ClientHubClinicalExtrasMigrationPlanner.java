package com.smart.therapy.flow.migration.clienthub;

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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ClientHubClinicalExtrasMigrationPlanner {

    ClinicalExtrasMigrationPlan buildPlan(
            SourceClinicalExtrasInventory inventory,
            List<SourcePatientConsentRecord> consents,
            List<SourceSupervisorAssignmentRecord> supervisors,
            List<SourceClientPortalRecord> portals,
            List<SourceChecklistTemplateRecord> templates,
            List<SourceChecklistItemRecord> checklistItems,
            List<SourceClientChecklistRecord> clientChecklists,
            List<SourceClientChecklistItemRecord> clientChecklistItems,
            List<SourceUserZoomIntegrationRecord> zoomIntegrations,
            ClinicalExtrasTargetState targetState) {

        boolean hasMappedStaff = !targetState.mappedUserIds().isEmpty();

        int consentsWouldCreate = 0;
        int consentsWouldUpdateMapped = 0;
        int consentsBlocked = 0;
        int consentsUnmappedClient = 0;
        int consentsNoStaff = 0;
        for (SourcePatientConsentRecord source : consents) {
            if (source.missingRequiredFields()) {
                consentsBlocked++;
                continue;
            }
            if (!targetState.mappedClientIds().contains(source.clientLegacyId())) {
                consentsBlocked++;
                consentsUnmappedClient++;
                continue;
            }
            if (!hasMappedStaff) {
                consentsBlocked++;
                consentsNoStaff++;
                continue;
            }
            if (targetState.mappedConsentIds().contains(source.legacyConsentPk())) {
                consentsWouldUpdateMapped++;
            } else {
                consentsWouldCreate++;
            }
        }

        int supervisorsWouldCreate = 0;
        int supervisorsWouldUpdateMapped = 0;
        int supervisorsBlocked = 0;
        int supervisorsUnmapped = 0;
        for (SourceSupervisorAssignmentRecord source : supervisors) {
            if (source.missingRequiredFields()) {
                supervisorsBlocked++;
                continue;
            }
            if (!targetState.mappedUserIds().contains(source.supervisorLegacyId())
                    || !targetState.mappedUserIds().contains(source.therapistLegacyId())) {
                supervisorsBlocked++;
                supervisorsUnmapped++;
                continue;
            }
            if (targetState.mappedSupervisorAssignmentIds().contains(source.legacyAssignmentPk())) {
                supervisorsWouldUpdateMapped++;
            } else {
                supervisorsWouldCreate++;
            }
        }

        int portalsWouldCreate = 0;
        int portalsWouldUpdateMapped = 0;
        int portalsBlocked = 0;
        int portalsUnmappedClient = 0;
        int portalsPasswordResetRequired = 0;
        for (SourceClientPortalRecord source : portals) {
            if (source.missingRequiredFields()) {
                portalsBlocked++;
                continue;
            }
            if (!targetState.mappedClientIds().contains(source.legacyClientPk())) {
                portalsBlocked++;
                portalsUnmappedClient++;
                continue;
            }
            if (!source.bcryptCompatiblePassword()) {
                portalsPasswordResetRequired++;
            }
            if (targetState.mappedClientPortalIds().contains(source.legacyClientPk())) {
                portalsWouldUpdateMapped++;
            } else {
                portalsWouldCreate++;
            }
        }

        int templatesWouldCreate = 0;
        int templatesWouldUpdateMapped = 0;
        int templatesWouldUpdateExistingName = 0;
        int templatesBlocked = 0;
        for (SourceChecklistTemplateRecord source : templates) {
            if (source.missingRequiredFields()) {
                templatesBlocked++;
                continue;
            }
            if (targetState.mappedChecklistTemplateIds().contains(source.legacyTemplatePk())) {
                templatesWouldUpdateMapped++;
            } else if (targetState.existingChecklistTemplateNames().contains(source.name())) {
                templatesWouldUpdateExistingName++;
            } else {
                templatesWouldCreate++;
            }
        }

        Set<String> projectedTemplateIds = new HashSet<>(targetState.mappedChecklistTemplateIds());
        for (SourceChecklistTemplateRecord source : templates) {
            if (!source.missingRequiredFields()) {
                projectedTemplateIds.add(source.legacyTemplatePk());
            }
        }

        int checklistItemsWouldCreate = 0;
        int checklistItemsWouldUpdateMapped = 0;
        int checklistItemsBlocked = 0;
        int checklistItemsUnmappedTemplate = 0;
        for (SourceChecklistItemRecord source : checklistItems) {
            if (source.missingRequiredFields()) {
                checklistItemsBlocked++;
                continue;
            }
            if (!projectedTemplateIds.contains(source.templateLegacyId())) {
                checklistItemsBlocked++;
                checklistItemsUnmappedTemplate++;
                continue;
            }
            if (targetState.mappedChecklistItemIds().contains(source.legacyItemPk())) {
                checklistItemsWouldUpdateMapped++;
            } else {
                checklistItemsWouldCreate++;
            }
        }

        Set<String> projectedItemIds = new HashSet<>(targetState.mappedChecklistItemIds());
        for (SourceChecklistItemRecord source : checklistItems) {
            if (!source.missingRequiredFields() && projectedTemplateIds.contains(source.templateLegacyId())) {
                projectedItemIds.add(source.legacyItemPk());
            }
        }

        int clientChecklistsWouldCreate = 0;
        int clientChecklistsWouldUpdateMapped = 0;
        int clientChecklistsBlocked = 0;
        int clientChecklistsUnmapped = 0;
        for (SourceClientChecklistRecord source : clientChecklists) {
            if (source.missingRequiredFields()) {
                clientChecklistsBlocked++;
                continue;
            }
            if (!targetState.mappedClientIds().contains(source.clientLegacyId())
                    || !projectedTemplateIds.contains(source.templateLegacyId())) {
                clientChecklistsBlocked++;
                clientChecklistsUnmapped++;
                continue;
            }
            if (targetState.mappedClientChecklistIds().contains(source.legacyChecklistPk())) {
                clientChecklistsWouldUpdateMapped++;
            } else {
                clientChecklistsWouldCreate++;
            }
        }

        Set<String> projectedClientChecklistIds = new HashSet<>(targetState.mappedClientChecklistIds());
        for (SourceClientChecklistRecord source : clientChecklists) {
            if (!source.missingRequiredFields()
                    && targetState.mappedClientIds().contains(source.clientLegacyId())
                    && projectedTemplateIds.contains(source.templateLegacyId())) {
                projectedClientChecklistIds.add(source.legacyChecklistPk());
            }
        }

        int clientChecklistItemsWouldCreate = 0;
        int clientChecklistItemsWouldUpdateMapped = 0;
        int clientChecklistItemsBlocked = 0;
        int clientChecklistItemsUnmapped = 0;
        for (SourceClientChecklistItemRecord source : clientChecklistItems) {
            if (source.missingRequiredFields()) {
                clientChecklistItemsBlocked++;
                continue;
            }
            if (!projectedClientChecklistIds.contains(source.clientChecklistLegacyId())
                    || !projectedItemIds.contains(source.checklistItemLegacyId())) {
                clientChecklistItemsBlocked++;
                clientChecklistItemsUnmapped++;
                continue;
            }
            if (targetState.mappedClientChecklistItemIds().contains(source.legacyChecklistItemPk())) {
                clientChecklistItemsWouldUpdateMapped++;
            } else {
                clientChecklistItemsWouldCreate++;
            }
        }

        int zoomWouldCreate = 0;
        int zoomWouldUpdateMapped = 0;
        int zoomBlocked = 0;
        int zoomUnmappedUser = 0;
        for (SourceUserZoomIntegrationRecord source : zoomIntegrations) {
            if (source.missingRequiredFields()) {
                zoomBlocked++;
                continue;
            }
            if (!targetState.mappedUserIds().contains(source.userLegacyId())) {
                zoomBlocked++;
                zoomUnmappedUser++;
                continue;
            }
            if (targetState.mappedZoomIntegrationIds().contains(source.userLegacyId())) {
                zoomWouldUpdateMapped++;
            } else {
                zoomWouldCreate++;
            }
        }

        List<String> blockers = new ArrayList<>();
        addBlocker(blockers, inventory.blankConsentClientRows(), "Consents missing client_id");
        addBlocker(blockers, inventory.blankConsentTypeRows(), "Consents missing consent_type");
        addBlocker(blockers, consentsUnmappedClient, "Consents reference unmapped clients");
        addBlocker(blockers, consentsNoStaff, "Consents blocked because no staff users are mapped");
        addBlocker(blockers, inventory.blankSupervisorIdRows(), "Supervisor assignments missing supervisor_id");
        addBlocker(blockers, inventory.blankTherapistIdRows(), "Supervisor assignments missing therapist_id");
        addBlocker(blockers, supervisorsUnmapped, "Supervisor assignments reference unmapped users");
        addBlocker(blockers, portalsUnmappedClient, "Portal rows reference unmapped clients");
        addBlocker(blockers, inventory.blankChecklistTemplateNameRows(), "Checklist templates missing name");
        addBlocker(blockers, inventory.blankChecklistItemTemplateRows(), "Checklist items missing template_id");
        addBlocker(blockers, inventory.blankChecklistItemTitleRows(), "Checklist items missing title");
        addBlocker(blockers, inventory.blankChecklistItemCategoryRows(), "Checklist items missing category");
        addBlocker(blockers, checklistItemsUnmappedTemplate, "Checklist items reference unmapped templates");
        addBlocker(blockers, inventory.blankClientChecklistClientRows(), "Client checklists missing client_id");
        addBlocker(blockers, inventory.blankClientChecklistTemplateRows(), "Client checklists missing template_id");
        addBlocker(blockers, clientChecklistsUnmapped, "Client checklists reference unmapped clients or templates");
        addBlocker(blockers, inventory.blankClientChecklistItemChecklistRows(),
                "Client checklist items missing client_checklist_id");
        addBlocker(blockers, inventory.blankClientChecklistItemItemRows(),
                "Client checklist items missing checklist_item_id");
        addBlocker(blockers, clientChecklistItemsUnmapped,
                "Client checklist items reference unmapped checklists or items");
        addBlocker(blockers, zoomUnmappedUser, "Zoom integrations reference unmapped users");

        List<String> warnings = new ArrayList<>();
        addWarning(warnings, inventory.portalMissingEmailRows(),
                "Portal rows missing portal_email and email (skipped)");
        addWarning(warnings, portalsPasswordResetRequired, "Portal passwords require reset (non-bcrypt)");

        return new ClinicalExtrasMigrationPlan(
                consents.size(),
                consentsWouldCreate,
                consentsWouldUpdateMapped,
                consentsBlocked,
                supervisors.size(),
                supervisorsWouldCreate,
                supervisorsWouldUpdateMapped,
                supervisorsBlocked,
                portals.size(),
                portalsWouldCreate,
                portalsWouldUpdateMapped,
                portalsBlocked,
                portalsPasswordResetRequired,
                templates.size(),
                templatesWouldCreate,
                templatesWouldUpdateMapped,
                templatesWouldUpdateExistingName,
                templatesBlocked,
                checklistItems.size(),
                checklistItemsWouldCreate,
                checklistItemsWouldUpdateMapped,
                checklistItemsBlocked,
                clientChecklists.size(),
                clientChecklistsWouldCreate,
                clientChecklistsWouldUpdateMapped,
                clientChecklistsBlocked,
                clientChecklistItems.size(),
                clientChecklistItemsWouldCreate,
                clientChecklistItemsWouldUpdateMapped,
                clientChecklistItemsBlocked,
                zoomIntegrations.size(),
                zoomWouldCreate,
                zoomWouldUpdateMapped,
                zoomBlocked,
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

    record ClinicalExtrasMigrationPlan(
            int sourceConsents,
            int consentsWouldCreate,
            int consentsWouldUpdateMapped,
            int consentsBlocked,
            int sourceSupervisors,
            int supervisorsWouldCreate,
            int supervisorsWouldUpdateMapped,
            int supervisorsBlocked,
            int sourcePortals,
            int portalsWouldCreate,
            int portalsWouldUpdateMapped,
            int portalsBlocked,
            int portalsPasswordResetRequired,
            int sourceChecklistTemplates,
            int templatesWouldCreate,
            int templatesWouldUpdateMapped,
            int templatesWouldUpdateExistingName,
            int templatesBlocked,
            int sourceChecklistItems,
            int checklistItemsWouldCreate,
            int checklistItemsWouldUpdateMapped,
            int checklistItemsBlocked,
            int sourceClientChecklists,
            int clientChecklistsWouldCreate,
            int clientChecklistsWouldUpdateMapped,
            int clientChecklistsBlocked,
            int sourceClientChecklistItems,
            int clientChecklistItemsWouldCreate,
            int clientChecklistItemsWouldUpdateMapped,
            int clientChecklistItemsBlocked,
            int sourceZoomIntegrations,
            int zoomWouldCreate,
            int zoomWouldUpdateMapped,
            int zoomBlocked,
            List<String> blockers,
            List<String> warnings) {

        boolean blocked() {
            return consentsBlocked > 0
                    || supervisorsBlocked > 0
                    // Portal rows missing email are skipped on execute and only warned; hard portal
                    // failures (unmapped clients) are represented in blockers.
                    || templatesBlocked > 0
                    || checklistItemsBlocked > 0
                    || clientChecklistsBlocked > 0
                    || clientChecklistItemsBlocked > 0
                    || zoomBlocked > 0
                    || !blockers.isEmpty();
        }
    }
}
