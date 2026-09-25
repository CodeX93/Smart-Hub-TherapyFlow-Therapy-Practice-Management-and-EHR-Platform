package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.ClientTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientRef;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClientHubClientMigrationPlanner {

    ClientMigrationPlan buildPlan(
            SourceClientInventory inventory,
            List<SourceClientRef> sourceClients,
            ClientTargetState targetState) {
        int wouldCreate = 0;
        int wouldUpdateMapped = 0;
        int blockedRows = 0;
        int missingTherapistMappings = 0;

        for (SourceClientRef sourceClient : sourceClients) {
            if (sourceClient.missingClientId() || sourceClient.missingFullName()) {
                blockedRows++;
                continue;
            }
            if (sourceClient.assignedTherapistLegacyId() != null
                    && !targetState.mappedUserIds().contains(sourceClient.assignedTherapistLegacyId())) {
                blockedRows++;
                missingTherapistMappings++;
                continue;
            }
            if (targetState.mappedClientIds().contains(sourceClient.legacyClientPk())) {
                wouldUpdateMapped++;
            } else {
                wouldCreate++;
            }
        }

        List<String> blockers = new ArrayList<>();
        if (inventory.blankClientIdRows() > 0) {
            blockers.add("Clients with blank client_id: " + inventory.blankClientIdRows());
        }
        if (inventory.blankFullNameRows() > 0) {
            blockers.add("Clients with blank full_name: " + inventory.blankFullNameRows());
        }
        if (inventory.duplicateClientIdGroups() > 0) {
            blockers.add("Duplicate source client_id groups: " + inventory.duplicateClientIdGroups());
        }
        if (missingTherapistMappings > 0) {
            blockers.add("Clients assigned to unmigrated therapists: " + missingTherapistMappings);
        }

        List<String> warnings = new ArrayList<>();
        if (inventory.incompleteInsuranceRows() > 0) {
            warnings.add("Clients with partial insurance data requiring review: " + inventory.incompleteInsuranceRows());
        }
        if (targetState.tenantClientRows() > 0 && targetState.mappedClientIds().isEmpty()) {
            warnings.add("Target tenant already has clients but no ClientHubAI client mappings; execute will attempt blind-index client_id matching before creating");
        }

        return new ClientMigrationPlan(
                sourceClients.size(),
                wouldCreate,
                wouldUpdateMapped,
                blockedRows,
                inventory.emailContactRows(),
                inventory.phoneContactRows(),
                inventory.emergencyContactRows(),
                inventory.addressRows(),
                inventory.completeInsuranceRows(),
                inventory.incompleteInsuranceRows(),
                inventory.referralRows(),
                inventory.employmentRows(),
                blockers,
                warnings);
    }

    record ClientMigrationPlan(
            int sourceClients,
            int wouldCreate,
            int wouldUpdateMapped,
            int blockedRows,
            long emailContactRows,
            long phoneContactRows,
            long emergencyContactRows,
            long addressRows,
            long completeInsuranceRows,
            long incompleteInsuranceRows,
            long referralRows,
            long employmentRows,
            List<String> blockers,
            List<String> warnings) {

        boolean blocked() {
            return blockedRows > 0 || !blockers.isEmpty();
        }
    }
}
