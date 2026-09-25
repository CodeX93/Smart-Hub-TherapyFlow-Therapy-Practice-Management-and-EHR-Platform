package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubStaffAuthWritePlanner.StaffAuthWritePlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceStaffUserRef;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.StaffAuthTargetState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHubStaffAuthWritePlannerTest {

    private final ClientHubStaffAuthWritePlanner planner =
            new ClientHubStaffAuthWritePlanner(new ClientHubStaffAuthMigrationPlanner());

    @Test
    void countsCreateUpdateAndMappedRowsWithoutExposingIdentifiers() {
        StaffAuthTargetState targetState = new StaffAuthTargetState(
                Set.of("3"),
                Set.of("existing@example.com"),
                Set.of("existinguser"),
                Set.of("tenantuser@example.com"));

        StaffAuthWritePlan plan = planner.buildPlan(List.of(
                user("1", "newuser", "new@example.com", "therapist", true),
                user("2", "existinguser", "other@example.com", "therapist", true),
                user("3", "mappeduser", "mapped@example.com", "admin", true),
                user("4", "tenantuser", "tenantuser@example.com", "therapist", false)
        ), targetState, Set.of("ADMIN", "THERAPIST"));

        assertThat(plan.sourceUsers()).isEqualTo(4);
        assertThat(plan.wouldCreate()).isEqualTo(1);
        assertThat(plan.wouldUpdateExisting()).isEqualTo(2);
        assertThat(plan.wouldUpdateMapped()).isEqualTo(1);
        assertThat(plan.blocked()).isZero();
        assertThat(plan.passwordResetRequired()).isEqualTo(1);
        assertThat(plan.actionableRows()).isEqualTo(4);
    }

    @Test
    void blocksRowsWithMissingIdentifiersOrUnsupportedRoles() {
        StaffAuthWritePlan plan = planner.buildPlan(List.of(
                user("1", "", "new@example.com", "therapist", true),
                user("2", "user2", "", "therapist", true),
                user("3", "user3", "user3@example.com", "owner", true),
                user("4", "user4", "user4@example.com", "billing", true)
        ), new StaffAuthTargetState(Set.of(), Set.of(), Set.of(), Set.of()), Set.of("THERAPIST"));

        assertThat(plan.wouldCreate()).isZero();
        assertThat(plan.blocked()).isEqualTo(4);
        assertThat(plan.actionableRows()).isZero();
    }

    @Test
    void appliesApprovedUsernameOverrideBeforeTargetConflictChecks() {
        StaffAuthTargetState targetState = new StaffAuthTargetState(
                Set.of(),
                Set.of(),
                Set.of("amjed.abojedi"),
                Set.of());

        StaffAuthWritePlan plan = planner.buildPlan(List.of(
                user("50", "amjed.abojedi", "supervisor@example.com", "supervisor", true)
        ), targetState, Set.of("SUPERVISOR"));

        assertThat(plan.wouldCreate()).isEqualTo(1);
        assertThat(plan.wouldUpdateExisting()).isZero();
        assertThat(plan.blocked()).isZero();
    }

    private SourceStaffUserRef user(
            String legacyId,
            String username,
            String email,
            String role,
            boolean bcryptCompatiblePassword) {
        return new SourceStaffUserRef(
                legacyId,
                username,
                email,
                username,
                email,
                "Test User",
                "+15555555555",
                role,
                "active",
                bcryptCompatiblePassword ? "$2a$10$valid" : "legacy",
                true,
                bcryptCompatiblePassword);
    }
}
