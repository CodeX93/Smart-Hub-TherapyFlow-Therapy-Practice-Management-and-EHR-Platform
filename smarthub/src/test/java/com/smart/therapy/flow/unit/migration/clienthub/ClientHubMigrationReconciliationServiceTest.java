package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientHubMigrationReconciliationServiceTest {

    @Test
    void refusesToBuildReportWithoutResolvedTargetOrganisation() {
        ClientHubMigrationReconciliationService service = new ClientHubMigrationReconciliationService(null);

        assertThatThrownBy(() -> service.buildReport(
                new SourceInventory("public", List.of(), "fingerprint"),
                new TargetInventory(0, 0, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Exactly one target organisation");
    }
}
