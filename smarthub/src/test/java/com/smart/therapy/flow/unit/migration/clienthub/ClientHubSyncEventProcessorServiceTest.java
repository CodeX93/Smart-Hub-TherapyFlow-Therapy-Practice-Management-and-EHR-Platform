package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientHubSyncEventProcessorServiceTest {

    @Test
    void refusesToProcessWithoutResolvedTargetOrganisation() {
        ClientHubSyncEventProcessorService service = new ClientHubSyncEventProcessorService(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.process(new ClientHubMigrationProperties(), new TargetInventory(0, 0, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Exactly one target organisation");
    }

    @Test
    void resolvesTargetMappingForSucceededEvent() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ClientHubSyncEventProcessorService.TargetMappingRef mapping =
                new ClientHubSyncEventProcessorService.TargetMappingRef("clients", 99L);
        when(jdbcTemplate.query(
                any(String.class),
                any(RowMapper.class),
                eq(7L),
                eq("clients"),
                eq("legacy-1"))).thenReturn(List.of(mapping));
        ClientHubSyncEventProcessorService service = new ClientHubSyncEventProcessorService(
                jdbcTemplate, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        Optional<ClientHubSyncEventProcessorService.TargetMappingRef> result = service.resolveTargetMapping(
                new TargetInventory(1, 0, 7L, "tenant_real"),
                new ClientHubSyncEventProcessorService.SyncEventRef(42L, "clients", "legacy-1", "UPDATE", 0));

        assertThat(result).contains(mapping);
    }

    @Test
    void refusesToApplyArchiveEventsUntilSemanticsAreApproved() {
        ClientHubSyncEventProcessorService service = new ClientHubSyncEventProcessorService(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.applyEvent(
                new ClientHubMigrationProperties(),
                new TargetInventory(1, 0, 7L, "tenant_real"),
                new ClientHubSyncEventProcessorService.SyncEventRef(42L, "services", "legacy-1", "ARCHIVE", 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not apply-enabled yet");
    }
}
