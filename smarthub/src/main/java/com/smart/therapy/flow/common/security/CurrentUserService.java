package com.smart.therapy.flow.common.security;

import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientPortalSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Resolves current User or Client from AuthPrincipal (unified auth).
 * Use in services when you need the staff User or client Client entity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentUserService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ClientPortalSettingsService portalSettingsService;

    /**
     * Get the staff User for the given AuthPrincipal (STAFF identity).
     * Returns empty if principal is null or identity is not STAFF or user not found.
     */
    public Optional<User> getCurrentUser(AuthPrincipal principal) {
        if (principal == null || principal.getIdentityType() != IdentityType.STAFF) {
            return Optional.empty();
        }
        if (isPublicSchema()) {
            return Optional.empty();
        }
        try {
            return userRepository.findByAuthId(principal.getAuthId());
        } catch (RuntimeException ex) {
            log.warn("Unable to resolve staff profile for authId {}: {}", principal.getAuthId(), ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get the staff User or throw if not found. Use when the endpoint is staff-only.
     */
    public User requireCurrentUser(AuthPrincipal principal) {
        return getCurrentUser(principal)
                .orElseThrow(() -> new com.smart.therapy.flow.common.exception.ForbiddenException("Staff user not found"));
    }

    /**
     * Get the client Client for the given AuthPrincipal (CLIENT identity).
     */
    public Optional<Client> getCurrentClient(AuthPrincipal principal) {
        if (principal == null || principal.getIdentityType() != IdentityType.CLIENT) {
            return Optional.empty();
        }
        if (isPublicSchema()) {
            return Optional.empty();
        }
        try {
            return clientRepository.findByAuthId(principal.getAuthId());
        } catch (RuntimeException ex) {
            log.warn("Unable to resolve client profile for authId {}: {}", principal.getAuthId(), ex.getMessage());
            return Optional.empty();
        }
    }

    public Client requireCurrentClient(AuthPrincipal principal) {
        Client client = getCurrentClient(principal)
                .orElseThrow(() -> new com.smart.therapy.flow.common.exception.ForbiddenException("Client not found"));
        portalSettingsService.requirePortalAccessEnabled(client.getId());
        return client;
    }

    /**
     * Get current user id (staff) from AuthPrincipal. Returns null if not staff or not found.
     */
    public Long getCurrentUserId(AuthPrincipal principal) {
        return getCurrentUser(principal).map(User::getId).orElse(null);
    }

    /**
     * Get current client id from AuthPrincipal. Returns null if not client or not found.
     */
    public Long getCurrentClientId(AuthPrincipal principal) {
        return getCurrentClient(principal).map(Client::getId).orElse(null);
    }

    private static boolean isPublicSchema() {
        String schema = TenantContext.getSchemaName();
        return schema == null || schema.isBlank() || "public".equalsIgnoreCase(schema);
    }
}
