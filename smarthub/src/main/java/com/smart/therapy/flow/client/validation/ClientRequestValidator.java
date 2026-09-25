package com.smart.therapy.flow.client.validation;

import com.smart.therapy.flow.client.dto.CreateClientRequest;
import com.smart.therapy.flow.client.dto.UpdateClientRequest;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientContact;
import com.smart.therapy.flow.client.enums.ContactType;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.service.AuthIdentityService;
import com.smart.therapy.flow.client.repository.ClientContactRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientSearchHelper;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.service.BlindIndexService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * Validates client creation and update requests.
 * 
 * This class encapsulates all validation logic for client requests,
 * ensuring business rules are enforced consistently.
 * 
 * Responsibilities:
 * - Required field validation
 * - Email uniqueness checks (ClientContact and Users)
 * - Portal email uniqueness checks
 * - Authorization checks (therapist assignment)
 * - Business rule enforcement
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ClientRequestValidator {

    private final ClientContactRepository contactRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final AuthIdentityService authIdentityService;
    private final OrganisationRepository organisationRepository;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;
    private final BlindIndexService blindIndexService;
    private final ClientSearchHelper clientSearchHelper;

    /**
     * Validate client creation request.
     * 
     * @param request The client creation request
     * @param requester The user making the request
     * @throws BadRequestException if validation fails
     * @throws ForbiddenException if authorization check fails
     */
    public void validateCreateRequest(CreateClientRequest request, AuthPrincipal requester) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Validate required fields
        validateRequiredFields(request);

        // Validate authorization
        validateTherapistAssignment(request, requester);

        // Validate email uniqueness
        validateEmailUniqueness(request);

        // Validate portal email uniqueness
        validatePortalEmailUniqueness(request);
    }

    /**
     * Validate client update request.
     * 
     * @param request The client update request
     * @param requester The user making the request
     * @param existingClient The existing client being updated
     * @throws BadRequestException if validation fails
     * @throws ForbiddenException if authorization check fails
     */
    public void validateUpdateRequest(UpdateClientRequest request, AuthPrincipal requester, Client existingClient) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");
        Objects.requireNonNull(existingClient, "Existing client is required");

        // Validate therapist assignment
        validateTherapistAssignmentForUpdate(request, requester, existingClient);

        // Validate email uniqueness (only if email is being changed)
        if (StringUtils.hasText(request.getEmail())) {
            validateEmailUniquenessForUpdate(request.getEmail(), existingClient.getId());
        }

        // Validate portal email uniqueness (only if portal email is being changed)
        if (StringUtils.hasText(request.getPortalEmail())) {
            validatePortalEmailUniquenessForUpdate(request.getPortalEmail(), existingClient.getId());
        }
    }

    /**
     * Validate required fields for client creation.
     */
    private void validateRequiredFields(CreateClientRequest request) {
        if (!StringUtils.hasText(request.getFullName())) {
            throw new BadRequestException("Full name is required");
        }
    }

    /**
     * Validate therapist assignment rules:
     * - Admins and Supervisors can assign any therapist (or leave null)
     * - Therapists can only assign themselves (or leave null to auto-assign themselves)
     * - Non-admins/supervisors cannot assign other therapists
     */
    private void validateTherapistAssignment(CreateClientRequest request, AuthPrincipal requester) {
        if (request.getAssignedTherapistId() == null) {
            return; // No therapist specified - will be auto-assigned if requester is therapist
        }

        // Admin or Supervisor can assign any therapist
        if (hasAdminRole(requester) || hasSupervisorRole(requester)) {
            return;
        }

        // Therapist can only assign themselves
        if (hasTherapistRole(requester)) {
            if (!request.getAssignedTherapistId().equals(currentUserService.requireCurrentUser(requester).getId())) {
                throw new ForbiddenException("Therapists can only assign themselves as the therapist");
            }
            return;
        }

        // Other roles cannot assign therapists
        throw new ForbiddenException("Only administrators, supervisors, and therapists can assign therapists");
    }

    /**
     * Validate therapist assignment rules for updates:
     * - Admins and Supervisors can assign any therapist (or leave null)
     * - Therapists can only assign themselves (if client has no therapist or is already assigned to them)
     * - Therapists cannot reassign a client that's assigned to another therapist
     */
    private void validateTherapistAssignmentForUpdate(UpdateClientRequest request, AuthPrincipal requester, Client existingClient) {
        if (request.getAssignedTherapistId() == null) {
            return; // No therapist specified - no change
        }

        Long existingTherapistId = existingClient.getAssignedTherapist() != null 
                ? existingClient.getAssignedTherapist().getId() 
                : null;
        Long requestedTherapistId = request.getAssignedTherapistId();

        // If no change, allow it
        if (Objects.equals(existingTherapistId, requestedTherapistId)) {
            return;
        }

        // Admin or Supervisor can assign any therapist
        if (hasAdminRole(requester) || hasSupervisorRole(requester)) {
            return;
        }

        Long requesterUserId = currentUserService.requireCurrentUser(requester).getId();

        // Therapist can only assign themselves
        if (hasTherapistRole(requester)) {
            // Therapist can assign themselves if:
            // 1. Client has no therapist (null), OR
            // 2. Client is already assigned to them
            if (existingTherapistId == null || existingTherapistId.equals(requesterUserId)) {
                // They can only assign themselves
                if (!requestedTherapistId.equals(requesterUserId)) {
                    throw new ForbiddenException("Therapists can only assign themselves as the therapist");
                }
                return;
            } else {
                // Client is assigned to another therapist - cannot reassign
                throw new ForbiddenException("Cannot reassign client that is already assigned to another therapist");
            }
        }

        // Other roles cannot assign therapists
        throw new ForbiddenException("Only administrators, supervisors, and therapists can assign therapists");
    }

    /**
     * Check if requester has admin role.
     */
    private boolean hasAdminRole(AuthPrincipal requester) {
        return permissionChecker.hasRole(requester, "ADMIN") || permissionChecker.hasRole(requester, "SUPER_ADMIN");
    }

    /**
     * Check if requester has therapist role.
     */
    private boolean hasTherapistRole(AuthPrincipal requester) {
        return permissionChecker.hasRole(requester, "THERAPIST");
    }

    /**
     * Check if requester has supervisor role.
     */
    private boolean hasSupervisorRole(AuthPrincipal requester) {
        return permissionChecker.hasRole(requester, "SUPERVISOR");
    }

    /**
     * Validate email uniqueness across ClientContact and Users tables.
     * Business Rule: Email must be unique across active clients and users.
     */
    private void validateEmailUniqueness(CreateClientRequest request) {
        if (!StringUtils.hasText(request.getEmail())) {
            return; // No email provided, skip validation
        }

        String email = request.getEmail().trim();
        for (ClientContact contact : findEmailContacts(email)) {
            if (contact.getContactType() == ContactType.EMAIL) {
                Client contactClient = contact.getClient();
                if (contactClient != null && !Boolean.TRUE.equals(contactClient.getIsDeleted())) {
                    throw new BadRequestException("Email already in use by an active client");
                }
            }
        }

        // Check if email exists in users table (for portal access)
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use by a user account");
        }

        Organisation org = currentOrganisation();
        if (org != null) {
            authIdentityService.assertEmailAvailable(
                    org, AuthIdentityService.normaliseLoginIdentifier(request.getEmail()), null);
        }
    }

    /**
     * Validate portal email uniqueness within the current organisation (shared pool with staff).
     */
    private void validatePortalEmailUniqueness(CreateClientRequest request) {
        if (!StringUtils.hasText(request.getPortalEmail())) {
            return;
        }
        String normalised = AuthIdentityService.normaliseLoginIdentifier(request.getPortalEmail());
        Organisation org = currentOrganisation();
        if (org != null) {
            authIdentityService.assertEmailAvailable(org, normalised, null);
        }
        if (userRepository.existsByEmail(request.getPortalEmail())) {
            throw new BadRequestException("Portal email already in use by a user account");
        }
    }

    /**
     * Validate email uniqueness for updates (excluding the current client).
     */
    private void validateEmailUniquenessForUpdate(String email, Long currentClientId) {
        if (!StringUtils.hasText(email)) {
            return;
        }

        for (ClientContact contact : findEmailContacts(email)) {
            if (contact.getContactType() == ContactType.EMAIL) {
                Client contactClient = contact.getClient();
                if (contactClient != null
                        && !Boolean.TRUE.equals(contactClient.getIsDeleted())
                        && !Objects.equals(contactClient.getId(), currentClientId)) {
                    throw new BadRequestException("Email already in use by another active client");
                }
            }
        }

        // Check if email exists in users table
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already in use by a user account");
        }

        Organisation org = currentOrganisation();
        if (org != null) {
            Long excludeAuthId = clientRepository.findById(currentClientId)
                    .map(Client::getAuthIdentity)
                    .map(ai -> ai != null ? ai.getId() : null)
                    .orElse(null);
            authIdentityService.assertEmailAvailable(
                    org, AuthIdentityService.normaliseLoginIdentifier(email), excludeAuthId);
        }
    }

    /**
     * Validate portal email uniqueness for updates (excluding the current client).
     */
    private void validatePortalEmailUniquenessForUpdate(String portalEmail, Long currentClientId) {
        if (!StringUtils.hasText(portalEmail)) {
            return;
        }
        String normalised = AuthIdentityService.normaliseLoginIdentifier(portalEmail);
        Organisation org = currentOrganisation();
        Long excludeAuthId = null;
        var currentClient = clientRepository.findById(currentClientId);
        if (currentClient.isPresent() && currentClient.get().getAuthIdentity() != null) {
            excludeAuthId = currentClient.get().getAuthIdentity().getId();
        }
        if (org != null) {
            authIdentityService.assertEmailAvailable(org, normalised, excludeAuthId);
        } else {
            var matches = authIdentityRepository.findAllByEmailOrUsername(normalised);
            for (var identity : matches) {
                if (excludeAuthId != null && excludeAuthId.equals(identity.getId())) {
                    continue;
                }
                var clientWithLogin = clientRepository.findByAuthId(identity.getId());
                if (clientWithLogin.isPresent() && !Objects.equals(clientWithLogin.get().getId(), currentClientId)) {
                    throw new BadRequestException("Portal email already in use by another active client");
                }
            }
        }
        if (userRepository.existsByEmail(portalEmail)) {
            throw new BadRequestException("Portal email already in use by a user account");
        }
    }

    private List<ClientContact> findEmailContacts(String email) {
        String normalized = AuthIdentityService.normaliseLoginIdentifier(email);
        List<ClientContact> contacts = List.of();
        if (blindIndexService.getSearchMode().writesBlindIndexes()) {
            byte[] digest = blindIndexService.compute(BlindIndexService.Kind.CONTACT, normalized);
            contacts = contactRepository.findByContactBlindIdx(digest);
        }
        if (contacts.isEmpty()
                && blindIndexService.getSearchMode() != BlindIndexService.SearchMode.BLIND_ONLY) {
            String encrypted = clientSearchHelper.encryptContactLookup(normalized);
            contacts = contactRepository.findByContactValue(encrypted);
            if (contacts.isEmpty()) {
                contacts = contactRepository.findByContactValue(normalized);
            }
            if (contacts.isEmpty()) {
                contacts = contactRepository.findByContactValue(email.trim());
            }
        }
        return contacts;
    }

    private Organisation currentOrganisation() {
        Long orgId = TenantContext.getOrganisationId();
        if (orgId == null) {
            return null;
        }
        return organisationRepository.findById(orgId).orElse(null);
    }
}

