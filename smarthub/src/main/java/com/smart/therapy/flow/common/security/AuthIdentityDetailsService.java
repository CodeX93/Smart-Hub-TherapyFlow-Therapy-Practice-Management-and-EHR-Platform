package com.smart.therapy.flow.common.security;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthIdentityRole;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.entity.Permission;
import com.smart.therapy.flow.auth.entity.Role;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Single UserDetailsService for unified auth. Loads by normalised_login_identifier, returns AuthPrincipal.
 * Supports username/login_identifier and tenant user email fallback.
 */
@Primary
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthIdentityDetailsService implements UserDetailsService {

    private final AuthIdentityRepository authIdentityRepository;
    private final AuthIdentityRoleRepository authIdentityRoleRepository;
    private final UserRepository userRepository;
    private final UserOrganisationRepository userOrganisationRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalised = (username != null) ? username.toLowerCase().trim() : "";
        AuthIdentity auth = resolveIdentityForLogin(normalised)
                .orElseThrow(() -> new UsernameNotFoundException("Identity not found: " + username));

        if (!Boolean.TRUE.equals(auth.getIsActive())) {
            throw new UsernameNotFoundException("Identity is inactive: " + username);
        }

        Set<GrantedAuthority> authorities = loadAuthorities(auth.getId());
        return AuthPrincipal.create(auth, authorities);
    }

    /**
     * Load UserDetails by authId (for JWT filter after validating token).
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserByAuthId(Long authId) {
        AuthIdentity auth = authIdentityRepository.findById(authId)
            .orElseThrow(() -> new UsernameNotFoundException("Identity not found: " + authId));
        if (!Boolean.TRUE.equals(auth.getIsActive())) {
            throw new UsernameNotFoundException("Identity is inactive: " + authId);
        }
        Set<GrantedAuthority> authorities = loadAuthorities(auth.getId());
        return AuthPrincipal.create(auth, authorities);
    }

    /**
     * Load authorities for authId (all orgs). Uses JOIN FETCH to avoid N+1.
     */
    @Transactional(readOnly = true)
    public Set<GrantedAuthority> loadAuthorities(Long authId) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        String schema = TenantContext.getSchemaName();
        boolean hasTenantSchema = schema != null && !schema.isBlank() && !"public".equalsIgnoreCase(schema);
        if (hasTenantSchema) {
            Long orgId = TenantContext.getOrganisationId();
            if (orgId == null) {
                return authorities;
            }
            List<AuthIdentityRole> identityRoles =
                    authIdentityRoleRepository.findByAuthIdAndOrganisationIdWithRolesAndPermissions(authId, orgId);
            for (AuthIdentityRole air : identityRoles) {
                Role role = air.getRole();
                if (role != null && Boolean.TRUE.equals(role.getIsActive())) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                    addPermissionAuthorities(authorities, role);
                }
            }
        }
        // Only grant platform roles when schema is public (admin.yourapp.com). Not when in a tenant — reduces risk.
        boolean isPublicContext = !hasTenantSchema;
        if (isPublicContext) {
            List<AuthIdentityRole> platformRoles =
                    authIdentityRoleRepository.findByAuthIdWithRolesAndPermissionsForPlatform(authId);
            for (AuthIdentityRole air : platformRoles) {
                Role role = air.getRole();
                if (role != null && Boolean.TRUE.equals(role.getIsActive())) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_PLATFORM_" + role.getName()));
                    addPermissionAuthorities(authorities, role);
                }
            }
        }
        return authorities;
    }

    /**
     * When TenantContext is already set (multi-org login with orgSlug/orgId), resolve the STAFF
     * identity for that organisation by email or username. Otherwise fall back to unscoped lookup
     * (platform / single-org auto-routing).
     */
    private Optional<AuthIdentity> resolveIdentityForLogin(String normalised) {
        if (normalised == null || normalised.isBlank()) {
            return Optional.empty();
        }

        String schema = TenantContext.getSchemaName();
        Long orgId = TenantContext.getOrganisationId();
        boolean hasTenantSchema = schema != null && !schema.isBlank() && !"public".equalsIgnoreCase(schema);

        if (hasTenantSchema && orgId != null) {
            List<AuthIdentity> candidates = authIdentityRepository
                    .findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                            normalised, IdentityType.STAFF, orgId)
                    .stream()
                    .filter(identity -> Boolean.TRUE.equals(identity.getIsActive()))
                    .toList();
            if (candidates.size() == 1) {
                return Optional.of(candidates.get(0));
            }
            if (candidates.size() > 1) {
                log.error("Ambiguous STAFF identities for '{}' in organisationId={}: {} matches",
                        normalised, orgId, candidates.size());
                return Optional.empty();
            }

            // Tenant fallback: profile email may differ from username on older rows.
            return resolveByTenantProfileEmail(normalised, orgId);
        }

        // Public / platform context: prefer platform (org-null) identities.
        List<AuthIdentity> matches = authIdentityRepository.findAllByEmailOrUsername(normalised).stream()
                .filter(identity -> Boolean.TRUE.equals(identity.getIsActive()))
                .toList();
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        List<AuthIdentity> platform = matches.stream()
                .filter(a -> a.getOrganisation() == null)
                .toList();
        if (platform.size() == 1) {
            return Optional.of(platform.get(0));
        }
        if (matches.size() == 1) {
            return Optional.of(matches.get(0));
        }
        log.debug("Ambiguous identities for '{}' without tenant context: {} matches", normalised, matches.size());
        return Optional.empty();
    }

    private Optional<AuthIdentity> resolveByTenantProfileEmail(String normalisedEmail, Long orgId) {
        try {
            Optional<Long> authId = userRepository.findAuthIdByEmail(normalisedEmail);
            if (authId.isEmpty()) {
                // Legacy path: entity load (may fail under tenant-only search_path).
                authId = userRepository.findByEmail(normalisedEmail)
                        .map(user -> user.getAuthIdentity() != null ? user.getAuthIdentity().getId() : null);
            }
            if (authId.isEmpty() || authId.get() == null) {
                return Optional.empty();
            }
            Long resolvedAuthId = authId.get();
            if (!isIdentityLinkedToOrganisation(resolvedAuthId, orgId)) {
                log.debug("Tenant email resolved authId={} but it is not linked to organisationId={}",
                        resolvedAuthId, orgId);
                return Optional.empty();
            }
            return authIdentityRepository.findById(resolvedAuthId)
                    .filter(identity -> Boolean.TRUE.equals(identity.getIsActive()))
                    .filter(identity -> identity.getIdentityType() == IdentityType.STAFF);
        } catch (RuntimeException ex) {
            log.debug("Tenant email fallback lookup failed for organisationId={}", orgId, ex);
            return Optional.empty();
        }
    }

    private boolean isIdentityLinkedToOrganisation(Long authId, Long orgId) {
        if (authId == null || orgId == null) {
            return false;
        }
        if (userOrganisationRepository.existsByAuth_IdAndOrganisation_Id(authId, orgId)) {
            return true;
        }
        return authIdentityRepository.findById(authId)
                .map(AuthIdentity::getOrganisation)
                .map(org -> org != null && orgId.equals(org.getId()))
                .orElse(false);
    }

    private void addPermissionAuthorities(Set<GrantedAuthority> authorities, Role role) {
        if (role.getRolePermissions() == null) {
            return;
        }
        role.getRolePermissions().stream()
                .filter(rp -> rp != null && rp.getPermission() != null)
                .map(rp -> rp.getPermission())
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(Permission::getName)
                .filter(n -> n != null)
                .forEach(n -> authorities.add(new SimpleGrantedAuthority(n)));
    }
}
