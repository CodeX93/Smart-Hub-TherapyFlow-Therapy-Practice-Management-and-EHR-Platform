package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubServiceMigrationPlanner.ServiceMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.ServiceTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceServiceInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceServiceRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHubServiceMigrationPlannerTest {

    private final ClientHubServiceMigrationPlanner planner = new ClientHubServiceMigrationPlanner();

    @Test
    void countsCreateUpdateByCodeAndMappedServices() {
        ServiceMigrationPlan plan = planner.buildPlan(
                inventory(3, 0, 0, 0, 0, 0),
                List.of(
                        service("1", "90834"),
                        service("2", "90837"),
                        service("3", "90791")),
                new ServiceTargetState(Set.of("3"), Set.of("90837")));

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.wouldCreate()).isEqualTo(1);
        assertThat(plan.wouldUpdateExistingCode()).isEqualTo(1);
        assertThat(plan.wouldUpdateMapped()).isEqualTo(1);
    }

    @Test
    void blocksMissingRequiredFieldsAndWarnsOnDuplicateCodes() {
        ServiceMigrationPlan plan = planner.buildPlan(
                inventory(2, 1, 1, 1, 1, 1),
                List.of(
                        new SourceServiceRecord("1", "", "Therapy", null, 45, BigDecimal.TEN, null, true, true, true),
                        new SourceServiceRecord("2", "90837", "", null, null, null, null, true, true, true)),
                new ServiceTargetState(Set.of(), Set.of()));

        assertThat(plan.blocked()).isTrue();
        assertThat(plan.blockedRows()).isEqualTo(2);
        assertThat(plan.blockers())
                .contains("Services with blank service_code: 1")
                .contains("Services with blank service_name: 1")
                .contains("Services missing duration: 1")
                .contains("Services missing base_rate: 1");
        assertThat(plan.warnings())
                .contains("Duplicate source service_code groups; execute will keep first match and "
                        + "disambiguate later duplicates: 1");
    }

    private SourceServiceInventory inventory(
            long services,
            long blankCode,
            long blankName,
            long missingDuration,
            long missingRate,
            long duplicateCodeGroups) {
        return new SourceServiceInventory(services, blankCode, blankName, missingDuration, missingRate, duplicateCodeGroups);
    }

    private SourceServiceRecord service(String legacyPk, String code) {
        return new SourceServiceRecord(
                legacyPk,
                code,
                "Service " + code,
                null,
                45,
                new BigDecimal("100.00"),
                "therapy",
                true,
                true,
                true);
    }
}
