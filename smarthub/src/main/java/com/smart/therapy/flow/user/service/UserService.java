package com.smart.therapy.flow.user.service;

import com.smart.therapy.flow.auth.entity.Role;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.entity.UserActivityLog;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthIdentityRole;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.repository.RoleRepository;
import com.smart.therapy.flow.auth.repository.UserActivityLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.auth.service.AuthIdentityService;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.exception.UnauthorizedException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.AuthIdentityDetailsService;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.entity.UserOrganisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import com.smart.therapy.flow.integration.zoom.ZoomOauthUrls;
import com.smart.therapy.flow.subscription.feature.CoreFeature;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.user.dto.*;
import com.smart.therapy.flow.user.entity.SupervisorAssignment;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.repository.SupervisorAssignmentRepository;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestClientException;
import java.util.Objects;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.Base64;
import java.util.stream.Collectors;

import com.smart.therapy.flow.user.repository.*;
import com.smart.therapy.flow.user.entity.*;
import com.smart.therapy.flow.user.event.UserCreatedEvent;
import com.smart.therapy.flow.user.event.UserUpdatedEvent;
import com.smart.therapy.flow.user.event.UserProfileUpdatedEvent;
import com.smart.therapy.flow.user.validation.UserProfileRequestValidator;
import com.smart.therapy.flow.common.service.AuditService;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.notification.service.NotificationEventCatalog;
import com.smart.therapy.flow.notification.service.NotificationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private static final String RESOURCE_TYPE_USER = "user";
    private static final DateTimeFormatter WORKING_HOURS_12H_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter WORKING_HOURS_24H_FORMAT = DateTimeFormatter.ofPattern("H:mm", Locale.ENGLISH);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserIdempotencyKeyRepository idempotencyKeyRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;
    private final TimezoneService timezoneService;
    private final UserProfileRequestValidator profileRequestValidator;
    private final UserProfileRepository userProfileRepository;
    private final SupervisorAssignmentRepository supervisorAssignmentRepository;
    private final AuditLogService auditLogService;
    private final UserActivityLogRepository userActivityLogRepository;
    private final UserIntegrationRepository userIntegrationRepository;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;
    private final AuthIdentityDetailsService authIdentityDetailsService;
    private final AuthIdentityService authIdentityService;
    private final AuthIdentityRoleRepository authIdentityRoleRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final OrganisationRepository organisationRepository;
    private final UserOrganisationRepository userOrganisationRepository;

    // User Profile Entity Repositories
    private final UserProfileLanguageRepository languageRepository;
    private final UserProfileCertificationRepository certificationRepository;
    private final UserProfileSpecializationRepository specializationRepository;
    private final UserProfileTreatmentApproachRepository treatmentApproachRepository;
    private final UserProfileAgeGroupRepository ageGroupRepository;
    private final UserProfileEducationRepository educationRepository;
    private final UserProfileContinuingEducationRepository continuingEducationRepository;
    private final UserProfileMembershipRepository membershipRepository;
    private final UserProfileAwardRepository awardRepository;
    private final UserProfilePublicationRepository publicationRepository;
    private final UserProfileReferenceRepository referenceRepository;
    private final UserProfilePreviousPositionRepository previousPositionRepository;
    private final UserProfilePhysicalRoomRepository physicalRoomRepository;
    private final UserProfileWorkingHoursRepository workingHoursRepository;
    private final UserContactRepository userContactRepository;
    private final com.smart.therapy.flow.session.repository.RoomRepository roomRepository;
    private final com.smart.therapy.flow.billing.repository.ServiceRepository serviceRepository;
    private final com.smart.therapy.flow.document.service.StorageService storageService;
    private final com.smart.therapy.flow.common.security.CaseloadScopeService caseloadScopeService;

    public static final String CONSULTATION_SERVICE_CODE = "CONSULTATION";

    private static final long PROFILE_PICTURE_MAX_BYTES = 800 * 1024L;
    private static final java.util.Set<String> PROFILE_PICTURE_ALLOWED_TYPES = java.util.Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif"
    );
    private final com.smart.therapy.flow.common.security.TokenBlacklistService tokenBlacklistService;
    private final RestTemplate restTemplate;

    @PersistenceContext
    private EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;
    
    @org.springframework.beans.factory.annotation.Value("${zoom.oauth.base-url:https://zoom.us}")
    private String zoomOauthBaseUrl;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Transactional(readOnly = true)
    public PaginatedResponse<UserResponse> getUsers(
            int page,
            int pageSize,
            String search,
            String role,
            Boolean active,
            AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<User> specification = buildUserSpecification(search, role, active, requester);
        Page<User> results = userRepository.findAll(specification, pageable);

        List<UserResponse> payload = results.getContent().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());

        return PaginatedResponse.of(payload, results.getTotalElements(), page, pageSize);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey(#userId)")
    public UserResponse getUser(Long userId, AuthPrincipal requester) {
        Objects.requireNonNull(userId, "User id is required");
        Objects.requireNonNull(requester, "Requester is required");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateUserVisibility(user, requester);
        return toUserResponse(user);
    }

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse createUser(CreateUserRequest request, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(request, "Request payload is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertCanCreateUser(requester, "Only administrators can create users");

        // STEP 0 — Idempotency Check (database-backed, race-condition safe)
        if (StringUtils.hasText(request.getIdempotencyKey())) {
            Optional<UserIdempotencyKey> existing = idempotencyKeyRepository.findByKey(request.getIdempotencyKey());
            
            if (existing.isPresent() && existing.get().getUserId() != null) {
                // User already created with this key - return existing
                User user = userRepository.findById(existing.get().getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found for idempotency key"));
                log.info("Idempotency: Returning existing user {} for key {}", 
                        user.getId(), request.getIdempotencyKey());
                return toUserResponse(user);
            }
            
            // Reserve the key (even if userId is null - prevents race conditions)
            if (existing.isEmpty()) {
                UserIdempotencyKey idempotencyKey = Objects.requireNonNull(UserIdempotencyKey.builder()
                        .key(request.getIdempotencyKey())
                        .userId(null)
                        .build());
                idempotencyKeyRepository.save(idempotencyKey);
            }
        }

        // STEP 1 — Validation (tenant-scoped uniqueness via public auth_identities + tenant users)
        String normalisedUsername = AuthIdentityService.normaliseLoginIdentifier(request.getUsername());
        String normalisedEmail = AuthIdentityService.normaliseLoginIdentifier(request.getEmail());
        Long orgId = TenantContext.getOrganisationId();
        Organisation previewOrg = orgId != null ? organisationRepository.findById(orgId).orElse(null) : null;
        authIdentityService.assertUsernameAvailable(previewOrg, normalisedUsername, null);
        authIdentityService.assertEmailAvailable(previewOrg, normalisedEmail, null);
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Email already in use");
        }

        Set<Role> roles = resolveRoles(request.getRoles());

        // Plan limits are enforced against active role counts, not metered usage.
        if (orgId != null) {
            boolean active = request.getActive() == null || request.getActive();
            enforceRoleLimitsForTransition(orgId, Set.of(), false, roles, active);
        }

        // STEP 2 — Create AuthIdentity then User
        User requesterUser = currentUserService.requireCurrentUser(requester);
        AuthIdentity identity = authIdentityService.createStaffIdentity(
                request.getUsername().trim(),
                request.getEmail().trim(),
                request.getPassword(),
                requesterUser.getId());
        if (identity == null) {
            throw new BadRequestException("Username already in use");
        }

        User user = User.builder()
                .authIdentity(identity)
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim().toLowerCase(Locale.ROOT))
                .phone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null)
                .isActive(request.getActive() == null || request.getActive())
                .build();

        User saved = Objects.requireNonNull(userRepository.save(user), "Persisted user must not be null");

        // Assign roles (AuthIdentityRole per role for this org; schema-per-tenant: org from TenantContext)
        Organisation org = TenantContext.getOrganisationId() != null
                ? organisationRepository.findById(TenantContext.getOrganisationId()).orElse(null)
                : null;

        if (org != null && identity.getId() != null
                && !userOrganisationRepository.existsByAuth_IdAndOrganisation_Id(identity.getId(), org.getId())) {
            userOrganisationRepository.save(UserOrganisation.builder()
                    .auth(identity)
                    .organisation(org)
                    .createdAt(Instant.now())
                    .build());
        }

        for (Role role : roles) {
            if (org == null) continue;
            AuthIdentityRole air = AuthIdentityRole.builder()
                    .authIdentity(identity)
                    .role(role)
                    .organisation(org)
                    .build();
            authIdentityRoleRepository.save(air);
        }
        Long savedId = requireUserId(saved);

        // Link idempotency key to user
        if (StringUtils.hasText(request.getIdempotencyKey())) {
            idempotencyKeyRepository.updateUserId(request.getIdempotencyKey(), savedId);
        }

        // Publish event for async post-commit processing (password generation, email, audit)
        eventPublisher.publishEvent(new UserCreatedEvent(
                savedId,
                currentUserService.requireCurrentUser(requester).getId(),
                ipAddress,
                TenantContext.getOrganisationId(),
                TenantContext.getSchemaName()
        ));

        if (notificationService != null) {
            try {
                notificationService.processEventInNewTransaction(
                        NotificationEventCatalog.ORGANIZATION_USER_CREATED,
                        buildOrganizationUserEventData(saved, requesterUser));
            } catch (Exception e) {
                log.error("Failed to trigger organization_user_created notification", e);
            }
        }

        // Activity logging (synchronous, lightweight)
        recordActivityEvent(currentUserService.requireCurrentUser(requester).getId(), "user_created", savedId, ipAddress);

        return toUserResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "users", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey(#userId)")
    public UserResponse updateUser(Long userId, UpdateUserRequest request, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(userId, "User id is required");
        Objects.requireNonNull(request, "Request payload is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertCanUpdateUser(requester, "Insufficient permissions to update users");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long requesterUserId = currentUserService.requireCurrentUser(requester).getId();
        if (!hasRole(requester, "ADMIN") && !hasRole(requester, "SUPER_ADMIN")
                && !Objects.equals(requesterUserId, userId)) {
            if (hasRole(requester, "SUPERVISOR")) {
                validateSupervisorOwnership(user, requester);
            } else if (!permissionChecker.hasPermission(requester, "USER_MANAGE")) {
                if (!hasFixedTenantPortalRole(requester)
                        && permissionChecker.hasPermission(requester, "USER_EDIT")) {
                    throw new ForbiddenException("You can only update your own user record");
                }
                throw new ForbiddenException("Insufficient permissions to update this user");
            }
        }

        // Capture before state for audit logging
        String beforeState = auditService.serializeUserState(user);

        if (user.getAuthIdentity() != null && StringUtils.hasText(request.getUsername())
                && !request.getUsername().trim().equalsIgnoreCase(
                        user.getAuthIdentity().getUsername() != null
                                ? user.getAuthIdentity().getUsername()
                                : user.getAuthIdentity().getLoginIdentifier())) {
            authIdentityService.updateUsername(user.getAuthIdentity(), request.getUsername());
        }

        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName().trim());
        }

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already in use");
            }
            String newEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
            user.setEmail(newEmail);
            if (user.getAuthIdentity() != null) {
                authIdentityService.updateEmail(user.getAuthIdentity(), newEmail);
            }
        }

        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone().trim());
        }

        boolean currentActive = Boolean.TRUE.equals(user.getIsActive());
        boolean targetActive = request.getActive() != null ? request.getActive() : currentActive;
        if (request.getActive() != null) {
            user.setIsActive(request.getActive());
            if (user.getAuthIdentity() != null) {
                user.getAuthIdentity().setIsActive(request.getActive());
                authIdentityRepository.save(user.getAuthIdentity());
            }
        }

        if (request.getRoles() != null) {
            assertCanChangeUserRoles(requester, "Only administrators can change roles");
            AuthIdentity authIdentity = user.getAuthIdentity();
            Long authId = authIdentity != null ? authIdentity.getId() : null;
            Long orgId = TenantContext.getOrganisationId();
            Organisation org = orgId != null ? organisationRepository.findById(orgId).orElse(null) : null;
            if (authId != null && org != null) {
                List<AuthIdentityRole> existing = authIdentityRoleRepository
                        .findByAuthIdAndOrganisationIdWithRolesAndPermissions(authId, orgId);
                Set<Role> existingRoles = existing.stream()
                        .map(AuthIdentityRole::getRole)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                Set<Role> newRoles = resolveRoles(request.getRoles());
                assertNoSupervisorAssignmentsBeforeRemovingSupervisorRole(user, existingRoles, newRoles);
                enforceRoleLimitsForTransition(orgId, existingRoles, currentActive, newRoles, targetActive);

                // Sync in-memory collection; otherwise cascade on save() can re-insert deleted roles.
                if (authIdentity.getRoles() != null) {
                    authIdentity.getRoles().removeIf(air ->
                            air.getOrganisation() != null && Objects.equals(air.getOrganisation().getId(), orgId));
                }
                authIdentityRoleRepository.deleteAll(existing);
                authIdentityRoleRepository.flush();

                for (Role role : newRoles) {
                    if (role == null || role.getId() == null) {
                        continue;
                    }
                    AuthIdentityRole air = AuthIdentityRole.builder()
                            .authIdentity(authIdentity)
                            .role(role)
                            .organisation(org)
                            .build();
                    if (authIdentity.getRoles() != null) {
                        authIdentity.getRoles().add(air);
                    }
                    authIdentityRoleRepository.save(air);
                }
            }
        }

        User updated = Objects.requireNonNull(userRepository.save(user), "Persisted user must not be null");
        Long updatedId = requireUserId(updated);

        // Capture after state
        String afterState = auditService.serializeUserState(updated);

        // Determine changed fields
        List<String> changedFields = determineChangedFields(beforeState, afterState);

        // Publish event for async post-commit audit logging
        eventPublisher.publishEvent(new UserUpdatedEvent(
            updatedId,
            currentUserService.requireCurrentUser(requester).getId(),
            ipAddress,
            beforeState,
            afterState,
            changedFields,
            TenantContext.getOrganisationId(),
            TenantContext.getSchemaName()
        ));

        // Activity logging (synchronous, lightweight)
        recordActivityEvent(currentUserService.requireCurrentUser(requester).getId(), "user_updated", updatedId, ipAddress);

        if (notificationService != null) {
            try {
                User actor = userRepository.findById(currentUserService.requireCurrentUser(requester).getId()).orElse(null);
                notificationService.processEventInNewTransaction(
                        NotificationEventCatalog.ORGANIZATION_USER_UPDATED,
                        buildOrganizationUserEventData(updated, actor));
            } catch (Exception e) {
                log.error("Failed to trigger organization_user_updated notification", e);
            }
        }

        return toUserResponse(updated);
    }

    /**
     * Determine which fields changed between before and after states.
     */
    private List<String> determineChangedFields(String beforeState, String afterState) {
        if (beforeState == null || afterState == null) {
            return Collections.emptyList();
        }
        try {
            // Simple JSON parsing to find changed fields
            List<String> changed = new ArrayList<>();
            
            // Extract field values from JSON strings
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"([^\"]+)\":([^,}]+)");
            java.util.regex.Matcher beforeMatcher = pattern.matcher(beforeState);
            java.util.regex.Matcher afterMatcher = pattern.matcher(afterState);
            
            Map<String, String> beforeFields = new HashMap<>();
            Map<String, String> afterFields = new HashMap<>();
            
            while (beforeMatcher.find()) {
                beforeFields.put(beforeMatcher.group(1), beforeMatcher.group(2).trim());
            }
            while (afterMatcher.find()) {
                afterFields.put(afterMatcher.group(1), afterMatcher.group(2).trim());
            }
            
            for (String key : afterFields.keySet()) {
                if (!Objects.equals(beforeFields.get(key), afterFields.get(key))) {
                    changed.add(key);
                }
            }
            
            return changed;
        } catch (Exception e) {
            log.error("Failed to determine changed fields", e);
            return Collections.emptyList();
        }
    }

    @Transactional
    @CacheEvict(value = "users", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey(#userId)")
    public void deleteUser(Long userId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(userId, "User id is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertCanDeleteUser(requester, "Only administrators can delete users");

        if (Objects.equals(currentUserService.requireCurrentUser(requester).getId(), userId)) {
            throw new BadRequestException("You cannot delete your own account");
        }

        // Find user including deleted (to check if already deleted)
        User user = userRepository.findByIdIncludingDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if already deleted
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new BadRequestException("User is already deleted");
        }

        // Soft delete the user (HIPAA/GDPR compliance)
        user.setIsDeleted(true);
        user.setDeletedAt(Instant.now());
        user.setIsActive(false);
        if (user.getAuthIdentity() != null) {
            user.getAuthIdentity().setIsActive(false);
        }
        userRepository.save(user);
        userRepository.flush();

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "user_deleted", userId, ipAddress, true);
        recordActivityEvent(currentUserService.requireCurrentUser(requester).getId(), "user_deleted", userId, ipAddress);

        if (notificationService != null) {
            try {
                User actor = userRepository.findById(currentUserService.requireCurrentUser(requester).getId()).orElse(null);
                notificationService.processEventInNewTransaction(
                        NotificationEventCatalog.ORGANIZATION_USER_DELETED,
                        buildOrganizationUserEventData(user, actor));
            } catch (Exception e) {
                log.error("Failed to trigger organization_user_deleted notification", e);
            }
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(AuthPrincipal principal) {
        Objects.requireNonNull(principal, "Principal is required");
        User user = userRepository.findById(currentUserService.requireCurrentUser(principal).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateCurrentUser(UpdateUserRequest request, AuthPrincipal principal, String ipAddress) {
        Objects.requireNonNull(request, "Request payload is required");
        Objects.requireNonNull(principal, "Principal is required");
        User user = userRepository.findById(currentUserService.requireCurrentUser(principal).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName().trim());
        }

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already in use");
            }
            user.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        }

        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone().trim());
        }

        User updated = Objects.requireNonNull(userRepository.save(user), "Persisted user must not be null");
        Long updatedId = requireUserId(updated);
        recordAuditEvent(currentUserService.requireCurrentUser(principal).getId(), "user_profile_updated", updatedId, ipAddress, false);
        recordActivityEvent(currentUserService.requireCurrentUser(principal).getId(), "user_profile_updated", updatedId, ipAddress);
        return toUserResponse(updated);
    }

    public Map<String, Object> updateCurrentUserProfilePicture(MultipartFile file, AuthPrincipal principal, String ipAddress) {
        Objects.requireNonNull(file, "Profile picture file is required");
        Objects.requireNonNull(principal, "Principal is required");

        if (file.isEmpty()) {
            throw new BadRequestException("Profile picture file is empty");
        }
        if (file.getSize() > PROFILE_PICTURE_MAX_BYTES) {
            throw new BadRequestException("Profile picture exceeds max size of 800 KB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !PROFILE_PICTURE_ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Unsupported profile picture type. Allowed: JPG, PNG, GIF");
        }

        User user = userRepository.findById(currentUserService.requireCurrentUser(principal).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String extension = resolveImageExtension(file.getOriginalFilename(), contentType);
        String fileName = "profile_" + user.getId() + "_" + System.currentTimeMillis() + extension;
        String storagePath;
        try {
            storagePath = storageService.uploadFile(file, "user-" + user.getId(), fileName);
        } catch (Exception e) {
            throw new BadRequestException("Failed to upload profile picture: " + e.getMessage());
        }

        String fileUrl = storageService.getFileUrl(storagePath);
        user.setProfilePicture(fileUrl != null ? fileUrl : storagePath);

        User updated = Objects.requireNonNull(userRepository.save(user), "Persisted user must not be null");
        Long updatedId = requireUserId(updated);
        recordAuditEvent(currentUserService.requireCurrentUser(principal).getId(), "user_profile_picture_updated", updatedId, ipAddress, false);
        recordActivityEvent(currentUserService.requireCurrentUser(principal).getId(), "user_profile_picture_updated", updatedId, ipAddress);

        return Map.of(
                "profilePicture", updated.getProfilePicture(),
                "userId", updatedId
        );
    }

    @Transactional
    // The cached UserResponse embeds this profile (timezone, hours, rooms), so saving
    // the profile must drop it, or schedulers keep reading the pre-save values.
    @CacheEvict(value = "users", allEntries = true)
    public UserProfileResponse upsertProfile(UserProfileRequest request, AuthPrincipal principal, String ipAddress) {
        Objects.requireNonNull(request, "Request payload is required");
        Objects.requireNonNull(principal, "Principal is required");
        
        User user = userRepository.findById(currentUserService.requireCurrentUser(principal).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> UserProfile.builder().user(user).build());

        boolean isUpdate = profile.getId() != null;
        
        // Capture before state for updates
        String beforeState = isUpdate ? auditService.serializeUserProfileState(profile) : null;

        // Validate request
        if (isUpdate) {
            profileRequestValidator.validateUpdateRequest(request);
        } else {
            profileRequestValidator.validateCreateRequest(request);
        }

        // ========================================
        // TAB 1: Basic Info - Update User entity
        // ========================================
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            // Check email uniqueness if changed
            if (!request.getEmail().equalsIgnoreCase(user.getEmail())) {
                if (userRepository.existsByEmail(request.getEmail())) {
                    throw new BadRequestException("Email already in use");
                }
                user.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
            }
        }

        applyProfileRequestToEntity(profile, request);

        // Save User entity if changed
        if (request.getFullName() != null || request.getEmail() != null) {
            userRepository.save(user);
        }

        // Save Profile
        UserProfile saved = Objects.requireNonNull(userProfileRepository.save(profile),
                "Persisted profile must not be null");

        // ========================================
        // Emergency Contact (Basic Info Tab)
        // ========================================
        if (hasEmergencyContactFields(request)) {
            updateEmergencyContact(saved, request, currentUserService.requireCurrentUser(principal).getId());
        }

        // ========================================
        // TAB 6: Zoom Integration
        // ========================================
        if (request.getZoomAccountId() != null || request.getZoomClientId() != null || request.getZoomClientSecret() != null) {
            updateZoomCredentialsFromProfile(request, user, principal, ipAddress);
        }

        // ========================================
        // TAB 7: Password
        // ========================================
        if (request.getNewPassword() != null) {
            changePasswordFromProfile(request, user, principal, ipAddress);
        }

        // Capture after state and determine changed fields
        String afterState = auditService.serializeUserProfileState(saved);
        List<String> changedFields = isUpdate ? determineChangedFields(beforeState, afterState) : null;

        // Publish event for async audit logging (for updates)
        if (isUpdate) {
            eventPublisher.publishEvent(new UserProfileUpdatedEvent(
                user.getId(),
                saved.getId(),
                currentUserService.requireCurrentUser(principal).getId(),
                ipAddress,
                beforeState,
                afterState,
                changedFields,
                TenantContext.getOrganisationId(),
                TenantContext.getSchemaName()
            ));
        } else {
            // For creation, use synchronous audit (lightweight)
            auditService.recordUserProfileCreated(saved, currentUserService.requireCurrentUser(principal).getId(), ipAddress);
        }

        // Activity logging (synchronous, lightweight)
        recordActivityEvent(currentUserService.requireCurrentUser(principal).getId(), isUpdate ? "user_profile_updated" : "user_profile_created", 
                requireUserId(user), ipAddress);

        return toProfileResponse(saved);
    }

    @Transactional
    public UserProfileResponse getProfile(AuthPrincipal principal) {
        Objects.requireNonNull(principal, "Principal is required");
        User currentUser = currentUserService.requireCurrentUser(principal);
        UserProfile profile = userProfileRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> userProfileRepository.save(UserProfile.builder().user(currentUser).build()));
        return toProfileResponse(profile);
    }

    @Transactional(readOnly = true)
    public String getTimezone(AuthPrincipal principal) {
        Objects.requireNonNull(principal, "Principal is required");
        UserProfile profile = userProfileRepository.findByUserId(currentUserService.requireCurrentUser(principal).getId())
                .orElse(null);
        return resolveProfileTimezone(profile);
    }

    /**
     * A therapist who has never chosen a timezone follows the clinic, so return that
     * rather than a blank the caller has to interpret. The stored value stays null in
     * that case, which is what lets the therapist move with the clinic if its timezone
     * changes. Only an explicit pick is written to the profile.
     */
    private String resolveProfileTimezone(UserProfile profile) {
        if (profile != null) {
            String own = profile.getTimezone();
            if (own != null && !own.isBlank()) {
                return own.trim();
            }
        }
        return timezoneService.findPracticeTimezoneId().orElse(null);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId, AuthPrincipal requester) {
        Objects.requireNonNull(userId, "User id is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Verify user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long requesterId = currentUserService.requireCurrentUser(requester).getId();
        boolean ownProfile = userId.equals(requesterId);
        boolean canManageUsers = permissionChecker.hasPermission(requester, "USER_MANAGE")
                || permissionChecker.hasPermission(requester, "USER_VIEW");
        boolean canViewTeamPeer = !ownProfile
                && !canManageUsers
                && (permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM")
                        || permissionChecker.hasPermission(requester, "SESSION_VIEW"))
                && caseloadScopeService.resolve(requester).includesTherapist(userId);

        if (!ownProfile && !canManageUsers && !canViewTeamPeer) {
            throw new ForbiddenException("You can only view your own profile");
        }

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        return toProfileResponse(profile);
    }

    @Transactional
    // The cached UserResponse embeds this profile; see upsertProfile.
    @CacheEvict(value = "users", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey(#userId)")
    public UserProfileResponse createUserProfile(Long userId, UserProfileRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(userId, "User id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Authorization check
        // PBAC: Check permission to manage users or own profile
        if (!userId.equals(currentUserService.requireCurrentUser(requester).getId()) && !permissionChecker.hasPermission(requester,"USER_MANAGE")) {
            throw new ForbiddenException("You can only create your own profile");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if profile already exists
        if (userProfileRepository.findByUserId(userId).isPresent()) {
            throw new BadRequestException("Profile already exists. Use PUT or PATCH to update.");
        }

        // Validate request
        profileRequestValidator.validateCreateRequest(request);

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!request.getEmail().equalsIgnoreCase(user.getEmail())) {
                if (userRepository.existsByEmail(request.getEmail())) {
                    throw new BadRequestException("Email already in use");
                }
                user.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
            }
        }
        if (request.getFullName() != null || request.getEmail() != null) {
            userRepository.save(user);
        }

        UserProfile profile = UserProfile.builder().user(user).build();
        applyProfileRequestToEntity(profile, request);

        UserProfile saved = Objects.requireNonNull(userProfileRepository.save(profile),
                "Persisted profile must not be null");

        // Emergency Contact
        if (hasEmergencyContactFields(request)) {
            updateEmergencyContact(saved, request, currentUserService.requireCurrentUser(requester).getId());
        }

        // TAB 6: Zoom Integration
        if (request.getZoomAccountId() != null || request.getZoomClientId() != null || request.getZoomClientSecret() != null) {
            updateZoomCredentialsFromProfile(request, user, requester, ipAddress);
        }

        // TAB 7: Password
        if (request.getNewPassword() != null) {
            changePasswordFromProfile(request, user, requester, ipAddress);
        }

        // Audit logging (synchronous for creation - lightweight)
        auditService.recordUserProfileCreated(saved, currentUserService.requireCurrentUser(requester).getId(), ipAddress);
        
        // Activity logging
        recordActivityEvent(currentUserService.requireCurrentUser(requester).getId(), "user_profile_created", userId, ipAddress);
        
        return toProfileResponse(saved);
    }

    @Transactional
    // The cached UserResponse embeds this profile; see upsertProfile.
    @CacheEvict(value = "users", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey(#userId)")
    public UserProfileResponse updateUserProfile(Long userId, UserProfileRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(userId, "User id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Authorization check
        // PBAC: Check permission to manage users or own profile
        if (!userId.equals(currentUserService.requireCurrentUser(requester).getId()) && !permissionChecker.hasPermission(requester,"USER_MANAGE")) {
            throw new ForbiddenException("You can only update your own profile");
        }

        // Verify user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        // Capture before state for audit logging
        String beforeState = auditService.serializeUserProfileState(profile);

        // Validate request
        profileRequestValidator.validateUpdateRequest(request);

        // Update User entity if basic info provided
        User user = profile.getUser();
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!request.getEmail().equalsIgnoreCase(user.getEmail())) {
                if (userRepository.existsByEmail(request.getEmail())) {
                    throw new BadRequestException("Email already in use");
                }
                user.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
            }
        }
        if (request.getFullName() != null || request.getEmail() != null) {
            userRepository.save(user);
        }

        applyProfileRequestToEntity(profile, request);

        UserProfile saved = Objects.requireNonNull(userProfileRepository.save(profile),
                "Persisted profile must not be null");

        // Emergency Contact
        if (hasEmergencyContactFields(request)) {
            updateEmergencyContact(saved, request, currentUserService.requireCurrentUser(requester).getId());
        }

        // TAB 6: Zoom Integration
        if (request.getZoomAccountId() != null || request.getZoomClientId() != null || request.getZoomClientSecret() != null) {
            updateZoomCredentialsFromProfile(request, user, requester, ipAddress);
        }

        // TAB 7: Password
        if (request.getNewPassword() != null) {
            changePasswordFromProfile(request, user, requester, ipAddress);
        }

        // Capture after state and determine changed fields
        String afterState = auditService.serializeUserProfileState(saved);
        List<String> changedFields = determineChangedFields(beforeState, afterState);

        // Publish event for async audit logging
        eventPublisher.publishEvent(new UserProfileUpdatedEvent(
            userId,
            saved.getId(),
            currentUserService.requireCurrentUser(requester).getId(),
            ipAddress,
            beforeState,
            afterState,
            changedFields,
            TenantContext.getOrganisationId(),
            TenantContext.getSchemaName()
        ));

        // Activity logging (synchronous, lightweight)
        recordActivityEvent(currentUserService.requireCurrentUser(requester).getId(), "user_profile_updated", userId, ipAddress);
        
        return toProfileResponse(saved);
    }

    @Transactional
    public void deleteUserProfile(Long userId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(userId, "User id is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Authorization check
        // PBAC: Check permission to manage users or own profile
        if (!userId.equals(currentUserService.requireCurrentUser(requester).getId()) && !permissionChecker.hasPermission(requester,"USER_MANAGE")) {
            throw new ForbiddenException("You can only delete your own profile");
        }

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        // Capture state before deletion for audit
        String beforeState = auditService.serializeUserProfileState(profile);

        userProfileRepository.delete(profile);

        // Record deletion audit (synchronous, lightweight)
        try {
            auditService.recordAuditEventWithUser(currentUserService.requireCurrentUser(requester).getId(), builder -> {
                builder.action("user_profile_deleted")
                        .result("success")
                        .resourceType("user_profile")
                        .resourceId(String.valueOf(profile.getId()))
                        .hipaaRelevant(false)
                        .ipAddress(ipAddress)
                        .beforeState(beforeState)
                        .afterState(null) // No after state for deletion
                        .changedFields(null);
            });
        } catch (Exception e) {
            log.error("Failed to record profile deletion audit: profileId={}", profile.getId(), e);
        }

        // Activity logging
        recordActivityEvent(currentUserService.requireCurrentUser(requester).getId(), "user_profile_deleted", userId, ipAddress);
    }

    @Transactional(readOnly = true)
    public ZoomCredentialsResponse getZoomCredentialsStatus(Long userId, AuthPrincipal requester) {
        Objects.requireNonNull(userId, "User ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Ensure only admins can query other users' Zoom status
        // PBAC: Check permission to manage users or own profile
        if (!userId.equals(currentUserService.requireCurrentUser(requester).getId()) && !permissionChecker.hasPermission(requester,"USER_MANAGE")) {
            throw new ForbiddenException("You can only view your own Zoom credentials");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return userIntegrationRepository.findByUserAndIntegrationType(user, "zoom")
                .map(this::toZoomResponse)
                .orElseGet(() -> ZoomCredentialsResponse.builder()
                        .configured(false)
                        .build());
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, AuthPrincipal principal, String ipAddress) {
        Objects.requireNonNull(request, "Request payload is required");
        Objects.requireNonNull(principal, "Principal is required");
        User user = userRepository.findById(currentUserService.requireCurrentUser(principal).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getAuthIdentity() == null) {
            throw new BadRequestException("User has no auth identity");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getAuthIdentity().getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getAuthIdentity().getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }

        authIdentityService.updatePassword(user.getAuthIdentity().getId(), request.getNewPassword());

        Long authId = user.getAuthIdentity().getId();
        if (authId != null) {
            tokenBlacklistService.blacklistAllUserTokens(authId);
        }

        Long persistedId = requireUserId(user);
        recordAuditEvent(currentUserService.requireCurrentUser(principal).getId(), "password_changed", persistedId, ipAddress, true);
        recordActivityEvent(currentUserService.requireCurrentUser(principal).getId(), "password_changed", persistedId, ipAddress);
    }

    @Transactional
    public ZoomCredentialsResponse updateZoomCredentials(ZoomCredentialsRequest request, AuthPrincipal principal,
            String ipAddress) {
        Objects.requireNonNull(request, "Request payload is required");
        Objects.requireNonNull(principal, "Principal is required");
        User user = userRepository.findById(currentUserService.requireCurrentUser(principal).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Find or create Zoom integration
        UserIntegration zoomIntegration = userIntegrationRepository
                .findByUserAndIntegrationType(user, "zoom")
                .orElseGet(() -> UserIntegration.builder()
                        .user(user)
                        .integrationType("zoom")
                        .isActive(true)
                        .build());

        // Update integration fields
        zoomIntegration.setExternalUserId(request.getAccountId().trim());
        zoomIntegration.setIsActive(true);

        // Store client ID and secret in settings JSON
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            java.util.Map<String, String> settings = new java.util.HashMap<>();
            settings.put("clientId", request.getClientId().trim());
            settings.put("clientSecret", request.getClientSecret().trim());
            zoomIntegration.setSettings(mapper.valueToTree(settings));
        } catch (Exception e) {
            log.error("Error creating Zoom settings JSON", e);
            throw new BadRequestException("Failed to save Zoom credentials");
        }

        UserIntegration persisted = userIntegrationRepository.save(zoomIntegration);
        recordAuditEvent(currentUserService.requireCurrentUser(principal).getId(), "zoom_credentials_updated", currentUserService.requireCurrentUser(principal).getId(), ipAddress, false);
        recordActivityEvent(currentUserService.requireCurrentUser(principal).getId(), "zoom_credentials_updated", currentUserService.requireCurrentUser(principal).getId(), ipAddress);

        return toZoomResponse(persisted);
    }

    @Transactional
    public void deleteZoomCredentials(AuthPrincipal principal, String ipAddress) {
        Objects.requireNonNull(principal, "Principal is required");
        User user = userRepository.findById(currentUserService.requireCurrentUser(principal).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Hard-delete all Zoom integrations for this user (defensive against duplicates).
        int deleted = userIntegrationRepository.deleteAllByUserAndIntegrationType(user, "zoom");
        userIntegrationRepository.flush();
        if (deleted > 0) {
            recordAuditEvent(currentUserService.requireCurrentUser(principal).getId(), "zoom_credentials_removed", currentUserService.requireCurrentUser(principal).getId(), ipAddress,
                    false);
            recordActivityEvent(currentUserService.requireCurrentUser(principal).getId(), "zoom_credentials_removed", currentUserService.requireCurrentUser(principal).getId(), ipAddress);
        }
    }

    @Transactional(readOnly = true)
    public ZoomCredentialsResponse getZoomCredentialsStatus(AuthPrincipal principal) {
        Objects.requireNonNull(principal, "Principal is required");
        User user = userRepository.findById(currentUserService.requireCurrentUser(principal).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return userIntegrationRepository.findByUserAndIntegrationType(user, "zoom")
                .map(this::toZoomResponse)
                .orElseGet(() -> ZoomCredentialsResponse.builder()
                        .configured(false)
                        .build());
    }

    @Transactional(readOnly = true)
    public ZoomCredentialsResponse testZoomCredentials(AuthPrincipal principal) {
        Objects.requireNonNull(principal, "Principal is required");
        User user = userRepository.findById(currentUserService.requireCurrentUser(principal).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserIntegration zoomIntegration = userIntegrationRepository
                .findByUserAndIntegrationType(user, "zoom")
                .orElseThrow(() -> new BadRequestException("Zoom credentials are not configured"));

        if (!zoomIntegration.getIsActive()) {
            throw new BadRequestException("Zoom integration is disabled");
        }

        // Actually test credentials by attempting to fetch an access token from Zoom OAuth API
        try {
            String clientId;
            String clientSecret;
            String accountId = zoomIntegration.getExternalUserId();

            // Extract credentials from settings JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            try {
                if (zoomIntegration.getSettings() != null) {
                    clientId = zoomIntegration.getSettings().get("clientId").asText();
                    clientSecret = zoomIntegration.getSettings().get("clientSecret").asText();
                } else {
                    throw new BadRequestException("Zoom credentials are incomplete: settings not found");
                }
            } catch (Exception e) {
                log.error("Failed to extract Zoom credentials from settings for user: {}", user.getId(), e);
                throw new BadRequestException("Zoom credentials are incomplete: failed to extract client ID and secret");
            }

            if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret) || !StringUtils.hasText(accountId)) {
                throw new BadRequestException("Zoom credentials are incomplete: missing client ID, client secret, or account ID");
            }

            // Attempt to get access token from Zoom OAuth API
            String credentials = clientId + ":" + clientSecret;
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + encoded);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "account_credentials");
            body.add("account_id", accountId);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            
            String oauthUrl = ZoomOauthUrls.tokenUrl(zoomOauthBaseUrl);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    oauthUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !response.getStatusCode().is2xxSuccessful()) {
                throw new BadRequestException("Zoom credentials test failed: Invalid response from Zoom API");
            }

            String accessToken = (String) responseBody.get("access_token");
            if (!StringUtils.hasText(accessToken)) {
                throw new BadRequestException("Zoom credentials test failed: No access token received from Zoom API");
            }

            // Credentials are valid - update last test timestamp
            log.info("Zoom credentials test successful for user: {}", user.getId());
            
            return ZoomCredentialsResponse.builder()
                    .configured(true)
                    .accountId(accountId)
                    .lastUpdatedAt(zoomIntegration.getUpdatedAt())
                    .build();

        } catch (RestClientException ex) {
            log.error("Zoom credentials test failed for user: {}", user.getId(), ex);
            throw new BadRequestException("Zoom credentials test failed: " + 
                    (ex.getMessage() != null ? ex.getMessage() : "Unable to connect to Zoom API. Please verify your credentials."));
        } catch (BadRequestException ex) {
            // Re-throw BadRequestException as-is
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during Zoom credentials test for user: {}", user.getId(), ex);
            throw new BadRequestException("Zoom credentials test failed: " + ex.getMessage());
        }
    }

    private Specification<User> buildUserSpecification(String search, String role, Boolean active,
            AuthPrincipal requester) {
        // Keep list behavior aligned with findById/exists queries that exclude soft-deleted users.
        Specification<User> specification = (root, query, cb) -> cb.or(
                cb.isFalse(root.get("isDeleted")),
                cb.isNull(root.get("isDeleted"))
        );
        Long orgId = TenantContext.getOrganisationId();

        if (StringUtils.hasText(search)) {
            String likePattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root1, query1, cb1) -> {
                Join<User, AuthIdentity> authIdentityJoin = root1.join("authIdentity", JoinType.LEFT);
                Predicate usernameMatch = cb1.like(cb1.lower(authIdentityJoin.get("loginIdentifier")), likePattern);
                Predicate emailMatch = cb1.like(cb1.lower(root1.get("email")), likePattern);
                Predicate nameMatch = cb1.like(cb1.lower(root1.get("fullName")), likePattern);
                return cb1.or(usernameMatch, emailMatch, nameMatch);
            });
        }

        if (StringUtils.hasText(role)) {
            String normalizedRole = normalizeRoleAlias(role.trim().toUpperCase(Locale.ROOT));
            specification = specification.and((root1, query1, cb1) -> {
                if (query1 != null) {
                    query1.distinct(true);
                }
                Join<User, AuthIdentity> authIdentityJoin = root1.join("authIdentity", JoinType.INNER);
                Join<AuthIdentity, AuthIdentityRole> authIdentityRoleJoin = authIdentityJoin.join("roles", JoinType.INNER);
                Join<AuthIdentityRole, Role> roleJoin = authIdentityRoleJoin.join("role", JoinType.INNER);

                Predicate roleNameMatch = cb1.equal(cb1.upper(roleJoin.get("name")), normalizedRole);
                if (orgId != null) {
                    Predicate roleInOrg = cb1.equal(authIdentityRoleJoin.get("organisation").get("id"), orgId);
                    return cb1.and(roleNameMatch, roleInOrg);
                }
                return roleNameMatch;
            });
        }

        if (active != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("isActive"), active));
        }

        if (hasRole(requester, "SUPERVISOR")) {
            specification = specification.and(limitToSupervisorAssignments(currentUserService.requireCurrentUser(requester).getId()));
        } else if (!hasRole(requester, "ADMIN") && !hasRole(requester, "SUPER_ADMIN")
                && !permissionChecker.hasPermission(requester, "USER_VIEW")
                && !permissionChecker.hasPermission(requester, "USER_MANAGE")) {
            // Users without directory permissions can only see themselves
            specification = specification.and((root, query, cb) -> cb.equal(root.get("id"), currentUserService.requireCurrentUser(requester).getId()));
        }

        return specification;
    }

    private Specification<User> limitToSupervisorAssignments(Long supervisorId) {
        return (root, query, cb) -> {
            List<Long> therapistIds = supervisorAssignmentRepository.findBySupervisorId(supervisorId)
                    .stream()
                    .map(SupervisorAssignment::getTherapist)
                    .filter(Objects::nonNull)
                    .map(User::getId)
                    .collect(Collectors.toList());

            Predicate superviseesPredicate = therapistIds.isEmpty()
                    ? cb.disjunction()
                    : root.get("id").in(therapistIds);

            Predicate selfPredicate = cb.equal(root.get("id"), supervisorId);
            return cb.or(superviseesPredicate, selfPredicate);
        };
    }

    private void validateUserVisibility(User target, AuthPrincipal requester) {
        Objects.requireNonNull(target, "Target user is required");
        Objects.requireNonNull(requester, "Requester is required");
        if (Objects.equals(target.getId(), currentUserService.requireCurrentUser(requester).getId())) {
            return;
        }

        // PBAC: Permission-based access control
        if (permissionChecker.hasPermission(requester, "USER_MANAGE")
                || permissionChecker.hasPermission(requester, "USER_VIEW")) {
            return; // Can view users for directory/scheduling reads
        }

        if (permissionChecker.hasPermission(requester,"CLIENT_VIEW_TEAM")) {
            validateSupervisorOwnership(target, requester);
            return;
        }

        throw new ForbiddenException("You do not have permission to view this user");
    }

    private void validateSupervisorOwnership(User target, AuthPrincipal supervisorPrincipal) {
        Objects.requireNonNull(target, "Target user is required");
        Objects.requireNonNull(supervisorPrincipal, "Supervisor principal is required");
        User supervisorUser = currentUserService.requireCurrentUser(supervisorPrincipal);
        boolean assigned = supervisorAssignmentRepository.findBySupervisorId(supervisorUser.getId())
                .stream()
                .map(SupervisorAssignment::getTherapist)
                .filter(Objects::nonNull)
                .anyMatch(therapist -> Objects.equals(therapist.getId(), target.getId()));

        if (!assigned && !Objects.equals(target.getId(), supervisorUser.getId())) {
            throw new ForbiddenException("You can only manage therapists you supervise");
        }
    }

    private Set<Role> resolveRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new BadRequestException("Roles are required");
        }

        Long orgId = TenantContext.getOrganisationId();
        if (orgId == null) {
            throw new BadRequestException("Organisation context is required to resolve roles");
        }

        Set<String> normalizedRoles = roles.stream()
                .filter(StringUtils::hasText)
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .map(this::normalizeRoleAlias)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return normalizedRoles.stream()
                .map(name -> roleRepository.findByNameForOrganisation(name, orgId)
                        .orElseThrow(() -> new BadRequestException("Role not found: " + name)))
                .collect(Collectors.toSet());
    }

    private String normalizeRoleAlias(String roleName) {
        if ("BILLING".equals(roleName)) {
            return "BILLING_SPECIALIST";
        }
        return roleName;
    }

    private void enforceRoleLimitsForTransition(Long organisationId,
                                                Collection<Role> existingRoles,
                                                boolean currentlyActive,
                                                Collection<Role> targetRoles,
                                                boolean targetActive) {
        if (organisationId == null || !targetActive) {
            return;
        }
        if (!hasNewActiveRole(existingRoles, currentlyActive, targetRoles, targetActive, RoleName.THERAPIST.name())
                && !hasNewActiveRole(existingRoles, currentlyActive, targetRoles, targetActive, RoleName.SUPERVISOR.name())) {
            return;
        }
        enforceTherapistLimitIfNeeded(organisationId, existingRoles, currentlyActive, targetRoles, targetActive);
        enforceSupervisorLimitIfNeeded(organisationId, existingRoles, currentlyActive, targetRoles, targetActive);
    }

    private void enforceTherapistLimitIfNeeded(Long organisationId,
                                               Collection<Role> existingRoles,
                                               boolean currentlyActive,
                                               Collection<Role> targetRoles,
                                               boolean targetActive) {
        if (!hasNewActiveRole(existingRoles, currentlyActive, targetRoles, targetActive, RoleName.THERAPIST.name())) {
            return;
        }
        Integer therapistLimit = subscriptionFeatureService.getEffectiveLimit(
                organisationId,
                CoreFeature.THERAPIST_LIMIT.getCode(),
                null);
        if (therapistLimit == null) {
            return;
        }
        long activeTherapists = authIdentityRoleRepository.countActiveByRoleForOrganisation(
                organisationId,
                RoleName.THERAPIST.name());
        if (activeTherapists >= therapistLimit.longValue()) {
            throw new ForbiddenException("Therapist limit reached (" + therapistLimit + "). Please upgrade your plan to add more therapists.");
        }
    }

    private void enforceSupervisorLimitIfNeeded(Long organisationId,
                                                Collection<Role> existingRoles,
                                                boolean currentlyActive,
                                                Collection<Role> targetRoles,
                                                boolean targetActive) {
        if (!hasNewActiveRole(existingRoles, currentlyActive, targetRoles, targetActive, RoleName.SUPERVISOR.name())) {
            return;
        }
        Integer supervisorLimit = subscriptionFeatureService.getEffectiveLimit(
                organisationId,
                CoreFeature.SUPERVISOR_LIMIT.getCode(),
                null);
        if (supervisorLimit == null) {
            return;
        }
        long activeSupervisors = authIdentityRoleRepository.countActiveByRoleForOrganisation(
                organisationId,
                RoleName.SUPERVISOR.name());
        if (activeSupervisors >= supervisorLimit.longValue()) {
            throw new ForbiddenException("Supervisor limit reached (" + supervisorLimit + "). Please upgrade your plan to add more supervisors.");
        }
    }

    private boolean hasNewActiveRole(Collection<Role> existingRoles,
                                     boolean currentlyActive,
                                     Collection<Role> targetRoles,
                                     boolean targetActive,
                                     String roleName) {
        boolean currentlyHasRole = currentlyActive && containsRoleName(existingRoles, roleName);
        boolean targetHasRole = targetActive && containsRoleName(targetRoles, roleName);
        return targetHasRole && !currentlyHasRole;
    }

    private boolean containsRoleName(Collection<Role> roles, String roleName) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream()
                .map(Role::getName)
                .filter(Objects::nonNull)
                .map(name -> name.trim().toUpperCase(Locale.ROOT))
                .anyMatch(normalized -> normalized.equals(roleName));
    }

    /**
     * Removing SUPERVISOR while therapist assignments still exist leaves orphaned caseload links.
     * Admin must delete those assignments first.
     */
    private void assertNoSupervisorAssignmentsBeforeRemovingSupervisorRole(
            User user, Set<Role> existingRoles, Set<Role> newRoles) {
        if (!containsRoleName(existingRoles, RoleName.SUPERVISOR.name())) {
            return;
        }
        if (containsRoleName(newRoles, RoleName.SUPERVISOR.name())) {
            return;
        }
        List<SupervisorAssignment> assignments =
                supervisorAssignmentRepository.findBySupervisorId(user.getId());
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        throw new BadRequestException(
                "Delete all supervisor assignments for this user before changing their role.");
    }

    private String resolveImageExtension(String originalFilename, String contentType) {
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".gif")) {
                return ext.equals(".jpeg") ? ".jpg" : ext;
            }
        }

        if (contentType == null) {
            return ".jpg";
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    private UserResponse toUserResponse(User user) {
        UserProfileResponse profileResponse = userProfileRepository.findByUserId(user.getId())
                .map(this::toProfileResponse)
                .orElse(null);

        String username = user.getAuthIdentity() != null ? user.getAuthIdentity().getLoginIdentifier() : null;
        Long orgId = TenantContext.getOrganisationId();
        List<String> roleNames = List.of();
        if (user.getAuthIdentity() != null && orgId != null) {
            List<AuthIdentityRole> assignedRoles = authIdentityRoleRepository
                    .findByAuthIdAndOrganisationIdWithRolesAndPermissions(user.getAuthIdentity().getId(), orgId);
            roleNames = assignedRoles.stream()
                    .map(AuthIdentityRole::getRole)
                    .filter(Objects::nonNull)
                    .map(Role::getName)
                    .collect(Collectors.toList());
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(username)
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profilePicture(user.getProfilePicture())
                .active(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLogin(user.getAuthIdentity() != null ? user.getAuthIdentity().getLastSuccessfulLogin() : null)
                .roles(roleNames)
                .profile(profileResponse)
                .build();
    }

    private UserProfileResponse toProfileResponse(UserProfile profile) {
        // Get User entity for basic info
        User user = profile.getUser();
        
        // Convert entity lists to simple string lists for frontend
        List<String> specializationNames = profile.getSpecializations() != null
                ? profile.getSpecializations().stream()
                        .map(UserProfileSpecialization::getSpecialization)
                        .collect(Collectors.toList())
                : List.of();

        List<String> languageNames = profile.getLanguages() != null
                ? profile.getLanguages().stream()
                        .map(UserProfileLanguage::getLanguage)
                        .collect(Collectors.toList())
                : List.of();

        List<UserProfileEducationResponse> educationEntries = profile.getEducation() != null
                ? profile.getEducation().stream()
                        .sorted(Comparator.comparing(
                                UserProfileEducation::getDisplayOrder,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        ))
                        .map(edu -> UserProfileEducationResponse.builder()
                                .id(edu.getId())
                                .degreeType(edu.getDegreeType())
                                .fieldOfStudy(edu.getFieldOfStudy())
                                .institution(edu.getInstitution())
                                .graduationYear(edu.getGraduationYear())
                                .graduationDate(edu.getGraduationDate())
                                .isAccredited(edu.getIsAccredited())
                                .accreditationBody(edu.getAccreditationBody())
                                .displayOrder(edu.getDisplayOrder())
                                .notes(edu.getNotes())
                                .build())
                        .collect(Collectors.toList())
                : List.of();

        // Convert working hours entities to frontend format (All-Services = service_id null)
        List<UserProfileWorkingHours> allServiceHours = filterWorkingHoursByService(profile.getWorkingHours(), null);
        String workingHoursJson = convertWorkingHoursToJson(allServiceHours);
        List<String> workingDaysList = allServiceHours.stream()
                .map(UserProfileWorkingHours::getDay)
                .distinct()
                .collect(Collectors.toList());
        Long consultationServiceId = serviceRepository.findByServiceCode(CONSULTATION_SERVICE_CODE)
                .map(com.smart.therapy.flow.billing.entity.Service::getId)
                .orElse(null);
        String consultationWorkingHoursJson = consultationServiceId == null
                ? null
                : convertWorkingHoursToJson(filterWorkingHoursByService(profile.getWorkingHours(), consultationServiceId));

        // Get emergency contact
        Optional<com.smart.therapy.flow.user.entity.UserContact> emergencyContact = 
                userContactRepository.findByUserProfileAndType(profile, "emergency");

        // Get physical room IDs
        List<Long> physicalRoomIds = profile.getAvailablePhysicalRooms() != null
                ? profile.getAvailablePhysicalRooms().stream()
                        .map(pr -> pr.getRoom() != null ? pr.getRoom().getId() : null)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                : List.of();

        return UserProfileResponse.builder()
                .id(profile.getId())
                // TAB 1: Basic Info
                .fullName(user != null ? user.getFullName() : null)
                .email(user != null ? user.getEmail() : null)
                .profilePicture(user != null ? user.getProfilePicture() : null)
                .emergencyContactName(emergencyContact.map(com.smart.therapy.flow.user.entity.UserContact::getName).orElse(null))
                .emergencyContactPhone(emergencyContact.map(com.smart.therapy.flow.user.entity.UserContact::getPhone).orElse(null))
                .emergencyContactEmail(emergencyContact.map(com.smart.therapy.flow.user.entity.UserContact::getEmail).orElse(null))
                .emergencyContactRelationship(emergencyContact.map(com.smart.therapy.flow.user.entity.UserContact::getRelationship).orElse(null))
                // TAB 2: License
                .licenseNumber(profile.getLicenseNumber())
                .licenseType(profile.getLicenseType())
                .licenseState(profile.getLicenseState())
                .licenseExpiry(profile.getLicenseExpiry())
                .licenseStatus(profile.getLicenseStatus())
                // TAB 3: Specialization
                .specializations(specializationNames)
                .languages(languageNames)
                // TAB 4: Background
                .yearsOfExperience(profile.getYearsOfExperience())
                .clinicalExperience(profile.getClinicalExperience())
                .researchBackground(profile.getResearchBackground())
                .education(educationEntries)
                .supervisoryExperience(profile.getSupervisoryExperience())
                .careerObjectives(profile.getCareerObjectives())
                // TAB 5: Schedule
                .workingDays(workingDaysList)
                .workingHours(workingHoursJson)
                .consultationWorkingHours(consultationWorkingHoursJson)
                .maxClientsPerDay(profile.getMaxClientsPerDay())
                .sessionDuration(profile.getSessionDuration())
                .availabilityStatus(profile.getAvailabilityStatus())
                .timezone(resolveProfileTimezone(profile))
                .virtualRoomId(profile.getVirtualRoom() != null ? profile.getVirtualRoom().getId() : null)
                .availablePhysicalRoomIds(physicalRoomIds)
                // TAB 6: Zoom Integration
                .zoomConfigured(getZoomConfiguredStatus(user))
                .zoomAccountId(getZoomAccountId(user))
                .zoomLastUpdatedAt(getZoomLastUpdatedAt(user))
                // TAB 7: Password (password state is on AuthIdentity; not exposed here)
                .passwordChangeRequired(false)
                .build();
    }

    /**
     * Update Zoom credentials from profile request.
     */
    private void updateZoomCredentialsFromProfile(UserProfileRequest request, User user, AuthPrincipal principal, String ipAddress) {
        // Validate all required fields are provided
        if (request.getZoomAccountId() == null || request.getZoomAccountId().isBlank()) {
            throw new BadRequestException("Zoom Account ID is required");
        }
        if (request.getZoomClientId() == null || request.getZoomClientId().isBlank()) {
            throw new BadRequestException("Zoom Client ID is required");
        }
        if (request.getZoomClientSecret() == null || request.getZoomClientSecret().isBlank()) {
            throw new BadRequestException("Zoom Client Secret is required");
        }

        // Find or create Zoom integration
        UserIntegration zoomIntegration = userIntegrationRepository
                .findByUserAndIntegrationType(user, "zoom")
                .orElseGet(() -> UserIntegration.builder()
                        .user(user)
                        .integrationType("zoom")
                        .isActive(true)
                        .build());

        // Update integration fields
        zoomIntegration.setExternalUserId(request.getZoomAccountId().trim());
        zoomIntegration.setIsActive(true);

        // Store client ID and secret in settings JSON
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            java.util.Map<String, String> settings = new java.util.HashMap<>();
            settings.put("clientId", request.getZoomClientId().trim());
            settings.put("clientSecret", request.getZoomClientSecret().trim());
            zoomIntegration.setSettings(mapper.valueToTree(settings));
        } catch (Exception e) {
            log.error("Error creating Zoom settings JSON", e);
            throw new BadRequestException("Failed to save Zoom credentials");
        }

        userIntegrationRepository.save(zoomIntegration);
        recordAuditEvent(currentUserService.requireCurrentUser(principal).getId(), "zoom_credentials_updated", currentUserService.requireCurrentUser(principal).getId(), ipAddress, false);
        recordActivityEvent(currentUserService.requireCurrentUser(principal).getId(), "zoom_credentials_updated", currentUserService.requireCurrentUser(principal).getId(), ipAddress);
        
        log.info("Zoom credentials updated via profile API: userId={}", user.getId());
    }

    /**
     * Change password from profile request.
     */
    private void changePasswordFromProfile(UserProfileRequest request, User user, AuthPrincipal principal, String ipAddress) {
        // Validate new password and confirm password match
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new BadRequestException("New password is required");
        }
        if (request.getNewPassword().length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters");
        }
        if (request.getConfirmNewPassword() == null || !request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        // Validate current password
        if (user.getAuthIdentity() == null) {
            throw new BadRequestException("User has no auth identity");
        }
        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new BadRequestException("Current password is required for password change");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getAuthIdentity().getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getAuthIdentity().getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }

        authIdentityService.updatePassword(user.getAuthIdentity().getId(), request.getNewPassword());

        // Invalidate old tokens (force re-login for security) — revoke by authId
        User currentUser = currentUserService.requireCurrentUser(principal);
        Long authId = currentUser.getAuthIdentity() != null ? currentUser.getAuthIdentity().getId() : null;
        if (authId != null) tokenBlacklistService.blacklistAllUserTokens(authId);

        Long persistedId = requireUserId(user);
        recordAuditEvent(currentUserService.requireCurrentUser(principal).getId(), "password_changed", persistedId, ipAddress, true);
        recordActivityEvent(currentUserService.requireCurrentUser(principal).getId(), "password_changed", persistedId, ipAddress);
        
        log.info("Password changed via profile API: userId={}", user.getId());
    }

    /**
     * Get Zoom configured status for user.
     */
    private Boolean getZoomConfiguredStatus(User user) {
        if (user == null) {
            return false;
        }
        return userIntegrationRepository.findByUserAndIntegrationType(user, "zoom")
                .map(UserIntegration::getIsActive)
                .orElse(false);
    }

    /**
     * Get Zoom Account ID for user.
     */
    private String getZoomAccountId(User user) {
        if (user == null) {
            return null;
        }
        return userIntegrationRepository.findByUserAndIntegrationType(user, "zoom")
                .map(UserIntegration::getExternalUserId)
                .orElse(null);
    }

    /**
     * Get Zoom last updated timestamp for user.
     */
    private java.time.Instant getZoomLastUpdatedAt(User user) {
        if (user == null) {
            return null;
        }
        return userIntegrationRepository.findByUserAndIntegrationType(user, "zoom")
                .map(UserIntegration::getUpdatedAt)
                .orElse(null);
    }

    private ZoomCredentialsResponse toZoomResponse(UserIntegration integration) {
        if (integration == null) {
            return ZoomCredentialsResponse.builder()
                    .configured(false)
                    .build();
        }

        String clientId = null;
        String clientSecret = null;
        if (integration.getSettings() != null) {
            if (integration.getSettings().has("clientId")) {
                clientId = integration.getSettings().get("clientId").asText(null);
            }
            if (integration.getSettings().has("clientSecret")) {
                clientSecret = integration.getSettings().get("clientSecret").asText(null);
            }
        }

        return ZoomCredentialsResponse.builder()
                .configured(integration.getIsActive())
                .accountId(integration.getExternalUserId())
                .clientId(clientId)
                .clientSecret(clientSecret)
                .lastUpdatedAt(integration.getUpdatedAt())
                .build();
    }

    private void recordAuditEvent(Long actorId, String action, Long resourceId, String ipAddress,
            boolean hipaaRelevant) {
        try {
            auditLogService.recordStaffEvent(actorId, action, RESOURCE_TYPE_USER, resourceId, null, ipAddress,
                    hipaaRelevant);
        } catch (Exception e) {
            log.error("Failed to record audit event for user: {}", resourceId, e);
        }
    }

    @Transactional
    public UserActivityLogResponse logUserActivity(Long userId, CreateUserActivityLogRequest request,
            AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(userId, "User id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Only admins can log activity for other users, or users can log their own
        // PBAC: Check permission to manage users or own profile
        if (!userId.equals(currentUserService.requireCurrentUser(requester).getId()) && !permissionChecker.hasPermission(requester,"USER_MANAGE")) {
            throw new ForbiddenException("You can only log activity for yourself");
        }

        UserActivityLog activityLog = UserActivityLog.builder()
                .user(user)
                .activityType(request.getActivityType())
                .description(request.getDescription())
                .ipAddress(request.getIpAddress() != null ? request.getIpAddress() : ipAddress)
                .userAgent(request.getUserAgent())
                .build();

        UserActivityLog saved = userActivityLogRepository.save(activityLog);
        return toActivityLogResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserActivityLogResponse> getUserActivityHistory(Long userId, int limit, AuthPrincipal requester) {
        Objects.requireNonNull(userId, "User id is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Verify user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Only admins can view activity for other users, or users can view their own
        // PBAC: Check permission to manage users or own profile
        if (!userId.equals(currentUserService.requireCurrentUser(requester).getId()) && !permissionChecker.hasPermission(requester,"USER_MANAGE")) {
            throw new ForbiddenException("You can only view your own activity history");
        }

        List<UserActivityLog> activities = userActivityLogRepository.findByUserId(userId);

        // Sort by createdAt descending and limit
        activities = activities.stream()
                .sorted(Comparator.comparing(UserActivityLog::getCreatedAt).reversed())
                .limit(limit > 0 ? limit : 50)
                .collect(Collectors.toList());

        return activities.stream()
                .map(this::toActivityLogResponse)
                .collect(Collectors.toList());
    }

    private UserActivityLogResponse toActivityLogResponse(UserActivityLog log) {
        return UserActivityLogResponse.builder()
                .id(log.getId())
                .userId(log.getUser().getId())
                .userName(log.getUser().getFullName())
                .activityType(log.getActivityType())
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private void recordActivityEvent(Long actorId, String action, Long resourceId, String ipAddress) {
        if (actorId == null) {
            return;
        }

        userRepository.findById(actorId).ifPresent(actor -> {
            Objects.requireNonNull(actor, "Actor is required");
            UserActivityLog activityLog = UserActivityLog.builder()
                    .activityType(action)
                    .description("User activity: " + action + " on user " + resourceId)
                    .ipAddress(ipAddress)
                    .user(actor)
                    .build();
            userActivityLogRepository.save(activityLog);
        });
    }

    private Long requireUserId(User user) {
        Objects.requireNonNull(user, "User is required");
        return Objects.requireNonNull(user.getId(), "Entity id must not be null");
    }

    // DEPRECATED: Use permission checks instead
    // This method is kept for backward compatibility in business logic validation
    // For access control, always use principal.hasPermission() instead
    @Deprecated
    private boolean hasRole(AuthPrincipal principal, String roleName) {
        return permissionChecker.hasRole(principal, roleName);
    }

    private boolean hasRole(User user, String roleName) {
        if (user.getAuthIdentity() == null) return false;
        AuthPrincipal principal = (AuthPrincipal) authIdentityDetailsService.loadUserByAuthId(user.getAuthIdentity().getId());
        return permissionChecker.hasRole(principal, roleName);
    }

    private static final Set<String> FIXED_TENANT_PORTAL_ROLES = Set.of(
            "SUPER_ADMIN", "ADMIN", "THERAPIST", "SUPERVISOR", "CLIENT");

    private boolean hasFixedTenantPortalRole(AuthPrincipal principal) {
        return FIXED_TENANT_PORTAL_ROLES.stream().anyMatch(role -> hasRole(principal, role));
    }

    private void assertCanCreateUser(AuthPrincipal principal, String message) {
        if (hasRole(principal, "ADMIN") || hasRole(principal, "SUPER_ADMIN")) {
            return;
        }
        if (!hasFixedTenantPortalRole(principal)
                && permissionChecker.hasAnyPermission(principal, "USER_CREATE", "USER_MANAGE")) {
            return;
        }
        throw new ForbiddenException(message);
    }

    private void assertCanUpdateUser(AuthPrincipal principal, String message) {
        if (hasRole(principal, "ADMIN") || hasRole(principal, "SUPER_ADMIN")
                || hasRole(principal, "SUPERVISOR")) {
            return;
        }
        if (!hasFixedTenantPortalRole(principal)
                && permissionChecker.hasAnyPermission(principal, "USER_EDIT", "USER_MANAGE")) {
            return;
        }
        throw new ForbiddenException(message);
    }

    private void assertCanDeleteUser(AuthPrincipal principal, String message) {
        if (hasRole(principal, "ADMIN") || hasRole(principal, "SUPER_ADMIN")) {
            return;
        }
        if (!hasFixedTenantPortalRole(principal)
                && permissionChecker.hasAnyPermission(principal, "USER_DELETE", "USER_MANAGE")) {
            return;
        }
        throw new ForbiddenException(message);
    }

    private void assertCanChangeUserRoles(AuthPrincipal principal, String message) {
        if (hasRole(principal, "ADMIN") || hasRole(principal, "SUPER_ADMIN")) {
            return;
        }
        if (!hasFixedTenantPortalRole(principal)
                && permissionChecker.hasPermission(principal, "USER_MANAGE")) {
            return;
        }
        throw new ForbiddenException(message);
    }

    private void assertAdmin(AuthPrincipal principal, String message) {
        if (!hasRole(principal, "ADMIN") && !hasRole(principal, "SUPER_ADMIN")) {
            throw new ForbiddenException(message);
        }
    }

    // ===================================================================
    // Production-Grade User Profile Entity Methods
    // ===================================================================

    /**
     * Verify a certification by admin.
     * Sets status to VERIFIED and records who verified and when.
     */
    @Transactional
    public void verifyCertification(Long certId, AuthPrincipal admin, String ipAddress) {
        Objects.requireNonNull(certId, "Certification ID is required");
        Objects.requireNonNull(admin, "Admin is required");
        assertAdmin(admin, "Only administrators can verify certifications");

        var cert = certificationRepository.findById(certId)
                .orElseThrow(() -> new ResourceNotFoundException("Certification not found"));

        User adminUser = currentUserService.requireCurrentUser(admin);
        cert.verify(adminUser);
        certificationRepository.save(cert);

        recordAuditEvent(adminUser.getId(), "certification_verified", cert.getUserProfile().getUser().getId(), ipAddress,
                false);
        log.info("Certification {} verified by admin {}", certId, adminUser.getId());
    }

    /**
     * Find certifications expiring within specified days.
     * Useful for automated renewal reminder systems.
     */
    @Transactional(readOnly = true)
    public List<UserProfileCertification> getExpiringCertifications(int daysAhead) {
        LocalDate now = LocalDate.now();
        LocalDate futureDate = now.plusDays(daysAhead);
        return certificationRepository.findExpiringCertifications(now, futureDate);
    }

    /**
     * Get all active certifications for a user profile.
     */
    @Transactional(readOnly = true)
    public List<UserProfileCertification> getActiveCertifications(Long userId) {
        Objects.requireNonNull(userId, "User ID is required");
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
        return certificationRepository.findActiveCertifications(profile, LocalDate.now());
    }

    /**
     * Find therapists by language proficiency.
     * Only returns therapists with clinically qualified proficiency levels.
     */
    @Transactional(readOnly = true)
    public List<UserProfile> findTherapistsByLanguage(String language) {
        Objects.requireNonNull(language, "Language is required");
        return languageRepository.findUserProfilesByLanguage(language);
    }

    /**
     * Find expert therapists by specialization.
     * Only returns therapists with ADVANCED or EXPERT expertise level.
     */
    @Transactional(readOnly = true)
    public List<UserProfile> findExpertsBySpecialization(String specialization) {
        Objects.requireNonNull(specialization, "Specialization is required");
        return specializationRepository.findExpertsBySpecialization(specialization);
    }

    /**
     * Calculate total CE credits for a therapist.
     */
    @Transactional(readOnly = true)
    public Double getTotalCECredits(Long userId) {
        Objects.requireNonNull(userId, "User ID is required");
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
        Double credits = continuingEducationRepository.getTotalCECredits(profile);
        return credits != null ? credits : 0.0;
    }

    /**
     * Find therapists matching specific criteria.
     * Combines language, specialization, and other filters for advanced matching.
     */
    @Transactional(readOnly = true)
    public List<UserProfile> findMatchingTherapists(
            String language,
            String specialization,
            String ageGroup,
            String treatmentApproach) {
        // Start with all profiles
        List<UserProfile> candidates = userProfileRepository.findAll();

        // Filter by language if specified
        if (StringUtils.hasText(language)) {
            Set<Long> languageProfileIds = languageRepository
                    .findClinicallyQualifiedByLanguage(language)
                    .stream()
                    .map(lang -> lang.getUserProfile().getId())
                    .collect(Collectors.toSet());
            candidates = candidates.stream()
                    .filter(profile -> languageProfileIds.contains(profile.getId()))
                    .collect(Collectors.toList());
        }

        // Filter by specialization if specified
        if (StringUtils.hasText(specialization)) {
            Set<Long> specProfileIds = specializationRepository
                    .findBySpecialization(specialization)
                    .stream()
                    .map(spec -> spec.getUserProfile().getId())
                    .collect(Collectors.toSet());
            candidates = candidates.stream()
                    .filter(profile -> specProfileIds.contains(profile.getId()))
                    .collect(Collectors.toList());
        }

        // Filter by age group if specified
        if (StringUtils.hasText(ageGroup)) {
            Set<Long> ageGroupProfileIds = ageGroupRepository
                    .findByAgeGroup(ageGroup)
                    .stream()
                    .map(ag -> ag.getUserProfile().getId())
                    .collect(Collectors.toSet());
            candidates = candidates.stream()
                    .filter(profile -> ageGroupProfileIds.contains(profile.getId()))
                    .collect(Collectors.toList());
        }

        // Filter by treatment approach if specified
        if (StringUtils.hasText(treatmentApproach)) {
            Set<Long> approachProfileIds = treatmentApproachRepository
                    .findByApproach(treatmentApproach)
                    .stream()
                    .map(app -> app.getUserProfile().getId())
                    .collect(Collectors.toSet());
            candidates = candidates.stream()
                    .filter(profile -> approachProfileIds.contains(profile.getId()))
                    .collect(Collectors.toList());
        }

        return candidates;
    }

    /**
     * Verify a language proficiency by admin.
     */
    @Transactional
    public void verifyLanguage(Long languageId, AuthPrincipal admin, String ipAddress) {
        Objects.requireNonNull(languageId, "Language ID is required");
        Objects.requireNonNull(admin, "Admin is required");
        assertAdmin(admin, "Only administrators can verify language proficiency");

        var lang = languageRepository.findById(languageId)
                .orElseThrow(() -> new ResourceNotFoundException("Language not found"));

        User adminUser = currentUserService.requireCurrentUser(admin);

        lang.verify(adminUser);
        languageRepository.save(lang);

        recordAuditEvent(adminUser.getId(), "language_verified", lang.getUserProfile().getUser().getId(), ipAddress, false);
        log.info("Language {} verified by admin {}", languageId, adminUser.getId());
    }

    /**
     * Get certification statistics for monitoring/dashboards.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCertificationStatistics() {
        LocalDate now = LocalDate.now();
        LocalDate in30Days = now.plusDays(30);
        LocalDate in90Days = now.plusDays(90);

        List<UserProfileCertification> expiring30 = certificationRepository.findExpiringCertifications(now, in30Days);
        List<UserProfileCertification> expiring90 = certificationRepository.findExpiringCertifications(now, in90Days);

        Map<String, Object> stats = new HashMap<>();
        stats.put("expiringIn30Days", expiring30.size());
        stats.put("expiringIn90Days", expiring90.size());
        stats.put("expiringIn30DaysList", expiring30);
        stats.put("expiringIn90DaysList", expiring90);

        return stats;
    }

    // ===================================================================
    // Helper Methods for Converting DTO Strings to Entities
    // ===================================================================

    private void applyProfileRequestToEntity(UserProfile profile, UserProfileRequest request) {
        if (request.getLicenseNumber() != null) {
            profile.setLicenseNumber(request.getLicenseNumber());
        }
        if (request.getLicenseType() != null) {
            profile.setLicenseType(request.getLicenseType());
        }
        if (request.getLicenseState() != null) {
            profile.setLicenseState(request.getLicenseState());
        }
        if (request.getLicenseExpiry() != null) {
            profile.setLicenseExpiry(request.getLicenseExpiry());
        }
        if (request.getLicenseStatus() != null) {
            profile.setLicenseStatus(request.getLicenseStatus());
        }

        if (request.getSpecializations() != null) {
            updateSpecializations(profile, request.getSpecializations());
        }
        if (request.getLanguages() != null) {
            updateLanguages(profile, request.getLanguages());
        }

        if (request.getYearsOfExperience() != null) {
            profile.setYearsOfExperience(request.getYearsOfExperience());
        }
        if (request.getClinicalExperience() != null) {
            profile.setClinicalExperience(request.getClinicalExperience());
        }
        if (request.getResearchBackground() != null) {
            profile.setResearchBackground(request.getResearchBackground());
        }
        if (request.getEducation() != null) {
            updateEducation(profile, request.getEducation());
        }
        if (request.getSupervisoryExperience() != null) {
            profile.setSupervisoryExperience(request.getSupervisoryExperience());
        }
        if (request.getCareerObjectives() != null) {
            profile.setCareerObjectives(request.getCareerObjectives());
        }

        if (request.getWorkingHours() != null || request.getWorkingDays() != null) {
            updateWorkingHours(profile, request.getWorkingHours(), request.getWorkingDays(), null);
        }
        if (request.getConsultationWorkingHours() != null) {
            com.smart.therapy.flow.billing.entity.Service consultation = serviceRepository
                    .findByServiceCode(CONSULTATION_SERVICE_CODE)
                    .orElseThrow(() -> new BadRequestException(
                            "Consultation service is not configured. Enable it under Settings → Public Site."));
            updateWorkingHours(profile, request.getConsultationWorkingHours(), null, consultation);
        }
        if (request.getMaxClientsPerDay() != null) {
            profile.setMaxClientsPerDay(request.getMaxClientsPerDay());
        }
        if (request.getSessionDuration() != null) {
            profile.setSessionDuration(request.getSessionDuration());
        }
        if (request.getAvailabilityStatus() != null) {
            profile.setAvailabilityStatus(request.getAvailabilityStatus());
        }
        if (request.getTimezone() != null && !request.getTimezone().isBlank()) {
            profile.setTimezone(timezoneService.normalizeTimezoneId(request.getTimezone()));
        }
        // A therapist who never picked a timezone keeps a null one on purpose: that is
        // what makes them follow the clinic, so back-filling the practice timezone here
        // would silently pin them to whatever it happened to be on the day they were
        // saved. Reads resolve the effective zone instead - see resolveProfileTimezone.

        if (request.getVirtualRoomId() != null) {
            com.smart.therapy.flow.session.entity.Room virtualRoom = roomRepository.findById(request.getVirtualRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Virtual room not found: " + request.getVirtualRoomId()));
            profile.setVirtualRoom(virtualRoom);
        }
        if (request.getAvailablePhysicalRoomIds() != null) {
            updatePhysicalRooms(profile, request.getAvailablePhysicalRoomIds());
        }
    }

    /**
     * Update specializations - convert simple string list to entity list.
     * Clears existing and creates new entities to avoid orphan objects.
     */
    private void updateSpecializations(UserProfile profile, List<String> specializationNames) {
        // Clear existing (orphanRemoval will delete them)
        profile.getSpecializations().clear();

        // Create new entities
        if (specializationNames != null && !specializationNames.isEmpty()) {
            int displayOrder = 0;
            for (String name : specializationNames) {
                if (StringUtils.hasText(name)) {
                    UserProfileSpecialization spec = UserProfileSpecialization.builder()
                            .userProfile(profile)
                            .specialization(name.trim())
                            .displayOrder(displayOrder++)
                            .expertiseLevel(UserProfileSpecialization.ExpertiseLevel.INTERMEDIATE) // Default
                            .build();
                    profile.getSpecializations().add(spec);
                }
            }
        }
    }

    /**
     * Update languages - convert simple string list to entity list.
     * Clears existing and creates new entities.
     */
    private void updateLanguages(UserProfile profile, List<String> languageNames) {
        // Clear existing (orphanRemoval will delete them)
        profile.getLanguages().clear();

        // Create new entities
        if (languageNames != null && !languageNames.isEmpty()) {
            int displayOrder = 0;
            for (String name : languageNames) {
                if (StringUtils.hasText(name)) {
                    UserProfileLanguage lang = UserProfileLanguage.builder()
                            .userProfile(profile)
                            .language(name.trim())
                            .proficiencyLevel(UserProfileLanguage.ProficiencyLevel.CONVERSATIONAL) // Default
                            .displayOrder(displayOrder++)
                            .build();
                    profile.getLanguages().add(lang);
                }
            }
        }
    }

    /**
     * Update education - convert request models to entity list.
     * Clears existing and creates new entities.
     */
    private void updateEducation(UserProfile profile, List<UserProfileEducationRequest> educationEntries) {
        profile.getEducation().clear();

        if (educationEntries == null || educationEntries.isEmpty()) {
            return;
        }

        int displayOrder = 0;
        for (UserProfileEducationRequest entry : educationEntries) {
            if (entry == null) {
                continue;
            }
            UserProfileEducation education = UserProfileEducation.builder()
                    .userProfile(profile)
                    .degreeType(trimToNull(entry.getDegreeType()))
                    .fieldOfStudy(trimToNull(entry.getFieldOfStudy()))
                    .institution(trimToNull(entry.getInstitution()))
                    .graduationYear(entry.getGraduationYear())
                    .graduationDate(entry.getGraduationDate())
                    .isAccredited(entry.getIsAccredited() != null ? entry.getIsAccredited() : Boolean.TRUE)
                    .accreditationBody(trimToNull(entry.getAccreditationBody()))
                    .displayOrder(displayOrder++)
                    .notes(trimToNull(entry.getNotes()))
                    .build();
            profile.getEducation().add(education);
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * Update working hours for a schedule scope.
     * {@code service == null} updates the All-Services schedule; otherwise updates hours for that service only.
     */
    private void updateWorkingHours(UserProfile profile, String workingHoursJson, List<String> workingDays,
            com.smart.therapy.flow.billing.entity.Service service) {
        Long targetServiceId = service != null ? service.getId() : null;
        profile.getWorkingHours().removeIf(h -> {
            Long sid = h.getService() != null ? h.getService().getId() : null;
            return Objects.equals(sid, targetServiceId);
        });

        // Parse from JSON if provided
        if (StringUtils.hasText(workingHoursJson)) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Object data = mapper.readValue(workingHoursJson, Object.class);

                if (data instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> daysList = (List<Map<String, Object>>) data;
                    int rowIndex = 0;
                    List<UserProfileWorkingHours> added = new ArrayList<>();
                    for (Map<String, Object> dayData : daysList) {
                        rowIndex++;
                        String day = (String) dayData.get("day");
                        Boolean enabled = (Boolean) dayData.getOrDefault("enabled", true);
                        String start = (String) dayData.get("start");
                        String end = (String) dayData.get("end");
                        ShiftMode shiftMode = parseShiftMode(dayData.get("mode"));

                        if (enabled != null && enabled && StringUtils.hasText(day) &&
                                StringUtils.hasText(start) && StringUtils.hasText(end)) {
                            LocalTime startTime = parseWorkingHoursTime(start, day, rowIndex, "start");
                            LocalTime endTime = parseWorkingHoursTime(end, day, rowIndex, "end");
                            if (!endTime.isAfter(startTime)) {
                                throw new BadRequestException("Invalid working-hours row " + rowIndex +
                                        " for " + day.toUpperCase(Locale.ROOT) +
                                        ": end time must be greater than start time");
                            }
                            UserProfileWorkingHours hours = UserProfileWorkingHours.builder()
                                    .userProfile(profile)
                                    .service(service)
                                    .day(day.toUpperCase())
                                    .startTime(startTime)
                                    .endTime(endTime)
                                    .sessionMode(shiftMode)
                                    .build();
                            profile.getWorkingHours().add(hours);
                            added.add(hours);
                        }
                    }
                    validateWorkingHoursNoOverlap(added);
                }
                else {
                    throw new BadRequestException("Invalid workingHours format: expected JSON array");
                }
            } catch (Exception e) {
                if (e instanceof BadRequestException) {
                    throw (BadRequestException) e;
                }
                log.error("Error parsing working hours JSON: {}", workingHoursJson, e);
                throw new BadRequestException("Invalid workingHours JSON payload");
            }
        }
        // Fallback: Create default hours from workingDays list if no JSON (All-Services only)
        else if (service == null && workingDays != null && !workingDays.isEmpty()) {
            List<UserProfileWorkingHours> added = new ArrayList<>();
            for (String day : workingDays) {
                if (StringUtils.hasText(day)) {
                    UserProfileWorkingHours hours = UserProfileWorkingHours.builder()
                            .userProfile(profile)
                            .service(null)
                            .day(day.toUpperCase())
                            .startTime(java.time.LocalTime.of(9, 0))
                            .endTime(java.time.LocalTime.of(17, 0))
                            .sessionMode(ShiftMode.BOTH)
                            .build();
                    profile.getWorkingHours().add(hours);
                    added.add(hours);
                }
            }
            validateWorkingHoursNoOverlap(added);
        }
    }

    private List<UserProfileWorkingHours> filterWorkingHoursByService(
            List<UserProfileWorkingHours> hours, Long serviceId) {
        if (hours == null || hours.isEmpty()) {
            return List.of();
        }
        return hours.stream()
                .filter(h -> {
                    Long sid = h.getService() != null ? h.getService().getId() : null;
                    return Objects.equals(sid, serviceId);
                })
                .collect(Collectors.toList());
    }

    /**
     * Update or create emergency contact for user profile.
     */
    private boolean hasEmergencyContactFields(UserProfileRequest request) {
        return request.getEmergencyContactName() != null
                || request.getEmergencyContactPhone() != null
                || request.getEmergencyContactEmail() != null
                || request.getEmergencyContactRelationship() != null;
    }

    private void updateEmergencyContact(UserProfile profile, UserProfileRequest request, Long requesterId) {
        Optional<com.smart.therapy.flow.user.entity.UserContact> existingContact = 
                userContactRepository.findByUserProfileAndType(profile, "emergency");

        com.smart.therapy.flow.user.entity.UserContact emergencyContact;
        
        if (existingContact.isPresent()) {
            emergencyContact = existingContact.get();
            // Update existing
            if (request.getEmergencyContactName() != null) {
                emergencyContact.setName(request.getEmergencyContactName().trim());
            }
            if (request.getEmergencyContactPhone() != null) {
                emergencyContact.setPhone(request.getEmergencyContactPhone().trim());
            }
            if (request.getEmergencyContactEmail() != null) {
                emergencyContact.setEmail(request.getEmergencyContactEmail().trim().toLowerCase(Locale.ROOT));
            }
            if (request.getEmergencyContactRelationship() != null) {
                emergencyContact.setRelationship(request.getEmergencyContactRelationship().trim());
            }
        } else {
            // Create new emergency contact
            if (request.getEmergencyContactName() == null || request.getEmergencyContactName().isBlank()) {
                return; // No emergency contact to create
            }
            
            emergencyContact = com.smart.therapy.flow.user.entity.UserContact.builder()
                    .userProfile(profile)
                    .name(request.getEmergencyContactName().trim())
                    .phone(request.getEmergencyContactPhone() != null ? request.getEmergencyContactPhone().trim() : null)
                    .email(request.getEmergencyContactEmail() != null ? request.getEmergencyContactEmail().trim().toLowerCase(Locale.ROOT) : null)
                    .relationship(request.getEmergencyContactRelationship() != null ? request.getEmergencyContactRelationship().trim() : null)
                    .type("emergency")
                    .build();
        }

        userContactRepository.save(emergencyContact);
        log.debug("Emergency contact updated for profile: profileId={}", profile.getId());
    }

    /**
     * Update physical rooms for user profile.
     */
    private void updatePhysicalRooms(UserProfile profile, List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            // Clear all physical rooms if empty list provided
            profile.getAvailablePhysicalRooms().clear();
            return;
        }

        List<Long> sanitizedRoomIds = roomIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (sanitizedRoomIds.isEmpty()) {
            profile.getAvailablePhysicalRooms().clear();
            return;
        }

        // Clear existing
        profile.getAvailablePhysicalRooms().clear();

        // Add new rooms
        int displayOrder = 0;
        for (Long roomId : sanitizedRoomIds) {
            com.smart.therapy.flow.session.entity.Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Physical room not found: " + roomId));
            if (room.getRoomType() != com.smart.therapy.flow.session.enums.RoomType.PHYSICAL) {
                throw new BadRequestException("Room is not a physical room: " + roomId);
            }
            if (!Boolean.TRUE.equals(room.getIsActive())) {
                throw new BadRequestException("Room is not active: " + roomId);
            }

            com.smart.therapy.flow.user.entity.UserProfilePhysicalRoom physicalRoom = 
                    com.smart.therapy.flow.user.entity.UserProfilePhysicalRoom.builder()
                            .userProfile(profile)
                            .room(room)
                            .isPrimary(displayOrder == 0) // First room is primary
                            .displayOrder(displayOrder++)
                            .build();

            profile.getAvailablePhysicalRooms().add(physicalRoom);
        }

        log.debug("Physical rooms updated for profile: profileId={}, roomCount={}", profile.getId(), sanitizedRoomIds.size());
    }

    /**
     * Convert working hours entities to JSON format for frontend.
     */
    private String convertWorkingHoursToJson(List<UserProfileWorkingHours> workingHours) {
        if (workingHours == null || workingHours.isEmpty()) {
            return null;
        }

        try {
            List<Map<String, Object>> daysList = workingHours.stream()
                    .map(hours -> {
                        Map<String, Object> dayData = new java.util.HashMap<>();
                        dayData.put("day", hours.getDay().toLowerCase());
                        dayData.put("enabled", true);
                        dayData.put("start", hours.getStartTime().toString());
                        dayData.put("end", hours.getEndTime().toString());
                        dayData.put("mode", (hours.getSessionMode() != null ? hours.getSessionMode() : ShiftMode.BOTH).toJsonValue());
                        return dayData;
                    })
                    .collect(Collectors.toList());

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(daysList);
        } catch (Exception e) {
            log.error("Error converting working hours to JSON", e);
            return null;
        }
    }

    private ShiftMode parseShiftMode(Object modeRaw) {
        if (modeRaw == null) {
            return ShiftMode.BOTH;
        }
        try {
            return ShiftMode.fromValue(String.valueOf(modeRaw));
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid working-hours mode '{}', defaulting to BOTH", modeRaw);
            return ShiftMode.BOTH;
        }
    }

    private LocalTime parseWorkingHoursTime(String value, String day, int rowIndex, String fieldName) {
        String raw = value == null ? "" : value.trim();
        if (raw.isEmpty()) {
            throw new BadRequestException("Missing " + fieldName + " time at working-hours row " + rowIndex);
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_TIME,
                WORKING_HOURS_24H_FORMAT,
                WORKING_HOURS_12H_FORMAT
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalTime.parse(raw.toUpperCase(Locale.ROOT), formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format
            }
        }

        throw new BadRequestException("Invalid " + fieldName + " time '" + value + "' at working-hours row "
                + rowIndex + " for " + day.toUpperCase(Locale.ROOT)
                + ". Supported formats: HH:mm, HH:mm:ss, h:mm AM/PM");
    }

    private void validateWorkingHoursNoOverlap(List<UserProfileWorkingHours> workingHours) {
        if (workingHours == null || workingHours.isEmpty()) {
            return;
        }

        Map<String, List<UserProfileWorkingHours>> byDay = workingHours.stream()
                .collect(Collectors.groupingBy(h -> h.getDay().toUpperCase(Locale.ROOT)));

        for (Map.Entry<String, List<UserProfileWorkingHours>> entry : byDay.entrySet()) {
            String day = entry.getKey();
            List<UserProfileWorkingHours> sortedShifts = entry.getValue().stream()
                    .sorted(Comparator.comparing(UserProfileWorkingHours::getStartTime))
                    .collect(Collectors.toList());

            int shiftNumber = 0;
            for (UserProfileWorkingHours current : sortedShifts) {
                shiftNumber++;
                if (current.getStartTime() == null || current.getEndTime() == null) {
                    throw new BadRequestException("Invalid working-hours shift " + shiftNumber + " for " + day
                            + ": start and end times are required");
                }
                if (!current.getEndTime().isAfter(current.getStartTime())) {
                    throw new BadRequestException("Invalid working-hours shift " + shiftNumber + " for " + day
                            + ": end time must be greater than start time");
                }
            }

            for (int i = 0; i < sortedShifts.size(); i++) {
                UserProfileWorkingHours left = sortedShifts.get(i);
                ShiftMode leftMode = left.getSessionMode() != null ? left.getSessionMode() : ShiftMode.BOTH;

                for (int j = i + 1; j < sortedShifts.size(); j++) {
                    UserProfileWorkingHours right = sortedShifts.get(j);
                    ShiftMode rightMode = right.getSessionMode() != null ? right.getSessionMode() : ShiftMode.BOTH;

                    if (right.getStartTime().isAfter(left.getEndTime())) {
                        break;
                    }
                    if (!hasWorkingHoursModeConflict(leftMode, rightMode)) {
                        continue;
                    }

                    throw new BadRequestException("Invalid working-hours shifts for " + day
                            + ": shift " + (i + 1) + " (" + leftMode.toJsonValue() + " "
                            + left.getStartTime() + "-" + left.getEndTime() + ") conflicts with shift " + (j + 1)
                            + " (" + rightMode.toJsonValue() + " " + right.getStartTime() + "-" + right.getEndTime()
                            + "). Overlap is only allowed between 'virtual' and 'in-person' shifts.");
                }
            }
        }
    }

    private boolean hasWorkingHoursModeConflict(ShiftMode leftMode, ShiftMode rightMode) {
        if (leftMode == ShiftMode.BOTH || rightMode == ShiftMode.BOTH) {
            return true;
        }
        return leftMode == rightMode;
    }

    // ========================================
    // SUPERVISOR ASSIGNMENT METHODS
    // ========================================

    @Transactional
    public SupervisorAssignmentResponse createSupervisorAssignment(
            CreateSupervisorAssignmentRequest request, 
            AuthPrincipal requester, 
            String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can create supervisor assignments");

        // Validate supervisor exists and has SUPERVISOR role
        User supervisor = userRepository.findById(request.getSupervisorId())
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor not found"));
        // Validate supervisor has appropriate permissions (via roles)
        AuthPrincipal supervisorPrincipal = resolveAuthPrincipalForAssignment(supervisor, "supervisor");
        if (!permissionChecker.hasPermission(supervisorPrincipal, "CLIENT_VIEW_TEAM") && 
            !permissionChecker.hasPermission(supervisorPrincipal, "USER_MANAGE")) {
            throw new BadRequestException("User must have CLIENT_VIEW_TEAM or USER_MANAGE permission to be assigned as supervisor");
        }

        // Validate therapist exists and has appropriate permissions
        User therapist = userRepository.findById(request.getTherapistId())
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));
        AuthPrincipal therapistPrincipal = resolveAuthPrincipalForAssignment(therapist, "therapist");
        if (!permissionChecker.hasPermission(therapistPrincipal, "CLIENT_VIEW_OWN") && 
            !permissionChecker.hasPermission(therapistPrincipal, "SESSION_CREATE")) {
            throw new BadRequestException("User must have CLIENT_VIEW_OWN or SESSION_CREATE permission to be assigned as therapist");
        }

        // Check if assignment already exists and is active
        List<SupervisorAssignment> existing = supervisorAssignmentRepository.findByTherapistId(request.getTherapistId());
        boolean hasActiveAssignment = existing.stream()
                .anyMatch(assignment -> assignment.getIsActive() && 
                        assignment.getSupervisor().getId().equals(request.getSupervisorId()));
        
        if (hasActiveAssignment) {
            throw new BadRequestException("Active supervisor assignment already exists for this therapist and supervisor");
        }

        // Create assignment
        SupervisorAssignment assignment = SupervisorAssignment.builder()
                .supervisor(supervisor)
                .therapist(therapist)
                .assignmentType(request.getAssignmentType() != null ? request.getAssignmentType() : AssignmentType.PRIMARY)
                .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now())
                .endDate(request.getEndDate())
                .requiredMeetingFrequency(request.getRequiredMeetingFrequency())
                .notes(request.getNotes())
                .isActive(true)
                .assignedDate(Instant.now())
                .build();

        SupervisorAssignment saved = supervisorAssignmentRepository.save(assignment);
        
        // Activity logging
        recordActivityEvent(currentUserService.requireCurrentUser(requester).getId(), "supervisor_assignment_created", saved.getId(), ipAddress);

        if (notificationService != null) {
            try {
                notificationService.processEventInNewTransaction(
                        NotificationEventCatalog.SUPERVISOR_ASSIGNMENT_CREATED,
                        buildSupervisorAssignmentEventData(saved));
            } catch (Exception e) {
                log.error("Failed to trigger supervisor_assignment_created notification", e);
            }
        }
        
        log.info("Supervisor assignment created: assignmentId={}, supervisorId={}, therapistId={}", 
                saved.getId(), supervisor.getId(), therapist.getId());
        
        return toSupervisorAssignmentResponse(saved);
    }

    private AuthPrincipal resolveAuthPrincipalForAssignment(User user, String userRoleLabel) {
        if (user.getAuthIdentity() == null || user.getAuthIdentity().getId() == null) {
            throw new BadRequestException("Selected " + userRoleLabel + " is not linked to an authentication identity");
        }
        try {
            return (AuthPrincipal) authIdentityDetailsService.loadUserByAuthId(user.getAuthIdentity().getId());
        } catch (UsernameNotFoundException ex) {
            throw new BadRequestException("Selected " + userRoleLabel + " does not have an active authentication identity");
        } catch (ClassCastException ex) {
            throw new BadRequestException("Unable to resolve " + userRoleLabel + " permissions");
        }
    }

    @Transactional(readOnly = true)
    public List<SupervisorAssignmentResponse> getSupervisorAssignments(
            Long supervisorId, 
            Long therapistId, 
            Boolean active,
            String search,
            String requiredMeetingFrequency,
            AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");
        
        // PBAC: Permission-based access control with data scope
        boolean canManageAll = permissionChecker.hasPermission(requester,"USER_MANAGE");
        
        if (!canManageAll) {
            if (permissionChecker.hasPermission(requester,"CLIENT_VIEW_TEAM")) {
                // Supervisor can only see their own assignments
                if (supervisorId != null && !supervisorId.equals(currentUserService.requireCurrentUser(requester).getId())) {
                    throw new ForbiddenException("You can only view your own supervisor assignments");
                }
                supervisorId = currentUserService.requireCurrentUser(requester).getId();
            } else if (permissionChecker.hasPermission(requester,"CLIENT_VIEW_OWN")) {
                // Therapist can only see assignments where they are the therapist
                if (therapistId != null && !therapistId.equals(currentUserService.requireCurrentUser(requester).getId())) {
                    throw new ForbiddenException("You can only view your own assignments");
                }
                therapistId = currentUserService.requireCurrentUser(requester).getId();
            } else {
                throw new ForbiddenException("Insufficient permissions to view supervisor assignments");
            }
        }

        // Capture resolved IDs into effectively-final locals for use in lambdas
        final Long resolvedSupervisorId = supervisorId;
        final Long resolvedTherapistId = therapistId;

        List<SupervisorAssignment> assignments;
        
        if (resolvedSupervisorId != null && resolvedTherapistId != null) {
            // Get assignments for specific supervisor-therapist pair
            assignments = supervisorAssignmentRepository.findBySupervisorId(resolvedSupervisorId).stream()
                    .filter(a -> a.getTherapist().getId().equals(resolvedTherapistId))
                    .collect(Collectors.toList());
        } else if (resolvedSupervisorId != null) {
            assignments = supervisorAssignmentRepository.findBySupervisorId(resolvedSupervisorId);
        } else if (resolvedTherapistId != null) {
            assignments = supervisorAssignmentRepository.findByTherapistId(resolvedTherapistId);
        } else {
            // Get all assignments (admin only)
            assignments = supervisorAssignmentRepository.findAll();
        }

        // Filter by active status if specified
        if (active != null) {
            assignments = assignments.stream()
                    .filter(a -> a.getIsActive().equals(active))
                    .collect(Collectors.toList());
        }

        if (StringUtils.hasText(search)) {
            String searchTerm = search.trim().toLowerCase(Locale.ROOT);
            assignments = assignments.stream()
                    .filter(a -> containsIgnoreCase(a.getSupervisor().getFullName(), searchTerm)
                            || containsIgnoreCase(a.getSupervisor().getEmail(), searchTerm)
                            || containsIgnoreCase(
                                    a.getSupervisor().getAuthIdentity() != null
                                            ? a.getSupervisor().getAuthIdentity().getLoginIdentifier()
                                            : null,
                                    searchTerm)
                            || containsIgnoreCase(a.getTherapist().getFullName(), searchTerm)
                            || containsIgnoreCase(a.getTherapist().getEmail(), searchTerm)
                            || containsIgnoreCase(
                                    a.getTherapist().getAuthIdentity() != null
                                            ? a.getTherapist().getAuthIdentity().getLoginIdentifier()
                                            : null,
                                    searchTerm))
                    .collect(Collectors.toList());
        }

        if (StringUtils.hasText(requiredMeetingFrequency)) {
            String normalizedFrequency = requiredMeetingFrequency.trim().toUpperCase(Locale.ROOT).replace("-", "");
            assignments = assignments.stream()
                    .filter(a -> a.getRequiredMeetingFrequency() != null
                            && a.getRequiredMeetingFrequency().name().replace("_", "").equalsIgnoreCase(normalizedFrequency))
                    .collect(Collectors.toList());
        }

        Comparator<SupervisorAssignment> latestFirst = Comparator
                .comparing(SupervisorAssignment::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
                .thenComparing(SupervisorAssignment::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
                .thenComparing(SupervisorAssignment::getId, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed();

        return assignments.stream()
                .sorted(latestFirst)
                .map(this::toSupervisorAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupervisorAssignmentResponse getSupervisorAssignment(Long assignmentId, AuthPrincipal requester) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        SupervisorAssignment assignment = supervisorAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor assignment not found"));

        // PBAC: Permission-based access control with data scope
        boolean canManageAll = permissionChecker.hasPermission(requester,"USER_MANAGE");
        
        if (!canManageAll) {
            boolean canView = false;
            if (permissionChecker.hasPermission(requester,"CLIENT_VIEW_TEAM") && 
                assignment.getSupervisor().getId().equals(currentUserService.requireCurrentUser(requester).getId())) {
                canView = true;
            } else if (permissionChecker.hasPermission(requester,"CLIENT_VIEW_OWN") && 
                      assignment.getTherapist().getId().equals(currentUserService.requireCurrentUser(requester).getId())) {
                canView = true;
            }
            
            if (!canView) {
                throw new ForbiddenException("You can only view your own supervisor assignments");
            }
        }

        return toSupervisorAssignmentResponse(assignment);
    }

    @Transactional
    public SupervisorAssignmentResponse updateSupervisorAssignment(
            Long assignmentId,
            UpdateSupervisorAssignmentRequest request,
            AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can update supervisor assignments");

        SupervisorAssignment assignment = supervisorAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor assignment not found"));

        // Update fields if provided
        if (request.getAssignmentType() != null) {
            assignment.setAssignmentType(request.getAssignmentType());
        }
        if (request.getStartDate() != null) {
            assignment.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            assignment.setEndDate(request.getEndDate());
        }
        if (request.getRequiredMeetingFrequency() != null) {
            assignment.setRequiredMeetingFrequency(request.getRequiredMeetingFrequency());
        }
        if (request.getNotes() != null) {
            assignment.setNotes(request.getNotes());
        }
        if (request.getNextMeetingDate() != null) {
            assignment.setNextMeetingDate(request.getNextMeetingDate());
        }
        if (request.getLastMeetingDate() != null) {
            assignment.setLastMeetingDate(request.getLastMeetingDate());
        }

        // Optional explicit toggle:
        // - activate => clear past end-date so entity date-window rule can mark active
        // - deactivate => close assignment as of yesterday
        if (request.getIsActive() != null) {
            LocalDate today = LocalDate.now();
            if (Boolean.TRUE.equals(request.getIsActive())) {
                if (assignment.getEndDate() != null && assignment.getEndDate().isBefore(today)) {
                    assignment.setEndDate(null);
                }
            } else {
                if (assignment.getEndDate() == null || !assignment.getEndDate().isBefore(today)) {
                    assignment.setEndDate(today.minusDays(1));
                }
            }
        }

        SupervisorAssignment saved = supervisorAssignmentRepository.save(assignment);
        
        // Activity logging
        recordActivityEvent(currentUserService.requireCurrentUser(requester).getId(), "supervisor_assignment_updated", saved.getId(), ipAddress);

        if (notificationService != null) {
            try {
                notificationService.processEventInNewTransaction(
                        NotificationEventCatalog.SUPERVISOR_ASSIGNMENT_UPDATED,
                        buildSupervisorAssignmentEventData(saved));
            } catch (Exception e) {
                log.error("Failed to trigger supervisor_assignment_updated notification", e);
            }
        }

        log.info("Supervisor assignment updated: assignmentId={}", saved.getId());
        
        return toSupervisorAssignmentResponse(saved);
    }

    @Transactional
    public void deleteSupervisorAssignment(Long assignmentId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can delete supervisor assignments");

        SupervisorAssignment assignment = supervisorAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor assignment not found"));

        // Capture event data before the row is removed (associations needed for the message).
        Map<String, Object> eventData = buildSupervisorAssignmentEventData(assignment);

        supervisorAssignmentRepository.delete(assignment);
        
        // Activity logging
        recordActivityEvent(currentUserService.requireCurrentUser(requester).getId(), "supervisor_assignment_deleted", assignmentId, ipAddress);

        if (notificationService != null) {
            try {
                notificationService.processEventInNewTransaction(
                        NotificationEventCatalog.SUPERVISOR_ASSIGNMENT_DELETED, eventData);
            } catch (Exception e) {
                log.error("Failed to trigger supervisor_assignment_deleted notification", e);
            }
        }

        log.info("Supervisor assignment deleted: assignmentId={}", assignmentId);
    }

    private SupervisorAssignmentResponse toSupervisorAssignmentResponse(SupervisorAssignment assignment) {
        String supervisorLoginId = assignment.getSupervisor().getAuthIdentity() != null
                ? assignment.getSupervisor().getAuthIdentity().getLoginIdentifier() : assignment.getSupervisor().getEmail();
        String therapistLoginId = assignment.getTherapist().getAuthIdentity() != null
                ? assignment.getTherapist().getAuthIdentity().getLoginIdentifier() : assignment.getTherapist().getEmail();
        SupervisorAssignmentResponse.SupervisorTherapistInfo supervisorInfo = 
                SupervisorAssignmentResponse.SupervisorTherapistInfo.builder()
                        .id(assignment.getSupervisor().getId())
                        .username(supervisorLoginId)
                        .fullName(assignment.getSupervisor().getFullName())
                        .email(assignment.getSupervisor().getEmail())
                        .build();

        SupervisorAssignmentResponse.SupervisorTherapistInfo therapistInfo = 
                SupervisorAssignmentResponse.SupervisorTherapistInfo.builder()
                        .id(assignment.getTherapist().getId())
                        .username(therapistLoginId)
                        .fullName(assignment.getTherapist().getFullName())
                        .email(assignment.getTherapist().getEmail())
                        .build();

        return SupervisorAssignmentResponse.builder()
                .id(assignment.getId())
                .supervisor(supervisorInfo)
                .therapist(therapistInfo)
                .assignmentType(assignment.getAssignmentType())
                .startDate(assignment.getStartDate())
                .endDate(assignment.getEndDate())
                .assignedDate(assignment.getAssignedDate())
                .isActive(assignment.getIsActive())
                .notes(assignment.getNotes())
                .requiredMeetingFrequency(assignment.getRequiredMeetingFrequency())
                .nextMeetingDate(assignment.getNextMeetingDate())
                .lastMeetingDate(assignment.getLastMeetingDate())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }

    private boolean containsIgnoreCase(String value, String lowerNeedle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerNeedle);
    }

    private Map<String, Object> buildOrganizationUserEventData(User targetUser, User actor) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", targetUser.getId());
        data.put("entityType", "user");
        data.put("entityId", targetUser.getId());
        data.put("createdUserId", targetUser.getId());
        data.put("createdUserName", targetUser.getFullName());
        data.put("createdUserEmail", targetUser.getEmail());
        // Generic keys used by the updated/deleted templates.
        data.put("userId", targetUser.getId());
        data.put("userName", targetUser.getFullName());
        data.put("userEmail", targetUser.getEmail());
        data.put("actorUserId", actor != null ? actor.getId() : null);
        data.put("actorName", actor != null ? actor.getFullName() : null);
        data.put("organisationId", TenantContext.getOrganisationId());
        return data;
    }

    private Map<String, Object> buildSupervisorAssignmentEventData(SupervisorAssignment assignment) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", assignment.getId());
        data.put("entityType", "supervisor_assignment");
        data.put("entityId", assignment.getId());
        data.put("supervisorId", assignment.getSupervisor() != null ? assignment.getSupervisor().getId() : null);
        data.put("supervisorName", assignment.getSupervisor() != null ? assignment.getSupervisor().getFullName() : null);
        data.put("therapistId", assignment.getTherapist() != null ? assignment.getTherapist().getId() : null);
        data.put("therapistName", assignment.getTherapist() != null ? assignment.getTherapist().getFullName() : null);
        data.put("assignmentType", assignment.getAssignmentType() != null ? assignment.getAssignmentType().name() : null);
        return data;
    }
}
