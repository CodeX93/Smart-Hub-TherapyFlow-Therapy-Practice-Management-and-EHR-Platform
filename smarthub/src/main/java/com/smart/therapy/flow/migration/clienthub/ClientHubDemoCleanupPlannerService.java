package com.smart.therapy.flow.migration.clienthub;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientHubDemoCleanupPlannerService {

    private static final List<DemoTenantDefinition> DEMO_TENANTS = List.of(
            new DemoTenantDefinition("northstar-wellness", "tenant_northstar"),
            new DemoTenantDefinition("harbor-mental-health", "tenant_harbor")
    );

    private static final List<String> TENANT_TABLES = List.of(
            "users",
            "clients",
            "services",
            "rooms",
            "sessions",
            "session_notes",
            "documents",
            "session_billing",
            "payments",
            "payment_transactions"
    );

    private final JdbcTemplate jdbcTemplate;

    DemoCleanupPlan buildPlan() {
        List<DemoTenantPlan> tenants = DEMO_TENANTS.stream()
                .map(this::buildTenantPlan)
                .toList();
        long seedAuthIdentities = count("""
                SELECT COUNT(*)
                FROM public.auth_identities
                WHERE normalised_login_identifier LIKE '%@therapyflowseed.com'
                """);
        long seedMemberships = count("""
                SELECT COUNT(*)
                FROM public.user_organisations uo
                JOIN public.auth_identities ai ON ai.id = uo.auth_id
                WHERE ai.normalised_login_identifier LIKE '%@therapyflowseed.com'
                """);
        return new DemoCleanupPlan(tenants, seedAuthIdentities, seedMemberships);
    }

    private DemoTenantPlan buildTenantPlan(DemoTenantDefinition demoTenant) {
        long organisationRows = count("""
                SELECT COUNT(*)
                FROM public.organisations
                WHERE slug = ?
                  AND schema_name = ?
                """,
                demoTenant.slug(),
                demoTenant.schemaName());
        List<DemoTenantTableCount> tableCounts = TENANT_TABLES.stream()
                .map(table -> new DemoTenantTableCount(table, countTenantTable(demoTenant.schemaName(), table)))
                .toList();
        return new DemoTenantPlan(demoTenant.slug(), demoTenant.schemaName(), organisationRows, tableCounts);
    }

    private long countTenantTable(String schemaName, String tableName) {
        String qualified = schemaName + "." + tableName;
        Boolean exists = jdbcTemplate.queryForObject("SELECT to_regclass(?) IS NOT NULL", Boolean.class, qualified);
        if (!Boolean.TRUE.equals(exists)) {
            return 0;
        }
        return count("SELECT COUNT(*) FROM " + ClientHubIdentifier.qualified(schemaName, tableName));
    }

    private long count(String sql, Object... args) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class, args);
        return result == null ? 0 : result;
    }

    record DemoCleanupPlan(List<DemoTenantPlan> tenants, long seedAuthIdentities, long seedMemberships) {
        long totalTenantRows() {
            return tenants.stream().mapToLong(DemoTenantPlan::totalTenantRows).sum();
        }
    }

    record DemoTenantPlan(
            String slug,
            String schemaName,
            long organisationRows,
            List<DemoTenantTableCount> tableCounts) {
        long totalTenantRows() {
            return tableCounts.stream().mapToLong(DemoTenantTableCount::rows).sum();
        }
    }

    record DemoTenantTableCount(String tableName, long rows) {
    }

    private record DemoTenantDefinition(String slug, String schemaName) {
    }
}
