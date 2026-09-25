package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientHubNotificationExecuteServiceTest {

    @Test
    void refusesToExecuteWithoutResolvedTargetOrganisation() {
        ClientHubNotificationExecuteService service = new ClientHubNotificationExecuteService(
                null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.execute(
                List.of(), List.of(), List.of(), List.of(), List.of(),
                new TargetInventory(0, 0, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Exactly one target organisation");
    }
}
