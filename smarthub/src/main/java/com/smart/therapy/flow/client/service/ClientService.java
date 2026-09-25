package com.smart.therapy.flow.client.service;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthIdentityRole;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.entity.Role;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.util.DeviceInfoUtil;
import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.RoleRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.auth.service.AuthIdentityService;
import com.smart.therapy.flow.client.dto.*;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientHistory;
import com.smart.therapy.flow.client.entity.*;
import com.smart.therapy.flow.client.enums.*;
import com.smart.therapy.flow.client.repository.ClientHistoryRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.repository.ClientContactRepository;
import com.smart.therapy.flow.client.repository.ClientAddressRepository;
import com.smart.therapy.flow.client.repository.ClientInsuranceRepository;
import com.smart.therapy.flow.client.repository.ClientReferralRepository;
import com.smart.therapy.flow.client.repository.ClientEmploymentRepository;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.dto.PatchUpdates;
import com.smart.therapy.flow.common.service.BlindIndexService;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.common.service.EncryptionService;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ConflictException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.exception.StoryApiException;
import jakarta.persistence.criteria.JoinType;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CaseloadScope;
import com.smart.therapy.flow.common.security.CaseloadScopeService;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.notification.service.NotificationEventCatalog;
import com.smart.therapy.flow.notification.service.NotificationPayloadFactory;
import com.smart.therapy.flow.user.entity.SupervisorAssignment;
import com.smart.therapy.flow.user.repository.SupervisorAssignmentRepository;
import com.smart.therapy.flow.billing.repository.SessionBillingRepository;
import com.smart.therapy.flow.notification.entity.Notification;
import com.smart.therapy.flow.notification.entity.NotificationDeliveryLog;
import com.smart.therapy.flow.notification.enums.NotificationChannel;
import com.smart.therapy.flow.notification.enums.NotificationStatus;
import com.smart.therapy.flow.notification.repository.NotificationDeliveryLogRepository;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.entity.SessionIntegration;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.dto.SessionResponse;
import com.smart.therapy.flow.session.enums.SessionStatus;
import com.smart.therapy.flow.task.entity.ClientChecklist;
import com.smart.therapy.flow.task.repository.ClientChecklistRepository;
import com.smart.therapy.flow.report.entity.ClientReport;
import com.smart.therapy.flow.document.repository.DocumentRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Join;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.ApplicationEventPublisher;
import com.smart.therapy.flow.client.entity.IdempotencyKey;
import com.smart.therapy.flow.client.repository.IdempotencyKeyRepository;
import com.smart.therapy.flow.client.event.ClientCreatedEvent;
import com.smart.therapy.flow.client.event.ClientNotificationEvent;
import com.smart.therapy.flow.client.validation.ClientRequestValidator;
import com.smart.therapy.flow.system.service.SystemOptionCategories;
import com.smart.therapy.flow.system.service.SystemOptionKeyMatcher;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientService {

    private static final String RESOURCE_TYPE_CLIENT = "client";
    private static final int ACTIVATION_TOKEN_BYTES = 32;
    /** Match ClientHub communications log cap (most recent). */
    private static final int EMAIL_HISTORY_LIMIT = 100;
    private static final List<NotificationStatus> SUCCESSFUL_EMAIL_STATUSES = List.of(
            NotificationStatus.SENT,
            NotificationStatus.DELIVERED,
            NotificationStatus.READ);

    private final ClientRepository clientRepository;
    private final com.smart.therapy.flow.common.service.TimezoneService timezoneService;
    private final ClientHistoryRepository clientHistoryRepository;
    private final ClientHistoryTrackingService clientHistoryTrackingService;
    private final UserRepository userRepository;
    private final SupervisorAssignmentRepository supervisorAssignmentRepository;
    private final AuditLogService auditLogService;
    private final SessionRepository sessionRepository;
    private final SessionBillingRepository sessionBillingRepository;
    private final NotificationDeliveryLogRepository notificationDeliveryLogRepository;
    private final ClientChecklistRepository clientChecklistRepository;
    private final DocumentRepository documentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ClientRequestValidator requestValidator;
    private final SystemOptionResolverService systemOptionResolverService;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;
    private final CaseloadScopeService caseloadScopeService;

    // Normalized entity services
    private final ClientPortalSettingsService portalSettingsService;
    private final AuthIdentityService authIdentityService;
    private final AuthIdentityRepository authIdentityRepository;
    private final AuthIdentityRoleRepository authIdentityRoleRepository;
    private final RoleRepository roleRepository;
    private final OrganisationRepository organisationRepository;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final ClientContactService contactService;
    private final ClientAddressService addressService;
    private final ClientInsuranceService insuranceService;
    private final ClientReferralService referralService;
    private final ClientEmploymentService employmentService;

    // Normalized entity repositories (for restore operations that need to access
    // soft-deleted entities)
    private final ClientContactRepository contactRepository;
    private final ClientAddressRepository addressRepository;
    private final ClientInsuranceRepository insuranceRepository;
    private final ClientReferralRepository referralRepository;
    private final ClientEmploymentRepository employmentRepository;
    private final EncryptionService encryptionService;
    private final ClientSearchHelper clientSearchHelper;
    private final BlindIndexService blindIndexService;
    private final ClientMrnService clientMrnService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired(required = false)
    private EmailService emailService;

    // ObjectMapper for JSON serialization (for audit trail state snapshots)
    // JavaTimeModule is required to handle java.time.Instant and other Java 8 date/time types
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * NEW: Typed getClients using ClientFilter + Pageable.
     * Controller should prefer this overload.
     */
    @Transactional
    public PaginatedResponse<ClientSummaryResponse> getClients(
            ClientFilter filter,
            Pageable pageable,
            AuthPrincipal requester) {
        Objects.requireNonNull(filter, "Filter is required");
        Objects.requireNonNull(pageable, "Pageable is required");
        Objects.requireNonNull(requester, "Requester is required");

        Specification<Client> specification = buildClientSpecification(filter, requester);
        Page<Client> results = clientRepository.findAll(specification, pageable);

        List<ClientSummaryResponse> payload = toClientSummaryResponses(results.getContent());

        // Pageable is zero-based; API is one-based
        int page = pageable.getPageNumber() + 1;
        int pageSize = pageable.getPageSize();

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                "client_list_viewed", null, null, true,
                "resultCount=" + payload.size() + ", page=" + page + ", pageSize=" + pageSize);
        return PaginatedResponse.of(payload, results.getTotalElements(), page, pageSize);
    }

    @Transactional
    public PaginatedResponse<ClientSummaryResponse> getClients(
            int page,
            int pageSize,
            String search,
            String status,
            String stage,
            Long therapistId,
            String clientType,
            Boolean hasPortalAccess,
            Boolean hasPendingTasks,
            Boolean hasNoSessions,
            Boolean needsFollowUp,
            Boolean unassigned,
            String sortBy,
            String sortOrder,
            AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        // Build sort with primary and secondary sort fields
        Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String primarySortField = sortBy != null ? sortBy : "createdAt";

        // If sorting by fullName, add secondary sort by id for consistent ordering when
        // names are the same
        Sort sort;
        if ("fullName".equals(primarySortField)) {
            sort = Sort.by(direction, primarySortField)
                    .and(Sort.by(direction, "id")); // Secondary sort by id for consistent ordering
        } else {
            sort = Sort.by(direction, primarySortField);
        }

        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        Specification<Client> specification = buildClientSpecification(
                search, status, stage, therapistId, clientType, hasPortalAccess,
                hasPendingTasks, hasNoSessions, needsFollowUp, unassigned, requester);

        Page<Client> results = clientRepository.findAll(specification, pageable);

        List<ClientSummaryResponse> payload = toClientSummaryResponses(results.getContent());

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                "client_list_viewed", null, null, true,
                "resultCount=" + payload.size() + ", page=" + page + ", pageSize=" + pageSize);
        return PaginatedResponse.of(payload, results.getTotalElements(), page, pageSize);
    }

    /**
     * Full-detail variant of {@link #getClients(ClientFilter, Pageable, AuthPrincipal)} for
     * internal callers (e.g. admin directory) that need the complete {@link ClientResponse}
     * shape. Prefer the slim overload for anything backing the clients list UI.
     */
    @Transactional
    public PaginatedResponse<ClientResponse> getClientsWithFullDetails(
            ClientFilter filter,
            Pageable pageable,
            AuthPrincipal requester) {
        Objects.requireNonNull(filter, "Filter is required");
        Objects.requireNonNull(pageable, "Pageable is required");
        Objects.requireNonNull(requester, "Requester is required");

        Specification<Client> specification = buildClientSpecification(filter, requester);
        Page<Client> results = clientRepository.findAll(specification, pageable);

        List<ClientResponse> payload = results.getContent().stream()
                .map(this::toClientResponse)
                .collect(Collectors.toList());

        int page = pageable.getPageNumber() + 1;
        int pageSize = pageable.getPageSize();

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                "client_list_viewed", null, null, true,
                "resultCount=" + payload.size() + ", page=" + page + ", pageSize=" + pageSize);
        return PaginatedResponse.of(payload, results.getTotalElements(), page, pageSize);
    }

    @Transactional
    public PaginatedResponse<DirectoryClientSummary> getClientsForDirectory(
            ClientFilter filter,
            Pageable pageable,
            AuthPrincipal requester) {
        Objects.requireNonNull(filter, "Filter is required");
        Objects.requireNonNull(pageable, "Pageable is required");
        Objects.requireNonNull(requester, "Requester is required");

        Specification<Client> specification = buildClientSpecification(filter, requester);
        Page<Client> results = clientRepository.findAll(specification, pageable);

        List<DirectoryClientSummary> payload = results.getContent().stream()
                .map(this::toDirectoryClientSummary)
                .collect(Collectors.toList());

        int page = pageable.getPageNumber() + 1;
        int pageSize = pageable.getPageSize();

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                "client_list_viewed", null, null, true,
                "resultCount=" + payload.size() + ", page=" + page + ", pageSize=" + pageSize + ", variant=directory");
        return PaginatedResponse.of(payload, results.getTotalElements(), page, pageSize);
    }

    @Transactional
    public ClientResponse getClient(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client id is required");
        Objects.requireNonNull(requester, "Requester is required");

        try {
            // Try to load with therapist relationship to avoid LazyInitializationException
            Client client = clientRepository.findByIdWithTherapist(clientId)
                    .orElseGet(() -> clientRepository.findById(clientId)
                            .orElseThrow(() -> new ResourceNotFoundException("Client not found")));

            validateClientAccess(client, requester);
            recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                    "client_viewed", clientId, null, true);
            return toClientResponse(client, true);
        } catch (org.hibernate.LazyInitializationException e) {
            log.error("LazyInitializationException when getting client {}: {}", clientId, e.getMessage());
            // Retry with explicit loading
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
            // Load therapist relationship explicitly
            if (client.getAssignedTherapist() != null) {
                client.getAssignedTherapist().getFullName(); // Trigger lazy load
            }
            validateClientAccess(client, requester);
            recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                    "client_viewed", clientId, null, true);
            return toClientResponse(client);
        } catch (Exception e) {
            log.error("Error getting client {}: {}", clientId, e.getMessage(), e);
            throw new ResourceNotFoundException("Error retrieving client: " + e.getMessage());
        }
    }

    /**
     * Create a new client - Enterprise Spring Architecture Pattern.
     * This method does ONLY database work - fast, transactional, deadlock-safe.
     * Post-commit processing (history, audit, portal, notifications) happens via event listener.
     * 
     * Flow:
     * 0. Real Idempotency (database-backed)
     * 1. Basic validation only (no uniqueness checks - DB constraints handle that)
     * 2. Generate Client ID
     * 3-5. Transaction: Create Client, Related Entities, Credential Record
     * 6-8. Event published (handled AFTER_COMMIT by ClientCreatedListener)
     */
    @Transactional
    @CacheEvict(value = "clients", allEntries = true)
    public ClientResponse createClient(CreateClientRequest request, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        // STEP 0 — REAL Idempotency (database-backed, race-condition safe)
        if (StringUtils.hasText(request.getIdempotencyKey())) {
            Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByKey(request.getIdempotencyKey());
            
            if (existing.isPresent() && existing.get().getClientId() != null) {
                // Client already created with this key - return existing
                Client client = clientRepository.findById(existing.get().getClientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Client not found for idempotency key"));
                log.info("Idempotency: Returning existing client {} for key {}", 
                        client.getId(), request.getIdempotencyKey());
                return toClientResponse(client);
            }
            
            // Reserve the key (even if clientId is null - prevents race conditions)
            if (existing.isEmpty()) {
                idempotencyKeyRepository.save(IdempotencyKey.builder()
                        .key(request.getIdempotencyKey())
                        .clientId(null)
                        .build());
            }
        }

        // STEP 1 — Validate request (required fields, authorization, email uniqueness)
        requestValidator.validateCreateRequest(request, requester);

        // Plan limit is enforced against active client records, not metered usage.
        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null) {
            enforceClientLimitIfNeeded(orgId);
        }

        // STEP 2 — Generate Client ID
        String clientId = generateClientId();

        // STEP 3 — Create Core Client Record
        Client client = buildClientEntity(request, clientId, requester);
        // Persist scalar indexes first. Adding the orphan-removal name-token
        // collection before related-entity lookups trigger an early flush can
        // make Hibernate schedule the same token inserts twice.
        blindIndexService.updateClientScalarBlindIndexes(client);
        Client saved = clientRepository.save(client);
        Long savedId = requireClientId(saved);

        // STEP 4 — Create Related Entities (within same transaction)
        createNormalizedEntities(savedId, request);
        refreshBlindIndexesAfterContactChanges(savedId);
        // Cascade + dirty checking persist the token rows at commit: `saved` is the
        // managed instance returned by the insert above, so no second save is needed.
        blindIndexService.syncNameTokenBlindIndexes(saved);

        // STEP 5 — Portal activation email (same path as PUT /clients/{id}/portal-access)
        if (Boolean.TRUE.equals(request.getHasPortalAccess())) {
            String portalEmail = StringUtils.hasText(request.getPortalEmail())
                    ? request.getPortalEmail()
                    : request.getEmail();
            if (StringUtils.hasText(portalEmail)) {
                handlePortalActivation(saved, portalEmail, requester);
                if (request.getEmailNotifications() != null) {
                    ClientPortalSettings settings = portalSettingsService.getOrCreate(savedId);
                    settings.setEmailNotifications(request.getEmailNotifications());
                    portalSettingsService.save(settings);
                }
                // Do not clientRepository.save(saved) here: Client.portalSettings / employment /
                // insurance use orphanRemoval=true. Saving with those fields still null deletes
                // rows created above. Auth identity + settings are already persisted.
            }
        }

        // Link idempotency key to client
        if (StringUtils.hasText(request.getIdempotencyKey())) {
            idempotencyKeyRepository.updateClientId(request.getIdempotencyKey(), savedId);
        }

        // STEP 8 — Publish Event (handled AFTER_COMMIT by listener)
        eventPublisher.publishEvent(new ClientCreatedEvent(
                savedId,
                currentUserService.requireCurrentUser(requester).getId(),
                ipAddress,
                TenantContext.getOrganisationId(),
                TenantContext.getSchemaName()
        ));

        entityManager.flush();
        Client persisted = clientRepository.findById(savedId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found after create"));
        return toClientResponse(persisted);
    }

    /**
     * Build client entity from request.
     * Business Rule: If a therapist creates a client without specifying a therapist,
     * they are automatically assigned as the therapist.
     */
    private Client buildClientEntity(CreateClientRequest request, String clientId, AuthPrincipal requester) {
        // Convert option keys from request
        String clientStatus = parseStatus(request.getStatus());
        String clientStage = parseStage(request.getStage());
        String gender = parseGender(request.getGender());
        String maritalStatus = parseMaritalStatus(request.getMaritalStatus());
        String clientType = parseClientType(request.getClientType());
        String followUpPriority = parseFollowUpPriority(request.getPriority());

        // Determine assigned therapist
        User assignedTherapist = null;
        if (request.getAssignedTherapistId() != null) {
            // Therapist explicitly specified in request
            assignedTherapist = userRepository.findById(request.getAssignedTherapistId()).orElse(null);
        } else if (permissionChecker.hasPermission(requester, "CLIENT_VIEW_OWN") && !permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL")) {
            // User with CLIENT_VIEW_OWN (therapist) creating client - auto-assign themselves
            assignedTherapist = userRepository.findById(currentUserService.requireCurrentUser(requester).getId()).orElse(null);
            log.debug("Auto-assigning therapist {} to newly created client", currentUserService.requireCurrentUser(requester).getId());
        }
        // If requester is admin and no therapist specified, assignedTherapist remains null

        return Client.builder()
                .clientId(clientId)
                .fullName(request.getFullName().trim())
                .dateOfBirth(request.getDateOfBirth())
                .gender(gender)
                .maritalStatus(maritalStatus)
                .preferredLanguage(parsePreferredLanguage(request.getPreferredLanguage()))
                .pronouns(request.getPronouns())
                .timezone(request.getTimezone())
                .status(clientStatus)
                .stage(clientStage)
                .clientType(clientType)
                .assignedTherapist(assignedTherapist)
                .notes(resolveNotes(request))
                .serviceType(convertServiceType(request.getServiceType()))
                .serviceFrequency(convertServiceFrequency(request.getServiceFrequency()))
                .treatmentModality(parseTreatmentModality(request.getTreatmentModality()))
                .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now())
                .followUpPriority(followUpPriority)
                .followUpDate(request.getFollowUpDate())
                .followUpNotes(request.getFollowUpNotes())
                .needsFollowUp(resolveNeedsFollowUp(request.getNeedsFollowUp(), followUpPriority,
                        request.getFollowUpDate(), request.getFollowUpNotes()))
                .lastUpdateDate(Instant.now())
                .build();
    }

    private boolean resolveNeedsFollowUp(Boolean explicitNeedsFollowUp, String followUpPriority,
            LocalDate followUpDate, String followUpNotes) {
        if (explicitNeedsFollowUp != null) {
            return explicitNeedsFollowUp;
        }
        return followUpPriority != null || followUpDate != null || StringUtils.hasText(followUpNotes);
    }

    private String parseStatus(String status) {
        return systemOptionResolverService.parseOptionKey(
                SystemOptionCategories.CLIENT_STATUS, status, "active");
    }

    private String parseStage(String stage) {
        return systemOptionResolverService.parseOptionKey(
                SystemOptionCategories.CLIENT_STAGE, stage,
                systemOptionResolverService.resolveDefaultOptionKey(SystemOptionCategories.CLIENT_STAGE) != null
                        ? systemOptionResolverService.resolveDefaultOptionKey(SystemOptionCategories.CLIENT_STAGE)
                        : "intake");
    }

    private String resolveStageKey(String stage) {
        return systemOptionResolverService.requireOptionKey(SystemOptionCategories.CLIENT_STAGE, stage);
    }

    /**
     * A client who has never chosen a timezone follows the clinic, so return that rather
     * than a blank the caller has to interpret. The stored value stays null in that case,
     * which is what lets the client move with the clinic if its timezone changes.
     */
    private String resolveClientTimezone(Client client) {
        String own = client.getTimezone();
        if (own != null && !own.isBlank()) {
            return own.trim();
        }
        return timezoneService.getPracticeTimezone().getId();
    }

    private String resolveStageLabel(String stageKey) {
        return resolveOptionLabel(SystemOptionCategories.CLIENT_STAGE, stageKey);
    }

    private String parseOptionOrNull(String categoryKey, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return systemOptionResolverService.resolveOptionKey(categoryKey, value);
    }

    private String resolveOptionLabel(String categoryKey, String optionKey) {
        if (!StringUtils.hasText(optionKey)) {
            return null;
        }
        return systemOptionResolverService.resolveOptionLabel(categoryKey, optionKey);
    }

    private String parseGender(String gender) {
        return parseOptionOrNull(SystemOptionCategories.GENDER, gender);
    }

    private String parseMaritalStatus(String maritalStatus) {
        return parseOptionOrNull(SystemOptionCategories.MARITAL_STATUS, maritalStatus);
    }

    private String parseClientType(String clientType) {
        return parseOptionOrNull(SystemOptionCategories.CLIENT_TYPE, clientType);
    }

    private String convertServiceType(String value) {
        return parseOptionOrNull(SystemOptionCategories.SERVICE_TYPE, value);
    }

    private String convertServiceFrequency(String value) {
        return parseOptionOrNull(SystemOptionCategories.SERVICE_FREQUENCY, value);
    }

    private String convertReferralSource(String value) {
        return parseOptionOrNull(SystemOptionCategories.REFERRAL_SOURCES, value);
    }

    private String convertEmploymentStatus(String value) {
        return parseOptionOrNull(SystemOptionCategories.EMPLOYMENT_STATUS, value);
    }

    private String convertEducationLevel(String value) {
        return parseOptionOrNull(SystemOptionCategories.EDUCATION_LEVEL, value);
    }

    private String parseFollowUpPriority(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return systemOptionResolverService.parseOptionKey(
                SystemOptionCategories.TASK_PRIORITY, value, "medium");
    }

    private String parsePreferredLanguage(String value) {
        return parseOptionOrNull(SystemOptionCategories.PREFERRED_LANGUAGE, value);
    }

    private String parseClientSource(String value) {
        return parseOptionOrNull(SystemOptionCategories.CLIENT_SOURCE, value);
    }

    private String parseInsuranceProvider(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return systemOptionResolverService.parseOptionKey(
                SystemOptionCategories.INSURANCE_PROVIDERS, value, value.trim());
    }

    private String requireInsuranceProvider(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return systemOptionResolverService.requireOptionKey(
                SystemOptionCategories.INSURANCE_PROVIDERS, value);
    }

    private String parseInsuranceType(String value) {
        return parseOptionOrNull(SystemOptionCategories.INSURANCE_TYPES, value);
    }

    private String requireInsuranceType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return systemOptionResolverService.requireOptionKey(
                SystemOptionCategories.INSURANCE_TYPES, value);
    }

    private String parseTreatmentModality(String value) {
        return parseOptionOrNull(SystemOptionCategories.TREATMENT_MODALITIES, value);
    }

    private String resolveNotes(CreateClientRequest request) {
        if (StringUtils.hasText(request.getNotes())) {
            return request.getNotes();
        }
        return request.getGeneralNotes();
    }

    @Transactional
    @CacheEvict(value = "clients", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey(#clientId)")
    public ClientResponse updateClient(Long clientId, UpdateClientRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(clientId, "Client id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        validateClientAccess(client, requester);

        boolean originalHasPortalAccess = portalSettingsService.findByClientId(clientId)
                .map(s -> Boolean.TRUE.equals(s.getHasPortalAccess()))
                .orElse(false);

        // Capture original state for audit trail (before any changes)
        String beforeState = serializeClientState(client);
        
        // Store original values for history tracking (before modifying client)
        String originalStatus = client.getStatus();
        String originalStage = client.getStage();
        User originalTherapist = client.getAssignedTherapist();

        sanitizeUpdateClientRequest(request, requester);

        // Validate update request (therapist assignment, email uniqueness)
        requestValidator.validateUpdateRequest(request, requester, client);

        // Auto-update stage when closing file
        if (isInactiveStatusValue(request.getStatus()) && !StringUtils.hasText(request.getStage())) {
            request.setStage("closed");
            request.markFieldPresent("stage");
        }

        // Update fields (explicit null in PATCH body clears nullable fields)
        PatchUpdates.applyString(request, "fullName", request.getFullName(), client::setFullName);
        PatchUpdates.apply(request, "dateOfBirth", request.getDateOfBirth(), client::setDateOfBirth);
        PatchUpdates.applyString(request, "timezone", request.getTimezone(), client::setTimezone);
        if (request.isFieldPresent("gender")) {
            client.setGender(StringUtils.hasText(request.getGender()) ? parseGender(request.getGender()) : null);
        }
        if (request.isFieldPresent("maritalStatus")) {
            if (!StringUtils.hasText(request.getMaritalStatus())) {
                client.setMaritalStatus(null);
            } else {
                String maritalStatusKey = parseOptionOrNull(SystemOptionCategories.MARITAL_STATUS, request.getMaritalStatus());
                if (maritalStatusKey == null) {
                    throw new BadRequestException("Invalid maritalStatus value: '" + request.getMaritalStatus() + "'");
                }
                client.setMaritalStatus(maritalStatusKey);
            }
        }
        if (request.isFieldPresent("preferredLanguage")) {
            if (!StringUtils.hasText(request.getPreferredLanguage())) {
                client.setPreferredLanguage(null);
            } else {
                String languageKey = systemOptionResolverService.requireOptionKey(
                        SystemOptionCategories.PREFERRED_LANGUAGE, request.getPreferredLanguage());
                client.setPreferredLanguage(languageKey);
            }
        }
        PatchUpdates.applyString(request, "pronouns", request.getPronouns(), client::setPronouns);
        if (request.isFieldPresent("status")) {
            if (!StringUtils.hasText(request.getStatus())) {
                throw new BadRequestException("status cannot be cleared");
            }
            String newStatusKey = systemOptionResolverService.requireOptionKey(
                    SystemOptionCategories.CLIENT_STATUS, request.getStatus());

            if (client.getStatus() != null && Objects.equals(client.getStatus(), newStatusKey)) {
                log.debug("No-op status update for client {}: {}", clientId, newStatusKey);
            } else {
                if (SystemOptionKeyMatcher.matchesAny(newStatusKey, "active")
                        && SystemOptionKeyMatcher.matchesAny(originalStatus, "inactive")) {
                    assertCanReopenClientFile(requester, client);
                }
                client.setStatus(newStatusKey);
            }
        }
        if (request.isFieldPresent("stage")) {
            if (!StringUtils.hasText(request.getStage())) {
                throw new BadRequestException("stage cannot be cleared");
            }
            String newStageKey = resolveStageKey(request.getStage());

            if (client.getStage() != null && Objects.equals(client.getStage(), newStageKey)) {
                log.debug("No-op stage update for client {}: {}", clientId, newStageKey);
            } else {
                client.setStage(newStageKey);
            }
        }
        if (request.isFieldPresent("clientType")) {
            client.setClientType(StringUtils.hasText(request.getClientType())
                    ? parseClientType(request.getClientType())
                    : null);
        }
        if (request.isFieldPresent("assignedTherapistId")) {
            if (request.getAssignedTherapistId() != null) {
                User therapist = userRepository.findById(request.getAssignedTherapistId()).orElse(null);
                client.setAssignedTherapist(therapist);
            } else {
                client.setAssignedTherapist(null);
            }
        } else if (permissionChecker.hasPermission(requester, "CLIENT_VIEW_OWN")
                && !permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL")) {
            // Therapist with CLIENT_VIEW_OWN: preserve existing assignment when field omitted
        }

        if (request.isFieldPresent("notes")) {
            client.setNotes(request.getNotes());
        } else if (request.isFieldPresent("generalNotes")) {
            client.setNotes(request.getGeneralNotes());
        }
        if (request.isFieldPresent("serviceType")) {
            client.setServiceType(StringUtils.hasText(request.getServiceType())
                    ? convertServiceType(request.getServiceType())
                    : null);
        }
        if (request.isFieldPresent("serviceFrequency")) {
            client.setServiceFrequency(StringUtils.hasText(request.getServiceFrequency())
                    ? convertServiceFrequency(request.getServiceFrequency())
                    : null);
        }
        if (request.isFieldPresent("treatmentModality")) {
            if (!StringUtils.hasText(request.getTreatmentModality())) {
                client.setTreatmentModality(null);
            } else {
                String treatmentModalityKey = parseTreatmentModality(request.getTreatmentModality());
                if (treatmentModalityKey == null) {
                    throw new BadRequestException(
                            "Invalid treatmentModality value: '" + request.getTreatmentModality() + "'");
                }
                client.setTreatmentModality(treatmentModalityKey);
            }
        }
        if (request.isAnyFieldPresent("startDate", "startdate", "start_date")) {
            client.setStartDate(request.getStartDate());
        }
        if (request.isFieldPresent("priority")) {
            client.setFollowUpPriority(StringUtils.hasText(request.getPriority())
                    ? parseFollowUpPriority(request.getPriority())
                    : null);
        }
        if (request.isAnyFieldPresent("followUpDate", "dueDate")) {
            client.setFollowUpDate(request.getFollowUpDate());
        }
        PatchUpdates.apply(request, "followUpNotes", request.getFollowUpNotes(), client::setFollowUpNotes);
        PatchUpdates.apply(request, "needsFollowUp", request.getNeedsFollowUp(), client::setNeedsFollowUp);
        if (!request.isFieldPresent("needsFollowUp")
                && (request.isFieldPresent("priority") || request.isFieldPresent("followUpDate")
                || request.isFieldPresent("dueDate") || request.isFieldPresent("followUpNotes"))) {
            client.setNeedsFollowUp(resolveNeedsFollowUp(null, parseFollowUpPriority(request.getPriority()),
                    request.getFollowUpDate(), request.getFollowUpNotes()));
        }

        // Portal access is now handled via ClientPortalSettings + AuthIdentity
        // No need to update deprecated fields

        client.setLastUpdateDate(Instant.now());

        blindIndexService.updateBlindIndexes(client, null);
        Client updated = Objects.requireNonNull(clientRepository.save(client), "Persisted client must not be null");
        Long updatedId = requireClientId(updated);

        // Flush + clear so the client row is written to DB before contact upsert runs.
        // Without this, Hibernate flushes both together and hits the blind-index unique
        // constraint on the clients table (self-collision on the same row).
        entityManager.flush();
        entityManager.clear();

        // Update normalized entities
        updateNormalizedEntities(updatedId, request, requester);
        refreshBlindIndexesAfterContactChanges(updatedId);

        // Capture after state for audit trail
        String afterState = serializeClientState(updated);

        // Determine changed fields for audit trail
        List<String> changedFields = determineChangedFields(beforeState, afterState);

        // Track history for changes (use original values captured before update)
        Client originalClientSnapshot = new Client();
        originalClientSnapshot.setId(clientId);
        originalClientSnapshot.setStatus(originalStatus);
        originalClientSnapshot.setStage(originalStage);
        originalClientSnapshot.setAssignedTherapist(originalTherapist);
        
        trackClientChanges(originalClientSnapshot, updated, requester);

        // Send activation email when portal access is newly enabled (after normalized updates)
        if (Boolean.TRUE.equals(request.getHasPortalAccess()) && !originalHasPortalAccess) {
            String email = request.getPortalEmail() != null ? request.getPortalEmail()
                    : getClientEmailForPortal(updated);
            if (email != null) {
                handlePortalActivation(updated, email, requester);
                // Avoid clientRepository.save here — orphanRemoval on portalSettings would
                // delete settings if the inverse side was not loaded on this entity.
                afterState = serializeClientState(updated);
            }
        }

        Long actorId = resolveActorId(requester);

        // Audit log with before/after states (Golden Rule 3)
        recordAuditEventWithStates(actorId, "client_updated", updatedId, ipAddress, true,
                beforeState, afterState, changedFields);

        // Publish via the application event bus (handled AFTER_COMMIT by ClientNotificationListener).
        try {
            Map<String, Object> eventData = buildClientEventData(updated);
            eventData.put("assignedToId", actorId);
            eventPublisher.publishEvent(new ClientNotificationEvent(
                    NotificationEventCatalog.CLIENT_UPDATED,
                    eventData,
                    TenantContext.getOrganisationId(),
                    TenantContext.getSchemaName()
            ));
        } catch (Exception e) {
            log.error("Failed to publish client_updated notification event", e);
        }

        entityManager.flush();
        Client persisted = clientRepository.findById(updatedId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found after update"));
        return toClientResponse(persisted);
    }

    @Transactional
    @CacheEvict(value = "clients", allEntries = true)
    public void deleteClient(Long clientId, AuthPrincipal requester, String ipAddress) {
        deleteClient(clientId, requester, ipAddress, null);
    }

    public void deleteClient(Long clientId, AuthPrincipal requester, String ipAddress, String userAgent) {
        Objects.requireNonNull(clientId, "Client id is required");
        Objects.requireNonNull(requester, "Requester is required");

        if (!permissionChecker.hasPermission(requester, "CLIENT_DELETE")) {
            throw new ForbiddenException("You do not have permission to delete clients");
        }

        // Find client including deleted (to check if already deleted)
        Client client = clientRepository.findByIdIncludingDeleted(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        // Check if already deleted
        if (Boolean.TRUE.equals(client.getIsDeleted())) {
            throw new BadRequestException("Client is already deleted");
        }

        // Check for related records (simplified - could check sessions, notes, etc.)
        if (!client.getHistory().isEmpty()) {
            log.warn("Soft deleting client {} with {} history records", clientId, client.getHistory().size());
        }

        // Soft delete the client (HIPAA/GDPR compliance)
        Instant deletedAt = Instant.now();
        client.setIsDeleted(true);
        client.setDeletedAt(deletedAt);
        client.setNextAppointmentDate(null);
        clientRepository.save(client);

        // A deleted client must not keep a working portal login (and must stop counting as an end user).
        AuthIdentity portalIdentity = client.getAuthIdentity();
        if (portalIdentity != null && Boolean.TRUE.equals(portalIdentity.getIsActive())) {
            portalIdentity.setIsActive(false);
            portalIdentity.setUpdatedAt(deletedAt);
            authIdentityRepository.save(portalIdentity);
        }

        // Cancel and soft-delete open schedule rows so calendars / conflict checks
        // do not keep showing a deleted client's appointments.
        int closedSessions = closeOpenSessionsForDeletedClient(clientId, deletedAt);
        if (closedSessions > 0) {
            log.info("Closed {} open session(s) while soft-deleting client {}", closedSessions, clientId);
        }

        // Flush to ensure deletion is persisted immediately
        clientRepository.flush();

        // Audit log — link client FK + MRN/device context for the detail view
        recordClientDeletedAudit(requester, client, ipAddress, userAgent, closedSessions, deletedAt);

        // Publish via the application event bus (handled AFTER_COMMIT by ClientNotificationListener).
        try {
            Map<String, Object> eventData = buildClientEventData(client);
            eventData.put("deletedAt", client.getDeletedAt());
            eventData.put("assignedToId", currentUserService.requireCurrentUser(requester).getId());
            eventPublisher.publishEvent(new ClientNotificationEvent(
                    NotificationEventCatalog.CLIENT_DELETED,
                    eventData,
                    TenantContext.getOrganisationId(),
                    TenantContext.getSchemaName()
            ));
        } catch (Exception e) {
            log.error("Failed to publish client_deleted notification event", e);
        }
    }

    private void recordClientDeletedAudit(AuthPrincipal requester, Client client, String ipAddress,
            String userAgent, int closedSessions, Instant deletedAt) {
        Long actorId = currentUserService.requireCurrentUser(requester).getId();
        Long clientPk = client.getId();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("internal_client_id", clientPk);
        details.put("client_mrn", client.getClientId());
        details.put("deleted_at", deletedAt != null ? deletedAt.toString() : null);
        details.put("closed_sessions", closedSessions);
        details.put("status", client.getStatus());
        details.put("stage", client.getStage());
        details.put("soft_delete", true);
        if (StringUtils.hasText(userAgent) && !"unknown".equalsIgnoreCase(userAgent.trim())) {
            details.put("user_agent", userAgent);
            details.put("device_label", DeviceInfoUtil.deviceLabel(userAgent));
            if (requester.getAuthId() != null) {
                details.put("device_fingerprint", DeviceInfoUtil.fingerprintHash(requester.getAuthId(), userAgent));
            }
        }
        String detailsJson;
        try {
            detailsJson = objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            log.warn("Failed to serialize client_deleted audit details", e);
            detailsJson = "internal_client_id=" + clientPk + ", client_mrn=" + client.getClientId();
        }
        try {
            auditLogService.recordStaffEvent(
                    actorId,
                    "client_deleted",
                    RESOURCE_TYPE_CLIENT,
                    clientPk,
                    clientPk,
                    ipAddress,
                    true,
                    detailsJson,
                    userAgent);
        } catch (Exception e) {
            log.error("Failed to record audit event for client: {}", clientPk, e);
        }
    }

    /**
     * Cancel and soft-delete open (non-terminal) sessions for a soft-deleted client.
     * Completed / already-cancelled history is left intact for restore and audit paths.
     */
    private int closeOpenSessionsForDeletedClient(Long clientId, Instant deletedAt) {
        List<Session> openSessions = sessionRepository.findByClientId(clientId);
        if (openSessions.isEmpty()) {
            return 0;
        }

        int closed = 0;
        for (Session session : openSessions) {
            if (isTerminalSessionStatus(session.getStatus()) || session.getBilling() != null) {
                continue;
            }
            session.setStatus("cancelled");
            session.setIsDeleted(true);
            session.setDeletedAt(deletedAt);
            sessionRepository.save(session);
            closed++;
        }
        return closed;
    }

    private static boolean isTerminalSessionStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        return SystemOptionKeyMatcher.matchesAny(status, "completed", "cancelled", "no-show");
    }

    /**
     * Restore a soft-deleted client.
     * <p>
     * Only administrators can restore clients. This operation will:
     * - Verify the client is currently soft-deleted
     * - Ensure restoring will not violate uniqueness constraints for email,
     * portalEmail, or clientId
     * - Mark the client as active (isDeleted = false, deletedAt = null)
     */
    @Transactional
    @CacheEvict(value = "clients", allEntries = true)
    public ClientResponse restoreClient(Long clientId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(clientId, "Client id is required");
        Objects.requireNonNull(requester, "Requester is required");

        if (!permissionChecker.hasPermission(requester, "CLIENT_DELETE")) {
            throw new ForbiddenException("You do not have permission to restore clients");
        }

        // Load client including soft-deleted records
        Client client = clientRepository.findByIdIncludingDeleted(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        // Ensure client is currently deleted
        if (!Boolean.TRUE.equals(client.getIsDeleted())) {
            throw new BadRequestException("Client is not deleted and cannot be restored");
        }

        // Check for potential uniqueness conflicts before restoring
        // Use normalized entities first, then fallback to deprecated fields
        String email = null;
        String portalEmail = null;
        String clientIdentifier = client.getClientId();

        // Get email from normalized entities
        try {
            Optional<ClientContact> primaryEmail = contactService.getPrimaryEmail(clientId);
            if (primaryEmail.isPresent() && StringUtils.hasText(primaryEmail.get().getContactValue())) {
                email = primaryEmail.get().getContactValue();
            }
        } catch (Exception e) {
            log.warn("Error getting primary email for client {}: {}", clientId, e.getMessage());
        }

        // Get portal email from AuthIdentity (login identifier)
        if (client.getAuthIdentity() != null && StringUtils.hasText(client.getAuthIdentity().getLoginIdentifier())) {
            portalEmail = client.getAuthIdentity().getLoginIdentifier();
        }

        // 1) Client ID conflict (another active client already using this clientId)
        if (StringUtils.hasText(clientIdentifier) && clientMrnService.existsMrn(clientIdentifier, false)) {
            throw new ConflictException(
                    "Cannot restore client because the client ID is already used by another active client");
        }

        // 2) Email conflict - check normalized entities first
        if (StringUtils.hasText(email)) {
            // Check if email exists in normalized ClientContact for other active clients
            // Note: We check all clients' contacts, but this is a simplified check
            // A more efficient approach would be to add a repository method
            boolean emailInNormalizedContacts = false;
            try {
                // Check if any other active client has this email as primary contact
                List<Client> allClients = clientRepository.findAll();
                for (Client c : allClients) {
                    if (c.getId().equals(clientId) || Boolean.TRUE.equals(c.getIsDeleted())) {
                        continue;
                    }
                    Optional<ClientContact> primaryEmail = contactService.getPrimaryEmail(c.getId());
                    if (primaryEmail.isPresent() && email.equalsIgnoreCase(primaryEmail.get().getContactValue())) {
                        emailInNormalizedContacts = true;
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("Error checking normalized contacts during restore: {}", e.getMessage());
            }

            boolean emailInUsers = userRepository.existsByEmail(email);

            if (emailInNormalizedContacts) {
                throw new ConflictException(
                        "Cannot restore client because the email is already used by another active client");
            }
            if (emailInUsers) {
                throw new ConflictException(
                        "Cannot restore client because the email is already used by a user account");
            }
        }

        // 3) Portal email conflict - check auth_identities (CLIENT) and users
        if (StringUtils.hasText(portalEmail)) {
            String normalised = AuthIdentityService.normaliseLoginIdentifier(portalEmail);
            Optional<AuthIdentity> existingIdentity = authIdentityRepository.findByNormalisedLoginIdentifierAndIdentityType(normalised, IdentityType.CLIENT);
            if (existingIdentity.isPresent()) {
                Optional<Client> otherClientOpt = clientRepository.findByAuthId(existingIdentity.get().getId());
                if (otherClientOpt.isPresent() && !otherClientOpt.get().getId().equals(clientId)
                        && !Boolean.TRUE.equals(otherClientOpt.get().getIsDeleted())) {
                    throw new ConflictException(
                            "Cannot restore client because the portal email is already used by another active client");
                }
            }

            // Check if portal email exists in users table
            boolean portalEmailInUsers = userRepository.existsByEmail(portalEmail);
            if (portalEmailInUsers) {
                throw new ConflictException(
                        "Cannot restore client because the portal email is already used by a user account");
            }
        }

        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null) {
            enforceClientLimitIfNeeded(orgId);
        }

        // Restore the client
        client.setIsDeleted(false);
        client.setDeletedAt(null);
        client.setLastUpdateDate(Instant.now());

        Client restored = clientRepository.save(client);
        Long restoredId = requireClientId(restored);

        // Restore normalized entities (CRITICAL: ensure all associated entities are
        // linked)
        // Note: Normalized entities (ClientContact, ClientAddress, etc.) don't have
        // soft-delete.
        // They remain in the database when a client is soft-deleted, so we just need to
        // verify
        // they exist and are properly linked to the restored client.

        // 1. Verify portal settings / auth identity exist for restored client (no-op; created on portal activation)
        if (portalSettingsService.findByClientId(restoredId).isPresent()) {
            log.debug("ClientPortalSettings exists for restored client {}", restoredId);
        } else {
            log.debug("No ClientPortalSettings for restored client {} - created on portal activation", restoredId);
        }

        // 2. Verify ClientContact entities exist
        try {
            List<ClientContact> contacts = contactService.getContacts(restoredId);
            log.debug("Found {} ClientContact entities for restored client {}", contacts.size(), restoredId);
        } catch (Exception e) {
            log.warn("Error checking ClientContact entities for restored client {}: {}", restoredId, e.getMessage());
        }

        // 3. Verify ClientAddress entities exist
        try {
            List<ClientAddress> addresses = addressRepository.findByClientId(restoredId);
            log.debug("Found {} ClientAddress entities for restored client {}", addresses.size(), restoredId);
        } catch (Exception e) {
            log.warn("Error checking ClientAddress entities for restored client {}: {}", restoredId, e.getMessage());
        }

        // 4. Verify ClientInsurance exists
        try {
            // Use repository to check if insurance exists for this client
            Optional<ClientInsurance> insurance = insuranceRepository.findByClientId(restoredId);
            if (insurance.isPresent()) {
                log.debug("ClientInsurance exists for restored client {}", restoredId);
            } else {
                log.debug("No ClientInsurance found for restored client {}", restoredId);
            }
        } catch (Exception e) {
            log.warn("Error checking ClientInsurance for restored client {}: {}", restoredId, e.getMessage());
        }

        // 5. Verify ClientReferral exists
        try {
            // Use repository to check if referral exists for this client
            Optional<ClientReferral> referral = referralRepository.findByClientId(restoredId);
            if (referral.isPresent()) {
                log.debug("ClientReferral exists for restored client {}", restoredId);
            } else {
                log.debug("No ClientReferral found for restored client {}", restoredId);
            }
        } catch (Exception e) {
            log.warn("Error checking ClientReferral for restored client {}: {}", restoredId, e.getMessage());
        }

        // 6. Verify ClientEmployment exists
        try {
            // Use repository to check if employment exists for this client
            Optional<ClientEmployment> employment = employmentRepository.findByClientId(restoredId);
            if (employment.isPresent()) {
                log.debug("ClientEmployment exists for restored client {}", restoredId);
            } else {
                log.debug("No ClientEmployment found for restored client {}", restoredId);
            }
        } catch (Exception e) {
            log.warn("Error checking ClientEmployment for restored client {}: {}", restoredId, e.getMessage());
        }

        // Track history for restoration
        String restoredStageValue = resolveStageLabel(restored.getStage());
        if (!StringUtils.hasText(restoredStageValue)) {
            restoredStageValue = "Intake";
        }
        trackClientHistory(restoredId, ClientEventType.RESTORED.getDisplayName(), null, restoredStageValue,
                "Client file restored from soft delete (including all normalized entities)", currentUserService.requireCurrentUser(requester).getId(),
                requester.getLoginIdentifier());

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "client_restored", restoredId, ipAddress, true);

        return toClientResponse(restored);
    }

    private void enforceClientLimitIfNeeded(Long organisationId) {
        Integer clientLimit = subscriptionFeatureService.getEffectiveLimit(
                organisationId,
                SubscriptionFeatureService.FEATURE_CLIENT_LIMIT,
                null);
        if (clientLimit == null) {
            return;
        }
        long activeClients = clientRepository.countNonDeleted();
        if (activeClients >= clientLimit.longValue()) {
            throw new StoryApiException(
                    HttpStatus.FORBIDDEN,
                    "LIMIT_EXCEEDED",
                    "Client limit reached (" + clientLimit + "). Please upgrade your plan to add more clients.");
        }
    }

    @Transactional(readOnly = true)
    public List<ClientHistoryResponse> getClientHistoryTimeline(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client id is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        List<ClientHistory> history = clientHistoryRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        List<ClientHistoryResponse> historyResponses = history.stream()
                .sorted(clientHistoryTimelineComparator())
                .map(this::toTimelineHistoryResponse)
                .collect(Collectors.toList());
        return historyResponses;
    }

    @Transactional(readOnly = true)
    public ClientHistoryListResponse getClientEmailHistory(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client id is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        // ClientHub Email History = notifications tied to this client / their sessions / billing.
        // client_history portal events alone are not enough (and miss migrated V1 emails).
        List<ClientHistoryResponse> emailHistory = loadClientEmailCommunications(client).stream()
                .limit(EMAIL_HISTORY_LIMIT)
                .collect(Collectors.toList());

        ClientHistoryListResponse.ClientHistoryListResponseBuilder builder = ClientHistoryListResponse.builder()
                .history(emailHistory)
                .count(emailHistory.size());

        if (emailHistory.isEmpty()) {
            builder.message("No email history records found for this client.");
        } else {
            builder.message(null);
        }

        return builder.build();
    }

    /** Build email history from successful EMAIL delivery records plus legacy portal email events. */
    private List<ClientHistoryResponse> loadClientEmailCommunications(Client client) {
        Long clientId = requireClientId(client);
        Map<Long, ClientHistoryResponse> byDeliveryId = new LinkedHashMap<>();

        for (NotificationDeliveryLog delivery : notificationDeliveryLogRepository.findSuccessfulByRelatedEntity(
                "client", clientId, NotificationChannel.EMAIL, SUCCESSFUL_EMAIL_STATUSES)) {
            putEmailDeliveryHistory(byDeliveryId, delivery, clientId);
        }

        for (NotificationDeliveryLog delivery : notificationDeliveryLogRepository.findSuccessfulByClientAndChannel(
                client, NotificationChannel.EMAIL, SUCCESSFUL_EMAIL_STATUSES)) {
            putEmailDeliveryHistory(byDeliveryId, delivery, clientId);
        }

        List<Long> sessionIds = sessionRepository.findIdsByClientId(clientId);
        if (!sessionIds.isEmpty()) {
            for (NotificationDeliveryLog delivery : notificationDeliveryLogRepository.findSuccessfulByRelatedEntities(
                    "session", sessionIds, NotificationChannel.EMAIL, SUCCESSFUL_EMAIL_STATUSES)) {
                putEmailDeliveryHistory(byDeliveryId, delivery, clientId);
            }
        }

        List<Long> billingIds = sessionBillingRepository.findIdsByClientId(clientId);
        if (!billingIds.isEmpty()) {
            for (NotificationDeliveryLog delivery : notificationDeliveryLogRepository.findSuccessfulByRelatedEntities(
                    "billing", billingIds, NotificationChannel.EMAIL, SUCCESSFUL_EMAIL_STATUSES)) {
                putEmailDeliveryHistory(byDeliveryId, delivery, clientId);
            }
        }

        List<ClientHistoryResponse> combined = new ArrayList<>(byDeliveryId.values());

        // Keep portal/email client_history rows that are not already represented as notifications.
        Set<Instant> notificationTimestamps = combined.stream()
                .map(ClientHistoryResponse::getCreatedAt)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (ClientHistory history : clientHistoryRepository.findByClientIdOrderByCreatedAtDesc(clientId)) {
            if (!isEmailHistoryEvent(history)) {
                continue;
            }
            // Avoid near-duplicate portal rows when a notification was also written at the same instant.
            if (history.getCreatedAt() != null && notificationTimestamps.contains(history.getCreatedAt())) {
                continue;
            }
            combined.add(toHistoryResponse(history));
        }

        combined.sort(Comparator
                .comparing(ClientHistoryResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ClientHistoryResponse::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        return combined;
    }

    private void putEmailDeliveryHistory(
            Map<Long, ClientHistoryResponse> byId,
            NotificationDeliveryLog delivery,
            Long clientId) {
        if (delivery == null || delivery.getId() == null || delivery.getNotification() == null) {
            return;
        }
        byId.putIfAbsent(delivery.getId(), toEmailHistoryResponse(delivery, clientId));
    }

    private ClientHistoryResponse toEmailHistoryResponse(NotificationDeliveryLog delivery, Long clientId) {
        Notification notification = delivery.getNotification();
        String typeKey = notification.getType() != null ? notification.getType().name() : "notification";
        String relatedType = StringUtils.hasText(notification.getRelatedEntityType())
                ? notification.getRelatedEntityType()
                : "client";
        String relatedId = notification.getRelatedEntityId() != null
                ? String.valueOf(notification.getRelatedEntityId())
                : null;

        return ClientHistoryResponse.builder()
                .id(delivery.getId())
                .clientId(clientId)
                // Frontend title uses eventType; status badge uses eventSource.
                .eventType(StringUtils.hasText(notification.getTitle()) ? notification.getTitle() : typeKey)
                .eventSource(typeKey)
                .fromValue(relatedType)
                .toValue(relatedId)
                .description(notification.getMessage())
                .changeSummary(notification.getMessage())
                .createdAt(emailDeliveryTimestamp(delivery, notification))
                .build();
    }

    private Instant emailDeliveryTimestamp(NotificationDeliveryLog delivery, Notification notification) {
        if (delivery.getDeliveredAt() != null) {
            return delivery.getDeliveredAt();
        }
        if (delivery.getSentAt() != null) {
            return delivery.getSentAt();
        }
        return delivery.getCreatedAt() != null ? delivery.getCreatedAt() : notification.getCreatedAt();
    }

    @Transactional
    @CacheEvict(value = "clients", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey(#clientId)")
    public ClientResponse updatePortalAccess(Long clientId, PortalAccessRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(clientId, "Client id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        validateClientAccess(client, requester);

        boolean wasEnabled = portalSettingsService.findByClientId(requireClientId(client))
                .map(s -> Boolean.TRUE.equals(s.getHasPortalAccess()))
                .orElse(false);

        Long actorId = resolveActorId(requester);

        if (Boolean.TRUE.equals(request.getEnable())) {
            if (!wasEnabled) {
                String email = StringUtils.hasText(request.getEmail()) ? request.getEmail() : getClientEmailForPortal(client);
                if (email != null) {
                    handlePortalActivation(client, email, requester);
                } else {
                    log.warn("Portal access enabled for client {} but no email address available", clientId);
                }
            }
        } else {
            portalSettingsService.disablePortalAccess(requireClientId(client));
            if (wasEnabled) {
                trackClientHistory(requireClientId(client), "portal_deactivated", "enabled", "disabled",
                        "Portal access disabled", actorId, requester.getLoginIdentifier());
            }
        }

        recordAuditEvent(actorId, "client_portal_access_updated", requireClientId(client), ipAddress, true);

        // Ensure subsequent read reflects latest portal settings state in this request
        entityManager.flush();
        entityManager.clear();

        // Reload client with assignedTherapist relationship to avoid
        // LazyInitializationException
        Client clientWithRelations = clientRepository.findByIdWithTherapist(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found after update"));

        return toClientResponse(clientWithRelations);
    }

    @Transactional
    public void sendPortalActivation(Long clientId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(clientId, "Client id is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        validateClientAccess(client, requester);

        String email = getClientEmailForPortal(client);
        if (email == null) {
            throw new BadRequestException("Client must have an email address to send activation");
        }

        handlePortalActivation(client, email, requester);
        // Settings + auth identity are already persisted inside handlePortalActivation.
        // Do not save client with a null portalSettings inverse (orphanRemoval would delete it).
        recordAuditEvent(resolveActorId(requester), "portal_activation_sent", requireClientId(client), ipAddress, true);
    }

    /**
     * Helper method to get client email for portal operations.
     * Uses AuthIdentity (login identifier) or ClientContact (primary email).
     */
    private String getClientEmailForPortal(Client client) {
        if (client.getAuthIdentity() != null && StringUtils.hasText(client.getAuthIdentity().getLoginIdentifier())) {
            return client.getAuthIdentity().getLoginIdentifier();
        }
        try {
            Optional<ClientContact> primaryEmail = contactService.getPrimaryEmail(client.getId());
            if (primaryEmail.isPresent() && StringUtils.hasText(primaryEmail.get().getContactValue())) {
                return primaryEmail.get().getContactValue();
            }
        } catch (Exception e) {
            // fall through
        }
        return null;
    }

    @Transactional(readOnly = true)
    public ClientStatsResponse getClientStats(AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        Specification<Client> baseSpec = buildClientSpecification(null, null, null, null, null,
                null, null, null, null, null, requester);

        long totalClients = clientRepository.count(baseSpec);
        // Match Client.isActive() / option-key aliases (case and hyphen insensitive)
        long activeClients = clientRepository
                .count(baseSpec.and(statusMatchesSpec("active")));
        long pendingClients = clientRepository
                .count(baseSpec.and((root, query, cb) -> cb.equal(root.get("stage"),
                        systemOptionResolverService.parseOptionKey(SystemOptionCategories.CLIENT_STAGE, null, "intake"))));
        long completedClients = clientRepository
                .count(baseSpec.and((root, query, cb) -> cb.or(
                        statusMatchesSpec("inactive").toPredicate(root, query, cb),
                        statusMatchesSpec("discharged").toPredicate(root, query, cb))));

        return ClientStatsResponse.builder()
                .totalClients(totalClients)
                .activeClients(activeClients)
                .pendingClients(pendingClients)
                .completedClients(completedClients)
                .build();
    }

    /**
     * Client stats for therapist/supervisor dashboard cards.
     * Therapists: own caseload. Supervisors: supervised therapists' caseloads (empty if none).
     */
    @Transactional(readOnly = true)
    public ClientStatsResponse getClientStatsForAssignedTherapist(AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        Specification<Client> baseSpec = (root, query, cb) -> cb.equal(root.get("isDeleted"), false);
        baseSpec = applyCaseloadScope(baseSpec, requester, false);

        long totalClients = clientRepository.count(baseSpec);
        long activeClients = clientRepository
                .count(baseSpec.and(statusMatchesSpec("active")));
        long pendingClients = clientRepository
                .count(baseSpec.and((root, query, cb) -> cb.equal(root.get("stage"),
                        systemOptionResolverService.parseOptionKey(SystemOptionCategories.CLIENT_STAGE, null, "intake"))));
        long completedClients = clientRepository
                .count(baseSpec.and((root, query, cb) -> cb.or(
                        statusMatchesSpec("inactive").toPredicate(root, query, cb),
                        statusMatchesSpec("discharged").toPredicate(root, query, cb))));

        return ClientStatsResponse.builder()
                .totalClients(totalClients)
                .activeClients(activeClients)
                .pendingClients(pendingClients)
                .completedClients(completedClients)
                .build();
    }

    /**
     * Status predicate aligned with {@link SystemOptionKeyMatcher} case-insensitive matching.
     */
    private Specification<Client> statusMatchesSpec(String expectedKey) {
        String normalizedExpected = SystemOptionKeyMatcher.normalize(expectedKey);
        return (root, query, cb) -> cb.equal(cb.lower(root.get("status")), normalizedExpected);
    }

    @Transactional(readOnly = true)
    public ClientStageDurationsResponse getStageDurations(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client id is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        List<ClientHistory> history = clientHistoryRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        List<ClientHistory> stageChanges = history.stream()
                .filter(h -> h.getEventType() == ClientEventType.STAGE_CHANGED && h.getCreatedAt() != null)
                .sorted(clientHistoryCreatedAtAscComparator())
                .collect(Collectors.toList());

        ClientHistory createdEvent = history.stream()
                .filter(h -> h.getEventType() == ClientEventType.CREATED && h.getCreatedAt() != null)
                .min(clientHistoryCreatedAtAscComparator())
                .orElse(null);

        Map<String, Long> durations = new LinkedHashMap<>();
        String currentStage = normalizeStageLabel(resolveStageLabel(client.getStage()));
        Instant currentStageStart = null;

        if (createdEvent != null) {
            String initialStage = normalizeStageLabel(createdEvent.getToValue());
            if (initialStage != null) {
                currentStage = initialStage;
            }
            currentStageStart = createdEvent.getCreatedAt();
        } else {
            currentStageStart = resolveStageFallbackStart(client);
        }

        for (ClientHistory change : stageChanges) {
            if (currentStageStart != null && currentStage != null) {
                long durationDays = Math.max(0L,
                        java.time.temporal.ChronoUnit.DAYS.between(currentStageStart, change.getCreatedAt()));
                durations.merge(currentStage, durationDays, Long::sum);
            }

            String nextStage = normalizeStageLabel(change.getToValue());
            if (nextStage != null) {
                currentStage = nextStage;
            }
            currentStageStart = change.getCreatedAt();
        }

        if (currentStageStart != null && currentStage != null) {
            long durationDays = Math.max(0L,
                    java.time.temporal.ChronoUnit.DAYS.between(currentStageStart, Instant.now()));
            durations.merge(currentStage, durationDays, Long::sum);
        }

        return ClientStageDurationsResponse.builder()
                .clientId(clientId)
                .currentStage(currentStage)
                .durations(durations)
                .totalEvents(stageChanges.size())
                .build();
    }

    // Private helper methods

    /**
     * Build client specification from ClientFilter (NEW: uses typed filter).
     */
    private Specification<Client> buildClientSpecification(ClientFilter filter, AuthPrincipal requester) {
        Objects.requireNonNull(filter, "Filter is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Always filter out soft-deleted records (HIPAA/GDPR compliance)
        Specification<Client> spec = (root, query, cb) -> cb.equal(root.get("isDeleted"), false);

        // Search filter — encrypted PHI fields use exact equality; clientId supports partial match
        if (StringUtils.hasText(filter.getSearch())) {
            spec = spec.and(buildClientSearchSpecification(filter.getSearch()));
        }

        // Status filter
        if (StringUtils.hasText(filter.getStatus())) {
            String statusKey = systemOptionResolverService.resolveOptionKey(
                    SystemOptionCategories.CLIENT_STATUS, filter.getStatus());
            if (statusKey != null) {
                spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("status"), statusKey));
            }
        }

        // Stage filter
        if (StringUtils.hasText(filter.getStage())) {
            String stageKey = systemOptionResolverService.resolveOptionKey(
                    SystemOptionCategories.CLIENT_STAGE, filter.getStage());
            if (stageKey != null) {
                spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("stage"), stageKey));
            } else {
                log.warn("Invalid stage filter value: {}, ignoring filter", filter.getStage());
            }
        }

        // Client type filter
        if (StringUtils.hasText(filter.getClientType())) {
            String clientTypeKey = systemOptionResolverService.resolveOptionKey(
                    SystemOptionCategories.CLIENT_TYPE, filter.getClientType());
            if (clientTypeKey != null) {
                spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("clientType"), clientTypeKey));
            }
        }

        // Therapist filter (optionally including unassigned clients for scheduling)
        if (filter.getTherapistId() != null && Boolean.TRUE.equals(filter.getIncludeUnassigned())) {
            Long therapistId = filter.getTherapistId();
            spec = spec.and((root1, query1, cb1) -> cb1.or(
                    cb1.equal(root1.get("assignedTherapist").get("id"), therapistId),
                    cb1.isNull(root1.get("assignedTherapist"))));
        } else if (filter.getTherapistId() != null) {
            spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("assignedTherapist").get("id"),
                    filter.getTherapistId()));
        }

        // Portal access filter (uses ClientPortalSettings)
        if (filter.getHasPortalAccess() != null) {
            spec = spec.and((root1, query1, cb1) -> {
                Join<Client, com.smart.therapy.flow.client.entity.ClientPortalSettings> settingsJoin = root1
                        .join("portalSettings", JoinType.LEFT);
                return cb1.equal(settingsJoin.get("hasPortalAccess"), filter.getHasPortalAccess());
            });
        }

        // Needs follow-up filter
        if (filter.getNeedsFollowUp() != null) {
            spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("needsFollowUp"), filter.getNeedsFollowUp()));
        }

        // Unassigned filter
        if (Boolean.TRUE.equals(filter.getUnassigned())) {
            spec = spec.and((root1, query1, cb1) -> cb1.isNull(root1.get("assignedTherapist")));
        }

        // Has no sessions filter
        if (Boolean.TRUE.equals(filter.getHasNoSessions())) {
            spec = spec.and((root1, query1, cb1) -> cb1.isNull(root1.get("lastSessionDate")));
        }

        // Checklist template: clients who currently have this template assigned
        if (filter.getChecklistTemplateId() != null) {
            Long templateId = filter.getChecklistTemplateId();
            spec = spec.and((root1, query1, cb1) -> {
                Subquery<Long> subquery = query1.subquery(Long.class);
                Root<ClientChecklist> checklistRoot = subquery.from(ClientChecklist.class);
                subquery.select(checklistRoot.get("id"));
                subquery.where(
                        cb1.equal(checklistRoot.get("client").get("id"), root1.get("id")),
                        cb1.equal(checklistRoot.get("template").get("id"), templateId),
                        cb1.equal(checklistRoot.get("isDeleted"), false));
                return cb1.exists(subquery);
            });
        }

        // Report template: clients who have a generated report for this template
        if (filter.getReportTemplateId() != null) {
            Long reportTemplateId = filter.getReportTemplateId();
            spec = spec.and((root1, query1, cb1) -> {
                Subquery<Long> subquery = query1.subquery(Long.class);
                Root<ClientReport> reportRoot = subquery.from(ClientReport.class);
                subquery.select(reportRoot.get("id"));
                subquery.where(
                        cb1.equal(reportRoot.get("client").get("id"), root1.get("id")),
                        cb1.equal(reportRoot.get("template").get("id"), reportTemplateId),
                        cb1.equal(reportRoot.get("isDeleted"), false));
                return cb1.exists(subquery);
            });
        }

        // PBAC: Permission-based filtering with data scope
        spec = applyCaseloadScope(spec, requester, filter.getIncludeUnassigned());

        return spec;
    }

    /**
     * Exact-match client search (MRN / full name or name tokens / email / phone). No LIKE on PHI columns.
     */
    private Specification<Client> buildClientSearchSpecification(String search) {
        return clientSearchHelper.clientSearchSpecification(search);
    }

    /**
     * Build client specification from raw parameters (DEPRECATED: kept for backward
     * compatibility).
     * 
     * @deprecated Use
     *             {@link #buildClientSpecification(ClientFilter, AuthPrincipal)}
     *             instead.
     */
    @Deprecated
    private Specification<Client> buildClientSpecification(
            String search, String status, String stage, Long therapistId, String clientType,
            Boolean hasPortalAccess, Boolean hasPendingTasks, Boolean hasNoSessions,
            Boolean needsFollowUp, Boolean unassigned, AuthPrincipal requester) {
        // Always filter out soft-deleted records (HIPAA/GDPR compliance)
        Specification<Client> spec = (root, query, cb) -> cb.equal(root.get("isDeleted"), false);

        // Search filter — encrypted PHI fields use exact equality; clientId supports partial match
        if (StringUtils.hasText(search)) {
            spec = spec.and(buildClientSearchSpecification(search));
        }

        // Status filter
        if (StringUtils.hasText(status)) {
            String statusKey = systemOptionResolverService.resolveOptionKey(SystemOptionCategories.CLIENT_STATUS, status);
            if (statusKey != null) {
                spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("status"), statusKey));
            } else {
                log.warn("Invalid status filter value: {}, ignoring filter", status);
            }
        }

        // Stage filter
        if (StringUtils.hasText(stage)) {
            String stageKey = systemOptionResolverService.resolveOptionKey(SystemOptionCategories.CLIENT_STAGE, stage);
            if (stageKey != null) {
                spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("stage"), stageKey));
            } else {
                log.warn("Invalid stage filter value: {}, ignoring filter", stage);
            }
        }

        // Therapist filter (optionally including unassigned clients for scheduling)
        if (therapistId != null && Boolean.TRUE.equals(unassigned)) {
            // Legacy path: unassigned=true with therapistId is uncommon; keep exact therapist match.
            spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("assignedTherapist").get("id"), therapistId));
        } else if (therapistId != null) {
            spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("assignedTherapist").get("id"), therapistId));
        }

        // Client type filter
        if (StringUtils.hasText(clientType)) {
            String clientTypeKey = systemOptionResolverService.resolveOptionKey(SystemOptionCategories.CLIENT_TYPE, clientType);
            if (clientTypeKey != null) {
                spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("clientType"), clientTypeKey));
            } else {
                log.warn("Invalid clientType filter value: {}, ignoring filter", clientType);
            }
        }

        // Portal access filter (uses ClientPortalSettings)
        if (hasPortalAccess != null) {
            spec = spec.and((root1, query1, cb1) -> {
                Join<Client, com.smart.therapy.flow.client.entity.ClientPortalSettings> settingsJoin = root1
                        .join("portalSettings", JoinType.LEFT);
                return cb1.equal(settingsJoin.get("hasPortalAccess"), hasPortalAccess);
            });
        }

        // Needs follow-up filter
        if (needsFollowUp != null) {
            spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("needsFollowUp"), needsFollowUp));
        }

        // Unassigned filter
        if (Boolean.TRUE.equals(unassigned) && therapistId == null) {
            spec = spec.and((root1, query1, cb1) -> cb1.isNull(root1.get("assignedTherapist")));
        }

        // Has no sessions filter
        if (Boolean.TRUE.equals(hasNoSessions)) {
            spec = spec.and((root1, query1, cb1) -> cb1.isNull(root1.get("lastSessionDate")));
        }

        // PBAC: Permission-based filtering with data scope
        spec = applyCaseloadScope(spec, requester, false);

        return spec;
    }

    private Specification<Client> applyCaseloadScope(
            Specification<Client> spec,
            AuthPrincipal requester,
            Boolean includeUnassigned) {
        CaseloadScopeService.ResolvedCaseloadScope resolved = caseloadScopeService.resolve(requester);
        boolean allowUnassigned = Boolean.TRUE.equals(includeUnassigned);
        return switch (resolved.scope()) {
            case ALL -> spec;
            case NONE -> spec.and((root1, query1, cb1) -> cb1.disjunction());
            case OWN -> {
                Long currentUserId = resolved.currentUserId();
                if (allowUnassigned) {
                    yield spec.and((root1, query1, cb1) -> cb1.or(
                            cb1.equal(root1.get("assignedTherapist").get("id"), currentUserId),
                            cb1.isNull(root1.get("assignedTherapist"))));
                }
                yield spec.and((root1, query1, cb1) ->
                        cb1.equal(root1.get("assignedTherapist").get("id"), currentUserId));
            }
            case TEAM -> {
                if (resolved.supervisedTherapistIds().isEmpty() && !allowUnassigned) {
                    yield spec.and((root1, query1, cb1) -> cb1.disjunction());
                }
                if (resolved.supervisedTherapistIds().isEmpty()) {
                    yield spec.and((root1, query1, cb1) -> cb1.isNull(root1.get("assignedTherapist")));
                }
                if (allowUnassigned) {
                    yield spec.and((root1, query1, cb1) -> cb1.or(
                            root1.get("assignedTherapist").get("id").in(resolved.supervisedTherapistIds()),
                            cb1.isNull(root1.get("assignedTherapist"))));
                }
                yield spec.and((root1, query1, cb1) ->
                        root1.get("assignedTherapist").get("id").in(resolved.supervisedTherapistIds()));
            }
            case TEAM_AND_OWN -> {
                List<Long> therapistIds = new ArrayList<>(resolved.supervisedTherapistIds());
                if (resolved.currentUserId() != null) {
                    therapistIds.add(resolved.currentUserId());
                }
                if (therapistIds.isEmpty() && !allowUnassigned) {
                    yield spec.and((root1, query1, cb1) -> cb1.disjunction());
                }
                if (therapistIds.isEmpty()) {
                    yield spec.and((root1, query1, cb1) -> cb1.isNull(root1.get("assignedTherapist")));
                }
                if (allowUnassigned) {
                    yield spec.and((root1, query1, cb1) -> cb1.or(
                            root1.get("assignedTherapist").get("id").in(therapistIds),
                            cb1.isNull(root1.get("assignedTherapist"))));
                }
                yield spec.and((root1, query1, cb1) ->
                        root1.get("assignedTherapist").get("id").in(therapistIds));
            }
        };
    }

    private void validateClientAccess(Client client, AuthPrincipal requester) {
        CaseloadScopeService.ResolvedCaseloadScope resolved = caseloadScopeService.resolve(requester);
        if (resolved.scope() == CaseloadScope.NONE) {
            throw new ForbiddenException("You do not have permission to access clients");
        }
        if (resolved.scope() == CaseloadScope.ALL) {
            return;
        }

        try {
            Long therapistId;
            try {
                therapistId = client.getAssignedTherapist() != null ? client.getAssignedTherapist().getId() : null;
            } catch (org.hibernate.LazyInitializationException e) {
                log.warn(
                        "LazyInitializationException when accessing assignedTherapist for client {}, loading explicitly",
                        client.getId());
                Client clientWithTherapist = clientRepository.findByIdWithTherapist(client.getId())
                        .orElse(client);
                therapistId = clientWithTherapist.getAssignedTherapist() != null
                        ? clientWithTherapist.getAssignedTherapist().getId()
                        : null;
            }

            if (!resolved.includesTherapist(therapistId)) {
                if (resolved.scope() == CaseloadScope.OWN) {
                    throw new ForbiddenException("You can only access clients assigned to you");
                }
                throw new ForbiddenException("You can only access clients of therapists you supervise");
            }
        } catch (ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating client access for client {}: {}", client.getId(), e.getMessage(), e);
            throw new ForbiddenException("Error validating access: " + e.getMessage());
        }
    }

    private List<Long> getSupervisedTherapistIds(Long supervisorId) {
        return caseloadScopeService.getSupervisedTherapistIds(supervisorId);
    }

    /**
     * All non-deleted sessions for a client file view. Client access is enforced by
     * {@link #validateClientAccess(Client, AuthPrincipal)} on the caller; do not
     * apply service therapistVisible filtering here — that is billing-catalog scope,
     * not client-session scope, and it caused KPI summaries to return 0 while
     * {@code /api/v1/sessions?clientId=} still listed the sessions.
     */
    private List<Session> loadClientSessionsForView(Long clientId) {
        return sessionRepository.findByClientIdWithRelations(clientId);
    }

    /**
     * Generate unique client ID in format CL-YYYY-NNNN (e.g., CL-2025-0001).
     * Uses efficient database query to find the highest existing ID for the current
     * year.
     * Includes retry logic to handle race conditions in concurrent scenarios.
     * 
     * @return Unique client ID string
     */
    private String generateClientId() {
        return clientMrnService.allocateNextMrn();
    }

    /**
     * Handle portal activation for post-commit processing (no principal needed).
     */
    public void handlePortalActivationPostCommit(Client client) {
        Long clientId = requireClientId(client);
        try {
            if (!portalSettingsService.hasPortalAccess(clientId)) {
                return;
            }
            AuthIdentity identity = client.getAuthIdentity();
            if (identity == null) {
                return;
            }
            String email = identity.getLoginIdentifier();
            if (!StringUtils.hasText(email)) {
                return;
            }
            String activationToken = identity.getEmailVerificationToken();
            if (!StringUtils.hasText(activationToken)) {
                activationToken = authIdentityService.generateSecureToken();
                authIdentityService.setEmailVerificationToken(identity.getId(), activationToken,
                        Instant.now().plusSeconds(48 * 3600));
            }
            if (emailService != null) {
                try {
                    emailService.sendActivationEmail(email, client.getFullName(), activationToken);
                    log.info("Activation email sent for clientId={}", clientId);
                } catch (Exception e) {
                    log.error("Failed to send activation email for clientId={}", clientId, e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to handle portal activation post-commit for client {}: {}", clientId, e.getMessage(), e);
        }
    }

    @Transactional
    public void enablePortalForPublicBooking(
            Client client,
            String email,
            Long createdByUserId,
            String createdByName) {
        Objects.requireNonNull(client, "Client is required");
        if (!StringUtils.hasText(email)) {
            throw new BadRequestException("Portal email is required");
        }
        if (createdByUserId == null) {
            throw new BadRequestException("Portal creator is required");
        }
        provisionPortalActivation(client, email, createdByUserId,
                StringUtils.hasText(createdByName) ? createdByName : "Public consultation booking");
    }

    private void handlePortalActivation(Client client, String email, AuthPrincipal requester) {
        try {
            Long actorId = resolveActorId(requester);
            if (actorId == null) {
                throw new ForbiddenException("Staff user not found for portal identity creation");
            }
            provisionPortalActivation(client, email, actorId, requester.getLoginIdentifier());
        } catch (BadRequestException | ForbiddenException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            Long clientId = requireClientId(client);
            log.error("Failed to handle portal activation for client {}: {}", clientId, e.getMessage(), e);
            throw new BadRequestException("Failed to enable portal access: " + e.getMessage());
        }
    }

    private void provisionPortalActivation(
            Client client,
            String email,
            Long actorId,
            String actorName) {
        Long clientId = requireClientId(client);
        AuthIdentity identity = client.getAuthIdentity();
        if (identity == null) {
            createClientPortalIdentity(client, email, actorId);
            identity = client.getAuthIdentity();
        } else {
            String normalised = AuthIdentityService.normaliseLoginIdentifier(email);
            boolean dirty = false;
            if (!normalised.equals(identity.getNormalisedLoginIdentifier())) {
                identity.setLoginIdentifier(email);
                identity.setNormalisedLoginIdentifier(normalised);
                dirty = true;
            }
            // Revoking portal access deactivates the identity, so re-enabling has to switch it back on
            // or the client keeps a login that can never authenticate.
            if (!Boolean.TRUE.equals(identity.getIsActive())) {
                identity.setIsActive(true);
                dirty = true;
            }
            if (dirty) {
                authIdentityRepository.save(identity);
            }
        }
        portalSettingsService.getOrCreate(clientId);
        portalSettingsService.setHasPortalAccess(clientId, true);
        portalSettingsService.setActivated(clientId, false, Instant.now());

        String activationToken = authIdentityService.generateSecureToken();
        authIdentityService.setEmailVerificationToken(identity.getId(), activationToken,
                Instant.now().plusSeconds(48 * 3600));

        schedulePortalActivationEmail(email, client.getFullName(), activationToken, clientId);
        trackClientHistory(clientId, "portal_activated", "disabled", "activation_sent",
                "Portal access enabled. Activation email sent to " + email,
                actorId, actorName);
    }

    private Long resolveActorId(AuthPrincipal requester) {
        Long actorId = currentUserService.getCurrentUserId(requester);
        if (actorId == null) {
            log.warn("Staff user missing for authId {} in schema {}; continuing without audit actor",
                    requester != null ? requester.getAuthId() : null, TenantContext.getSchemaName());
        }
        return actorId;
    }

    private void schedulePortalActivationEmail(String email, String clientName, String activationToken, Long clientId) {
        Runnable sendEmail = () -> {
            if (emailService == null) {
                log.warn("Email service not configured - activation email not sent");
                return;
            }
            try {
                emailService.sendActivationEmailAsync(email, clientName, activationToken);
                log.debug("Queued portal activation email on emailExecutor for client {}", clientId);
            } catch (Exception e) {
                log.error("Failed to queue activation email for clientId={}", clientId, e);
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendEmail.run();
                }
            });
            log.debug("Queued portal activation email after commit for client {}", clientId);
            return;
        }

        sendEmail.run();
    }

    /** Create AuthIdentity (CLIENT), assign CLIENT role, and link it to the client. */
    private void createClientPortalIdentity(Client client, String email, Long createdBy) {
        String tempPassword = UUID.randomUUID().toString();
        AuthIdentity identity;
        try {
            identity = authIdentityService.createClientIdentity(email, tempPassword, createdBy);
        } catch (BadRequestException ex) {
            throw new BadRequestException("Portal email already in use");
        }
        if (identity == null) {
            throw new BadRequestException("Portal email already in use");
        }
        Long orgId = TenantContext.getOrganisationId();
        if (orgId == null) {
            throw new BadRequestException("Tenant context required to enable portal");
        }
        Role clientRole = roleRepository.findByNameForOrganisation(com.smart.therapy.flow.common.util.RoleName.CLIENT.name(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("CLIENT role not found"));
        Organisation org = organisationRepository.findById(orgId).orElseThrow(() -> new BadRequestException("Organisation not found"));
        AuthIdentityRole air = AuthIdentityRole.builder()
                .authIdentity(identity)
                .role(clientRole)
                .organisation(org)
                .build();
        authIdentityRoleRepository.save(air);
        client.setAuthIdentity(identity);
        clientRepository.save(client);
    }

    private String generateActivationToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[ACTIVATION_TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Track client history - public for use by event listeners.
     */
    public void trackClientHistory(Long clientId, String eventType, String fromValue, String toValue,
            String description, Long createdByUserId, String createdByName) {
        ClientEventType eventTypeEnum = resolveClientEventType(eventType);
        if (eventTypeEnum == null) {
            log.warn("Invalid event type: {}, using CREATED as default", eventType);
            eventTypeEnum = ClientEventType.CREATED;
        }

        clientHistoryTrackingService.persistHistory(
                clientId,
                eventTypeEnum,
                EventSource.API,
                fromValue,
                toValue,
                description,
                createdByUserId,
                createdByName);
    }

    private ClientEventType resolveClientEventType(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return null;
        }
        ClientEventType mapped = mapLegacyEventType(eventType);
        if (mapped != null) {
            return mapped;
        }
        try {
            return ClientEventType.fromValue(eventType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private ClientEventType mapLegacyEventType(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return null;
        }
        return switch (eventType.trim().toLowerCase(Locale.ROOT)) {
            case "file_created" -> ClientEventType.CREATED;
            case "file_closed" -> ClientEventType.FILE_CLOSED;
            case "file_reopened" -> ClientEventType.FILE_REOPENED;
            case "portal_deactivated" -> ClientEventType.PORTAL_DEACTIVATED;
            case "portal_activated" -> ClientEventType.PORTAL_ACTIVATED;
            case "portal_activation_resent" -> ClientEventType.PORTAL_ACTIVATED;
            case "status_change" -> ClientEventType.STATUS_CHANGED;
            case "stage_change" -> ClientEventType.STAGE_CHANGED;
            case "therapist_assignment" -> ClientEventType.THERAPIST_CHANGED;
            default -> null;
        };
    }

    private void trackClientChanges(Client original, Client updated, AuthPrincipal requester) {
        Long actorUserId = resolveActorId(requester);
        String actorName = requester.getLoginIdentifier();

        // Track status changes
        if (!Objects.equals(original.getStatus(), updated.getStatus())) {
            String fromStatus = resolveOptionLabel(SystemOptionCategories.CLIENT_STATUS, original.getStatus());
            String toStatus = resolveOptionLabel(SystemOptionCategories.CLIENT_STATUS, updated.getStatus());
            if (SystemOptionKeyMatcher.matchesAny(updated.getStatus(), "inactive")) {
                trackClientHistory(requireClientId(updated), "file_closed",
                        fromStatus, toStatus,
                        "Client file closed and set to inactive",
                        actorUserId, actorName);
            } else if (SystemOptionKeyMatcher.matchesAny(updated.getStatus(), "active")
                    && SystemOptionKeyMatcher.matchesAny(original.getStatus(), "inactive")) {
                trackClientHistory(requireClientId(updated), "file_reopened",
                        fromStatus, toStatus,
                        "Client file reopened and reactivated",
                        actorUserId, actorName);
            } else {
                trackClientHistory(requireClientId(updated), "status_change",
                        fromStatus, toStatus,
                        "Status changed from " + fromStatus + " to " + toStatus,
                        actorUserId, actorName);
            }
        }

        // Track stage changes
        if (!Objects.equals(original.getStage(), updated.getStage())) {
            String fromStage = resolveStageLabel(original.getStage());
            String toStage = resolveStageLabel(updated.getStage());
            trackClientHistory(requireClientId(updated), "stage_change",
                    fromStage, toStage,
                    "Stage changed from " + fromStage + " to " + toStage,
                    actorUserId, actorName);
        }

        // Track therapist assignment changes
        Long originalTherapistId = original.getAssignedTherapist() != null ? original.getAssignedTherapist().getId()
                : null;
        Long updatedTherapistId = updated.getAssignedTherapist() != null ? updated.getAssignedTherapist().getId()
                : null;
        if (!Objects.equals(originalTherapistId, updatedTherapistId)) {
            String fromTherapist = originalTherapistId != null ? "Therapist " + originalTherapistId : "Unassigned";
            String toTherapist = updatedTherapistId != null ? "Therapist " + updatedTherapistId : "Unassigned";

            trackClientHistory(requireClientId(updated), "therapist_assignment",
                    fromTherapist, toTherapist,
                    "Therapist assignment changed from " + fromTherapist + " to " + toTherapist,
                    actorUserId, actorName);

            // Trigger notification (published AFTER_COMMIT by ClientNotificationListener).
            if (updatedTherapistId != null) {
                try {
                    Map<String, Object> eventData = buildClientEventData(updated);
                    eventData.put("assignedToId", updatedTherapistId);
                    eventPublisher.publishEvent(new ClientNotificationEvent(
                            NotificationEventCatalog.CLIENT_ASSIGNED,
                            eventData,
                            TenantContext.getOrganisationId(),
                            TenantContext.getSchemaName()
                    ));
                } catch (Exception e) {
                    log.error("Failed to publish client_assigned notification event", e);
                }
            }
        }
    }

    /**
     * Build client event data - public for use by event listeners.
     */
    public Map<String, Object> buildClientEventData(Client client) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", client.getId());
        data.put("clientId", client.getClientId());
        NotificationPayloadFactory.putClientIdentity(data, client);
        data.put("assignedTherapistId",
                client.getAssignedTherapist() != null ? client.getAssignedTherapist().getId() : null);
        data.put("therapistId",
                client.getAssignedTherapist() != null ? client.getAssignedTherapist().getId() : null);
        data.put("therapistName",
                client.getAssignedTherapist() != null ? client.getAssignedTherapist().getFullName() : "Unassigned");
        data.put("status", client.getStatus());
        data.put("stage", client.getStage());
        // Get reference number from normalized referral entity
        String referenceNumber = null;
        if (client.getReferral() != null) {
            referenceNumber = client.getReferral().getReferenceNumber();
        }
        data.put("referenceNumber", referenceNumber);
        data.put("createdAt", client.getCreatedAt());
        return data;
    }

    private void recordAuditEvent(Long actorId, String action, Long resourceId, String ipAddress,
            boolean hipaaRelevant) {
        recordAuditEvent(actorId, action, resourceId, ipAddress, hipaaRelevant, null);
    }

    private void recordAuditEvent(Long actorId, String action, Long resourceId, String ipAddress,
            boolean hipaaRelevant, String details) {
        try {
            auditLogService.recordStaffEvent(actorId, action, RESOURCE_TYPE_CLIENT, resourceId, resourceId, ipAddress,
                    hipaaRelevant, details);
        } catch (Exception e) {
            log.error("Failed to record audit event for client: {}", resourceId, e);
        }
    }

    /**
     * Golden Rule 3: Record audit event with before/after state snapshots for compliance (HIPAA/GDPR).
     * This method captures complete state snapshots, not just changed fields.
     */
    private void recordAuditEventWithStates(Long actorId, String action, Long resourceId, String ipAddress,
            boolean hipaaRelevant, String beforeState, String afterState, List<String> changedFields) {
        String changedFieldsJson = null;
        if (changedFields != null && !changedFields.isEmpty()) {
            try {
                changedFieldsJson = objectMapper.writeValueAsString(changedFields);
            } catch (Exception e) {
                log.warn("Failed to serialize changed fields to JSON", e);
            }
        }
        try {
            auditLogService.recordStaffEventWithStates(actorId, action, RESOURCE_TYPE_CLIENT, resourceId, resourceId,
                    ipAddress, hipaaRelevant, beforeState, afterState, changedFieldsJson);
        } catch (Exception e) {
            log.error("Failed to record audit event with states for client: {}", resourceId, e);
        }
    }

    /**
     * Serialize client entity state to JSON for audit trail.
     * Golden Rule 3: Capture complete state snapshot for compliance.
     */
    private String serializeClientState(Client client) {
        if (client == null) {
            return null;
        }
        try {
            Map<String, Object> state = new HashMap<>();
            state.put("id", client.getId());
            state.put("clientId", client.getClientId());
            state.put("fullName", client.getFullName());
            state.put("dateOfBirth", client.getDateOfBirth());
            state.put("gender", resolveOptionLabel(SystemOptionCategories.GENDER, client.getGender()));
            state.put("maritalStatus", resolveOptionLabel(SystemOptionCategories.MARITAL_STATUS, client.getMaritalStatus()));
            state.put("status", resolveOptionLabel(SystemOptionCategories.CLIENT_STATUS, client.getStatus()));
            state.put("stage", resolveStageLabel(client.getStage()));
            state.put("clientType", resolveOptionLabel(SystemOptionCategories.CLIENT_TYPE, client.getClientType()));
            state.put("assignedTherapistId", client.getAssignedTherapist() != null ? client.getAssignedTherapist().getId() : null);
            state.put("createdAt", client.getCreatedAt());
            state.put("updatedAt", client.getUpdatedAt());
            // Note: We don't include sensitive fields like notes in the audit log for privacy
            return objectMapper.writeValueAsString(state);
        } catch (Exception e) {
            log.error("Failed to serialize client state for audit trail", e);
            return null;
        }
    }

    /**
     * Determine which fields changed between before and after states.
     * Used for audit trail to track specific field changes.
     */
    private List<String> determineChangedFields(String beforeState, String afterState) {
        List<String> changedFields = new ArrayList<>();
        if (beforeState == null || afterState == null) {
            return changedFields;
        }

        try {
            Map<String, Object> before = objectMapper.readValue(beforeState, Map.class);
            Map<String, Object> after = objectMapper.readValue(afterState, Map.class);

            // Compare all fields
            Set<String> allKeys = new HashSet<>();
            allKeys.addAll(before.keySet());
            allKeys.addAll(after.keySet());

            for (String key : allKeys) {
                Object beforeValue = before.get(key);
                Object afterValue = after.get(key);
                if (!Objects.equals(beforeValue, afterValue)) {
                    changedFields.add(key);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to determine changed fields for audit trail", e);
        }

        return changedFields;
    }

    /**
     * Golden Rule 2: Find existing client by unique fields for idempotency check.
     * Checks for clients with matching name, DOB (if provided), and email (if provided).
     * This prevents duplicate client creation from double-clicks.
     */
    private Optional<Client> findClientByUniqueFields(String fullName, LocalDate dateOfBirth, String email) {
        if (!StringUtils.hasText(fullName)) {
            return Optional.empty();
        }

        List<Client> candidates;
        if (blindIndexService.getSearchMode().writesBlindIndexes()) {
            byte[] nameIdx = blindIndexService.compute(
                    BlindIndexService.Kind.FULL_NAME, blindIndexService.normalizeFullName(fullName.trim()));
            byte[] dobIdx = dateOfBirth == null
                    ? null
                    : blindIndexService.compute(
                            BlindIndexService.Kind.DATE_OF_BIRTH,
                            blindIndexService.normalizeDateOfBirth(dateOfBirth));
            candidates = clientRepository.findByFullNameBlindIdxAndDateOfBirthBlindIdx(nameIdx, dobIdx);
        } else {
            candidates = clientRepository.findByFullNameAndDateOfBirth(fullName.trim(), dateOfBirth);
        }

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        // If email is provided, further filter by email match
        if (StringUtils.hasText(email)) {
            for (Client candidate : candidates) {
                // Check if email matches in contacts
                List<ClientContact> contacts = contactRepository.findByClientIdAndContactType(
                        candidate.getId(), ContactType.EMAIL);
                for (ClientContact contact : contacts) {
                    if (email.equalsIgnoreCase(contact.getContactValue())) {
                        return Optional.of(candidate);
                    }
                }
            }
            // If email provided but no match found, return empty (strict matching)
            return Optional.empty();
        } else {
            // If no email provided, return first match (name + DOB match is sufficient)
            return candidates.stream()
                    .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                    .findFirst();
        }
    }

    /**
     * Lighter-weight mapper for the clients list endpoint. Avoids the normalized-entity
     * lookups (contacts/address/insurance/employment/portal settings) that
     * {@link #toClientResponse(Client)} performs, since the list UI does not render them.
     * Still resolves the assigned therapist, checklist/document counts, and referral
     * reference number, since those back visible list columns.
     * <p>
     * {@code lastSessionDate} / {@code nextAppointmentDate} are computed from live sessions
     * (not the denormalized columns alone) so future bookings never appear as last session.
     */
    private List<ClientSummaryResponse> toClientSummaryResponses(List<Client> clients) {
        if (clients == null || clients.isEmpty()) {
            return List.of();
        }
        List<Long> clientIds = clients.stream()
                .map(Client::getId)
                .filter(Objects::nonNull)
                .toList();
        Instant asOf = Instant.now();
        Map<Long, Instant> lastHeldByClient = new HashMap<>();
        Map<Long, Instant> nextApptByClient = new HashMap<>();
        if (!clientIds.isEmpty()) {
            try {
                for (Object[] row : sessionRepository.findLastHeldSessionDatesByClientIds(clientIds, asOf)) {
                    if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                        continue;
                    }
                    lastHeldByClient.put(((Number) row[0]).longValue(), toInstant(row[1]));
                }
            } catch (Exception e) {
                log.debug("Could not batch-load last held session dates: {}", e.getMessage());
            }
            try {
                for (Object[] row : sessionRepository.findNextAppointmentDatesByClientIds(clientIds, asOf)) {
                    if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                        continue;
                    }
                    nextApptByClient.put(((Number) row[0]).longValue(), toInstant(row[1]));
                }
            } catch (Exception e) {
                log.debug("Could not batch-load next appointment dates: {}", e.getMessage());
            }
        }
        return clients.stream()
                .map(client -> toClientSummaryResponse(
                        client,
                        lastHeldByClient.get(client.getId()),
                        nextApptByClient.get(client.getId())))
                .collect(Collectors.toList());
    }

    private ClientSummaryResponse toClientSummaryResponse(Client client) {
        Instant asOf = Instant.now();
        Instant lastHeld = null;
        Instant nextAppt = null;
        if (client.getId() != null) {
            try {
                lastHeld = sessionRepository.findLastHeldSessionDate(client.getId(), asOf);
            } catch (Exception e) {
                log.debug("Could not load last held session for client {}: {}", client.getId(), e.getMessage());
            }
            try {
                nextAppt = sessionRepository.findNextAppointmentDate(client.getId(), asOf);
            } catch (Exception e) {
                log.debug("Could not load next appointment for client {}: {}", client.getId(), e.getMessage());
            }
        }
        return toClientSummaryResponse(client, lastHeld, nextAppt);
    }

    private ClientSummaryResponse toClientSummaryResponse(Client client, Instant lastSessionDate, Instant nextAppointmentDate) {
        Long assignedTherapistId = null;
        String assignedTherapistName = null;
        try {
            if (client.getAssignedTherapist() != null) {
                assignedTherapistId = client.getAssignedTherapist().getId();
                assignedTherapistName = client.getAssignedTherapist().getFullName();
            }
        } catch (org.hibernate.LazyInitializationException e) {
            log.warn(
                    "LazyInitializationException when accessing assignedTherapist for client {}, relationship not loaded",
                    client.getId());
        }

        String referenceNumber = null;
        ClientReferral referral = null;
        try {
            referral = client.getReferral();
            if (referral != null) {
                referral.getId();
            }
        } catch (org.hibernate.LazyInitializationException e) {
            log.warn("LazyInitializationException when accessing referral for client {}", client.getId());
        }
        if (referral == null) {
            try {
                referral = referralService.getReferral(client.getId()).orElse(null);
            } catch (Exception e) {
                log.debug("Unable to resolve referral for client {}: {}", client.getId(), e.getMessage());
            }
        }
        if (referral != null) {
            referenceNumber = referral.getReferenceNumber();
        }

        Long checklistCount = 0L;
        Long documentCount = 0L;
        try {
            checklistCount = clientChecklistRepository.countByClientIdAndIsDeletedFalse(client.getId());
        } catch (Exception e) {
            log.debug("Error getting checklist count for client {}: {}", client.getId(), e.getMessage());
        }
        try {
            documentCount = documentRepository.countByClientIdAndIsDeletedFalse(client.getId());
        } catch (Exception e) {
            log.debug("Error getting document count for client {}: {}", client.getId(), e.getMessage());
        }

        return ClientSummaryResponse.builder()
                .id(client.getId())
                .clientId(client.getClientId())
                .fullName(client.getFullName())
                .status(resolveOptionLabel(SystemOptionCategories.CLIENT_STATUS, client.getStatus()))
                .stage(resolveStageLabel(client.getStage()))
                .assignedTherapistId(assignedTherapistId)
                .assignedTherapistName(assignedTherapistName)
                .referenceNumber(referenceNumber)
                .checklistCount(checklistCount)
                .documentCount(documentCount)
                .lastSessionDate(lastSessionDate)
                .nextAppointmentDate(nextAppointmentDate)
                .build();
    }

    private static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toInstant();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant();
        }
        if (value instanceof java.time.OffsetDateTime odt) {
            return odt.toInstant();
        }
        if (value instanceof java.time.LocalDateTime ldt) {
            return ldt.atZone(ZoneId.systemDefault()).toInstant();
        }
        return null;
    }

    private DirectoryClientSummary toDirectoryClientSummary(Client client) {
        Long assignedTherapistId = null;
        String assignedTherapistName = null;
        try {
            if (client.getAssignedTherapist() != null) {
                assignedTherapistId = client.getAssignedTherapist().getId();
                assignedTherapistName = client.getAssignedTherapist().getFullName();
            }
        } catch (org.hibernate.LazyInitializationException e) {
            log.debug("Assigned therapist not loaded for directory client {}", client.getId());
        }

        Boolean hasPortalAccess = portalSettingsService.findByClientId(client.getId())
                .map(ClientPortalSettings::getHasPortalAccess)
                .orElse(null);

        return DirectoryClientSummary.builder()
                .id(client.getId())
                .clientId(client.getClientId())
                .fullName(client.getFullName())
                .email(client.getPrimaryEmail())
                .phone(client.getPrimaryPhone())
                .dateOfBirth(client.getDateOfBirth())
                .status(resolveOptionLabel(SystemOptionCategories.CLIENT_STATUS, client.getStatus()))
                .stage(resolveStageLabel(client.getStage()))
                .clientType(resolveOptionLabel(SystemOptionCategories.CLIENT_TYPE, client.getClientType()))
                .preferredLanguage(client.getPreferredLanguage())
                .assignedTherapistId(assignedTherapistId)
                .assignedTherapistName(assignedTherapistName)
                .hasPortalAccess(hasPortalAccess)
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .build();
    }

    private ClientResponse toClientResponse(Client client) {
        return toClientResponse(client, false);
    }

    private ClientResponse toClientResponse(Client client, boolean loadedForRead) {
        // Safely extract assigned therapist info to avoid LazyInitializationException
        Long assignedTherapistId = null;
        String assignedTherapistName = null;
        try {
            if (client.getAssignedTherapist() != null) {
                assignedTherapistId = client.getAssignedTherapist().getId();
                assignedTherapistName = client.getAssignedTherapist().getFullName();
            }
        } catch (org.hibernate.LazyInitializationException e) {
            log.warn(
                    "LazyInitializationException when accessing assignedTherapist for client {}, relationship not loaded",
                    client.getId());
            // Values remain null, which is acceptable
        }

        // Get data from normalized entities with lazy-loading safeguards
        String email = null;
        String phone = null;
        ClientAddress primaryAddress = null;
        try {
            email = client.getPrimaryEmail();
            phone = client.getPrimaryPhone();
            primaryAddress = client.getPrimaryAddress();
        } catch (org.hibernate.LazyInitializationException e) {
            log.warn("LazyInitializationException when reading contact/address relations for client {}", client.getId());
        }

        if (email == null && !loadedForRead) {
            try {
                email = contactService.getPrimaryEmail(client.getId())
                        .map(ClientContact::getContactValue)
                        .orElse(null);
            } catch (Exception e) {
                log.debug("Unable to resolve primary email for client {}: {}", client.getId(), e.getMessage());
            }
        }
        if (phone == null && !loadedForRead) {
            try {
                phone = contactService.getPrimaryPhone(client.getId())
                        .map(ClientContact::getContactValue)
                        .orElse(null);
            } catch (Exception e) {
                log.debug("Unable to resolve primary phone for client {}: {}", client.getId(), e.getMessage());
            }
        }
        if (primaryAddress == null && !loadedForRead) {
            try {
                primaryAddress = addressService.getPrimaryAddress(client.getId()).orElse(null);
            } catch (Exception e) {
                log.debug("Unable to resolve primary address for client {}: {}", client.getId(), e.getMessage());
            }
        }

        // Get insurance from normalized entity
        String insuranceProvider = null;
        String policyNumber = null;
        String groupNumber = null;
        String insurancePhone = null;
        String insuranceType = null;
        java.math.BigDecimal copayAmount = null;
        java.math.BigDecimal deductible = null;
        ClientInsurance insurance = null;
        try {
            insurance = client.getInsurance();
            if (insurance != null) {
                // touch lazy proxy safely in transactional contexts
                insurance.getId();
            }
        } catch (org.hibernate.LazyInitializationException e) {
            log.warn("LazyInitializationException when accessing insurance for client {}", client.getId());
        }
        if (insurance == null && !loadedForRead) {
            try {
                insurance = insuranceService.getInsurance(client.getId()).orElse(null);
            } catch (Exception e) {
                log.debug("Unable to resolve insurance for client {}: {}", client.getId(), e.getMessage());
            }
        }
        if (insurance != null) {
            insuranceProvider = insurance.getInsuranceProvider();
            policyNumber = insurance.getPolicyNumber();
            groupNumber = insurance.getGroupNumber();
            insurancePhone = insurance.getInsurancePhone();
            insuranceType = resolveOptionLabel(SystemOptionCategories.INSURANCE_TYPES, insurance.getInsuranceType());
            copayAmount = insurance.getCopayAmount();
            deductible = insurance.getDeductible();
        }

        // Get referral from normalized entity
        String referrerName = null;
        java.time.LocalDate referralDate = null;
        String referenceNumber = null;
        String clientSource = null;
        String referralNotes = null;
        ClientReferral referral = null;
        try {
            referral = client.getReferral();
            if (referral != null) {
                referral.getId();
            }
        } catch (org.hibernate.LazyInitializationException e) {
            log.warn("LazyInitializationException when accessing referral for client {}", client.getId());
        }
        if (referral == null && !loadedForRead) {
            try {
                referral = referralService.getReferral(client.getId()).orElse(null);
            } catch (Exception e) {
                log.debug("Unable to resolve referral for client {}: {}", client.getId(), e.getMessage());
            }
        }
        if (referral != null) {
            referrerName = referral.getReferrerName();
            referralDate = referral.getReferralDate();
            referenceNumber = referral.getReferenceNumber();
            clientSource = referral.getClientSource();
            referralNotes = referral.getReferralNotes();
        }

        // Get employment / socioeconomic data from normalized entity
        String employmentStatus = null;
        String educationLevel = null;
        Integer numberOfDependents = null;
        ClientEmployment employment = null;
        try {
            employment = client.getEmployment();
            if (employment != null) {
                employment.getId();
            }
        } catch (org.hibernate.LazyInitializationException e) {
            log.warn("LazyInitializationException when accessing employment for client {}", client.getId());
        }
        if (employment == null && !loadedForRead) {
            try {
                employment = employmentService.getEmployment(client.getId()).orElse(null);
            } catch (Exception e) {
                log.debug("Unable to resolve employment for client {}: {}", client.getId(), e.getMessage());
            }
        }
        if (employment != null) {
            employmentStatus = employment.getEmploymentStatus();
            educationLevel = employment.getEducationLevel();
            numberOfDependents = employment.getDependents();
        }

        // Get portal access from ClientPortalSettings and AuthIdentity
        Boolean hasPortalAccess = null;
        String portalEmail = null;
        Boolean emailNotifications = null;
        Instant lastLogin = null;
        try {
            if (client.getAuthIdentity() != null) {
                AuthIdentity authIdentity = client.getAuthIdentity();
                portalEmail = authIdentity.getLoginIdentifier();
                lastLogin = authIdentity.getLastSuccessfulLogin();
            }
        } catch (org.hibernate.LazyInitializationException e) {
            log.warn("LazyInitializationException when accessing authIdentity for client {}", client.getId());
            try {
                Optional<Client> reloaded = clientRepository.findById(client.getId());
                if (reloaded.isPresent() && reloaded.get().getAuthIdentity() != null) {
                    AuthIdentity authIdentity = reloaded.get().getAuthIdentity();
                    portalEmail = authIdentity.getLoginIdentifier();
                    lastLogin = authIdentity.getLastSuccessfulLogin();
                }
            } catch (Exception ignored) {
                // Keep portal fields null if not resolvable
            }
        }
        Optional<ClientPortalSettings> settings = loadedForRead
                ? Optional.ofNullable(client.getPortalSettings())
                : portalSettingsService.findByClientId(client.getId());
        if (settings.isPresent()) {
            hasPortalAccess = settings.get().getHasPortalAccess();
            emailNotifications = settings.get().getEmailNotifications();
        }

        // Get emergency contact from normalized entities
        String emergencyContactName = null;
        String emergencyContactPhone = null;
        String emergencyContactRelationship = null;
        try {
            List<ClientContact> loadedContacts = loadedForRead ? client.getContacts() : contactService.getContacts(client.getId());
            List<ClientContact> emergencyContacts = loadedContacts.stream()
                    .filter(c -> c.getContactType() == ContactType.EMERGENCY_CONTACT)
                    .sorted(Comparator.comparing(ClientContact::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(ClientContact::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
            if (!emergencyContacts.isEmpty()) {
                ClientContact emergency = emergencyContacts.get(0);
                emergencyContactName = emergency.getContactPersonName();
                emergencyContactPhone = emergency.getContactValue();
                emergencyContactRelationship = emergency.getRelationship();
            }
        } catch (Exception e) {
            log.debug("Error getting emergency contact for client {}: {}", client.getId(), e.getMessage());
        }

        Long checklistCount = 0L;
        Long documentCount = 0L;
        try {
            checklistCount = clientChecklistRepository.countByClientIdAndIsDeletedFalse(client.getId());
        } catch (Exception e) {
            log.debug("Error getting checklist count for client {}: {}", client.getId(), e.getMessage());
        }
        try {
            documentCount = documentRepository.countByClientIdAndIsDeletedFalse(client.getId());
        } catch (Exception e) {
            log.debug("Error getting document count for client {}: {}", client.getId(), e.getMessage());
        }

        return ClientResponse.builder()
                .id(client.getId())
                .clientId(client.getClientId())
                .fullName(client.getFullName())
                .email(email)
                .phone(phone)
                .dateOfBirth(client.getDateOfBirth())
                .gender(resolveOptionLabel(SystemOptionCategories.GENDER, client.getGender()))
                .maritalStatus(resolveOptionLabel(SystemOptionCategories.MARITAL_STATUS, client.getMaritalStatus()))
                .preferredLanguage(client.getPreferredLanguage())
                .pronouns(client.getPronouns())
                .timezone(resolveClientTimezone(client))
                .status(resolveOptionLabel(SystemOptionCategories.CLIENT_STATUS, client.getStatus()))
                .stage(resolveStageLabel(client.getStage()))
                .clientType(resolveOptionLabel(SystemOptionCategories.CLIENT_TYPE, client.getClientType()))
                .assignedTherapistId(assignedTherapistId)
                .assignedTherapistName(assignedTherapistName)
                .streetAddress1(primaryAddress != null ? primaryAddress.getStreetAddress1() : null)
                .streetAddress2(primaryAddress != null ? primaryAddress.getStreetAddress2() : null)
                .city(primaryAddress != null ? primaryAddress.getCity() : null)
                .province(primaryAddress != null ? primaryAddress.getStateProvince() : null)
                .postalCode(primaryAddress != null ? primaryAddress.getPostalCode() : null)
                .country(primaryAddress != null ? primaryAddress.getCountry() : null)
                .emergencyContactName(emergencyContactName)
                .emergencyContactPhone(emergencyContactPhone)
                .emergencyContactRelationship(emergencyContactRelationship)
                .insuranceProvider(insuranceProvider)
                .policyNumber(policyNumber)
                .groupNumber(groupNumber)
                .insuranceType(insuranceType)
                .insurancePhone(insurancePhone)
                .copayAmount(copayAmount)
                .deductible(deductible)
                .referrerName(referrerName)
                .referralDate(referralDate)
                .startDate(client.getStartDate())
                .referenceNumber(referenceNumber)
                .clientSource(clientSource)
                .referralNotes(referralNotes)
                .hasPortalAccess(hasPortalAccess)
                .portalEmail(portalEmail)
                .emailNotifications(emailNotifications)
                .lastLogin(lastLogin)
                .employmentStatus(employmentStatus)
                .educationLevel(educationLevel)
                .numberOfDependents(numberOfDependents)
                .notes(client.getNotes())
                .needsFollowUp(client.getNeedsFollowUp())
                .priority(client.getFollowUpPriority())
                .followUpDate(client.getFollowUpDate())
                .followUpNotes(client.getFollowUpNotes())
                .serviceType(resolveOptionLabel(SystemOptionCategories.SERVICE_TYPE, client.getServiceType()))
                .serviceFrequency(resolveOptionLabel(SystemOptionCategories.SERVICE_FREQUENCY, client.getServiceFrequency()))
                .treatmentModality(resolveOptionLabel(
                        SystemOptionCategories.TREATMENT_MODALITIES, client.getTreatmentModality()))
                .checklistCount(checklistCount)
                .documentCount(documentCount)
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .lastSessionDate(resolveLastHeldSessionDate(client))
                .nextAppointmentDate(resolveNextAppointmentDate(client))
                .build();
    }

    private Instant resolveLastHeldSessionDate(Client client) {
        if (client == null || client.getId() == null) {
            return null;
        }
        try {
            return sessionRepository.findLastHeldSessionDate(client.getId(), Instant.now());
        } catch (Exception e) {
            log.debug("Could not resolve last held session for client {}: {}", client.getId(), e.getMessage());
            // Fall back only if stored value is not in the future
            Instant stored = client.getLastSessionDate();
            if (stored != null && !stored.isAfter(Instant.now())) {
                return stored;
            }
            return null;
        }
    }

    private Instant resolveNextAppointmentDate(Client client) {
        if (client == null || client.getId() == null) {
            return null;
        }
        try {
            return sessionRepository.findNextAppointmentDate(client.getId(), Instant.now());
        } catch (Exception e) {
            log.debug("Could not resolve next appointment for client {}: {}", client.getId(), e.getMessage());
            Instant stored = client.getNextAppointmentDate();
            if (stored != null && stored.isAfter(Instant.now())) {
                return stored;
            }
            return null;
        }
    }

    private ClientHistoryResponse toHistoryResponse(ClientHistory history) {
        return ClientHistoryResponse.builder()
                .id(history.getId())
                .clientId(history.getClient() != null ? history.getClient().getId() : null)
                .eventSource(history.getEventSource() != null ? history.getEventSource().getDisplayName() : null)
                .eventType(history.getEventType() != null ? history.getEventType().getDisplayName() : null)
                .fromValue(history.getFromValue())
                .toValue(history.getToValue())
                .description(history.getDescription())
                .createdByUserId(history.getCreatedByUser() != null ? history.getCreatedByUser().getId() : null)
                .createdByName(history.getCreatedByName())
                .createdAt(history.getCreatedAt())
                .build();
    }

    private ClientHistoryResponse toTimelineHistoryResponse(ClientHistory history) {
        String createdByDisplayName = StringUtils.hasText(history.getCreatedByName())
                ? history.getCreatedByName()
                : history.getCreatedByDisplayName();

        return ClientHistoryResponse.builder()
                .id(history.getId())
                .clientId(history.getClient() != null ? history.getClient().getId() : null)
                .eventSource(history.getEventSource() != null ? history.getEventSource().getDisplayName() : null)
                .eventType(toTimelineEventType(history))
                .fromValue(history.getFromValue())
                .toValue(history.getToValue())
                .description(history.getDescription())
                .createdByUserId(history.getCreatedByUser() != null ? history.getCreatedByUser().getId() : null)
                .createdByName(createdByDisplayName)
                .createdAt(history.getCreatedAt())
                .build();
    }

    private String toTimelineEventType(ClientHistory history) {
        if (history == null || history.getEventType() == null) {
            return "event";
        }

        return switch (history.getEventType()) {
            case CREATED -> isFileCreatedEvent(history) ? "file_created" : "client_created";
            case UPDATED -> "client_updated";
            case DELETED -> "client_deleted";
            case RESTORED -> "client_restored";
            case FILE_CLOSED -> "file_closed";
            case FILE_REOPENED -> "file_reopened";
            case STATUS_CHANGED -> "status_change";
            case STAGE_CHANGED -> "stage_change";
            case THERAPIST_ASSIGNED, THERAPIST_CHANGED, THERAPIST_UNASSIGNED -> "therapist_assignment";
            case PORTAL_ACTIVATED -> isPortalActivationResentEvent(history) ? "portal_activation_resent" : "portal_activated";
            case PORTAL_DEACTIVATED -> "portal_deactivated";
            case PORTAL_PASSWORD_RESET -> "password_reset";
            case SESSION_SCHEDULED -> "session_scheduled";
            case SESSION_COMPLETED -> "session_completed";
            case SESSION_CANCELLED -> "session_cancelled";
            case DOCUMENT_UPLOADED -> "document_uploaded";
            case DOCUMENT_SHARED -> "document_shared";
            case FORM_ASSIGNED -> "form_assigned";
            case FORM_COMPLETED -> "form_completed";
            case CONSENT_GRANTED -> "consent_granted";
            case CONSENT_WITHDRAWN -> "consent_withdrawn";
            case FOLLOW_UP_SCHEDULED -> "follow_up_scheduled";
            case FOLLOW_UP_COMPLETED -> "follow_up_completed";
            case NOTE_ADDED -> "note_added";
            case DUPLICATE_MARKED -> "duplicate_marked";
            case DUPLICATE_UNMARKED -> "duplicate_unmarked";
            default -> history.getEventType().name().toLowerCase(Locale.ROOT);
        };
    }

    private boolean isFileCreatedEvent(ClientHistory history) {
        if (history == null || !StringUtils.hasText(history.getDescription())) {
            return false;
        }
        String description = history.getDescription().toLowerCase(Locale.ROOT);
        return description.contains("file created") || description.contains("client file created");
    }

    private boolean isPortalActivationResentEvent(ClientHistory history) {
        if (history == null || !StringUtils.hasText(history.getDescription())) {
            return false;
        }
        return history.getDescription().toLowerCase(Locale.ROOT).contains("resent");
    }

    private Comparator<ClientHistory> clientHistoryTimelineComparator() {
        return Comparator
                .comparing(
                        (ClientHistory h) -> truncateToSecond(h != null ? h.getCreatedAt() : null),
                        Comparator.reverseOrder())
                .thenComparing(ClientHistory::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private Comparator<ClientHistory> clientHistoryCreatedAtAscComparator() {
        return Comparator
                .comparing(ClientHistory::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ClientHistory::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private Instant truncateToSecond(Instant instant) {
        return instant == null ? Instant.EPOCH : instant.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    }

    private String normalizeStageLabel(String stage) {
        if (!StringUtils.hasText(stage)) {
            return null;
        }
        return stage.trim();
    }

    private Instant resolveStageFallbackStart(Client client) {
        if (client == null) {
            return null;
        }
        if (client.getStartDate() != null) {
            return client.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        }
        return client.getCreatedAt();
    }

    private boolean isEmailHistoryEvent(ClientHistory history) {
        if (history == null) {
            return false;
        }

        if (history.getEventType() == ClientEventType.PORTAL_ACTIVATED
                || history.getEventType() == ClientEventType.PORTAL_DEACTIVATED
                || history.getEventType() == ClientEventType.PORTAL_PASSWORD_RESET) {
            return true;
        }

        String description = history.getDescription() == null ? "" : history.getDescription().toLowerCase(Locale.ROOT);
        String metadata = history.getMetadata() == null ? "" : history.getMetadata().toLowerCase(Locale.ROOT);
        return description.contains("email")
                || description.contains("portal activation")
                || description.contains("activation")
                || metadata.contains("email");
    }

    private Long requireClientId(Client client) {
        Objects.requireNonNull(client, "Client is required");
        return Objects.requireNonNull(client.getId(), "Client id must not be null");
    }

    private void copyClientFields(Client source, Client target) {
        target.setId(source.getId());
        target.setStatus(source.getStatus());
        target.setStage(source.getStage());
        target.setAssignedTherapist(source.getAssignedTherapist());
    }

    private <T> void updateIfNotNull(java.util.function.Consumer<T> setter, T value) {
        if (value != null) {
            setter.accept(value);
        }
    }

    // DEPRECATED: Use permission checks instead
    // This method is kept for backward compatibility but should not be used
    @Deprecated
    private boolean hasRole(AuthPrincipal principal, String roleName) {
        if (principal == null || principal.getAuthorities() == null) {
            return false;
        }
        return principal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + roleName));
    }

    // ========== BULK OPERATIONS ==========

    @Transactional
    public BulkUploadResponse bulkUploadClients(BulkUploadRequest request, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        if (request.getClients() == null || request.getClients().isEmpty()) {
            throw new BadRequestException("Invalid input: clients must be a non-empty array");
        }

        int total = request.getClients().size();
        int successful = 0;
        int failed = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        for (int i = 0; i < request.getClients().size(); i++) {
            Map<String, Object> clientData = request.getClients().get(i);
            try {
                // Convert map to CreateClientRequest
                CreateClientRequest createRequest = mapToCreateClientRequest(clientData);

                // Create client
                createClient(createRequest, requester, ipAddress);
                successful++;
            } catch (Exception e) {
                failed++;
                Map<String, Object> error = new HashMap<>();
                error.put("row", i + 1);
                error.put("data", clientData);
                error.put("message", e.getMessage() != null ? e.getMessage() : "Unknown error");
                errors.add(error);
            }
        }

        // Flush all changes to ensure persistence
        clientRepository.flush();

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "bulk_upload_clients", null, ipAddress, true);

        return BulkUploadResponse.builder()
                .total(total)
                .successful(successful)
                .failed(failed)
                .errors(errors)
                .build();
    }

    @Transactional
    public BulkOperationResponse bulkUpdateStage(BulkUpdateStageRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Only admin and supervisor can perform bulk updates
        if (!permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL") && !permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM")) {
            throw new ForbiddenException(
                    "Access denied. Only administrators and supervisors can perform bulk updates.");
        }

        if (request.getClientIds() == null || request.getClientIds().isEmpty()) {
            throw new BadRequestException("Invalid input: clientIds must be a non-empty array");
        }

        String stageKey = resolveStageKey(request.getStage());

        // For supervisors, verify scope
        final List<Long> supervisedTherapistIds;
        if (permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM")) {
            supervisedTherapistIds = supervisorAssignmentRepository.findBySupervisorId(currentUserService.requireCurrentUser(requester).getId()).stream()
                    .map(SupervisorAssignment::getTherapist)
                    .filter(Objects::nonNull)
                    .map(User::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (supervisedTherapistIds.isEmpty()) {
                throw new ForbiddenException("You have no supervised therapists");
            }

            // Verify all clients belong to supervised therapists
            List<Client> clients = clientRepository.findAllById(request.getClientIds());
            List<Long> unauthorizedClientIds = clients.stream()
                    .filter(c -> c.getAssignedTherapist() != null &&
                            !supervisedTherapistIds.contains(c.getAssignedTherapist().getId()))
                    .map(Client::getId)
                    .collect(Collectors.toList());

            if (!unauthorizedClientIds.isEmpty()) {
                throw new ForbiddenException("You can only update clients assigned to therapists you supervise");
            }
        }

        int total = request.getClientIds().size();
        int successful = 0;
        int failed = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        for (Long clientId : request.getClientIds()) {
            try {
                // Verify client exists
                clientRepository.findById(clientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

                UpdateClientRequest updateRequest = new UpdateClientRequest();
                updateRequest.setStage(request.getStage());
                updateClient(clientId, updateRequest, requester, ipAddress);
                successful++;
            } catch (Exception e) {
                failed++;
                Map<String, Object> error = new HashMap<>();
                error.put("clientId", clientId);
                error.put("message", e.getMessage() != null ? e.getMessage() : "Unknown error");
                errors.add(error);
            }
        }

        // Flush all changes to ensure persistence
        clientRepository.flush();

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "bulk_update_stage", null, ipAddress, true);

        return BulkOperationResponse.builder()
                .total(total)
                .successful(successful)
                .failed(failed)
                .errors(errors)
                .build();
    }

    @Transactional
    public BulkOperationResponse bulkReassignTherapist(BulkReassignTherapistRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Only admin and supervisor can perform bulk reassignment
        if (!permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL") && !permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM")) {
            throw new ForbiddenException(
                    "Access denied. Only administrators and supervisors can perform bulk updates.");
        }

        if (request.getClientIds() == null || request.getClientIds().isEmpty()) {
            throw new BadRequestException("Invalid input: clientIds must be a non-empty array");
        }

        if (request.getTherapistIds() == null || request.getTherapistIds().isEmpty()) {
            throw new BadRequestException("Invalid input: therapistIds must be a non-empty array");
        }

        // For supervisors, verify scope
        final List<Long> supervisedTherapistIds;
        if (permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM")) {
            supervisedTherapistIds = supervisorAssignmentRepository.findBySupervisorId(currentUserService.requireCurrentUser(requester).getId()).stream()
                    .map(SupervisorAssignment::getTherapist)
                    .filter(Objects::nonNull)
                    .map(User::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (supervisedTherapistIds.isEmpty()) {
                throw new ForbiddenException("You have no supervised therapists");
            }

            // Verify all target therapists are supervised
            List<Long> unauthorizedTherapistIds = request.getTherapistIds().stream()
                    .filter(id -> !supervisedTherapistIds.contains(id))
                    .collect(Collectors.toList());

            if (!unauthorizedTherapistIds.isEmpty()) {
                throw new ForbiddenException("You can only reassign to therapists you supervise");
            }

            // Verify all clients belong to supervised therapists
            List<Client> clients = clientRepository.findAllById(request.getClientIds());
            List<Long> unauthorizedClientIds = clients.stream()
                    .filter(c -> c.getAssignedTherapist() != null &&
                            !supervisedTherapistIds.contains(c.getAssignedTherapist().getId()))
                    .map(Client::getId)
                    .collect(Collectors.toList());

            if (!unauthorizedClientIds.isEmpty()) {
                throw new ForbiddenException("You can only reassign clients assigned to therapists you supervise");
            }
        }

        int total = request.getClientIds().size();
        int successful = 0;
        int failed = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        Map<Long, Integer> distribution = new HashMap<>();

        // Initialize distribution counter
        request.getTherapistIds().forEach(id -> distribution.put(id, 0));

        // Distribute clients evenly or to single therapist
        if ("even".equals(request.getDistribution())) {
            // Sort therapists by current workload
            List<Map<String, Object>> therapistWorkloads = request.getTherapistIds().stream()
                    .map(id -> {
                        long count = clientRepository.countByAssignedTherapistId(id);
                        Map<String, Object> workload = new HashMap<>();
                        workload.put("therapistId", id);
                        workload.put("currentCount", count);
                        return workload;
                    })
                    .sorted(Comparator.comparingLong(w -> (Long) w.get("currentCount")))
                    .collect(Collectors.toList());

            int therapistIndex = 0;
            for (Long clientId : request.getClientIds()) {
                try {
                    Long therapistId = (Long) therapistWorkloads.get(therapistIndex).get("therapistId");

                    // Verify client exists
                    clientRepository.findById(clientId)
                            .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

                    UpdateClientRequest updateRequest = new UpdateClientRequest();
                    updateRequest.setAssignedTherapistId(therapistId);
                    updateClient(clientId, updateRequest, requester, ipAddress);

                    successful++;
                    distribution.put(therapistId, distribution.get(therapistId) + 1);
                    therapistWorkloads.get(therapistIndex).put("currentCount",
                            (Long) therapistWorkloads.get(therapistIndex).get("currentCount") + 1);

                    // Re-sort to keep balanced
                    therapistWorkloads.sort(Comparator.comparingLong(w -> (Long) w.get("currentCount")));
                    therapistIndex = 0;
                } catch (Exception e) {
                    failed++;
                    Map<String, Object> error = new HashMap<>();
                    error.put("clientId", clientId);
                    error.put("message", e.getMessage() != null ? e.getMessage() : "Unknown error");
                    errors.add(error);
                }
            }
        } else {
            // Assign all to single therapist (first in array)
            Long therapistId = request.getTherapistIds().get(0);
            for (Long clientId : request.getClientIds()) {
                try {
                    // Verify client exists
                    clientRepository.findById(clientId)
                            .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

                    UpdateClientRequest updateRequest = new UpdateClientRequest();
                    updateRequest.setAssignedTherapistId(therapistId);
                    updateClient(clientId, updateRequest, requester, ipAddress);

                    successful++;
                    distribution.put(therapistId, distribution.get(therapistId) + 1);
                } catch (Exception e) {
                    failed++;
                    Map<String, Object> error = new HashMap<>();
                    error.put("clientId", clientId);
                    error.put("message", e.getMessage() != null ? e.getMessage() : "Unknown error");
                    errors.add(error);
                }
            }
        }

        // Flush all changes to ensure persistence
        clientRepository.flush();

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "bulk_reassign_therapist", null, ipAddress, true);

        return BulkOperationResponse.builder()
                .total(total)
                .successful(successful)
                .failed(failed)
                .errors(errors)
                .distribution(distribution)
                .build();
    }

    @Transactional
    public BulkOperationResponse bulkUpdatePortalAccess(BulkPortalAccessRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Only admin can modify portal access (security concern)
        if (!permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL")) {
            throw new ForbiddenException("Access denied. Only administrators can modify portal access.");
        }

        if (request.getClientIds() == null || request.getClientIds().isEmpty()) {
            throw new BadRequestException("Invalid input: clientIds must be a non-empty array");
        }

        int total = request.getClientIds().size();
        int successful = 0;
        int failed = 0;
        int skipped = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        for (Long clientId : request.getClientIds()) {
            try {
                Client client = clientRepository.findById(clientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

                // Skip if enabling portal but no email (check normalized entities first)
                String emailForPortal = null;
                if (Boolean.TRUE.equals(request.getEnable())) {
                    // Get email from normalized entities
                    try {
                        Optional<ClientContact> primaryEmail = contactService.getPrimaryEmail(clientId);
                        if (primaryEmail.isPresent() && StringUtils.hasText(primaryEmail.get().getContactValue())) {
                            emailForPortal = primaryEmail.get().getContactValue();
                        }
                    } catch (Exception e) {
                        log.debug("Error getting primary email for client {}: {}", clientId, e.getMessage());
                    }

                    if (!StringUtils.hasText(emailForPortal)) {
                        skipped++;
                        Map<String, Object> error = new HashMap<>();
                        error.put("clientId", clientId);
                        error.put("message", "Skipped: No email address");
                        errors.add(error);
                        continue;
                    }
                }

                UpdateClientRequest updateRequest = new UpdateClientRequest();
                updateRequest.setHasPortalAccess(request.getEnable());
                // Use email from normalized entities or deprecated field if enabling
                if (Boolean.TRUE.equals(request.getEnable()) && StringUtils.hasText(emailForPortal)) {
                    updateRequest.setPortalEmail(emailForPortal);
                }
                updateClient(clientId, updateRequest, requester, ipAddress);
                successful++;
            } catch (Exception e) {
                failed++;
                Map<String, Object> error = new HashMap<>();
                error.put("clientId", clientId);
                error.put("message", e.getMessage() != null ? e.getMessage() : "Unknown error");
                errors.add(error);
            }
        }

        // Flush all changes to ensure persistence
        clientRepository.flush();

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "bulk_portal_access", null, ipAddress, true);

        return BulkOperationResponse.builder()
                .total(total)
                .successful(successful)
                .failed(failed)
                .skipped(skipped)
                .errors(errors)
                .build();
    }

    @Transactional
    public BulkOperationResponse bulkUpdateStatus(BulkUpdateStatusRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Only admin and supervisor can perform bulk updates
        if (!permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL") && !permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM")) {
            throw new ForbiddenException(
                    "Access denied. Only administrators and supervisors can perform bulk updates.");
        }

        if (request.getClientIds() == null || request.getClientIds().isEmpty()) {
            throw new BadRequestException("Invalid input: clientIds must be a non-empty array");
        }

        String statusKey = systemOptionResolverService.requireOptionKey(
                SystemOptionCategories.CLIENT_STATUS, request.getStatus());

        // For supervisors, verify scope
        if (permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM")) {
            List<Long> supervisedTherapistIds = supervisorAssignmentRepository.findBySupervisorId(currentUserService.requireCurrentUser(requester).getId())
                    .stream()
                    .map(SupervisorAssignment::getTherapist)
                    .filter(Objects::nonNull)
                    .map(User::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (supervisedTherapistIds.isEmpty()) {
                throw new ForbiddenException("You have no supervised therapists");
            }

            // Verify all clients belong to supervised therapists
            List<Client> clients = clientRepository.findAllById(request.getClientIds());
            List<Long> unauthorizedClientIds = clients.stream()
                    .filter(c -> c.getAssignedTherapist() != null &&
                            !supervisedTherapistIds.contains(c.getAssignedTherapist().getId()))
                    .map(Client::getId)
                    .collect(Collectors.toList());

            if (!unauthorizedClientIds.isEmpty()) {
                throw new ForbiddenException("You can only update clients assigned to therapists you supervise");
            }
        }

        int total = request.getClientIds().size();
        int successful = 0;
        int failed = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        for (Long clientId : request.getClientIds()) {
            try {
                // Verify client exists
                clientRepository.findById(clientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

                UpdateClientRequest updateRequest = new UpdateClientRequest();
                updateRequest.setStatus(statusKey);
                updateClient(clientId, updateRequest, requester, ipAddress);
                successful++;
            } catch (Exception e) {
                failed++;
                Map<String, Object> error = new HashMap<>();
                error.put("clientId", clientId);
                error.put("message", e.getMessage() != null ? e.getMessage() : "Unknown error");
                errors.add(error);
            }
        }

        // Flush all changes to ensure persistence
        clientRepository.flush();

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "bulk_update_status", null, ipAddress, true);

        return BulkOperationResponse.builder()
                .total(total)
                .successful(successful)
                .failed(failed)
                .errors(errors)
                .build();
    }

    @Transactional
    public String exportClientsToCsv(AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(requester, "Requester is required");

        List<Client> allClients = clientRepository.findAll();
        final int columnCount = 6;

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Client ID,First Name,Last Name,Status,Assigned Therapist\n");

        for (Client client : allClients) {
            String[] nameParts = splitFullName(client.getFullName());
            csv.append(escapeCsvField(client.getId() != null ? client.getId().toString() : "")).append(",");
            csv.append(escapeCsvField(client.getClientId())).append(",");
            csv.append(escapeCsvField(nameParts[0])).append(",");
            csv.append(escapeCsvField(nameParts[1])).append(",");
            csv.append(escapeCsvField(resolveOptionLabel(SystemOptionCategories.CLIENT_STATUS, client.getStatus()))).append(",");
            csv.append(escapeCsvField(
                    client.getAssignedTherapist() != null ? client.getAssignedTherapist().getFullName() : ""))
                    .append("\n");
        }

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                "client_exported", null, ipAddress, true,
                "format=csv, rowCount=" + allClients.size() + ", columnCount=" + columnCount);
        return csv.toString();
    }

    private static String[] splitFullName(String fullName) {
        if (!StringUtils.hasText(fullName)) {
            return new String[] { "", "" };
        }
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        if (space <= 0) {
            return new String[] { trimmed, "" };
        }
        return new String[] { trimmed.substring(0, space), trimmed.substring(space + 1).trim() };
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getClientSessions(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Verify client exists and user has access
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        validateClientAccess(client, requester);

        List<Session> sessions = loadClientSessionsForView(clientId);

        return sessions.stream()
                .map(this::toSessionResponse)
                .sorted(Comparator.comparing(SessionResponse::getSessionDate).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClientSessionSummaryResponse getClientSessionSummary(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        List<Session> sessions = loadClientSessionsForView(clientId);

        int completed = 0;
        int scheduled = 0;
        int missedCancelled = 0;

        for (Session session : sessions) {
            String status = session.getStatus();
            if (status == null) {
                continue;
            }
            if (SystemOptionKeyMatcher.matchesAny(status, "completed")) {
                completed++;
            } else if (SystemOptionKeyMatcher.matchesAny(status, "cancelled", "no-show", "no_show")) {
                missedCancelled++;
            } else if (SystemOptionKeyMatcher.matchesAny(status, "scheduled", "confirmed", "in_progress", "in-progress", "overdue", "rescheduling")) {
                scheduled++;
            }
        }

        int conflicts = getClientSessionConflicts(clientId, requester).getConflicts().size();

        return ClientSessionSummaryResponse.builder()
                .clientId(clientId)
                .totalSessions(sessions.size())
                .completed(completed)
                .scheduled(scheduled)
                .missedCancelled(missedCancelled)
                .conflicts(conflicts)
                .build();
    }

    @Transactional(readOnly = true)
    public ClientSessionConflictsResponse getClientSessionConflicts(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Verify client exists and user has access
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        validateClientAccess(client, requester);

        Instant now = Instant.now();
        List<Session> allSessions = loadClientSessionsForView(clientId);

        // Find sessions on the same day
        Map<LocalDate, List<Session>> sessionsByDate = allSessions.stream()
                .filter(s -> s.getSessionDate() != null && s.getSessionDate().isAfter(now))
                .collect(Collectors.groupingBy(s -> s.getSessionDate()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()));

        List<ClientSessionConflictsResponse.ConflictGroup> conflicts = new ArrayList<>();

        for (Map.Entry<LocalDate, List<Session>> entry : sessionsByDate.entrySet()) {
            if (entry.getValue().size() > 1) {
                // Multiple sessions on the same day - potential conflict
                List<SessionResponse> conflictSessions = entry.getValue().stream()
                        .map(this::toSessionResponse)
                        .collect(Collectors.toList());

                conflicts.add(ClientSessionConflictsResponse.ConflictGroup.builder()
                        .date(entry.getKey())
                        .sessions(conflictSessions)
                        .build());
            }
        }

        return ClientSessionConflictsResponse.builder()
                .clientId(clientId)
                .conflicts(conflicts)
                .build();
    }

    private SessionResponse toSessionResponse(Session session) {
        // Get Zoom integration data
        SessionIntegration zoomIntegration = getZoomIntegration(session);

        return SessionResponse.builder()
                .id(session.getId())
                .clientId(session.getClient().getId())
                .clientName(session.getClient().getFullName())
                .therapistId(session.getTherapist() != null ? session.getTherapist().getId() : null)
                .therapistName(session.getTherapist() != null ? session.getTherapist().getFullName() : null)
                .sessionDate(session.getSessionDate())
                .duration(session.getDuration())
                .sessionType(session.getClinicalSessionType() != null
                        ? session.getClinicalSessionType()
                        : (session.getService() != null
                                ? (session.getService().getCategory() != null
                                        ? session.getService().getCategory()
                                        : session.getService().getServiceName())
                                : null))
                .sessionMode(session.getSessionType())
                .status(session.getStatus())
                .serviceId(session.getService() != null ? session.getService().getId() : null)
                .serviceName(session.getService() != null ? session.getService().getServiceName() : null)
                .roomId(session.getRoom() != null ? session.getRoom().getId() : null)
                .roomName(session.getRoom() != null ? session.getRoom().getRoomName() : null)
                .notes(session.getNotes())
                .zoomEnabled(zoomIntegration != null)
                .zoomMeetingId(zoomIntegration != null ? zoomIntegration.getMeetingId() : null)
                .zoomJoinUrl(zoomIntegration != null ? zoomIntegration.getJoinUrl() : null)
                .zoomPassword(zoomIntegration != null ? zoomIntegration.getPassword() : null)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    /**
     * Helper method to get Zoom integration from a session
     */
    private SessionIntegration getZoomIntegration(Session session) {
        if (session.getIntegrations() == null || session.getIntegrations().isEmpty()) {
            return null;
        }
        return session.getIntegrations().stream()
                .filter(integration -> "zoom".equals(integration.getProvider()))
                .findFirst()
                .orElse(null);
    }

    // Helper methods for bulk operations

    private CreateClientRequest mapToCreateClientRequest(Map<String, Object> data) {
        CreateClientRequest request = new CreateClientRequest();

        // Validate required field
        if (data.get("fullName") == null || data.get("fullName").toString().trim().isEmpty()) {
            throw new BadRequestException("Full name is required");
        }
        request.setFullName(data.get("fullName").toString());

        if (data.get("email") != null) {
            request.setEmail(data.get("email").toString());
        }
        if (data.get("phone") != null) {
            request.setPhone(data.get("phone").toString());
        }
        if (data.get("dateOfBirth") != null) {
            try {
                request.setDateOfBirth(java.time.LocalDate.parse(data.get("dateOfBirth").toString()));
            } catch (Exception e) {
                log.warn("Invalid dateOfBirth format: {}", data.get("dateOfBirth"));
            }
        }
        if (data.get("gender") != null) {
            request.setGender(data.get("gender").toString());
        }
        if (data.get("status") != null) {
            request.setStatus(data.get("status").toString());
        }
        if (data.get("stage") != null) {
            request.setStage(data.get("stage").toString());
        }
        if (data.get("clientType") != null) {
            request.setClientType(data.get("clientType").toString());
        }
        if (data.get("assignedTherapistId") != null) {
            try {
                request.setAssignedTherapistId(Long.parseLong(data.get("assignedTherapistId").toString()));
            } catch (NumberFormatException e) {
                // Try to find by username/name
                if (data.get("assignedTherapist") != null) {
                    String therapistIdentifier = data.get("assignedTherapist").toString();
                    userRepository.findByEmail(therapistIdentifier)
                            .ifPresent(user -> request.setAssignedTherapistId(user.getId()));
                }
            }
        }
        // Add more field mappings as needed

        return request;
    }

    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        String stripped = field.stripLeading();
        if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0) {
            field = "'" + field;
        }
        // Escape quotes and wrap in quotes if contains comma or quote
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    private void assertAdmin(AuthPrincipal principal, String message) {
        if (!hasRole(principal, "ADMIN") && !hasRole(principal, "SUPER_ADMIN")) {
            throw new ForbiddenException(message);
        }
    }

    private void assertCanReopenClientFile(AuthPrincipal principal, Client client) {
        if (hasRole(principal, "ADMIN") || hasRole(principal, "SUPER_ADMIN")) {
            return;
        }
        if (hasRole(principal, "SUPERVISOR")
                && permissionChecker.hasPermission(principal, "CLIENT_EDIT")) {
            validateClientAccess(client, principal);
            return;
        }
        throw new ForbiddenException("Only administrators can reopen this file");
    }

    /**
     * Normalize update payload before validation: strip non-admin therapist assignment and empty strings.
     */
    private void sanitizeUpdateClientRequest(UpdateClientRequest request, AuthPrincipal requester) {
        if (!hasRole(requester, "ADMIN") && !hasRole(requester, "SUPER_ADMIN")) {
            request.setAssignedTherapistId(null);
        }

        if (request.getStatus() != null && !StringUtils.hasText(request.getStatus())) {
            request.setStatus(null);
        }
        if (request.getStage() != null && !StringUtils.hasText(request.getStage())) {
            request.setStage(null);
        }
        if (request.getFullName() != null && !StringUtils.hasText(request.getFullName())) {
            request.setFullName(null);
        }
        if (request.getEmail() != null && !StringUtils.hasText(request.getEmail())) {
            request.setEmail(null);
        } else if (StringUtils.hasText(request.getEmail())) {
            request.setEmail(request.getEmail().trim());
        }
        if (request.getPhone() != null && !StringUtils.hasText(request.getPhone())) {
            request.setPhone(null);
        } else if (StringUtils.hasText(request.getPhone())) {
            request.setPhone(request.getPhone().trim());
        }
        if (request.getGender() != null && !StringUtils.hasText(request.getGender())) {
            request.setGender(null);
        }
        if (request.getMaritalStatus() != null && !StringUtils.hasText(request.getMaritalStatus())) {
            request.setMaritalStatus(null);
        }
        if (request.getPreferredLanguage() != null && !StringUtils.hasText(request.getPreferredLanguage())) {
            request.setPreferredLanguage(null);
        }
        if (request.getPronouns() != null && !StringUtils.hasText(request.getPronouns())) {
            request.setPronouns(null);
        }
        if (request.getTimezone() != null && !StringUtils.hasText(request.getTimezone())) {
            request.setTimezone(null);
        }
        if (request.getClientType() != null && !StringUtils.hasText(request.getClientType())) {
            request.setClientType(null);
        }
        if (request.getPortalEmail() != null && !StringUtils.hasText(request.getPortalEmail())) {
            request.setPortalEmail(null);
        }
    }

    private boolean isInactiveStatusValue(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String statusKey = systemOptionResolverService.resolveOptionKey(SystemOptionCategories.CLIENT_STATUS, status);
        return SystemOptionKeyMatcher.matchesAny(statusKey != null ? statusKey : status, "inactive");
    }

    // ========== NORMALIZED ENTITY HELPERS ==========

    /**
     * Create normalized entities after client creation
     */
    /**
     * Dual-write blind indexes for client + contacts when search mode is dual/blind_only.
     */
    private void refreshBlindIndexesAfterContactChanges(Long clientId) {
        if (!blindIndexService.getSearchMode().writesBlindIndexes()) {
            return;
        }
        List<ClientContact> contacts = contactService.getContacts(clientId);
        // The client-level indexes (including orphan-removal name tokens) were
        // already rebuilt before the client was saved. Rebuilding them here can
        // make Hibernate insert replacements before deleting the old tokens and
        // violate uq_client_name_token_ord for a newly-created client.
        blindIndexService.updateContactBlindIndexes(contacts);
        for (ClientContact contact : contacts) {
            contactRepository.save(contact);
        }
    }

    private void createNormalizedEntities(Long clientId, CreateClientRequest request) {
        try {
            // Create primary email contact if email provided
            if (StringUtils.hasText(request.getEmail())) {
                contactService.upsertPrimaryEmail(clientId, request.getEmail());
            }

            // Create primary phone contact if phone provided
            if (StringUtils.hasText(request.getPhone())) {
                contactService.upsertPrimaryPhone(clientId, request.getPhone());
            }

            // Create primary address if address fields provided
            if (StringUtils.hasText(request.getStreetAddress1())
                    || StringUtils.hasText(request.getCity())
                    || StringUtils.hasText(request.getAddressLegacy())) {
                try {
                    String streetAddress1 = StringUtils.hasText(request.getStreetAddress1())
                            ? request.getStreetAddress1()
                            : request.getAddressLegacy();
                    ClientAddress address = ClientAddress.builder()
                            .addressType(AddressType.HOME)
                            .streetAddress1(streetAddress1)
                            .streetAddress2(request.getStreetAddress2())
                            .city(request.getCity())
                            .stateProvince(request.getProvince())
                            .postalCode(request.getPostalCode())
                            .country(request.getCountry())
                            .addressLegacy(request.getAddressLegacy())
                            .stateLegacy(request.getStateLegacy())
                            .zipCodeLegacy(request.getZipCodeLegacy())
                            .isPrimary(true)
                            .isCurrent(true)
                            .isVerified(false)
                            .displayOrder(1)
                            .build();
                    addressService.createAddress(clientId, address);
                } catch (Exception e) {
                    log.warn("Failed to create address for client {}: {}", clientId, e.getMessage());
                }
            }

            createInsuranceFromCreateRequest(clientId, request);

            // Create referral if referral fields provided
            if (StringUtils.hasText(request.getReferrerName())
                    || StringUtils.hasText(request.getReferringPersonName())
                    || request.getReferralDate() != null
                    || StringUtils.hasText(request.getLegacyReferral())
                    || StringUtils.hasText(request.getReferralType())
                    || StringUtils.hasText(request.getReferralNotes())) {
                try {
                    String referrerName = StringUtils.hasText(request.getReferrerName())
                            ? request.getReferrerName()
                            : request.getReferringPersonName();
                    String rawClientSource = StringUtils.hasText(request.getClientSource())
                            ? request.getClientSource()
                            : request.getLegacyReferral();
                    String clientSource = parseClientSource(rawClientSource);
                    if (clientSource == null && StringUtils.hasText(rawClientSource)) {
                        clientSource = rawClientSource.trim();
                    }
                    ClientReferral referral = ClientReferral.builder()
                            .referrerName(referrerName)
                            .referralDate(request.getReferralDate())
                            .startDate(request.getStartDate())
                            .referenceNumber(request.getReferenceNumber())
                            .clientSource(clientSource)
                            .referralSource(convertReferralSource(clientSource))
                            .referralType(convertReferralType(request.getReferralType()))
                            .referralNotes(request.getReferralNotes())
                            .build();
                    referralService.createReferral(clientId, referral);
                } catch (Exception e) {
                    log.warn("Failed to create referral for client {}: {}", clientId, e.getMessage());
                }
            }

            // Create emergency contact if provided
            if (StringUtils.hasText(request.getEmergencyContactName())
                    || StringUtils.hasText(request.getEmergencyContactPhone())
                    || StringUtils.hasText(request.getEmergencyContactLegacy())) {
                try {
                    String contactName = StringUtils.hasText(request.getEmergencyContactName())
                            ? request.getEmergencyContactName()
                            : request.getEmergencyContactLegacy();
                    String emergencyPhone = StringUtils.hasText(request.getEmergencyContactPhone())
                            ? request.getEmergencyContactPhone()
                            : "N/A";
                    ClientContact emergencyContact = ClientContact.builder()
                            .contactType(ContactType.EMERGENCY_CONTACT)
                            .contactValue(emergencyPhone)
                            .contactPersonName(contactName)
                            .relationship(request.getEmergencyContactRelationship())
                            .notes(request.getEmergencyContactLegacy())
                            .isPrimary(false)
                            .isVerified(false)
                            .displayOrder(3)
                            .build();
                    contactService.createContact(clientId, emergencyContact);
                } catch (Exception e) {
                    log.warn("Failed to create emergency contact for client {}: {}", clientId, e.getMessage());
                }
            }

            // Create employment if requested fields provided
            if (request.getEmploymentStatus() != null
                    || request.getEducationLevel() != null
                    || request.getDependents() != null) {
                try {
                    ClientEmployment employment = ClientEmployment.builder()
                            .employmentStatus(convertEmploymentStatus(request.getEmploymentStatus()))
                            .educationLevel(convertEducationLevel(request.getEducationLevel()))
                            .dependents(request.getDependents())
                            .build();
                    employmentService.createEmployment(clientId, employment);
                } catch (Exception e) {
                    log.warn("Failed to create employment for client {}: {}", clientId, e.getMessage());
                }
            }

            // Portal access enable + activation email is handled in createClient/updateClient
            // or PUT /clients/{id}/portal-access. Here we only persist settings changes.
        } catch (BadRequestException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating normalized entities for client {}: {}", clientId, e.getMessage(), e);
            // Don't fail client creation if normalized entities fail
        }
    }

    /**
     * Update normalized entities based on UpdateClientRequest.
     * This method updates contacts, addresses, insurance, referral, and
     * credentials.
     */
    private void updateNormalizedEntities(Long clientId, UpdateClientRequest request, AuthPrincipal requester) {
        try {
            // Update or create primary email contact
            if (request.isFieldPresent("email")) {
                contactService.upsertPrimaryEmail(clientId, request.getEmail());
            }

            // Update or create primary phone contact
            if (request.isFieldPresent("phone")) {
                contactService.upsertPrimaryPhone(clientId, request.getPhone());
            }

            // Update or create primary address
            if (request.isAnyFieldPresent("streetAddress1", "streetAddress2", "city", "province", "postalCode",
                    "country", "legacyAddress", "addressLegacy", "stateLegacy", "zipCodeLegacy")) {
                try {
                    Optional<ClientAddress> existingAddress = addressService.getPrimaryAddress(clientId);
                    if (existingAddress.isPresent()) {
                        addressService.patchAddress(existingAddress.get().getId(), address -> {
                            if (request.isFieldPresent("streetAddress1")) {
                                address.setStreetAddress1(request.getStreetAddress1());
                            }
                            if (request.isFieldPresent("streetAddress2")) {
                                address.setStreetAddress2(request.getStreetAddress2());
                            }
                            if (request.isFieldPresent("city")) {
                                address.setCity(request.getCity());
                            }
                            if (request.isFieldPresent("province")) {
                                address.setStateProvince(request.getProvince());
                            }
                            if (request.isFieldPresent("postalCode")) {
                                address.setPostalCode(request.getPostalCode());
                            }
                            if (request.isFieldPresent("country")) {
                                address.setCountry(request.getCountry());
                            }
                            if (request.isAnyFieldPresent("addressLegacy", "legacyAddress")) {
                                address.setAddressLegacy(request.getAddressLegacy());
                            }
                            if (request.isFieldPresent("stateLegacy")) {
                                address.setStateLegacy(request.getStateLegacy());
                            }
                            if (request.isFieldPresent("zipCodeLegacy")) {
                                address.setZipCodeLegacy(request.getZipCodeLegacy());
                            }
                        });
                    } else {
                        // Create new address
                        String streetAddress1 = request.getStreetAddress1() != null
                                ? request.getStreetAddress1()
                                : request.getAddressLegacy();
                        String city = request.getCity() != null ? request.getCity() : "Unknown";
                        ClientAddress address = ClientAddress.builder()
                                .addressType(AddressType.HOME)
                                .streetAddress1(streetAddress1)
                                .streetAddress2(request.getStreetAddress2())
                                .city(city)
                                .stateProvince(request.getProvince())
                                .postalCode(request.getPostalCode())
                                .country(request.getCountry())
                                .addressLegacy(request.getAddressLegacy())
                                .stateLegacy(request.getStateLegacy())
                                .zipCodeLegacy(request.getZipCodeLegacy())
                                .isPrimary(true)
                                .isCurrent(true)
                                .isVerified(false)
                                .displayOrder(1)
                                .build();
                        addressService.createAddress(clientId, address);
                    }
                } catch (Exception e) {
                    log.warn("Failed to update address for client {}: {}", clientId, e.getMessage());
                }
            }

            syncInsuranceFromUpdateRequest(clientId, request);

            // Update or create referral
            if (request.isAnyFieldPresent("referrerName", "referringPersonName", "referralDate", "referenceNumber",
                    "clientSource", "legacyReferral", "referralType", "referralNotes")) {
                try {
                    validateReferralDate(request.getReferralDate());
                    String referrerName = request.getReferrerName() != null
                            ? request.getReferrerName()
                            : request.getReferringPersonName();
                    String clientSource = request.getClientSource() != null
                            ? request.getClientSource()
                            : request.getLegacyReferral();
                    Optional<ClientReferral> existingReferral = referralService.getReferral(clientId);
                    if (existingReferral.isPresent()) {
                        // Update existing referral
                        ClientReferral referral = existingReferral.get();
                        if (request.isAnyFieldPresent("referrerName", "referringPersonName")) {
                            referral.setReferrerName(referrerName);
                        }
                        if (request.isFieldPresent("referralDate")) {
                            referral.setReferralDate(request.getReferralDate());
                        }
                        if (request.isFieldPresent("referenceNumber")) {
                            referral.setReferenceNumber(request.getReferenceNumber());
                        }
                        if (request.isAnyFieldPresent("clientSource", "legacyReferral")) {
                            referral.setClientSource(clientSource);
                            referral.setReferralSource(convertReferralSource(clientSource));
                        }
                        if (request.isFieldPresent("referralType")) {
                            referral.setReferralType(convertReferralType(request.getReferralType()));
                        }
                        if (request.isFieldPresent("referralNotes")) {
                            referral.setReferralNotes(request.getReferralNotes());
                        }
                        referralService.updateReferral(clientId, referral);
                    } else {
                        // Create new referral
                        ClientReferral referral = ClientReferral.builder()
                                .referrerName(referrerName)
                                .referralDate(request.getReferralDate())
                                .referenceNumber(request.getReferenceNumber())
                                .clientSource(clientSource)
                                .referralSource(convertReferralSource(clientSource))
                                .referralType(convertReferralType(request.getReferralType()))
                                .referralNotes(request.getReferralNotes())
                                .build();
                        referralService.createReferral(clientId, referral);
                    }
                } catch (BadRequestException | ResourceNotFoundException e) {
                    throw e;
                } catch (Exception e) {
                    log.warn("Failed to update referral for client {}: {}", clientId, e.getMessage());
                }
            }

            // Update or create emergency contact
            if (request.isAnyFieldPresent("emergencyContactName", "emergencyContactPhone",
                    "emergencyContactRelationship", "emergencyContactLegacy")) {
                try {
                    String contactName = request.getEmergencyContactName() != null
                            ? request.getEmergencyContactName()
                            : request.getEmergencyContactLegacy();
                    String contactPhone = request.getEmergencyContactPhone() != null
                            ? request.getEmergencyContactPhone()
                            : "N/A";
                    List<ClientContact> emergencyContacts = contactService.getContacts(clientId).stream()
                            .filter(c -> c.getContactType() == ContactType.EMERGENCY_CONTACT)
                            .collect(Collectors.toList());

                    if (!emergencyContacts.isEmpty()) {
                        // Update first emergency contact
                        ClientContact contact = emergencyContacts.get(0);
                        if (request.isAnyFieldPresent("emergencyContactName", "emergencyContactLegacy")) {
                            contact.setContactPersonName(contactName);
                        }
                        if (request.isFieldPresent("emergencyContactPhone")) {
                            contact.setContactValue(contactPhone);
                        }
                        if (request.isFieldPresent("emergencyContactRelationship")) {
                            contact.setRelationship(request.getEmergencyContactRelationship());
                        }
                        if (request.isFieldPresent("emergencyContactLegacy")) {
                            contact.setNotes(request.getEmergencyContactLegacy());
                        }
                        contactService.updateContact(contact.getId(), contact);
                    } else {
                        // Create new emergency contact
                        ClientContact emergencyContact = ClientContact.builder()
                                .contactType(ContactType.EMERGENCY_CONTACT)
                                .contactValue(contactPhone)
                                .contactPersonName(contactName)
                                .relationship(request.getEmergencyContactRelationship())
                                .notes(request.getEmergencyContactLegacy())
                                .isPrimary(false)
                                .isVerified(false)
                                .displayOrder(3)
                                .build();
                        contactService.createContact(clientId, emergencyContact);
                    }
                } catch (Exception e) {
                    log.warn("Failed to update emergency contact for client {}: {}", clientId, e.getMessage());
                }
            }

            // Update or create employment
            if (request.isAnyFieldPresent("employmentStatus", "educationLevel", "dependents", "numberOfDependents")) {
                try {
                    Optional<ClientEmployment> existingEmployment = employmentService.getEmployment(clientId);
                    if (existingEmployment.isPresent()) {
                        ClientEmployment employment = existingEmployment.get();
                        if (request.isFieldPresent("employmentStatus")) {
                            employment.setEmploymentStatus(StringUtils.hasText(request.getEmploymentStatus())
                                    ? convertEmploymentStatus(request.getEmploymentStatus())
                                    : null);
                        }
                        if (request.isFieldPresent("educationLevel")) {
                            employment.setEducationLevel(StringUtils.hasText(request.getEducationLevel())
                                    ? convertEducationLevel(request.getEducationLevel())
                                    : null);
                        }
                        if (request.isAnyFieldPresent("dependents", "numberOfDependents")) {
                            employment.setDependents(request.getDependents());
                        }
                        employmentService.updateEmployment(clientId, employment);
                    } else {
                        ClientEmployment employment = ClientEmployment.builder()
                                .employmentStatus(convertEmploymentStatus(request.getEmploymentStatus()))
                                .educationLevel(convertEducationLevel(request.getEducationLevel()))
                                .dependents(request.getDependents())
                                .build();
                        employmentService.createEmployment(clientId, employment);
                    }
                } catch (Exception e) {
                    log.warn("Failed to update employment for client {}: {}", clientId, e.getMessage());
                }
            }

            // Update portal settings / auth identity (portal access)
            if (request.isAnyFieldPresent("hasPortalAccess", "portalEmail", "emailNotifications")) {
                try {
                    Client client = clientRepository.findById(clientId).orElse(null);
                    if (client == null) return;
                    ClientPortalSettings settings = portalSettingsService.getOrCreate(clientId);
                    if (request.isFieldPresent("hasPortalAccess") && Boolean.FALSE.equals(request.getHasPortalAccess())) {
                        portalSettingsService.disablePortalAccess(clientId);
                        settings = portalSettingsService.getOrCreate(clientId);
                    }
                    if (request.isFieldPresent("emailNotifications")) {
                        settings.setEmailNotifications(request.getEmailNotifications());
                    }
                    portalSettingsService.save(settings);
                    if (request.isFieldPresent("portalEmail") && client.getAuthIdentity() != null) {
                        String normalised = AuthIdentityService.normaliseLoginIdentifier(request.getPortalEmail());
                        AuthIdentity identity = client.getAuthIdentity();
                        if (!normalised.equals(identity.getNormalisedLoginIdentifier())) {
                            identity.setLoginIdentifier(request.getPortalEmail());
                            identity.setNormalisedLoginIdentifier(normalised);
                            authIdentityRepository.save(identity);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to update portal settings for client {}: {}", clientId, e.getMessage());
                }
            }
        } catch (BadRequestException | ResourceNotFoundException e) {
            throw e;
        } catch (com.smart.therapy.flow.common.exception.ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating normalized entities for client {}: {}", clientId, e.getMessage(), e);
            // Don't fail client update if normalized entities fail
        }
    }

    private void validateReferralDate(LocalDate referralDate) {
        if (referralDate != null && referralDate.isAfter(LocalDate.now())) {
            throw new BadRequestException("Referral date must be in the past or present");
        }
    }

    private ReferralType convertReferralType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return ReferralType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid referral type value: {}", value);
            return null;
        }
    }

    // ========== NORMALIZED ENTITY ENDPOINT METHODS ==========
    // These methods support the new granular endpoints for managing normalized
    // entities.
    // The existing createClient and updateClient methods remain intact and continue
    // to
    // handle all fields in one request, creating/updating normalized entities
    // automatically.

    // ========== CONTACTS ==========

    @Transactional(readOnly = true)
    public List<ClientContactResponse> getClientContacts(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        List<ClientContact> contacts = contactService.getContacts(clientId);
        return contacts.stream()
                .map(this::toContactResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClientContactResponse createClientContact(Long clientId, ClientContactRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        ClientContact contact = ClientContact.builder()
                .contactType(request.getContactType())
                .contactValue(request.getContactValue())
                .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false)
                .isVerified(request.getIsVerified() != null ? request.getIsVerified() : false)
                .contactPersonName(request.getContactPersonName())
                .relationship(request.getRelationship())
                .displayOrder(contactService.getContacts(clientId).size() + 1)
                .build();

        ClientContact saved = contactService.createContact(clientId, contact);

        // Track history for contact creation
        trackClientHistory(clientId, ClientEventType.CONTACT_ADDED.getDisplayName(), null,
                saved.getContactType().getDisplayName() + ": " + saved.getContactValue(),
                "Contact added: " + saved.getContactType().getDisplayName(),
                currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier());

        return toContactResponse(saved);
    }

    @Transactional
    public ClientContactResponse updateClientContact(Long clientId, Long contactId, ClientContactRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(contactId, "Contact ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        ClientContact contact = contactService.getContact(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        // Verify contact belongs to client
        if (!contact.getClient().getId().equals(clientId)) {
            throw new BadRequestException("Contact does not belong to this client");
        }

        contact.setContactType(request.getContactType());
        contact.setContactValue(request.getContactValue());
        if (request.getIsPrimary() != null)
            contact.setIsPrimary(request.getIsPrimary());
        if (request.getIsVerified() != null)
            contact.setIsVerified(request.getIsVerified());
        if (request.getContactPersonName() != null)
            contact.setContactPersonName(request.getContactPersonName());
        if (request.getRelationship() != null)
            contact.setRelationship(request.getRelationship());

        ClientContact updated = contactService.updateContact(contactId, contact);

        // Track history for contact update
        trackClientHistory(clientId, ClientEventType.CONTACT_UPDATED.getDisplayName(), null,
                updated.getContactType().getDisplayName() + ": " + updated.getContactValue(),
                "Contact updated: " + updated.getContactType().getDisplayName(),
                currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier());

        return toContactResponse(updated);
    }

    @Transactional
    public void deleteClientContact(Long clientId, Long contactId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(contactId, "Contact ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        ClientContact contact = contactService.getContact(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        // Verify contact belongs to client
        if (!contact.getClient().getId().equals(clientId)) {
            throw new BadRequestException("Contact does not belong to this client");
        }

        contactService.deleteContact(contactId);

        // Track history for contact deletion
        trackClientHistory(clientId, ClientEventType.CONTACT_REMOVED.getDisplayName(),
                contact.getContactType().getDisplayName() + ": " + contact.getContactValue(), null,
                "Contact removed: " + contact.getContactType().getDisplayName(),
                currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier());
    }

    // ========== ADDRESSES ==========

    @Transactional(readOnly = true)
    public List<ClientAddressResponse> getClientAddresses(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        List<ClientAddress> addresses = addressService.getAddresses(clientId);
        return addresses.stream()
                .map(this::toAddressResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClientAddressResponse createClientAddress(Long clientId, ClientAddressRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        ClientAddress address = ClientAddress.builder()
                .addressType(request.getAddressType())
                .streetAddress1(request.getStreetAddress1())
                .streetAddress2(request.getStreetAddress2())
                .city(request.getCity())
                .stateProvince(request.getStateProvince())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false)
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .isCurrent(true)
                .isVerified(false)
                .displayOrder(addressService.getAddresses(clientId).size() + 1)
                .build();

        ClientAddress saved = addressService.createAddress(clientId, address);

        // Track history for address creation
        trackClientHistory(clientId, ClientEventType.ADDRESS_ADDED.getDisplayName(), null,
                saved.getAddressType().getDisplayName() + ": " + saved.getCity(),
                "Address added: " + saved.getAddressType().getDisplayName(),
                currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier());

        return toAddressResponse(saved);
    }

    @Transactional
    public ClientAddressResponse updateClientAddress(Long clientId, Long addressId, ClientAddressRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(addressId, "Address ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        ClientAddress address = addressService.getAddress(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Verify address belongs to client
        if (!address.getClient().getId().equals(clientId)) {
            throw new BadRequestException("Address does not belong to this client");
        }

        address.setAddressType(request.getAddressType());
        address.setStreetAddress1(request.getStreetAddress1());
        address.setStreetAddress2(request.getStreetAddress2());
        address.setCity(request.getCity());
        address.setStateProvince(request.getStateProvince());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        if (request.getIsPrimary() != null)
            address.setIsPrimary(request.getIsPrimary());
        if (request.getValidFrom() != null)
            address.setValidFrom(request.getValidFrom());
        if (request.getValidUntil() != null)
            address.setValidUntil(request.getValidUntil());

        ClientAddress updated = addressService.updateAddress(addressId, address);

        // Track history for address update
        trackClientHistory(clientId, ClientEventType.ADDRESS_UPDATED.getDisplayName(), null,
                updated.getAddressType().getDisplayName() + ": " + updated.getCity(),
                "Address updated: " + updated.getAddressType().getDisplayName(),
                currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier());

        return toAddressResponse(updated);
    }

    @Transactional
    public void deleteClientAddress(Long clientId, Long addressId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(addressId, "Address ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        ClientAddress address = addressService.getAddress(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Verify address belongs to client
        if (!address.getClient().getId().equals(clientId)) {
            throw new BadRequestException("Address does not belong to this client");
        }

        addressService.deleteAddress(addressId);

        // Track history for address deletion
        trackClientHistory(clientId, ClientEventType.ADDRESS_REMOVED.getDisplayName(),
                address.getAddressType().getDisplayName() + ": " + address.getCity(), null,
                "Address removed: " + address.getAddressType().getDisplayName(),
                currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier());
    }

    // ========== INSURANCE ==========

    @Transactional(readOnly = true)
    public ClientInsuranceResponse getClientInsurance(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        ClientInsurance insurance = insuranceService.getInsurance(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance information not found for this client"));
        return toInsuranceResponse(insurance);
    }

    @Transactional
    public ClientInsuranceResponse upsertClientInsurance(Long clientId, ClientInsuranceRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        Optional<ClientInsurance> existing = insuranceService.getInsurance(clientId);

        if (existing.isPresent()) {
            // Update existing
            ClientInsurance insurance = existing.get();
            updateInsuranceFromRequest(insurance, request);
            ClientInsurance updated = insuranceService.updateInsurance(clientId, insurance);

            // Track history for insurance update
            trackClientHistory(clientId, ClientEventType.INSURANCE_UPDATED.getDisplayName(), null,
                    updated.getInsuranceProvider() + " - " + updated.getPolicyNumber(),
                    "Insurance information updated",
                    currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier());

            return toInsuranceResponse(updated);
        } else {
            // Create new
            ClientInsurance insurance = ClientInsurance.builder()
                    .insuranceProvider(requireInsuranceProvider(request.getInsuranceProvider()))
                    .insuranceType(requireInsuranceType(request.getInsuranceType()))
                    .policyNumber(request.getPolicyNumber())
                    .groupNumber(request.getGroupNumber())
                    .subscriberName(request.getSubscriberName())
                    .subscriberRelationship(request.getSubscriberRelationship())
                    .insurancePhone(request.getInsurancePhone())
                    .insuranceEmail(request.getInsuranceEmail())
                    .copayAmount(request.getCopayAmount())
                    .deductible(request.getDeductible())
                    .outOfPocketMax(request.getOutOfPocketMax())
                    .coveragePercentage(request.getCoveragePercentage())
                    .effectiveDate(request.getEffectiveDate())
                    .expiryDate(request.getExpiryDate())
                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                    .isVerified(request.getIsVerified() != null ? request.getIsVerified() : false)
                    .authorizationRequired(
                            request.getAuthorizationRequired() != null ? request.getAuthorizationRequired() : false)
                    .authorizationNumber(request.getAuthorizationNumber())
                    .authorizationExpiresAt(request.getAuthorizationExpiresAt())
                    .mentalHealthCoverage(
                            request.getMentalHealthCoverage() != null ? request.getMentalHealthCoverage() : true)
                    .telehealthCoverage(
                            request.getTelehealthCoverage() != null ? request.getTelehealthCoverage() : false)
                    .sessionsPerYear(request.getSessionsPerYear())
                    .notes(request.getNotes())
                    .build();
            ClientInsurance created = insuranceService.createInsurance(clientId, insurance);

            // Track history for insurance creation
            trackClientHistory(clientId, ClientEventType.INSURANCE_ADDED.getDisplayName(), null,
                    created.getInsuranceProvider() + " - " + created.getPolicyNumber(),
                    "Insurance information added",
                    currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier());

            return toInsuranceResponse(created);
        }
    }

    @Transactional
    public void deleteClientInsurance(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        // Get insurance info before deletion for history
        ClientInsurance insurance = insuranceService.getInsurance(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance information not found"));
        String insuranceInfo = insurance.getInsuranceProvider() + " - " + insurance.getPolicyNumber();

        insuranceService.deleteInsurance(clientId);

        // Track history for insurance deletion
        trackClientHistory(clientId, ClientEventType.INSURANCE_UPDATED.getDisplayName(),
                insuranceInfo, null,
                "Insurance information removed",
                currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier());
    }

    // ========== REFERRAL ==========

    @Transactional(readOnly = true)
    public ClientReferralResponse getClientReferral(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        ClientReferral referral = referralService.getReferral(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Referral information not found for this client"));
        return toReferralResponse(referral);
    }

    @Transactional
    public ClientReferralResponse upsertClientReferral(Long clientId, ClientReferralRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        Optional<ClientReferral> existing = referralService.getReferral(clientId);

        if (existing.isPresent()) {
            // Update existing
            ClientReferral referral = existing.get();
            updateReferralFromRequest(referral, request);
            ClientReferral updated = referralService.updateReferral(clientId, referral);

            // Track history for referral update
            trackClientHistory(clientId, ClientEventType.REFERRAL_UPDATED.getDisplayName(), null,
                    updated.getReferrerName() != null ? updated.getReferrerName() : "Referral updated",
                    "Referral information updated",
                    currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier());

            return toReferralResponse(updated);
        } else {
            // Create new
            ClientReferral referral = ClientReferral.builder()
                    .referralDate(request.getReferralDate())
                    .referralSource(resolveReferralSource(request))
                    .referralType(convertReferralType(request.getReferralType()))
                    .referrerName(request.getReferrerName())
                    .referrerTitle(request.getReferrerTitle())
                    .referrerOrganization(request.getReferrerOrganization())
                    .referrerPhone(request.getReferrerPhone())
                    .referrerEmail(request.getReferrerEmail())
                    .referenceNumber(request.getReferenceNumber())
                    .clientSource(request.getClientSource())
                    .isCourtOrdered(request.getIsCourtOrdered() != null ? request.getIsCourtOrdered() : false)
                    .courtOrderNumber(request.getCourtOrderNumber())
                    .courtJurisdiction(request.getCourtJurisdiction())
                    .requiresReporting(request.getRequiresReporting() != null ? request.getRequiresReporting() : false)
                    .reportingFrequency(request.getReportingFrequency())
                    .reportingRecipient(request.getReportingRecipient())
                    .consentToContactReferrer(
                            request.getConsentToContactReferrer() != null ? request.getConsentToContactReferrer()
                                    : false)
                    .marketingCampaign(request.getMarketingCampaign())
                    .promoCode(request.getPromoCode())
                    .referralNotes(request.getReferralNotes())
                    .intakeSummary(request.getIntakeSummary())
                    .build();
            ClientReferral created = referralService.createReferral(clientId, referral);

            // Track history for referral creation
            trackClientHistory(clientId, ClientEventType.REFERRAL_ADDED.getDisplayName(), null,
                    created.getReferrerName() != null ? created.getReferrerName() : "Referral added",
                    "Referral information added",
                    currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier());

            return toReferralResponse(created);
        }
    }

    @Transactional
    public void deleteClientReferral(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        // Get referral info before deletion for history
        ClientReferral referral = referralService.getReferral(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Referral information not found"));
        String referralInfo = referral.getReferrerName() != null ? referral.getReferrerName() : "Referral";

        referralService.deleteReferral(clientId);

        // Track history for referral deletion
        trackClientHistory(clientId, ClientEventType.REFERRAL_UPDATED.getDisplayName(),
                referralInfo, null,
                "Referral information removed",
                currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier());
    }

    // ========== EMPLOYMENT ==========

    @Transactional(readOnly = true)
    public ClientEmploymentResponse getClientEmployment(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        ClientEmployment employment = employmentService.getEmployment(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Employment information not found for this client"));
        return toEmploymentResponse(employment);
    }

    @Transactional
    public ClientEmploymentResponse upsertClientEmployment(Long clientId, ClientEmploymentRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        Optional<ClientEmployment> existing = employmentService.getEmployment(clientId);

        if (existing.isPresent()) {
            // Update existing
            ClientEmployment employment = existing.get();
            updateEmploymentFromRequest(employment, request);
            ClientEmployment updated = employmentService.updateEmployment(clientId, employment);

            // Track history for employment update
            trackClientHistory(clientId, ClientEventType.EMPLOYMENT_UPDATED.getDisplayName(), null,
                    updated.getEmploymentStatus() != null ? resolveOptionLabel(SystemOptionCategories.EMPLOYMENT_STATUS, updated.getEmploymentStatus())
                            : "Employment updated",
                    "Employment information updated",
                    currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier());

            return toEmploymentResponse(updated);
        } else {
            // Create new
            ClientEmployment employment = ClientEmployment.builder()
                    .employmentStatus(request.getEmploymentStatus())
                    .employerName(request.getEmployerName())
                    .jobTitle(request.getJobTitle())
                    .employmentStartDate(request.getEmploymentStartDate())
                    .employmentEndDate(request.getEmploymentEndDate())
                    .isCurrentlyEmployed(
                            request.getIsCurrentlyEmployed() != null ? request.getIsCurrentlyEmployed() : false)
                    .educationLevel(request.getEducationLevel())
                    .fieldOfStudy(request.getFieldOfStudy())
                    .schoolName(request.getSchoolName())
                    .graduationYear(request.getGraduationYear())
                    .isStudent(request.getIsStudent() != null ? request.getIsStudent() : false)
                    .occupationCategory(request.getOccupationCategory())
                    .workHoursPerWeek(request.getWorkHoursPerWeek())
                    .shiftWork(request.getShiftWork() != null ? request.getShiftWork() : false)
                    .remoteWork(request.getRemoteWork() != null ? request.getRemoteWork() : false)
                    .annualIncome(request.getAnnualIncome())
                    .householdIncome(request.getHouseholdIncome())
                    .dependents(request.getDependents())
                    .householdSize(request.getHouseholdSize())
                    .financialHardship(request.getFinancialHardship() != null ? request.getFinancialHardship() : false)
                    .eligibleForSlidingScale(
                            request.getEligibleForSlidingScale() != null ? request.getEligibleForSlidingScale() : false)
                    .slidingScalePercentage(request.getSlidingScalePercentage())
                    .disabilityStatus(request.getDisabilityStatus())
                    .veteranStatus(request.getVeteranStatus() != null ? request.getVeteranStatus() : false)
                    .militaryBranch(request.getMilitaryBranch())
                    .militaryServiceYears(request.getMilitaryServiceYears())
                    .notes(request.getNotes())
                    .build();
            ClientEmployment created = employmentService.createEmployment(clientId, employment);

            // Track history for employment creation
            trackClientHistory(clientId, ClientEventType.EMPLOYMENT_ADDED.getDisplayName(), null,
                    created.getEmploymentStatus() != null ? resolveOptionLabel(SystemOptionCategories.EMPLOYMENT_STATUS, created.getEmploymentStatus())
                            : "Employment added",
                    "Employment information added",
                    currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier());

            return toEmploymentResponse(created);
        }
    }

    @Transactional
    public void deleteClientEmployment(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateClientAccess(client, requester);

        // Get employment info before deletion for history
        ClientEmployment employment = employmentService.getEmployment(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Employment information not found"));
        String employmentInfo = employment.getEmploymentStatus() != null
                ? resolveOptionLabel(SystemOptionCategories.EMPLOYMENT_STATUS, employment.getEmploymentStatus())
                : "Employment";

        employmentService.deleteEmployment(clientId);

        // Track history for employment deletion
        trackClientHistory(clientId, ClientEventType.EMPLOYMENT_UPDATED.getDisplayName(),
                employmentInfo, null,
                "Employment information removed",
                currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier());
    }

    // ========== DTO CONVERSION METHODS ==========

    private ClientContactResponse toContactResponse(ClientContact contact) {
        return ClientContactResponse.builder()
                .id(contact.getId())
                .clientId(contact.getClient().getId())
                .contactType(contact.getContactType())
                .contactValue(contact.getContactValue())
                .isPrimary(contact.getIsPrimary())
                .isVerified(contact.getIsVerified())
                .contactPersonName(contact.getContactPersonName())
                .relationship(contact.getRelationship())
                .createdAt(contact.getCreatedAt())
                .updatedAt(contact.getUpdatedAt())
                .build();
    }

    private ClientAddressResponse toAddressResponse(ClientAddress address) {
        return ClientAddressResponse.builder()
                .id(address.getId())
                .clientId(address.getClient().getId())
                .addressType(address.getAddressType())
                .streetAddress1(address.getStreetAddress1())
                .streetAddress2(address.getStreetAddress2())
                .city(address.getCity())
                .stateProvince(address.getStateProvince())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .isPrimary(address.getIsPrimary())
                .validFrom(address.getValidFrom())
                .validUntil(address.getValidUntil())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    private ClientInsuranceResponse toInsuranceResponse(ClientInsurance insurance) {
        return ClientInsuranceResponse.builder()
                .id(insurance.getId())
                .clientId(insurance.getClient().getId())
                .insuranceProvider(insurance.getInsuranceProvider())
                .insuranceType(insurance.getInsuranceType())
                .policyNumber(insurance.getPolicyNumber())
                .groupNumber(insurance.getGroupNumber())
                .subscriberName(insurance.getSubscriberName())
                .subscriberRelationship(insurance.getSubscriberRelationship())
                .insurancePhone(insurance.getInsurancePhone())
                .insuranceEmail(insurance.getInsuranceEmail())
                .copayAmount(insurance.getCopayAmount())
                .deductible(insurance.getDeductible())
                .deductibleMet(insurance.getDeductibleMet())
                .outOfPocketMax(insurance.getOutOfPocketMax())
                .coveragePercentage(insurance.getCoveragePercentage())
                .effectiveDate(insurance.getEffectiveDate())
                .expiryDate(insurance.getExpiryDate())
                .isActive(insurance.getIsActive())
                .isVerified(insurance.getIsVerified())
                .authorizationRequired(insurance.getAuthorizationRequired())
                .authorizationNumber(insurance.getAuthorizationNumber())
                .authorizationExpiresAt(insurance.getAuthorizationExpiresAt())
                .mentalHealthCoverage(insurance.getMentalHealthCoverage())
                .telehealthCoverage(insurance.getTelehealthCoverage())
                .sessionsPerYear(insurance.getSessionsPerYear())
                .sessionsUsed(insurance.getSessionsUsed())
                .remainingSessions(insurance.getRemainingSessions())
                .remainingDeductible(insurance.getRemainingDeductible())
                .isDeductibleMet(insurance.isDeductibleMet())
                .isAuthorizationValid(insurance.isAuthorizationValid())
                .notes(insurance.getNotes())
                .createdAt(insurance.getCreatedAt())
                .updatedAt(insurance.getUpdatedAt())
                .build();
    }

    private ClientReferralResponse toReferralResponse(ClientReferral referral) {
        return ClientReferralResponse.builder()
                .id(referral.getId())
                .clientId(referral.getClient().getId())
                .referralDate(referral.getReferralDate())
                .referralSource(referral.getReferralSource())
                .referralType(referral.getReferralType() != null ? referral.getReferralType().name() : null)
                .referrerName(referral.getReferrerName())
                .referrerTitle(referral.getReferrerTitle())
                .referrerOrganization(referral.getReferrerOrganization())
                .referrerPhone(referral.getReferrerPhone())
                .referrerEmail(referral.getReferrerEmail())
                .referenceNumber(referral.getReferenceNumber())
                .clientSource(referral.getClientSource())
                .isCourtOrdered(referral.getIsCourtOrdered())
                .courtOrderNumber(referral.getCourtOrderNumber())
                .courtJurisdiction(referral.getCourtJurisdiction())
                .requiresReporting(referral.getRequiresReporting())
                .reportingFrequency(referral.getReportingFrequency())
                .reportingRecipient(referral.getReportingRecipient())
                .consentToContactReferrer(referral.getConsentToContactReferrer())
                .marketingCampaign(referral.getMarketingCampaign())
                .promoCode(referral.getPromoCode())
                .referralNotes(referral.getReferralNotes())
                .intakeSummary(referral.getIntakeSummary())
                .referrerDisplayName(referral.getReferrerDisplayName())
                .referralTypeDisplay(referral.getReferralTypeDisplay())
                .canContactReferrer(referral.canContactReferrer())
                .createdAt(referral.getCreatedAt())
                .updatedAt(referral.getUpdatedAt())
                .build();
    }

    private ClientEmploymentResponse toEmploymentResponse(ClientEmployment employment) {
        return ClientEmploymentResponse.builder()
                .id(employment.getId())
                .clientId(employment.getClient().getId())
                .employmentStatus(employment.getEmploymentStatus())
                .employerName(employment.getEmployerName())
                .jobTitle(employment.getJobTitle())
                .employmentStartDate(employment.getEmploymentStartDate())
                .employmentEndDate(employment.getEmploymentEndDate())
                .isCurrentlyEmployed(employment.getIsCurrentlyEmployed())
                .educationLevel(employment.getEducationLevel())
                .fieldOfStudy(employment.getFieldOfStudy())
                .schoolName(employment.getSchoolName())
                .graduationYear(employment.getGraduationYear())
                .isStudent(employment.getIsStudent())
                .occupationCategory(employment.getOccupationCategory())
                .workHoursPerWeek(employment.getWorkHoursPerWeek())
                .shiftWork(employment.getShiftWork())
                .remoteWork(employment.getRemoteWork())
                .annualIncome(employment.getAnnualIncome())
                .householdIncome(employment.getHouseholdIncome())
                .dependents(employment.getDependents())
                .householdSize(employment.getHouseholdSize())
                .financialHardship(employment.getFinancialHardship())
                .eligibleForSlidingScale(employment.getEligibleForSlidingScale())
                .slidingScalePercentage(employment.getSlidingScalePercentage())
                .disabilityStatus(employment.getDisabilityStatus())
                .veteranStatus(employment.getVeteranStatus())
                .militaryBranch(employment.getMilitaryBranch())
                .militaryServiceYears(employment.getMilitaryServiceYears())
                .yearsOfExperience(employment.getYearsOfExperience())
                .employmentDisplayStatus(employment.getEmploymentDisplayStatus())
                .isEmployed(employment.isEmployed())
                .isRetired(employment.isRetired())
                .isUnemployed(employment.isUnemployed())
                .hasFinancialHardship(employment.hasFinancialHardship())
                .isVeteran(employment.isVeteran())
                .hasDisability(employment.hasDisability())
                .notes(employment.getNotes())
                .createdAt(employment.getCreatedAt())
                .updatedAt(employment.getUpdatedAt())
                .build();
    }

    // ========== UPDATE HELPER METHODS ==========

    private void createInsuranceFromCreateRequest(Long clientId, CreateClientRequest request) {
        if (!StringUtils.hasText(request.getInsuranceProvider()) && !StringUtils.hasText(request.getPolicyNumber())
                && request.getGroupNumber() == null && request.getInsurancePhone() == null
                && request.getCopayAmount() == null && request.getDeductible() == null) {
            return;
        }

        if (!StringUtils.hasText(request.getInsuranceProvider()) || !StringUtils.hasText(request.getPolicyNumber())) {
            throw new BadRequestException(
                    "Insurance provider and policy number are required when adding insurance for a client.");
        }

        ClientInsurance insurance = ClientInsurance.builder()
                .insuranceProvider(requireInsuranceProvider(request.getInsuranceProvider()))
                .insuranceType(requireInsuranceType(request.getInsuranceType()))
                .policyNumber(request.getPolicyNumber())
                .groupNumber(request.getGroupNumber())
                .insurancePhone(request.getInsurancePhone())
                .copayAmount(request.getCopayAmount())
                .deductible(request.getDeductible())
                .isActive(true)
                .isVerified(false)
                .build();
        insuranceService.createInsurance(clientId, insurance);
    }

    private void syncInsuranceFromUpdateRequest(Long clientId, UpdateClientRequest request) {
        if (!request.isAnyFieldPresent("insuranceProvider", "policyNumber", "groupNumber", "insurancePhone",
                "insuranceType", "copayAmount", "deductible")) {
            return;
        }

        Optional<ClientInsurance> existingInsurance = insuranceService.getInsurance(clientId);
        if (existingInsurance.isPresent()) {
            ClientInsurance insurance = existingInsurance.get();
            if (request.isFieldPresent("insuranceProvider")) {
                insurance.setInsuranceProvider(StringUtils.hasText(request.getInsuranceProvider())
                        ? requireInsuranceProvider(request.getInsuranceProvider())
                        : null);
            }
            if (request.isFieldPresent("insuranceType")) {
                insurance.setInsuranceType(StringUtils.hasText(request.getInsuranceType())
                        ? requireInsuranceType(request.getInsuranceType())
                        : null);
            }
            if (request.isFieldPresent("policyNumber")) {
                insurance.setPolicyNumber(request.getPolicyNumber());
            }
            if (request.isFieldPresent("groupNumber")) {
                insurance.setGroupNumber(request.getGroupNumber());
            }
            if (request.isFieldPresent("insurancePhone")) {
                insurance.setInsurancePhone(request.getInsurancePhone());
            }
            if (request.isFieldPresent("copayAmount")) {
                insurance.setCopayAmount(request.getCopayAmount());
            }
            if (request.isFieldPresent("deductible")) {
                insurance.setDeductible(request.getDeductible());
            }
            insuranceService.updateInsurance(clientId, insurance);
            return;
        }

        if (!StringUtils.hasText(request.getInsuranceProvider()) || !StringUtils.hasText(request.getPolicyNumber())) {
            throw new BadRequestException(
                    "Insurance provider and policy number are required when adding insurance for a client.");
        }

        ClientInsurance insurance = ClientInsurance.builder()
                .insuranceProvider(requireInsuranceProvider(request.getInsuranceProvider()))
                .insuranceType(requireInsuranceType(request.getInsuranceType()))
                .policyNumber(request.getPolicyNumber())
                .groupNumber(request.getGroupNumber())
                .insurancePhone(request.getInsurancePhone())
                .copayAmount(request.getCopayAmount())
                .deductible(request.getDeductible())
                .isActive(true)
                .isVerified(false)
                .build();
        insuranceService.createInsurance(clientId, insurance);
    }

    private void updateInsuranceFromRequest(ClientInsurance insurance, ClientInsuranceRequest request) {
        if (request.getInsuranceProvider() != null) {
            insurance.setInsuranceProvider(requireInsuranceProvider(request.getInsuranceProvider()));
        }
        if (request.getInsuranceType() != null) {
            insurance.setInsuranceType(StringUtils.hasText(request.getInsuranceType())
                    ? requireInsuranceType(request.getInsuranceType())
                    : null);
        }
        if (request.getPolicyNumber() != null)
            insurance.setPolicyNumber(request.getPolicyNumber());
        if (request.getGroupNumber() != null)
            insurance.setGroupNumber(request.getGroupNumber());
        if (request.getSubscriberName() != null)
            insurance.setSubscriberName(request.getSubscriberName());
        if (request.getSubscriberRelationship() != null)
            insurance.setSubscriberRelationship(request.getSubscriberRelationship());
        if (request.getInsurancePhone() != null)
            insurance.setInsurancePhone(request.getInsurancePhone());
        if (request.getInsuranceEmail() != null)
            insurance.setInsuranceEmail(request.getInsuranceEmail());
        if (request.getCopayAmount() != null)
            insurance.setCopayAmount(request.getCopayAmount());
        if (request.getDeductible() != null)
            insurance.setDeductible(request.getDeductible());
        if (request.getOutOfPocketMax() != null)
            insurance.setOutOfPocketMax(request.getOutOfPocketMax());
        if (request.getCoveragePercentage() != null)
            insurance.setCoveragePercentage(request.getCoveragePercentage());
        if (request.getEffectiveDate() != null)
            insurance.setEffectiveDate(request.getEffectiveDate());
        if (request.getExpiryDate() != null)
            insurance.setExpiryDate(request.getExpiryDate());
        if (request.getIsActive() != null)
            insurance.setIsActive(request.getIsActive());
        if (request.getIsVerified() != null)
            insurance.setIsVerified(request.getIsVerified());
        if (request.getAuthorizationRequired() != null)
            insurance.setAuthorizationRequired(request.getAuthorizationRequired());
        if (request.getAuthorizationNumber() != null)
            insurance.setAuthorizationNumber(request.getAuthorizationNumber());
        if (request.getAuthorizationExpiresAt() != null)
            insurance.setAuthorizationExpiresAt(request.getAuthorizationExpiresAt());
        if (request.getMentalHealthCoverage() != null)
            insurance.setMentalHealthCoverage(request.getMentalHealthCoverage());
        if (request.getTelehealthCoverage() != null)
            insurance.setTelehealthCoverage(request.getTelehealthCoverage());
        if (request.getSessionsPerYear() != null)
            insurance.setSessionsPerYear(request.getSessionsPerYear());
        if (request.getNotes() != null)
            insurance.setNotes(request.getNotes());
    }

    private void updateReferralFromRequest(ClientReferral referral, ClientReferralRequest request) {
        if (request.getReferralDate() != null)
            referral.setReferralDate(request.getReferralDate());
        if (request.getReferralSource() != null || request.getClientSource() != null)
            referral.setReferralSource(resolveReferralSource(request));
        if (request.getReferralType() != null)
            referral.setReferralType(convertReferralType(request.getReferralType()));
        if (request.getReferrerName() != null)
            referral.setReferrerName(request.getReferrerName());
        if (request.getReferrerTitle() != null)
            referral.setReferrerTitle(request.getReferrerTitle());
        if (request.getReferrerOrganization() != null)
            referral.setReferrerOrganization(request.getReferrerOrganization());
        if (request.getReferrerPhone() != null)
            referral.setReferrerPhone(request.getReferrerPhone());
        if (request.getReferrerEmail() != null)
            referral.setReferrerEmail(request.getReferrerEmail());
        if (request.getReferenceNumber() != null)
            referral.setReferenceNumber(request.getReferenceNumber());
        if (request.getClientSource() != null)
            referral.setClientSource(request.getClientSource());
        if (request.getIsCourtOrdered() != null)
            referral.setIsCourtOrdered(request.getIsCourtOrdered());
        if (request.getCourtOrderNumber() != null)
            referral.setCourtOrderNumber(request.getCourtOrderNumber());
        if (request.getCourtJurisdiction() != null)
            referral.setCourtJurisdiction(request.getCourtJurisdiction());
        if (request.getRequiresReporting() != null)
            referral.setRequiresReporting(request.getRequiresReporting());
        if (request.getReportingFrequency() != null)
            referral.setReportingFrequency(request.getReportingFrequency());
        if (request.getReportingRecipient() != null)
            referral.setReportingRecipient(request.getReportingRecipient());
        if (request.getConsentToContactReferrer() != null)
            referral.setConsentToContactReferrer(request.getConsentToContactReferrer());
        if (request.getMarketingCampaign() != null)
            referral.setMarketingCampaign(request.getMarketingCampaign());
        if (request.getPromoCode() != null)
            referral.setPromoCode(request.getPromoCode());
        if (request.getReferralNotes() != null)
            referral.setReferralNotes(request.getReferralNotes());
        if (request.getIntakeSummary() != null)
            referral.setIntakeSummary(request.getIntakeSummary());
    }

    private String resolveReferralSource(ClientReferralRequest request) {
        if (request == null) {
            return null;
        }
        if (StringUtils.hasText(request.getReferralSource())) {
            return convertReferralSource(request.getReferralSource());
        }
        if (StringUtils.hasText(request.getClientSource())) {
            return convertReferralSource(request.getClientSource());
        }
        return null;
    }

    private void updateEmploymentFromRequest(ClientEmployment employment, ClientEmploymentRequest request) {
        if (request.getEmploymentStatus() != null)
            employment.setEmploymentStatus(request.getEmploymentStatus());
        if (request.getEmployerName() != null)
            employment.setEmployerName(request.getEmployerName());
        if (request.getJobTitle() != null)
            employment.setJobTitle(request.getJobTitle());
        if (request.getEmploymentStartDate() != null)
            employment.setEmploymentStartDate(request.getEmploymentStartDate());
        if (request.getEmploymentEndDate() != null)
            employment.setEmploymentEndDate(request.getEmploymentEndDate());
        if (request.getIsCurrentlyEmployed() != null)
            employment.setIsCurrentlyEmployed(request.getIsCurrentlyEmployed());
        if (request.getEducationLevel() != null)
            employment.setEducationLevel(request.getEducationLevel());
        if (request.getFieldOfStudy() != null)
            employment.setFieldOfStudy(request.getFieldOfStudy());
        if (request.getSchoolName() != null)
            employment.setSchoolName(request.getSchoolName());
        if (request.getGraduationYear() != null)
            employment.setGraduationYear(request.getGraduationYear());
        if (request.getIsStudent() != null)
            employment.setIsStudent(request.getIsStudent());
        if (request.getOccupationCategory() != null)
            employment.setOccupationCategory(request.getOccupationCategory());
        if (request.getWorkHoursPerWeek() != null)
            employment.setWorkHoursPerWeek(request.getWorkHoursPerWeek());
        if (request.getShiftWork() != null)
            employment.setShiftWork(request.getShiftWork());
        if (request.getRemoteWork() != null)
            employment.setRemoteWork(request.getRemoteWork());
        if (request.getAnnualIncome() != null)
            employment.setAnnualIncome(request.getAnnualIncome());
        if (request.getHouseholdIncome() != null)
            employment.setHouseholdIncome(request.getHouseholdIncome());
        if (request.getDependents() != null)
            employment.setDependents(request.getDependents());
        if (request.getHouseholdSize() != null)
            employment.setHouseholdSize(request.getHouseholdSize());
        if (request.getFinancialHardship() != null)
            employment.setFinancialHardship(request.getFinancialHardship());
        if (request.getEligibleForSlidingScale() != null)
            employment.setEligibleForSlidingScale(request.getEligibleForSlidingScale());
        if (request.getSlidingScalePercentage() != null)
            employment.setSlidingScalePercentage(request.getSlidingScalePercentage());
        if (request.getDisabilityStatus() != null)
            employment.setDisabilityStatus(request.getDisabilityStatus());
        if (request.getVeteranStatus() != null)
            employment.setVeteranStatus(request.getVeteranStatus());
        if (request.getMilitaryBranch() != null)
            employment.setMilitaryBranch(request.getMilitaryBranch());
        if (request.getMilitaryServiceYears() != null)
            employment.setMilitaryServiceYears(request.getMilitaryServiceYears());
        if (request.getNotes() != null)
            employment.setNotes(request.getNotes());
    }
}
