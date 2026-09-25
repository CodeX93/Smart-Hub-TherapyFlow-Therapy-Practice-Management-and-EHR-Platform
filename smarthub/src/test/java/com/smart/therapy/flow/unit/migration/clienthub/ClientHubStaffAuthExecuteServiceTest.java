package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientHubStaffAuthExecuteServiceTest {

    @Test
    void refusesToExecuteWithoutResolvedTargetOrganisation() {
        ClientHubStaffAuthExecuteService service =
                new ClientHubStaffAuthExecuteService(null, new ClientHubStaffAuthMigrationPlanner());

        assertThatThrownBy(() -> service.execute(List.of(), new TargetInventory(0, 0, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Exactly one target organisation");
    }
}
