package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubClientMigrationPlanner.ClientMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.ClientTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientRef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHubClientMigrationPlannerTest {

    private final ClientHubClientMigrationPlanner planner = new ClientHubClientMigrationPlanner();

    @Test
    void countsCreateUpdateAndNormalizedChildRows() {
        SourceClientInventory inventory = inventory(3, 0, 0, 0, 2, 3, 2, 1, 2, 1, 0, 1, 1);
        List<SourceClientRef> sourceClients = List.of(
                client("1", "CL-1", "Client One", "10"),
                client("2", "CL-2", "Client Two", "11"),
                client("3", "CL-3", "Client Three", null));
        ClientTargetState targetState = new ClientTargetState(Set.of("2"), Set.of("10", "11"), 1);

        ClientMigrationPlan plan = planner.buildPlan(inventory, sourceClients, targetState);

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.wouldCreate()).isEqualTo(2);
        assertThat(plan.wouldUpdateMapped()).isEqualTo(1);
        assertThat(plan.emailContactRows()).isEqualTo(3);
        assertThat(plan.completeInsuranceRows()).isEqualTo(1);
    }

    @Test
    void blocksMissingRequiredFieldsAndUnmappedTherapistAssignments() {
        SourceClientInventory inventory = inventory(4, 1, 1, 1, 2, 0, 0, 0, 0, 0, 2, 0, 0);
        List<SourceClientRef> sourceClients = List.of(
                client("1", "", "Client One", null),
                client("2", "CL-2", "", null),
                client("3", "CL-3", "Client Three", "missing-user"),
                client("4", "CL-4", "Client Four", "mapped-user"));
        ClientTargetState targetState = new ClientTargetState(Set.of(), Set.of("mapped-user"), 0);

        ClientMigrationPlan plan = planner.buildPlan(inventory, sourceClients, targetState);

        assertThat(plan.blocked()).isTrue();
        assertThat(plan.blockedRows()).isEqualTo(3);
        assertThat(plan.wouldCreate()).isEqualTo(1);
        assertThat(plan.blockers())
                .contains("Clients with blank client_id: 1")
                .contains("Clients with blank full_name: 1")
                .contains("Duplicate source client_id groups: 1")
                .contains("Clients assigned to unmigrated therapists: 1");
        assertThat(plan.warnings())
                .contains("Clients with partial insurance data requiring review: 2");
    }

    @Test
    void warnsWhenTargetHasUnmappedClients() {
        ClientMigrationPlan plan = planner.buildPlan(
                inventory(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                List.of(client("1", "CL-1", "Client One", null)),
                new ClientTargetState(Set.of(), Set.of(), 5));

        assertThat(plan.warnings())
                .contains("Target tenant already has clients but no ClientHubAI client mappings; execute will attempt blind-index client_id matching before creating");
    }

    private SourceClientRef client(String legacyPk, String clientId, String fullName, String assignedTherapistLegacyId) {
        return new SourceClientRef(legacyPk, clientId, fullName, assignedTherapistLegacyId);
    }

    private SourceClientInventory inventory(
            long clientRows,
            long blankClientIdRows,
            long blankFullNameRows,
            long duplicateClientIdGroups,
            long assignedTherapistRows,
            long emailContactRows,
            long phoneContactRows,
            long emergencyContactRows,
            long addressRows,
            long completeInsuranceRows,
            long incompleteInsuranceRows,
            long referralRows,
            long employmentRows) {
        return new SourceClientInventory(
                clientRows,
                blankClientIdRows,
                blankFullNameRows,
                duplicateClientIdGroups,
                assignedTherapistRows,
                emailContactRows,
                phoneContactRows,
                emergencyContactRows,
                addressRows,
                completeInsuranceRows,
                incompleteInsuranceRows,
                referralRows,
                employmentRows);
    }
}
