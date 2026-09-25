package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubStaffAuthMigrationPlanner.StaffAuthMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceStaffAuthInventory;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHubStaffAuthMigrationPlannerTest {

    private final ClientHubStaffAuthMigrationPlanner planner = new ClientHubStaffAuthMigrationPlanner();

    @Test
    void mapsKnownSourceRolesToActiveTargetRoles() {
        SourceStaffAuthInventory source = source(Map.of("admin", 1L, "therapist", 3L, "accountant", 2L),
                6, 0, 0, 0, 0, 0);

        StaffAuthMigrationPlan plan = planner.buildPlan(source, Set.of("ADMIN", "THERAPIST", "BILLING_SPECIALIST"));

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.roleMappings())
                .extracting(mapping -> mapping.sourceRole() + "->" + mapping.targetRole())
                .containsExactlyInAnyOrder(
                        "admin->ADMIN",
                        "therapist->THERAPIST",
                        "accountant->BILLING_SPECIALIST");
    }

    @Test
    void blocksUnknownAndUnavailableRoles() {
        SourceStaffAuthInventory source = source(Map.of("owner", 1L, "billing", 2L), 3, 0, 0, 0, 0, 0);

        StaffAuthMigrationPlan plan = planner.buildPlan(source, Set.of("ADMIN", "THERAPIST"));

        assertThat(plan.blocked()).isTrue();
        assertThat(plan.blockers())
                .contains("Unmapped source role: owner")
                .contains("Target role is missing or inactive: BILLING_SPECIALIST");
    }

    @Test
    void blocksDuplicateOrBlankLoginIdentifiers() {
        SourceStaffAuthInventory source = source(Map.of("therapist", 4L), 4, 0, 1, 2, 3, 4);

        StaffAuthMigrationPlan plan = planner.buildPlan(source, Set.of("THERAPIST"));

        assertThat(plan.blocked()).isTrue();
        assertThat(plan.blockers())
                .contains("Users with blank email: 1")
                .contains("Users with blank username: 2")
                .contains("Duplicate source email groups: 3")
                .contains("Duplicate source username groups: 4");
    }

    @Test
    void warnsWhenPasswordHashesNeedResetFlow() {
        SourceStaffAuthInventory source = source(Map.of("therapist", 4L), 2, 2, 0, 0, 0, 0);

        StaffAuthMigrationPlan plan = planner.buildPlan(source, Set.of("THERAPIST"));

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.warnings())
                .contains("Users requiring activation/reset flow because password hash is not bcrypt-compatible: 2");
    }

    private SourceStaffAuthInventory source(
            Map<String, Long> roleCounts,
            long bcryptRows,
            long unsupportedRows,
            long blankEmailRows,
            long blankUsernameRows,
            long duplicateEmailGroups,
            long duplicateUsernameGroups) {
        return new SourceStaffAuthInventory(
                roleCounts.values().stream().mapToLong(Long::longValue).sum(),
                roleCounts,
                Map.of("active", roleCounts.values().stream().mapToLong(Long::longValue).sum()),
                bcryptRows,
                unsupportedRows,
                blankEmailRows,
                blankUsernameRows,
                duplicateEmailGroups,
                duplicateUsernameGroups);
    }
}
