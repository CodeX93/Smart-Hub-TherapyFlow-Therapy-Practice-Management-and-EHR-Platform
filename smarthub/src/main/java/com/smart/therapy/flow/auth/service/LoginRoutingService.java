package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.auth.dto.LoginContextResponse;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthIdentityRole;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.entity.Role;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.entity.UserOrganisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.superadmin.service.TenantResolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LoginRoutingService {

    private final OrganisationRepository organisationRepository;
    private final TenantResolutionService tenantResolutionService;
    private final SsoService ssoService;
    private final AuthIdentityRepository authIdentityRepository;
    private final AuthIdentityRoleRepository authIdentityRoleRepository;

    public LoginContextResponse resolveContext(String email, String orgSlug) {
        return resolveContext(email, orgSlug, IdentityType.STAFF);
    }

    /**
     * Resolve login context for a given identity type.
     * Staff login uses {@link IdentityType#STAFF}; client portal uses {@link IdentityType#CLIENT}.
     * This keeps staff/client org pickers from mixing identities that share the same email.
     */
    public LoginContextResponse resolveContext(String email, String orgSlug, IdentityType identityType) {
        String trimmedIdentifier = normalize(email);
        String trimmedSlug = normalize(orgSlug);
        IdentityType requiredType = identityType != null ? identityType : IdentityType.STAFF;

        if (!StringUtils.hasText(trimmedIdentifier) && !StringUtils.hasText(trimmedSlug)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "email, username, or orgSlug is required");
        }

        if (StringUtils.hasText(trimmedSlug)) {
            Organisation org = organisationRepository.findBySlug(trimmedSlug)
                    .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Organisation not found"));
            if (StringUtils.hasText(trimmedIdentifier)
                    && !tenantResolutionService.emailBelongsToOrganisation(
                            trimmedIdentifier, org.getId(), requiredType)) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "EMAIL_NOT_IN_ORG", "Identifier does not belong to organisation");
            }
            LoginContextResponse.OrgSummary summary =
                    toOrgSummaryFromOrg(org, trimmedIdentifier, null, requiredType);
            return LoginContextResponse.builder()
                    .email(trimmedIdentifier)
                    .orgSlug(org.getSlug())
                    .organisationId(org.getId())
                    .organisationName(org.getName())
                    .branding(toBranding(org))
                    .ssoProviders(ssoService.getEnabledProvidersForOrganisation(org.getId()))
                    .organisations(List.of(summary))
                    .count(1)
                    .resolvedAt(Instant.now())
                    .build();
        }

        List<UserOrganisation> rows = tenantResolutionService.resolveByEmail(trimmedIdentifier, requiredType);
        if (rows.isEmpty()) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "No organisation found for identifier");
        }
        if (rows.size() == 1) {
            UserOrganisation row = rows.get(0);
            Organisation org = row.getOrganisation();
            LoginContextResponse.OrgSummary summary = toOrgSummary(row, trimmedIdentifier, requiredType);
            return LoginContextResponse.builder()
                    .email(trimmedIdentifier)
                    .orgSlug(org.getSlug())
                    .organisationId(org.getId())
                    .organisationName(org.getName())
                    .branding(toBranding(org))
                    .ssoProviders(ssoService.getEnabledProvidersForOrganisation(org.getId()))
                    .organisations(List.of(summary))
                    .count(1)
                    .resolvedAt(Instant.now())
                    .build();
        }

        List<LoginContextResponse.OrgSummary> orgs = rows.stream()
                .map(row -> toOrgSummary(row, trimmedIdentifier, requiredType))
                .toList();
        return LoginContextResponse.builder()
                .email(trimmedIdentifier)
                .organisations(orgs)
                .count(orgs.size())
                .resolvedAt(Instant.now())
                .build();
    }

    private LoginContextResponse.OrgSummary toOrgSummary(
            UserOrganisation row, String email, IdentityType identityType) {
        Organisation org = row.getOrganisation();
        return toOrgSummaryFromOrg(org, email, row, identityType);
    }

    private LoginContextResponse.OrgSummary toOrgSummaryFromOrg(
            Organisation org, String email, UserOrganisation row, IdentityType identityType) {
        List<String> roles = resolveRolesForEmailInOrg(email, org != null ? org.getId() : null, row, identityType);
        String username = null;
        if (row != null && row.getAuth() != null && StringUtils.hasText(row.getAuth().getLoginIdentifier())) {
            username = row.getAuth().getLoginIdentifier();
        }
        return LoginContextResponse.OrgSummary.builder()
                .organisationId(org != null ? org.getId() : null)
                .name(org != null ? org.getName() : null)
                .slug(org != null ? org.getSlug() : null)
                .subdomain(org != null ? org.getSubdomain() : null)
                .status(org != null ? org.getStatus() : null)
                .branding(toBranding(org))
                .roles(roles)
                .username(username)
                .build();
    }

    private List<String> resolveRolesForEmailInOrg(
            String email, Long organisationId, UserOrganisation row, IdentityType identityType) {
        if (organisationId == null) {
            return List.of();
        }
        Set<String> roleNames = new LinkedHashSet<>();

        // Prefer the auth identity linked on the user_organisations row.
        if (row != null && row.getAuth() != null && row.getAuth().getId() != null) {
            if (identityType == null || row.getAuth().getIdentityType() == identityType) {
                roleNames.addAll(loadRoleNames(row.getAuth().getId(), organisationId));
            }
        }

        // Merge matching identities of the requested type for this org.
        if (StringUtils.hasText(email) && identityType != null) {
            for (AuthIdentity identity : authIdentityRepository
                    .findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                            email, identityType, organisationId)) {
                if (identity != null && identity.getId() != null && Boolean.TRUE.equals(identity.getIsActive())) {
                    roleNames.addAll(loadRoleNames(identity.getId(), organisationId));
                }
            }
        }

        // CLIENT portal accounts may have no RBAC role rows — surface a stable label for the picker.
        if (identityType == IdentityType.CLIENT && roleNames.isEmpty()) {
            roleNames.add("CLIENT");
        }
        return new ArrayList<>(roleNames);
    }

    private List<String> loadRoleNames(Long authId, Long organisationId) {
        List<String> names = new ArrayList<>();
        for (AuthIdentityRole air : authIdentityRoleRepository
                .findByAuthIdAndOrganisationIdWithRolesAndPermissions(authId, organisationId)) {
            Role role = air.getRole();
            if (role != null && Boolean.TRUE.equals(role.getIsActive()) && StringUtils.hasText(role.getName())) {
                names.add(role.getName());
            }
        }
        return names;
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private static LoginContextResponse.Branding toBranding(Organisation org) {
        if (org == null) {
            return null;
        }
        return LoginContextResponse.Branding.builder()
                .logoUrl(org.getLogoUrl())
                .brandPrimaryColor(org.getBrandPrimaryColor())
                .brandSecondaryColor(org.getBrandSecondaryColor())
                .brandAccentColor(org.getBrandAccentColor())
                .build();
    }
}
