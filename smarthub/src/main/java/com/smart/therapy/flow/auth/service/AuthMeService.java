package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.auth.dto.AuthMeResponse;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.user.dto.UserResponse;
import com.smart.therapy.flow.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthMeService {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public AuthMeResponse getCurrentUserContext(AuthPrincipal principal) {
        Objects.requireNonNull(principal, "Principal is required");

        List<String> authorities = principal.getAuthorities() == null
                ? List.of()
                : principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a != null && !a.isBlank())
                .distinct()
                .collect(Collectors.toList());

        List<String> roles = authorities.stream()
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .distinct()
                .collect(Collectors.toList());

        List<String> permissions = authorities.stream()
                .filter(a -> !a.startsWith("ROLE_"))
                .distinct()
                .collect(Collectors.toList());

        UserResponse user = null;
        AuthMeResponse.ClientSummary client = null;
        boolean isPublicSchema = isPublicSchema();

        if (principal.getIdentityType() == IdentityType.STAFF && !isPublicSchema
                && currentUserService.getCurrentUser(principal).isPresent()) {
            user = userService.getCurrentUser(principal);
            // In public/platform context, UserResponse roles can be empty due org-scoped mapping.
            // Normalize /auth/me to always reflect effective auth roles.
            if ((user.getRoles() == null || user.getRoles().isEmpty()) && !roles.isEmpty()) {
                user.setRoles(roles);
            }
        } else if (principal.getIdentityType() == IdentityType.CLIENT && !isPublicSchema) {
            client = currentUserService.getCurrentClient(principal)
                    .map(c -> AuthMeResponse.ClientSummary.builder()
                            .id(c.getId())
                            .fullName(c.getFullName())
                            .email(c.getPrimaryEmail())
                            .portalEmail(principal.getLoginIdentifier())
                            .phone(c.getPrimaryPhone())
                            .build())
                    .orElse(null);
        }

        return AuthMeResponse.builder()
                .authId(principal.getAuthId())
                .username(principal.getLoginIdentifier())
                .email(principal.getLoginIdentifier())
                .identityType(principal.getIdentityType() != null ? principal.getIdentityType().name() : null)
                .tenantSchema(TenantContext.getSchemaName())
                .organisationId(TenantContext.getOrganisationId())
                .roles(roles)
                .permissions(permissions)
                .authorities(authorities)
                .isPlatformAdmin(roles.contains("PLATFORM_SUPER_ADMIN"))
                .isTenantAdmin(roles.contains("ADMIN"))
                .isTherapist(roles.contains("THERAPIST"))
                .isSupervisor(roles.contains("SUPERVISOR"))
                .isClient(principal.getIdentityType() == IdentityType.CLIENT)
                .user(user)
                .client(client)
                .build();
    }

    private static boolean isPublicSchema() {
        String schema = TenantContext.getSchemaName();
        return schema == null || schema.isBlank() || "public".equalsIgnoreCase(schema);
    }
}
