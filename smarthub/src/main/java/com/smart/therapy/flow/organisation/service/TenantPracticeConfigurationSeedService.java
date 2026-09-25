package com.smart.therapy.flow.organisation.service;

import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantPracticeConfigurationSeedService {

    private final JdbcTemplate jdbcTemplate;
    private final TenantTransactionExecutor tenantTransactionExecutor;

    public void seedDefaults(Long organisationId, String schemaName, String timezone, String practiceName) {
        if (organisationId == null || schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
            return;
        }

        tenantTransactionExecutor.executeWrite(organisationId, schemaName, () -> {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from " + schemaName + ".practice_configuration",
                    Integer.class
            );

            if (count == null || count == 0) {
                String finalTimezone = (timezone != null && !timezone.isBlank()) ? timezone : "America/New_York";
                String finalPracticeName = (practiceName != null && !practiceName.isBlank()) ? practiceName : "My Practice";
                Instant now = Instant.now();
                jdbcTemplate.update(
                        "insert into " + schemaName + ".practice_configuration " +
                                "(createdat, created_by, is_deleted, updatedat, updated_by, version, timezone, practice_name) " +
                                "values (?, ?, false, ?, ?, 0, ?, ?)",
                        Timestamp.from(now),
                        0L,
                        Timestamp.from(now),
                        0L,
                        finalTimezone,
                        finalPracticeName
                );
                log.info("Seeded default practice_configuration with timezone {} and practice name {} for org {} (schema {})", finalTimezone, finalPracticeName, organisationId, schemaName);
            }
            return null;
        });
    }

    /**
     * Keep tenant practice_configuration.timezone aligned with organisation settings.
     */
    public void syncTimezone(Long organisationId, String schemaName, String timezone) {
        if (organisationId == null || schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
            return;
        }
        if (timezone == null || timezone.isBlank()) {
            return;
        }

        tenantTransactionExecutor.executeWrite(organisationId, schemaName, () -> {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from " + schemaName + ".practice_configuration where is_deleted = false",
                    Integer.class
            );
            Instant now = Instant.now();
            if (count == null || count == 0) {
                jdbcTemplate.update(
                        "insert into " + schemaName + ".practice_configuration " +
                                "(createdat, created_by, is_deleted, updatedat, updated_by, version, timezone, practice_name) " +
                                "values (?, ?, false, ?, ?, 0, ?, ?)",
                        Timestamp.from(now),
                        0L,
                        Timestamp.from(now),
                        0L,
                        timezone.trim(),
                        "My Practice"
                );
            } else {
                jdbcTemplate.update(
                        "update " + schemaName + ".practice_configuration " +
                                "set timezone = ?, updatedat = ?, updated_by = 0 " +
                                "where is_deleted = false",
                        timezone.trim(),
                        Timestamp.from(now)
                );
            }
            log.info("Synced practice_configuration timezone {} for org {} (schema {})", timezone, organisationId, schemaName);
            return null;
        });
    }
}
