package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthIdentityRole;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.service.AuthSessionService;
import com.smart.therapy.flow.common.security.AuthIdentityDetailsService;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.organisation.service.TenantStaffProfileBootstrapService;
import com.smart.therapy.flow.superadmin.entity.PlatformImpersonationPolicy;
import com.smart.therapy.flow.superadmin.entity.PlatformImpersonationSession;
import com.smart.therapy.flow.superadmin.repository.PlatformImpersonationPolicyRepository;
import com.smart.therapy.flow.superadmin.repository.PlatformImpersonationSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuperAdminImpersonationService {

    private final PlatformImpersonationPolicyRepository policyRepository;
    private final PlatformImpersonationSessionRepository sessionRepository;
    private final OrganisationRepository organisationRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final UserOrganisationRepository userOrganisationRepository;
    private final PlatformAuditService platformAuditService;
    private final TenantStaffProfileBootstrapService tenantStaffProfileBootstrapService;
    private final AuthIdentityDetailsService authIdentityDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthSessionService authSessionService;
    private final SecureRandom secureRandom = new SecureRandom();
    @Value("${superadmin.jobs.impersonation-cleanup.enabled:true}")
    private boolean impersonationCleanupEnabled;
    private volatile Boolean impersonationCleanupEnabledOverride;

    @Transactional(readOnly = true)
    public PlatformImpersonationPolicy getPolicy() {
        return policyRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    return policyRepository.save(PlatformImpersonationPolicy.builder()
                            .enabled(true)
                            .requireReason(true)
                            .minReasonLength(10)
                            .maxDurationMinutes(60)
                            .allowCrossOrganisation(false)
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
                });
    }

    @Transactional
    public PlatformImpersonationPolicy upsertPolicy(
            Boolean enabled,
            Boolean requireReason,
            Integer minReasonLength,
            Integer maxDurationMinutes,
            Boolean allowCrossOrganisation,
            List<String> allowedRoles,
            List<String> deniedRoles,
            List<Long> allowedOrgIds,
            List<Long> deniedOrgIds,
            Long actorAuthId
    ) {
        PlatformImpersonationPolicy policy = getPolicy();
        policy.setEnabled(enabled != null ? enabled : policy.getEnabled());
        policy.setRequireReason(requireReason != null ? requireReason : policy.getRequireReason());
        if (minReasonLength != null && minReasonLength >= 0) {
            policy.setMinReasonLength(minReasonLength);
        }
        if (maxDurationMinutes != null && maxDurationMinutes > 0) {
            policy.setMaxDurationMinutes(maxDurationMinutes);
        }
        policy.setAllowCrossOrganisation(allowCrossOrganisation != null ? allowCrossOrganisation : policy.getAllowCrossOrganisation());
        if (allowedRoles != null) {
            policy.setAllowedRoleNames(toRoleCsv(allowedRoles));
        }
        if (deniedRoles != null) {
            policy.setDeniedRoleNames(toRoleCsv(deniedRoles));
        }
        if (allowedOrgIds != null) {
            policy.setAllowedOrgIds(toLongCsv(allowedOrgIds));
        }
        if (deniedOrgIds != null) {
            policy.setDeniedOrgIds(toLongCsv(deniedOrgIds));
        }
        policy.setUpdatedByAuthId(actorAuthId);
        policy.setUpdatedAt(Instant.now());
        PlatformImpersonationPolicy saved = policyRepository.save(policy);
        platformAuditService.log(actorAuthId, "IMPERSONATION_POLICY_UPDATED", "PlatformImpersonationPolicy", String.valueOf(saved.getId()), "");
        return saved;
    }

    @Transactional
    public CreatedImpersonation startSession(
            Long superAdminAuthId,
            Long targetAuthId,
            Long organisationId,
            String reason,
            Integer requestedDurationMinutes
    ) {
        PlatformImpersonationPolicy policy = getPolicy();
        if (!Boolean.TRUE.equals(policy.getEnabled())) {
            throw new IllegalArgumentException("Impersonation is disabled");
        }
        if (Boolean.TRUE.equals(policy.getRequireReason())) {
            if (reason == null || reason.isBlank() || reason.trim().length() < policy.getMinReasonLength()) {
                throw new IllegalArgumentException("Reason is required and too short");
            }
        }

        Organisation organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Organisation not found"));
        AuthIdentity target = authIdentityRepository.findById(targetAuthId)
                .orElseThrow(() -> new IllegalArgumentException("Target identity not found"));

        enforceOrgPolicy(policy, organisationId);

        boolean belongsByLink = userOrganisationRepository.existsByAuth_IdAndOrganisation_Id(targetAuthId, organisationId);
        boolean belongsByIdentity = target.getOrganisation() != null && organisationId.equals(target.getOrganisation().getId());
        if (!Boolean.TRUE.equals(policy.getAllowCrossOrganisation()) && !(belongsByLink || belongsByIdentity)) {
            throw new IllegalArgumentException("Target identity does not belong to organisation");
        }

        enforceRolePolicy(policy, target, organisationId);

        // Ensure tenant user profile exists for staff impersonation targets to avoid "admin not found" on tenant APIs.
        if (target.getIdentityType() == com.smart.therapy.flow.auth.entity.IdentityType.STAFF
                && organisation.getSchemaName() != null
                && !organisation.getSchemaName().isBlank()
                && !"public".equalsIgnoreCase(organisation.getSchemaName())) {
            try {
                tenantStaffProfileBootstrapService.ensureStaffProfiles(organisationId, organisation.getSchemaName(), organisation.getTimezone());
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Tenant staff profile bootstrap failed before impersonation: " + ex.getMessage());
            }
        }

        int maxDuration = policy.getMaxDurationMinutes() != null ? policy.getMaxDurationMinutes() : 60;
        int effectiveDuration = requestedDurationMinutes != null && requestedDurationMinutes > 0
                ? Math.min(requestedDurationMinutes, maxDuration)
                : maxDuration;

        String raw = generateToken();
        String prefix = raw.substring(0, Math.min(22, raw.length()));
        String hash = sha256(raw);
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(effectiveDuration * 60L);

        PlatformImpersonationSession session = PlatformImpersonationSession.builder()
                .superAdminAuthId(superAdminAuthId)
                .targetAuthId(targetAuthId)
                .organisation(organisation)
                .reason(reason)
                .tokenPrefix(prefix)
                .tokenHash(hash)
                .status("active")
                .startedAt(now)
                .expiresAt(expiresAt)
                .createdAt(now)
                .updatedAt(now)
                .build();
        session = sessionRepository.save(session);
        platformAuditService.log(
                superAdminAuthId,
                "IMPERSONATION_STARTED",
                "PlatformImpersonationSession",
                String.valueOf(session.getId()),
                "targetAuthId=" + targetAuthId + ", organisationId=" + organisationId
        );
        return new CreatedImpersonation(session, raw);
    }

    @Transactional(readOnly = true)
    public List<PlatformImpersonationSession> listSessions() {
        return sessionRepository.findTop200ByOrderByCreatedAtDesc();
    }

    @Transactional
    public PlatformImpersonationSession endSession(Long sessionId, Long actorAuthId) {
        PlatformImpersonationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Impersonation session not found"));
        session.setStatus("ended");
        session.setEndedAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        PlatformImpersonationSession saved = sessionRepository.save(session);
        authSessionService.revokeAllForImpersonationSession(sessionId);
        platformAuditService.log(actorAuthId, "IMPERSONATION_ENDED", "PlatformImpersonationSession", String.valueOf(sessionId), "");
        return saved;
    }

    @Transactional
    public ExchangedImpersonation exchangeSessionToken(Long actorAuthId, String rawToken, String ipAddress) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Impersonation token is required");
        }
        String normalizedToken = rawToken.trim();
        String prefix = normalizedToken.substring(0, Math.min(22, normalizedToken.length()));
        PlatformImpersonationSession session = sessionRepository.findByTokenPrefix(prefix)
                .orElseThrow(() -> new IllegalArgumentException("Invalid impersonation token"));
        String expectedHash = session.getTokenHash();
        String actualHash = sha256(normalizedToken);
        if (expectedHash == null || !MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                actualHash.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Invalid impersonation token");
        }
        if (!"active".equalsIgnoreCase(session.getStatus())) {
            throw new IllegalArgumentException("Impersonation session is not active");
        }
        if (actorAuthId == null || !actorAuthId.equals(session.getSuperAdminAuthId())) {
            throw new SecurityException("Only the initiating super admin can exchange this impersonation token");
        }
        Instant now = Instant.now();
        if (session.getExpiresAt() == null || !session.getExpiresAt().isAfter(now)) {
            session.setStatus("expired");
            session.setEndedAt(now);
            session.setUpdatedAt(now);
            sessionRepository.save(session);
            throw new IllegalArgumentException("Impersonation session expired");
        }

        Organisation organisation = session.getOrganisation();
        String previousSchema = TenantContext.getSchemaName();
        Long previousOrgId = TenantContext.getOrganisationId();
        try {
            String schema = organisation != null ? organisation.getSchemaName() : null;
            if (schema == null || schema.isBlank()) {
                schema = "public";
            }
            TenantContext.setSchemaName(schema);
            TenantContext.setOrganisationId(organisation != null ? organisation.getId() : null);

            UserDetails targetDetails = authIdentityDetailsService.loadUserByAuthId(session.getTargetAuthId());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    targetDetails, null, targetDetails.getAuthorities());
            String jti = AuthSessionService.generateJti();
            String accessToken = jwtTokenProvider.generateToken(
                    authentication,
                    jti,
                    java.util.Date.from(session.getExpiresAt()),
                    Map.of(
                            "impersonation", true,
                            "impersonationSessionId", session.getId(),
                            "impersonatedByAuthId", actorAuthId
                    )
            );
            authSessionService.createSession(
                    session.getTargetAuthId(), jti, session.getExpiresAt(), ipAddress, "impersonation", session.getId());
            session.setUpdatedAt(now);
            sessionRepository.save(session);
            platformAuditService.log(
                    actorAuthId,
                    "IMPERSONATION_TOKEN_EXCHANGED",
                    "PlatformImpersonationSession",
                    String.valueOf(session.getId()),
                    "targetAuthId=" + session.getTargetAuthId() + ", organisationId=" + session.getOrganisation().getId()
            );
            return new ExchangedImpersonation(
                    session,
                    accessToken,
                    ((AuthPrincipal) targetDetails).getLoginIdentifier(),
                    extractRoles(targetDetails.getAuthorities()),
                    extractPermissions(targetDetails.getAuthorities())
            );
        } finally {
            TenantContext.clear();
            if (previousSchema != null && !previousSchema.isBlank()) {
                TenantContext.setSchemaName(previousSchema);
            }
            if (previousOrgId != null) {
                TenantContext.setOrganisationId(previousOrgId);
            }
        }
    }

    @Scheduled(fixedDelayString = "${superadmin.impersonation.cleanup-ms:60000}")
    @Transactional
    public void expireSessions() {
        if (!isImpersonationCleanupEnabled()) {
            return;
        }
        expireSessionsInternal();
    }

    @Transactional
    public void runImpersonationCleanupNow() {
        expireSessionsInternal();
    }

    public void setImpersonationCleanupEnabledOverride(Boolean enabled) {
        this.impersonationCleanupEnabledOverride = enabled;
    }

    public Boolean getImpersonationCleanupEnabledOverride() {
        return impersonationCleanupEnabledOverride;
    }

    public boolean isImpersonationCleanupEnabled() {
        return impersonationCleanupEnabledOverride != null ? impersonationCleanupEnabledOverride : impersonationCleanupEnabled;
    }

    private void expireSessionsInternal() {
        Instant now = Instant.now();
        List<PlatformImpersonationSession> expired = sessionRepository.findByStatusAndExpiresAtBefore("active", now);
        for (PlatformImpersonationSession s : expired) {
            s.setStatus("expired");
            s.setEndedAt(now);
            s.setUpdatedAt(now);
            sessionRepository.save(s);
            authSessionService.revokeAllForImpersonationSession(s.getId());
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "imp_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash impersonation token", e);
        }
    }

    private void enforceOrgPolicy(PlatformImpersonationPolicy policy, Long organisationId) {
        Set<Long> allowed = parseLongCsv(policy.getAllowedOrgIds());
        if (!allowed.isEmpty() && !allowed.contains(organisationId)) {
            throw new IllegalArgumentException("Impersonation not allowed for this organisation");
        }
        Set<Long> denied = parseLongCsv(policy.getDeniedOrgIds());
        if (denied.contains(organisationId)) {
            throw new IllegalArgumentException("Impersonation denied for this organisation");
        }
    }

    private void enforceRolePolicy(PlatformImpersonationPolicy policy, AuthIdentity target, Long organisationId) {
        Set<RoleName> allowedRoles = parseRoleCsv(policy.getAllowedRoleNames());
        Set<RoleName> deniedRoles = parseRoleCsv(policy.getDeniedRoleNames());
        Set<RoleName> targetRoles = resolveTargetRoles(target, organisationId);

        if (!deniedRoles.isEmpty() && targetRoles.stream().anyMatch(deniedRoles::contains)) {
            throw new IllegalArgumentException("Impersonation denied for target role");
        }
        if (!allowedRoles.isEmpty() && targetRoles.stream().noneMatch(allowedRoles::contains)) {
            throw new IllegalArgumentException("Target role is not allowed for impersonation");
        }
    }

    private Set<RoleName> resolveTargetRoles(AuthIdentity target, Long organisationId) {
        if (target.getRoles() == null) {
            return Set.of();
        }
        return target.getRoles().stream()
                .filter(role -> isRoleInOrganisation(role, organisationId))
                .map(AuthIdentityRole::getRole)
                .filter(java.util.Objects::nonNull)
                .map(r -> {
                    try {
                        return RoleName.valueOf(r.getName().toUpperCase(Locale.ROOT));
                    } catch (Exception ex) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean isRoleInOrganisation(AuthIdentityRole role, Long organisationId) {
        if (role.getOrganisation() == null) {
            return true;
        }
        return organisationId != null && organisationId.equals(role.getOrganisation().getId());
    }

    private static String toRoleCsv(List<String> roles) {
        if (roles == null) {
            return null;
        }
        return roles.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(r -> RoleName.valueOf(r.trim().toUpperCase(Locale.ROOT)).name())
                .distinct()
                .collect(Collectors.joining(","));
    }

    private static String toLongCsv(List<Long> values) {
        if (values == null) {
            return null;
        }
        return values.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private static Set<RoleName> parseRoleCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> RoleName.valueOf(s.toUpperCase(Locale.ROOT)))
                .collect(Collectors.toSet());
    }

    private static Set<Long> parseLongCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }

    private List<String> extractRoles(java.util.Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .distinct()
                .toList();
    }

    private List<String> extractPermissions(java.util.Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && !authority.startsWith("ROLE_"))
                .distinct()
                .toList();
    }

    public record CreatedImpersonation(PlatformImpersonationSession session, String rawToken) {}
    public record ExchangedImpersonation(
            PlatformImpersonationSession session,
            String accessToken,
            String loginIdentifier,
            List<String> roles,
            List<String> permissions
    ) {}
}
