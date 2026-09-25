package com.smart.therapy.flow.payment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.organisation.entity.OrgStripeAccount;
import com.smart.therapy.flow.organisation.entity.OrgStripeOnboardingStatus;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrgStripeAccountRepository;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.billing.audit.BillingAuditActions;
import com.smart.therapy.flow.payment.dto.OrgStripeConnectStatusResponse;
import com.smart.therapy.flow.payment.dto.StripeConnectOauthStartResponse;
import com.stripe.model.Account;
import com.stripe.net.RequestOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrgStripeConnectService {

    private static final String STRIPE_OAUTH_AUTHORIZE_URL = "https://connect.stripe.com/oauth/authorize";
    private static final String STRIPE_OAUTH_TOKEN_URL = "https://connect.stripe.com/oauth/token";

    private final OrgStripeAccountRepository orgStripeAccountRepository;
    private final OrganisationRepository organisationRepository;
    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final StripePlatformConfigService stripePlatformConfigService;
    private final PlatformAuditService platformAuditService;
    private final ObjectMapper objectMapper;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.frontend.stripe-connect-success-url:https://app.therapyflow.pro/stripe/success}")
    private String stripeConnectSuccessUrl;

    @Transactional(readOnly = true)
    public OrgStripeConnectStatusResponse getStatus(Long organisationId) {
        Organisation org = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));
        OrgStripeAccount account = orgStripeAccountRepository.findByOrganisationId(org.getId())
                .orElseGet(() -> OrgStripeAccount.builder().organisationId(org.getId()).build());
        return toStatusResponse(account, fetchStripeAccount(account));
    }

    @Transactional
    public StripeConnectOauthStartResponse startOauth(Long organisationId, Long actorAuthId) {
        ensureOrganisationExists(organisationId);
        OrgStripeAccount row = getOrCreate(organisationId);
        row.setOnboardingStatus(OrgStripeOnboardingStatus.PENDING);
        row.setDisabledReason(null);

        String state = generateStateToken();
        Instant expiry = Instant.now().plusSeconds(15 * 60);
        row.setOauthState(state);
        row.setOauthStateExpiresAt(expiry);
        orgStripeAccountRepository.save(row);

        String clientId = stripePlatformConfigService.requireConnectClientId();
        String redirectUri = buildRedirectUri();

        String authorizeUrl = UriComponentsBuilder.fromHttpUrl(STRIPE_OAUTH_AUTHORIZE_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("scope", "read_write")
                .queryParam("state", state)
                .queryParam("redirect_uri", redirectUri)
                .build(true)
                .toUriString();

        platformAuditService.log(actorAuthId, BillingAuditActions.STRIPE_ONBOARDING_STARTED, "Organisation", String.valueOf(organisationId),
                "status=PENDING");

        return StripeConnectOauthStartResponse.builder()
                .authorizeUrl(authorizeUrl)
                .stateExpiresAt(expiry)
                .build();
    }

    @Transactional
    public OrgStripeConnectStatusResponse handleOauthCallback(Long organisationId,
                                                              String state,
                                                              String code,
                                                              String error,
                                                              String errorDescription,
                                                              Long actorAuthId) {
        OrgStripeAccount row = getOrCreate(organisationId);

        validateOauthState(row, state);

        if (StringUtils.hasText(error)) {
            row.setOnboardingStatus(OrgStripeOnboardingStatus.RESTRICTED);
            row.setDisabledReason("OAuth error: " + error + (StringUtils.hasText(errorDescription) ? " - " + errorDescription : ""));
            clearOauthState(row);
            orgStripeAccountRepository.save(row);
            platformAuditService.log(actorAuthId, "ORG_STRIPE_CONNECT_OAUTH_FAILED", "Organisation",
                    String.valueOf(organisationId), row.getDisabledReason());
            return toStatusResponse(row, Optional.empty());
        }

        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Missing Stripe OAuth code");
        }

        Map<String, Object> tokenResponse = exchangeOAuthCode(code);
        Object connectAccountObj = tokenResponse.get("stripe_user_id");
        if (!(connectAccountObj instanceof String connectAccountId) || !StringUtils.hasText(connectAccountId)) {
            throw new BadRequestException("Stripe OAuth response missing connected account id");
        }

        if (orgStripeAccountRepository.existsByConnectAccountIdAndOrganisationIdNot(connectAccountId, organisationId)) {
            throw new BadRequestException("This Stripe account is already linked to another organisation");
        }

        row.setConnectAccountId(connectAccountId);
        row.setOnboardingStatus(OrgStripeOnboardingStatus.PENDING);
        row.setDisabledReason(null);
        clearOauthState(row);
        orgStripeAccountRepository.save(row);

        Optional<Account> stripeAccount = refreshInternal(row);
        platformAuditService.log(actorAuthId, BillingAuditActions.STRIPE_ONBOARDING_COMPLETED, "Organisation",
                String.valueOf(organisationId), "account=" + connectAccountId);
        return toStatusResponse(row, stripeAccount);
    }

    @Transactional
    public OrgStripeConnectStatusResponse handleOauthCallbackByState(String state,
                                                                     String code,
                                                                     String error,
                                                                     String errorDescription,
                                                                     Long actorAuthId) {
        if (!StringUtils.hasText(state)) {
            throw new BadRequestException("Invalid Stripe OAuth state");
        }
        OrgStripeAccount row = orgStripeAccountRepository.findByOauthState(state)
                .orElseThrow(() -> new BadRequestException("Invalid Stripe OAuth state"));
        return handleOauthCallback(row.getOrganisationId(), state, code, error, errorDescription, actorAuthId);
    }

    @Transactional
    public OrgStripeConnectStatusResponse refresh(Long organisationId, Long actorAuthId) {
        OrgStripeAccount row = getOrCreate(organisationId);
        if (!StringUtils.hasText(row.getConnectAccountId())) {
            throw new BadRequestException("Organization payment account is not configured");
        }
        Optional<Account> stripeAccount = refreshInternal(row);
        platformAuditService.log(actorAuthId, "ORG_STRIPE_CONNECT_REFRESH", "Organisation",
                String.valueOf(organisationId), "account=" + row.getConnectAccountId());
        return toStatusResponse(row, stripeAccount);
    }

    @Transactional
    public OrgStripeConnectStatusResponse disconnect(Long organisationId, Long actorAuthId) {
        OrgStripeAccount row = getOrCreate(organisationId);
        row.setConnectAccountId(null);
        row.setOnboardingStatus(OrgStripeOnboardingStatus.NOT_CONNECTED);
        row.setChargesEnabled(false);
        row.setPayoutsEnabled(false);
        row.setDetailsSubmitted(false);
        row.setCountry(null);
        row.setDefaultCurrency(null);
        row.setDisabledReason("Disconnected by organization admin");
        row.setLastSyncedAt(Instant.now());
        clearOauthState(row);
        orgStripeAccountRepository.save(row);
        platformAuditService.log(actorAuthId, "ORG_STRIPE_CONNECT_DISCONNECT", "Organisation",
                String.valueOf(organisationId), "disconnected");
        return toStatusResponse(row, Optional.empty());
    }

    @Transactional(readOnly = true)
    public String requireReadyConnectAccountId(Long organisationId) {
        OrgStripeAccount row = orgStripeAccountRepository.findByOrganisationId(organisationId)
                .orElseThrow(() -> new BadRequestException("Tenant Stripe Connect account is not configured"));
        if (!StringUtils.hasText(row.getConnectAccountId())) {
            throw new BadRequestException("Stripe Connect onboarding is incomplete. Complete onboarding before accepting portal payments.");
        }
        if (!Boolean.TRUE.equals(row.getChargesEnabled())) {
            throw new BadRequestException("Stripe Connect onboarding is incomplete. Charges are not enabled for this organisation.");
        }
        return row.getConnectAccountId();
    }

    @Transactional(readOnly = true)
    public Optional<String> findConnectAccountId(Long organisationId) {
        return orgStripeAccountRepository.findByOrganisationId(organisationId)
                .map(OrgStripeAccount::getConnectAccountId)
                .filter(StringUtils::hasText);
    }

    @Transactional
    public void handleConnectAccountEvent(String eventType, String eventAccountId, Map<String, Object> eventData) {
        String connectAccountId = StringUtils.hasText(eventAccountId) ? eventAccountId : extractObjectId(eventData);
        if (!StringUtils.hasText(connectAccountId)) {
            return;
        }
        Optional<OrgStripeAccount> existing = orgStripeAccountRepository.findByConnectAccountId(connectAccountId);
        if (existing.isEmpty()) {
            return;
        }
        OrgStripeAccount row = existing.get();

        @SuppressWarnings("unchecked")
        Map<String, Object> object = eventData.get("object") instanceof Map ? (Map<String, Object>) eventData.get("object") : Map.of();
        applyAccountState(row, object);
        orgStripeAccountRepository.save(row);
        platformAuditService.log(null, BillingAuditActions.STRIPE_ONBOARDING_STATUS_CHANGED + "_" + sanitizeAction(eventType), "Organisation",
                String.valueOf(row.getOrganisationId()), "account=" + connectAccountId);
    }

    public void runInTenantContextForAccount(String connectAccountId, Runnable runnable) {
        OrgStripeAccount account = orgStripeAccountRepository.findByConnectAccountId(connectAccountId)
                .orElseThrow(() -> new BadRequestException("Unknown Stripe connected account"));
        runInTenantContextForOrganisation(account.getOrganisationId(), runnable);
    }

    public void runInTenantContextForOrganisation(Long organisationId, Runnable runnable) {
        Organisation org = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));
        tenantTransactionExecutor.runWriteIsolated(org.getId(), org.getSchemaName(), runnable);
    }

    private void validateOauthState(OrgStripeAccount row, String state) {
        if (!StringUtils.hasText(state) || !StringUtils.hasText(row.getOauthState())) {
            throw new BadRequestException("Invalid Stripe OAuth state");
        }
        if (!row.getOauthState().equals(state)) {
            throw new BadRequestException("Stripe OAuth state mismatch");
        }
        if (row.getOauthStateExpiresAt() == null || row.getOauthStateExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Stripe OAuth state has expired");
        }
    }

    private Map<String, Object> exchangeOAuthCode(String code) {
        try {
            String body = "grant_type=authorization_code"
                    + "&code=" + urlEncode(code)
                    + "&client_secret=" + urlEncode(stripePlatformConfigService.requireConnectClientSecret());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(STRIPE_OAUTH_TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BadRequestException("Stripe OAuth token exchange failed with status " + response.statusCode());
            }
            Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {});
            if (payload.get("error") != null) {
                throw new BadRequestException("Stripe OAuth token exchange failed: " + payload.get("error"));
            }
            return payload;
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Failed to exchange Stripe OAuth code: " + ex.getMessage());
        }
    }

    private Optional<Account> refreshInternal(OrgStripeAccount row) {
        try {
            RequestOptions requestOptions = RequestOptions.builder()
                    .setApiKey(stripePlatformConfigService.requirePlatformSecretKey())
                    .build();
            Account account = Account.retrieve(row.getConnectAccountId(), requestOptions);

            row.setChargesEnabled(Boolean.TRUE.equals(account.getChargesEnabled()));
            row.setPayoutsEnabled(Boolean.TRUE.equals(account.getPayoutsEnabled()));
            row.setDetailsSubmitted(Boolean.TRUE.equals(account.getDetailsSubmitted()));
            row.setCountry(account.getCountry());
            row.setDefaultCurrency(account.getDefaultCurrency());
            row.setLastSyncedAt(Instant.now());
            row.setDisabledReason(
                    account.getRequirements() != null ? account.getRequirements().getDisabledReason() : null
            );
            row.setOnboardingStatus(resolveOnboardingStatus(row));
            orgStripeAccountRepository.save(row);
            return Optional.of(account);
        } catch (Exception ex) {
            row.setOnboardingStatus(OrgStripeOnboardingStatus.RESTRICTED);
            row.setDisabledReason("Unable to refresh account: " + ex.getMessage());
            row.setLastSyncedAt(Instant.now());
            orgStripeAccountRepository.save(row);
            return Optional.empty();
        }
    }

    private Optional<Account> fetchStripeAccount(OrgStripeAccount row) {
        if (!StringUtils.hasText(row.getConnectAccountId())) {
            return Optional.empty();
        }
        try {
            RequestOptions requestOptions = RequestOptions.builder()
                    .setApiKey(stripePlatformConfigService.requirePlatformSecretKey())
                    .build();
            return Optional.of(Account.retrieve(row.getConnectAccountId(), requestOptions));
        } catch (Exception ex) {
            log.warn("Unable to fetch Stripe account {} for status: {}", row.getConnectAccountId(), ex.getMessage());
            return Optional.empty();
        }
    }

    private OrgStripeOnboardingStatus resolveOnboardingStatus(OrgStripeAccount row) {
        if (!StringUtils.hasText(row.getConnectAccountId())) {
            return OrgStripeOnboardingStatus.NOT_CONNECTED;
        }
        if (StringUtils.hasText(row.getDisabledReason())) {
            return OrgStripeOnboardingStatus.RESTRICTED;
        }
        if (Boolean.TRUE.equals(row.getChargesEnabled())) {
            return OrgStripeOnboardingStatus.CONNECTED;
        }
        return OrgStripeOnboardingStatus.PENDING;
    }

    private void applyAccountState(OrgStripeAccount row, Map<String, Object> object) {
        row.setChargesEnabled(Boolean.TRUE.equals(object.get("charges_enabled")));
        row.setPayoutsEnabled(Boolean.TRUE.equals(object.get("payouts_enabled")));
        row.setDetailsSubmitted(Boolean.TRUE.equals(object.get("details_submitted")));
        row.setCountry(object.get("country") instanceof String s ? s : row.getCountry());
        row.setDefaultCurrency(object.get("default_currency") instanceof String s ? s : row.getDefaultCurrency());

        String disabledReason = null;
        if (object.get("requirements") instanceof Map<?, ?> requirements) {
            Object reason = requirements.get("disabled_reason");
            if (reason instanceof String text && StringUtils.hasText(text)) {
                disabledReason = text;
            }
        }
        row.setDisabledReason(disabledReason);
        row.setLastSyncedAt(Instant.now());
        row.setOnboardingStatus(resolveOnboardingStatus(row));
    }

    private OrgStripeAccount getOrCreate(Long organisationId) {
        ensureOrganisationExists(organisationId);
        return orgStripeAccountRepository.findByOrganisationId(organisationId)
                .orElseGet(() -> OrgStripeAccount.builder()
                        .organisationId(organisationId)
                        .onboardingStatus(OrgStripeOnboardingStatus.NOT_CONNECTED)
                        .chargesEnabled(false)
                        .payoutsEnabled(false)
                        .detailsSubmitted(false)
                        .build());
    }

    private void ensureOrganisationExists(Long organisationId) {
        if (organisationId == null || !organisationRepository.existsById(organisationId)) {
            throw new ResourceNotFoundException("Organisation not found");
        }
    }

    private OrgStripeConnectStatusResponse toStatusResponse(OrgStripeAccount row, Optional<Account> stripeAccount) {
        OrgStripeConnectStatusResponse.OrgStripeConnectStatusResponseBuilder builder = OrgStripeConnectStatusResponse.builder()
                .organisationId(row.getOrganisationId())
                .connectAccountId(row.getConnectAccountId())
                .onboardingStatus(row.getOnboardingStatus())
                .chargesEnabled(Boolean.TRUE.equals(row.getChargesEnabled()))
                .payoutsEnabled(Boolean.TRUE.equals(row.getPayoutsEnabled()))
                .detailsSubmitted(Boolean.TRUE.equals(row.getDetailsSubmitted()))
                .country(row.getCountry())
                .defaultCurrency(row.getDefaultCurrency())
                .lastSyncedAt(row.getLastSyncedAt())
                .disabledReason(row.getDisabledReason());
        stripeAccount.ifPresent(account -> applyRequirementDetails(builder, account));
        return builder.build();
    }

    private static void applyRequirementDetails(
            OrgStripeConnectStatusResponse.OrgStripeConnectStatusResponseBuilder builder,
            Account account) {
        if (account.getRequirements() == null) {
            builder.pastDueRequirements(List.of());
            builder.currentlyDueRequirements(List.of());
            return;
        }
        builder.pastDueRequirements(safeRequirementList(account.getRequirements().getPastDue()));
        builder.currentlyDueRequirements(safeRequirementList(account.getRequirements().getCurrentlyDue()));
    }

    private static List<String> safeRequirementList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private String buildRedirectUri() {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/v1/admin/stripe-connect/oauth/callback")
                .build(true)
                .toUriString();
    }

    public String getOauthFrontendSuccessUrl() {
        return stripeConnectSuccessUrl;
    }

    private static String generateStateToken() {
        byte[] bytes = new byte[24];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String extractObjectId(Map<String, Object> eventData) {
        @SuppressWarnings("unchecked")
        Map<String, Object> object = eventData.get("object") instanceof Map ? (Map<String, Object>) eventData.get("object") : Map.of();
        Object id = object.get("id");
        return id instanceof String s && StringUtils.hasText(s) ? s : null;
    }

    private static String sanitizeAction(String eventType) {
        return eventType == null ? "UNKNOWN" : eventType.toUpperCase().replace('.', '_');
    }

    private static void clearOauthState(OrgStripeAccount row) {
        row.setOauthState(null);
        row.setOauthStateExpiresAt(null);
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
