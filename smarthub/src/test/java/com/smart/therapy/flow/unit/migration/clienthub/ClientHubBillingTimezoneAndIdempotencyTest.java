package com.smart.therapy.flow.unit.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubBillingExecuteService;
import com.smart.therapy.flow.migration.clienthub.ClientHubMigrationProperties;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientHubBillingTimezoneAndIdempotencyTest {

    @Test
    void refusesToExecuteWithoutResolvedTargetOrganisation() {
        ClientHubBillingExecuteService service = new ClientHubBillingExecuteService(
                null, null, null, null, null, null);

        assertThatThrownBy(() -> service.execute(List.of(), List.of(), new TargetInventory(0, 0, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Exactly one target organisation");
    }

    @Test
    void midnightUtcBillingDateKeepsUtcCalendarDay() {
        Instant midnightUtc = LocalDateTime.of(2026, 9, 6, 0, 0).toInstant(ZoneOffset.UTC);

        assertThat(ClientHubSourceInventoryService.toBillingLocalDate(midnightUtc, ZoneId.of("America/Toronto")))
                .isEqualTo(LocalDate.of(2026, 9, 6));
    }

    @Test
    void nonMidnightBillingDateUsesPracticeTimezone() {
        // V1 rows stored as local-midnight in UTC+5 → 19:00Z previous calendar day.
        Instant source = LocalDateTime.of(2025, 9, 2, 19, 0).toInstant(ZoneOffset.UTC);

        assertThat(ClientHubSourceInventoryService.toBillingLocalDate(source, ZoneId.of("America/Toronto")))
                .isEqualTo(LocalDate.of(2025, 9, 2));
        assertThat(ClientHubSourceInventoryService.toBillingLocalDate(source, ZoneId.of("Asia/Karachi")))
                .isEqualTo(LocalDate.of(2025, 9, 3));
    }

    @Test
    void prefersOrganisationTimezoneOverSourceTimezoneProperty() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();
        properties.setSourceTimezone("UTC");
        TargetInventory target = new TargetInventory(1, 0, 50L, "tenant_50", "America/Toronto");

        assertThat(ClientHubSourceInventoryService.billingDateZone(target, properties))
                .isEqualTo(ZoneId.of("America/Toronto"));
    }

    @Test
    void paymentInstantUsesPracticeTimezoneStartOfDay() {
        LocalDate paymentDate = LocalDate.of(2026, 9, 6);
        Instant instant = ClientHubBillingExecuteService.toInstant(
                paymentDate, null, ZoneId.of("America/Toronto"));

        assertThat(instant).isEqualTo(paymentDate.atStartOfDay(ZoneId.of("America/Toronto")).toInstant());
        assertThat(instant.toString()).isEqualTo("2026-09-06T04:00:00Z");
    }
}
