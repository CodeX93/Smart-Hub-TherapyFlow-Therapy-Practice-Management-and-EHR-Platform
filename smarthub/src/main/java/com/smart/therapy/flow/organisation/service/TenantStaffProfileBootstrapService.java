package com.smart.therapy.flow.organisation.service;

import com.smart.therapy.flow.auth.dto.UserStatus;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Ensures tenant-scoped staff profiles exist for staff identities linked to an organisation.
 * This bridges onboarding-created auth identities (public schema) with tenant users table.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantStaffProfileBootstrapService {

    private static final Pattern NAME_SPLIT = Pattern.compile("[._\\-]+");
    private static final Pattern SAFE_SCHEMA = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final UserOrganisationRepository userOrganisationRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TenantTransactionExecutor tenantTransactionExecutor;

    public int ensureStaffProfiles(Long organisationId, String schemaName, String timezone) {
        if (organisationId == null || schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
            return 0;
        }

        List<Long> authIds = userOrganisationRepository.findDistinctAuthIdsByOrganisationId(organisationId);
        if (authIds.isEmpty()) {
            return 0;
        }

        List<AuthIdentity> staffIdentities = authIdentityRepository.findAllById(authIds).stream()
                .filter(identity -> identity != null
                        && identity.getIdentityType() == IdentityType.STAFF
                        && !Boolean.TRUE.equals(identity.getIsDeleted()))
                .toList();

        if (staffIdentities.isEmpty()) {
            return 0;
        }

        Integer created = tenantTransactionExecutor.executeWrite(organisationId, schemaName, () -> {
            String safeSchema = requireSafeSchema(schemaName);
            if (!tenantUsersTableExists(safeSchema)) {
                throw new IllegalStateException("relation \"users\" does not exist");
            }
            int localCreated = 0;
            for (AuthIdentity identity : staffIdentities) {
                if (tenantUserExistsByAuthId(safeSchema, identity.getId())) {
                    continue;
                }
                String login = identity.getLoginIdentifier() != null ? identity.getLoginIdentifier().trim() : "";
                String email = login.isBlank()
                        ? ("auth-" + identity.getId() + "@local.invalid")
                        : login.toLowerCase(Locale.ROOT);
                insertTenantUser(safeSchema, identity.getId(), email, resolveDisplayName(identity), Boolean.TRUE.equals(identity.getIsActive())
                        ? UserStatus.ACTIVE.name()
                        : UserStatus.INACTIVE.name(), Boolean.TRUE.equals(identity.getIsActive()), timezone);
                localCreated++;
                log.info("Bootstrapped tenant staff profile for authId {} in org {}", identity.getId(), organisationId);
            }
            return localCreated;
        });
        return created != null ? created : 0;
    }

    /** Onboarding-provided full name when present; otherwise a name derived from the login identifier. */
    private String resolveDisplayName(AuthIdentity identity) {
        String fullName = identity.getFullName();
        if (fullName != null && !fullName.isBlank()) {
            return fullName.trim();
        }
        return toDisplayName(identity.getLoginIdentifier());
    }

    private String toDisplayName(String loginIdentifier) {
        if (loginIdentifier == null || loginIdentifier.isBlank()) {
            return "Tenant Admin";
        }
        String local = loginIdentifier;
        int atIdx = local.indexOf('@');
        if (atIdx > 0) {
            local = local.substring(0, atIdx);
        }
        String[] parts = NAME_SPLIT.split(local);
        if (parts.length == 0) {
            return "Tenant Admin";
        }
        StringBuilder name = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (name.length() > 0) {
                name.append(' ');
            }
            String normalized = part.toLowerCase(Locale.ROOT);
            name.append(Character.toUpperCase(normalized.charAt(0)));
            if (normalized.length() > 1) {
                name.append(normalized.substring(1));
            }
        }
        return name.length() > 0 ? name.toString() : "Tenant Admin";
    }

    private static String requireSafeSchema(String schemaName) {
        if (!SAFE_SCHEMA.matcher(schemaName).matches()) {
            throw new IllegalArgumentException("Invalid schema name");
        }
        return schemaName;
    }

    private boolean tenantUsersTableExists(String schema) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = ? and table_name = 'users'",
                Integer.class,
                schema
        );
        return count != null && count > 0;
    }

    private boolean tenantUserExistsByAuthId(String schema, Long authId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from " + schema + ".users where auth_id = ? and is_deleted = false",
                Integer.class,
                authId
        );
        return count != null && count > 0;
    }

    private void insertTenantUser(String schema, Long authId, String email, String fullName, String status, boolean isActive, String timezone) {
        Instant now = Instant.now();
        // Tenant users table has no timezone column (timezone lives on user_profiles / organisation).
        jdbcTemplate.update(
                "insert into " + schema + ".users " +
                        "(createdat, created_by, is_deleted, updatedat, updated_by, version, email, full_name, is_active, status, auth_id) " +
                        "values (?, ?, false, ?, ?, 0, ?, ?, ?, ?, ?)",
                Timestamp.from(now),
                0L,
                Timestamp.from(now),
                0L,
                email,
                fullName,
                isActive,
                status,
                authId
        );
    }
}
