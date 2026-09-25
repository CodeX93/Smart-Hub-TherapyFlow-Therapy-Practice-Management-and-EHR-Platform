package com.smart.therapy.flow.client.portal.service;

import com.smart.therapy.flow.auth.entity.AuditLog;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.billing.audit.BillingAuditActions;
import com.smart.therapy.flow.client.entity.Client;

import com.smart.therapy.flow.client.portal.util.PortalDocumentMapper;
import com.smart.therapy.flow.client.portal.dto.ForgotPasswordRequest;
import com.smart.therapy.flow.client.portal.dto.PortalActivateRequest;
import com.smart.therapy.flow.client.portal.dto.PortalActivateResponse;
import com.smart.therapy.flow.client.portal.dto.PortalActivationTokenValidationResponse;
import com.smart.therapy.flow.client.portal.dto.PortalClientResponse;
import com.smart.therapy.flow.client.portal.dto.OnlineBookingRequestResponse;
import com.smart.therapy.flow.client.entity.ClientPortalSettings;
import com.smart.therapy.flow.notification.enums.NotificationType;
import com.smart.therapy.flow.client.portal.dto.PortalLoginRequest;
import com.smart.therapy.flow.client.portal.dto.PortalLoginResponse;
import com.smart.therapy.flow.client.portal.dto.PortalAppointmentResponse;
import com.smart.therapy.flow.client.portal.dto.ResetPasswordRequest;
import com.smart.therapy.flow.client.portal.dto.CancelAppointmentResponse;
import com.smart.therapy.flow.client.portal.dto.RescheduleAppointmentRequest;
import com.smart.therapy.flow.client.portal.dto.RescheduleAppointmentResponse;

