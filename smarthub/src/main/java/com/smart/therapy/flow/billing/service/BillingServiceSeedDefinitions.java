package com.smart.therapy.flow.billing.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Default therapy billing services seeded into empty tenant catalogs.
 */
public final class BillingServiceSeedDefinitions {

    public record SeedService(
            String serviceCode,
            String serviceName,
            String description,
            int durationMinutes,
            BigDecimal baseRate,
            String category,
            boolean clientPortalVisible
    ) {}

    private BillingServiceSeedDefinitions() {
    }

    public static List<SeedService> defaultServices() {
        return List.of(
                new SeedService(
                        "90834",
                        "Individual Psychotherapy 45 min",
                        "Individual psychotherapy, 45 minutes",
                        45,
                        new BigDecimal("150.00"),
                        "Psychotherapy",
                        true
                ),
                new SeedService(
                        "90837",
                        "Individual Psychotherapy 60 min",
                        "Individual psychotherapy, 60 minutes",
                        60,
                        new BigDecimal("200.00"),
                        "Psychotherapy",
                        true
                ),
                new SeedService(
                        "90847",
                        "Family Psychotherapy",
                        "Family psychotherapy with patient present",
                        50,
                        new BigDecimal("175.00"),
                        "Psychotherapy",
                        false
                )
        );
    }
}
