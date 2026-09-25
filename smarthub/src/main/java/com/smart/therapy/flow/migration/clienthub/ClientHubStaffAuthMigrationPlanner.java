package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceStaffAuthInventory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ClientHubStaffAuthMigrationPlanner {

    StaffAuthMigrationPlan buildPlan(SourceStaffAuthInventory source, Set<String> activePlatformRoles) {
        List<RoleMappingDecision> roleMappings = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (Map.Entry<String, Long> entry : source.roleCounts().entrySet()) {
            String sourceRole = entry.getKey();
            String targetRole = mapRole(sourceRole);
            if (targetRole == null) {
                blockers.add("Unmapped source role: " + printableRole(sourceRole));
                roleMappings.add(new RoleMappingDecision(printableRole(sourceRole), null, entry.getValue(), true));
                continue;
            }
            if (!activePlatformRoles.contains(targetRole)) {
                blockers.add("Target role is missing or inactive: " + targetRole);
                roleMappings.add(new RoleMappingDecision(printableRole(sourceRole), targetRole, entry.getValue(), true));
                continue;
            }
            roleMappings.add(new RoleMappingDecision(printableRole(sourceRole), targetRole, entry.getValue(), false));
        }

        if (source.blankEmailRows() > 0) {
            blockers.add("Users with blank email: " + source.blankEmailRows());
        }
        if (source.blankUsernameRows() > 0) {
            blockers.add("Users with blank username: " + source.blankUsernameRows());
        }
        if (source.duplicateEmailGroups() > 0) {
            blockers.add("Duplicate source email groups: " + source.duplicateEmailGroups());
        }
        if (source.duplicateUsernameGroups() > 0) {
            blockers.add("Duplicate source username groups: " + source.duplicateUsernameGroups());
        }
        if (source.unsupportedPasswordRows() > 0) {
            warnings.add("Users requiring activation/reset flow because password hash is not bcrypt-compatible: "
                    + source.unsupportedPasswordRows());
        }

        return new StaffAuthMigrationPlan(
                source.userRows(),
                source.bcryptCompatiblePasswordRows(),
                source.unsupportedPasswordRows(),
                source.statusCounts(),
                roleMappings,
                blockers,
                warnings);
    }

    String mapRole(String sourceRole) {
        if (sourceRole == null || sourceRole.isBlank()) {
            return null;
        }
        String normalized = sourceRole.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "admin", "administrator", "practice_admin" -> "ADMIN";
            case "therapist", "clinician" -> "THERAPIST";
            case "supervisor", "clinical_supervisor" -> "SUPERVISOR";
            case "accountant", "billing", "billing_specialist", "billingspecialist" -> "BILLING_SPECIALIST";
            case "receptionist", "front_desk", "frontdesk" -> "RECEPTIONIST";
            case "client", "patient" -> "CLIENT";
            default -> null;
        };
    }

    private String printableRole(String sourceRole) {
        return sourceRole == null || sourceRole.isBlank() ? "<blank>" : sourceRole;
    }

    record StaffAuthMigrationPlan(
            long userRows,
            long bcryptCompatiblePasswordRows,
            long unsupportedPasswordRows,
            Map<String, Long> statusCounts,
            List<RoleMappingDecision> roleMappings,
            List<String> blockers,
            List<String> warnings) {

        boolean blocked() {
            return !blockers.isEmpty() || roleMappings.stream().anyMatch(RoleMappingDecision::blocked);
        }
    }

    record RoleMappingDecision(String sourceRole, String targetRole, long rows, boolean blocked) {
    }
}
