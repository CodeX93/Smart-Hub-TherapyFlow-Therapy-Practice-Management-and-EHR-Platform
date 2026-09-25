package com.smart.therapy.flow.client.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientPortalSettings;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.client.repository.ClientPortalSettingsRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.exception.ErrorCode;
import com.smart.therapy.flow.common.exception.PortalAccessDisabledException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Manages client portal preferences only. Auth (login/password) is in AuthIdentity.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClientPortalSettingsService {

    public static final String PORTAL_ACCESS_DISABLED_MESSAGE =
            ErrorCode.CLIENT_PORTAL_DISABLED.getMessage();

    private final ClientPortalSettingsRepository portalSettingsRepository;
    private final ClientRepository clientRepository;
    private final AuthIdentityRepository authIdentityRepository;

    @Transactional(readOnly = true)
    public ClientPortalSettings getByClientId(Long clientId) {
        return portalSettingsRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Portal settings not found for client"));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ClientPortalSettings> findByClientId(Long clientId) {
        return portalSettingsRepository.findByClientId(clientId);
    }

    @Transactional(readOnly = true)
    public boolean hasPortalAccess(Long clientId) {
        return portalSettingsRepository.findByClientId(clientId)
                .map(s -> Boolean.TRUE.equals(s.getHasPortalAccess()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isActivated(Long clientId) {
        return portalSettingsRepository.findByClientId(clientId)
                .map(s -> Boolean.TRUE.equals(s.getIsActivated()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isPortalAccessEnabled(Long clientId) {
        return portalSettingsRepository.findActiveByClientId(clientId).isPresent();
    }

    @Transactional(readOnly = true)
    public void requirePortalAccessEnabled(Long clientId) {
        if (!hasPortalAccess(clientId)) {
            throw new PortalAccessDisabledException();
        }
    }

    @Transactional
    public void disablePortalAccess(Long clientId) {
        setHasPortalAccess(clientId, false);
        setActivated(clientId, false, null);
        deactivatePortalIdentity(clientId);
    }

    /**
     * Portal settings gate the UI; the login itself lives in {@link AuthIdentity}. Without this the
     * identity stays active after access is revoked and keeps counting as a platform end user.
     */
    @Transactional
    public void deactivatePortalIdentity(Long clientId) {
        if (clientId == null) {
            return;
        }
        clientRepository.findByIdIncludingDeleted(clientId)
                .map(Client::getAuthIdentity)
                .filter(identity -> Boolean.TRUE.equals(identity.getIsActive()))
                .ifPresent(identity -> {
                    identity.setIsActive(false);
                    identity.setUpdatedAt(Instant.now());
                    authIdentityRepository.save(identity);
                    log.debug("Deactivated portal identity {} for client {}", identity.getId(), clientId);
                });
    }

    /**
     * Get or create portal settings for a client. New settings have hasPortalAccess=false, isActivated=false.
     */
    @Transactional
    public ClientPortalSettings getOrCreate(Long clientId) {
        return portalSettingsRepository.findByClientId(clientId)
                .map(existing -> {
                    // Keep inverse side in sync — Client.portalSettings has orphanRemoval=true.
                    syncClientReference(existing);
                    return existing;
                })
                .orElseGet(() -> {
                    Client client = clientRepository.findById(clientId)
                            .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
                    ClientPortalSettings settings = ClientPortalSettings.builder()
                            .client(client)
                            .hasPortalAccess(false)
                            .isActivated(false)
                            .emailNotifications(true)
                            .smsNotifications(false)
                            .pushNotifications(false)
                            .build();
                    ClientPortalSettings saved = portalSettingsRepository.save(settings);
                    client.setPortalSettings(saved);
                    return saved;
                });
    }

    /**
     * Ensures {@link Client#getPortalSettings()} points at the persisted row so a later
     * {@code clientRepository.save(client)} does not orphan-delete portal settings.
     */
    public void syncClientReference(ClientPortalSettings settings) {
        if (settings == null || settings.getClient() == null) {
            return;
        }
        Client client = settings.getClient();
        if (client.getPortalSettings() != settings) {
            client.setPortalSettings(settings);
        }
    }

    @Transactional
    public ClientPortalSettings save(ClientPortalSettings settings) {
        ClientPortalSettings saved = portalSettingsRepository.save(settings);
        syncClientReference(saved);
        return saved;
    }

    @Transactional
    public void setActivated(Long clientId, boolean activated, Instant activatedAt) {
        ClientPortalSettings settings = getOrCreate(clientId);
        settings.setIsActivated(activated);
        settings.setActivatedAt(activatedAt);
        save(settings);
        // Activation builds its response through another tenant read context, which
        // clears the current persistence context. Flush first so that clear cannot
        // discard activation while the password and verification token are saved.
        // This remains part of the caller's transaction and can still roll back.
        portalSettingsRepository.flush();
    }

    @Transactional
    public void setHasPortalAccess(Long clientId, boolean hasAccess) {
        ClientPortalSettings settings = getOrCreate(clientId);
        settings.setHasPortalAccess(hasAccess);
        save(settings);
    }

    @Transactional
    public void setAvatarUrl(Long clientId, String avatarUrl) {
        ClientPortalSettings settings = getOrCreate(clientId);
        settings.setAvatarUrl(avatarUrl);
        save(settings);
    }

    /** Online-session requests that no therapist digest has covered yet. */
    @Transactional(readOnly = true)
    public java.util.List<ClientPortalSettings> findPendingOnlineBookingRequests() {
        return portalSettingsRepository.findPendingOnlineBookingRequests();
    }

    /** When this therapist last received a digest, or null if never. */
    @Transactional(readOnly = true)
    public Instant findLastOnlineBookingDigestAt(Long therapistId) {
        return therapistId == null ? null : portalSettingsRepository.findLastDigestSentAt(therapistId);
    }

    @Transactional
    public void markOnlineBookingRequestsNotified(java.util.List<ClientPortalSettings> settings, Instant notifiedAt) {
        if (settings == null || settings.isEmpty()) {
            return;
        }
        settings.forEach(s -> s.setOnlineBookingNotifiedAt(notifiedAt));
        portalSettingsRepository.saveAll(settings);
    }
}
