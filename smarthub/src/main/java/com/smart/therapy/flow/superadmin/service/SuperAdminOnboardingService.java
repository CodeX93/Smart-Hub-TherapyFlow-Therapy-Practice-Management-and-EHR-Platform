package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.auth.entity.*;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.auth.repository.RoleRepository;
import com.smart.therapy.flow.auth.service.TenantRbacSeedService;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.ReservedSubdomainRepository;
import com.smart.therapy.flow.organisation.entity.UserOrganisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.organisation.service.TenantMigrationScheduler;
import com.smart.therapy.flow.organisation.service.TenantProvisioningService;
import com.smart.therapy.flow.payment.service.StripePlatformSubscriptionService;
import com.smart.therapy.flow.subscription.entity.BillingContact;
import com.smart.therapy.flow.subscription.entity.Invoice;
import com.smart.therapy.flow.subscription.entity.OrgSubscription;
import com.smart.therapy.flow.subscription.entity.SubscriptionPlan;
import com.smart.therapy.flow.subscription.enums.InvoiceStatus;
import com.smart.therapy.flow.subscription.repository.BillingContactRepository;
import com.smart.therapy.flow.subscription.repository.InvoiceRepository;
import com.smart.therapy.flow.subscription.repository.OrgSubscriptionRepository;
import com.smart.therapy.flow.subscription.repository.SubscriptionPlanRepository;
import com.smart.therapy.flow.superadmin.dto.SuperAdminOnboardingCreateOrganisationRequest;
import com.smart.therapy.flow.superadmin.event.OrganisationOnboardedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuperAdminOnboardingService {

    private static final Map<String, Set<String>> ALLOWED_REGIONS_BY_RESIDENCY = Map.of(
            "US", Set.of("us-east-1", "us-west-1", "us-west-2"),
            "EU", Set.of("eu-west-1", "eu-central-1", "eu-north-1"),
            "AU", Set.of("ap-southeast-2")
    );

    private final OrganisationRepository organisationRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final OrgSubscriptionRepository orgSubscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final BillingContactRepository billingContactRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final RoleRepository roleRepository;
    private final TenantRbacSeedService tenantRbacSeedService;
    private final AuthIdentityRoleRepository authIdentityRoleRepository;
    private final UserOrganisationRepository userOrganisationRepository;
    private final TenantProvisioningService tenantProvisioningService;
    private final TenantMigrationScheduler tenantMigrationScheduler;
    private final ReservedSubdomainRepository reservedSubdomainRepository;
    private final TenantDirectoryService tenantDirectoryService;
    private final PlatformAuditService platformAuditService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final StripePlatformSubscriptionService stripePlatformSubscriptionService;

    @Value("${superadmin.onboarding.provision-sync:false}")
    private boolean provisionTenantSynchronously;
    @Value("${super-admin.onboarding.default-admin-password:${SUPERADMIN_ONBOARDING_DEFAULT_ADMIN_PASSWORD:}}")
    private String defaultAdminPassword;

    @Transactional
    public Organisation onboard(SuperAdminOnboardingCreateOrganisationRequest req, Long actorAuthId) {
        validate(req);

        String normalizedEmail = req.getPrimaryAdminEmail().trim().toLowerCase(Locale.ROOT);
        // Uniqueness is per-tenant; org does not exist yet — no cross-tenant block.
        // Email will be unique within the new org once the identity is created.
        if (organisationRepository.existsBySlug(req.getSlug().trim())) {
            throw new StoryApiException(HttpStatus.CONFLICT, "SLUG_TAKEN", "slug already exists");
        }
        if (req.getSubdomain() != null && !req.getSubdomain().isBlank()) {
            String subdomain = req.getSubdomain().trim().toLowerCase(Locale.ROOT);
            if (reservedSubdomainRepository.existsBySubdomainIgnoreCase(subdomain)) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "subdomain is reserved");
            }
            if (organisationRepository.existsBySubdomain(subdomain)) {
                throw new StoryApiException(HttpStatus.CONFLICT, "VALIDATION_ERROR", "subdomain already exists");
            }
        }

        String requestedPlan = req.getPlan().trim();
        SubscriptionPlan plan = subscriptionPlanRepository.findByCodeIgnoreCase(requestedPlan)
                .or(() -> subscriptionPlanRepository.findByNameIgnoreCase(requestedPlan))
                .orElseThrow(() -> new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PLAN_NOT_FOUND", "plan not found"));

        String region = req.getRegion().trim().toLowerCase(Locale.ROOT);
        String residency = req.getDataResidency().trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_REGIONS_BY_RESIDENCY.getOrDefault(residency, Set.of()).contains(region)) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_REGION_RESIDENCY", "region is not allowed for dataResidency");
        }

        Instant now = Instant.now();
        Organisation org = new Organisation();
        org.setName(req.getName().trim());
        org.setSlug(req.getSlug().trim());
        org.setStatus("ACTIVE");
        org.setSubdomain(req.getSubdomain() == null || req.getSubdomain().isBlank() ? null : req.getSubdomain().trim().toLowerCase(Locale.ROOT));
        org.setTimezone(req.getTimezone().trim());
        org.setRegion(region);
        org.setDataResidency(residency);
        org.setSchemaName("tenant_temp_" + System.currentTimeMillis());
        org = organisationRepository.save(org);
        org.setSchemaName("tenant_" + org.getId());
        org = organisationRepository.save(org);
        tenantRbacSeedService.seedDefaultsForOrganisation(org.getId());
        Instant organisationCreatedAt = org.getCreatedAt() != null ? org.getCreatedAt() : now;

        String temporaryPassword = resolveDefaultAdminPassword();
        AuthIdentity identity = AuthIdentity.builder()
                .organisation(org)
                .loginIdentifier(normalizedEmail)
                .normalisedLoginIdentifier(normalizedEmail)
                .email(normalizedEmail)
                .normalisedEmail(normalizedEmail)
                .username(normalizedEmail)
                .normalisedUsername(normalizedEmail)
                .fullName((req.getPrimaryAdminFirstName().trim() + " " + req.getPrimaryAdminLastName().trim()).trim())
                .identityType(IdentityType.STAFF)
                .authProvider(AuthProvider.LOCAL)
                .ssoEnabled(false)
                .emailVerified(false)
                .isActive(true)
                .mustChangePassword(true)
                .passwordHash(passwordEncoder.encode(temporaryPassword))
                .build();
        identity = authIdentityRepository.save(identity);

        var adminRole = roleRepository.findByNameIgnoreCaseAndOrganisation_Id(RoleName.ADMIN.name(), org.getId())
                .or(() -> roleRepository.findByName(RoleName.ADMIN.name()))
                .orElseThrow(() -> new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "ADMIN role not found"));
        AuthIdentityRole identityRole = AuthIdentityRole.builder()
                .authIdentity(identity)
                .organisation(org)
                .role(adminRole)
                .build();
        authIdentityRoleRepository.save(identityRole);

        UserOrganisation userOrg = UserOrganisation.builder()
                .auth(identity)
                .organisation(org)
                .createdAt(now)
                .build();
        userOrganisationRepository.save(userOrg);

        String billingCycle = normalizeBillingCycle(req.getBillingCycle());
        boolean annualBilling = "yearly".equals(billingCycle);
        BigDecimal priceAtTime = annualBilling && plan.getAnnualPrice() != null
                ? plan.getAnnualPrice()
                : plan.getBasePrice();
        Integer effectiveTrialDays = resolveRequestedTrialDays(req.getTrialDays());
        Instant trialEndAt = effectiveTrialDays == null ? null : organisationCreatedAt.plus(effectiveTrialDays.longValue(), ChronoUnit.DAYS);
        OrgSubscription subscription = OrgSubscription.builder()
                .organisation(org)
                .plan(plan)
                .status(effectiveTrialDays == null ? "active" : "trialing")
                .startAt(organisationCreatedAt)
                .priceAtTime(priceAtTime)
                .billingCycleAtTime(billingCycle)
                .trialEndAt(trialEndAt)
                .createdAt(organisationCreatedAt)
                .build();
        subscription = orgSubscriptionRepository.save(subscription);

        Instant billingPeriodEnd = resolveOnboardingPeriodEnd(
                organisationCreatedAt,
                trialEndAt,
                subscription.getBillingCycleAtTime()
        );
        boolean isTrialing = effectiveTrialDays != null;
        BigDecimal invoiceAmount = isTrialing ? BigDecimal.ZERO : (priceAtTime != null ? priceAtTime : BigDecimal.ZERO);
        boolean isPayable = invoiceAmount.compareTo(BigDecimal.ZERO) > 0;
        InvoiceStatus invoiceStatus = isPayable ? InvoiceStatus.PENDING : InvoiceStatus.PAID;
        Invoice invoice = Invoice.builder()
                .subscription(subscription)
                .amount(invoiceAmount)
                .outstandingBalance(isPayable ? invoiceAmount : BigDecimal.ZERO)
                .totalPaid(isPayable ? BigDecimal.ZERO : invoiceAmount)
                .refundedAmount(BigDecimal.ZERO)
                .billingPeriodStart(organisationCreatedAt)
                .billingPeriodEnd(billingPeriodEnd)
                .status(invoiceStatus)
                .dueDate((isPayable ? organisationCreatedAt : billingPeriodEnd).atZone(ZoneOffset.UTC).toLocalDate())
                .paidAt(isPayable ? null : organisationCreatedAt)
                .build();
        invoiceRepository.save(invoice);

        seedPrimaryBillingContact(org, normalizedEmail, req.getPrimaryAdminFirstName(), req.getPrimaryAdminLastName(), now);

        if (Boolean.TRUE.equals(req.getProvisionStripeSubscription())) {
            try {
                stripePlatformSubscriptionService.provisionStripeSubscription(org.getId(), actorAuthId);
            } catch (StoryApiException ex) {
                log.warn("Stripe subscription provisioning skipped during onboarding for org {}: {}", org.getId(), ex.getMessage());
            }
        }

        if (req.getProvisionTenant() == null || req.getProvisionTenant()) {
            tenantProvisioningService.queueTenantProvisioning(org.getId());
            if (provisionTenantSynchronously) {
                // Optional for controlled environments; may increase request latency.
                tenantProvisioningService.provisionTenantSchema(org.getId());
            } else {
                log.info("Tenant provisioning queued asynchronously for org {} - triggering migration scheduler", org.getId());
                // Immediately trigger the migration scheduler to process this tenant
                tenantMigrationScheduler.runMigrationBatchNow();
            }
        }
        tenantDirectoryService.evictCache();
        platformAuditService.log(actorAuthId, "ORGANISATION_ONBOARDED", "Organisation", String.valueOf(org.getId()),
                "plan=" + plan.getName()
                        + ", billingCycle=" + billingCycle
                        + ", adminEmail=" + normalizedEmail
                        + ", adminName=" + req.getPrimaryAdminFirstName().trim() + " " + req.getPrimaryAdminLastName().trim());
        String adminName = (req.getPrimaryAdminFirstName().trim() + " " + req.getPrimaryAdminLastName().trim()).trim();
        eventPublisher.publishEvent(new OrganisationOnboardedEvent(
                org.getId(),
                org.getName(),
                normalizedEmail,
                adminName,
                temporaryPassword,
                actorAuthId
        ));
        return org;
    }

    private void seedPrimaryBillingContact(Organisation org,
                                           String email,
                                           String firstName,
                                           String lastName,
                                           Instant now) {
        if (org == null || email == null || email.isBlank()) {
            return;
        }
        BillingContact contact = BillingContact.builder()
                .organisation(org)
                .fullName((firstName.trim() + " " + lastName.trim()).trim())
                .email(email.trim().toLowerCase(Locale.ROOT))
                .isPrimary(true)
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        billingContactRepository.save(contact);
    }

    private String resolveDefaultAdminPassword() {
        if (defaultAdminPassword == null || defaultAdminPassword.isBlank()) {
            throw new StoryApiException(
                    HttpStatus.BAD_REQUEST,
                    "ONBOARDING_ADMIN_PASSWORD_NOT_CONFIGURED",
                    "Set SUPERADMIN_ONBOARDING_DEFAULT_ADMIN_PASSWORD environment variable"
            );
        }
        return defaultAdminPassword;
    }

    private void validate(SuperAdminOnboardingCreateOrganisationRequest req) {
        if (req.getTrialDays() != null && req.getTrialDays() != 14 && req.getTrialDays() != 30) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_TRIAL_DAYS", "trialDays must be 14 or 30");
        }
        normalizeBillingCycle(req.getBillingCycle());
        String residency = req.getDataResidency() == null ? "" : req.getDataResidency().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("US", "EU", "AU").contains(residency)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "dataResidency must be US, EU, or AU");
        }
        try {
            ZoneId.of(req.getTimezone().trim());
        } catch (Exception ex) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "timezone must be a valid IANA zone");
        }
    }

    /** Normalizes to internal {@code monthly} / {@code yearly}; accepts annual as alias. */
    private static String normalizeBillingCycle(String billingCycle) {
        String incoming = billingCycle != null ? billingCycle.trim().toLowerCase(Locale.ROOT) : "";
        if ("monthly".equals(incoming)) {
            return "monthly";
        }
        if ("annual".equals(incoming) || "yearly".equals(incoming)) {
            return "yearly";
        }
        throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "billingCycle must be monthly, annual, or yearly");
    }

    private static Integer resolveRequestedTrialDays(Integer requestedTrialDays) {
        if (requestedTrialDays != null) {
            return requestedTrialDays > 0 ? requestedTrialDays : null;
        }
        return null;
    }

    private static Instant resolveOnboardingPeriodEnd(Instant periodStart, Instant trialEndAt, String billingCycleAtTime) {
        if (trialEndAt != null) {
            return trialEndAt;
        }
        ZonedDateTime start = periodStart.atZone(ZoneOffset.UTC);
        if ("yearly".equalsIgnoreCase(billingCycleAtTime)) {
            return start.plusYears(1).toInstant();
        }
        return start.plusMonths(1).toInstant();
    }

}
