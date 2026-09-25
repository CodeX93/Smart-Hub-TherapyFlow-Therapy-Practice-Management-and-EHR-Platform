package com.smart.therapy.flow.common.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Fail-closed guardrail for new production platform migrations. The four historical seed
 * migrations remain immutable for Flyway history and are retired after migration instead.
 */
@Component
@Slf4j
public class ProductionMigrationSecurityScanner {
    static final Set<String> HISTORICAL_SEED_MIGRATIONS = Set.of(
            "V3__platform_seed_auth.sql",
            "V36__seed_two_tenants_default_users.sql",
            "V75__seed_additional_platform_super_admins.sql",
            "V76__seed_amjad_aqeel_platform_super_admins.sql");

    private static final Pattern BCRYPT_HASH = Pattern.compile("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");
    private static final List<String> KNOWN_SEED_IDENTIFIERS = List.of(
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
    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    public void assertProductionSafe(Flyway flyway) {
        List<Violation> violations = new ArrayList<>();
        for (Location location : flyway.getConfiguration().getLocations()) {
            if (!location.isClassPath()) {
                continue;
            }
            Resource[] resources = resources(location);
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) {
                    continue;
                }
                if (HISTORICAL_SEED_MIGRATIONS.contains(filename)) {
                    continue;
                }
                violations.addAll(scan(filename, read(resource)));
            }
        }
        if (!violations.isEmpty()) {
            throw new IllegalStateException("Unsafe production migration content: " + violations);
        }
        log.debug("Production migration security scan passed");
    }

    public List<Violation> scan(String filename, String sql) {
        String lowerSql = sql.toLowerCase(Locale.ROOT);
        List<Violation> violations = new ArrayList<>();
        if (BCRYPT_HASH.matcher(sql).find()) {
            violations.add(new Violation(filename, "fixed password hash"));
        }
        for (String identifier : KNOWN_SEED_IDENTIFIERS) {
            Pattern identifierPattern = Pattern.compile(
                    "(?<![a-z0-9._%+-])" + Pattern.quote(identifier) + "(?![a-z0-9._%+-])");
            if (identifierPattern.matcher(lowerSql).find()) {
                violations.add(new Violation(filename, "known seeded identifier: " + identifier));
            }
        }
        return violations;
    }

    public boolean isHistoricalSeedMigration(String filename) {
        return HISTORICAL_SEED_MIGRATIONS.contains(filename);
    }

    public List<String> knownSeedIdentifiers() {
        return KNOWN_SEED_IDENTIFIERS;
    }

    private Resource[] resources(Location location) {
        try {
            return resourceResolver.getResources("classpath*:" + location.getPath() + "/*.sql");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to enumerate migrations in " + location, e);
        }
    }

    private static String read(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                content.append(buffer, 0, read);
            }
            return content.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to scan migration " + resource.getFilename(), e);
        }
    }

    public record Violation(String filename, String reason) {
        @Override
        public String toString() {
            return filename + " (" + reason + ")";
        }
    }
}
