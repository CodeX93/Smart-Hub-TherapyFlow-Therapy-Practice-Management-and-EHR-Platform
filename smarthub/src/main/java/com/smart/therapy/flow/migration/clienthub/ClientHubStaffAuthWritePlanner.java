package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceStaffUserRef;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.StaffAuthTargetState;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class ClientHubStaffAuthWritePlanner {

    private final ClientHubStaffAuthMigrationPlanner staffAuthMigrationPlanner;

    public ClientHubStaffAuthWritePlanner(ClientHubStaffAuthMigrationPlanner staffAuthMigrationPlanner) {
        this.staffAuthMigrationPlanner = staffAuthMigrationPlanner;
    }

    StaffAuthWritePlan buildPlan(
            List<SourceStaffUserRef> sourceUsers,
            StaffAuthTargetState targetState,
            Set<String> activePlatformRoles) {
        int wouldCreate = 0;
        int wouldUpdateExisting = 0;
        int wouldUpdateMapped = 0;
        int blocked = 0;
        int passwordResetRequired = 0;

        for (SourceStaffUserRef sourceUser : sourceUsers) {
            String targetRole = staffAuthMigrationPlanner.mapRole(sourceUser.sourceRole());
            String targetUsername = ClientHubStaffAuthOverrides.normalisedUsername(sourceUser);
            if (sourceUser.normalisedEmail().isBlank()
                    || targetUsername.isBlank()
                    || targetRole == null
                    || !activePlatformRoles.contains(targetRole)) {
                blocked++;
                continue;
            }

            if (!sourceUser.bcryptCompatiblePassword()) {
                passwordResetRequired++;
            }

            if (targetState.mappedLegacyIds().contains(sourceUser.legacyUserId())) {
                wouldUpdateMapped++;
                continue;
            }

            if (targetState.authNormalisedEmails().contains(sourceUser.normalisedEmail())
                    || targetState.authNormalisedUsernames().contains(targetUsername)
                    || targetState.tenantUserEmails().contains(sourceUser.normalisedEmail())) {
                wouldUpdateExisting++;
                continue;
            }

            wouldCreate++;
        }

        return new StaffAuthWritePlan(
                sourceUsers.size(),
                wouldCreate,
                wouldUpdateExisting,
                wouldUpdateMapped,
                blocked,
                passwordResetRequired);
    }

    record StaffAuthWritePlan(
            int sourceUsers,
            int wouldCreate,
            int wouldUpdateExisting,
            int wouldUpdateMapped,
            int blocked,
            int passwordResetRequired) {

        int actionableRows() {
            return wouldCreate + wouldUpdateExisting + wouldUpdateMapped;
        }
    }
}
