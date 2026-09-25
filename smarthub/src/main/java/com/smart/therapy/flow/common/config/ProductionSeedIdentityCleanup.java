package com.smart.therapy.flow.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/** Retires the immutable historical seed identities when the production profile migrates. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductionSeedIdentityCleanup {
    static final String EVIDENCE_KEY = "production_seed_identity_cleanup";
    private static final List<String> SEEDED_IDENTIFIERS = List.of(
            "superadmin@therapyflow.com",
            "admin@therapyflow.com",
            "supervisor@therapyflow.com",
            "therapist@therapyflow.com",
            "platform.superadmin@therapyflowseed.com",
            "northstar.admin1@therapyflowseed.com",
            "northstar.admin2@therapyflowseed.com",
            "northstar.therapist1@therapyflowseed.com",
            "northstar.therapist2@therapyflowseed.com",
            "northstar.therapist3@therapyflowseed.com",
            "northstar.client1@therapyflowseed.com",
            "northstar.client2@therapyflowseed.com",
            "northstar.client3@therapyflowseed.com",
            "northstar.client4@therapyflowseed.com",
            "northstar.client5@therapyflowseed.com",
            "northstar.billing@therapyflowseed.com",
            "northstar.supervisor@therapyflowseed.com",
            "harbor.admin1@therapyflowseed.com",
            "harbor.admin2@therapyflowseed.com",
            "harbor.therapist1@therapyflowseed.com",
            "harbor.therapist2@therapyflowseed.com",
            "harbor.therapist3@therapyflowseed.com",
            "harbor.client1@therapyflowseed.com",
            "harbor.client2@therapyflowseed.com",
            "harbor.client3@therapyflowseed.com",
            "harbor.client4@therapyflowseed.com",
            "harbor.client5@therapyflowseed.com",
            "harbor.billing@therapyflowseed.com",
            "harbor.supervisor@therapyflowseed.com",
            "superadmin@therapyflow.pro",
            "superadmin2@smarthub.com",
            "amjad-superadmin@therapyflow.pro",
            "aqeel-superadmin@therapyflow.pro");

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public CleanupResult removeSeedIdentities() {
        CleanupResult result = transactionTemplate.execute(status -> {
            Object[] identifiers = SEEDED_IDENTIFIERS.toArray();
            String placeholders = String.join(", ", SEEDED_IDENTIFIERS.stream().map(value -> "?").toList());
            String identityPredicate = "normalised_login_identifier IN (" + placeholders + ")";

            int retired = jdbcTemplate.update(
                    "UPDATE public.auth_identities "
                            + "SET is_active = false, account_locked = true, email_verified = false, "
                            + "    is_deleted = true, deleted_at = NOW(), password_hash = NULL, "
                            + "    login_identifier = 'retired-seeded-' || id || '@invalid.local', "
                            + "    normalised_login_identifier = 'retired-seeded-' || id || '@invalid.local', "
                            + "    email = NULL, normalised_email = NULL, username = NULL, normalised_username = NULL, "
                            + "    provider_user_id = NULL, updated_at = NOW() "
                            + "WHERE " + identityPredicate,
                    identifiers);

            jdbcTemplate.update(
                    "DELETE FROM public.auth_identity_roles air "
                            + "USING public.auth_identities ai "
                            + "WHERE air.auth_id = ai.id AND ai.normalised_login_identifier LIKE 'retired-seeded-%@invalid.local'");
            jdbcTemplate.update(
                    "DELETE FROM public.user_organisations uo "
                            + "USING public.auth_identities ai "
                            + "WHERE uo.auth_id = ai.id AND ai.normalised_login_identifier LIKE 'retired-seeded-%@invalid.local'");

            Long remaining = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM public.auth_identities WHERE normalised_login_identifier LIKE 'retired-seeded-%@invalid.local' AND is_active = true",
                    Long.class);
            if (remaining != null && remaining != 0) {
                throw new IllegalStateException("Production seed identity cleanup left active identities: " + remaining);
            }

            jdbcTemplate.update(
                    "INSERT INTO public.platform_migration_evidence (evidence_key, observed_count, recorded_at, details) "
                            + "VALUES (?, ?, NOW(), ?) "
                            + "ON CONFLICT (evidence_key) DO UPDATE SET observed_count = EXCLUDED.observed_count, "
                            + "recorded_at = EXCLUDED.recorded_at, details = EXCLUDED.details",
                    EVIDENCE_KEY, retired, "Historical seeded identities retired; password material cleared and privileges removed.");
            return new CleanupResult(retired, remaining == null ? 0 : remaining);
        });
        if (result == null) {
            throw new IllegalStateException("Production seed identity cleanup did not execute");
        }
        log.info("Production seed identity cleanup retired {} historical identities", result.retiredCount());
        return result;
    }

    public static List<String> seededIdentifiers() {
        return SEEDED_IDENTIFIERS;
    }

    record CleanupResult(int retiredCount, long activeRetainedCount) {}
}
