package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientHubSyncScannerServiceTest {

    @Test
    void refusesToScanWithoutResolvedTargetOrganisation() {
        ClientHubSyncScannerService service = new ClientHubSyncScannerService(null);

        assertThatThrownBy(() -> service.scan(new ClientHubMigrationProperties(), new TargetInventory(0, 0, null, null), false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Exactly one target organisation");
    }

    @Test
    void usesUpdateEventTypeWhenEntityHasNoArchiveCondition() {
        ClientHubSyncScannerService service = new ClientHubSyncScannerService(null);

        String expression = service.eventTypeExpression(
                new ClientHubSyncScannerService.SyncEntityDefinition("clients", "clients", "updated_at", "clients", null));

        assertThat(expression).isEqualTo("'UPDATE'");
    }

    @Test
    void usesArchiveEventTypeWhenEntityArchiveConditionMatches() {
        ClientHubSyncScannerService service = new ClientHubSyncScannerService(null);

        String expression = service.eventTypeExpression(
                new ClientHubSyncScannerService.SyncEntityDefinition(
                        "services", "services", "updated_at", "services", "is_active = false"));

        assertThat(expression).isEqualTo("CASE WHEN is_active = false THEN 'ARCHIVE' ELSE 'UPDATE' END");
    }
}
