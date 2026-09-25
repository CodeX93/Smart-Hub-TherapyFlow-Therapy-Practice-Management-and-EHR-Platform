package com.smart.therapy.flow.unit.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubMigrationProperties;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHubSourceTimezoneTest {

    @Test
    void defaultsSourceTimezoneToUtc() {
        assertThat(ClientHubSourceInventoryService.sourceZone(new ClientHubMigrationProperties()))
                .isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    void convertsNaiveUtcStorageToEasternDisplay() {
        LocalDateTime storedUtcNaive = LocalDateTime.of(2026, 9, 1, 13, 0);
        var instant = storedUtcNaive.atZone(ZoneId.of("UTC")).toInstant();

        // Old ClientHub UI: 13:00Z → 09:00 America/New_York (EDT)
        assertThat(instant.toString()).isEqualTo("2026-09-01T13:00:00Z");
        assertThat(instant.atZone(ZoneId.of("America/New_York")).toLocalTime().toString())
                .isEqualTo("09:00");
    }

    @Test
    void billingDateZoneFallsBackToSourceTimezone() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();
        properties.setSourceTimezone("America/New_York");

        assertThat(ClientHubSourceInventoryService.billingDateZone(null, properties))
                .isEqualTo(ZoneId.of("America/New_York"));
    }
}
