package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.ServiceTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceServiceInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceServiceRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClientHubServiceMigrationPlanner {

    ServiceMigrationPlan buildPlan(
            SourceServiceInventory inventory,
            List<SourceServiceRecord> sourceServices,
            ServiceTargetState targetState) {
        int wouldCreate = 0;
        int wouldUpdateExistingCode = 0;
        int wouldUpdateMapped = 0;
        int blockedRows = 0;

        for (SourceServiceRecord source : sourceServices) {
            if (source.missingRequiredFields()) {
                blockedRows++;
                continue;
            }
            if (targetState.mappedServiceIds().contains(source.legacyServicePk())) {
                wouldUpdateMapped++;
            } else if (targetState.existingServiceCodes().contains(source.serviceCode())) {
                wouldUpdateExistingCode++;
            } else {
                wouldCreate++;
            }
        }

        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        addBlocker(blockers, inventory.blankServiceCodeRows(), "Services with blank service_code");
        addBlocker(blockers, inventory.blankServiceNameRows(), "Services with blank service_name");
        addBlocker(blockers, inventory.missingDurationRows(), "Services missing duration");
        addBlocker(blockers, inventory.missingBaseRateRows(), "Services missing base_rate");
        if (inventory.duplicateServiceCodeGroups() > 0) {
            warnings.add("Duplicate source service_code groups; execute will keep first match and "
                    + "disambiguate later duplicates: " + inventory.duplicateServiceCodeGroups());
        }

        return new ServiceMigrationPlan(
                sourceServices.size(),
                wouldCreate,
                wouldUpdateExistingCode,
                wouldUpdateMapped,
                blockedRows,
                blockers,
                warnings);
    }

    private void addBlocker(List<String> blockers, long count, String label) {
        if (count > 0) {
            blockers.add(label + ": " + count);
        }
    }

    record ServiceMigrationPlan(
            int sourceServices,
            int wouldCreate,
            int wouldUpdateExistingCode,
            int wouldUpdateMapped,
            int blockedRows,
            List<String> blockers,
            List<String> warnings) {

        boolean blocked() {
            return blockedRows > 0 || !blockers.isEmpty();
        }
    }
}