import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.repository.ClientContactRepository;
import com.smart.therapy.flow.client.service.ClientPortalSettingsService;
import com.smart.therapy.flow.client.service.ClientContactService;
import com.smart.therapy.flow.client.entity.ClientContact;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.service.AuthIdentityService;
import com.smart.therapy.flow.auth.service.AuthKnownDeviceService;
import com.smart.therapy.flow.auth.service.AuthRefreshTokenService;
import com.smart.therapy.flow.auth.service.AuthSessionService;
import com.smart.therapy.flow.auth.service.MfaService;
import com.smart.therapy.flow.auth.dto.MfaDtos;
import com.smart.therapy.flow.common.security.AuthIdentityDetailsService;
import com.smart.therapy.flow.client.enums.ContactType;
import com.smart.therapy.flow.client.enums.ConsentType;
import com.smart.therapy.flow.common.audit.HipaaAuditLabels;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.TenantUnavailableException;
import com.smart.therapy.flow.common.exception.ConflictException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.PortalAccessDisabledException;
import com.smart.therapy.flow.common.exception.PortalActivationPendingException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.exception.UnauthorizedException;
import com.smart.therapy.flow.organisation.entity.UserOrganisation;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import com.smart.therapy.flow.organisation.service.TenantSchemaHealthService;
import com.smart.therapy.flow.superadmin.service.TenantResolutionService;
import org.springframework.http.HttpStatus;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.TokenBlacklistService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.smart.therapy.flow.session.entity.Room;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.entity.SessionNote;
import com.smart.therapy.flow.session.entity.SessionIntegration;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.repository.SessionNoteRepository;
import com.smart.therapy.flow.session.repository.RoomRepository;
import com.smart.therapy.flow.user.entity.UserProfilePhysicalRoom;
import com.smart.therapy.flow.user.entity.ShiftMode;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.billing.repository.SessionBillingRepository;
import com.smart.therapy.flow.notification.entity.Notification;
import com.smart.therapy.flow.notification.repository.NotificationRepository;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.entity.TherapistBlockedTime;
import com.smart.therapy.flow.user.entity.UserProfileWorkingHours;
import com.smart.therapy.flow.user.service.WorkingHoursServiceMatcher;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import com.smart.therapy.flow.user.repository.TherapistBlockedTimeRepository;
import com.smart.therapy.flow.user.repository.SupervisorAssignmentRepository;
import com.smart.therapy.flow.user.entity.SupervisorAssignment;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.system.service.SystemOptionKeyMatcher;
import com.smart.therapy.flow.system.service.SystemOptionCategories;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import com.smart.therapy.flow.session.dto.ZoomMeetingResponse;
import com.smart.therapy.flow.session.service.SessionService;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.notification.service.NotificationEventCatalog;
import com.smart.therapy.flow.notification.service.NotificationPayloadFactory;
import com.smart.therapy.flow.audit.service.AuditEventFactory;
import com.smart.therapy.flow.notification.service.NotificationService;
import com.smart.therapy.flow.payment.service.StripeService;
import com.smart.therapy.flow.document.entity.Document;
import com.smart.therapy.flow.document.entity.FormAssignment;
import com.smart.therapy.flow.document.entity.FormAssignmentField;
import com.smart.therapy.flow.document.entity.FormField;
import com.smart.therapy.flow.document.entity.FormResponse;
import com.smart.therapy.flow.document.entity.FormSignature;
import com.smart.therapy.flow.document.entity.FormTemplate;
import com.smart.therapy.flow.document.entity.FormTemplateVersion;
import com.smart.therapy.flow.document.repository.DocumentRepository;
import com.smart.therapy.flow.document.repository.FormAssignmentFieldRepository;
import com.smart.therapy.flow.document.repository.FormAssignmentRepository;
import com.smart.therapy.flow.document.repository.FormFieldRepository;
import com.smart.therapy.flow.document.repository.FormResponseRepository;
import com.smart.therapy.flow.document.repository.FormSignatureRepository;
import com.smart.therapy.flow.document.repository.FormTemplateRepository;
import com.smart.therapy.flow.document.service.StorageService;
import com.smart.therapy.flow.task.entity.Task;
import com.smart.therapy.flow.task.enums.TaskStatus;
import com.smart.therapy.flow.task.repository.TaskRepository;
import com.smart.therapy.flow.client.entity.PatientConsent;
import com.smart.therapy.flow.client.repository.PatientConsentRepository;
import com.smart.therapy.flow.client.service.ConsentCommandService;
import com.smart.therapy.flow.document.dto.FormAssignmentResponse;
import com.smart.therapy.flow.document.dto.FormResponseDto;
import com.smart.therapy.flow.document.enums.SignatureType;
import com.smart.therapy.flow.document.service.FormSubmissionValidator;
import com.smart.therapy.flow.document.dto.FormSignatureResponse;
import com.smart.therapy.flow.document.dto.FormFieldResponse;
import com.smart.therapy.flow.document.dto.FormFieldOptionResponse;
import com.smart.therapy.flow.document.enums.DocumentType;
import com.smart.therapy.flow.document.enums.DocumentCategory;
import com.smart.therapy.flow.document.enums.ReviewStatus;
import com.smart.therapy.flow.document.enums.Status;
import com.smart.therapy.flow.document.enums.FromCategory;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.net.URI;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientPortalService {

    private final ClientRepository clientRepository;
    private final CurrentUserService currentUserService;

    private final AuditLogService auditLogService;
    private final ClientPortalSettingsService portalSettingsService;
    private final AuthIdentityRepository authIdentityRepository;
    private final AuthIdentityRoleRepository authIdentityRoleRepository;
    private final UserOrganisationRepository userOrganisationRepository;
    private final AuthIdentityService authIdentityService;
    private final AuthSessionService authSessionService;
    private final AuthIdentityDetailsService authIdentityDetailsService;
    private final ClientContactService contactService;
    private final com.smart.therapy.flow.client.repository.ClientContactRepository contactRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuthRefreshTokenService authRefreshTokenService;
    private final MfaService mfaService;
    private final AuthKnownDeviceService authKnownDeviceService;
    private final EmailService emailService;
    private final SessionRepository sessionRepositoryForAppointments;
    private final SessionNoteRepository sessionNoteRepository;
    private final ServiceRepository serviceRepository;
    private final RoomRepository roomRepository;
    private final NotificationRepository notificationRepository;
    private final SessionBillingRepository sessionBillingRepository;
    private final UserProfileRepository userProfileRepository;
    private final TherapistBlockedTimeRepository therapistBlockedTimeRepository;
    private final UserRepository userRepository;
    private final SupervisorAssignmentRepository supervisorAssignmentRepository;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final TenantDirectoryService tenantDirectoryService;
    private final TenantSchemaHealthService tenantSchemaHealthService;
    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final TenantResolutionService tenantResolutionService;
    private final SystemOptionResolverService systemOptionResolverService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private StripeService stripeService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.smart.therapy.flow.billing.service.BillingService billingService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private SessionService.ZoomService zoomService;


    private final DocumentRepository documentRepository;
    private final FormAssignmentRepository formAssignmentRepository;
    private final FormAssignmentFieldRepository formAssignmentFieldRepository;
    private final FormTemplateRepository formTemplateRepository;
    private final FormFieldRepository formFieldRepository;
    private final FormResponseRepository formResponseRepository;
    private final FormSignatureRepository formSignatureRepository;
    private final PatientConsentRepository patientConsentRepository;
    private final ConsentCommandService consentCommandService;
    private final NotificationPayloadFactory notificationPayloadFactory;
    private final AuditEventFactory auditEventFactory;
    private final TaskRepository taskRepository;
    private final com.smart.therapy.flow.common.service.TimezoneService timezoneService;

    @PersistenceContext
    private EntityManager entityManager;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private StorageService storageService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${document.review.default-days:7}")
    private long defaultReviewDays;

    @Value("${app.frontend.staff-login-url:https://app.therapyflow.pro/auth/therapist/login}")
    private String staffLoginUrl;

    /**
     * Where a therapist connects Zoom. The setup lives in a modal with no route of its own, so
     * this points at the dashboard until the frontend exposes a deep link - at which point it is
     * a config change, not a code one.
     */
    @Value("${app.frontend.therapist-zoom-setup-url:https://app.therapyflow.pro/therapist/dashboard}")
    private String zoomSetupUrl;

    // Formatters will be created dynamically based on client timezone
    private static final DateTimeFormatter DATE_FORMATTER_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER_PATTERN = DateTimeFormatter.ofPattern("HH:mm");

    // Constants
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MIN_DELAY_MS = 50;
    private static final int MAX_DELAY_RANDOM_MS = 100;
    private static final int DEFAULT_APPOINTMENTS_PAGE = 1;
    private static final int DEFAULT_APPOINTMENTS_PAGE_SIZE = 20;
    private static final int MAX_APPOINTMENTS_PAGE_SIZE = 100;

    /**
     * One nudge per client per week. Long enough that the therapist has time to act, short
     * enough that the client can raise it again the following week.
     */
    private static final java.time.Duration ONLINE_BOOKING_REQUEST_COOLDOWN = java.time.Duration.ofDays(7);

    /** Client-facing copy: never names the vendor, so it stays true if the provider changes. */
    private static final String ONLINE_BOOKING_UNAVAILABLE_MESSAGE =
            "Your therapist hasn't set up video meetings yet. Choose in-person, or contact the clinic.";
    private static final int MAX_NOTIFICATIONS_LIMIT = 50;

    @Transactional
    public PortalLoginResponse login(PortalLoginRequest request, String ipAddress, String userAgent) {
        Objects.requireNonNull(request, "Login request is required");
        boolean publicSchemaRequest = isPublicSchemaContext();

        if (!StringUtils.hasText(request.getEmail()) || !StringUtils.hasText(request.getPassword())) {
            recordAuditEvent(null, request.getEmail() != null ? request.getEmail() : "unknown",
                    "login_failed", ipAddress, userAgent, "failure",
                    "Missing credentials", true);
            throw new BadRequestException("Email and password are required");
        }

        String normalizedEmail = AuthIdentityService.normaliseLoginIdentifier(request.getEmail());
        Long targetOrganisationId = resolveTargetOrganisationId(
                request.getOrgId(),
                request.getOrgSlug(),
                request.getOrgIdentifier(),
                request.getOrgValue()
        );
        if (publicSchemaRequest && targetOrganisationId == null) {
            resolveTenantContextForPortalLogin(request, normalizedEmail);
        }
        Long lookupOrganisationId = targetOrganisationId != null
                ? targetOrganisationId
                : TenantContext.getOrganisationId();

        if (publicSchemaRequest && lookupOrganisationId != null) {
            String schema = resolveTenantSchema(lookupOrganisationId);
            if (!StringUtils.hasText(schema) || "public".equalsIgnoreCase(schema)) {
                throw new UnauthorizedException("Client tenant is unavailable");
            }
            // Changing TenantContext cannot retarget an already-open public Hibernate
            // session. Start authentication on a fresh tenant transaction and thread.
            try {
                return runPortalAuthenticationIsolated(lookupOrganisationId, schema,
                        () -> login(request, ipAddress, userAgent));
            } finally {
                TenantContext.clear();
                TenantContext.setSchemaName("public");
            }
        }

        AuthIdentity identity;
        if (publicSchemaRequest || lookupOrganisationId != null) {
            identity = resolveClientIdentityForLogin(normalizedEmail, lookupOrganisationId);
        } else {
            AuthIdentity found = authIdentityService.getByNormalisedLogin(normalizedEmail);
            identity = found != null
                    && found.getIdentityType() == IdentityType.CLIENT
                    && Boolean.TRUE.equals(found.getIsActive())
                    ? found
                    : null;
        }
        if (identity == null) {
            log.debug("Portal login failed: no client identity found");
            recordAuditEvent(null, normalizedEmail, "login_failed", ipAddress, userAgent,
                    "failure", "Invalid email or password", true);
            throw new UnauthorizedException("Invalid email or password");
        }

        Long effectiveOrganisationId = lookupOrganisationId;
        if (effectiveOrganisationId == null && identity.getOrganisation() != null) {
            effectiveOrganisationId = identity.getOrganisation().getId();
        }
        final Long organisationIdForSession = effectiveOrganisationId;
        Client client = resolveClientInTenantContext(identity.getId(), organisationIdForSession, publicSchemaRequest);
        if (client == null) {
            log.debug("Portal login failed: no client linked to authId={}", identity.getId());
            recordAuditEvent(null, normalizedEmail, "login_failed", ipAddress, userAgent,
                    "failure", "Invalid email or password", true);
            throw new UnauthorizedException("Invalid email or password");
        }

        // Check portal settings: has access and activated
        if (!hasPortalAccessInTenantContext(client.getId(), effectiveOrganisationId, publicSchemaRequest)) {
            log.debug("Portal login failed: Portal access disabled for client ID: {}", client.getId());
            recordAuditEvent(client.getId(), normalizedEmail, "login_failed", ipAddress, userAgent,
                    "failure", "Portal access disabled", true);
            throw new PortalAccessDisabledException();
        }
        if (!isPortalActivatedInTenantContext(client.getId(), effectiveOrganisationId, publicSchemaRequest)) {
            log.debug("Portal login failed: Portal not activated for client ID: {}", client.getId());
            recordAuditEvent(client.getId(), normalizedEmail, "login_failed", ipAddress, userAgent,
                    "failure", "Portal activation pending", true);
            throw new PortalActivationPendingException();
        }

        if (authIdentityService.isLocked(identity)) {
            recordAuditEvent(client.getId(), normalizedEmail, "login_failed", ipAddress, userAgent,
                    "failure", "Account locked", true);
            throw new UnauthorizedException("Account temporarily locked. Try again later.");
        }

        AuthIdentity identityForPasswordCheck = authIdentityService.getById(identity.getId());
        if (identityForPasswordCheck == null
                || !authIdentityService.validatePassword(identityForPasswordCheck, request.getPassword())) {
            authIdentityService.recordFailedLogin(identity.getId());
            recordAuditEvent(client.getId(), normalizedEmail, "login_failed", ipAddress, userAgent,
                    "failure", "Invalid password", true);
            throw new UnauthorizedException("Invalid email or password");
        }

        // Public portal login must bind the resolved clinic before loading permissions
        // or issuing MFA tokens, which capture TenantContext in their claims.
        ensureTenantContextForOrganisation(organisationIdForSession);
        AuthPrincipal authPrincipal = (AuthPrincipal) authIdentityDetailsService.loadUserByAuthId(identity.getId());
        boolean mfaEnabled = mfaService.isEnabled(identity.getId());
        boolean mfaEnrollmentRequired = mfaService.isEnrollmentRequired(
                authPrincipal.getAuthorities(), mfaEnabled);
        boolean staySignedIn = Boolean.TRUE.equals(request.getStaySignedIn());
        boolean trustedDeviceLogin = mfaEnabled
                && authKnownDeviceService.isTrustedDevice(identity.getId(), request.getDeviceTrustToken());
        if (mfaEnabled && !trustedDeviceLogin) {
            String challengeToken = mfaService.createLoginChallenge(identityForPasswordCheck);
            MfaDtos.LoginChallengeInfo challengeInfo = mfaService.getLoginChallengeInfo(identity.getId());
            SecurityContextHolder.clearContext();
            return PortalLoginResponse.builder()
                    .mfaRequired(true)
                    .mfaEnrollmentRequired(false)
                    .mfaChallengeToken(challengeToken)
                    .mfaMethod(challengeInfo.method().name())
                    .mfaMaskedDestination(challengeInfo.maskedDestination())
                    .mfaMethods(challengeInfo.enrolledMethods())
                    .mfaSmsAvailable(challengeInfo.smsAvailable())
                    .mfaEmailAvailable(challengeInfo.emailAvailable())
                    .message("MFA verification is required.")
                    .build();
        }
        if (mfaEnrollmentRequired) {
            MfaDtos.LoginChallengeInfo challengeInfo = mfaService.getLoginChallengeInfo(identity.getId());
            SecurityContextHolder.clearContext();
            return PortalLoginResponse.builder()
                    .mfaRequired(false)
                    .mfaEnrollmentRequired(true)
                    .mfaChallengeToken(mfaService.createEnrollmentChallenge(identity.getId()))
                    .mfaMethods(List.of())
                    .mfaSmsAvailable(challengeInfo.smsAvailable())
                    .mfaEmailAvailable(challengeInfo.emailAvailable())
                    .message("MFA enrollment is required before login.")
                    .build();
        }

        if (trustedDeviceLogin) {
            authKnownDeviceService.touchTrustedDevice(
                    identity.getId(), request.getDeviceTrustToken(), ipAddress, userAgent);
            recordAuditEvent(client.getId(), normalizedEmail, "mfa_skipped_trusted_device",
                    ipAddress, userAgent, "success", "MFA skipped for trusted device", true);
        }

        authIdentityService.recordSuccessfulLogin(identity.getId());
        authKnownDeviceService.recordSuccessfulLogin(identity.getId(), ipAddress, userAgent);

        Long refreshExpirationMs = staySignedIn
                ? authKnownDeviceService.staySignedInRefreshExpirationMs()
                : null;
        PortalAuthTokens tokens = executeWithTenantContextForPublicRequest(
                publicSchemaRequest,
                organisationIdForSession,
                () -> issuePortalAuthTokens(
                        identity.getId(), organisationIdForSession, ipAddress, userAgent, refreshExpirationMs)
        );
        SecurityContextHolder.getContext().setAuthentication(tokens.authentication());

        PortalClientResponse portalClient = buildPortalClientResponse(
                client, organisationIdForSession, identity.getLoginIdentifier());
        recordAuditEvent(client.getId(), normalizedEmail, "login", ipAddress, userAgent,
                "success", "Portal login successful", true);

        return PortalLoginResponse.builder()
                .client(portalClient)
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .tokenType("Bearer")
                .expiresIn(tokens.expiresIn())
                .mfaSkippedTrustedDevice(trustedDeviceLogin)
                .staySignedIn(staySignedIn)
                .build();
    }

    @Transactional(noRollbackFor = {UnauthorizedException.class, StoryApiException.class})
    public PortalLoginResponse verifyMfaLogin(
            MfaDtos.LoginVerifyRequest request,
            String ipAddress,
            String userAgent) {
        Objects.requireNonNull(request, "MFA verify request is required");
        String previousSchema = TenantContext.getSchemaName();
        Long previousOrg = TenantContext.getOrganisationId();
        try {
            if (isPublicSchemaContext() && jwtTokenProvider.isMfaChallengeToken(request.mfaChallengeToken())) {
                restoreTenantFromMfaChallenge(new MfaService.VerifiedChallenge(
                        jwtTokenProvider.getAuthIdFromToken(request.mfaChallengeToken()),
                        jwtTokenProvider.getTenantSchemaFromToken(request.mfaChallengeToken()),
                        jwtTokenProvider.getOrganisationIdFromToken(request.mfaChallengeToken())));
                if (!isPublicSchemaContext()) {
                    return runPortalAuthenticationIsolated(TenantContext.getOrganisationId(),
                            TenantContext.getSchemaName(), () -> verifyMfaLogin(request, ipAddress, userAgent));
                }
            }
            MfaService.VerifiedChallenge challenge = mfaService.verifyLoginChallenge(
                    request.mfaChallengeToken(),
                    request.code(),
                    request.method());
            restoreTenantFromMfaChallenge(challenge);

            AuthIdentity identity = authIdentityService.getById(challenge.authId());
            if (identity == null
                    || identity.getIdentityType() != IdentityType.CLIENT
                    || !Boolean.TRUE.equals(identity.getIsActive())) {
                throw new UnauthorizedException("Invalid email or password");
            }

            Long resolvedOrganisationId = challenge.organisationId();
            if (resolvedOrganisationId == null && identity.getOrganisation() != null) {
                resolvedOrganisationId = identity.getOrganisation().getId();
            }
            final Long organisationIdForSession = resolvedOrganisationId;
            boolean publicSchemaRequest = isPublicSchemaContext();
            Client client = resolveClientInTenantContext(
                    identity.getId(), organisationIdForSession, publicSchemaRequest);
            if (client == null) {
                throw new UnauthorizedException("Invalid email or password");
            }
            if (!hasPortalAccessInTenantContext(client.getId(), organisationIdForSession, publicSchemaRequest)) {
                throw new PortalAccessDisabledException();
            }
            if (!isPortalActivatedInTenantContext(client.getId(), organisationIdForSession, publicSchemaRequest)) {
                throw new PortalActivationPendingException();
            }

            authIdentityService.recordSuccessfulLogin(identity.getId());
            boolean trustRequested = Boolean.TRUE.equals(request.trustDevice());
            boolean stay = Boolean.TRUE.equals(request.staySignedIn());
            String deviceTrustToken = null;
            if (trustRequested) {
                deviceTrustToken = authKnownDeviceService.trustCurrentDevice(
                        identity.getId(), ipAddress, userAgent);
                recordAuditEvent(client.getId(), identity.getLoginIdentifier(), "device_trusted",
                        ipAddress, userAgent, "success", "Portal device trusted after MFA", true);
            }
            authKnownDeviceService.recordSuccessfulLogin(identity.getId(), ipAddress, userAgent);

            Long refreshExpirationMs = stay
                    ? authKnownDeviceService.staySignedInRefreshExpirationMs()
                    : null;
            PortalAuthTokens tokens = executeWithTenantContextForPublicRequest(
                    publicSchemaRequest,
                    organisationIdForSession,
                    () -> issuePortalAuthTokens(
                            identity.getId(), organisationIdForSession, ipAddress, userAgent, refreshExpirationMs));
            SecurityContextHolder.getContext().setAuthentication(tokens.authentication());

            String normalizedEmail = identity.getLoginIdentifier();
            PortalClientResponse portalClient = buildPortalClientResponse(
                    client, organisationIdForSession, normalizedEmail);
            recordAuditEvent(client.getId(), normalizedEmail, "login", ipAddress, userAgent,
                    "success", "Portal login successful after MFA", true);

            return PortalLoginResponse.builder()
                    .client(portalClient)
                    .accessToken(tokens.accessToken())
                    .refreshToken(tokens.refreshToken())
                    .tokenType("Bearer")
                    .expiresIn(tokens.expiresIn())
                    .mfaRequired(false)
                    .mfaEnrollmentRequired(false)
                    .deviceTrustToken(deviceTrustToken)
                    .staySignedIn(stay)
                    .build();
        } finally {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
            if (previousSchema != null && !previousSchema.isBlank()) {
                TenantContext.setSchemaName(previousSchema);
            }
            if (previousOrg != null) {
                TenantContext.setOrganisationId(previousOrg);
            }
        }
    }

    private void restoreTenantFromMfaChallenge(MfaService.VerifiedChallenge challenge) {
        String schema = challenge.tenantSchema();
        if (!StringUtils.hasText(schema) || "public".equalsIgnoreCase(schema)) {
            TenantContext.setSchemaName("public");
            TenantContext.setOrganisationId(null);
            return;
        }
        tenantDirectoryService.findBySchemaName(schema).ifPresentOrElse(info -> {
            if (challenge.organisationId() != null
                    && !challenge.organisationId().equals(info.getOrganisationId())) {
                throw new UnauthorizedException("MFA challenge organisation does not match tenant");
            }
            if (!info.isActive() || info.isForceDisabled()) {
                throw new TenantUnavailableException("Tenant is unavailable", null);
            }
            TenantContext.setSchemaName(info.getSchemaName());
            TenantContext.setOrganisationId(info.getOrganisationId());
        }, () -> {
            throw new UnauthorizedException("MFA challenge tenant is invalid");
        });
    }

    private record PortalAuthTokens(
            String accessToken,
            String refreshToken,
            long expiresIn,
            Authentication authentication
    ) {}

    private PortalAuthTokens issuePortalAuthTokens(
            Long authId,
            Long organisationId,
            String ipAddress,
            String userAgent
    ) {
        return issuePortalAuthTokens(authId, organisationId, ipAddress, userAgent, null);
    }

    /**
     * Issue portal JWTs with a registered auth session (required by JwtAuthenticationFilter).
     */
    private PortalAuthTokens issuePortalAuthTokens(
            Long authId,
            Long organisationId,
            String ipAddress,
            String userAgent,
            Long refreshExpirationMs
    ) {
        ensureTenantContextForOrganisation(organisationId);
        AuthPrincipal authPrincipal = (AuthPrincipal) authIdentityDetailsService.loadUserByAuthId(authId);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                authPrincipal, null, authPrincipal.getAuthorities());

        String jti = AuthSessionService.generateJti();
        String accessToken = jwtTokenProvider.generateToken(authentication, jti);
        Instant expiresAt = jwtTokenProvider.getExpirationDateFromToken(accessToken).toInstant();
        authSessionService.createSession(authId, jti, expiresAt, ipAddress, userAgent);

        String refreshToken = refreshExpirationMs != null && refreshExpirationMs > 0
                ? authRefreshTokenService.issueRefreshToken(authId, ipAddress, userAgent, refreshExpirationMs)
                : authRefreshTokenService.issueRefreshToken(authId, ipAddress, userAgent);
        long expiresIn = jwtTokenProvider.getExpirationDateFromToken(accessToken).getTime() / 1000
                - Instant.now().getEpochSecond();

        return new PortalAuthTokens(accessToken, refreshToken, expiresIn, authentication);
    }

    /**
     * Helper method to get current client ID from SecurityContext (AuthPrincipal only).
     */
    private Long getCurrentClientId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthorizedException("Not authenticated as a client");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthPrincipal authPrincipal) {
            return currentUserService.requireCurrentClient(authPrincipal).getId();
        }
        throw new UnauthorizedException("Not authenticated as a client");
    }

    /**
     * Helper method to get current client from SecurityContext
     * Uses findByIdWithTherapist to eagerly load the therapist relationship
     */
    private Client getCurrentClientEntity() {
        Long clientId = getCurrentClientId();
        return clientRepository.findByIdWithTherapist(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
    }

    @Transactional(readOnly = true)
    public PortalClientResponse getCurrentClient() {
        Client client = getCurrentClientEntity();
        String avatarUrl = portalSettingsService.findByClientId(client.getId())
                .map(com.smart.therapy.flow.client.entity.ClientPortalSettings::getAvatarUrl)
                .orElse(null);

        return PortalClientResponse.builder()
                .id(client.getId())
                .clientId(client.getClientId())
                .fullName(client.getFullName())
                .email(getClientEmail(client))
                .phone(getClientPhone(client))
                .assignedTherapistId(
                        client.getAssignedTherapist() != null ? client.getAssignedTherapist().getId() : null)
                .timezone(client.getTimezone())
                .avatarUrl(avatarUrl)
                .onlineBookingAvailable(isOnlineBookingAvailable(client))
                .onlineBookingRequestedAt(portalSettingsService.findByClientId(client.getId())
                        .map(ClientPortalSettings::getOnlineBookingRequestedAt)
                        .orElse(null))
                .build();
    }

    @Transactional(readOnly = true)
    public String getClientTimezone() {
        Client client = getCurrentClientEntity();
        return client.getTimezone();
    }

    @Transactional
    public PortalClientResponse updateClientTimezone(String timezone, String ipAddress) {
        Client client = getCurrentClientEntity();

        // Validate timezone
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new BadRequestException("Invalid timezone format: " + timezone);
        }

        client.setTimezone(timezone);
        client.setLastUpdateDate(Instant.now());
        Client saved = clientRepository.save(client);

        // Record audit event
        recordAuditEvent(
                client.getId(),
                getClientEmail(client),
                "client_timezone_updated",
                ipAddress,
                null, // userAgent
                "success",
                "Timezone updated to: " + timezone,
                true // portal
        );

        log.info("Client {} timezone updated to {}", client.getId(), timezone);

        return PortalClientResponse.builder()
                .id(saved.getId())
                .clientId(saved.getClientId())
                .fullName(saved.getFullName())
                .email(getClientEmail(saved))
                .phone(getClientPhone(saved))
                .assignedTherapistId(saved.getAssignedTherapist() != null ? saved.getAssignedTherapist().getId() : null)
                .timezone(saved.getTimezone())
                .build();
    }

    private static final long MAX_AVATAR_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    /**
     * Upload avatar for the current client. Returns the avatar URL for the uploaded file.
     */
    @Transactional
    public String uploadAvatar(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Avatar file is required");
        }
        if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new BadRequestException("Avatar file size must not exceed 5 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Avatar must be image/jpeg, image/png, image/gif, or image/webp");
        }
        if (storageService == null) {
            throw new BadRequestException("File storage is not configured");
        }
        Client client = getCurrentClientEntity();
        String ext = getFileExtension(file.getOriginalFilename());
        String fileName = "avatar_" + System.currentTimeMillis() + ext;
        try {
            String path = storageService.uploadFile(file, String.valueOf(client.getId()), fileName);
            String url = storageService.getFileUrl(path);
            portalSettingsService.setAvatarUrl(client.getId(), url);
            return url;
        } catch (Exception e) {
            log.warn("Avatar upload failed for client {}", client.getId(), e);
            throw new BadRequestException("Failed to upload avatar: " + (e.getMessage() != null ? e.getMessage() : "unknown error"));
        }
    }

    private static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }

    @Transactional
    public void logout(String jwtToken, String refreshToken, String ipAddress, String userAgent) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthorizedException("Not authenticated as a client");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthPrincipal)) {
            throw new UnauthorizedException("Not authenticated as a client");
        }
        AuthPrincipal authPrincipal = (AuthPrincipal) principal;
        Long clientId = currentUserService.requireCurrentClient(authPrincipal).getId();
        String clientEmail = authPrincipal.getLoginIdentifier();

        if (StringUtils.hasText(jwtToken)) {
            String token = jwtToken.startsWith("Bearer ") ? jwtToken.substring(7) : jwtToken;
            String jti = jwtTokenProvider.getJtiFromToken(token);
            if (jti != null) {
                authSessionService.revokeByJwtId(jti);
            }
            tokenBlacklistService.blacklistToken(token);
            recordAuditEvent(clientId, clientEmail != null ? clientEmail : "client", "logout", ipAddress, userAgent,
                    "success", "Portal logout successful", true);
        }

        if (StringUtils.hasText(refreshToken)) {
            try {
                authRefreshTokenService.revokePresentedToken(refreshToken);
                tokenBlacklistService.blacklistToken(refreshToken);
            } catch (Exception e) {
                log.debug("Revoke refresh token failed", e);
            }
        }
    }

    @Transactional(readOnly = true)
    public PortalActivationTokenValidationResponse validateActivationToken(String token) {
        AuthIdentity identity = authIdentityService.validateEmailVerificationToken(token);
        if (identity == null) {
            return PortalActivationTokenValidationResponse.builder()
                    .valid(false)
                    .message("This activation link is no longer valid. It may have already been used, expired, "
                            + "or replaced by a newer activation email.")
                    .build();
        }
        if (identity.getIdentityType() != IdentityType.CLIENT) {
            return PortalActivationTokenValidationResponse.builder()
                    .valid(false)
                    .message("Invalid activation token")
                    .build();
        }
        return PortalActivationTokenValidationResponse.builder()
                .valid(true)
                .message(null)
                .build();
    }

    @Transactional
    public PortalActivateResponse activate(PortalActivateRequest request) {
        Objects.requireNonNull(request, "Activate request is required");
        boolean publicSchemaRequest = isPublicSchemaContext();

        if (!StringUtils.hasText(request.getToken()) || !StringUtils.hasText(request.getPassword())) {
            throw new BadRequestException("Activation token and password are required");
        }

        if (request.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new BadRequestException("Password must be at least 8 characters long");
        }

        String activationToken = AuthIdentityService.normalizeOpaqueToken(request.getToken());
        AuthIdentity identity = authIdentityService.activateClientPortalAccount(
                activationToken, request.getPassword());
        if (identity == null) {
            throw new BadRequestException(
                    "This activation link is no longer valid. It may have already been used, expired, "
                            + "or replaced by a newer activation email. Sign in if you already activated, "
                            + "use Forgot password, or ask your clinic to resend portal access.");
        }
        if (identity.getIdentityType() != IdentityType.CLIENT) {
            throw new BadRequestException("Invalid activation token");
        }
        if (!authIdentityService.validatePassword(identity, request.getPassword())) {
            throw new BadRequestException("Failed to save portal password. Please try again or use Forgot password.");
        }

        authIdentityService.recordSuccessfulLogin(identity.getId());
        Long effectiveOrganisationId = identity.getOrganisation() != null
                ? identity.getOrganisation().getId()
                : TenantContext.getOrganisationId();
        Client client = resolveClientInTenantContext(identity.getId(), effectiveOrganisationId, publicSchemaRequest);
        if (client == null) {
            throw new ResourceNotFoundException("Client not found for activation token");
        }
        if (!hasPortalAccessInTenantContext(client.getId(), effectiveOrganisationId, publicSchemaRequest)) {
            throw new PortalAccessDisabledException();
        }
        setActivatedInTenantContext(client.getId(), true, Instant.now(), effectiveOrganisationId, publicSchemaRequest);

        PortalAuthTokens tokens = executeWithTenantContextForPublicRequest(
                publicSchemaRequest,
                effectiveOrganisationId,
                () -> issuePortalAuthTokens(identity.getId(), effectiveOrganisationId, null, null)
        );
        SecurityContextHolder.getContext().setAuthentication(tokens.authentication());

        PortalClientResponse portalClient = buildPortalClientResponse(
                client, effectiveOrganisationId, identity.getLoginIdentifier());

        return PortalActivateResponse.builder()
                .message("Account activated successfully")
                .client(portalClient)
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .tokenType("Bearer")
                .expiresIn(tokens.expiresIn())
                .build();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        Objects.requireNonNull(request, "Request is required");

        if (!StringUtils.hasText(request.getEmail())) {
            throw new BadRequestException("Email is required");
        }

        String normalizedEmail = AuthIdentityService.normaliseLoginIdentifier(request.getEmail());
        Long targetOrganisationId = resolveTargetOrganisationIdForForgotPassword(request);
        AuthIdentity identity = resolveClientIdentityForForgotPassword(normalizedEmail, targetOrganisationId);
        if (identity != null) {
            try {
                Long effectiveOrganisationId = targetOrganisationId != null
                        ? targetOrganisationId
                        : identity.getOrganisation() != null ? identity.getOrganisation().getId() : null;
                Client client = resolveClientForForgotPassword(identity.getId(), effectiveOrganisationId);
                if (client != null && hasPortalAccessForForgotPassword(client.getId(), effectiveOrganisationId)) {
                    String resetToken = authIdentityService.generatePasswordResetTokenValue();
                    java.time.Instant expiry = Instant.now().plusSeconds(24 * 3600);
                    authIdentityService.setPasswordResetToken(identity.getId(), resetToken, expiry);

                    String emailToUse = identity.getLoginIdentifier();
                    if (StringUtils.hasText(emailToUse)) {
                        emailService.sendPasswordResetEmail(emailToUse, client.getFullName(), resetToken);
                        log.info("Portal password reset email sent for authId={}", identity.getId());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to send password reset email", e);
            }
        }

        try {
            Thread.sleep(MIN_DELAY_MS + secureRandom.nextInt(MAX_DELAY_RANDOM_MS));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private PortalLoginResponse runPortalAuthenticationIsolated(
            Long organisationId, String schema, Supplier<PortalLoginResponse> work) {
        return tenantTransactionExecutor.executeWriteIsolated(organisationId, schema, () -> {
            try {
                return work.get();
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
    }

    private boolean isPublicSchemaContext() {
        String schema = TenantContext.getSchemaName();
        return schema == null || schema.isBlank() || "public".equalsIgnoreCase(schema);
    }

    private void resolveTenantContextForPortalLogin(PortalLoginRequest request, String normalizedEmail) {
        if (!isPublicSchemaContext() || request == null) {
            return;
        }

        String selectedIdentifier = normalizeIdentifier(
                request.getOrgId(),
                request.getOrgSlug(),
                request.getOrgIdentifier(),
                request.getOrgValue()
        );
        if (selectedIdentifier != null) {
            var info = tenantDirectoryService.findByOrganisationIdOrSlug(selectedIdentifier)
                    .orElseThrow(() -> new StoryApiException(
                            HttpStatus.BAD_REQUEST, "INVALID_ORG_SELECTION", "Organisation selection not found"));
            if (StringUtils.hasText(normalizedEmail)
                    && !tenantResolutionService.emailBelongsToOrganisation(
                            normalizedEmail, info.getOrganisationId(), IdentityType.CLIENT)) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "EMAIL_NOT_IN_ORG", "Email does not belong to organisation");
            }
            if (StringUtils.hasText(normalizedEmail)
                    && tenantResolutionService.isEmailBlockedInOrganisation(normalizedEmail, info.getOrganisationId())) {
                throw new StoryApiException(HttpStatus.FORBIDDEN, "USER_BLOCKED_IN_ORG", "User is blocked for this organisation");
            }
            setTenantContext(info);
            return;
        }

        // Simple client login: resolve tenant from the CLIENT auth identity for this email.
        List<AuthIdentity> candidates = authIdentityRepository
                .findAllByEmailOrUsername(normalizedEmail)
                .stream()
                .filter(identity -> identity.getIdentityType() == IdentityType.CLIENT)
                .filter(identity -> Boolean.TRUE.equals(identity.getIsActive()))
                .toList();
        if (candidates.isEmpty()) {
            return;
        }
        if (candidates.size() > 1) {
            throw new StoryApiException(HttpStatus.CONFLICT, "TENANT_SELECTION_REQUIRED",
                    "Multiple organisations found for email");
        }
        AuthIdentity identity = candidates.get(0);
        Long organisationId = resolveOrganisationIdFromIdentity(identity);
        if (organisationId != null) {
            if (tenantResolutionService.isEmailBlockedInOrganisation(normalizedEmail, organisationId)) {
                throw new StoryApiException(HttpStatus.FORBIDDEN, "USER_BLOCKED_IN_ORG", "User is blocked for this organisation");
            }
            tenantDirectoryService.findByOrganisationId(organisationId).ifPresent(this::setTenantContext);
        }
    }

    private void ensureTenantContextForOrganisation(Long organisationId) {
        if (organisationId == null) {
            return;
        }
        if (!isPublicSchemaContext()) {
            return;
        }
        tenantDirectoryService.findByOrganisationId(organisationId).ifPresent(this::setTenantContext);
    }

    private void setTenantContext(TenantDirectoryService.TenantInfo info) {
        if (info == null) {
            return;
        }
        TenantContext.setSchemaName(info.getSchemaName());
        TenantContext.setOrganisationId(info.getOrganisationId());
    }

    private boolean isPortalActivatedForLogin(Long clientId, Long organisationId) {
        if (organisationId != null) {
            return Boolean.TRUE.equals(executeTenantReadOnly(
                    organisationId,
                    () -> portalSettingsService.isActivated(clientId)
            ));
        }
        if (!isPublicSchemaContext()) {
            return portalSettingsService.isActivated(clientId);
        }
        return false;
    }

    private Long resolveTargetOrganisationIdForForgotPassword(ForgotPasswordRequest request) {
        return resolveTargetOrganisationId(
                request.getOrgId(),
                request.getOrgSlug(),
                request.getOrgIdentifier(),
                request.getOrgValue()
        );
    }

    private Long resolveTargetOrganisationId(String orgId, String orgSlug, String orgIdentifier, String orgValue) {
        if (!isPublicSchemaContext()) {
            return TenantContext.getOrganisationId();
        }
        String selectedIdentifier = normalizeIdentifier(orgId, orgSlug, orgIdentifier, orgValue);
        if (!StringUtils.hasText(selectedIdentifier)) {
            return null;
        }
        return tenantDirectoryService.findByOrganisationIdOrSlug(selectedIdentifier)
                .map(TenantDirectoryService.TenantInfo::getOrganisationId)
                .orElse(null);
    }

    private AuthIdentity resolveClientIdentityForLogin(String normalizedEmail, Long organisationId) {
        List<AuthIdentity> candidates = organisationId != null
                ? authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                        normalizedEmail, IdentityType.CLIENT, organisationId)
                : authIdentityRepository.findAllByEmailOrUsername(normalizedEmail).stream()
                        .filter(identity -> identity.getIdentityType() == IdentityType.CLIENT)
                        .toList();

        List<AuthIdentity> activeCandidates = candidates.stream()
                .filter(identity -> Boolean.TRUE.equals(identity.getIsActive()))
                .toList();

        if (activeCandidates.isEmpty()) {
            return null;
        }
        if (activeCandidates.size() > 1) {
            if (organisationId == null) {
                throw new ConflictException("Multiple organisations found for email. Please select an organisation.");
            }
            log.error("Portal login ambiguous in orgId={}: {} CLIENT identities found",
                    organisationId, activeCandidates.size());
            return null;
        }
        return activeCandidates.get(0);
    }

    private Client resolveClientForForgotPassword(Long authId, Long organisationId) {
        if (organisationId != null) {
            return executeTenantReadOnly(
                    organisationId,
                    () -> clientRepository.findByAuthId(authId).orElse(null)
            );
        }
        if (!isPublicSchemaContext()) {
            return clientRepository.findByAuthId(authId).orElse(null);
        }
        return null;
    }

    private String resolveTenantSchema(Long organisationId) {
        if (organisationId == null) {
            return null;
        }
        return tenantDirectoryService.findByOrganisationId(organisationId)
                .map(TenantDirectoryService.TenantInfo::getSchemaName)
                .orElse(null);
    }

    private <T> T executeWithTenantContextForPublicRequest(boolean publicSchemaRequest,
                                                           Long organisationId,
                                                           Supplier<T> work) {
        if (!publicSchemaRequest || organisationId == null) {
            return work.get();
        }
        String schema = resolveTenantSchema(organisationId);
        if (!StringUtils.hasText(schema) || "public".equalsIgnoreCase(schema)) {
            return work.get();
        }
        String previousSchema = TenantContext.getSchemaName();
        Long previousOrg = TenantContext.getOrganisationId();
        try {
            TenantContext.setSchemaName(schema);
            TenantContext.setOrganisationId(organisationId);
            return work.get();
        } finally {
            TenantContext.clear();
            if (previousSchema != null && !previousSchema.isBlank()) {
                TenantContext.setSchemaName(previousSchema);
            }
            if (previousOrg != null) {
                TenantContext.setOrganisationId(previousOrg);
            }
        }
    }

    private Client resolveClientWithTherapistInTenantContext(Long clientId,
                                                             Long organisationId,
                                                             boolean publicSchemaRequest) {
        if (!publicSchemaRequest) {
            return clientRepository.findByIdWithTherapist(clientId).orElse(null);
        }
        if (organisationId == null) {
            return null;
        }
        String schema = resolveTenantSchema(organisationId);
        if (!StringUtils.hasText(schema) || "public".equalsIgnoreCase(schema)) {
            return null;
        }
        return tenantTransactionExecutor.executeReadOnly(
                organisationId,
                schema,
                () -> clientRepository.findByIdWithTherapist(clientId).orElse(null)
        );
    }

    private void setActivatedInTenantContext(Long clientId,
                                             boolean activated,
                                             Instant activatedAt,
                                             Long organisationId,
                                             boolean publicSchemaRequest) {
        if (!publicSchemaRequest) {
            portalSettingsService.setActivated(clientId, activated, activatedAt);
            return;
        }
        if (organisationId == null) {
            throw new ResourceNotFoundException("Client organisation context not found");
        }
        String schema = resolveTenantSchema(organisationId);
        if (!StringUtils.hasText(schema) || "public".equalsIgnoreCase(schema)) {
            throw new ResourceNotFoundException("Client tenant schema not found");
        }
        tenantTransactionExecutor.runWrite(
                organisationId,
                schema,
                () -> portalSettingsService.setActivated(clientId, activated, activatedAt)
        );
    }

    private Client resolveClientInTenantContext(Long authId, Long organisationId) {
        return resolveClientInTenantContext(authId, organisationId, isPublicSchemaContext());
    }

    private Client resolveClientInTenantContext(Long authId, Long organisationId, boolean publicSchemaRequest) {
        if (!publicSchemaRequest) {
            return clientRepository.findByAuthId(authId).orElse(null);
        }
        if (organisationId == null) {
            return null;
        }
        String schema = resolveTenantSchema(organisationId);
        if (!StringUtils.hasText(schema) || "public".equalsIgnoreCase(schema)) {
            return null;
        }
        return tenantTransactionExecutor.executeReadOnly(
                organisationId,
                schema,
                () -> clientRepository.findByAuthId(authId).orElse(null)
        );
    }

    private boolean hasPortalAccessForForgotPassword(Long clientId, Long organisationId) {
        if (organisationId != null) {
            return Boolean.TRUE.equals(executeTenantReadOnly(
                    organisationId,
                    () -> portalSettingsService.hasPortalAccess(clientId)
            ));
        }
        if (!isPublicSchemaContext()) {
            return portalSettingsService.hasPortalAccess(clientId);
        }
        return false;
    }

    private boolean hasPortalAccessInTenantContext(Long clientId, Long organisationId) {
        return hasPortalAccessInTenantContext(clientId, organisationId, isPublicSchemaContext());
    }

    private boolean hasPortalAccessInTenantContext(Long clientId, Long organisationId, boolean publicSchemaRequest) {
        if (!publicSchemaRequest) {
            return portalSettingsService.hasPortalAccess(clientId);
        }
        return hasPortalAccessForForgotPassword(clientId, organisationId);
    }

    private void setPortalActivatedInTenant(Long clientId, Long organisationId, boolean activated, Instant activatedAt) {
        if (organisationId != null) {
            executeTenantWrite(organisationId, () -> portalSettingsService.setActivated(clientId, activated, activatedAt));
            return;
        }
        portalSettingsService.setActivated(clientId, activated, activatedAt);
    }

    private Client findClientWithTherapist(Long clientId, Long organisationId, Client fallback) {
        if (organisationId != null) {
            Client loaded = executeTenantReadOnly(
                    organisationId,
                    () -> clientRepository.findByIdWithTherapist(clientId).orElse(null)
            );
            return loaded != null ? loaded : fallback;
        }
        return clientRepository.findByIdWithTherapist(clientId).orElse(fallback);
    }

    /**
     * Build portal client payload without touching detached lazy associations (e.g. authIdentity on Client).
     */
    private PortalClientResponse buildPortalClientResponse(Client client, Long organisationId, String email) {
        if (organisationId != null) {
            PortalClientResponse response = executeTenantReadOnly(organisationId, () -> {
                Client loaded = clientRepository.findByIdWithTherapist(client.getId()).orElse(client);
                return PortalClientResponse.builder()
                        .id(loaded.getId())
                        .clientId(loaded.getClientId())
                        .fullName(loaded.getFullName())
                        .email(email)
                        .phone(getClientPhone(loaded))
                        .assignedTherapistId(loaded.getAssignedTherapist() != null
                                ? loaded.getAssignedTherapist().getId()
                                : null)
                        .onlineBookingAvailable(isOnlineBookingAvailable(loaded))
                        .build();
            });
            if (response != null) {
                return response;
            }
        }
        Client loaded = clientRepository.findByIdWithTherapist(client.getId()).orElse(client);
        return PortalClientResponse.builder()
                .id(loaded.getId())
                .clientId(loaded.getClientId())
                .fullName(loaded.getFullName())
                .email(email)
                .phone(getClientPhone(loaded))
                .assignedTherapistId(loaded.getAssignedTherapist() != null
                        ? loaded.getAssignedTherapist().getId()
                        : null)
                .onlineBookingAvailable(isOnlineBookingAvailable(loaded))
                .build();
    }

    private <T> T executeTenantReadOnly(Long organisationId, Supplier<T> work) {
        String schema = resolveTenantSchemaName(organisationId);
        if (schema == null) {
            return null;
        }
        return tenantTransactionExecutor.executeReadOnly(organisationId, schema, work);
    }

    private void executeTenantWrite(Long organisationId, Runnable work) {
        String schema = resolveTenantSchemaName(organisationId);
        if (schema == null) {
            return;
        }
        tenantTransactionExecutor.runWrite(organisationId, schema, work);
    }

    private String resolveTenantSchemaName(Long organisationId) {
        if (organisationId == null) {
            return null;
        }
        String schema = resolveTenantSchema(organisationId);
        if (!StringUtils.hasText(schema) || "public".equalsIgnoreCase(schema)) {
            return null;
        }
        return schema;
    }

    private boolean isPortalActivatedInTenantContext(Long clientId, Long organisationId) {
        return isPortalActivatedInTenantContext(clientId, organisationId, isPublicSchemaContext());
    }

    private boolean isPortalActivatedInTenantContext(Long clientId,
                                                     Long organisationId,
                                                     boolean publicSchemaRequest) {
        if (!publicSchemaRequest) {
            return portalSettingsService.isActivated(clientId);
        }
        if (organisationId == null) {
            return false;
        }
        String schema = resolveTenantSchema(organisationId);
        if (!StringUtils.hasText(schema) || "public".equalsIgnoreCase(schema)) {
            return false;
        }
        return tenantTransactionExecutor.executeReadOnly(
                organisationId,
                schema,
                () -> portalSettingsService.isActivated(clientId)
        );
    }

    private AuthIdentity resolveClientIdentityForForgotPassword(String normalizedEmail, Long organisationId) {
        List<AuthIdentity> candidates = organisationId != null
                ? authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                        normalizedEmail, IdentityType.CLIENT, organisationId)
                : authIdentityRepository.findAllByEmailOrUsername(normalizedEmail).stream()
                        .filter(identity -> identity.getIdentityType() == IdentityType.CLIENT)
                        .toList();

        List<AuthIdentity> activeCandidates = candidates.stream()
                .filter(identity -> Boolean.TRUE.equals(identity.getIsActive()))
                .toList();

        if (activeCandidates.isEmpty()) {
            log.debug("Portal forgot-password requested for an unknown, non-client, or inactive identifier");
            return null;
        }
        if (activeCandidates.size() > 1) {
            if (organisationId == null) {
                log.warn("Portal forgot-password identifier matched {} CLIENT identities; organisation selection required",
                        activeCandidates.size());
            } else {
                log.error("Portal forgot-password identifier matched {} CLIENT identities in orgId={}",
                        activeCandidates.size(), organisationId);
            }
            return null;
        }
        return activeCandidates.get(0);
    }

    private static String normalizeIdentifier(String orgId, String orgSlug, String orgIdentifier, String orgValue) {
        if (orgId != null && !orgId.isBlank()) {
            return orgId.trim();
        }
        if (orgSlug != null && !orgSlug.isBlank()) {
            return orgSlug.trim();
        }
        if (orgIdentifier != null && orgValue != null) {
            String idType = orgIdentifier.trim().toLowerCase();
            String idValue = orgValue.trim();
            if ("id".equals(idType) || "slug".equals(idType)) {
                return idValue;
            }
        }
        return null;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Objects.requireNonNull(request, "Request is required");
        boolean publicSchemaRequest = isPublicSchemaContext();

        if (!StringUtils.hasText(request.getToken()) || !StringUtils.hasText(request.getPassword())) {
            throw new BadRequestException("Reset token and password are required");
        }

        if (request.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new BadRequestException("Password must be at least 8 characters long");
        }

        AuthIdentity pendingIdentity = authIdentityService.validatePasswordResetToken(request.getToken());
        if (pendingIdentity == null) {
            throw new BadRequestException("Invalid or expired reset token");
        }
        if (pendingIdentity.getIdentityType() != IdentityType.CLIENT) {
            throw new BadRequestException(
                    "This reset link is for a staff account. Use /auth/staff/set-new-password or POST /api/v1/auth/reset-password.");
        }

        Long organisationId = resolveOrganisationIdFromIdentity(pendingIdentity);
        Client client = requireClientForPortalIdentity(pendingIdentity, organisationId);
        if (!hasPortalAccessForForgotPassword(client.getId(), organisationId)) {
            throw new BadRequestException("Portal access is not enabled for this account");
        }

        AuthIdentity identity = authIdentityService.resetPasswordByToken(
                AuthIdentityService.normalizeResetToken(request.getToken()), request.getPassword());
        if (identity == null) {
            throw new BadRequestException("Invalid or expired reset token");
        }
        if (!authIdentityService.validatePassword(identity, request.getPassword())) {
            throw new BadRequestException("Failed to save new password. Please request a new reset link.");
        }

        tokenBlacklistService.blacklistAllUserTokens(identity.getId());
        authRefreshTokenService.revokeAllForAuthId(identity.getId());
        authKnownDeviceService.revokeAllTrustedDevices(identity.getId());
        authIdentityService.recordSuccessfulLogin(identity.getId());
    }

    private AuthIdentity requireValidClientActivationIdentity(String token) {
        AuthIdentity identity = authIdentityService.findByEmailVerificationToken(token);
        if (identity == null || !authIdentityService.isEmailVerificationTokenValid(identity)) {
            throw new BadRequestException("Invalid or expired activation token");
        }
        if (identity.getIdentityType() != IdentityType.CLIENT) {
            throw new BadRequestException("Invalid activation token");
        }
        return identity;
    }

    private Long resolveOrganisationIdFromIdentity(AuthIdentity identity) {
        if (identity == null || identity.getId() == null) {
            return null;
        }
        if (identity.getOrganisation() != null && identity.getOrganisation().getId() != null) {
            return identity.getOrganisation().getId();
        }

        List<Long> linkedOrgIds = userOrganisationRepository.findByAuth_Id(identity.getId()).stream()
                .filter(link -> link.getOrganisation() != null && link.getOrganisation().getId() != null)
                .map(link -> link.getOrganisation().getId())
                .distinct()
                .toList();
        if (linkedOrgIds.size() == 1) {
            return linkedOrgIds.get(0);
        }
        if (linkedOrgIds.size() > 1) {
            return null;
        }

        List<Long> roleOrgIds = authIdentityRoleRepository.findByAuthIdWithRolesAndPermissions(identity.getId()).stream()
                .filter(roleAssignment -> roleAssignment.getOrganisation() != null
                        && roleAssignment.getOrganisation().getId() != null)
                .map(roleAssignment -> roleAssignment.getOrganisation().getId())
                .distinct()
                .toList();
        if (roleOrgIds.size() == 1) {
            return roleOrgIds.get(0);
        }
        return null;
    }

    private Client requireClientForPortalIdentity(AuthIdentity identity, Long organisationId) {
        if (organisationId == null) {
            throw new BadRequestException("Client portal account is not linked to a clinic. Please contact your clinic.");
        }
        String schemaName = tenantDirectoryService.findByOrganisationId(organisationId)
                .map(TenantDirectoryService.TenantInfo::getSchemaName)
                .orElse(null);
        if (!StringUtils.hasText(schemaName) || "public".equalsIgnoreCase(schemaName)) {
            throw new TenantUnavailableException(
                    "Clinic database is not configured. Please contact your clinic administrator.",
                    "TENANT_SCHEMA_MISSING");
        }
        if (!tenantSchemaHealthService.schemaExists(schemaName)) {
            throw new TenantUnavailableException(
                    "Clinic database is not available. Please contact your clinic administrator.",
                    "TENANT_SCHEMA_MISSING");
        }
        if (!tenantSchemaHealthService.tableExists(schemaName, "clients")) {
            throw new TenantUnavailableException(
                    "Clinic database is not provisioned yet. Tenant migrations must be applied before clients can activate portal access.",
                    "TENANT_SCHEMA_MISSING");
        }
        Client client = resolveClientForForgotPassword(identity.getId(), organisationId);
        if (client == null) {
            throw new BadRequestException("Client portal account is not fully set up. Please contact your clinic.");
        }
        return client;
    }

    // ========== APPOINTMENTS ==========

    @Transactional(readOnly = true)
    public PaginatedResponse<PortalAppointmentResponse> getAppointments(
            int page,
            int pageSize,
            String status,
            String ipAddress,
            String userAgent) {
        Client client = getCurrentClientEntity();
        if (client == null) {
            throw new ResourceNotFoundException("Client not found");
        }

        int safePage = Math.max(page, DEFAULT_APPOINTMENTS_PAGE);
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_APPOINTMENTS_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
                safePage - 1,
                safePageSize,
                Sort.by(Sort.Direction.DESC, "sessionDate"));

        Collection<String> statusCandidates = resolvePortalAppointmentStatusFilter(status);
        Page<Session> sessionPage = statusCandidates.isEmpty()
                ? sessionRepositoryForAppointments.findByClientId(client.getId(), pageable)
                : sessionRepositoryForAppointments.findByClientIdAndStatusInIgnoreCase(
                        client.getId(), statusCandidates, pageable);
        List<Session> sessions = sessionPage.getContent();

        // Get all rooms and services for lookup
        Map<Long, Room> roomMap = roomRepository.findAll().stream()
                .collect(Collectors.toMap(Room::getId, r -> r));

        Map<Long, com.smart.therapy.flow.billing.entity.Service> serviceMap = serviceRepository.findAll().stream()
                .collect(Collectors.toMap(com.smart.therapy.flow.billing.entity.Service::getId, s -> s));

        // Get client's timezone for date/time formatting (profile → UTC)
        ZoneId clientTimezone = timezoneService.resolveClientPortalZone(client.getId(), null);
        DateTimeFormatter dateFormatter = DATE_FORMATTER_PATTERN.withZone(clientTimezone);
        DateTimeFormatter timeFormatter = TIME_FORMATTER_PATTERN.withZone(clientTimezone);

        // Get therapist profiles to check for virtual rooms
        // Batch load therapist profiles to avoid N+1 query
        Set<Long> therapistIds = sessions.stream()
                .filter(s -> s.getTherapist() != null)
                .map(s -> s.getTherapist().getId())
                .collect(Collectors.toSet());

        Map<Long, UserProfile> therapistProfileMap = new HashMap<>();
        if (!therapistIds.isEmpty()) {
            userProfileRepository.findByUserIdIn(therapistIds)
                    .forEach(profile -> therapistProfileMap
                            .put(profile.getUser() != null ? profile.getUser().getId() : null, profile));
        }

        Map<Long, SessionNote> ratedNotesBySessionId = sessionNoteRepository
                .findByClientIdWithRelations(client.getId())
                .stream()
                .filter(note -> note.getClientRating() != null && note.getSession() != null)
                .collect(Collectors.toMap(
                        note -> note.getSession().getId(),
                        note -> note,
                        (first, second) -> first));

        // Format sessions for portal display
        List<PortalAppointmentResponse> appointments = sessions.stream()
                .map(s -> {
                    Room room = s.getRoom() != null ? roomMap.get(s.getRoom().getId()) : null;
                    com.smart.therapy.flow.billing.entity.Service service = s.getService() != null
                            ? serviceMap.get(s.getService().getId())
                            : null;

                    // Determine location: check session type, zoom enabled, or if room is
                    // therapist's virtual room
                    String location = "Office"; // default
                    // Check if session type is "online"
                    if (s.getSessionType() != null && "online".equalsIgnoreCase(s.getSessionType())) {
                        location = "Online";
                    }
                    // Check if zoom is enabled via SessionIntegration
                    else if (getZoomIntegration(s) != null) {
                        location = "Online";
                    }
                    // Check if room is therapist's virtual room
                    else if (s.getTherapist() != null && s.getRoom() != null) {
                        UserProfile therapistProfile = therapistProfileMap.get(s.getTherapist().getId());
                        if (therapistProfile != null && therapistProfile.getVirtualRoom() != null) {
                            if (s.getRoom().getId().equals(therapistProfile.getVirtualRoom().getId())) {
                                location = "Online";
                            }
                        }
                    }

                    // Convert Instant to ZonedDateTime in client's timezone for accurate formatting
                    java.time.ZonedDateTime zonedDateTime = s.getSessionDate().atZone(clientTimezone);
                    SessionNote ratedNote = ratedNotesBySessionId.get(s.getId());

                    return PortalAppointmentResponse.builder()
                            .id(s.getId())
                            .sessionDate(zonedDateTime.format(dateFormatter))
                            .sessionTime(zonedDateTime.format(timeFormatter))
                            .duration(s.getDuration() != null ? s.getDuration()
                                    : (service != null ? service.getDuration() : null))
                            .sessionType(resolveClinicalSessionType(s))
                            .sessionMode(resolveSessionMode(s))
                            .status(s.getStatus() != null ? s.getStatus() : null)
                            .location(location)
                            .roomName(location.equals("Online") ? "Virtual Room"
                                    : (room != null
                                            ? (room.getRoomName() != null ? room.getRoomName() : room.getRoomNumber())
                                            : null))
                            .referenceNumber(
                                    client.getReferral() != null && client.getReferral().getReferenceNumber() != null
                                            ? client.getReferral().getReferenceNumber()
                                            : client.getClientId())
                            .serviceCode(service != null ? service.getServiceCode() : null)
                            .serviceName(service != null ? service.getServiceName() : null)
                            .serviceRate(service != null ? service.getBaseRate() : null)
                            .therapistName(s.getTherapist() != null ? s.getTherapist().getFullName() : null)
                            .clientRating(ratedNote != null ? ratedNote.getClientRating() : null)
                            .clientRatingComment(resolveClientRatingComment(ratedNote))
                            .build();
                })
                .collect(Collectors.toList());

        // Audit appointment access (in separate transaction, wrapped to not fail
        // read-only transaction)
        try {
            recordAuditEvent(client.getId(),
                    getClientEmail(client),
                    "appointments_viewed", ipAddress, userAgent, "success",
                    "Portal appointment viewing - page: " + safePage + ", count: " + appointments.size(), true);
        } catch (Exception e) {
            // Log but don't fail the read operation if audit logging fails
            log.warn("Failed to record audit event for appointment viewing: {}", e.getMessage());
        }

        return PaginatedResponse.of(appointments, sessionPage.getTotalElements(), safePage, safePageSize);
    }

    /**
     * Resolve portal status filter values to match stored session status strings
     * (hyphen / underscore / alias variants). Empty = no status filter.
     */
    private Collection<String> resolvePortalAppointmentStatusFilter(String status) {
        if (!StringUtils.hasText(status)) {
            return List.of();
        }

        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String raw = status.trim();
        addPortalStatusCandidate(candidates, raw);

        String resolved = systemOptionResolverService.resolveOptionKey(
                SystemOptionCategories.SESSION_STATUS, raw);
        if (StringUtils.hasText(resolved)) {
            addPortalStatusCandidate(candidates, resolved);
        }

        String normalized = SystemOptionKeyMatcher.normalize(raw);
        switch (normalized) {
            case "noshow", "no_show" -> {
                addPortalStatusCandidate(candidates, "no-show");
                addPortalStatusCandidate(candidates, "no_show");
                addPortalStatusCandidate(candidates, "noshow");
            }
            case "in_progress", "inprogress" -> {
                addPortalStatusCandidate(candidates, "in-progress");
                addPortalStatusCandidate(candidates, "in_progress");
            }
            case "rescheduled", "rescheduling" -> {
                addPortalStatusCandidate(candidates, "rescheduling");
                addPortalStatusCandidate(candidates, "rescheduled");
            }
            case "cancelled", "canceled" -> {
                addPortalStatusCandidate(candidates, "cancelled");
                addPortalStatusCandidate(candidates, "canceled");
            }
            default -> {
                // already captured via raw/resolved
            }
        }

        if (candidates.isEmpty()) {
            throw new BadRequestException("Invalid session status filter: '" + status + "'");
        }
        return candidates;
    }

    private static void addPortalStatusCandidate(Set<String> candidates, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String lower = value.trim().toLowerCase(Locale.ROOT);
        candidates.add(lower);
        candidates.add(lower.replace('_', '-'));
        candidates.add(lower.replace('-', '_'));
    }

    // ========== NOTIFICATIONS ==========

    @Transactional(readOnly = true)
    public List<com.smart.therapy.flow.notification.dto.NotificationResponse> getNotifications(String ipAddress,
            String userAgent) {
        Long clientId = getCurrentClientId();
        List<Notification> allNotifications = new ArrayList<>();

        // Get client-related notifications
        List<Notification> clientNotifications = notificationRepository.findByRelatedEntity("client",
                clientId);
        allNotifications.addAll(clientNotifications);

        // Get session-related notifications
        List<Session> clientSessions = sessionRepositoryForAppointments.findByClientId(clientId);
        if (!clientSessions.isEmpty()) {
            List<Long> sessionIds = clientSessions.stream()
                    .map(Session::getId)
                    .collect(Collectors.toList());
            List<Notification> sessionNotifications = notificationRepository
                    .findByRelatedEntityTypeAndEntityIds("session", sessionIds);
            allNotifications.addAll(sessionNotifications);
        }

        // Get billing-related notifications
        List<com.smart.therapy.flow.billing.entity.SessionBilling> billingRecords = new ArrayList<>();
        if (!clientSessions.isEmpty()) {
            List<Long> sessionIds = clientSessions.stream()
                    .map(Session::getId)
                    .collect(Collectors.toList());
            if (!sessionIds.isEmpty()) {
                billingRecords = sessionBillingRepository.findBySessionIdIn(sessionIds);
            }
        }

        if (!billingRecords.isEmpty()) {
            List<Long> billingIds = billingRecords.stream()
                    .map(com.smart.therapy.flow.billing.entity.SessionBilling::getId)
                    .collect(Collectors.toList());
            List<Notification> billingNotifications = notificationRepository
                    .findByRelatedEntityTypeAndEntityIds("billing", billingIds);
            allNotifications.addAll(billingNotifications);
        }

        // Combine, sort, and limit to 50 most recent
        List<Notification> sortedNotifications = allNotifications.stream()
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .limit(MAX_NOTIFICATIONS_LIMIT)
                .collect(Collectors.toList());

        return sortedNotifications.stream()
                .map(this::toNotificationResponse)
                .collect(Collectors.toList());
    }

    // ========== SERVICES ==========

    @Transactional(readOnly = true)
    public List<com.smart.therapy.flow.client.portal.dto.PortalServiceResponse> getServices() {
        // Client is authenticated via JWT, no need to validate session

        // Get all active services that are visible in client portal
        List<com.smart.therapy.flow.billing.entity.Service> services = serviceRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()) && Boolean.TRUE.equals(s.getClientPortalVisible()))
                .sorted(Comparator.comparing(com.smart.therapy.flow.billing.entity.Service::getServiceName))
                .collect(Collectors.toList());

        return services.stream()
                .map(s -> com.smart.therapy.flow.client.portal.dto.PortalServiceResponse.builder()
                        .id(s.getId())
                        .serviceCode(s.getServiceCode())
                        .serviceName(s.getServiceName())
                        .description(s.getDescription())
                        .duration(s.getDuration())
                        .baseRate(s.getBaseRate())
                        // category field removed - not needed in portal response
                        .build())
                .collect(Collectors.toList());
    }

    // ========== AVAILABLE SLOTS ==========

    @Transactional(readOnly = true)
    public com.smart.therapy.flow.client.portal.dto.AvailableSlotResponse getAvailableSlots(
            String startDate,
            String endDate,
            String sessionType,
            Long serviceId) {
        Objects.requireNonNull(startDate, "Start date is required");
        Objects.requireNonNull(endDate, "End date is required");
        Objects.requireNonNull(sessionType, "Session type is required");
        Objects.requireNonNull(serviceId, "Service ID is required");

        String sessionModeKey = systemOptionResolverService.requireOptionKey(
                SystemOptionCategories.SESSION_MODE, sessionType);
        // Match staff booking: online includes virtual-style modes; in-person is physical.
        boolean onlineMode = SystemOptionKeyMatcher.matchesAny(sessionModeKey, "online", "virtual", "telehealth", "video");
        boolean inPersonMode = SystemOptionKeyMatcher.matchesAny(sessionModeKey, "in_person", "in-person", "inperson");
        if (!onlineMode && !inPersonMode) {
            throw new BadRequestException(
                    "Invalid sessionType. Use online/virtual or in-person.");
        }

        Client client = getCurrentClientEntity();
        if (client.getAssignedTherapist() == null) {
            throw new BadRequestException("No therapist assigned to your account");
        }

        Long therapistId = client.getAssignedTherapist().getId();

        if (onlineMode && !isOnlineBookingAvailable(client)) {
            throw new BadRequestException(ONLINE_BOOKING_UNAVAILABLE_MESSAGE);
        }

        // Get therapist profile for working hours
        UserProfile therapistProfile = userProfileRepository.findByUserId(therapistId)
                .orElseThrow(() -> new BadRequestException("Therapist profile not found"));

        com.smart.therapy.flow.billing.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        // Parse date range with proper error handling
        java.time.LocalDate start;
        java.time.LocalDate end;
        try {
            start = java.time.LocalDate.parse(startDate, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (java.time.format.DateTimeParseException e) {
            throw new BadRequestException(
                    String.format("Invalid start date format: '%s'. Expected format: yyyy-MM-dd (e.g., 2025-01-15)",
                            startDate));
        }

        try {
            end = java.time.LocalDate.parse(endDate, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (java.time.format.DateTimeParseException e) {
            throw new BadRequestException(
                    String.format("Invalid end date format: '%s'. Expected format: yyyy-MM-dd (e.g., 2025-01-20)",
                            endDate));
        }

        // Validate date range
        if (start.isAfter(end)) {
            throw new BadRequestException(
                    String.format("Start date (%s) cannot be after end date (%s)", startDate, endDate));
        }

        // Validate date range is not too large (reduced from 90 to 30 days for
        // performance)
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, end);
        if (daysBetween > 30) {
            throw new BadRequestException(
                    String.format("Date range cannot exceed 30 days. Provided range: %d days", daysBetween + 1));
        }

        // Validate dates are not in the past (use client's timezone for "today")
        ZoneId clientTimezone = timezoneService.resolveClientPortalZone(client.getId(), null);
        java.time.LocalDate today = java.time.LocalDate.now(clientTimezone);
        if (start.isBefore(today)) {
            throw new BadRequestException(
                    String.format("Start date (%s) cannot be in the past. Today's date: %s", startDate, today));
        }

        // Match admin/therapist booking: step slots by selected service duration
        int sessionDuration = service.getDuration() != null ? service.getDuration()
                : (therapistProfile.getSessionDuration() != null ? therapistProfile.getSessionDuration() : 60);
        if (sessionDuration <= 0) {
            sessionDuration = 60;
        }

        // Get therapist's timezone for proper date/time handling
        ZoneId therapistTimezone = timezoneService.getTherapistTimezone(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Therapist timezone is not configured. Please set timezone in therapist profile."));

        // Get all blocked times for date range (using therapist's timezone)
        Instant rangeStart = start.atStartOfDay(therapistTimezone).toInstant();
        Instant rangeEnd = end.atTime(23, 59, 59).atZone(therapistTimezone).toInstant();

        List<TherapistBlockedTime> blockedTimes = therapistBlockedTimeRepository
                .findByTherapistIdAndIsActiveTrueOrderByStartTimeAsc(therapistId)
                .stream()
                .filter(bt -> {
                    Instant btStart = bt.getStartTime();
                    Instant btEnd = bt.getEndTime();
                    return (btStart.isBefore(rangeEnd) || btStart.equals(rangeEnd)) &&
                            (btEnd.isAfter(rangeStart) || btEnd.equals(rangeStart));
                })
                .collect(Collectors.toList());

        // Get all therapist sessions for date range (optimized query - filters at
        // database level)
        List<String> activeStatuses = List.of("scheduled", "confirmed", "in_progress");
        List<Session> therapistSessions = sessionRepositoryForAppointments.findByTherapistAndDateRangeWithStatuses(
                therapistId, rangeStart, rangeEnd, activeStatuses);

        // Get all sessions for room checking (optimized query - filters at database
        // level)
        List<Session> allSessions = sessionRepositoryForAppointments.findByDateRangeWithStatuses(
                rangeStart, rangeEnd, activeStatuses);

        // Parse working hours
        // Get working hours list
        List<UserProfileWorkingHours> workingHours = therapistProfile.getWorkingHours() != null
                ? therapistProfile.getWorkingHours()
                : Collections.emptyList();

        // Pre-group sessions by date for efficient lookup (avoids repeated filtering)
        // Use therapist's timezone for grouping
        Map<java.time.LocalDate, List<Session>> therapistSessionsByDate = therapistSessions.stream()
                .collect(Collectors.groupingBy(s -> s.getSessionDate().atZone(therapistTimezone).toLocalDate()));

        Map<java.time.LocalDate, List<Session>> allSessionsByDate = allSessions.stream()
                .collect(Collectors.groupingBy(s -> s.getSessionDate().atZone(therapistTimezone).toLocalDate()));

        // Pre-group blocked times by date (using therapist's timezone)
        Map<java.time.LocalDate, List<TherapistBlockedTime>> blockedTimesByDate = new HashMap<>();
        for (TherapistBlockedTime bt : blockedTimes) {
            Instant btStart = bt.getStartTime();
            Instant btEnd = bt.getEndTime();
            java.time.LocalDate btStartDate = btStart.atZone(therapistTimezone).toLocalDate();
            java.time.LocalDate btEndDate = btEnd.atZone(therapistTimezone).toLocalDate();

            java.time.LocalDate current = btStartDate;
            while (!current.isAfter(btEndDate)) {
                blockedTimesByDate.computeIfAbsent(current, k -> new ArrayList<>()).add(bt);
                current = current.plusDays(1);
            }
        }

        // Generate slots for each day
        Map<String, List<com.smart.therapy.flow.client.portal.dto.AvailableSlotResponse.TimeSlot>> slotsByDate = new LinkedHashMap<>();

        java.time.LocalDate currentDate = start;
        while (!currentDate.isAfter(end)) {
            final java.time.LocalDate date = currentDate;
            String dateKey = date.toString();
            String dayOfWeek = date.getDayOfWeek().name(); // MONDAY, TUESDAY, etc.

            // Get shifts for this day, filtered to the client's requested modality
            // (same rules as TherapistAvailabilityService isShiftCompatible)
            List<UserProfileWorkingHours> dayShifts = workingHours.stream()
                    .filter(wh -> wh.getDay() != null && wh.getDay().equalsIgnoreCase(dayOfWeek))
                    // Consultation / public-site hours belong to the marketing site only.
                    .filter(wh -> WorkingHoursServiceMatcher.matches(wh, service))
                    .filter(wh -> isPortalShiftCompatibleWithSessionMode(wh, onlineMode, inPersonMode))
                    .collect(Collectors.toList());

            if (dayShifts.isEmpty()) {
                slotsByDate.put(dateKey, Collections.emptyList());
                currentDate = currentDate.plusDays(1);
                continue;
            }

            // Get sessions and blocked times for this day (from pre-grouped maps)
            List<Session> dayTherapistSessions = therapistSessionsByDate.getOrDefault(date, Collections.emptyList());
            List<TherapistBlockedTime> dayBlockedTimes = blockedTimesByDate.getOrDefault(date, Collections.emptyList());
            List<Session> dayAllSessions = allSessionsByDate.getOrDefault(date, Collections.emptyList());

            // Pre-compute busy time intervals for efficient checking
            // Build sets of busy intervals (start, end) for therapist, blocked times, and
            // rooms
            java.util.Set<java.util.AbstractMap.SimpleEntry<Instant, Instant>> therapistBusyIntervals = new java.util.HashSet<>();
            for (Session s : dayTherapistSessions) {
                Instant sStart = s.getSessionDate();
                int sDuration = s.getDuration() != null ? s.getDuration() : 60;
                Instant sEnd = sStart.plusSeconds(sDuration * 60L);
                therapistBusyIntervals.add(new java.util.AbstractMap.SimpleEntry<>(sStart, sEnd));
            }

            java.util.Set<java.util.AbstractMap.SimpleEntry<Instant, Instant>> blockedIntervals = new java.util.HashSet<>();
            for (TherapistBlockedTime bt : dayBlockedTimes) {
                blockedIntervals.add(new java.util.AbstractMap.SimpleEntry<>(bt.getStartTime(), bt.getEndTime()));
            }

            // For room checking, build intervals by room ID
            Map<Long, java.util.Set<java.util.AbstractMap.SimpleEntry<Instant, Instant>>> roomBusyIntervals = new HashMap<>();
            // Online/virtual: no room occupancy requirement (aligned with staff availability)
            if (inPersonMode) {
                // Extract room IDs from UserProfilePhysicalRoom entities
                List<Long> availableRoomIds = therapistProfile.getAvailablePhysicalRooms() != null
                        ? therapistProfile.getAvailablePhysicalRooms().stream()
                                .map(pr -> pr.getRoom() != null ? pr.getRoom().getId() : null)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList())
                        : Collections.emptyList();
                for (Long roomId : availableRoomIds) {
                    java.util.Set<java.util.AbstractMap.SimpleEntry<Instant, Instant>> intervals = new java.util.HashSet<>();
                    for (Session s : dayAllSessions) {
                        if (s.getRoom() != null && s.getRoom().getId().equals(roomId)) {
                            Instant sStart = s.getSessionDate();
                            int sDuration = s.getDuration() != null ? s.getDuration() : 60;
                            Instant sEnd = sStart.plusSeconds(sDuration * 60L);
                            intervals.add(new java.util.AbstractMap.SimpleEntry<>(sStart, sEnd));
                        }
                    }
                    roomBusyIntervals.put(roomId.longValue(), intervals);
                }
            }

            // Generate time slots stepped by selected service duration (same as admin/therapist).
            // Keyed by start instant so overlapping availability rows cannot emit the same slot twice.
            Map<Instant, com.smart.therapy.flow.client.portal.dto.AvailableSlotResponse.TimeSlot> slotsByStart = new LinkedHashMap<>();

            // Iterate over each shift for the day
            for (UserProfileWorkingHours shift : dayShifts) {
                if (shift.getStartTime() == null || shift.getEndTime() == null)
                    continue;

                java.time.LocalTime currentTime = shift.getStartTime();
                java.time.LocalTime shiftEnd = shift.getEndTime();

                while (currentTime.isBefore(shiftEnd)) {
                    // Create slot time in therapist's timezone
                    java.time.LocalDateTime slotDateTime = date.atTime(currentTime);
                    final Instant slotTime = slotDateTime.atZone(therapistTimezone).toInstant();
                    final Instant slotEnd = slotTime.plusSeconds(sessionDuration * 60L);

                    java.time.LocalTime slotEndLocal = slotEnd.atZone(therapistTimezone).toLocalTime();
                    // Slot must fully fit inside the shift
                    if (slotEnd.atZone(therapistTimezone).toLocalDate().isAfter(date)
                            || (slotEndLocal.isAfter(shiftEnd) && !slotEndLocal.equals(shiftEnd))) {
                        break;
                    }

                    // Check therapist availability using pre-computed intervals
                    boolean isTherapistBusy = therapistBusyIntervals.stream()
                            .anyMatch(interval -> slotTime.isBefore(interval.getValue())
                                    && slotEnd.isAfter(interval.getKey()));

                    // Check blocked times using pre-computed intervals
                    boolean isBlocked = blockedIntervals.stream()
                            .anyMatch(interval -> slotTime.isBefore(interval.getValue())
                                    && slotEnd.isAfter(interval.getKey()));

                    // Check room availability using pre-computed intervals
                    boolean roomAvailable;
                    if (onlineMode) {
                        // Virtual sessions do not need a free physical/virtual room assignment
                        roomAvailable = true;
                    } else if (inPersonMode) {
                        // Extract room IDs from UserProfilePhysicalRoom entities
                        List<Long> availableRoomIds = therapistProfile.getAvailablePhysicalRooms() != null
                                ? therapistProfile.getAvailablePhysicalRooms().stream()
                                        .map(pr -> pr.getRoom() != null ? pr.getRoom().getId() : null)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList())
                                : Collections.emptyList();
                        if (availableRoomIds.isEmpty()) {
                            roomAvailable = false;
                        } else {
                            roomAvailable = availableRoomIds.stream().anyMatch(roomId -> {
                                java.util.Set<java.util.AbstractMap.SimpleEntry<Instant, Instant>> intervals = roomBusyIntervals
                                        .getOrDefault(roomId, Collections.emptySet());
                                boolean roomBusy = intervals.stream()
                                        .anyMatch(interval -> slotTime.isBefore(interval.getValue())
                                                && slotEnd.isAfter(interval.getKey()));
                                return !roomBusy;
                            });
                        }
                    } else {
                        roomAvailable = false;
                    }

                    if (!isTherapistBusy && !isBlocked && roomAvailable) {
                        // Hide slots that have already started (timezone-safe Instant check)
                        if (!slotTime.isAfter(Instant.now())) {
                            currentTime = currentTime.plusMinutes(sessionDuration);
                            continue;
                        }

                        String startStr = String.format("%02d:%02d", currentTime.getHour(), currentTime.getMinute());
                        String endStr = String.format("%02d:%02d",
                                slotEndLocal.getHour(),
                                slotEndLocal.getMinute());

                        slotsByStart.putIfAbsent(slotTime,
                                com.smart.therapy.flow.client.portal.dto.AvailableSlotResponse.TimeSlot
                                        .builder()
                                        .start(startStr)
                                        .end(endStr)
                                        .startUtc(slotTime.toString())
                                        .endUtc(slotEnd.toString())
                                        .build());
                    }

                    currentTime = currentTime.plusMinutes(sessionDuration);
                }
            }

            // Sort slots by time (using string comparison e.g. "09:00" < "10:00")
            List<com.smart.therapy.flow.client.portal.dto.AvailableSlotResponse.TimeSlot> availableSlots =
                    new ArrayList<>(slotsByStart.values());
            availableSlots.sort(Comparator
                    .comparing(com.smart.therapy.flow.client.portal.dto.AvailableSlotResponse.TimeSlot::getStart));

            slotsByDate.put(dateKey, availableSlots);
            currentDate = currentDate.plusDays(1);
        }

        return com.smart.therapy.flow.client.portal.dto.AvailableSlotResponse.builder()
                .slotsByDate(slotsByDate)
                .timezone(clientTimezone.getId())
                .build();
    }

    /**
     * Reject bookings outside the therapist's clinical availability for the requested
     * service and modality. Consultation / public-site hours never qualify here.
     */
    private void validateWithinTherapistAvailability(
            UserProfile therapistProfile,
            com.smart.therapy.flow.billing.entity.Service service,
            Long therapistId,
            String sessionModeKey,
            Instant sessionStart,
            Instant sessionEnd) {
        boolean onlineMode = SystemOptionKeyMatcher.matchesAny(
                sessionModeKey, "online", "virtual", "telehealth", "video");
        boolean inPersonMode = SystemOptionKeyMatcher.matchesAny(
                sessionModeKey, "in_person", "in-person", "inperson");

        ZoneId therapistTimezone = timezoneService.getTherapistTimezone(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Therapist timezone is not configured. Please set timezone in therapist profile."));

        java.time.ZonedDateTime startLocal = sessionStart.atZone(therapistTimezone);
        java.time.ZonedDateTime endLocal = sessionEnd.atZone(therapistTimezone);
        String dayOfWeek = startLocal.getDayOfWeek().name();

        List<UserProfileWorkingHours> workingHours = therapistProfile.getWorkingHours() != null
                ? therapistProfile.getWorkingHours()
                : Collections.emptyList();

        boolean withinAvailability = workingHours.stream()
                .filter(wh -> wh.getDay() != null && wh.getDay().equalsIgnoreCase(dayOfWeek))
                .filter(wh -> WorkingHoursServiceMatcher.matches(wh, service))
                .filter(wh -> isPortalShiftCompatibleWithSessionMode(wh, onlineMode, inPersonMode))
                .anyMatch(wh -> wh.getStartTime() != null
                        && wh.getEndTime() != null
                        && !startLocal.toLocalTime().isBefore(wh.getStartTime())
                        && !endLocal.toLocalDate().isAfter(startLocal.toLocalDate())
                        && !endLocal.toLocalTime().isAfter(wh.getEndTime()));

        if (!withinAvailability) {
            throw new BadRequestException(
                    "The selected time is outside your therapist's availability. Please pick one of the offered slots.");
        }
    }

    /**
     * Whether a therapist working-hours shift allows the portal booking modality.
     * Aligns with staff availability: BOTH always matches; VIRTUAL only online; IN_PERSON only in-person.
     */
    private boolean isPortalShiftCompatibleWithSessionMode(
            UserProfileWorkingHours shift,
            boolean onlineMode,
            boolean inPersonMode) {
        ShiftMode shiftMode = shift.getSessionMode() != null ? shift.getSessionMode() : ShiftMode.BOTH;
        if (shiftMode == ShiftMode.BOTH) {
            return true;
        }
        if (onlineMode && shiftMode == ShiftMode.VIRTUAL) {
            return true;
        }
        if (inPersonMode && shiftMode == ShiftMode.IN_PERSON) {
            return true;
        }
        return false;
    }

    /**
     * Auto-assign a physical room from the therapist's schedule for in-person sessions only.
     * Online/virtual sessions never use a room (Zoom is the meeting location).
     * Clients cannot select rooms; primary physical room is preferred when free.
     */
    private Room resolveRoomFromTherapistSchedule(
            Long therapistId,
            UserProfile therapistProfile,
            String sessionModeKey,
            Instant sessionStart,
            int durationMinutes,
            Long excludeSessionId) {
        Objects.requireNonNull(therapistProfile, "Therapist profile is required");
        Objects.requireNonNull(sessionStart, "Session start is required");

        // Online/virtual: no room required or assigned (aligned with staff booking).
        if (SystemOptionKeyMatcher.matchesAny(sessionModeKey, "online", "virtual", "telehealth", "video")) {
            return null;
        }

        if (!SystemOptionKeyMatcher.matchesAny(sessionModeKey, "in_person", "in-person", "inperson")) {
            throw new BadRequestException("Unsupported session mode for portal booking: " + sessionModeKey);
        }

        Instant sessionEnd = sessionStart.plusSeconds(Math.max(durationMinutes, 1) * 60L);
        List<Room> candidates = new ArrayList<>();
        List<UserProfilePhysicalRoom> physicalRooms = therapistProfile.getAvailablePhysicalRooms() != null
                ? new ArrayList<>(therapistProfile.getAvailablePhysicalRooms())
                : new ArrayList<>();
        physicalRooms.sort(
                Comparator
                        .comparing((UserProfilePhysicalRoom pr) -> !Boolean.TRUE.equals(pr.getIsPrimary()))
                        .thenComparing(pr -> pr.getDisplayOrder() != null ? pr.getDisplayOrder() : Integer.MAX_VALUE));
        for (UserProfilePhysicalRoom physicalRoom : physicalRooms) {
            Room room = physicalRoom.getRoom();
            if (room != null && Boolean.TRUE.equals(room.getIsActive())) {
                candidates.add(room);
            }
        }

        if (candidates.isEmpty()) {
            throw new BadRequestException(
                    "Your therapist has no physical rooms configured for this session. Please contact the clinic.");
        }

        for (Room candidate : candidates) {
            if (isPortalRoomAvailable(candidate, sessionStart, sessionEnd, excludeSessionId)) {
                log.info(
                        "Portal auto-assigned room {} ({}) for therapist {} at {}",
                        candidate.getId(),
                        candidate.getRoomName(),
                        therapistId,
                        sessionStart);
                return candidate;
            }
        }

        throw new BadRequestException("No rooms available for this time slot. Please select another time.");
    }

    private boolean isPortalRoomAvailable(
            Room room,
            Instant sessionStart,
            Instant sessionEnd,
            Long excludeSessionId) {
        if (room == null || !Boolean.TRUE.equals(room.getIsActive())) {
            return false;
        }
        int roomCapacity = (room.getCapacity() != null && room.getCapacity() > 0) ? room.getCapacity() : 1;
        long overlaps = sessionRepositoryForAppointments.countOverlappingSessionsForRoom(
                room.getId(), sessionStart, sessionEnd);

        // Reschedule must ignore the session being moved when it currently occupies this room.
        if (excludeSessionId != null && overlaps > 0) {
            Session existing = sessionRepositoryForAppointments.findById(excludeSessionId).orElse(null);
            if (existing != null
                    && existing.getRoom() != null
                    && Objects.equals(existing.getRoom().getId(), room.getId())
                    && existing.getSessionDate() != null) {
                Instant existingStart = existing.getSessionDate();
                int existingDuration = existing.getDuration() != null ? existing.getDuration() : 60;
                Instant existingEnd = existingStart.plusSeconds(existingDuration * 60L);
                if (sessionStart.isBefore(existingEnd) && sessionEnd.isAfter(existingStart)) {
                    overlaps = Math.max(0L, overlaps - 1L);
                }
            }
        }

        return overlaps < roomCapacity;
    }

    // ========== BOOK APPOINTMENT ==========

    @Transactional
    public com.smart.therapy.flow.client.portal.dto.BookAppointmentResponse bookAppointment(
            com.smart.therapy.flow.client.portal.dto.BookAppointmentRequest request,
            String ipAddress,
            String userAgent) {
        Objects.requireNonNull(request, "Request is required");

        Client client = getCurrentClientEntity();
        if (client.getAssignedTherapist() == null) {
            throw new BadRequestException("No therapist assigned to your account");
        }

        Long therapistId = client.getAssignedTherapist().getId();

        // Parse session start time (accepts ISO 8601 with or without timezone offset)
        // If timezone offset is provided, it's interpreted as the client's local time
        // If no offset (ends with Z), it's interpreted as UTC
        Instant sessionDateTime;
        try {
            String timeString = request.getSessionStartUtc().trim();

            // If it has a timezone offset (e.g., +05:00, -05:00), parse as OffsetDateTime
            // This means the client is sending their local time with timezone
            if (timeString.contains("+") || (timeString.contains("-") && timeString.lastIndexOf("-") > 10)) {
                // Has timezone offset - parse as OffsetDateTime and convert to UTC
                // The offset pins the instant, nothing else. A client's timezone is their own
                // setting; it is never inferred from a request offset, which spans many zones.
                sessionDateTime = java.time.OffsetDateTime.parse(timeString).toInstant();
            } else if (timeString.endsWith("Z")) {
                // Explicitly UTC - parse directly
                sessionDateTime = Instant.parse(timeString);
            } else {
                // No timezone specified - assume it's in client's timezone
                // Parse as local time and convert using client's timezone
                ZoneId clientTimezone = timezoneService.getClientTimezone(client.getId())
                        .orElseThrow(() -> new BadRequestException(
                                "Client timezone is not configured. Please set timezone in client profile."));
                java.time.LocalDateTime localDateTime = java.time.LocalDateTime.parse(timeString);
                sessionDateTime = localDateTime.atZone(clientTimezone).toInstant();
            }
        } catch (java.time.format.DateTimeParseException e) {
            log.warn("Invalid date format provided: {}", request.getSessionStartUtc(), e);
            throw new BadRequestException(
                    "Invalid session date format. Expected ISO 8601 format (e.g., 2025-01-24T14:30:00Z or 2025-01-24T14:30:00+05:00).");
        } catch (Exception e) {
            log.error("Unexpected error parsing session date: {}", request.getSessionStartUtc(), e);
            throw new BadRequestException("Invalid session date format. Expected ISO 8601 format.");
        }

        // Validate that session date is not in the past
        if (sessionDateTime.isBefore(Instant.now())) {
            throw new BadRequestException("Cannot book appointment for a time in the past");
        }

        // Get service
        com.smart.therapy.flow.billing.entity.Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        // Get duration (use service duration if not provided)
        int sessionDuration = request.getDuration() != null ? request.getDuration() : service.getDuration();

        // Validate duration
        if (sessionDuration <= 0) {
            throw new BadRequestException("Session duration must be greater than 0 minutes");
        }
        if (sessionDuration > 480) { // 8 hours max
            throw new BadRequestException("Session duration cannot exceed 8 hours (480 minutes)");
        }

        Instant sessionEnd = sessionDateTime.plusSeconds(sessionDuration * 60L);

        // Get therapist profile for room assignment (eager-load schedule rooms)
        UserProfile therapistProfile = userProfileRepository.findByUserIdWithRooms(therapistId)
                .or(() -> userProfileRepository.findByUserId(therapistId))
                .orElseThrow(() -> new BadRequestException("Therapist profile not found"));

        // Check for therapist conflicts
        // Calculate session end time for query
        Instant checkEnd = sessionEnd;
        // Depending on query implementation, we might need to adjust start/end slightly
        // Existing query: s.sessionDate >= :startTime AND s.sessionDate < :endTime
        // This only checks if a session STARTS in the interval.
        // We need robust overlap check.
        // Let's use the query logic I verified/added in SessionRepository.
        // findConflictingTherapistSessions checks: sessionDate >= start AND sessionDate
        // < end.
        // That creates a gap where a session started BEFORE start end ends AFTER start
        // is missed.
        // However, standard overlap logic is: (StartA < EndB) and (EndA > StartB).
        // Let's use the method I verified earlier: findConflictingTherapistSessions
        // Wait, the query in SessionRepository was:
        // WHERE s.therapist.id = :therapistId ... AND s.sessionDate >= :startTime AND
        // s.sessionDate < :endTime
        // That is INSUFFICIENT for full overlap check (e.g. valid slots).
        // But for booking, we usually check if the slot is free.
        // Let's rely on findConflictingTherapistSessions but create a better query if
        // needed.
        // Actually, for RoomService checkDetailedAvailability I reused existing
        // queries.
        // Let's stick to what is available or improve it if I can.
        // The existing `findAll()` logic was:
        // return sessionDateTime.isBefore(sEnd) && sessionEnd.isAfter(sStart);
        // This is standard overlap.
        // `findConflictingTherapistSessions` is NOT checking overlap correctly for
        // sessions starting before.
        // I should use `countOverlappingSessionsForRoom` equivalent for Therapist if
        // possible, or fetch a range.
        // For now, I will use `findByTherapistAndDateRange` which sorts by date, but I
        // should simply filter properly.
        // actually, let's use `findConflictingTherapistSessions` but with a wider
        // window? No.
        // Best approach: Use the `findConflictingTherapistSessions` but acknowledging
        // its limitation OR
        // Use `findByTherapistAndDateRange` with a wider window (e.g. -8 hours to +8
        // hours) and filter in memory?
        // OR add `countOverlappingSessionsForTherapist` to repository?
        // Given I cannot easily add new methods to Repo without going back, let's look
        // at SessionRepository again.
        // It has `findConflictingTherapistSessions`.
        // Let's assume it works for "conflicting start times".
        // Wait, I saw the query: `s.sessionDate >= :startTime AND s.sessionDate <
        // :endTime`.
        // This means it finds sessions that START during the requested slot.
        // It does NOT find a 2-hour session that started 30 mins ago.
        // That IS a bug in the system if true.
        // BUT, `findAll()` was doing `isBefore(sEnd) && isAfter(sStart)`.
        // I will use `findByTherapistAndDateRange` with
        // `sessionDateTime.minus(selectedDuration)` to `sessionEnd`.
        // This covers sessions starting before but ending in our window.
        // `findByTherapistAndDateRange` query: `sessionDate >= :startDate AND
        // sessionDate < :endDate`.

        // Let's define the search window to be safe: ensure we catch any session that
        // could overlap.
        // Max session is 8 hours (480 mins).
        Instant searchStart = sessionDateTime.minus(480, java.time.temporal.ChronoUnit.MINUTES);
        Instant searchEnd = sessionEnd;

        List<Session> candidateSessions = sessionRepositoryForAppointments.findByTherapistAndDateRange(
                therapistId, searchStart, searchEnd);

        boolean hasTherapistConflict = candidateSessions.stream()
                .filter(s -> SystemOptionKeyMatcher.matchesAny(s.getStatus(), "scheduled", "confirmed", "in_progress", "in-progress"))
                .anyMatch(s -> {
                    Instant sStart = s.getSessionDate();
                    int sDuration = s.getDuration() != null ? s.getDuration() : 60;
                    Instant sEnd = sStart.plusSeconds(sDuration * 60L);
                    return sessionDateTime.isBefore(sEnd) && sessionEnd.isAfter(sStart);
                });

        if (hasTherapistConflict) {
            throw new BadRequestException("This time slot is no longer available. Please select another time.");
        }

        // Validate session mode
        if (!StringUtils.hasText(request.getSessionType())) {
            throw new BadRequestException("Session mode is required");
        }
        String sessionModeKey = systemOptionResolverService.requireOptionKey(
                SystemOptionCategories.SESSION_MODE, request.getSessionType());

        // The offered slot list is advisory; availability is enforced here so a request
        // built by hand cannot book outside the hours the therapist actually offers.
        validateWithinTherapistAvailability(
                therapistProfile, service, therapistId, sessionModeKey, sessionDateTime, sessionEnd);

        // Assign room from therapist schedule (clients cannot pick a room)
        Room assignedRoom = resolveRoomFromTherapistSchedule(
                therapistId,
                therapistProfile,
                sessionModeKey,
                sessionDateTime,
                sessionDuration,
                null);
        SessionIntegration zoomIntegration = null;
        String zoomJoinUrl = null;
        String zoomPassword = null;
        String zoomMeetingId = null;

        if (SystemOptionKeyMatcher.matchesAny(sessionModeKey, "online")) {
            // Create Zoom meeting for online sessions
            if (zoomService != null && zoomService.isTherapistConfigured(client.getAssignedTherapist())) {
                // Consume quota only when a meeting is actually going to be created.
                // Charging before the configuration check burns credits for therapists
                // without Zoom and eventually blocks their bookings on an unusable feature.
                Long orgId = TenantContext.getOrganisationId();
                if (orgId != null) {
                    subscriptionFeatureService.consumeUsageOrThrow(
                            orgId,
                            String.valueOf(therapistId),
                            SubscriptionFeatureService.FEATURE_ZOOM_SESSIONS_PER_MONTH,
                            1L,
                            "Zoom session creation");
                }

                try {
                    // Build Zoom meeting request
                    Map<String, Object> zoomRequest = new HashMap<>();
                    ZoneId therapistTz = timezoneService.getTherapistTimezone(therapistId)
                            .orElse(ZoneId.systemDefault());
                    zoomRequest.put("topic", "Therapy Session with " + client.getAssignedTherapist().getFullName());
                    zoomRequest.put("startTime", sessionDateTime);
                    zoomRequest.put("duration", sessionDuration);
                    zoomRequest.put("timezone", therapistTz.getId());
                    zoomRequest.put("therapistName", client.getAssignedTherapist().getFullName());
                    zoomRequest.put("settings", Map.of(
                            "waiting_room", true,
                            "video_host", true,
                            "video_participant", true,
                            "mute_upon_entry", true));

                    ZoomMeetingResponse zoomMeeting = zoomService.createMeeting(
                            zoomRequest, client.getAssignedTherapist());

                    zoomMeetingId = zoomMeeting.getMeetingId();
                    zoomJoinUrl = zoomMeeting.getJoinUrl();
                    zoomPassword = zoomMeeting.getPassword();

                    // Create SessionIntegration for Zoom
                    zoomIntegration = SessionIntegration.builder()
                            .session(null) // Will be set after session is saved
                            .provider("zoom")
                            .meetingId(zoomMeetingId)
                            .joinUrl(zoomJoinUrl)
                            .password(zoomPassword)
                            .build();

                    log.info("Zoom meeting created for session: meetingId={}, therapistId={}", 
                            zoomMeetingId, therapistId);
                } catch (Exception e) {
                    log.error("Failed to create Zoom meeting for online session", e);
                    throw new BadRequestException("Failed to create Zoom meeting. Please try again or contact support.");
                }
            } else {
                log.warn("Online booking rejected: therapist {} has no active Zoom integration", therapistId);
                throw new BadRequestException(ONLINE_BOOKING_UNAVAILABLE_MESSAGE);
            }
        }

        // Create session
        Session newSession = Session.builder()
                .client(client)
                .therapist(client.getAssignedTherapist())
                .service(service)
                .room(assignedRoom)
                .sessionDate(sessionDateTime)
                .duration(sessionDuration)
                .sessionType(sessionModeKey)
                .clinicalSessionType(service.getCategory() != null ? service.getCategory() : service.getServiceName())
                .status("scheduled")
                .notes(request.getLocation())
                .build();

        // Add Zoom integration if created
        if (zoomIntegration != null) {
            zoomIntegration.setSession(newSession);
            if (newSession.getIntegrations() == null) {
                newSession.setIntegrations(new ArrayList<>());
            }
            newSession.getIntegrations().add(zoomIntegration);
        }

        Session saved = sessionRepositoryForAppointments.save(newSession);

        // Get client's timezone for date/time formatting (required)
        // Reload client to ensure we have the latest timezone (in case it was just
        // updated)
        Client clientWithTimezone = clientRepository.findById(client.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        // Display follows the client's own timezone setting (clinic default when unset).
        ZoneId clientTimezone = timezoneService.resolveClientPortalZone(clientWithTimezone.getId(), null);

        DateTimeFormatter dateFormatter = DATE_FORMATTER_PATTERN.withZone(clientTimezone);
        DateTimeFormatter timeFormatter = TIME_FORMATTER_PATTERN.withZone(clientTimezone);

        // Format response dates in client's timezone
        // Convert Instant to ZonedDateTime in client's timezone for accurate formatting
        java.time.ZonedDateTime zonedDateTime = saved.getSessionDate().atZone(clientTimezone);
        String sessionDateEST = zonedDateTime.format(dateFormatter);
        String sessionTimeEST = zonedDateTime.format(timeFormatter);

        // Log for debugging timezone issues
        log.info(
                "Book appointment - stored UTC time: {}, client timezone: {}, zoned time: {}, formatted time: {}",
                saved.getSessionDate(), clientTimezone, zonedDateTime, sessionTimeEST);

        // Audit appointment booking
        recordAuditEvent(client.getId(),
                getClientEmail(client),
                "session_created", ipAddress, userAgent, "success",
                String.format(
                        "{\"mrn\":\"%s\", \"sessionDate\":\"%s\", \"sessionTime\":\"%s\", \"sessionType\":\"%s\", \"bookedByClient\":true}",
                        client.getClientId(), sessionDateEST, sessionTimeEST, request.getSessionType() != null ? request.getSessionType() : "unknown"),
                true);

        // Trigger notification for all parties (client, therapist, admin, supervisor)
        if (notificationService != null) {
            try {
                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("id", saved.getId());
                notificationData.put("clientId", saved.getClient().getId());
                notificationData.put("therapistId", saved.getTherapist().getId());
                NotificationPayloadFactory.putClientIdentity(notificationData, client);
                notificationData.put("therapistName", saved.getTherapist().getFullName());
                notificationData.put("sessionDate", saved.getSessionDate());
                notificationData.put("sessionType", resolveClinicalSessionType(saved));
                notificationData.put("sessionMode", resolveSessionMode(saved));
                notificationData.put("roomId", saved.getRoom() != null ? saved.getRoom().getId() : null);
                notificationData.put("duration", saved.getDuration());
                notificationData.put("bookedByClient", true);
                notificationData.put("zoomEnabled", zoomJoinUrl != null);
                notificationData.put("zoomJoinUrl", zoomJoinUrl);
                notificationData.put("zoomPassword", zoomPassword);
                populateBookingEmailDetails(notificationData, saved, clientTimezone, zoomJoinUrl, zoomPassword);

                // Dispatch only after booking commits; notification failures must not roll it back.
                notificationService.processEventInNewTransaction(NotificationEventCatalog.SESSION_SCHEDULED, notificationData);
            } catch (Exception e) {
                log.error("Failed to trigger session_scheduled notification", e);
                // Don't fail the booking if notification fails
            }
        }

        return com.smart.therapy.flow.client.portal.dto.BookAppointmentResponse.builder()
                .message("Appointment booked successfully")
                .appointment(com.smart.therapy.flow.client.portal.dto.BookAppointmentResponse.AppointmentInfo.builder()
                        .id(saved.getId())
                        .sessionDate(sessionDateEST)
                        .sessionTime(sessionTimeEST)
                        .duration(saved.getDuration())
                        .sessionType(resolveClinicalSessionType(saved))
                        .sessionMode(resolveSessionMode(saved))
                        .status(saved.getStatus() != null ? saved.getStatus() : null)
                        .location(determineLocation(saved))
                        .zoomEnabled(zoomJoinUrl != null)
                        .zoomJoinUrl(zoomJoinUrl)
                        .zoomPassword(zoomPassword)
                        .build())
                .build();
    }

    // ========== GET SINGLE APPOINTMENT ==========

    @Transactional(readOnly = true)
    public PortalAppointmentResponse getAppointment(Long appointmentId, String ipAddress, String userAgent) {
        Objects.requireNonNull(appointmentId, "Appointment ID is required");

        Client client = getCurrentClientEntity(); // Already throws ResourceNotFoundException if not found

        // Get session and verify it belongs to the client
        Session session = sessionRepositoryForAppointments.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        // Verify session belongs to the authenticated client
        if (session.getClient() == null || !session.getClient().getId().equals(client.getId())) {
            throw new UnauthorizedException("You can only view your own appointments");
        }

        // Get room and service for lookup
        Room room = session.getRoom() != null ? roomRepository.findById(session.getRoom().getId()).orElse(null) : null;
        com.smart.therapy.flow.billing.entity.Service service = session.getService() != null
                ? serviceRepository.findById(session.getService().getId()).orElse(null)
                : null;

        // Get client's timezone for date/time formatting (profile → UTC)
        ZoneId clientTimezone = timezoneService.resolveClientPortalZone(client.getId(), null);
        DateTimeFormatter dateFormatter = DATE_FORMATTER_PATTERN.withZone(clientTimezone);
        DateTimeFormatter timeFormatter = TIME_FORMATTER_PATTERN.withZone(clientTimezone);

        // Determine location: check session type, zoom enabled, or if room is
        // therapist's virtual room
        String location = determineLocation(session);

        // Convert Instant to ZonedDateTime in client's timezone for accurate formatting
        java.time.ZonedDateTime zonedDateTime = session.getSessionDate().atZone(clientTimezone);
        SessionNote ratedNote = sessionNoteRepository.findBySessionId(appointmentId).stream()
                .filter(note -> note.getClientRating() != null)
                .findFirst()
                .orElse(null);

        PortalAppointmentResponse appointment = PortalAppointmentResponse.builder()
                .id(session.getId())
                .sessionDate(zonedDateTime.format(dateFormatter))
                .sessionTime(zonedDateTime.format(timeFormatter))
                .duration(session.getDuration() != null ? session.getDuration()
                        : (service != null ? service.getDuration() : null))
                .sessionType(resolveClinicalSessionType(session))
                .sessionMode(resolveSessionMode(session))
                .status(session.getStatus() != null ? session.getStatus() : null)
                .location(location)
                .roomName(location.equals("Online") ? "Virtual Room"
                        : (room != null ? (room.getRoomName() != null ? room.getRoomName() : room.getRoomNumber())
                                : null))
                .referenceNumber(client.getReferral() != null && client.getReferral().getReferenceNumber() != null
                        ? client.getReferral().getReferenceNumber()
                        : client.getClientId())
                .serviceCode(service != null ? service.getServiceCode() : null)
                .serviceName(service != null ? service.getServiceName() : null)
                .serviceRate(service != null ? service.getBaseRate() : null)
                .therapistName(session.getTherapist() != null ? session.getTherapist().getFullName() : null)
                .clientRating(ratedNote != null ? ratedNote.getClientRating() : null)
                .clientRatingComment(resolveClientRatingComment(ratedNote))
                .build();

        // Audit appointment access (in separate transaction, wrapped to not fail
        // read-only transaction)
        try {
            recordAuditEvent(client.getId(),
                    getClientEmail(client),
                    "appointment_viewed", ipAddress, userAgent, "success",
                    String.format("Portal single appointment viewing - appointmentId: %d", appointmentId), true);
        } catch (Exception e) {
            // Log but don't fail the read operation if audit logging fails
            log.warn("Failed to record audit event for single appointment viewing: {}", e.getMessage());
        }

        return appointment;
    }

    // ========== CANCEL APPOINTMENT ==========

    @Transactional
    public CancelAppointmentResponse cancelAppointment(Long appointmentId, String ipAddress, String userAgent) {
        Objects.requireNonNull(appointmentId, "Appointment ID is required");

        Client client = getCurrentClientEntity(); // Already throws ResourceNotFoundException if not found

        // Get session and verify it belongs to the client
        Session session = sessionRepositoryForAppointments.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        // Verify session belongs to the authenticated client
        if (session.getClient() == null || !session.getClient().getId().equals(client.getId())) {
            throw new UnauthorizedException("You can only cancel your own appointments");
        }

        // Check if appointment can be cancelled
        String currentStatus = session.getStatus();
        if (currentStatus == null) {
            throw new BadRequestException("Appointment status is invalid");
        }

        if (SystemOptionKeyMatcher.matchesAny(currentStatus, "cancelled")) {
            throw new BadRequestException("Appointment is already cancelled");
        }

        if (SystemOptionKeyMatcher.matchesAny(currentStatus, "completed")) {
            throw new BadRequestException("Cannot cancel a completed appointment");
        }

        if (session.getBilling() != null) {
            throw new BadRequestException("An appointment with an existing bill cannot be cancelled");
        }

        // Update status to cancelled
        String oldStatus = currentStatus;
        session.setStatus("cancelled");
        Session updated = sessionRepositoryForAppointments.save(session);

        // Get client's timezone for date/time formatting (profile → UTC)
        ZoneId clientTimezone = timezoneService.resolveClientPortalZone(client.getId(), null);
        DateTimeFormatter dateFormatter = DATE_FORMATTER_PATTERN.withZone(clientTimezone);
        DateTimeFormatter timeFormatter = TIME_FORMATTER_PATTERN.withZone(clientTimezone);

        // Format response dates in client's timezone
        // Convert Instant to ZonedDateTime in client's timezone for accurate formatting
        java.time.ZonedDateTime zonedDateTime = updated.getSessionDate().atZone(clientTimezone);
        String sessionDateEST = zonedDateTime.format(dateFormatter);
        String sessionTimeEST = zonedDateTime.format(timeFormatter);

        // Audit appointment cancellation
        recordAuditEvent(client.getId(),
                getClientEmail(client),
                "appointment_cancelled", ipAddress, userAgent, "success",
                String.format(
                        "Portal appointment cancellation - appointmentId: %d, oldStatus: %s, sessionDate: %s, sessionTime: %s",
                        appointmentId, oldStatus, sessionDateEST, sessionTimeEST),
                true);

        // Trigger notification
        if (notificationService != null) {
            try {
                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("id", updated.getId());
                notificationData.put("clientId", updated.getClient() != null ? updated.getClient().getId() : null);
                notificationData.put("therapistId",
                        updated.getTherapist() != null ? updated.getTherapist().getId() : null);
                NotificationPayloadFactory.putClientIdentity(notificationData, client);
                notificationData.put("therapistName",
                        updated.getTherapist() != null ? updated.getTherapist().getFullName() : null);
                notificationData.put("sessionDate", updated.getSessionDate());
                notificationData.put("sessionType", resolveClinicalSessionType(updated));
                notificationData.put("sessionMode", resolveSessionMode(updated));
                notificationData.put("oldStatus", oldStatus);
                notificationData.put("cancelledByClient", true);

                notificationService.processEvent(NotificationEventCatalog.SESSION_CANCELLED, notificationData);
            } catch (Exception e) {
                log.error("Failed to trigger session_cancelled notification for appointment {}", appointmentId, e);
                // Don't fail the cancellation if notification fails
            }
        }

        return CancelAppointmentResponse.builder()
                .message("Appointment cancelled successfully")
                .appointmentId(updated.getId())
                .status(updated.getStatus() != null ? updated.getStatus() : null)
                .build();
    }

    // ========== RESCHEDULE APPOINTMENT ==========

    @Transactional
    public RescheduleAppointmentResponse rescheduleAppointment(
            Long appointmentId,
            RescheduleAppointmentRequest request,
            String ipAddress,
            String userAgent) {
        Objects.requireNonNull(appointmentId, "Appointment ID is required");
        Objects.requireNonNull(request, "Request is required");

        // Validate request fields
        if (!StringUtils.hasText(request.getNewSessionStartUtc())) {
            throw new BadRequestException("New session start time is required");
        }

        Client client = getCurrentClientEntity(); // Already throws ResourceNotFoundException if not found

        if (client.getAssignedTherapist() == null) {
            throw new BadRequestException("No therapist assigned to your account");
        }

        // Get session and verify it belongs to the client
        Session session = sessionRepositoryForAppointments.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        // Verify session belongs to the authenticated client
        if (session.getClient() == null || !session.getClient().getId().equals(client.getId())) {
            throw new UnauthorizedException("You can only reschedule your own appointments");
        }

        // Check if appointment can be rescheduled
        String currentStatus = session.getStatus() != null ? session.getStatus() : null;
        if (currentStatus == null) {
            throw new BadRequestException("Appointment status is invalid");
        }

        if ("cancelled".equalsIgnoreCase(currentStatus)) {
            throw new BadRequestException("Cannot reschedule a cancelled appointment");
        }

        if ("completed".equalsIgnoreCase(currentStatus)) {
            throw new BadRequestException("Cannot reschedule a completed appointment");
        }

        Long therapistId = client.getAssignedTherapist().getId();
        Instant oldSessionDate = session.getSessionDate();

        // Parse new session start time (accepts ISO 8601 with or without timezone
        // offset)
        // If timezone offset is provided, it's interpreted as the client's local time
        // If no offset (ends with Z), it's interpreted as UTC
        Instant newSessionDateTime;
        try {
            String timeString = request.getNewSessionStartUtc().trim();

            // If it has a timezone offset (e.g., +05:00, -05:00), parse as OffsetDateTime
            // This means the client is sending their local time with timezone
            if (timeString.contains("+") || (timeString.contains("-") && timeString.lastIndexOf("-") > 10)) {
                // Has timezone offset - parse as OffsetDateTime and convert to UTC
                // The offset pins the instant, nothing else. A client's timezone is their own
                // setting; it is never inferred from a request offset, which spans many zones.
                newSessionDateTime = java.time.OffsetDateTime.parse(timeString).toInstant();
            } else if (timeString.endsWith("Z")) {
                // Explicitly UTC - parse directly
                newSessionDateTime = Instant.parse(timeString);
            } else {
                // No timezone specified - assume it's in client's timezone
                // Parse as local time and convert using client's timezone
                ZoneId clientTimezone = timezoneService.getClientTimezone(client.getId())
                        .orElseThrow(() -> new BadRequestException(
                                "Client timezone is not configured. Please set timezone in client profile."));
                java.time.LocalDateTime localDateTime = java.time.LocalDateTime.parse(timeString);
                newSessionDateTime = localDateTime.atZone(clientTimezone).toInstant();
            }
        } catch (java.time.format.DateTimeParseException e) {
            log.warn("Invalid date format provided: {}", request.getNewSessionStartUtc(), e);
            throw new BadRequestException(
                    "Invalid session date format. Expected ISO 8601 format (e.g., 2025-01-24T14:30:00Z or 2025-01-24T14:30:00+05:00).");
        } catch (Exception e) {
            log.error("Unexpected error parsing session date: {}", request.getNewSessionStartUtc(), e);
            throw new BadRequestException("Invalid session date format. Expected ISO 8601 format.");
        }

        // Validate that new session date is not in the past
        if (newSessionDateTime.isBefore(Instant.now())) {
            throw new BadRequestException("Cannot reschedule appointment to a time in the past");
        }

        // Get duration (use existing if not provided)
        int sessionDuration = request.getDuration() != null ? request.getDuration()
                : (session.getDuration() != null ? session.getDuration() : 60);

        // Validate duration
        if (sessionDuration <= 0) {
            throw new BadRequestException("Session duration must be greater than 0 minutes");
        }
        if (sessionDuration > 480) { // 8 hours max
            throw new BadRequestException("Session duration cannot exceed 8 hours (480 minutes)");
        }

        Instant newSessionEnd = newSessionDateTime.plusSeconds(sessionDuration * 60L);

        // Get therapist profile for room assignment (eager-load schedule rooms)
        UserProfile therapistProfile = userProfileRepository.findByUserIdWithRooms(therapistId)
                .or(() -> userProfileRepository.findByUserId(therapistId))
                .orElseThrow(() -> new BadRequestException("Therapist profile not found"));

        // Check for therapist conflicts (exclude current session)
        List<Session> therapistConflicts = sessionRepositoryForAppointments.findAll()
                .stream()
                .filter(s -> s.getTherapist() != null && s.getTherapist().getId().equals(therapistId))
                .filter(s -> !s.getId().equals(appointmentId)) // Exclude current session
                .filter(s -> SystemOptionKeyMatcher.matchesAny(s.getStatus(), "scheduled", "confirmed", "in_progress", "in-progress"))
                .filter(s -> {
                    Instant sStart = s.getSessionDate();
                    int sDuration = s.getDuration() != null ? s.getDuration() : 60;
                    Instant sEnd = sStart.plusSeconds(sDuration * 60L);
                    return newSessionDateTime.isBefore(sEnd) && newSessionEnd.isAfter(sStart);
                })
                .collect(Collectors.toList());

        if (!therapistConflicts.isEmpty()) {
            throw new BadRequestException("This time slot is no longer available. Please select another time.");
        }

        String sessionModeKey = systemOptionResolverService.requireOptionKey(
                SystemOptionCategories.SESSION_MODE,
                session.getSessionType() != null ? session.getSessionType() : "in-person");

        // Re-assign room from therapist schedule for the new slot
        Room assignedRoom = resolveRoomFromTherapistSchedule(
                therapistId,
                therapistProfile,
                sessionModeKey,
                newSessionDateTime,
                sessionDuration,
                appointmentId);

        // Update session
        session.setSessionDate(newSessionDateTime);
        if (request.getDuration() != null) {
            session.setDuration(sessionDuration);
        }
        session.setRoom(assignedRoom);

        Session updated = sessionRepositoryForAppointments.save(session);

        // Get client's timezone for date/time formatting (required)
        // Reload client to ensure we have the latest timezone (in case it was just
        // updated)
        Client clientWithTimezone = clientRepository.findById(client.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        // Display follows the client's own timezone setting (clinic default when unset).
        ZoneId clientTimezone = timezoneService.resolveClientPortalZone(clientWithTimezone.getId(), null);

        DateTimeFormatter dateFormatter = DATE_FORMATTER_PATTERN.withZone(clientTimezone);
        DateTimeFormatter timeFormatter = TIME_FORMATTER_PATTERN.withZone(clientTimezone);

        // Format response dates in client's timezone
        // Convert Instant to ZonedDateTime in client's timezone for accurate formatting
        java.time.ZonedDateTime zonedDateTime = updated.getSessionDate().atZone(clientTimezone);
        String sessionDateEST = zonedDateTime.format(dateFormatter);
        String sessionTimeEST = zonedDateTime.format(timeFormatter);

        // Log for debugging timezone issues
        log.info(
                "Reschedule appointment - stored UTC time: {}, client timezone: {}, zoned time: {}, formatted time: {}",
                updated.getSessionDate(), clientTimezone, zonedDateTime, sessionTimeEST);

        // Audit appointment rescheduling
        java.time.ZonedDateTime oldZonedDateTime = oldSessionDate.atZone(clientTimezone);
        recordAuditEvent(client.getId(),
                getClientEmail(client),
                "appointment_rescheduled", ipAddress, userAgent, "success",
                String.format(
                        "Portal appointment rescheduling - appointmentId: %d, oldDate: %s, newDate: %s, newTime: %s",
                        appointmentId, oldZonedDateTime.format(dateFormatter), sessionDateEST, sessionTimeEST),
                true);

        // Trigger notification
        if (notificationService != null) {
            try {
                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("id", updated.getId());
                notificationData.put("clientId", updated.getClient().getId());
                notificationData.put("therapistId",
                        updated.getTherapist() != null ? updated.getTherapist().getId() : null);
                NotificationPayloadFactory.putClientIdentity(notificationData, client);
                notificationData.put("therapistName",
                        updated.getTherapist() != null ? updated.getTherapist().getFullName() : null);
                notificationData.put("sessionDate", updated.getSessionDate());
                notificationData.put("sessionType", resolveClinicalSessionType(updated));
                notificationData.put("sessionMode", resolveSessionMode(updated));
                notificationData.put("oldDate", oldSessionDate);
                notificationData.put("rescheduledByClient", true);

                notificationService.processEvent(NotificationEventCatalog.SESSION_RESCHEDULED, notificationData);
            } catch (Exception e) {
                log.error("Failed to trigger session_rescheduled notification for appointment {}", appointmentId, e);
                // Don't fail the rescheduling if notification fails
            }
        }

        return RescheduleAppointmentResponse.builder()
                .message("Appointment rescheduled successfully")
                .appointment(RescheduleAppointmentResponse.AppointmentInfo.builder()
                        .id(updated.getId())
                        .sessionDate(sessionDateEST)
                        .sessionTime(sessionTimeEST)
                        .duration(updated.getDuration())
                        .sessionType(resolveClinicalSessionType(updated))
                        .sessionMode(resolveSessionMode(updated))
                        .status(updated.getStatus() != null ? updated.getStatus() : null)
                        .location(determineLocation(updated))
                        .build())
                .build();
    }

    // ========== PAY INVOICE ==========

    /**
     * Intentionally not {@code @Transactional}: Stripe checkout is an external HTTP call and must not
     * hold a DB connection. Persisting the checkout session id uses {@link BillingService}'s own TX;
     * audit is best-effort and must not block returning the checkout URL.
     */
    public com.smart.therapy.flow.client.portal.dto.PayInvoiceResponse payInvoice(
            Long invoiceId,
            String ipAddress,
            String userAgent) {
        Objects.requireNonNull(invoiceId, "Invoice ID is required");

        if (stripeService == null) {
            throw new BadRequestException("Payment system not configured");
        }
        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null && !subscriptionFeatureService.isFeatureEnabled(
                orgId,
                SubscriptionFeatureService.FEATURE_STRIPE_PAYMENTS,
                null)) {
            throw new BadRequestException("Stripe payments are not enabled for your plan.");
        }

        // Get current authenticated client
        Client client = getCurrentClientEntity();
        String clientEmail = getClientEmail(client);

        // Get the invoice (must belong to this client)
        List<com.smart.therapy.flow.billing.entity.SessionBilling> clientInvoices = sessionBillingRepository
                .findByClientId(client.getId());
        com.smart.therapy.flow.billing.entity.SessionBilling invoice = clientInvoices.stream()
                .filter(inv -> inv.getId().equals(invoiceId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found or access denied"));

        // Check if already paid (use billingStatus instead of paymentStatus)
        if (invoice.getBillingStatus() == com.smart.therapy.flow.billing.enums.BillingStatus.PAID) {
            throw new BadRequestException("Invoice already paid");
        }

        java.math.BigDecimal outstandingAmount = billingService != null
                ? billingService.resolveOutstandingAmount(invoice)
                : (invoice.getOutstandingAmount() != null ? invoice.getOutstandingAmount() : invoice.getTotalAmount());
        if (outstandingAmount == null || outstandingAmount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Invoice has no outstanding balance");
        }

        // Get service name
        com.smart.therapy.flow.billing.entity.Service service = serviceRepository
                .findByServiceCode(invoice.getServiceCode())
                .orElse(null);
        String serviceName = service != null ? service.getServiceName() : invoice.getServiceCode();

        // Format session date in client's timezone
        String sessionDateStr = "N/A";
        if (invoice.getSession() != null && invoice.getSession().getSessionDate() != null) {
            ZoneId clientTimezone = timezoneService.resolveClientPortalZone(client.getId(), null);
            DateTimeFormatter dateFormatter = DATE_FORMATTER_PATTERN.withZone(clientTimezone);
            sessionDateStr = dateFormatter.format(invoice.getSession().getSessionDate());
        }

        // Create Stripe checkout session (no open portal TX held across this call)
        com.smart.therapy.flow.payment.dto.StripeCheckoutResponse checkoutResponse = stripeService
                .createCheckoutSession(
                        invoice.getId(),
                        client.getId(),
                        outstandingAmount.toPlainString(),
                        serviceName,
                        invoice.getServiceCode(),
                        invoice.getSession() != null && invoice.getSession().getSessionType() != null ? invoice.getSession().getSessionType() : "online",
                        sessionDateStr,
                        clientEmail);

        if (billingService != null) {
            billingService.attachStripeCheckoutSession(invoice.getId(), client.getId(), checkoutResponse.getSessionId());
        }

        final Long auditClientId = client.getId();
        final Long auditOrgId = TenantContext.getOrganisationId();
        final String auditSchema = TenantContext.getSchemaName();
        final String auditDetails = String.format(
                "Portal payment initiation - invoiceId: %d, amount: %s, stripeSessionId: %s",
                invoice.getId(), outstandingAmount, checkoutResponse.getSessionId());
        CompletableFuture.runAsync(() -> {
            try {
                if (auditOrgId != null) {
                    TenantContext.setOrganisationId(auditOrgId);
                }
                if (auditSchema != null) {
                    TenantContext.setSchemaName(auditSchema);
                }
                recordAuditEvent(auditClientId, clientEmail, BillingAuditActions.PAYMENT_INITIATED,
                        ipAddress, userAgent, "success", auditDetails, true);
            } catch (Exception e) {
                log.warn("Async portal payment audit failed: clientId={}, error={}",
                        auditClientId, e.getMessage());
            } finally {
                TenantContext.clear();
            }
        });

        return com.smart.therapy.flow.client.portal.dto.PayInvoiceResponse.builder()
                .sessionId(checkoutResponse.getSessionId())
                .checkoutUrl(checkoutResponse.getCheckoutUrl())
                .build();
    }

    // ========== DOCUMENTS (list in PortalDocumentService) ==========

    @Transactional
    public com.smart.therapy.flow.document.dto.DocumentResponse uploadDocument(
            MultipartFile file,
            String documentType,
            String ipAddress,
            String userAgent) {
        Objects.requireNonNull(file, "File is required");

        if (storageService == null) {
            throw new BadRequestException("File storage service not configured");
        }

        Client client = getCurrentClientEntity();

        // Validate file
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        long maxFileSize = 50 * 1024 * 1024; // 50MB
        if (file.getSize() > maxFileSize) {
            throw new BadRequestException("File size exceeds maximum allowed size of 50MB");
        }

        try {
            // Upload to storage
            String storagePath = storageService.uploadFile(file, String.valueOf(client.getId()),
                    file.getOriginalFilename());
            // String fileName = storagePath.substring(storagePath.lastIndexOf('/') + 1);
            // For S3, storagePath is the full S3 key (e.g.,
            // "clients/123/uuid_filename.pdf")
            // Store the full storage path to enable file retrieval

            // Derive category from documentType if provided, otherwise default to
            // "uploaded"
            String finalCategory = "uploaded"; // Default category
            if (documentType != null && !documentType.trim().isEmpty()) {
                String derivedCategory = deriveCategoryFromDocumentType(documentType);
                if (derivedCategory != null) {
                    finalCategory = derivedCategory;
                }
            }

            DocumentType docType = PortalDocumentMapper.parseDocumentType(documentType);

            // Create document record (uploaded by client - uploadedBy is null)
            // Client uploads should be reviewed by staff
            Instant reviewDueAt = Instant.now().plus(defaultReviewDays, ChronoUnit.DAYS);
            Document document = Document.builder()
                    .client(client)
                    .uploadedBy(null) // Null for client uploads
                    .fileName(storagePath) // Store full S3 key, not just filename
                    .originalName(file.getOriginalFilename())
                    .fileSize((int) file.getSize())
                    .mimeType(file.getContentType())
                    .category(DocumentCategory.valueOf(finalCategory.toUpperCase())) // Derived from documentType or
                                                                                     // default to "uploaded"
                    .documentType(docType) // Set from client input (validated)
                    .description(null) // Can be set later by staff if needed
                    .isSharedInPortal(true) // Always share portal uploads
                    .needsReview(true) // Client uploads need review
                    .reviewStatus(ReviewStatus.THERAPIST_REVIEW) // Default for client uploads
                    .reviewDueAt(reviewDueAt)
                    .downloadCount(0)
                    .build();

            Document saved = documentRepository.save(document);
            documentRepository.flush(); // Ensure document is persisted

            if (notificationService != null) {
                try {
                    notificationService.processEvent(
                            NotificationEventCatalog.DOCUMENT_UPLOADED,
                            notificationPayloadFactory.documentUploaded(saved, client.getId(),
                                    HipaaAuditLabels.clientActor(client)));
                } catch (Exception notificationError) {
                    log.error("Failed to trigger portal document_uploaded notification", notificationError);
                }
            }

            // Audit document upload
            recordAuditEvent(client.getId(),
                    getClientEmail(client),
                    "document_uploaded", ipAddress, userAgent, "success",
                    auditEventFactory.documentUploadDetails(file.getOriginalFilename(), file.getSize(), finalCategory,
                            documentType != null ? documentType : "not_specified", "client_portal"),
                    true);

            // Build response with portal preview/download URLs
            return PortalDocumentMapper.toResponse(saved, client);

        } catch (Exception e) {
            log.error("Failed to upload document", e);
            throw new BadRequestException("Failed to upload document: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> rateSession(
            Long sessionId,
            Integer rating,
            String comment,
            String ipAddress,
            String userAgent) {
        if (sessionId == null || rating == null) {
            throw new BadRequestException("sessionId and rating are required");
        }
        Client client = getCurrentClientEntity();
        Session session = sessionRepositoryForAppointments.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (session.getClient() == null || !Objects.equals(session.getClient().getId(), client.getId())) {
            throw new ForbiddenException("You can only rate your own sessions");
        }

        List<SessionNote> notes = sessionNoteRepository.findBySessionId(sessionId);
        boolean alreadyRated = notes.stream().anyMatch(note -> note.getClientRating() != null);
        if (alreadyRated) {
            throw new BadRequestException("You have already rated this session");
        }

        SessionNote note = notes.isEmpty()
                ? SessionNote.builder()
                        .session(session)
                        .client(client)
                        .therapist(session.getTherapist())
                        .date(Instant.now())
                        .build()
                : notes.get(0);
        note.setClientRating(rating);
        if (StringUtils.hasText(comment)) {
            note.setRemarks(comment.trim());
        }
        sessionNoteRepository.save(note);

        String sessionUrl = toFrontendUrl("/sessions/" + sessionId);
        Long taskId = null;
        String taskUrl = null;
        String taskListUrl = toFrontendUrl("/tasks?clientId=" + client.getId() + "&status=pending");
        if (rating <= 4 && session.getTherapist() != null) {
            Task task = Task.builder()
                    .title("Low session rating follow-up")
                    .description("Client rated session " + rating + "/10. Review feedback and follow up. "
                            + "sessionUrl=" + sessionUrl + ", taskListUrl=" + taskListUrl)
                    .status("pending")
                    .priority("high")
                    .dueDate(Instant.now().plus(48, ChronoUnit.HOURS))
                    .client(client)
                    .assignedTo(session.getTherapist())
                    .build();
            Task savedTask = taskRepository.save(task);
            taskId = savedTask.getId();
            taskUrl = toFrontendUrl("/tasks/" + taskId);
        }

        recordAuditEvent(client.getId(), getClientEmail(client), "client_session_rated", ipAddress, userAgent, "success",
                "sessionId=" + sessionId + ", rating=" + rating + ", lowRatingTaskId=" + (taskId != null ? taskId : "none"), true);
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("rating", rating);
        response.put("taskCreated", taskId != null);
        response.put("taskId", taskId != null ? taskId : 0L);
        response.put("sessionUrl", sessionUrl);
        response.put("taskUrl", taskUrl);
        response.put("taskListUrl", taskListUrl);
        return response;
    }

    private String toFrontendUrl(String path) {
        String base = resolveFrontendBaseUrl();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    private String resolveFrontendBaseUrl() {
        try {
            URI uri = URI.create(staffLoginUrl);
            if (uri.getScheme() != null && uri.getHost() != null) {
                int port = uri.getPort();
                if (port > 0) {
                    return uri.getScheme() + "://" + uri.getHost() + ":" + port;
                }
                return uri.getScheme() + "://" + uri.getHost();
            }
        } catch (Exception e) {
            log.warn("Invalid app.frontend.staff-login-url '{}'; using default frontend base URL", staffLoginUrl);
        }
        return "https://app.therapyflow.pro";
    }

    /**
     * Result class for document download operation
     */
    public static class DocumentDownloadResult {
        private final byte[] content;
        private final String mimeType;
        private final String fileName;

        public DocumentDownloadResult(byte[] content, String mimeType, String fileName) {
            this.content = content;
            this.mimeType = mimeType;
            this.fileName = fileName;
        }

        public byte[] getContent() {
            return content;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getFileName() {
            return fileName;
        }
    }

    @Transactional
    public DocumentDownloadResult downloadDocument(
            Long documentId,
            String ipAddress,
            String userAgent) {
        Objects.requireNonNull(documentId, "Document ID is required");

        if (storageService == null) {
            throw new BadRequestException("File storage service not configured");
        }

        Client client = getCurrentClientEntity();
        Document document = documentRepository.findByIdWithRelations(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        verifyPortalDocumentAccess(document, client);

        try {
            int currentCount = document.getDownloadCount() != null ? document.getDownloadCount() : 0;
            document.setDownloadCount(currentCount + 1);
            documentRepository.save(document);

            recordAuditEvent(client.getId(),
                    getClientEmail(client),
                    "document_downloaded", ipAddress, userAgent, "success",
                    String.format("Portal document download - documentId: %d, fileName: %s",
                            documentId, document.getOriginalName()),
                    true);

            byte[] content = readDocumentBytes(document.getFileName());
            return new DocumentDownloadResult(
                    content,
                    document.getMimeType() != null ? document.getMimeType() : "application/octet-stream",
                    document.getOriginalName() != null ? document.getOriginalName() : "document");

        } catch (BadRequestException | ResourceNotFoundException | UnauthorizedException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("Failed to download document", e);
            throw new BadRequestException("Failed to download document: " + e.getMessage());
        }
    }

    /**
     * Soft-delete a document the client uploaded in the portal.
     * Staff-shared documents cannot be deleted by the client.
     */
    @Transactional
    public void deleteDocument(Long documentId, String ipAddress, String userAgent) {
        Objects.requireNonNull(documentId, "Document ID is required");

        Client client = getCurrentClientEntity();
        Document document = documentRepository.findByIdIncludingDeleted(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (document.getClient() == null || !Objects.equals(document.getClient().getId(), client.getId())) {
            throw new UnauthorizedException("Document not found or access denied");
        }
        if (Boolean.TRUE.equals(document.getIsDeleted())) {
            throw new BadRequestException("Document is already deleted");
        }
        // Only allow deleting client-uploaded files (uploadedBy is null for portal uploads)
        if (document.getUploadedBy() != null) {
            throw new ForbiddenException("You can only delete documents you uploaded");
        }

        try {
            if (storageService != null && StringUtils.hasText(document.getFileName())) {
                try {
                    storageService.deleteFile(document.getFileName());
                } catch (Exception storageError) {
                    log.warn("Failed to delete portal document file from storage (id={}): {}",
                            documentId, storageError.getMessage());
                }
            }

            document.setIsDeleted(true);
            document.setDeletedAt(Instant.now());
            documentRepository.save(document);
            documentRepository.flush();

            recordAuditEvent(client.getId(),
                    getClientEmail(client),
                    "document_deleted", ipAddress, userAgent, "success",
                    String.format("Portal document delete - documentId: %d, fileName: %s",
                            documentId, document.getOriginalName()),
                    true);
        } catch (BadRequestException | ResourceNotFoundException | UnauthorizedException | ForbiddenException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("Failed to delete portal document", e);
            throw new BadRequestException("Failed to delete document: " + e.getMessage());
        }
    }

    /**
     * Result class for document view operation
     */
    public static class DocumentViewResult {
        private final byte[] content;
        private final String mimeType;
        private final String fileName;

        public DocumentViewResult(byte[] content, String mimeType, String fileName) {
            this.content = content;
            this.mimeType = mimeType;
            this.fileName = fileName;
        }

        public byte[] getContent() {
            return content;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getFileName() {
            return fileName;
        }
    }

    public static class SignatureImageResult {
        private final byte[] content;
        private final String mimeType;

        public SignatureImageResult(byte[] content, String mimeType) {
            this.content = content;
            this.mimeType = mimeType;
        }

        public byte[] getContent() {
            return content;
        }

        public String getMimeType() {
            return mimeType;
        }
    }

    @Transactional(readOnly = true)
    public DocumentViewResult viewDocument(
            Long documentId,
            String ipAddress,
            String userAgent) {
        Objects.requireNonNull(documentId, "Document ID is required");

        if (storageService == null) {
            throw new BadRequestException("File storage service not configured");
        }

        Client client = getCurrentClientEntity();
        Document document = documentRepository.findByIdWithRelations(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        verifyPortalDocumentAccess(document, client);

        try {
            recordAuditEvent(client.getId(),
                    getClientEmail(client),
                    "document_viewed", ipAddress, userAgent, "success",
                    String.format("Portal document view - documentId: %d, fileName: %s, mimeType: %s",
                            documentId, document.getOriginalName(), document.getMimeType()),
                    true);

            byte[] content = readDocumentBytes(document.getFileName());
            return new DocumentViewResult(
                    content,
                    document.getMimeType() != null ? document.getMimeType() : "application/octet-stream",
                    document.getOriginalName() != null ? document.getOriginalName() : "document");

        } catch (BadRequestException | ResourceNotFoundException | UnauthorizedException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("Failed to view document", e);
            throw new BadRequestException("Failed to view document: " + e.getMessage());
        }
    }

    private void verifyPortalDocumentAccess(Document document, Client client) {
        if (document.getClient() == null || !Objects.equals(document.getClient().getId(), client.getId())) {
            throw new UnauthorizedException("Document not found or access denied");
        }
        boolean isClientUpload = document.getUploadedBy() == null;
        boolean isShared = Boolean.TRUE.equals(document.getIsSharedInPortal());
        if (!isClientUpload && !isShared) {
            throw new UnauthorizedException("Document not found or access denied");
        }
    }

    private byte[] readDocumentBytes(String storagePath) throws Exception {
        try (InputStream inputStream = storageService.downloadFile(storagePath)) {
            return inputStream.readAllBytes();
        }
    }

    // ========== FORMS ==========

    @Transactional(readOnly = true)
    public List<FormAssignmentResponse> getFormAssignments(String ipAddress, String userAgent) {
        Client client = getCurrentClientEntity();
        if (client == null) {
            throw new ResourceNotFoundException("Client not found");
        }

        List<FormAssignment> assignments = formAssignmentRepository.findByClientIdOrderByCreatedAtDesc(client.getId());

        List<FormAssignmentResponse> responses = assignments.stream()
                .map(assignment -> {
                    FormTemplateVersion version = assignment.getTemplateVersion();
                    FormTemplate template = version != null ? version.getTemplate() : null;
                    
                    FormAssignmentResponse.FormAssignmentResponseBuilder builder = FormAssignmentResponse.builder()
                            .id(assignment.getId())
                            .templateId(template != null ? template.getId() : null)
                            .templateVersionId(version != null ? version.getId() : null)
                            .versionNumber(version != null ? version.getVersionNumber() : null)
                            .templateName(template != null ? template.getName() : null)
                            .templateCategory(
                                    template != null && template.getCategory() != null
                                            ? template.getCategory().name()
                                            : null)
                            .clientId(assignment.getClient().getId())
                            .clientName(assignment.getClient().getFullName())
                            .assignedById(
                                    assignment.getAssignedBy() != null ? assignment.getAssignedBy().getId() : null)
                            .assignedByName(
                                    assignment.getAssignedBy() != null ? assignment.getAssignedBy().getFullName()
                                            : null)
                            .status(assignment.getStatus() != null ? assignment.getStatus().name() : null)
                            .dueDate(assignment.getDueDate())
                            .instructions(resolvePortalAssignmentInstructions(assignment))
                            .completedAt(assignment.getCompletedAt())
                            .submittedAt(assignment.getSubmittedAt())
                            .reviewedAt(assignment.getReviewedAt())
                            .reviewedById(
                                    assignment.getReviewedBy() != null ? assignment.getReviewedBy().getId() : null)
                            .reviewedByName(
                                    assignment.getReviewedBy() != null ? assignment.getReviewedBy().getFullName()
                                            : null)
                            .reviewNotes(assignment.getReviewNotes())
                            .remindersSent(assignment.getRemindersSent())
                            .lastReminderAt(assignment.getLastReminderAt())
                            .createdAt(assignment.getCreatedAt())
                            .updatedAt(assignment.getUpdatedAt());

                    // Add responses if available
                    if (assignment.getResponses() != null && !assignment.getResponses().isEmpty()) {
                        List<FormResponseDto> responseDtos = assignment.getResponses().stream()
                                .map(r -> {
                                    FormAssignmentField assignmentField = r.getAssignmentField();
                                    return FormResponseDto.builder()
                                            .id(r.getId())
                                            .assignmentId(r.getAssignment() != null ? r.getAssignment().getId() : null)
                                            .assignmentFieldId(assignmentField != null ? assignmentField.getId() : null)
                                            .fieldId(assignmentField != null && assignmentField.getField() != null 
                                                    ? assignmentField.getField().getId() : null)
                                            .fieldLabel(assignmentField != null ? assignmentField.getFieldLabel() : null)
                                            .fieldType(assignmentField != null ? assignmentField.getFieldType() : null)
                                            .value(r.getResponseValue())
                                            .createdAt(r.getCreatedAt())
                                            .updatedAt(r.getUpdatedAt())
                                            .build();
                                })
                                .collect(Collectors.toList());
                        builder.responses(responseDtos);
                    }

                    // Add signatures if available
                    if (assignment.getSignatures() != null && !assignment.getSignatures().isEmpty()) {
                        List<FormSignatureResponse> signatureDtos = assignment.getSignatures().stream()
                                .map(s -> FormSignatureResponse.builder()
                                        .id(s.getId())
                                        .assignmentId(s.getAssignment().getId())
                                        .signatureData(s.getSignatureData())
                                        .signatureImageUrl(buildSignatureImageUrl(s.getAssignment().getId()))
                                        .signerName(s.getSignerName())
                                        .signerRole(s.getSignerRole())
                                        .signedAt(s.getSignedAt())
                                        .ipAddress(s.getIpAddress())
                                        .userAgent(s.getUserAgent())
                                        .build())
                                .collect(Collectors.toList());
                        builder.signatures(signatureDtos);
                    }

                    return builder.build();
                })
                .collect(Collectors.toList());

        // Audit
        recordAuditEvent(client.getId(),
                getClientEmail(client),
                "forms_list_viewed", ipAddress, userAgent, "success",
                "Portal forms list access - count: " + responses.size(), true);

        return responses;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFormAssignment(Long assignmentId, String ipAddress, String userAgent) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");

        Client client = getCurrentClientEntity();
        if (client == null) {
            throw new ResourceNotFoundException("Client not found");
        }

        FormAssignment assignment = formAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Form assignment not found"));

        // Verify assignment belongs to this client
        if (!assignment.getClient().getId().equals(client.getId())) {
            throw new UnauthorizedException("Form assignment not found or access denied");
        }

        FormTemplateVersion version = assignment.getTemplateVersion();
        FormTemplate template = version != null ? version.getTemplate() : null;
        if (template == null) {
            throw new ResourceNotFoundException("Form template not found");
        }

        // Get assignment fields (snapshots) - these are what the client should see
        List<FormAssignmentField> assignmentFields = formAssignmentFieldRepository
                .findByAssignmentIdAndIsDeletedFalseOrderBySortOrderAsc(assignmentId);
        
        List<FormFieldResponse> fieldResponses = assignmentFields.stream()
                .map(assignmentField -> {
                    FormField sourceField = assignmentField.getField();
                    FormFieldResponse.FormFieldResponseBuilder builder = FormFieldResponse.builder()
                            .id(assignmentField.getId()) // Use assignment field ID for client responses
                            .assignmentFieldId(assignmentField.getId())
                            .templateVersionId(version != null ? version.getId() : null)
                            .sectionId(sourceField != null && sourceField.getSection() != null
                                    ? sourceField.getSection().getId() : null)
                            .sectionName(sourceField != null && sourceField.getSection() != null
                                    ? sourceField.getSection().getName() : null)
                            .label(assignmentField.getFieldLabel()) // From snapshot
                            .fieldType(assignmentField.getFieldType()) // From snapshot
                            .placeholder(sourceField != null ? sourceField.getPlaceholder() : null)
                            .helpText(sourceField != null ? sourceField.getHelpText() : null)
                            .isRequired(assignmentField.getIsRequired()) // From snapshot
                            .defaultValue(sourceField != null ? sourceField.getDefaultValue() : null)
                            .autoPopulate(assignmentField.getAutoPopulate()) // From snapshot
                            .conditionalDisplay(assignmentField.getConditionalDisplay()) // From snapshot
                            .sortOrder(assignmentField.getSortOrder()) // From snapshot
                            .createdAt(assignmentField.getCreatedAt())
                            .updatedAt(assignmentField.getUpdatedAt());

                    // Parse options from JSONB snapshot
                    if (StringUtils.hasText(assignmentField.getOptions())) {
                        try {
                            // Options are stored as JSONB string, parse to list
                            // For now, we'll return the JSON string - frontend can parse
                            // In future, could parse here and return as List<FormFieldOptionResponse>
                            // For backward compatibility, return options as JSON string
                            // Note: FormFieldResponse.options is now List<FormFieldOptionResponse>
                            // but we can't easily convert JSONB string to that without proper parsing
                            // This is a limitation - options should be stored in normalized table
                            builder.options(null); // Options not available in snapshot format
                        } catch (Exception e) {
                            log.warn("Failed to parse options for assignment field {}: {}", 
                                    assignmentField.getId(), e.getMessage());
                        }
                    }

                    return builder.build();
                })
                .collect(Collectors.toList());

        String resolvedInstructions = resolvePortalAssignmentInstructions(assignment);

        // Build response with template, fields, client data, therapist data, and
        // practice data
        Map<String, Object> response = new HashMap<>();
        response.put("id", assignment.getId());
        response.put("templateId", template.getId());
        response.put("clientId", assignment.getClient().getId());
        response.put("status", assignment.getStatus() != null ? assignment.getStatus().name() : null);
        response.put("dueDate", assignment.getDueDate());
        response.put("instructions", resolvedInstructions);
        response.put("completedAt", assignment.getCompletedAt());
        response.put("submittedAt", assignment.getSubmittedAt());
        response.put("createdAt", assignment.getCreatedAt());
        response.put("updatedAt", assignment.getUpdatedAt());

        // Template with fields — instructions live on the assigned version
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("id", template.getId());
        templateData.put("name", template.getName());
        templateData.put("description", template.getDescription());
        templateData.put("category", template.getCategory() != null ? template.getCategory().name() : null);
        templateData.put("instructions", resolvedInstructions);
        templateData.put("requiresSignature", template.getRequiresSignature());
        templateData.put("fields", fieldResponses);
        response.put("template", templateData);

        // Client data
        Map<String, Object> clientData = new HashMap<>();
        clientData.put("fullName", client.getFullName());
        clientData.put("clientId", client.getClientId());
        clientData.put("email", getClientEmail(client));
        clientData.put("phone", getClientPhone(client));
        clientData.put("dateOfBirth", client.getDateOfBirth());
        response.put("clientData", clientData);

        // Therapist data
        if (assignment.getAssignedBy() != null) {
            Map<String, Object> therapistData = new HashMap<>();
            therapistData.put("fullName", assignment.getAssignedBy().getFullName());
            therapistData.put("email", assignment.getAssignedBy().getEmail());
            therapistData.put("phone", assignment.getAssignedBy().getPhone());
            response.put("therapistData", therapistData);
        }

        // Practice data (from system options - simplified for now)
        Map<String, Object> practiceData = new HashMap<>();
        practiceData.put("name", "Resilience Counseling Research & Consultation");
        practiceData.put("address", "111 Waterloo St Unit 406, London, ON N6B 2M4");
        practiceData.put("phone", "+1 (548)866-0366");
        practiceData.put("email", "resiliencecrc@gmail.com");
        practiceData.put("website", "www.resiliencec.com");
        response.put("practiceData", practiceData);

        // Audit
        recordAuditEvent(client.getId(),
                getClientEmail(client),
                "form_viewed", ipAddress, userAgent, "success",
                String.format("Portal form view - assignmentId: %d, formName: %s", assignmentId, template.getName()),
                true);

        return response;
    }

    @Transactional(readOnly = true)
    public List<FormResponseDto> getFormResponses(Long assignmentId) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");

        Client client = getCurrentClientEntity();
        if (client == null) {
            throw new ResourceNotFoundException("Client not found");
        }

        FormAssignment assignment = formAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Form assignment not found"));

        if (!assignment.getClient().getId().equals(client.getId())) {
            throw new UnauthorizedException("Form assignment not found or access denied");
        }

        List<FormResponse> responses = formResponseRepository.findByAssignmentId(assignmentId);
        return responses.stream()
                .map(r -> {
                    FormAssignmentField assignmentField = r.getAssignmentField();
                    return FormResponseDto.builder()
                            .id(r.getId())
                            .assignmentId(r.getAssignment() != null ? r.getAssignment().getId() : null)
                            .assignmentFieldId(assignmentField != null ? assignmentField.getId() : null)
                            .fieldId(assignmentField != null && assignmentField.getField() != null 
                                    ? assignmentField.getField().getId() : null)
                            .fieldLabel(assignmentField != null ? assignmentField.getFieldLabel() : null)
                            .fieldType(assignmentField != null ? assignmentField.getFieldType() : null)
                            .value(r.getResponseValue())
                            .createdAt(r.getCreatedAt())
                            .updatedAt(r.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Long resolveAssignmentFieldId(Long assignmentId, Long templateFieldId) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(templateFieldId, "Field ID is required");

        Client client = getCurrentClientEntity();
        FormAssignment assignment = formAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Form assignment not found"));

        if (!assignment.getClient().getId().equals(client.getId())) {
            throw new UnauthorizedException("Form assignment not found or access denied");
        }

        return formAssignmentFieldRepository
                .findByAssignmentIdAndFieldIdAndIsDeletedFalse(assignmentId, templateFieldId)
                .map(FormAssignmentField::getId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Form assignment field not found for fieldId: " + templateFieldId));
    }

    @Transactional
    public FormResponseDto saveFormResponse(Long assignmentId, Long assignmentFieldId, String value) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(assignmentFieldId, "Assignment field ID is required");

        // Get current authenticated client
        Client client = getCurrentClientEntity();

        FormAssignment assignment = formAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Form assignment not found"));

        if (!assignment.getClient().getId().equals(client.getId())) {
            throw new UnauthorizedException("Form assignment not found or access denied");
        }

        // Get assignment field (snapshot)
        FormAssignmentField assignmentField = formAssignmentFieldRepository.findById(assignmentFieldId)
                .orElseThrow(() -> new ResourceNotFoundException("Form assignment field not found"));

        if (!assignmentField.getAssignment().getId().equals(assignmentId)) {
            throw new BadRequestException("Assignment field does not belong to this assignment");
        }

        // Check if response already exists
        FormResponse existingResponse = formResponseRepository
                .findByAssignmentIdAndAssignmentFieldId(assignmentId, assignmentFieldId)
                .orElse(null);

        FormResponse response;
        if (existingResponse != null) {
            existingResponse.setResponseValue(value);
            response = formResponseRepository.save(existingResponse);
        } else {
            response = FormResponse.builder()
                    .assignment(assignment)
                    .assignmentField(assignmentField)
                    .responseValue(value)
                    .build();
            response.setIsDeleted(false);
            response = formResponseRepository.save(response);
        }

        // Update assignment status to in_progress if pending
        if (Status.ASSIGNED.equals(assignment.getStatus())) {
            assignment.setStatus(Status.IN_PROGRESS);
            formAssignmentRepository.save(assignment);
        }

        FormAssignmentField responseAssignmentField = response.getAssignmentField();
        return FormResponseDto.builder()
                .id(response.getId())
                .assignmentId(response.getAssignment() != null ? response.getAssignment().getId() : null)
                .assignmentFieldId(responseAssignmentField != null ? responseAssignmentField.getId() : null)
                .fieldId(responseAssignmentField != null && responseAssignmentField.getField() != null 
                        ? responseAssignmentField.getField().getId() : null)
                .fieldLabel(responseAssignmentField != null ? responseAssignmentField.getFieldLabel() : null)
                .fieldType(responseAssignmentField != null ? responseAssignmentField.getFieldType() : null)
                .value(response.getResponseValue())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }

    @Transactional
    public FormSignatureResponse saveFormSignature(Long assignmentId, String signatureData, Boolean agreedToTerms, String ipAddress,
            String userAgent) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");

        Client client = getCurrentClientEntity();
        if (client == null) {
            throw new ResourceNotFoundException("Client not found");
        }

        FormAssignment assignment = formAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Form assignment not found"));

        if (!assignment.getClient().getId().equals(client.getId())) {
            throw new UnauthorizedException("Form assignment not found or access denied");
        }

        // If signatureData is empty, delete existing signature
        if (!StringUtils.hasText(signatureData)) {
            List<FormSignature> existingSignatures = formSignatureRepository.findByAssignmentId(assignmentId);
            if (!existingSignatures.isEmpty()) {
                formSignatureRepository.deleteAll(existingSignatures);
            }
            recordAuditEvent(client.getId(),
                    getClientEmail(client),
                    "form_signature_cleared", ipAddress, userAgent, "success",
                    String.format("Portal form signature cleared - assignmentId: %d", assignmentId), true);
            return null;
        }

        FormSubmissionValidator.requireAcceptedTerms(agreedToTerms);

        // Check if signature already exists
        List<FormSignature> existingSignatures = formSignatureRepository.findByAssignmentId(assignmentId);
        FormSignature existingSignature = existingSignatures.isEmpty() ? null : existingSignatures.get(0);

        FormSignature signature;
        if (existingSignature != null) {
            existingSignature.setSignatureData(signatureData);
            existingSignature.setAgreedToTerms(true);
            existingSignature.setSignerName(client.getFullName());
            existingSignature.setSignerRole("client");
            if (existingSignature.getSignatureType() == null) {
                existingSignature.setSignatureType(SignatureType.DRAWN);
            }
            existingSignature.setSignedAt(Instant.now());
            existingSignature.setIpAddress(ipAddress);
            existingSignature.setUserAgent(userAgent);
            signature = formSignatureRepository.save(existingSignature);
        } else {
            signature = FormSignature.builder()
                    .assignment(assignment)
                    .signatureData(signatureData)
                    .signerName(client.getFullName())
                    .signerRole("client")
                    .signatureType(SignatureType.DRAWN)
                    .signedAt(Instant.now())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .agreedToTerms(true)
                    .build();
            signature = formSignatureRepository.save(signature);
        }

        // Audit
        recordAuditEvent(client.getId(),
                getClientEmail(client),
                "form_signed", ipAddress, userAgent, "success",
                String.format("Portal form signature - assignmentId: %d", assignmentId), true);

        return FormSignatureResponse.builder()
                .id(signature.getId())
                .assignmentId(signature.getAssignment().getId())
                .signatureData(signature.getSignatureData())
                .signatureImageUrl(buildSignatureImageUrl(signature.getAssignment().getId()))
                .signerName(signature.getSignerName())
                .signerRole(signature.getSignerRole())
                .signedAt(signature.getSignedAt())
                .ipAddress(signature.getIpAddress())
                .userAgent(signature.getUserAgent())
                .agreedToTerms(signature.getAgreedToTerms())
                .createdAt(signature.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public FormSignatureResponse getFormSignature(Long assignmentId) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");

        Client client = getCurrentClientEntity();
        if (client == null) {
            throw new ResourceNotFoundException("Client not found");
        }

        FormAssignment assignment = formAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Form assignment not found"));

        if (!assignment.getClient().getId().equals(client.getId())) {
            throw new UnauthorizedException("Form assignment not found or access denied");
        }

        List<FormSignature> signatures = formSignatureRepository.findByAssignmentId(assignmentId);
        if (signatures.isEmpty()) {
            throw new ResourceNotFoundException("Signature not found");
        }
        FormSignature signature = signatures.get(0);

        return FormSignatureResponse.builder()
                .id(signature.getId())
                .assignmentId(signature.getAssignment().getId())
                .signatureData(signature.getSignatureData())
                .signatureImageUrl(buildSignatureImageUrl(signature.getAssignment().getId()))
                .signerName(signature.getSignerName())
                .signerRole(signature.getSignerRole())
                .signedAt(signature.getSignedAt())
                .ipAddress(signature.getIpAddress())
                .userAgent(signature.getUserAgent())
                .agreedToTerms(signature.getAgreedToTerms())
                .createdAt(signature.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public SignatureImageResult getFormSignatureImage(Long assignmentId) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");

        Client client = getCurrentClientEntity();
        if (client == null) {
            throw new ResourceNotFoundException("Client not found");
        }

        FormAssignment assignment = formAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Form assignment not found"));

        if (!assignment.getClient().getId().equals(client.getId())) {
            throw new UnauthorizedException("Form assignment not found or access denied");
        }

        List<FormSignature> signatures = formSignatureRepository.findByAssignmentId(assignmentId);
        if (signatures.isEmpty()) {
            throw new ResourceNotFoundException("Signature not found");
        }

        return decodeSignatureData(signatures.get(0).getSignatureData());
    }

    private String buildSignatureImageUrl(Long assignmentId) {
        return "/api/v1/portal/forms/signature/" + assignmentId + "/image";
    }

    private SignatureImageResult decodeSignatureData(String signatureData) {
        if (!StringUtils.hasText(signatureData)) {
            throw new BadRequestException("Signature data is empty");
        }

        String mimeType = "image/png";
        String rawBase64 = signatureData;

        if (signatureData.startsWith("data:")) {
            int commaIndex = signatureData.indexOf(',');
            if (commaIndex <= 0) {
                throw new BadRequestException("Invalid signature data format");
            }

            String metadata = signatureData.substring(5, commaIndex);
            rawBase64 = signatureData.substring(commaIndex + 1);

            int semicolonIndex = metadata.indexOf(';');
            mimeType = semicolonIndex > 0 ? metadata.substring(0, semicolonIndex) : metadata;
            if (!StringUtils.hasText(mimeType)) {
                mimeType = "image/png";
            }
        }

        try {
            byte[] imageBytes = Base64.getDecoder().decode(rawBase64);
            return new SignatureImageResult(imageBytes, mimeType);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid signature image data");
        }
    }

    @Transactional
    public FormAssignmentResponse submitForm(Long assignmentId, String ipAddress, String userAgent) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");

        Client client = getCurrentClientEntity();
        if (client == null) {
            throw new ResourceNotFoundException("Client not found");
        }

        FormAssignment assignment = formAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Form assignment not found"));

        if (!assignment.getClient().getId().equals(client.getId())) {
            throw new UnauthorizedException("Form assignment not found or access denied");
        }

        if (Status.COMPLETED.equals(assignment.getStatus()) || Status.REVIEWED.equals(assignment.getStatus())) {
            throw new BadRequestException("Form already submitted");
        }

        // Check if signature is required and present
        FormTemplate template = assignment.getTemplateVersion() != null 
                ? assignment.getTemplateVersion().getTemplate() 
                : null;
        FormSubmissionValidator.validateSubmission(assignment,
                formAssignmentFieldRepository.findByAssignmentIdAndIsDeletedFalseOrderBySortOrderAsc(assignmentId),
                formResponseRepository.findByAssignmentId(assignmentId), formSignatureRepository.findByAssignmentId(assignmentId));

        // Update assignment status
        assignment.setStatus(Status.COMPLETED);
        assignment.setSubmittedAt(Instant.now());
        assignment.setCompletedAt(Instant.now());
        FormAssignment saved = formAssignmentRepository.save(assignment);

        // Audit
        recordAuditEvent(client.getId(),
                getClientEmail(client),
                "form_submitted", ipAddress, userAgent, "success",
                String.format("Portal form submission - assignmentId: %d, formName: %s",
                        assignmentId, template != null ? template.getName() : "Unknown"),
                true);

        // Build response
        FormTemplateVersion savedVersion = saved.getTemplateVersion();
        FormTemplate savedTemplate = savedVersion != null ? savedVersion.getTemplate() : null;
        
        return FormAssignmentResponse.builder()
                .id(saved.getId())
                .templateId(savedTemplate != null ? savedTemplate.getId() : null)
                .templateVersionId(savedVersion != null ? savedVersion.getId() : null)
                .versionNumber(savedVersion != null ? savedVersion.getVersionNumber() : null)
                .templateName(savedTemplate != null ? savedTemplate.getName() : null)
                .templateCategory(
                        savedTemplate != null && savedTemplate.getCategory() != null 
                                ? savedTemplate.getCategory().name() : null)
                .clientId(saved.getClient().getId())
                .clientName(saved.getClient().getFullName())
                .assignedById(saved.getAssignedBy() != null ? saved.getAssignedBy().getId() : null)
                .assignedByName(saved.getAssignedBy() != null ? saved.getAssignedBy().getFullName() : null)
                .status(saved.getStatus() != null ? saved.getStatus().name() : null)
                .dueDate(saved.getDueDate())
                .instructions(resolvePortalAssignmentInstructions(saved))
                .completedAt(saved.getCompletedAt())
                .submittedAt(saved.getSubmittedAt())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    // ========== CONSENTS ==========

    private String resolvePortalAssignmentInstructions(FormAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        if (StringUtils.hasText(assignment.getInstructions())) {
            return assignment.getInstructions();
        }
        FormTemplateVersion version = assignment.getTemplateVersion();
        if (version != null && StringUtils.hasText(version.getInstructions())) {
            return version.getInstructions();
        }
        FormTemplate template = version != null ? version.getTemplate() : null;
        return template != null ? template.getInstructions() : null;
    }

    @Transactional(readOnly = true)
    public List<com.smart.therapy.flow.client.portal.dto.PortalConsentResponse> getConsents() {
        Client client = getCurrentClientEntity();
        if (client == null) {
            throw new ResourceNotFoundException("Client not found");
        }

        List<PatientConsent> consents = patientConsentRepository.findByClientIdOrderByCreatedAtDesc(client.getId());
        Map<ConsentType, PatientConsent> latestByType = new LinkedHashMap<>();
        for (PatientConsent consent : consents) {
            if (consent.getConsentType() != null) {
                latestByType.putIfAbsent(consent.getConsentType(), consent);
            }
        }

        return latestByType.values().stream()
                .map(c -> com.smart.therapy.flow.client.portal.dto.PortalConsentResponse.builder()
                        .id(c.getId())
                        .clientId(c.getClient().getId())
                        .consentType(c.getConsentType() != null ? c.getConsentType().getDisplayName() : null)
                        .consentVersion(c.getConsentFormVersion())
                        .granted(c.getGranted())
                        .grantedAt(c.getGrantedAt())
                        .withdrawnAt(c.getWithdrawnAt())
                        .ipAddress(c.getIpAddress())
                        .userAgent(c.getUserAgent())
                        .notes(c.getNotes())
                        .createdAt(c.getCreatedAt())
                        .updatedAt(c.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public com.smart.therapy.flow.client.portal.dto.PortalConsentResponse grantConsent(
            com.smart.therapy.flow.client.portal.dto.GrantConsentRequest request,
            String ipAddress,
            String userAgent) {
        Objects.requireNonNull(request, "Request is required");

        // Validate request fields
        if (request.getConsentType() == null || request.getConsentType().trim().isEmpty()) {
            throw new BadRequestException("Consent type is required");
        }
        if (request.getConsentVersion() == null || request.getConsentVersion().trim().isEmpty()) {
            throw new BadRequestException("Consent version is required");
        }
        if (request.getGranted() == null) {
            throw new BadRequestException("Granted status is required");
        }

        Client client = getCurrentClientEntity();
        ConsentType consentTypeEnum = ConsentType.fromValue(request.getConsentType());
        return consentCommandService.togglePortalConsent(
                client.getId(),
                consentTypeEnum,
                request.getGranted(),
                request.getConsentVersion(),
                request.getGranted() ? "Consent granted via client portal" : "Consent withdrawn via client portal",
                "client_portal",
                ipAddress,
                userAgent);
    }

    @Transactional
    public com.smart.therapy.flow.client.portal.dto.PortalConsentResponse withdrawConsent(
            com.smart.therapy.flow.client.portal.dto.WithdrawConsentRequest request,
            String ipAddress,
            String userAgent) {
        Objects.requireNonNull(request, "Request is required");

        Client client = getCurrentClientEntity();
        ConsentType consentTypeEnum = ConsentType.fromValue(request.getConsentType());
        String version = patientConsentRepository.findLatestByClientIdAndConsentType(client.getId(), consentTypeEnum)
                .map(PatientConsent::getConsentFormVersion)
                .orElse("1.0");
        return consentCommandService.togglePortalConsent(
                client.getId(),
                consentTypeEnum,
                false,
                version,
                "Consent withdrawn via client portal",
                "client_portal",
                ipAddress,
                userAgent);
    }

    private com.smart.therapy.flow.notification.dto.NotificationResponse toNotificationResponse(
            Notification notification) {
        return com.smart.therapy.flow.notification.dto.NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .priority(notification.getPriority() != null ? notification.getPriority().name() : null)
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .actionUrl(notification.getActionUrl())
                .actionLabel(notification.getActionLabel())
                .relatedEntityType(notification.getRelatedEntityType())
                .relatedEntityId(
                        notification.getRelatedEntityId() != null ? notification.getRelatedEntityId().longValue()
                                : null)
                .createdAt(notification.getCreatedAt())
                .build();
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Determine location (Online or Office) based on whether the room is the
     * therapist's virtual room.
     */
    private String resolveClientRatingComment(SessionNote note) {
        if (note == null || note.getClientRating() == null || !StringUtils.hasText(note.getRemarks())) {
            return null;
        }
        return note.getRemarks().trim();
    }

    private String determineLocation(Session session) {
        // Check if session type is "online" (case-insensitive)
        if (SystemOptionKeyMatcher.matchesAny(session.getSessionType(), "online")) {
            return "Online";
        }

        // Check if zoom is enabled via SessionIntegration
        if (getZoomIntegration(session) != null) {
            return "Online";
        }

        // Check if room is therapist's virtual room
        if (session.getTherapist() != null && session.getRoom() != null) {
            try {
                UserProfile therapistProfile = userProfileRepository.findByUserId(session.getTherapist().getId())
                        .orElse(null);
                if (therapistProfile != null && therapistProfile.getVirtualRoom() != null) {
                    if (session.getRoom().getId().equals(therapistProfile.getVirtualRoom().getId())) {
                        return "Online";
                    }
                }
            } catch (Exception e) {
                log.warn("Error checking virtual room for session {}: {}", session.getId(), e.getMessage());
            }
        }

        return "Office"; // Default to Office
    }

    private String resolveSessionMode(Session session) {
        return session != null && session.getSessionType() != null ? session.getSessionType() : null;
    }

    private String resolveClinicalSessionType(Session session) {
        if (session == null) {
            return null;
        }
        if (session.getClinicalSessionType() != null && !session.getClinicalSessionType().isBlank()) {
            return session.getClinicalSessionType();
        }
        if (session.getService() != null) {
            if (session.getService().getCategory() != null && !session.getService().getCategory().isBlank()) {
                return session.getService().getCategory();
            }
            if (session.getService().getServiceName() != null && !session.getService().getServiceName().isBlank()) {
                return session.getService().getServiceName();
            }
        }
        return null;
    }


    private String generateSessionToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Derive category from documentType.
     * Common mappings: insurance_card -> insurance, intake_form -> forms, etc.
     */
    private String deriveCategoryFromDocumentType(String documentType) {
        if (documentType == null || documentType.trim().isEmpty()) {
            return null;
        }

        String docTypeLower = documentType.toLowerCase().trim();

        // Map document types to categories
        switch (docTypeLower) {
            case "insurance_card":
                return "insurance";
            case "intake_form":
            case "consent_form":
                return "forms";
            default:
                // Return null if no mapping found - will default to "uploaded"
                return null;
        }
    }

    private ReviewStatus resolveEffectiveReviewStatus(Document document) {
        if (document == null) {
            return null;
        }
        ReviewStatus status = document.getReviewStatus();
        if (status == null) {
            return null;
        }
        if (status == ReviewStatus.APPROVED || status == ReviewStatus.REJECTED) {
            return status;
        }
        if (Boolean.TRUE.equals(document.getNeedsReview())
                && document.getReviewDueAt() != null
                && document.getReviewDueAt().isBefore(Instant.now())) {
            return ReviewStatus.OVERDUE;
        }
        return status;
    }

    /**
     * Record audit via {@link AuditLogService#write}.
     * <p>
     * Does not attach a {@code Client} entity FK: loading/setting the client while the
     * outer TX holds the clients row lock can deadlock. Client id is stored on resourceId instead.
     * <p>
     * HIPAA: User column uses MRN only for known clients — never email or display name.
     */
    private void recordAuditEvent(Long clientId, String username, String action, String ipAddress,
            String userAgent, String result, String details, boolean portal) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .result(result)
                    .resourceType("client_portal")
                    .resourceId(clientId != null ? String.valueOf(clientId) : null)
                    .username(resolvePortalAuditUsername(clientId, username))
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .hipaaRelevant(true)
                    .riskLevel("medium")
                    .timestamp(Instant.now())
                    .details(details)
                    .build();
            auditLogService.write(auditLog);
        } catch (Exception e) {
            log.warn("Failed to record audit event for client portal: clientId={}, action={}, error={}",
                    clientId, action, e.getMessage());
        }
    }

    /**
     * Prefer MRN when client is known. Never persist email for portal actors.
     * Avoids attaching the Client entity (FK) to prevent row-lock deadlocks.
     */
    private String resolvePortalAuditUsername(Long clientId, String username) {
        if (clientId != null) {
            if (StringUtils.hasText(username) && username.regionMatches(true, 0, "CL-", 0, 3)) {
                return username;
            }
            try {
                return clientRepository.findById(clientId)
                        .map(HipaaAuditLabels::clientActor)
                        .orElse("client-" + clientId);
            } catch (Exception e) {
                log.debug("Could not resolve MRN for portal audit clientId={}: {}", clientId, e.getMessage());
                return "client-" + clientId;
            }
        }
        // Pre-auth / unknown actor: do not store email or name
        if (StringUtils.hasText(username) && username.contains("@")) {
            return "unknown";
        }
        return StringUtils.hasText(username) ? username : "unknown";
    }

    // ========== HELPER METHODS FOR NORMALIZED ENTITIES ==========

    /**
     * Get client email from AuthIdentity (login identifier) or ClientContact (primary email).
     */
    private String getClientEmail(Client client) {
        if (client.getAuthIdentity() != null && StringUtils.hasText(client.getAuthIdentity().getLoginIdentifier())) {
            return client.getAuthIdentity().getLoginIdentifier();
        }
        try {
            Optional<ClientContact> primaryEmail = contactService.getPrimaryEmail(client.getId());
            if (primaryEmail.isPresent() && StringUtils.hasText(primaryEmail.get().getContactValue())) {
                return primaryEmail.get().getContactValue();
            }
        } catch (Exception e) {
            log.debug("Failed to get email from contact for client {}: {}", client.getId(), e.getMessage());
        }
        return null;
    }

    /**
     * Get client phone from normalized ClientContact entity (primary phone)
     */
    private String getClientPhone(Client client) {
        try {
            Optional<ClientContact> primaryPhone = contactService.getPrimaryPhone(client.getId());
            if (primaryPhone.isPresent() && StringUtils.hasText(primaryPhone.get().getContactValue())) {
                return primaryPhone.get().getContactValue();
            }
        } catch (Exception e) {
            log.debug("Failed to get phone from normalized entity for client {}: {}", client.getId(), e.getMessage());
        }
        // No phone found in normalized entities
        return null;
    }

    /**
     * Helper method to get Zoom integration from a session
     */
    /**
     * Online sessions need a Zoom meeting, so the mode is only offered when the assigned
     * therapist has an active integration. Also covers zoom.enabled=false, which leaves
     * zoomService unresolved.
     */
    private boolean isOnlineBookingAvailable(Client client) {
        User therapist = client != null ? client.getAssignedTherapist() : null;
        return therapist != null && zoomService != null && zoomService.isTherapistConfigured(therapist);
    }

    /**
     * Lets a client ask their therapist to connect Zoom after finding online booking disabled.
     * The client sends no content of their own: they press a button, and the therapist gets a
     * fixed message identified by MRN. That keeps this from becoming an unlogged message channel.
     */
    @Transactional
    public OnlineBookingRequestResponse requestOnlineBooking(String ipAddress, String userAgent) {
        Client client = getCurrentClientEntity();
        User therapist = client.getAssignedTherapist();
        if (therapist == null) {
            throw new BadRequestException("No therapist assigned to your account");
        }

        // The therapist may have connected Zoom while this page was open; asking now would be noise.
        if (isOnlineBookingAvailable(client)) {
            return OnlineBookingRequestResponse.builder()
                    .onlineBookingAvailable(true)
                    .build();
        }

        ClientPortalSettings settings = portalSettingsService.getOrCreate(client.getId());
        Instant now = Instant.now();
        Instant previous = settings.getOnlineBookingRequestedAt();
        if (previous != null && previous.isAfter(now.minus(ONLINE_BOOKING_REQUEST_COOLDOWN))) {
            // Inside the cooldown this is a no-op, so a replayed or repeated call sends nothing.
            return OnlineBookingRequestResponse.builder()
                    .onlineBookingAvailable(false)
                    .requestedAt(previous)
                    .nextRequestAllowedAt(previous.plus(ONLINE_BOOKING_REQUEST_COOLDOWN))
                    .build();
        }

        settings.setOnlineBookingRequestedAt(now);
        portalSettingsService.save(settings);

        notifyTherapistOfOnlineBookingRequest(client, therapist);

        recordAuditEvent(client.getId(), getClientEmail(client), "online_booking_requested",
                ipAddress, userAgent, "success",
                String.format("{\"therapistId\":%d}", therapist.getId()), true);

        return OnlineBookingRequestResponse.builder()
                .onlineBookingAvailable(false)
                .requestedAt(now)
                .nextRequestAllowedAt(now.plus(ONLINE_BOOKING_REQUEST_COOLDOWN))
                .build();
    }

    /**
     * The therapist sees this in the app straight away. The email is left to the digest job so
     * that ten clients asking on one day produce one message rather than ten.
     */
    private void notifyTherapistOfOnlineBookingRequest(Client client, User therapist) {
        if (notificationService == null) {
            return;
        }
        try {
            notificationService.createSystemNotification(
                    therapist,
                    NotificationType.ONLINE_BOOKING_REQUESTED,
                    "Online session requested",
                    "Client " + HipaaAuditLabels.clientActor(client)
                            + " asked about online sessions. Your Zoom account isn't connected.",
                    zoomSetupUrl,
                    "Set up Zoom");
        } catch (Exception e) {
            log.warn("Failed to notify therapist {} of online booking request", therapist.getId(), e);
        }
    }

    private SessionIntegration getZoomIntegration(Session session) {
        if (session.getIntegrations() == null || session.getIntegrations().isEmpty()) {
            return null;
        }
        return session.getIntegrations().stream()
                .filter(integration -> "zoom".equals(integration.getProvider()))
                .findFirst()
                .orElse(null);
    }

    /** The notification workflow is the sole sender, after the booking commits. */
    private void populateBookingEmailDetails(Map<String, Object> data, Session session, ZoneId zone,
            String zoomJoinUrl, String zoomPassword) {
        var start = session.getSessionDate();
        var end = start.plusSeconds((session.getDuration() != null ? session.getDuration() : 60) * 60L);
        var dateFormat = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", java.util.Locale.ENGLISH).withZone(zone);
        var timeFormat = DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH).withZone(zone);
        String date = dateFormat.format(start);
        String time = timeFormat.format(start) + " - " + timeFormat.format(end) + " (" + zone.getId() + ")";
        String service = session.getService() != null ? session.getService().getServiceName() : "Therapy Session";
        String provider = session.getTherapist() != null ? session.getTherapist().getFullName() : "";
        String location = session.getRoom() != null ? session.getRoom().getRoomName() : determineLocation(session);
        String mrn = session.getClient().getClientId();
        data.put("serviceId", session.getService() != null ? session.getService().getId() : null);
        data.put("serviceName", service);
        data.put("sessionDateFormatted", date + " " + time);
        data.put("sessionDateOnlyFormatted", date);
        data.put("sessionTimeRangeFormatted", time);
        data.put("roomName", session.getRoom() != null ? session.getRoom().getRoomName() : null);
        data.put("locationLabel", escapeBookingEmailText(location));
        String zoomHtml = com.smart.therapy.flow.common.service.EmailHtmlComponents.zoomMeetingCard(zoomJoinUrl, zoomPassword);
        data.put("zoomMeetingHtml", zoomHtml);
        data.put("sessionDetailsHtml", com.smart.therapy.flow.common.service.EmailHtmlComponents.sessionDetailsCardHtml(
                escapeBookingEmailText(date), escapeBookingEmailText(time), escapeBookingEmailText(mrn),
                escapeBookingEmailText(provider), escapeBookingEmailText(location), escapeBookingEmailText(service)) + zoomHtml);
        // Delivery rewrites this CTA for the staff or client recipient and configured frontend host.
        data.put("sessionUrl", com.smart.therapy.flow.common.service.EmailAppLinks.STAFF_SCHEDULING);
        data.put("calendarUrl", com.smart.therapy.flow.common.service.EmailHtmlComponents.googleCalendarUrl(
                service, start, end, "SmartHub session", location));
    }

    private String escapeBookingEmailText(String value) {
        return value == null ? "" : org.springframework.web.util.HtmlUtils.htmlEscape(value);
    }
}
