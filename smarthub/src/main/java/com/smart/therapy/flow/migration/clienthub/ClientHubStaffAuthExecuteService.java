package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceStaffUserRef;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientHubStaffAuthExecuteService {

    private final JdbcTemplate jdbcTemplate;
    private final ClientHubStaffAuthMigrationPlanner staffAuthMigrationPlanner;

    @Transactional
    public StaffAuthExecuteResult execute(List<SourceStaffUserRef> sourceUsers, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required for staff/auth execution");
        }

        int created = 0;
        int updated = 0;
        int mapped = 0;
        int passwordResetRequired = 0;

        for (SourceStaffUserRef sourceUser : sourceUsers) {
            String targetRole = staffAuthMigrationPlanner.mapRole(sourceUser.sourceRole());
            if (targetRole == null) {
                throw new IllegalStateException("Cannot execute staff/auth import with unmapped source role");
            }

            Long roleId = requireRoleId(targetRole);
            TargetUser targetUser = resolveMappedUser(target, sourceUser.legacyUserId()).orElse(null);
            boolean alreadyMapped = targetUser != null;
            if (targetUser == null) {
                targetUser = resolveExistingUser(target, sourceUser).orElse(null);
            }

            boolean resetRequired = !sourceUser.bcryptCompatiblePassword();
            if (resetRequired) {
                passwordResetRequired++;
            }

            Long authId;
            Long tenantUserId;
            if (targetUser == null) {
                authId = insertAuthIdentity(sourceUser, target.organisationId(), resetRequired);
                tenantUserId = insertTenantUser(sourceUser, target.schemaName(), authId);
                created++;
            } else {
                authId = targetUser.authId();
                tenantUserId = targetUser.tenantUserId();
                updateAuthIdentity(sourceUser, target.organisationId(), authId, resetRequired);
                if (tenantUserId == null) {
                    tenantUserId = insertTenantUser(sourceUser, target.schemaName(), authId);
                } else {
                    updateTenantUser(sourceUser, target.schemaName(), tenantUserId, authId);
                }
                updated++;
                if (alreadyMapped) {
                    mapped++;
                }
            }

            ensureUserOrganisation(authId, target.organisationId());
            ensureRoleAssignment(authId, roleId, target.organisationId());
            upsertLegacyMapping(target, sourceUser, tenantUserId);
        }

        return new StaffAuthExecuteResult(sourceUsers.size(), created, updated, mapped, passwordResetRequired);
    }

    private Long insertAuthIdentity(SourceStaffUserRef sourceUser, Long organisationId, boolean resetRequired) {
        String passwordHash = resetRequired ? null : sourceUser.passwordHash();
        return jdbcTemplate.queryForObject("""
                INSERT INTO public.auth_identities (
                    account_locked, auth_provider, created_at, created_by,
                    email_verification_expiry, email_verification_token, email_verified,
                    failed_login_attempts, identity_type, is_active,
                    last_failed_login, last_password_change_by, last_successful_login,
                    locked_reason, locked_until, login_identifier, login_identifier_changed_at,
                    normalised_login_identifier, password_changed_at, password_hash,
                    password_reset_expiry, password_reset_token, updated_at, version,
                    provider_user_id, sso_enabled, organisation_id, updated_by,
                    is_deleted, deleted_at, full_name, phone, must_change_password,
                    email, normalised_email, username, normalised_username
                )
                VALUES (
                    false, 'LOCAL', CURRENT_TIMESTAMP, 0,
                    NULL, NULL, ?, 0, 'STAFF', ?,
                    NULL, NULL, NULL, NULL, NULL, ?, CURRENT_TIMESTAMP,
                    ?, CASE WHEN ?::varchar IS NULL THEN NULL ELSE CURRENT_TIMESTAMP END, ?,
                    NULL, NULL, CURRENT_TIMESTAMP, 0,
                    NULL, false, ?, 0,
                    false, NULL, ?, ?, ?,
                    ?, ?, ?, ?
                )
                RETURNING id
                """,
                Long.class,
                sourceUser.emailVerified(),
                isActive(sourceUser),
                trimTo(targetUsername(sourceUser), 255),
                targetNormalisedUsername(sourceUser),
                passwordHash,
                passwordHash,
                organisationId,
                trimTo(nonBlank(sourceUser.fullName(), "ClientHub User " + sourceUser.legacyUserId()), 150),
                trimTo(sourceUser.phone(), 20),
                resetRequired,
                trimTo(sourceUser.email(), 255),
                sourceUser.normalisedEmail(),
                trimTo(targetUsername(sourceUser), 255),
                targetNormalisedUsername(sourceUser));
    }

    private void updateAuthIdentity(SourceStaffUserRef sourceUser, Long organisationId, Long authId, boolean resetRequired) {
        String passwordHash = resetRequired ? null : sourceUser.passwordHash();
        jdbcTemplate.update("""
                UPDATE public.auth_identities
                SET email = ?,
                    normalised_email = ?,
                    username = ?,
                    normalised_username = ?,
                    login_identifier = ?,
                    normalised_login_identifier = ?,
                    full_name = ?,
                    phone = ?,
                    is_active = ?,
                    email_verified = ?,
                    password_hash = COALESCE(?, password_hash),
                    password_changed_at = CASE WHEN ?::varchar IS NULL THEN password_changed_at ELSE CURRENT_TIMESTAMP END,
                    must_change_password = ?,
                    updated_at = CURRENT_TIMESTAMP,
                    updated_by = 0,
                    is_deleted = false,
                    deleted_at = NULL
                WHERE id = ?
                  AND organisation_id = ?
                  AND identity_type = 'STAFF'
                """,
                trimTo(sourceUser.email(), 255),
                sourceUser.normalisedEmail(),
                trimTo(targetUsername(sourceUser), 255),
                targetNormalisedUsername(sourceUser),
                trimTo(targetUsername(sourceUser), 255),
                targetNormalisedUsername(sourceUser),
                trimTo(nonBlank(sourceUser.fullName(), "ClientHub User " + sourceUser.legacyUserId()), 150),
                trimTo(sourceUser.phone(), 20),
                isActive(sourceUser),
                sourceUser.emailVerified(),
                passwordHash,
                passwordHash,
                resetRequired,
                authId,
                organisationId);
    }

    private Long insertTenantUser(SourceStaffUserRef sourceUser, String schemaName, Long authId) {
        String usersTable = ClientHubIdentifier.qualified(schemaName, "users");
        return jdbcTemplate.queryForObject("""
                INSERT INTO %s (
                    createdat, created_by, is_deleted, updatedat, updated_by, version,
                    email, full_name, is_active, phone, status, auth_id
                )
                VALUES (
                    CURRENT_TIMESTAMP, 0, false, CURRENT_TIMESTAMP, 0, 0,
                    ?, ?, ?, ?, ?, ?
                )
                RETURNING id
                """.formatted(usersTable),
                Long.class,
                trimTo(sourceUser.email(), 150),
                trimTo(nonBlank(sourceUser.fullName(), "ClientHub User " + sourceUser.legacyUserId()), 150),
                isActive(sourceUser),
                trimTo(sourceUser.phone(), 20),
                toTargetStatus(sourceUser),
                authId);
    }

    private void updateTenantUser(SourceStaffUserRef sourceUser, String schemaName, Long tenantUserId, Long authId) {
        String usersTable = ClientHubIdentifier.qualified(schemaName, "users");
        jdbcTemplate.update("""
                UPDATE %s
                SET email = ?,
                    full_name = ?,
                    is_active = ?,
                    phone = ?,
                    status = ?,
                    auth_id = ?,
                    updatedat = CURRENT_TIMESTAMP,
                    updated_by = 0,
                    is_deleted = false,
                    deleted_at = NULL
                WHERE id = ?
                """.formatted(usersTable),
                trimTo(sourceUser.email(), 150),
                trimTo(nonBlank(sourceUser.fullName(), "ClientHub User " + sourceUser.legacyUserId()), 150),
                isActive(sourceUser),
                trimTo(sourceUser.phone(), 20),
                toTargetStatus(sourceUser),
                authId,
                tenantUserId);
    }

    private void ensureUserOrganisation(Long authId, Long organisationId) {
        jdbcTemplate.update("""
                INSERT INTO public.user_organisations (auth_id, organisation_id, created_at)
                SELECT ?, ?, CURRENT_TIMESTAMP
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM public.user_organisations
                    WHERE auth_id = ?
                      AND organisation_id = ?
                )
                """, authId, organisationId, authId, organisationId);
    }

    private void ensureRoleAssignment(Long authId, Long roleId, Long organisationId) {
        jdbcTemplate.update("""
                INSERT INTO public.auth_identity_roles (
                    created_at, updated_at, version, auth_id, organisation_id, role_id,
                    created_by, updated_by, is_deleted, deleted_at
                )
                SELECT CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, ?, ?, ?, 0, 0, false, NULL
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM public.auth_identity_roles
                    WHERE auth_id = ?
                      AND organisation_id = ?
                      AND role_id = ?
                )
                """, authId, organisationId, roleId, authId, organisationId, roleId);
    }

    private void upsertLegacyMapping(TargetInventory target, SourceStaffUserRef sourceUser, Long tenantUserId) {
        jdbcTemplate.update("""
                INSERT INTO public.clienthub_legacy_id_mappings (
                    organisation_id, source_system, entity_name, source_id,
                    target_schema, target_table, target_id, source_checksum_sha256,
                    first_seen_run_id, last_seen_run_id
                )
                VALUES (?, 'ClientHubAI', 'users', ?, ?, 'users', ?, ?, NULL, NULL)
                ON CONFLICT (organisation_id, source_system, entity_name, source_id)
                DO UPDATE SET
                    target_schema = EXCLUDED.target_schema,
                    target_table = EXCLUDED.target_table,
                    target_id = EXCLUDED.target_id,
                    source_checksum_sha256 = EXCLUDED.source_checksum_sha256,
                    updated_at = CURRENT_TIMESTAMP
                """,
                target.organisationId(),
                sourceUser.legacyUserId(),
                target.schemaName(),
                tenantUserId,
                checksum(sourceUser));
    }

    private Long requireRoleId(String roleName) {
        return queryLong("""
                SELECT id
                FROM public.roles
                WHERE name = ?
                  AND is_active = true
                LIMIT 1
                """, roleName).orElseThrow(() -> new IllegalStateException("Missing active target role: " + roleName));
    }

    private Optional<TargetUser> resolveMappedUser(TargetInventory target, String legacyUserId) {
        String usersTable = ClientHubIdentifier.qualified(target.schemaName(), "users");
        List<TargetUser> users = jdbcTemplate.query("""
                SELECT u.id, u.auth_id
                FROM public.clienthub_legacy_id_mappings m
                JOIN %s u ON u.id = m.target_id
                WHERE m.organisation_id = ?
                  AND m.source_system = 'ClientHubAI'
                  AND m.entity_name = 'users'
                  AND m.source_id = ?
                LIMIT 1
                """.formatted(usersTable),
                (rs, rowNum) -> new TargetUser(rs.getLong("id"), rs.getLong("auth_id")),
                target.organisationId(),
                legacyUserId);
        return users.stream().findFirst();
    }

    private Optional<TargetUser> resolveExistingUser(TargetInventory target, SourceStaffUserRef sourceUser) {
        String usersTable = ClientHubIdentifier.qualified(target.schemaName(), "users");
        List<TargetUser> users = jdbcTemplate.query("""
                SELECT u.id, u.auth_id
                FROM %s u
                WHERE LOWER(TRIM(u.email)) = ?
                LIMIT 1
                """.formatted(usersTable),
                (rs, rowNum) -> new TargetUser(rs.getLong("id"), rs.getLong("auth_id")),
                sourceUser.normalisedEmail());
        if (!users.isEmpty()) {
            return Optional.of(users.get(0));
        }

        Optional<Long> authId = queryLong("""
                SELECT id
                FROM public.auth_identities
                WHERE organisation_id = ?
                  AND identity_type = 'STAFF'
                  AND (normalised_email = ? OR normalised_username = ?)
                LIMIT 1
                """, target.organisationId(), sourceUser.normalisedEmail(), targetNormalisedUsername(sourceUser));
        return authId.map(id -> new TargetUser(null, id));
    }

    private Optional<Long> queryLong(String sql, Object... args) {
        List<Long> values = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong(1), args);
        return values.stream().findFirst();
    }

    private String checksum(SourceStaffUserRef sourceUser) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                sourceUser.legacyUserId(),
                targetNormalisedUsername(sourceUser),
                sourceUser.normalisedEmail(),
                sourceUser.sourceRole(),
                sourceUser.sourceStatus(),
                String.valueOf(sourceUser.bcryptCompatiblePassword())));
    }

    private boolean isActive(SourceStaffUserRef sourceUser) {
        return !"inactive".equals(sourceUser.sourceStatus())
                && !"suspended".equals(sourceUser.sourceStatus())
                && !"locked".equals(sourceUser.sourceStatus());
    }

    private String targetUsername(SourceStaffUserRef sourceUser) {
        return ClientHubStaffAuthOverrides.username(sourceUser);
    }

    private String targetNormalisedUsername(SourceStaffUserRef sourceUser) {
        return ClientHubStaffAuthOverrides.normalisedUsername(sourceUser);
    }

    private String toTargetStatus(SourceStaffUserRef sourceUser) {
        return switch (sourceUser.sourceStatus()) {
            case "inactive" -> "INACTIVE";
            case "suspended" -> "SUSPENDED";
            case "locked" -> "LOCKED";
            default -> "ACTIVE";
        };
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String trimTo(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private record TargetUser(Long tenantUserId, Long authId) {
    }

    public record StaffAuthExecuteResult(
            int sourceUsers,
            int created,
            int updated,
            int mapped,
            int passwordResetRequired) {
    }
}
