package com.smart.therapy.flow.billing.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.billing.audit.BillingAuditActions;
import com.smart.therapy.flow.billing.dto.*;
import com.smart.therapy.flow.billing.enums.BillingStatus;
import com.smart.therapy.flow.billing.enums.BillingRecordStatusFilter;
import com.smart.therapy.flow.billing.enums.DiscountType;
import com.smart.therapy.flow.billing.enums.PaymentMethod;
import com.smart.therapy.flow.billing.enums.PaymentSource;
import com.smart.therapy.flow.billing.enums.PaymentStatus;
import com.smart.therapy.flow.billing.entity.Payment;
import com.smart.therapy.flow.billing.entity.PaymentTransaction;
import com.smart.therapy.flow.billing.enums.TransactionType;
import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.billing.repository.SessionBillingRepository;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ConflictException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.service.EmailAppLinks;
import com.smart.therapy.flow.common.service.EmailHtmlComponents;
import com.smart.therapy.flow.common.security.CaseloadScopeService;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.notification.service.NotificationEventCatalog;
import com.smart.therapy.flow.notification.service.NotificationPayloadFactory;
import com.smart.therapy.flow.organisation.service.TenantFeatureService;
import com.smart.therapy.flow.report.service.ClientReportAccessService;
import com.smart.therapy.flow.report.util.HtmlToPdfConverter;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.system.dto.PracticeConfigurationResponse;
import com.smart.therapy.flow.system.service.PracticeConfigurationService;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@Slf4j
@RequiredArgsConstructor
public class BillingService {

    private static final String RESOURCE_TYPE_BILLING = "billing";

    private final ServiceRepository serviceRepository;
    private final SessionBillingRepository sessionBillingRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final com.smart.therapy.flow.client.repository.ClientRepository clientRepository;
    private final com.smart.therapy.flow.client.service.ClientSearchHelper clientSearchHelper;
    private final com.smart.therapy.flow.billing.repository.PaymentRepository paymentRepository;
    private final com.smart.therapy.flow.billing.repository.PaymentTransactionRepository paymentTransactionRepository;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;
    private final CaseloadScopeService caseloadScopeService;
    private final TenantFeatureService tenantFeatureService;
    private final PracticeConfigurationService practiceConfigurationService;
    private final UserProfileRepository userProfileRepository;
    private final InvoicePolicyService invoicePolicyService;
    private final com.smart.therapy.flow.client.repository.ClientInsuranceRepository clientInsuranceRepository;
    private final BillingGuard billingGuard;
    private final ClientReportAccessService clientReportAccessService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.smart.therapy.flow.common.service.EmailService emailService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.smart.therapy.flow.common.config.AppProperties appProperties;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.smart.therapy.flow.notification.service.NotificationService notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    @Cacheable(value = "services", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('all:' + (#activeOnly != null ? #activeOnly : 'all') + ':' + (#therapistVisible != null ? #therapistVisible : 'all') + ':' + (#clientPortalVisible != null ? #clientPortalVisible : 'all'))")
    public List<ServiceResponse> getServices(Boolean activeOnly, Boolean therapistVisible, Boolean clientPortalVisible) {
        return serviceRepository.findAll().stream()
                .filter(service -> isMatch(service, activeOnly, therapistVisible, clientPortalVisible))
                .map(this::toServiceResponse)
                .toList();
    }

    private boolean isMatch(
            com.smart.therapy.flow.billing.entity.Service service,
            Boolean activeOnly,
            Boolean therapistVisible,
            Boolean clientPortalVisible) {

        if (Boolean.TRUE.equals(activeOnly) && !Boolean.TRUE.equals(service.getIsActive())) {
            return false;
        }

        if (matchesFilter(therapistVisible, service.getTherapistVisible())) {
            return false;
        }

        if (matchesFilter(clientPortalVisible, service.getClientPortalVisible())) {
            return false;
        }

        return true;
    }

    private boolean matchesFilter(Boolean requested, Boolean actual) {
        return requested != null && !Objects.equals(requested, actual);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "services", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('id:' + #serviceId)")
    public ServiceResponse getService(Long serviceId) {
        com.smart.therapy.flow.billing.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        return toServiceResponse(service);
    }

    @Transactional
    @CacheEvict(value = "services", allEntries = true)
    public ServiceResponse createService(CreateServiceRequest request, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        final String serviceCode = normalize(request.getServiceCode(), "Service code");
        final String serviceName = normalize(request.getServiceName(), "Service name");

        if (serviceRepository.findByServiceCode(serviceCode).isPresent()) {
            throw new BadRequestException("Service code already exists");
        }

        com.smart.therapy.flow.billing.entity.Service service = toServiceEntity(request, serviceCode, serviceName);
        com.smart.therapy.flow.billing.entity.Service saved = Objects.requireNonNull(
                serviceRepository.save(service),
                "Persisted service must not be null"
        );

        Long savedId = requireServiceId(saved);
        recordAuditEvent(
                currentUserService.requireCurrentUser(requester).getId(),
                "service_created",
                savedId,
                ipAddress
        );

        return toServiceResponse(saved);
    }

    private com.smart.therapy.flow.billing.entity.Service toServiceEntity(
            CreateServiceRequest request, String serviceCode, String serviceName) {

        return com.smart.therapy.flow.billing.entity.Service.builder()
                .serviceCode(serviceCode)
                .serviceName(serviceName)
                .description(request.getDescription())
                .duration(request.getDurationInMinutes())
                .baseRate(request.getBaseRate())
                .isActive(defaultIfNull(request.getIsActive(), true))
                .therapistVisible(defaultIfNull(request.getTherapistVisible(), true))
                .clientPortalVisible(defaultIfNull(request.getClientPortalVisible(), false))
                .publicSiteEnabled(defaultIfNull(request.getPublicSiteEnabled(), false))
                .build();
    }

    private String normalize(String value, String fieldName) {
        String trimmed = value == null ? null : value.trim();
        if (trimmed == null || trimmed.isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }
        return trimmed;
    }

    private boolean defaultIfNull(Boolean value, boolean defaultValue) {
        return value != null ? value : defaultValue;
    }

    @Transactional
    @CacheEvict(value = "services", allEntries = true)
    public ServiceResponse updateService(Long serviceId, UpdateServiceRequest request, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(serviceId, "Service id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        com.smart.therapy.flow.billing.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        applyIfPresent(request.getServiceCode(), service::setServiceCode,
                code -> validateAndNormalizeText(code, "Service code"));
        applyIfPresent(request.getServiceName(), service::setServiceName,
                name -> validateAndNormalizeText(name, "Service name"));

        validateUpdatedServiceCode(serviceId, service);

        applyIfPresent(request.getDescription(), service::setDescription, value -> value);
        applyIfPresent(request.getDurationInMinutes(), service::setDuration, value -> value);
        applyIfPresent(request.getBaseRate(), service::setBaseRate, value -> value);
        applyIfPresent(request.getIsActive(), service::setIsActive, value -> value);
        applyIfPresent(request.getTherapistVisible(), service::setTherapistVisible, value -> value);
        applyIfPresent(request.getClientPortalVisible(), service::setClientPortalVisible, value -> value);
        applyIfPresent(request.getPublicSiteEnabled(), service::setPublicSiteEnabled, value -> value);

        com.smart.therapy.flow.billing.entity.Service updated = Objects.requireNonNull(
                serviceRepository.saveAndFlush(service),
                "Persisted service must not be null"
        );

        Long updatedId = requireServiceId(updated);
        recordAuditEvent(
                currentUserService.requireCurrentUser(requester).getId(),
                "service_updated",
                updatedId,
                ipAddress
        );

        return toServiceResponse(updated);
    }

    private <T, R> void applyIfPresent(T value, java.util.function.Consumer<R> setter, java.util.function.Function<T, R> mapper) {
        if (value != null) {
            setter.accept(mapper.apply(value));
        }
    }

    private String validateAndNormalizeText(String value, String fieldName) {
        String trimmed = value == null ? null : value.trim();
        if (trimmed == null || trimmed.isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }
        return trimmed;
    }

    private void validateUpdatedServiceCode(Long serviceId, com.smart.therapy.flow.billing.entity.Service service) {
        String currentCode = service.getServiceCode();
        serviceRepository.findByServiceCode(currentCode)
                .filter(existing -> !existing.getId().equals(serviceId))
                .ifPresent(existing -> {
                    throw new BadRequestException("Service code already exists");
                });
    }

    /**
     * Bulk update: set therapist visibility for all services.
     * Used by admin-only endpoints to show/hide all services for therapists.
     */
    @Transactional
    @CacheEvict(value = "services", allEntries = true)
    public void setAllServicesTherapistVisibility(boolean visible, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(requester, "Requester is required");
        serviceRepository.updateAllTherapistVisible(visible);

        // Optional: record a single audit event for the bulk operation
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                visible ? "services_therapist_visibility_set_all_true" : "services_therapist_visibility_set_all_false",
                null,
                ipAddress);
    }

    @Transactional
    @CacheEvict(value = "services", allEntries = true)
    public void deleteService(Long serviceId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(serviceId, "Service ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can delete services");

        com.smart.therapy.flow.billing.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        // Check if service is in use (has sessions or billing records)
        long sessionCount = sessionRepository.countByServiceId(serviceId);
        long billingCount = sessionBillingRepository.countByServiceCode(service.getServiceCode());

        if (sessionCount > 0 || billingCount > 0) {
            throw new BadRequestException(
                    String.format(
                            "Cannot delete service: %d session(s) and %d billing record(s) are associated with this service",
                            sessionCount, billingCount));
        }

        serviceRepository.delete(service);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "service_deleted", serviceId, ipAddress);
    }

    @Transactional
    public SessionBillingResponse createSessionBilling(CreateSessionBillingRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");
        billingGuard.requireTenantContext();

        if (request.getSessionId() == null) {
            throw new BadRequestException("Session is required to create billing");
        }

        Session session = sessionRepository.findByIdForBilling(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        billingGuard.requireSessionForBilling(session);
        billingGuard.assertStaffBillingAccess(SessionBilling.builder().session(session).build(), requester);

        // Check if billing already exists for this session
        if (sessionBillingRepository.findBySessionId(request.getSessionId()).isPresent()) {
            throw new BadRequestException("Billing already exists for this session");
        }

        com.smart.therapy.flow.billing.entity.Service service = null;
        if (request.getServiceId() != null) {
            service = serviceRepository.findById(request.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        } else if (request.getServiceCode() != null) {
            service = serviceRepository.findByServiceCode(request.getServiceCode())
                    .orElse(null);
        } else if (session.getService() != null) {
            // Default to the service attached to the session so completion-triggered
            // billing inherits the actual booked service/rate.
            service = session.getService();
        }

        BigDecimal unitRate = request.getUnitRate();
        if (unitRate == null && service != null) {
            unitRate = service.getBaseRate();
        } else if (unitRate == null) {
            throw new BadRequestException("Service base rate is required before billing can be created");
        }
        billingGuard.requireRateConfigured(unitRate);

        Long policyServiceId = service != null ? service.getId() : null;
        ZoneId billingZone = resolveBillingZoneId();
        LocalDate policyBillingDate = request.getBillingDate() != null
                ? request.getBillingDate().atZone(billingZone).toLocalDate()
                : (session.getSessionDate() != null
                ? session.getSessionDate().atZone(billingZone).toLocalDate()
                : LocalDate.now(billingZone));
        InvoicePolicyRateResult policyRate = invoicePolicyService.resolveBillingRate(
                session, unitRate, policyServiceId, policyBillingDate);
        BigDecimal originalUnitRate = unitRate.setScale(2, RoundingMode.HALF_UP);
        unitRate = policyRate.getRatePerUnit();

        Integer units = request.getUnits() != null ? request.getUnits() : 1;
        BigDecimal originalSubtotal = originalUnitRate.multiply(BigDecimal.valueOf(units))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = unitRate.multiply(BigDecimal.valueOf(units))
                .setScale(2, RoundingMode.HALF_UP);

        InsuranceSnapshot insuranceSnapshot = resolveInsuranceSnapshot(session, request);

        String resolvedServiceCode = service != null ? service.getServiceCode() : request.getServiceCode();
        billingGuard.requireServiceConfigured(service, resolvedServiceCode);
        SessionBilling billing = SessionBilling.builder()
                .session(session)
                .serviceCode(resolvedServiceCode)
                .ratePerUnit(unitRate)
                .originalRatePerUnit(originalUnitRate)
                .units(units)
                .subtotalAmount(totalAmount)
                .originalSubtotalAmount(originalSubtotal)
                .totalAmount(totalAmount)
                .insuranceCovered(insuranceSnapshot.insuranceCovered())
                .billingStatus(BillingStatus.PENDING)
                .billingDate(policyBillingDate)
                .copayAmount(insuranceSnapshot.copayAmount())
                .invoicePolicyId(policyRate.getInvoicePolicyId())
                .build();
        applyDiscountValues(billing, request.getDiscountType(), request.getDiscountValue(),
                request.getDiscountAmount());
        billing.setOutstandingAmount(calculateAmountDue(billing).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));

        SessionBilling saved = Objects.requireNonNull(sessionBillingRepository.save(billing),
                "Persisted billing must not be null");
        Long savedId = requireBillingId(saved);
        session.setCalculatedRate(unitRate);
        sessionRepository.save(session);
        applyAvailableClientCredit(saved);

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), BillingAuditActions.BILLING_CREATED, savedId, ipAddress);
        if (notificationService != null) {
            try {
                notificationService.processEventInNewTransaction(
                        NotificationEventCatalog.BILL_GENERATED,
                        buildBillingEventData(saved));
                if (saved.getDueDate() != null
                        && saved.getOutstandingAmount() != null
                        && saved.getOutstandingAmount().compareTo(BigDecimal.ZERO) > 0) {
                    notificationService.processEventInNewTransaction(
                            NotificationEventCatalog.BILL_DUE_REMINDER,
                            buildBillingEventData(saved));
                }
            } catch (Exception e) {
                log.error("Failed to trigger billing lifecycle notifications for billingId={}", savedId, e);
            }
        }
        return toSessionBillingResponse(saved);
    }

    @Transactional(readOnly = true)
    public SessionBillingResponse getSessionBilling(Long sessionId, AuthPrincipal requester, String ipAddress) {
        billingGuard.requireTenantContext();
        SessionBilling billing = sessionBillingRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session billing not found"));
        if (requester != null) {
            billingGuard.assertStaffBillingAccess(billing, requester);
            Long clientId = billing.getSession() != null && billing.getSession().getClient() != null
                    ? billing.getSession().getClient().getId()
                    : null;
            recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                    BillingAuditActions.INVOICE_VIEWED, billing.getId(), clientId, ipAddress, true);
        }
        return toSessionBillingResponse(billing);
    }

    @Transactional(readOnly = true)
    public SessionBillingResponse getSessionBilling(Long sessionId) {
        return getSessionBilling(sessionId, null, null);
    }

    @Transactional(readOnly = true)
    public boolean hasSessionBilling(Long sessionId) {
        Objects.requireNonNull(sessionId, "Session id is required");
        return sessionBillingRepository.existsBySessionId(sessionId);
    }

    @Transactional
    public SessionBillingResponse updatePaymentStatus(Long billingId, String paymentStatus,
            String stripePaymentIntentId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(billingId, "Billing id is required");
        Objects.requireNonNull(paymentStatus, "Payment status is required");
        Objects.requireNonNull(requester, "Requester is required");

        SessionBilling billing = sessionBillingRepository.findByIdForUpdate(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Session billing not found"));
        billingGuard.assertStaffBillingAccess(billing, requester);

        try {
            PaymentStatus status = PaymentStatus.fromValue(paymentStatus);
            if (status == PaymentStatus.REFUNDED) {
                throw new BadRequestException("Use Refund Payment to record a refund transaction");
            }
            if (status == PaymentStatus.FAILED) {
                billing.setBillingStatus(BillingStatus.DENIED);
            } else {
                requireLedgerStatus(billing, status);
                recalculateBillingAmountsAndStatus(billing);
                billing.setBillingStatus(status == PaymentStatus.PAID ? BillingStatus.PAID
                        : status == PaymentStatus.PARTIAL ? BillingStatus.BILLED : BillingStatus.PENDING);
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid payment status: " + paymentStatus);
        }

        // stripePaymentIntentId is accepted for compatibility but not persisted in SessionBilling

        SessionBilling updated = sessionBillingRepository.save(billing);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "payment_status_updated", billingId, ipAddress);
        if (notificationService != null) {
            try {
                if ("PAID".equalsIgnoreCase(paymentStatus)) {
                    notificationService.processEventInNewTransaction(
                            NotificationEventCatalog.PAYMENT_RECEIVED,
                            buildBillingEventData(updated));
                } else if ("FAILED".equalsIgnoreCase(paymentStatus) || "DENIED".equalsIgnoreCase(paymentStatus)) {
                    notificationService.processEventInNewTransaction(
                            NotificationEventCatalog.PAYMENT_FAILED,
                            buildBillingEventData(updated));
                }
            } catch (Exception e) {
                log.error("Failed to trigger payment lifecycle notification for billingId={} status={}", billingId,
                        paymentStatus, e);
            }
        }

        return toSessionBillingResponse(updated);
    }

    private record InsuranceSnapshot(boolean insuranceCovered, BigDecimal copayAmount) {
    }

    private InsuranceSnapshot resolveInsuranceSnapshot(Session session, CreateSessionBillingRequest request) {
        if (request.getInsuranceCovered() != null || request.getCopayAmount() != null) {
            return new InsuranceSnapshot(
                    Boolean.TRUE.equals(request.getInsuranceCovered()),
                    request.getCopayAmount() != null ? request.getCopayAmount() : BigDecimal.ZERO);
        }
        if (session.getClient() == null || session.getClient().getId() == null) {
            return new InsuranceSnapshot(false, BigDecimal.ZERO);
        }
        return clientInsuranceRepository.findActiveByClientId(session.getClient().getId())
                .filter(insurance -> Boolean.TRUE.equals(insurance.getIsActive()) && insurance.isActive())
                .map(insurance -> {
                    BigDecimal copay = insurance.getCopayAmount() != null
                            ? insurance.getCopayAmount()
                            : BigDecimal.ZERO;
                    return new InsuranceSnapshot(true, copay);
                })
                .orElseGet(() -> new InsuranceSnapshot(false, BigDecimal.ZERO));
    }

    @Transactional
    public SessionBillingResponse applyDiscount(Long billingId, ApplyDiscountRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(billingId, "Billing id is required");
        Objects.requireNonNull(requester, "Requester is required");
        billingGuard.requireTenantContext();

        // Gate by tenant feature: advanced billing (e.g. discounts) can be enabled per organisation.
        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null && !tenantFeatureService.isAdvancedBillingEnabled(orgId)) {
            throw new ForbiddenException("Applying discounts requires the ADVANCED_BILLING feature for this organisation");
        }

        // PBAC: Check permission to manage billing
        if (!permissionChecker.hasPermission(requester, "BILLING_MANAGE")) {
            throw new ForbiddenException("Only users with BILLING_MANAGE permission can apply discounts");
        }

        SessionBilling billing = sessionBillingRepository.findById(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Session billing not found"));

        DiscountType existingType = billing.getDiscountType();
        boolean hasExistingDiscount = existingType != null && existingType.hasDiscount()
                && billing.getDiscountAmount() != null
                && billing.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0;
        String requestedType = request.getDiscountType() != null ? request.getDiscountType().trim() : "";
        boolean clearingDiscount = requestedType.isEmpty()
                || "none".equalsIgnoreCase(requestedType)
                || "no_discount".equalsIgnoreCase(requestedType);
        if (hasExistingDiscount && !clearingDiscount) {
            throw new BadRequestException(
                    "A discount has already been applied to this invoice. Remove it before applying a different discount.");
        }

        applyDiscountValues(billing, request.getDiscountType(), request.getDiscountValue(),
                request.getDiscountAmount());
        BigDecimal amountDue = calculateAmountDue(billing);
        BigDecimal paidAmount = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal outstanding = amountDue.subtract(paidAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        billing.setOutstandingAmount(outstanding);
        syncBillingStatusAfterDiscount(billing, outstanding);

        SessionBilling updated = sessionBillingRepository.save(billing);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), BillingAuditActions.DISCOUNT_APPLIED, billingId, ipAddress);
        return toSessionBillingResponse(updated);
    }

    @Transactional(readOnly = true)
    public Page<SessionBillingResponse> getBillingRecords(
            Long clientId,
            String clientSearch,
            Long therapistId,
            String status,
            String paymentStatus,
            String serviceCode,
            String clientType,
            String sessionType,
            String paymentMethod,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Pageable pageable,
            AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        // DEBUG: Log request details
        Long currentUserId = currentUserService.requireCurrentUser(requester).getId();
        BillingCaseloadScope scope = resolveBillingCaseloadScope(requester);
        log.info("getBillingRecords - userId={}, authId={}, scope={}, requestedTherapistId={}, requestedClientId={}",
                currentUserId, requester.getAuthId(), scope, therapistId, clientId);

        Long effectiveTherapistId = resolveEffectiveTherapistFilter(requester, therapistId);
        log.info("getBillingRecords - effectiveTherapistId={} (null means use applyTherapistClientScope)", effectiveTherapistId);

        BillingRecordStatusFilter statusFilter = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusFilter = BillingRecordStatusFilter.fromValue(status.trim());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status filter: " + status
                        + ". Allowed values: pending, partial, paid, denied, refunded");
            }
        }

        Specification<SessionBilling> spec = buildBillingSpecification(
                clientId, clientSearch, effectiveTherapistId, statusFilter, serviceCode, clientType, sessionType, paymentMethod,
                startDate, endDate, minAmount, maxAmount, paymentStatus);
        spec = applyTherapistClientScope(requester, spec);

        Page<SessionBilling> billingPage = findBillingRecordsClientHubOrder(spec, pageable);
        log.info("getBillingRecords - Query returned {} results (total: {})",
                billingPage.getNumberOfElements(), billingPage.getTotalElements());

        Page<SessionBillingResponse> result = billingPage.map(this::toSessionBillingResponse);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                "billing_list_viewed", null, clientId, null, true,
                "resultCount=" + result.getNumberOfElements() + ",total=" + result.getTotalElements()
                        + ",page=" + (result.getNumber() + 1) + ",pageSize=" + result.getSize());
        return result;
    }

    private Specification<SessionBilling> buildBillingSpecification(
            Long clientId,
            String clientSearch,
            Long therapistId,
            BillingRecordStatusFilter statusFilter,
            String serviceCode,
            String clientType,
            String sessionType,
            String paymentMethod,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal minAmount,
            BigDecimal maxAmount) {
        return buildBillingSpecification(clientId, clientSearch, therapistId, statusFilter, serviceCode,
                clientType, sessionType, paymentMethod, startDate, endDate, minAmount, maxAmount, null);
    }

    private Specification<SessionBilling> buildBillingSpecification(
            Long clientId,
            String clientSearch,
            Long therapistId,
            BillingRecordStatusFilter statusFilter,
            String serviceCode,
            String clientType,
            String sessionType,
            String paymentMethod,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal minAmount,
            BigDecimal maxAmount, String paymentStatus) {
        List<Long> typeIds = null;
        if (StringUtils.hasText(clientType)) {
            String normalized = clientType.trim().toLowerCase(java.util.Locale.ROOT);
            if (!java.util.Set.of("individual", "couple", "family", "group", "refugee", "mva").contains(normalized)) {
                throw new BadRequestException("Invalid client type filter");
            }
            typeIds = clientSearchHelper.findClientIdsMatchingType(normalized);
        }
        final List<Long> matchingTypeIds = typeIds;
        final String mode;
        if (!StringUtils.hasText(sessionType)) mode = null;
        else mode = switch (sessionType.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "in_person", "in-person" -> "in-person";
            case "online", "virtual", "telehealth" -> "online";
            default -> throw new BadRequestException("Invalid session type filter");
        };
        final PaymentMethod method;
        try { method = StringUtils.hasText(paymentMethod) ? PaymentMethod.fromValue(paymentMethod.trim()) : null; }
        catch (IllegalArgumentException ex) { throw new BadRequestException("Invalid payment method filter"); }
        final PaymentStatus paymentState;
        try { paymentState = StringUtils.hasText(paymentStatus) ? PaymentStatus.fromValue(paymentStatus.trim()) : null; }
        catch (IllegalArgumentException ex) { throw new BadRequestException("Invalid payment status filter"); }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) throw new BadRequestException("Start date cannot be after end date");
        if ((minAmount != null && minAmount.signum() < 0) || (maxAmount != null && maxAmount.signum() < 0)
                || (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0)) {
            throw new BadRequestException("Invalid amount range");
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by client ID
            if (clientId != null) {
                predicates.add(cb.equal(root.get("session").get("client").get("id"), clientId));
            }

            // Exact-match client search (MRN / name / email / phone) — no LIKE on encrypted PHI
            if (clientSearch != null && !clientSearch.trim().isEmpty()) {
                String term = clientSearch.trim();
                predicates.add(clientSearchHelper.predicateForClientPath(
                        root.get("session").get("client"), query, cb, term));
            }

            // Filter by therapist ID
            if (therapistId != null) {
                predicates.add(cb.equal(root.get("session").get("therapist").get("id"), therapistId));
            }

            // Unified status filter for billing table dropdown.
            if (statusFilter != null) {
                switch (statusFilter) {
                    case PENDING -> predicates.add(cb.equal(root.get("billingStatus"), BillingStatus.PENDING));
                    case PARTIAL -> {
                        // Imported and older bills may remain BILLED after a partial payment.
                        var due = cb.diff(cb.coalesce(root.<BigDecimal>get("totalAmount"), BigDecimal.ZERO),
                                cb.coalesce(root.<BigDecimal>get("discountAmount"), BigDecimal.ZERO));
                        var paid = cb.coalesce(root.<BigDecimal>get("paidAmount"), BigDecimal.ZERO);
                        predicates.add(cb.and(
                                root.get("billingStatus").in(BillingStatus.PAID, BillingStatus.DENIED, BillingStatus.CANCELLED).not(),
                                cb.gt(paid, BigDecimal.ZERO), cb.lt(paid, due)));
                    }
                    case PAID -> predicates.add(cb.equal(root.get("billingStatus"), BillingStatus.PAID));
                    case DENIED -> predicates.add(cb.equal(root.get("billingStatus"), BillingStatus.DENIED));
                    case REFUNDED -> {
                        var refundedSubQuery = query.subquery(Long.class);
                        var paymentRoot = refundedSubQuery.from(com.smart.therapy.flow.billing.entity.Payment.class);
                        refundedSubQuery.select(paymentRoot.get("id"));
                        refundedSubQuery.where(
                                cb.equal(paymentRoot.get("sessionBilling").get("id"), root.get("id")),
                                cb.equal(paymentRoot.get("status"), PaymentStatus.REFUNDED));
                        predicates.add(cb.exists(refundedSubQuery));
                    }
                }
            }

            // Filter by service code
            if (serviceCode != null && !serviceCode.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("serviceCode"), serviceCode.trim()));
            }

            if (matchingTypeIds != null) {
                predicates.add(matchingTypeIds.isEmpty() ? cb.disjunction()
                        : root.get("session").get("client").get("id").in(matchingTypeIds));
            }
            if (mode != null) predicates.add(cb.equal(root.get("session").get("sessionType"), mode));
            if (method != null) {
                var sub = query.subquery(Long.class);
                var payment = sub.from(Payment.class);
                sub.select(payment.get("id")).where(
                        cb.equal(payment.get("sessionBilling").get("id"), root.get("id")),
                        cb.equal(payment.get("paymentMethod"), method));
                predicates.add(cb.exists(sub));
            }
            if (paymentState != null) {
                if (paymentState == PaymentStatus.FAILED || paymentState == PaymentStatus.REFUNDED) {
                    var sub = query.subquery(Long.class);
                    var payment = sub.from(Payment.class);
                    sub.select(payment.get("id")).where(
                            cb.equal(payment.get("sessionBilling").get("id"), root.get("id")),
                            cb.equal(payment.get("status"), paymentState));
                    predicates.add(cb.exists(sub));
                } else {
                    var due = cb.diff(cb.coalesce(root.<BigDecimal>get("totalAmount"), BigDecimal.ZERO),
                            cb.coalesce(root.<BigDecimal>get("discountAmount"), BigDecimal.ZERO));
                    var paid = cb.coalesce(root.<BigDecimal>get("paidAmount"), BigDecimal.ZERO);
                    Predicate paidInFull = cb.or(cb.equal(root.get("billingStatus"), BillingStatus.PAID),
                            cb.le(due, BigDecimal.ZERO), cb.ge(paid, due));
                    predicates.add(root.get("billingStatus").in(BillingStatus.DENIED, BillingStatus.CANCELLED).not());
                    predicates.add(switch (paymentState) {
                        case PAID -> paidInFull;
                        case PARTIAL -> cb.and(cb.not(paidInFull), cb.gt(paid, BigDecimal.ZERO));
                        case PENDING -> cb.and(cb.not(paidInFull), cb.le(paid, BigDecimal.ZERO));
                        default -> throw new IllegalStateException("Unexpected payment status");
                    });
                }
            }

            // Match ClientHubAI /api/billing/reports: DATE(session_date) range in practice TZ.
            ZoneId zone = resolveBillingZoneId();
            if (startDate != null) {
                Instant startInstant = startDate.atStartOfDay(zone).toInstant();
                predicates.add(cb.greaterThanOrEqualTo(root.get("session").get("sessionDate"), startInstant));
            }
            if (endDate != null) {
                Instant endExclusive = endDate.plusDays(1).atStartOfDay(zone).toInstant();
                predicates.add(cb.lessThan(root.get("session").get("sessionDate"), endExclusive));
            }

            // Filter by amount range
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), maxAmount));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Use the Administration practice timezone consistently for billing dates and filters.
     * Fall back to UTC when configuration is unavailable; never use the server timezone.
     */
    private ZoneId resolveBillingZoneId() {
        try {
            PracticeConfigurationResponse practice = practiceConfigurationService.getPracticeConfiguration();
            if (practice != null && StringUtils.hasText(practice.getTimezone())) {
                return ZoneId.of(practice.getTimezone().trim());
            }
        } catch (Exception ex) {
            log.warn("Falling back to UTC for billing dates because practice timezone is unavailable: {}",
                    ex.getMessage());
        }
        return java.time.ZoneOffset.UTC;
    }

    /**
     * Page billing rows using ClientHubAI-compatible order when sorting by billingDate.
     * Live ClientHub same-day order for newest-first lists is billing id descending
     * (Nisreen → Gerson → Miguel). Target ids are not monotonic with ClientHub source ids,
     * so we tie-break on legacy {@code session_billing} source ids (same direction as
     * billingDate) with database pagination.
     */
    private Page<SessionBilling> findBillingRecordsClientHubOrder(
            Specification<SessionBilling> spec,
            Pageable pageable) {
        if (!isBillingDatePrimarySort(pageable.getSort())) {
            return sessionBillingRepository.findAll(spec, pageable);
        }
        Sort.Direction billingDateDirection = Sort.Direction.DESC;
        Sort.Order billingDateOrder = pageable.getSort().getOrderFor("billingDate");
        if (billingDateOrder != null) {
            billingDateDirection = billingDateOrder.getDirection();
        }
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(SessionBilling.class);
        var root = query.from(SessionBilling.class);
        query.select(root).where(spec.toPredicate(root, query, cb));
        jakarta.persistence.criteria.Expression<BigDecimal> orderId = ((org.hibernate.query.criteria.JpaExpression<Long>) root.<Long>get("id")).cast(BigDecimal.class);
        Long organisationId = TenantContext.getOrganisationId();
        if (organisationId != null) {
            var legacy = query.subquery(BigDecimal.class);
            var mapping = legacy.from(com.smart.therapy.flow.billing.entity.ClientHubBillingOrderMapping.class);
            var raw = cb.trim(mapping.<String>get("sourceId"));
            var numeric = cb.<BigDecimal>selectCase().when(cb.and(
                    cb.le(cb.length(raw), 20),
                    cb.isTrue(cb.function("regexp_like", Boolean.class, raw, cb.literal("^[+-]?[0-9]+$")))),
                    ((org.hibernate.query.criteria.JpaExpression<String>) raw).cast(BigDecimal.class)).otherwise(cb.nullLiteral(BigDecimal.class));
            var validLong = cb.<BigDecimal>selectCase().when(
                    cb.between(numeric, BigDecimal.valueOf(Long.MIN_VALUE), BigDecimal.valueOf(Long.MAX_VALUE)), numeric)
                    .otherwise(cb.nullLiteral(BigDecimal.class));
            // Match the full target identity so V91's unique target index can seek one mapping per bill.
            // Without schema/table predicates PostgreSQL repeatedly scans the organisation's migration history.
            legacy.select(cb.max(validLong)).where(cb.equal(mapping.get("organisationId"), organisationId),
                    cb.equal(mapping.get("targetSchema"), TenantContext.getSchemaName()),
                    cb.equal(mapping.get("targetTable"), "session_billing"),
                    cb.equal(mapping.get("entityName"), "session_billing"), cb.equal(mapping.get("targetId"), root.get("id")));
            orderId = cb.coalesce(legacy, orderId);
        }
        boolean ascending = billingDateDirection == Sort.Direction.ASC;
        query.orderBy(cb.asc(cb.<Integer>selectCase().when(cb.isNull(root.get("billingDate")), 1).otherwise(0)),
                ascending ? cb.asc(root.get("billingDate")) : cb.desc(root.get("billingDate")),
                ascending ? cb.asc(orderId) : cb.desc(orderId),
                ascending ? cb.asc(root.get("id")) : cb.desc(root.get("id")));
        List<SessionBilling> rows = entityManager.createQuery(query)
                .setFirstResult(Math.toIntExact(pageable.getOffset())).setMaxResults(pageable.getPageSize()).getResultList();
        return new PageImpl<>(rows, pageable, sessionBillingRepository.count(spec));
    }

    private static boolean isBillingDatePrimarySort(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return true;
        }
        return sort.stream().anyMatch(order -> "billingDate".equalsIgnoreCase(order.getProperty()));
    }


    private void sortLikeClientHub(List<SessionBilling> rows, Sort.Direction billingDateDirection) {
        Map<Long, Long> legacyBillingIds = loadClientHubBillingSourceIds(rows);
        Comparator<LocalDate> dateComparator = billingDateDirection == Sort.Direction.ASC
                ? Comparator.nullsLast(Comparator.naturalOrder())
                : Comparator.nullsLast(Comparator.reverseOrder());
        Comparator<Long> idComparator = billingDateDirection == Sort.Direction.ASC
                ? Comparator.nullsLast(Comparator.naturalOrder())
                : Comparator.nullsLast(Comparator.reverseOrder());
        rows.sort(Comparator
                .comparing(SessionBilling::getBillingDate, dateComparator)
                .thenComparing((SessionBilling row) -> {
                    if (row.getId() == null) {
                        return billingDateDirection == Sort.Direction.ASC ? Long.MAX_VALUE : Long.MIN_VALUE;
                    }
                    return legacyBillingIds.getOrDefault(row.getId(), row.getId());
                }, idComparator)
                .thenComparing(SessionBilling::getId, idComparator));
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Long> loadClientHubBillingSourceIds(List<SessionBilling> rows) {
        Map<Long, Long> legacyByTarget = new HashMap<>();
        if (rows == null || rows.isEmpty()) {
            return legacyByTarget;
        }
        Long organisationId = TenantContext.getOrganisationId();
        if (organisationId == null) {
            return legacyByTarget;
        }
        List<Long> targetIds = rows.stream()
                .map(SessionBilling::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (targetIds.isEmpty()) {
            return legacyByTarget;
        }
        List<Object[]> mapped = entityManager.createNativeQuery("""
                SELECT target_id, source_id
                FROM public.clienthub_legacy_id_mappings
                WHERE organisation_id = :organisationId
                  AND entity_name = 'session_billing'
                  AND target_id IN (:targetIds)
                """)
                .setParameter("organisationId", organisationId)
                .setParameter("targetIds", targetIds)
                .getResultList();
        for (Object[] row : mapped) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            try {
                Long targetId = ((Number) row[0]).longValue();
                Long sourceId = Long.parseLong(String.valueOf(row[1]).trim());
                legacyByTarget.put(targetId, sourceId);
            } catch (NumberFormatException ignored) {
                // Non-numeric legacy ids are ignored; target id tie-break still applies.
            }
        }
        return legacyByTarget;
    }

    // Private helper methods

    private ServiceResponse toServiceResponse(com.smart.therapy.flow.billing.entity.Service service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .serviceCode(service.getServiceCode())
                .serviceName(service.getServiceName())
                .description(service.getDescription())
                .durationInMinutes(service.getDuration())
                .baseRate(service.getBaseRate())
                .isActive(service.getIsActive())
                .therapistVisible(service.getTherapistVisible())
                .clientPortalVisible(service.getClientPortalVisible())
                .publicSiteEnabled(service.getPublicSiteEnabled())
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .build();
    }

    private SessionBillingResponse toSessionBillingResponse(SessionBilling billing) {
        com.smart.therapy.flow.session.entity.Session session = billing.getSession();
        com.smart.therapy.flow.client.entity.Client client = session != null ? session.getClient() : null;
        com.smart.therapy.flow.auth.entity.User therapist = session != null ? session.getTherapist() : null;
        com.smart.therapy.flow.billing.entity.Service service = session != null && session.getService() != null
                ? session.getService()
                : null;

        BigDecimal amountDue = calculateAmountDue(billing);
        BigDecimal paidAmount = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal remainingDue = amountDue.subtract(paidAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal creditAmount = paidAmount.subtract(amountDue).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        String paymentStatus = derivePaymentStatus(billing, paidAmount, amountDue);

        Payment latestPayment = resolveLatestSuccessfulPayment(billing.getId());

        return SessionBillingResponse.builder()
                .id(billing.getId())
                .sessionId(session != null ? session.getId() : null)
                .sessionDate(session != null ? session.getSessionDate() : null)
                .sessionStatus(session != null ? session.getStatus() : null)
                .clientId(client != null ? client.getId() : null)
                .clientMrn(client != null ? client.getClientId() : null)
                .clientReferenceNumber(client != null && client.getReferral() != null
                        ? client.getReferral().getReferenceNumber() : null)
                .clientName(client != null ? client.getFullName() : null)
                .therapistId(therapist != null ? therapist.getId() : null)
                .therapistName(therapist != null ? therapist.getFullName() : null)
                .serviceId(service != null ? service.getId() : null)
                .serviceCode(billing.getServiceCode())
                .serviceName(service != null ? service.getServiceName() : null)
                .unitRate(billing.getRatePerUnit())
                .originalRatePerUnit(resolveOriginalRatePerUnit(billing, service))
                .units(billing.getUnits())
                .originalSubtotalAmount(resolveOriginalSubtotal(billing, service))
                .totalAmount(billing.getTotalAmount())
                .insuranceCovered(billing.getInsuranceCovered())
                .billingStatus(billing.getBillingStatus() != null ? billing.getBillingStatus().getValue() : null)
                .paymentStatus(paymentStatus)
                .paymentMethod(latestPayment != null && latestPayment.getPaymentMethod() != null
                        ? latestPayment.getPaymentMethod().getValue()
                        : null)
                .paymentDate(latestPayment != null ? latestPayment.getPaymentDate() : null)
                .billingDate(billing.getBillingDate() != null
                        ? billing.getBillingDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
                        : null)
                .copayAmount(billing.getCopayAmount())
                .discountType(billing.getDiscountType() != null ? billing.getDiscountType().getValue() : null)
                .discountValue(billing.getDiscountValue())
                .discountAmount(billing.getDiscountAmount())
                .amountDue(amountDue)
                .remainingDue(remainingDue)
                .creditAmount(creditAmount)
                .clientPaidAmount(billing.getClientPaidAmount())
                .insurancePaidAmount(billing.getInsurancePaidAmount())
                .invoicePolicyId(billing.getInvoicePolicyId())
                .stripeCheckoutSessionId(billing.getStripeCheckoutSessionId())
                .stripePaymentIntentId(billing.getStripePaymentIntentId())
                .createdAt(billing.getCreatedAt())
                .updatedAt(billing.getUpdatedAt())
                .build();
    }

    private Payment resolveLatestSuccessfulPayment(Long billingId) {
        if (billingId == null) {
            return null;
        }
        return paymentRepository.findBySessionBillingIdOrderByPaymentDateDescCreatedAtDesc(billingId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID || p.getStatus() == PaymentStatus.PARTIAL)
                .findFirst()
                .orElse(null);
    }

    private String derivePaymentStatus(SessionBilling billing, BigDecimal paidAmount, BigDecimal amountDue) {
        BillingStatus billingStatus = billing.getBillingStatus();
        if (billingStatus == BillingStatus.CANCELLED) {
            return "cancelled";
        }
        if (billingStatus == BillingStatus.DENIED) {
            return "denied";
        }
        BigDecimal due = amountDue != null ? amountDue : BigDecimal.ZERO;
        BigDecimal paid = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        // Fully discounted (amount due = 0) or paid in full → paid
        if (billingStatus == BillingStatus.PAID
                || due.compareTo(BigDecimal.ZERO) == 0
                || (due.compareTo(BigDecimal.ZERO) > 0 && paid.compareTo(due) >= 0)) {
            return "paid";
        }
        if (paid.compareTo(BigDecimal.ZERO) > 0 && paid.compareTo(due) < 0) {
            return "partial";
        }
        return "unpaid";
    }

    private BigDecimal resolveOriginalRatePerUnit(
            SessionBilling billing,
            com.smart.therapy.flow.billing.entity.Service service) {
        if (billing.getOriginalRatePerUnit() != null) {
            return billing.getOriginalRatePerUnit();
        }
        if (billing.getInvoicePolicyId() != null && service != null && service.getBaseRate() != null) {
            return service.getBaseRate().setScale(2, RoundingMode.HALF_UP);
        }
        return billing.getRatePerUnit();
    }

    private BigDecimal resolveOriginalSubtotal(
            SessionBilling billing,
            com.smart.therapy.flow.billing.entity.Service service) {
        if (billing.getOriginalSubtotalAmount() != null) {
            return billing.getOriginalSubtotalAmount();
        }
        BigDecimal rate = resolveOriginalRatePerUnit(billing, service);
        int units = billing.getUnits() != null ? billing.getUnits() : 1;
        if (rate != null) {
            return rate.multiply(BigDecimal.valueOf(units)).setScale(2, RoundingMode.HALF_UP);
        }
        return billing.getTotalAmount();
    }

    private BigDecimal currentCumulativePaid(SessionBilling billing, PaymentSource paymentSource) {
        if (paymentSource == PaymentSource.INSURANCE_PORTAL) {
            return billing.getInsurancePaidAmount() != null
                    ? billing.getInsurancePaidAmount().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return billing.getClientPaidAmount() != null
                ? billing.getClientPaidAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private SplitPaymentLegRequest withDeltaAmount(SplitPaymentLegRequest leg, BigDecimal delta) {
        SplitPaymentLegRequest copy = new SplitPaymentLegRequest();
        copy.setAmount(delta);
        if (leg != null) {
            copy.setPaymentMethod(leg.getPaymentMethod());
            copy.setPaymentDate(leg.getPaymentDate());
            copy.setReferenceNumber(leg.getReferenceNumber());
        }
        return copy;
    }

    private void recordAuditEvent(Long actorId, String action, Long resourceId, String ipAddress) {
        recordAuditEvent(actorId, action, resourceId, null, ipAddress, false);
    }

    private void recordAuditEvent(Long actorId, String action, Long resourceId, Long clientId, String ipAddress,
            boolean hipaaRelevant) {
        recordAuditEvent(actorId, action, resourceId, clientId, ipAddress, hipaaRelevant, null);
    }

    private void recordAuditEvent(Long actorId, String action, Long resourceId, Long clientId, String ipAddress,
            boolean hipaaRelevant, String details) {
        try {
            auditLogService.recordStaffEvent(actorId, action, RESOURCE_TYPE_BILLING, resourceId, clientId, ipAddress,
                    hipaaRelevant, details);
        } catch (Exception e) {
            log.error("Failed to record audit event for billing: {}", resourceId, e);
        }
    }

    private Long requireServiceId(com.smart.therapy.flow.billing.entity.Service service) {
        Objects.requireNonNull(service, "Service is required");
        return Objects.requireNonNull(service.getId(), "Service id must not be null");
    }

    private Long requireBillingId(SessionBilling billing) {
        Objects.requireNonNull(billing, "Billing is required");
        return Objects.requireNonNull(billing.getId(), "Billing id must not be null");
    }

    private void applyDiscountValues(SessionBilling billing, String discountTypeInput, BigDecimal discountValueInput,
            BigDecimal discountAmountInput) {
        if (billing.getTotalAmount() == null) {
            throw new IllegalStateException("Total amount must be set before applying discounts");
        }

        if (discountTypeInput == null || discountTypeInput.trim().isEmpty()
                || "no_discount".equalsIgnoreCase(discountTypeInput.trim())) {
            billing.setDiscountType(DiscountType.NONE);
            billing.setDiscountValue(null);
            billing.setDiscountAmount(null);
            return;
        }

        try {
            DiscountType discountType = DiscountType.fromValue(discountTypeInput);

            if (discountType == DiscountType.NONE) {
                billing.setDiscountType(DiscountType.NONE);
                billing.setDiscountValue(null);
                billing.setDiscountAmount(null);
                return;
            }

            BigDecimal total = billing.getTotalAmount();
            BigDecimal paid = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
            // Discount is always computed against current outstanding (what is still owed),
            // not the original service total. With one-time apply and no prior discount,
            // outstanding = total - paid.
            BigDecimal outstandingBefore = total.subtract(paid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            if (outstandingBefore.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Cannot apply a discount when there is no outstanding balance");
            }

            BigDecimal value = discountValueInput != null ? discountValueInput : BigDecimal.ZERO;
            BigDecimal amount = discountAmountInput != null ? discountAmountInput : BigDecimal.ZERO;

            switch (discountType) {
                case PERCENTAGE -> {
                    if (value.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BadRequestException("Discount percentage must be greater than zero");
                    }
                    int maxPercentage = appProperties != null && appProperties.getBilling() != null
                            ? appProperties.getBilling().getDiscount().getMaxPercentage()
                            : 100;
                    int roundingScale = appProperties != null && appProperties.getBilling() != null
                            ? appProperties.getBilling().getDiscount().getRoundingScale()
                            : 6;

                    if (value.compareTo(new BigDecimal(String.valueOf(maxPercentage))) > 0) {
                        throw new BadRequestException("Discount percentage cannot exceed " + maxPercentage);
                    }
                    BigDecimal percent = value.divide(new BigDecimal("100"), roundingScale, RoundingMode.HALF_UP);
                    BigDecimal computed = outstandingBefore.multiply(percent).setScale(2, RoundingMode.HALF_UP);
                    if (computed.compareTo(outstandingBefore) > 0) {
                        computed = outstandingBefore;
                    }
                    billing.setDiscountType(DiscountType.PERCENTAGE);
                    billing.setDiscountValue(value.setScale(2, RoundingMode.HALF_UP));
                    billing.setDiscountAmount(computed);
                }
                case FIXED_AMOUNT -> {
                    BigDecimal fixed = amount.compareTo(BigDecimal.ZERO) > 0 ? amount : value;
                    if (fixed == null || fixed.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BadRequestException("Discount amount must be greater than zero");
                    }
                    if (fixed.compareTo(outstandingBefore) > 0) {
                        fixed = outstandingBefore;
                    }
                    billing.setDiscountType(DiscountType.FIXED_AMOUNT);
                    billing.setDiscountValue(fixed.setScale(2, RoundingMode.HALF_UP));
                    billing.setDiscountAmount(fixed.setScale(2, RoundingMode.HALF_UP));
                }
                default -> {
                    billing.setDiscountType(DiscountType.NONE);
                    billing.setDiscountValue(null);
                    billing.setDiscountAmount(null);
                }
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid discount type: " + discountTypeInput);
        }
    }

    /**
     * When a discount brings outstanding to zero (e.g. 100% off), mark the invoice paid.
     * Clearing a discount restores pending/billed when money is still owed.
     */
    private void syncBillingStatusAfterDiscount(SessionBilling billing, BigDecimal outstanding) {
        if (outstanding.compareTo(BigDecimal.ZERO) == 0) {
            billing.setBillingStatus(BillingStatus.PAID);
            return;
        }
        if (billing.getBillingStatus() == BillingStatus.PAID) {
            BigDecimal paid = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
            billing.setBillingStatus(
                    paid.compareTo(BigDecimal.ZERO) > 0 ? BillingStatus.BILLED : BillingStatus.PENDING);
        }
    }

    private BigDecimal calculateAmountDue(SessionBilling billing) {
        BigDecimal amountDue = billing.getTotalAmount() != null ? billing.getTotalAmount() : BigDecimal.ZERO;
        if (billing.getDiscountAmount() != null) {
            amountDue = amountDue.subtract(billing.getDiscountAmount());
        }
        if (amountDue.compareTo(BigDecimal.ZERO) < 0) {
            amountDue = BigDecimal.ZERO;
        }
        return amountDue.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean hasRole(AuthPrincipal principal, String roleName) {
        return permissionChecker.hasRole(principal, roleName);
    }

    /**
     * PBAC caseload scope for billing lists (CLIENT_VIEW_*), not role names.
     */
    private enum BillingCaseloadScope {
        ALL, TEAM, OWN, TEAM_AND_OWN, NONE
    }

    private BillingCaseloadScope resolveBillingCaseloadScope(AuthPrincipal requester) {
        return switch (caseloadScopeService.resolve(requester).scope()) {
            case ALL -> BillingCaseloadScope.ALL;
            case TEAM -> BillingCaseloadScope.TEAM;
            case OWN -> BillingCaseloadScope.OWN;
            case TEAM_AND_OWN -> BillingCaseloadScope.TEAM_AND_OWN;
            case NONE -> BillingCaseloadScope.NONE;
        };
    }

    private boolean shouldScopeToAssignedTherapist(AuthPrincipal requester) {
        return resolveBillingCaseloadScope(requester) == BillingCaseloadScope.OWN;
    }

    private Long resolveEffectiveTherapistFilter(AuthPrincipal requester, Long requestedTherapistId) {
        BillingCaseloadScope scope = resolveBillingCaseloadScope(requester);
        if (scope == BillingCaseloadScope.NONE) {
            throw new ForbiddenException("You do not have permission to view billing records");
        }
        if (scope == BillingCaseloadScope.OWN) {
            // For therapists with OWN scope, don't force a therapist filter here.
            // The applyTherapistClientScope() method already applies the correct filtering:
            // - client.assigned_therapist_id = currentTherapistId OR
            // - session.therapist_id = currentTherapistId
            // If we return therapistId here, it would force ONLY session.therapist_id filter,
            // which would exclude sessions for assigned clients conducted by other therapists.
            if (requestedTherapistId != null) {
                Long currentTherapistId = currentUserService.requireCurrentUser(requester).getId();
                if (!requestedTherapistId.equals(currentTherapistId)) {
                    throw new ForbiddenException("You can only view your own billing records");
                }
            }
            return null;  // Don't apply therapist filter - let applyTherapistClientScope handle it
        }
        if (scope == BillingCaseloadScope.TEAM || scope == BillingCaseloadScope.TEAM_AND_OWN) {
            if (requestedTherapistId != null) {
                CaseloadScopeService.ResolvedCaseloadScope resolved = caseloadScopeService.resolve(requester);
                if (!resolved.includesTherapist(requestedTherapistId)) {
                    throw new ForbiddenException("You can only view billing for therapists you supervise");
                }
            }
        }
        return requestedTherapistId;
    }

    private Specification<SessionBilling> applyTherapistClientScope(AuthPrincipal requester, Specification<SessionBilling> spec) {
        BillingCaseloadScope scope = resolveBillingCaseloadScope(requester);
        log.debug("applyTherapistClientScope - scope={}", scope);

        if (scope == BillingCaseloadScope.NONE) {
            throw new ForbiddenException("You do not have permission to view billing records");
        }
        if (scope == BillingCaseloadScope.ALL) {
            log.debug("applyTherapistClientScope - ALL scope, no filtering");
            return spec;
        }
        if (scope == BillingCaseloadScope.OWN) {
            Long therapistId = currentUserService.requireCurrentUser(requester).getId();
            log.info("applyTherapistClientScope - OWN scope, filtering by therapistId={} (session.therapist_id = {})",
                    therapistId, therapistId);
            // Therapist can only see billing for sessions they actually conducted
            // This prevents seeing old sessions when client reassignment happens
            Specification<SessionBilling> therapistSessions = (root, query, cb) ->
                    cb.equal(root.get("session").get("therapist").get("id"), therapistId);
            return spec.and(therapistSessions);
        }
        // TEAM or TEAM_AND_OWN
        CaseloadScopeService.ResolvedCaseloadScope resolved = caseloadScopeService.resolve(requester);
        List<Long> therapistIds = new ArrayList<>(resolved.supervisedTherapistIds());
        if (scope == BillingCaseloadScope.TEAM_AND_OWN && resolved.currentUserId() != null) {
            therapistIds.add(resolved.currentUserId());
        }
        log.debug("applyTherapistClientScope - {} scope, therapistIds={}", scope, therapistIds);
        if (therapistIds.isEmpty()) {
            log.warn("applyTherapistClientScope - {} scope with no therapistIds, returning empty", scope);
            Specification<SessionBilling> none = (root, query, cb) -> cb.disjunction();
            return spec.and(none);
        }
        Specification<SessionBilling> teamClients = (root, query, cb) ->
                root.get("session").get("client").get("assignedTherapist").get("id").in(therapistIds);
        return spec.and(teamClients);
    }

    private void validateTherapistClientBillingAccess(
            com.smart.therapy.flow.client.entity.Client client,
            AuthPrincipal requester) {
        clientReportAccessService.validateClientAccess(client, requester);
    }

    private List<Long> getSupervisedTherapistIds(Long supervisorId) {
        return caseloadScopeService.getSupervisedTherapistIds(supervisorId);
    }

    private java.util.Map<String, Object> buildBillingEventData(SessionBilling billing) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("billingId", billing.getId());
        data.put("sessionId", billing.getSession() != null ? billing.getSession().getId() : null);
        data.put("clientId", billing.getSession() != null && billing.getSession().getClient() != null
                ? billing.getSession().getClient().getId()
                : null);
        if (billing.getSession() != null && billing.getSession().getClient() != null) {
            NotificationPayloadFactory.putClientIdentity(data, billing.getSession().getClient());
        } else {
            data.put("clientName", null);
            data.put("clientMrn", null);
        }
        data.put("therapistId", billing.getSession() != null && billing.getSession().getTherapist() != null
                ? billing.getSession().getTherapist().getId()
                : null);
        data.put("therapistName", billing.getSession() != null && billing.getSession().getTherapist() != null
                ? billing.getSession().getTherapist().getFullName()
                : null);
        data.put("serviceCode", billing.getServiceCode());
        String serviceName = billing.getServiceCode();
        String sessionDateFormatted = null;
        if (billing.getSession() != null) {
            if (billing.getSession().getService() != null
                    && org.springframework.util.StringUtils.hasText(billing.getSession().getService().getServiceName())) {
                serviceName = billing.getSession().getService().getServiceName();
            }
            if (billing.getSession().getSessionDate() != null) {
                java.time.format.DateTimeFormatter sessionFmt = java.time.format.DateTimeFormatter
                        .ofPattern("MMM d, yyyy h:mm a z")
                        .withZone(java.time.ZoneId.of("UTC"));
                sessionDateFormatted = sessionFmt.format(billing.getSession().getSessionDate());
                data.put("sessionDateFormatted", sessionDateFormatted);
            }
        }
        data.put("serviceName", serviceName);
        data.put("totalAmount", billing.getTotalAmount());
        data.put("amountDue", calculateAmountDue(billing));
        data.put("paidAmount", billing.getPaidAmount());
        data.put("dueDate", billing.getDueDate());
        
        if (billing.getDueDate() != null) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")
                    .withZone(java.time.ZoneId.of("UTC")); // Default to UTC
            data.put("dueDateFormatted", formatter.format(billing.getDueDate()));
        }

        String invoiceNumber = "INV-%s-%d".formatted(
                billing.getSession() != null && billing.getSession().getClient() != null
                        && billing.getSession().getClient().getClientId() != null
                        ? billing.getSession().getClient().getClientId()
                        : "CL",
                billing.getId());
        data.put("invoiceNumber", invoiceNumber);
        String frontendBase = EmailAppLinks.DEFAULT_FRONTEND_BASE;
        data.put("paymentUrl", EmailAppLinks.absolute(frontendBase, EmailAppLinks.CLIENT_INVOICES));
        data.put("invoiceUrl", EmailAppLinks.absolute(frontendBase, EmailAppLinks.CLIENT_INVOICES));
        data.put("entityType", "billing");
        data.put("entityId", billing.getId());
        return data;
    }

    private void assertAdmin(AuthPrincipal principal, String message) {
        if (principal == null || !permissionChecker.hasPermission(principal, "USER_MANAGE")) {
            throw new com.smart.therapy.flow.common.exception.ForbiddenException(message);
        }
    }

    // ========== NEW METHODS ==========

    @Transactional(readOnly = true)
    public BillingStatisticsResponse getBillingStatistics(AuthPrincipal requester) {
        return getBillingStatistics(requester, null, null);
    }

    @Transactional(readOnly = true)
    public BillingStatisticsResponse getBillingStatistics(
            AuthPrincipal requester, LocalDate startDate, LocalDate endDate) {
        return getBillingStatistics(requester, null, null, null, null, null,
                null, null, null, null, startDate, endDate, null, null);
    }

    @Transactional(readOnly = true)
    public BillingStatisticsResponse getBillingStatistics(
            AuthPrincipal requester, Long clientId, String clientSearch, Long therapistId,
            String status, String paymentStatus, String serviceCode, String clientType,
            String sessionType, String paymentMethod, LocalDate startDate, LocalDate endDate,
            BigDecimal minAmount, BigDecimal maxAmount) {
        Objects.requireNonNull(requester, "Requester is required");
        BillingCaseloadScope scope = resolveBillingCaseloadScope(requester);
        Long effectiveTherapistId = resolveEffectiveTherapistFilter(requester, therapistId);
        BillingRecordStatusFilter statusFilter = null;
        if (StringUtils.hasText(status)) {
            try { statusFilter = BillingRecordStatusFilter.fromValue(status.trim()); }
            catch (IllegalArgumentException ex) { throw new BadRequestException("Invalid status filter"); }
        }
        boolean unfiltered = clientId == null && !StringUtils.hasText(clientSearch) && therapistId == null
                && statusFilter == null && !StringUtils.hasText(paymentStatus) && !StringUtils.hasText(serviceCode)
                && !StringUtils.hasText(clientType) && !StringUtils.hasText(sessionType)
                && !StringUtils.hasText(paymentMethod) && startDate == null && endDate == null
                && minAmount == null && maxAmount == null;
        if (scope == BillingCaseloadScope.ALL && unfiltered) {
            return buildBillingStatisticsFromAggregates(null, null);
        }
        Specification<SessionBilling> spec = buildBillingSpecification(
                clientId, clientSearch, effectiveTherapistId, statusFilter, serviceCode, clientType,
                sessionType, paymentMethod, startDate, endDate, minAmount, maxAmount, paymentStatus);
        spec = applyTherapistClientScope(requester, spec);
        return buildBillingStatistics(sessionBillingRepository.findAll(spec));
    }

    private BillingStatisticsResponse buildBillingStatisticsFromAggregates(LocalDate startDate, LocalDate endDate) {
        // PostgreSQL cannot type-bind ":date IS NULL OR ..." params — always use BETWEEN.
        LocalDate from = startDate != null ? startDate : LocalDate.of(1900, 1, 1);
        LocalDate to = endDate != null ? endDate : LocalDate.of(9999, 12, 31);

        BigDecimal outstandingBalance = nvl(sessionBillingRepository
                .sumPositiveOutstandingAmountForDateRange(from, to));
        BigDecimal creditBalance = nvl(sessionBillingRepository
                .sumCreditBalanceForDateRange(from, to));
        BigDecimal totalCollected = nvl(sessionBillingRepository
                .sumPaidAmountForDateRange(from, to));
        long activeClients = sessionBillingRepository.countDistinctClientsForDateRange(from, to);
        long totalBillingRecords = sessionBillingRepository.countForDateRange(from, to);
        long pendingRecords = sessionBillingRepository.countByBillingStatusInForDateRange(
                List.of(BillingStatus.PENDING, BillingStatus.BILLED, BillingStatus.FOLLOW_UP),
                from,
                to);
        long paidRecords = sessionBillingRepository.countByBillingStatusInForDateRange(
                List.of(BillingStatus.PAID),
                from,
                to);
        long partialRecords = sessionBillingRepository.countPartiallyPaidForDateRange(
                from, to, BillingStatus.PAID);
        long deniedRecords = sessionBillingRepository.countByBillingStatusInForDateRange(
                List.of(BillingStatus.DENIED),
                from,
                to);
        long followUpRecords = sessionBillingRepository.countByBillingStatusInForDateRange(
                List.of(BillingStatus.FOLLOW_UP),
                from,
                to);

        return BillingStatisticsResponse.builder()
                .outstandingBalance(outstandingBalance.setScale(2, RoundingMode.HALF_UP))
                .creditBalance(creditBalance.setScale(2, RoundingMode.HALF_UP))
                .totalCollected(totalCollected.setScale(2, RoundingMode.HALF_UP))
                .activeClients(activeClients)
                .totalBillingRecords(totalBillingRecords)
                .pendingRecords(pendingRecords)
                .paidRecords(paidRecords)
                .partialRecords(partialRecords)
                .deniedRecords(deniedRecords)
                .followUpRecords(followUpRecords)
                .build();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BillingStatisticsResponse buildBillingStatistics(List<SessionBilling> allBilling) {
        List<SessionBilling> safeBilling = allBilling != null ? allBilling : List.of();

        BigDecimal outstandingBalance = safeBilling.stream()
                .map(this::calculateRemainingDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal creditBalance = safeBilling.stream()
                .map(this::calculateCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCollected = safeBilling.stream()
                // .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                .map(b -> b.getPaidAmount() != null ? b.getPaidAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long activeClients = safeBilling.stream()
                .map(b -> b.getSession() != null ? b.getSession().getClient() : null)
                .filter(Objects::nonNull)
                .map(com.smart.therapy.flow.client.entity.Client::getId)
                .distinct()
                .count();

        Long totalBillingRecords = (long) safeBilling.size();
        Long pendingRecords = safeBilling.stream()
                .filter(b -> b.getBillingStatus() == BillingStatus.PENDING
                        || b.getBillingStatus() == BillingStatus.BILLED
                        || b.getBillingStatus() == BillingStatus.FOLLOW_UP)
                .count();
        Long paidRecords = safeBilling.stream()
                .filter(b -> b.getBillingStatus() == BillingStatus.PAID)
                .count();
        Long partialRecords = safeBilling.stream()
                .filter(this::isPartiallyPaidRecord)
                .count();
        Long deniedRecords = safeBilling.stream()
                .filter(b -> b.getBillingStatus() == BillingStatus.DENIED)
                .count();
        Long followUpRecords = safeBilling.stream()
                .filter(b -> b.getBillingStatus() == BillingStatus.FOLLOW_UP)
                .count();

        return BillingStatisticsResponse.builder()
                .outstandingBalance(outstandingBalance.setScale(2, RoundingMode.HALF_UP))
                .creditBalance(creditBalance.setScale(2, RoundingMode.HALF_UP))
                .totalCollected(totalCollected.setScale(2, RoundingMode.HALF_UP))
                .activeClients(activeClients)
                .totalBillingRecords(totalBillingRecords)
                .pendingRecords(pendingRecords)
                .paidRecords(paidRecords)
                .partialRecords(partialRecords)
                .deniedRecords(deniedRecords)
                .followUpRecords(followUpRecords)
                .build();
    }

    /** Payment received but invoice not fully paid — these amounts are included in totalCollected. */
    private boolean isPartiallyPaidRecord(SessionBilling billing) {
        if (billing == null || billing.getBillingStatus() == BillingStatus.PAID) {
            return false;
        }
        BigDecimal paid = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
        return paid.compareTo(BigDecimal.ZERO) > 0;
    }

    @Transactional(readOnly = true)
    public ClientBillingStatsResponse getClientBillingStats(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        com.smart.therapy.flow.client.entity.Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        validateTherapistClientBillingAccess(client, requester);

        List<SessionBilling> records = sessionBillingRepository.findByClientId(clientId);
        if (shouldScopeToAssignedTherapist(requester)) {
            Long therapistId = currentUserService.requireCurrentUser(requester).getId();
            records = records.stream()
                    .filter(record -> record.getSession() != null
                            && record.getSession().getTherapist() != null
                            && therapistId.equals(record.getSession().getTherapist().getId()))
                    .toList();
        }
        BigDecimal totalBilledAmount = BigDecimal.ZERO;
        BigDecimal totalDiscountAmount = BigDecimal.ZERO;
        BigDecimal totalPaidAmount = BigDecimal.ZERO;
        BigDecimal dueAmount = BigDecimal.ZERO;
        BigDecimal creditAmount = BigDecimal.ZERO;

        long pending = 0L;
        long billed = 0L;
        long paid = 0L;
        long denied = 0L;
        long followUp = 0L;
        long cancelled = 0L;

        for (SessionBilling billing : records) {
            BigDecimal total = billing.getTotalAmount() != null ? billing.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal discount = billing.getDiscountAmount() != null ? billing.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal paidValue = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal amountDue = calculateAmountDue(billing);
            BigDecimal remainingDue = amountDue.subtract(paidValue).max(BigDecimal.ZERO);
            BigDecimal overpaidCredit = paidValue.subtract(amountDue).max(BigDecimal.ZERO);

            totalBilledAmount = totalBilledAmount.add(total);
            totalDiscountAmount = totalDiscountAmount.add(discount.max(BigDecimal.ZERO));
            totalPaidAmount = totalPaidAmount.add(paidValue);
            dueAmount = dueAmount.add(remainingDue);
            creditAmount = creditAmount.add(overpaidCredit);

            BillingStatus status = billing.getBillingStatus();
            if (status == null) {
                pending++;
            } else {
                switch (status) {
                    case PENDING -> pending++;
                    case BILLED -> billed++;
                    case PAID -> paid++;
                    case DENIED -> denied++;
                    case FOLLOW_UP -> followUp++;
                    case CANCELLED -> cancelled++;
                }
            }
        }

        return ClientBillingStatsResponse.builder()
                .clientId(clientId)
                .totalInvoices((long) records.size())
                .pendingInvoices(pending)
                .billedInvoices(billed)
                .paidInvoices(paid)
                .deniedInvoices(denied)
                .followUpInvoices(followUp)
                .cancelledInvoices(cancelled)
                .totalBilledAmount(totalBilledAmount.setScale(2, RoundingMode.HALF_UP))
                .totalDiscountAmount(totalDiscountAmount.setScale(2, RoundingMode.HALF_UP))
                .totalPaidAmount(totalPaidAmount.setScale(2, RoundingMode.HALF_UP))
                .dueAmount(dueAmount.setScale(2, RoundingMode.HALF_UP))
                .creditAmount(creditAmount.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<BillingHistoryResponse> getBillingHistory(
            Long clientId,
            Long therapistId,
            String paymentStatus,
            String billingStatus,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int limit,
            AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");
        Long effectiveTherapistId = resolveEffectiveTherapistFilter(requester, therapistId);

        BillingRecordStatusFilter historyStatus = null;
        String preferredStatus = billingStatus != null && !billingStatus.isBlank() ? billingStatus : paymentStatus;
        if (preferredStatus != null && !preferredStatus.isBlank()) {
            try {
                historyStatus = BillingRecordStatusFilter.fromValue(preferredStatus.trim());
            } catch (IllegalArgumentException ignored) {
                // Keep history endpoint tolerant; unknown status simply means no status filter.
            }
        }

        Specification<SessionBilling> spec = buildBillingSpecification(
                clientId, null, effectiveTherapistId, historyStatus, null, null, null, null,
                startDate, endDate, null, null);
        spec = applyTherapistClientScope(requester, spec);

        int defaultPage = appProperties != null ? appProperties.getPagination().getDefaultPage() : 1;
        int defaultPageSize = appProperties != null ? appProperties.getPagination().getDefaultPageSize() : 20;
        int maxPageSize = appProperties != null ? appProperties.getPagination().getMaxPageSize() : 200;
        int minPageSize = appProperties != null ? appProperties.getPagination().getMinPageSize() : 1;

        int safePage = Math.max(defaultPage, page);
        int safeLimit = Math.min(Math.max(limit > 0 ? limit : defaultPageSize, minPageSize), maxPageSize);

        // Match ClientHubAI: session-date filter + billing_date DESC, billing id DESC.
        List<SessionBilling> billingRecords = new ArrayList<>(sessionBillingRepository.findAll(spec));
        sortLikeClientHub(billingRecords, Sort.Direction.DESC);

        long total = billingRecords.size();
        int fromIndex = Math.min((safePage - 1) * safeLimit, billingRecords.size());
        int toIndex = Math.min(fromIndex + safeLimit, billingRecords.size());

        List<BillingHistoryResponse> items = billingRecords.subList(fromIndex, toIndex).stream()
                .map(this::toBillingHistoryResponse)
                .collect(Collectors.toList());

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                "billing_history_viewed", null, clientId, null, true,
                "resultCount=" + items.size() + ",total=" + total + ",page=" + safePage + ",pageSize=" + safeLimit);

        return PaginatedResponse.of(items, total, safePage, safeLimit);
    }

    @Transactional
    public SessionBillingResponse recordPayment(
            Long billingId,
            RecordPaymentRequest request,
            AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(billingId, "Billing id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");
        billingGuard.requireTenantContext();

        SessionBilling billing = sessionBillingRepository.findByIdForUpdate(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Session billing not found"));
        billingGuard.assertStaffBillingAccess(billing, requester);
        if (request.getClientId() != null) {
            billingGuard.assertClientOwnsInvoice(billing, request.getClientId());
        }

        PaymentMethod paymentMethod = request.getPaymentMethod();
        if (paymentMethod == null) {
            throw new BadRequestException("Payment method is required");
        }
        PaymentSource paymentSource = resolvePaymentSource(request.getPaymentSide(), paymentMethod);

        BigDecimal cumulativeAmount = request.getPaymentAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal previousCumulative = currentCumulativePaid(billing, paymentSource);

        if (request.getExpectedPreviousForSource() != null) {
            BigDecimal expected = request.getExpectedPreviousForSource().setScale(2, RoundingMode.HALF_UP);
            if (expected.subtract(previousCumulative).abs().compareTo(new BigDecimal("0.005")) > 0) {
                throw new ConflictException(
                        "Payment totals changed. Reload latest totals and try again.",
                        Map.of(
                                "reason", "STALE_PAYMENT_STATE",
                                "expectedPreviousForSource", expected,
                                "actualPreviousForSource", previousCumulative));
            }
        }

        BigDecimal delta = cumulativeAmount.subtract(previousCumulative).setScale(2, RoundingMode.HALF_UP);

        billingGuard.rejectNegativePaymentDelta(delta);

        enforceZeroBillGuard(
                billing,
                delta,
                Boolean.TRUE.equals(request.getAllowZeroBillOverpayment()),
                request.getOverrideReason(),
                requester,
                ipAddress);

        BigDecimal amountDue = calculateAmountDue(billing);
        boolean zeroBillOverrideApproved = amountDue.compareTo(BigDecimal.ZERO) == 0
                && Boolean.TRUE.equals(request.getAllowZeroBillOverpayment());
        if (!zeroBillOverrideApproved) {
            billingGuard.rejectPaymentExceedingOutstanding(delta, calculateRemainingDue(billing));
        }

        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            recalculateBillingAmountsAndStatus(billing);
            SessionBilling unchanged = sessionBillingRepository.save(billing);
            return toSessionBillingResponse(unchanged);
        }

        com.smart.therapy.flow.billing.entity.Payment payment = com.smart.therapy.flow.billing.entity.Payment.builder()
                .sessionBilling(billing)
                .amount(delta)
                .paymentMethod(paymentMethod)
                .paymentSource(paymentSource)
                .status(PaymentStatus.fromValue("paid"))
                .paymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : Instant.now())
                .reference(request.getReferenceNumber())
                .notes(request.getNotes())
                .build();

        com.smart.therapy.flow.billing.entity.Payment savedPayment = paymentRepository.save(payment);

        com.smart.therapy.flow.billing.entity.PaymentTransaction paymentTxn = com.smart.therapy.flow.billing.entity.PaymentTransaction
                .builder()
                .payment(savedPayment)
                .provider(paymentSource == PaymentSource.INSURANCE_PORTAL ? PaymentSource.INSURANCE_PORTAL : PaymentSource.MANUAL)
                .transactionType(com.smart.therapy.flow.billing.enums.TransactionType.CHARGE)
                .amount(delta)
                .status(PaymentStatus.PAID)
                .failureReason(null)
                .providerIntentId(request.getReferenceNumber())
                .build();
        paymentTransactionRepository.save(paymentTxn);

        recalculateBillingAmountsAndStatus(billing);

        SessionBilling updated = sessionBillingRepository.save(billing);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), BillingAuditActions.PAYMENT_RECORDED, billingId,
                ipAddress);

        return toSessionBillingResponse(updated);
    }

    @Transactional
    public void applyStripePortalPayment(
            Long billingId,
            Long clientId,
            Long organisationId,
            BigDecimal paymentAmount,
            String paymentIntentId,
            String checkoutSessionId,
            String connectedAccountId,
            String metadataConnectedAccountId) {
        Objects.requireNonNull(billingId, "Billing id is required");
        Objects.requireNonNull(clientId, "Client id is required");

        SessionBilling billing = sessionBillingRepository.findByIdForUpdate(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Session billing not found"));

        Long sessionId = sessionBillingRepository.findSessionIdByBillingId(billingId)
                .orElseThrow(() -> new BadRequestException("Billing record is missing session context"));
        Long sessionClientId = sessionRepository.findClientIdBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!Objects.equals(sessionClientId, clientId)) {
            throw new ForbiddenException("Invoice does not belong to the specified client");
        }

        billingGuard.assertTenantOrganisationMatches(organisationId);
        billingGuard.assertConnectedAccountMatches(connectedAccountId, metadataConnectedAccountId);

        if (billing.getBillingStatus() == BillingStatus.PAID) {
            log.warn("Stripe webhook ignored because billing {} is already paid", billingId);
            return;
        }
        if (StringUtils.hasText(paymentIntentId)
                && paymentTransactionRepository.existsByProviderIntentId(paymentIntentId)) {
            log.warn("Stripe webhook ignored duplicate payment intent {}", paymentIntentId);
            return;
        }

        BigDecimal delta = paymentAmount != null ? paymentAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        if (delta.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Stripe payment amount must be greater than zero");
        }

        com.smart.therapy.flow.billing.entity.Payment payment = com.smart.therapy.flow.billing.entity.Payment.builder()
                .sessionBilling(billing)
                .amount(delta)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .paymentSource(PaymentSource.STRIPE)
                .status(PaymentStatus.PAID)
                .paymentDate(Instant.now())
                .reference(paymentIntentId)
                .notes("Stripe portal payment")
                .build();
        com.smart.therapy.flow.billing.entity.Payment savedPayment = paymentRepository.save(payment);

        paymentTransactionRepository.save(
                com.smart.therapy.flow.billing.entity.PaymentTransaction.builder()
                        .payment(savedPayment)
                        .provider(PaymentSource.STRIPE)
                        .transactionType(com.smart.therapy.flow.billing.enums.TransactionType.CHARGE)
                        .amount(delta)
                        .providerIntentId(paymentIntentId)
                        .connectedAccountId(connectedAccountId)
                        .status(PaymentStatus.PAID)
                        .build());

        billing.setStripeCheckoutSessionId(checkoutSessionId);
        billing.setStripePaymentIntentId(paymentIntentId);
        recalculateBillingAmountsAndStatus(billing);
        sessionBillingRepository.save(billing);

        log.info("Applied Stripe portal payment billingId={} clientId={} organisationId={} amount={}",
                billingId, clientId, organisationId, delta);
    }

    @Transactional
    public void attachStripeCheckoutSession(Long billingId, Long clientId, String checkoutSessionId) {
        SessionBilling billing = sessionBillingRepository.findById(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Session billing not found"));
        if (billing.getSession() == null || billing.getSession().getClient() == null
                || !Objects.equals(billing.getSession().getClient().getId(), clientId)) {
            throw new BadRequestException("Invoice access denied");
        }
        billing.setStripeCheckoutSessionId(checkoutSessionId);
        sessionBillingRepository.save(billing);
    }

    public BigDecimal resolveOutstandingAmount(SessionBilling billing) {
        recalculateBillingAmountsAndStatus(billing);
        if (billing.getOutstandingAmount() != null) {
            return billing.getOutstandingAmount().max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal amountDue = calculateAmountDue(billing);
        BigDecimal paid = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
        return amountDue.subtract(paid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public SessionBillingResponse recordSplitPayment(
            Long billingId,
            RecordSplitPaymentRequest request,
            AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(billingId, "Billing id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");
        billingGuard.requireTenantContext();

        SessionBilling billing = sessionBillingRepository.findByIdForUpdate(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Session billing not found"));
        billingGuard.assertStaffBillingAccess(billing, requester);

        BigDecimal clientCumulative = request.getClientLeg() != null && request.getClientLeg().getAmount() != null
                ? request.getClientLeg().getAmount()
                : null;
        BigDecimal insuranceCumulative = request.getInsuranceLeg() != null && request.getInsuranceLeg().getAmount() != null
                ? request.getInsuranceLeg().getAmount()
                : null;

        BigDecimal clientDelta = clientCumulative != null
                ? clientCumulative.subtract(currentCumulativePaid(billing, PaymentSource.MANUAL)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal insuranceDelta = insuranceCumulative != null
                ? insuranceCumulative.subtract(currentCumulativePaid(billing, PaymentSource.INSURANCE_PORTAL)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal totalDelta = clientDelta.add(insuranceDelta);

        if (totalDelta.compareTo(BigDecimal.ZERO) <= 0) {
            if (clientDelta.compareTo(BigDecimal.ZERO) == 0 && insuranceDelta.compareTo(BigDecimal.ZERO) == 0) {
                recalculateBillingAmountsAndStatus(billing);
                return toSessionBillingResponse(sessionBillingRepository.save(billing));
            }
            billingGuard.rejectNegativePaymentDelta(clientDelta);
            billingGuard.rejectNegativePaymentDelta(insuranceDelta);
            throw new BadRequestException("Invalid payment amount: cumulative split payment cannot reduce recorded totals");
        }

        enforceZeroBillGuard(
                billing,
                totalDelta,
                Boolean.TRUE.equals(request.getAllowZeroBillOverpayment()),
                request.getOverrideReason(),
                requester,
                ipAddress);

        BigDecimal amountDue = calculateAmountDue(billing);
        boolean zeroBillOverrideApproved = amountDue.compareTo(BigDecimal.ZERO) == 0
                && Boolean.TRUE.equals(request.getAllowZeroBillOverpayment());
        if (!zeroBillOverrideApproved) {
            billingGuard.rejectPaymentExceedingOutstanding(totalDelta, calculateRemainingDue(billing));
        }

        if (clientDelta.compareTo(BigDecimal.ZERO) > 0) {
            createManualPaymentLeg(billing, withDeltaAmount(request.getClientLeg(), clientDelta), request.getNotes(), PaymentSource.MANUAL);
        }
        if (insuranceDelta.compareTo(BigDecimal.ZERO) > 0) {
            createManualPaymentLeg(billing, withDeltaAmount(request.getInsuranceLeg(), insuranceDelta), request.getNotes(), PaymentSource.INSURANCE_PORTAL);
        }

        recalculateBillingAmountsAndStatus(billing);
        SessionBilling updated = sessionBillingRepository.save(billing);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), BillingAuditActions.PAYMENT_RECORDED, billingId, ipAddress);
        return toSessionBillingResponse(updated);
    }

    @Transactional(readOnly = true)
    public PaymentGuidanceResponse getPaymentGuidance(Long billingId) {
        return getPaymentGuidance(billingId, null);
    }

    @Transactional(readOnly = true)
    public PaymentGuidanceResponse getPaymentGuidance(Long billingId, AuthPrincipal requester) {
        Objects.requireNonNull(billingId, "Billing id is required");
        SessionBilling billing = sessionBillingRepository.findById(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Session billing not found"));
        if (requester != null) {
            billingGuard.assertStaffBillingAccess(billing, requester);
        }

        BigDecimal amountAfterDiscount = calculateAmountDue(billing);
        BigDecimal amountAfterPolicy = billing.getTotalAmount() != null
                ? billing.getTotalAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal originalSubtotal = resolveOriginalSubtotal(
                billing,
                billing.getSession() != null ? billing.getSession().getService() : null);

        BigDecimal expectedClientPortion;
        BigDecimal expectedInsurancePortion;

        if (Boolean.TRUE.equals(billing.getInsuranceCovered()) && billing.getCopayAmount() != null) {
            expectedClientPortion = billing.getCopayAmount().max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            expectedInsurancePortion = amountAfterDiscount.subtract(expectedClientPortion)
                    .max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            expectedClientPortion = amountAfterDiscount.setScale(2, RoundingMode.HALF_UP);
            expectedInsurancePortion = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal clientAlreadyPaid = billing.getClientPaidAmount() != null
                ? billing.getClientPaidAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal insuranceAlreadyPaid = billing.getInsurancePaidAmount() != null
                ? billing.getInsurancePaidAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAlreadyPaid = clientAlreadyPaid.add(insuranceAlreadyPaid).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalRemainingDue = amountAfterDiscount.subtract(totalAlreadyPaid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        // Side remainings are advisory splits; never exceed the real outstanding balance.
        BigDecimal clientRemaining = expectedClientPortion.subtract(clientAlreadyPaid).max(BigDecimal.ZERO)
                .min(totalRemainingDue)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal insuranceRemaining = expectedInsurancePortion.subtract(insuranceAlreadyPaid).max(BigDecimal.ZERO)
                .min(totalRemainingDue)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal overpayDelta = totalAlreadyPaid.subtract(amountAfterDiscount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        return PaymentGuidanceResponse.builder()
                .billingId(billing.getId())
                .originalSubtotalAmount(originalSubtotal)
                .amountAfterPolicy(amountAfterPolicy)
                .amountAfterDiscount(amountAfterDiscount.setScale(2, RoundingMode.HALF_UP))
                .expectedClientPortion(expectedClientPortion)
                .expectedInsurancePortion(expectedInsurancePortion)
                .clientAlreadyPaid(clientAlreadyPaid)
                .insuranceAlreadyPaid(insuranceAlreadyPaid)
                .clientRemaining(clientRemaining)
                .insuranceRemaining(insuranceRemaining)
                .totalAlreadyPaid(totalAlreadyPaid)
                .totalRemainingDue(totalRemainingDue)
                .overpayDelta(overpayDelta)
                .insuranceCovered(Boolean.TRUE.equals(billing.getInsuranceCovered()))
                .build();
    }

    @Transactional
    public SessionBillingResponse editPayment(
            Long billingId,
            Long paymentId,
            EditPaymentRequest request,
            AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(billingId, "Billing id is required");
        Objects.requireNonNull(paymentId, "Payment id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        SessionBilling billing = sessionBillingRepository.findByIdForUpdate(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Session billing not found"));
        billingGuard.assertStaffBillingAccess(billing, requester);
        com.smart.therapy.flow.billing.entity.Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (payment.getSessionBilling() == null || !Objects.equals(payment.getSessionBilling().getId(), billingId)) {
            throw new BadRequestException("Payment does not belong to the provided billing record");
        }

        if (request.getAmount() != null || request.getPaymentMethod() != null) {
            if (payment.getPaymentSource() == PaymentSource.STRIPE
                    || payment.getPaymentSource() == PaymentSource.CREDIT_BALANCE_TRANSFER) {
                throw new BadRequestException("Provider payments and credit transfers cannot be edited manually");
            }
            var entries = paymentTransactionRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId).stream()
                    .filter(t -> !Boolean.TRUE.equals(t.getIsVoided())).toList();
            if (entries.size() != 1 || entries.get(0).getTransactionType() != TransactionType.CHARGE
                    || (entries.get(0).getStatus() != PaymentStatus.PAID && entries.get(0).getStatus() != PaymentStatus.PARTIAL)) {
                throw new BadRequestException("Payment cannot be edited without one active charge transaction; use a refund or adjustment");
            }
            if (request.getAmount() != null) {
                BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
                if (amount.signum() <= 0) throw new BadRequestException("Payment amount must be greater than zero");
                BigDecimal increase = amount.subtract(entries.get(0).getAmount());
                if (increase.compareTo(calculateRemainingDue(billing)) > 0) {
                    throw new BadRequestException("Payment increase exceeds the outstanding balance");
                }
                entries.get(0).setAmount(amount);
                paymentTransactionRepository.save(entries.get(0));
            }
        }

        if (request.getAmount() != null) {
            payment.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getPaymentMethod() != null) {
            payment.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getPaymentDate() != null) {
            payment.setPaymentDate(request.getPaymentDate());
        }
        if (request.getReference() != null) {
            payment.setReference(request.getReference());
        }
        if (request.getNotes() != null) {
            payment.setNotes(request.getNotes());
        }
        paymentRepository.save(payment);
        recalculateBillingAmountsAndStatus(billing);
        SessionBilling updated = sessionBillingRepository.save(billing);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "payment_edited", billingId, ipAddress);
        return toSessionBillingResponse(updated);
    }

    @Transactional
    public SessionBillingResponse refundPayment(
            Long billingId,
            RefundPaymentRequest request,
            AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(billingId, "Billing id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        SessionBilling billing = sessionBillingRepository.findByIdForUpdate(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Session billing not found"));
        billingGuard.assertStaffBillingAccess(billing, requester);

        BigDecimal refundAmount = request.getRefundAmount();
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Refund amount must be greater than zero");
        }

        BigDecimal currentlyPaid = calculateTotalPaidAmount(billingId);
        if (currentlyPaid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("No paid balance available to refund");
        }
        if (refundAmount.compareTo(currentlyPaid) > 0) {
            throw new BadRequestException("Refund amount cannot exceed currently paid balance: " + currentlyPaid);
        }

        com.smart.therapy.flow.billing.entity.Payment refundEntry = com.smart.therapy.flow.billing.entity.Payment.builder()
                .sessionBilling(billing)
                .amount(refundAmount.negate().setScale(2, RoundingMode.HALF_UP))
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.BANK_TRANSFER)
                .paymentSource(PaymentSource.MANUAL)
                .status(PaymentStatus.REFUNDED)
                .paymentDate(Instant.now())
                .reference(request.getReferenceNumber())
                .notes(request.getNotes())
                .build();
        com.smart.therapy.flow.billing.entity.Payment savedRefund = paymentRepository.save(refundEntry);
        com.smart.therapy.flow.billing.entity.PaymentTransaction refundTxn = com.smart.therapy.flow.billing.entity.PaymentTransaction
                .builder()
                .payment(savedRefund)
                .provider(PaymentSource.MANUAL)
                .transactionType(com.smart.therapy.flow.billing.enums.TransactionType.REFUND)
                .amount(refundAmount.negate().setScale(2, RoundingMode.HALF_UP))
                .status(PaymentStatus.REFUNDED)
                .failureReason(null)
                .providerIntentId(request.getReferenceNumber())
                .build();
        paymentTransactionRepository.save(refundTxn);

        recalculateBillingAmountsAndStatus(billing);
        SessionBilling updated = sessionBillingRepository.save(billing);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "payment_refunded", billingId, ipAddress);

        return toSessionBillingResponse(updated);
    }

    @Transactional
    public SessionBillingResponse changeBillingStatus(
            Long billingId,
            ChangeBillingStatusRequest request,
            AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(billingId, "Billing id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        SessionBilling billing = sessionBillingRepository.findByIdForUpdate(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Session billing not found"));
        billingGuard.assertStaffBillingAccess(billing, requester);

        try {
            BillingStatus newStatus = BillingStatus.fromValue(request.getBillingStatus());
            if (newStatus == BillingStatus.PAID || newStatus == BillingStatus.PARTIAL || newStatus == BillingStatus.PENDING) {
                requireLedgerStatus(billing, newStatus == BillingStatus.PAID ? PaymentStatus.PAID
                        : newStatus == BillingStatus.PARTIAL ? PaymentStatus.PARTIAL : PaymentStatus.PENDING);
                recalculateBillingAmountsAndStatus(billing);
            }
            billing.setBillingStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid billing status: " + request.getBillingStatus());
        }

        SessionBilling updated = sessionBillingRepository.save(billing);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), BillingAuditActions.BILLING_STATUS_CHANGED, billingId, ipAddress);

        return toSessionBillingResponse(updated);
    }

    private BillingHistoryResponse toBillingHistoryResponse(SessionBilling billing) {
        com.smart.therapy.flow.client.entity.Client client = billing.getSession() != null
                ? billing.getSession().getClient()
                : null;
        com.smart.therapy.flow.billing.entity.Service service = null;
        if (billing.getSession() != null && billing.getSession().getService() != null) {
            service = billing.getSession().getService();
        }

        BigDecimal amountDue = calculateAmountDue(billing);
        BigDecimal paidAmount = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal remainingDue = amountDue.subtract(paidAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        Payment latestPayment = resolveLatestSuccessfulPayment(billing.getId());

        return BillingHistoryResponse.builder()
                .billingId(billing.getId())
                .clientId(client != null ? client.getId() : null)
                .clientMrn(client != null ? client.getClientId() : null)
                .clientReferenceNumber(client != null && client.getReferral() != null
                        ? client.getReferral().getReferenceNumber() : null)
                .clientName(client != null ? client.getFullName() : null)
                .sessionId(billing.getSession() != null ? billing.getSession().getId() : null)
                .sessionDate(billing.getSession() != null ? billing.getSession().getSessionDate() : null)
                .sessionStatus(billing.getSession() != null ? billing.getSession().getStatus() : null)
                .serviceCode(billing.getServiceCode())
                .serviceName(service != null ? service.getServiceName() : null)
                .totalAmount(billing.getTotalAmount())
                .originalSubtotalAmount(resolveOriginalSubtotal(billing, service))
                .discountAmount(billing.getDiscountAmount())
                .amountDue(amountDue)
                .remainingDue(remainingDue)
                .paymentStatus(derivePaymentStatus(billing, paidAmount, amountDue))
                .billingStatus(billing.getBillingStatus() != null ? billing.getBillingStatus().getValue() : null)
                .paymentMethod(latestPayment != null && latestPayment.getPaymentMethod() != null
                        ? latestPayment.getPaymentMethod().getValue()
                        : null)
                .paymentAmount(paidAmount)
                .paymentDate(latestPayment != null ? latestPayment.getPaymentDate() : null)
                .invoicePolicyId(billing.getInvoicePolicyId())
                .billingDate(billing.getBillingDate() != null
                        ? billing.getBillingDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
                        : null)
                .createdAt(billing.getCreatedAt())
                .build();
    }

    @Transactional
    public void sendInvoiceEmail(Long billingId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(billingId, "Billing id is required");
        Objects.requireNonNull(requester, "Requester is required");

        if (emailService == null) {
            throw new BadRequestException("Email service is not configured");
        }

        SessionBilling billing = sessionBillingRepository.findById(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Session billing not found"));

        com.smart.therapy.flow.client.entity.Client client = billing.getSession() != null
                ? billing.getSession().getClient()
                : null;

        if (client == null) {
            throw new ResourceNotFoundException("Client not found for this billing record");
        }

        String clientEmail = getClientPrimaryEmailSafe(client);
        if (clientEmail == null || clientEmail.trim().isEmpty()) {
            throw new BadRequestException("Client email not found");
        }

        String invoiceHtml = generateInvoiceEmailHtml(billing, client);
        emailService.sendInvoiceEmail(clientEmail, client.getFullName(), invoiceHtml);

        // Update billing status to BILLED if still pending
        if (billing.getBillingStatus() == BillingStatus.PENDING) {
            billing.setBillingStatus(BillingStatus.BILLED);
            sessionBillingRepository.save(billing);
        }

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), BillingAuditActions.INVOICE_SENT, billingId, ipAddress);
    }

    @Transactional(readOnly = true)
    public String getInvoicePreview(Long billingId, AuthPrincipal requester) {
        return getInvoicePreview(billingId, requester, null);
    }

    @Transactional(readOnly = true)
    public String getInvoicePreview(Long billingId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(billingId, "Billing id is required");
        Objects.requireNonNull(requester, "Requester is required");

        SessionBilling billing = sessionBillingRepository.findById(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Session billing not found"));

        com.smart.therapy.flow.client.entity.Client client = billing.getSession() != null
                ? billing.getSession().getClient()
                : null;

        if (client == null) {
            throw new ResourceNotFoundException("Client not found for this billing record");
        }

        String html = generateInvoiceHtml(billing, client);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                BillingAuditActions.INVOICE_VIEWED, billingId, client.getId(), ipAddress, true);
        return html;
    }

    @Transactional(readOnly = true)
    public String getInvoiceDownload(Long billingId, AuthPrincipal requester) {
        return getInvoiceDownload(billingId, requester, null);
    }

    @Transactional(readOnly = true)
    public String getInvoiceDownload(Long billingId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(billingId, "Billing id is required");
        Objects.requireNonNull(requester, "Requester is required");

        SessionBilling billing = sessionBillingRepository.findById(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Session billing not found"));

        com.smart.therapy.flow.client.entity.Client client = billing.getSession() != null
                ? billing.getSession().getClient()
                : null;

        if (client == null) {
            throw new ResourceNotFoundException("Client not found for this billing record");
        }

        String html = generateInvoiceHtml(billing, client);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                BillingAuditActions.INVOICE_DOWNLOADED, billingId, client.getId(), ipAddress, true);
        return html;
    }

    @Transactional(readOnly = true)
    public byte[] getInvoicePdfForClientPortal(Long billingId, Long clientId) {
        Objects.requireNonNull(billingId, "Billing id is required");
        Objects.requireNonNull(clientId, "Client id is required");

        SessionBilling billing = sessionBillingRepository.findById(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Session billing not found"));
        com.smart.therapy.flow.client.entity.Client client = billing.getSession() != null
                ? billing.getSession().getClient()
                : null;
        if (client == null || !clientId.equals(client.getId())) {
            throw new ResourceNotFoundException("Invoice not found or access denied");
        }
        return generateInvoicePdf(billing, client);
    }

    @Transactional(readOnly = true)
    public String getInvoiceHtmlForClientPortal(Long billingId, Long clientId) {
        Objects.requireNonNull(billingId, "Billing id is required");
        Objects.requireNonNull(clientId, "Client id is required");

        SessionBilling billing = sessionBillingRepository.findById(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Session billing not found"));
        com.smart.therapy.flow.client.entity.Client client = billing.getSession() != null
                ? billing.getSession().getClient()
                : null;
        if (client == null || !clientId.equals(client.getId())) {
            throw new ResourceNotFoundException("Invoice not found or access denied");
        }
        return generateInvoiceHtml(billing, client);
    }

    @Transactional(readOnly = true)
    public byte[] getInvoicePdf(Long billingId, AuthPrincipal requester) {
        return getInvoicePdf(billingId, requester, null);
    }

    @Transactional(readOnly = true)
    public byte[] getInvoicePdf(Long billingId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(billingId, "Billing id is required");
        Objects.requireNonNull(requester, "Requester is required");

        SessionBilling billing = sessionBillingRepository.findById(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Session billing not found"));
        com.smart.therapy.flow.client.entity.Client client = billing.getSession() != null
                ? billing.getSession().getClient()
                : null;
        if (client == null) {
            throw new ResourceNotFoundException("Client not found for this billing record");
        }
        byte[] pdf = generateInvoicePdf(billing, client);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                BillingAuditActions.INVOICE_PDF_VIEWED, billingId, client.getId(), ipAddress, true);
        return pdf;
    }

    private boolean isInvoicePaidForDisplay(
            SessionBilling billing,
            BigDecimal paidAmount,
            BigDecimal remainingDue) {
        if (billing.getBillingStatus() == BillingStatus.PAID) {
            return true;
        }
        // Remaining due of $0 includes 100% discounts (no cash payment required).
        return remainingDue != null && remainingDue.compareTo(BigDecimal.ZERO) == 0;
    }

    private String resolveInvoiceStatusLabel(
            SessionBilling billing,
            boolean paidInFull,
            BigDecimal paidAmount,
            BigDecimal amountDue) {
        BillingStatus status = billing.getBillingStatus();
        if (status == BillingStatus.DENIED) {
            return "DENIED";
        }
        if (status == BillingStatus.FOLLOW_UP) {
            return "FOLLOW UP";
        }
        if (status == BillingStatus.CANCELLED) {
            return "CANCELLED";
        }
        if (paidInFull || status == BillingStatus.PAID) {
            return "PAID IN FULL";
        }
        BigDecimal paid = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        BigDecimal due = amountDue != null ? amountDue : BigDecimal.ZERO;
        if (paid.compareTo(BigDecimal.ZERO) > 0 && paid.compareTo(due) < 0) {
            return "PARTIAL";
        }
        if (status == BillingStatus.PENDING) {
            return "PENDING";
        }
        if (status == BillingStatus.BILLED) {
            return "BILLED";
        }
        return "PENDING";
    }

    private String resolveInvoiceStatusColor(
            SessionBilling billing,
            boolean paidInFull,
            BigDecimal paidAmount,
            BigDecimal amountDue) {
        BillingStatus status = billing.getBillingStatus();
        if (status == BillingStatus.DENIED) {
            return "#dc2626";
        }
        if (status == BillingStatus.CANCELLED) {
            return "#6b7280";
        }
        if (paidInFull || status == BillingStatus.PAID) {
            return "#059669";
        }
        if (billing.getBillingStatus() == BillingStatus.FOLLOW_UP) {
            return "#d97706";
        }
        BigDecimal paid = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        BigDecimal due = amountDue != null ? amountDue : BigDecimal.ZERO;
        if (paid.compareTo(BigDecimal.ZERO) > 0 && paid.compareTo(due) < 0) {
            return "#2563eb";
        }
        return "#000000";
    }

    /**
     * Email-safe invoice fragment for send-invoice emails (tables + Clinical Clarity styles).
     * Printable PDF/preview continues to use {@link #generateInvoiceHtml}.
     */
    private String generateInvoiceEmailHtml(SessionBilling billing, com.smart.therapy.flow.client.entity.Client client) {
        com.smart.therapy.flow.billing.entity.Service service = null;
        if (billing.getSession() != null && billing.getSession().getService() != null) {
            service = billing.getSession().getService();
        }
        PracticeConfigurationResponse practice = practiceConfigurationService.getPracticeConfiguration();
        UserProfile therapistProfile = resolveTherapistProfile(billing);
        String therapistName = billing.getSession() != null && billing.getSession().getTherapist() != null
                ? valueOrFallback(billing.getSession().getTherapist().getFullName(), "N/A")
                : "N/A";
        String licenseName = therapistProfile != null
                ? valueOrFallback(therapistProfile.getLicenseType(), "N/A")
                : "N/A";
        String licenseNumber = therapistProfile != null
                ? valueOrFallback(therapistProfile.getLicenseNumber(), "N/A")
                : "N/A";
        String clientEmail = valueOrEmpty(getClientPrimaryEmailSafe(client));
        String clientPhone = valueOrEmpty(getClientPrimaryPhoneSafe(client));
        String clientName = valueOrFallback(client.getFullName(), "N/A");
        String serviceLabel = service != null ? service.getServiceName() : valueOrFallback(billing.getServiceCode(), "N/A");
        String cptCode = valueOrFallback(billing.getServiceCode(), serviceLabel);
        com.smart.therapy.flow.client.entity.ClientInsurance insurance = getClientInsuranceSafe(client);
        String insuranceProvider = insurance != null ? formatInsuranceProviderName(insurance.getInsuranceProvider()) : "N/A";
        String insurancePolicy = insurance != null ? valueOrFallback(insurance.getPolicyNumber(), "N/A") : "N/A";
        String insuranceGroup = insurance != null ? valueOrFallback(insurance.getGroupNumber(), "N/A") : "N/A";

        BigDecimal serviceAmount = billing.getTotalAmount() != null
                ? billing.getTotalAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountDue = calculateAmountDue(billing);
        BigDecimal paidAmount = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal remainingByAmount = amountDue.subtract(paidAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        boolean paidInFull = isInvoicePaidForDisplay(billing, paidAmount, remainingByAmount);
        BigDecimal totalDueAfterPayments = paidInFull
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : remainingByAmount;
        String paymentStatusLabel = resolveInvoiceStatusLabel(billing, paidInFull, paidAmount, amountDue);
        String statusColor = resolveInvoiceStatusColor(billing, paidInFull, paidAmount, amountDue);

        BigDecimal insurancePaid = billing.getInsurancePaidAmount() != null
                ? billing.getInsurancePaidAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal clientPaid = billing.getClientPaidAmount() != null
                ? billing.getClientPaidAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal plannedInsuranceCoverage = billing.getInsuranceAmount() != null
                ? billing.getInsuranceAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal insuranceCoverageDisplay = insurancePaid.compareTo(BigDecimal.ZERO) > 0
                ? insurancePaid
                : plannedInsuranceCoverage;
        BigDecimal clientCopay = billing.getCopayAmount() != null
                ? billing.getCopayAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        boolean showInsuranceTotals = insuranceCoverageDisplay.compareTo(BigDecimal.ZERO) > 0
                || Boolean.TRUE.equals(billing.getInsuranceCovered())
                || clientCopay.compareTo(BigDecimal.ZERO) > 0
                || insurancePaid.compareTo(BigDecimal.ZERO) > 0
                || clientPaid.compareTo(BigDecimal.ZERO) > 0;

        StringBuilder totalsRows = new StringBuilder();
        totalsRows.append(EmailHtmlComponents.invoiceEmailTotalRow(
                "Service Amount", "$" + serviceAmount.toPlainString(), false));

        if (billing.getDiscountAmount() != null && billing.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            String discountLabel = "Discount";
            if (billing.getDiscountType() != null && billing.getDiscountValue() != null) {
                String discountTypeDisplay = billing.getDiscountType().getDisplayName();
                if (billing.getDiscountType().getValue().equals("percentage")) {
                    discountLabel = String.format("Discount (%s - %s%%)",
                            discountTypeDisplay,
                            billing.getDiscountValue().setScale(0, RoundingMode.HALF_UP).toPlainString());
                } else if (billing.getDiscountType().getValue().equals("fixed_amount")) {
                    discountLabel = String.format("Discount (%s - $%s)",
                            discountTypeDisplay,
                            billing.getDiscountValue().setScale(2, RoundingMode.HALF_UP).toPlainString());
                } else {
                    discountLabel = discountTypeDisplay;
                }
            }
            totalsRows.append(EmailHtmlComponents.invoiceEmailTotalRow(
                    escapeHtml(discountLabel),
                    "-$" + billing.getDiscountAmount().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    false));
        }

        if (showInsuranceTotals) {
            totalsRows.append(EmailHtmlComponents.invoiceEmailTotalRow(
                    "Insurance Coverage",
                    "-$" + insuranceCoverageDisplay.toPlainString(),
                    false));
            totalsRows.append(EmailHtmlComponents.invoiceEmailTotalRow(
                    "Client Copay",
                    "$" + clientCopay.toPlainString(),
                    false));
            if (clientPaid.compareTo(BigDecimal.ZERO) > 0
                    && clientPaid.compareTo(clientCopay) != 0) {
                totalsRows.append(EmailHtmlComponents.invoiceEmailTotalRow(
                        "Client Payments",
                        "-$" + clientPaid.toPlainString(),
                        false));
            }
        }

        boolean sourceBreakdownShown = showInsuranceTotals
                && (insurancePaid.compareTo(BigDecimal.ZERO) > 0 || clientPaid.compareTo(BigDecimal.ZERO) > 0);
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0 && !sourceBreakdownShown) {
            totalsRows.append(EmailHtmlComponents.invoiceEmailTotalRow(
                    "Payments Received",
                    "-$" + paidAmount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    false));
        }

        totalsRows.append(EmailHtmlComponents.invoiceEmailTotalRow(
                "Total Due",
                "$" + totalDueAfterPayments.toPlainString(),
                true));
        totalsRows.append(EmailHtmlComponents.invoiceEmailStatusRow(
                escapeHtml(paymentStatusLabel),
                statusColor));

        java.time.format.DateTimeFormatter invoiceDateFmt =
                java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy");
        String billingDateStr = billing.getBillingDate() != null
                ? billing.getBillingDate().format(invoiceDateFmt)
                : "N/A";
        String serviceDateStr = billing.getSession() != null && billing.getSession().getSessionDate() != null
                ? java.time.LocalDateTime.ofInstant(
                                billing.getSession().getSessionDate(),
                                resolveBillingZoneId())
                        .toLocalDate()
                        .format(invoiceDateFmt)
                : "N/A";

        String invoiceNumber = "INV-%s-%d".formatted(
                client.getClientId() != null ? escapeHtml(client.getClientId()) : "CL",
                billing.getId());

        String practiceName = escapeHtml(valueOrFallback(practice.getPracticeName(), "N/A"));
        String practiceAddress = escapeHtml(valueOrEmpty(practice.getPracticeAddress())).replace("\n", "<br/>");
        String practicePhone = escapeHtml(valueOrEmpty(practice.getPracticePhone()));
        String practiceEmail = escapeHtml(valueOrEmpty(practice.getPracticeEmail()));
        String practiceWebsite = escapeHtml(valueOrEmpty(practice.getPracticeWebsite()));

        return EmailHtmlComponents.invoiceEmailDocument(
                invoiceNumber,
                billingDateStr,
                serviceDateStr,
                practiceName,
                practiceAddress,
                practicePhone,
                practiceEmail,
                practiceWebsite,
                escapeHtml(clientName),
                escapeHtml(clientPhone),
                escapeHtml(clientEmail),
                escapeHtml(insuranceProvider),
                escapeHtml(insurancePolicy),
                escapeHtml(insuranceGroup),
                escapeHtml(serviceLabel),
                escapeHtml(cptCode),
                serviceAmount.toPlainString(),
                totalsRows.toString(),
                escapeHtml(therapistName),
                escapeHtml(licenseName),
                escapeHtml(licenseNumber));
    }

    private String generateInvoiceHtml(SessionBilling billing, com.smart.therapy.flow.client.entity.Client client) {
        com.smart.therapy.flow.billing.entity.Service service = null;
        if (billing.getSession() != null && billing.getSession().getService() != null) {
            service = billing.getSession().getService();
        }
        PracticeConfigurationResponse practice = practiceConfigurationService.getPracticeConfiguration();
        UserProfile therapistProfile = resolveTherapistProfile(billing);
        String therapistName = billing.getSession() != null && billing.getSession().getTherapist() != null
                ? valueOrFallback(billing.getSession().getTherapist().getFullName(), "N/A")
                : "N/A";
        String licenseName = therapistProfile != null
                ? valueOrFallback(therapistProfile.getLicenseType(), "N/A")
                : "N/A";
        String licenseNumber = therapistProfile != null
                ? valueOrFallback(therapistProfile.getLicenseNumber(), "N/A")
                : "N/A";
        String clientEmail = valueOrEmpty(getClientPrimaryEmailSafe(client));
        String clientPhone = valueOrEmpty(getClientPrimaryPhoneSafe(client));
        String clientName = valueOrFallback(client.getFullName(), "N/A");
        String serviceLabel = service != null ? service.getServiceName() : valueOrFallback(billing.getServiceCode(), "N/A");
        String cptCode = valueOrFallback(billing.getServiceCode(), serviceLabel);
        com.smart.therapy.flow.client.entity.ClientInsurance insurance = getClientInsuranceSafe(client);
        String insuranceProvider = insurance != null ? formatInsuranceProviderName(insurance.getInsuranceProvider()) : "N/A";
        String insurancePolicy = insurance != null ? valueOrFallback(insurance.getPolicyNumber(), "N/A") : "N/A";
        String insuranceGroup = insurance != null ? valueOrFallback(insurance.getGroupNumber(), "N/A") : "N/A";

        BigDecimal serviceAmount = billing.getTotalAmount() != null
                ? billing.getTotalAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountDue = calculateAmountDue(billing);
        BigDecimal paidAmount = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal remainingByAmount = amountDue.subtract(paidAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        boolean paidInFull = isInvoicePaidForDisplay(billing, paidAmount, remainingByAmount);
        BigDecimal totalDueAfterPayments = paidInFull
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : remainingByAmount;
        String paymentStatusLabel = resolveInvoiceStatusLabel(billing, paidInFull, paidAmount, amountDue);
        String statusColor = resolveInvoiceStatusColor(billing, paidInFull, paidAmount, amountDue);

        BigDecimal insurancePaid = billing.getInsurancePaidAmount() != null
                ? billing.getInsurancePaidAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal clientPaid = billing.getClientPaidAmount() != null
                ? billing.getClientPaidAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal plannedInsuranceCoverage = billing.getInsuranceAmount() != null
                ? billing.getInsuranceAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        // Prefer actual insurance payments; fall back to planned coverage amount on the bill.
        BigDecimal insuranceCoverageDisplay = insurancePaid.compareTo(BigDecimal.ZERO) > 0
                ? insurancePaid
                : plannedInsuranceCoverage;
        BigDecimal clientCopay = billing.getCopayAmount() != null
                ? billing.getCopayAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        boolean showInsuranceTotals = insuranceCoverageDisplay.compareTo(BigDecimal.ZERO) > 0
                || Boolean.TRUE.equals(billing.getInsuranceCovered())
                || clientCopay.compareTo(BigDecimal.ZERO) > 0
                || insurancePaid.compareTo(BigDecimal.ZERO) > 0
                || clientPaid.compareTo(BigDecimal.ZERO) > 0;

        StringBuilder totalsRows = new StringBuilder();
        totalsRows.append("""
                <div class="total-row">
                  <span>Service Amount:</span>
                  <span>$%s</span>
                </div>
                """.formatted(serviceAmount.toPlainString()));

        if (billing.getDiscountAmount() != null && billing.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            String discountLabel = "Discount";
            if (billing.getDiscountType() != null && billing.getDiscountValue() != null) {
                String discountTypeDisplay = billing.getDiscountType().getDisplayName();
                if (billing.getDiscountType().getValue().equals("percentage")) {
                    discountLabel = String.format("Discount (%s - %s%%)",
                            discountTypeDisplay,
                            billing.getDiscountValue().setScale(0, RoundingMode.HALF_UP).toPlainString());
                } else if (billing.getDiscountType().getValue().equals("fixed_amount")) {
                    discountLabel = String.format("Discount (%s - $%s)",
                            discountTypeDisplay,
                            billing.getDiscountValue().setScale(2, RoundingMode.HALF_UP).toPlainString());
                } else {
                    discountLabel = discountTypeDisplay;
                }
            }
            totalsRows.append("""
                    <div class="total-row">
                      <span>%s:</span>
                      <span>-$%s</span>
                    </div>
                    """.formatted(
                    escapeHtml(discountLabel),
                    billing.getDiscountAmount().setScale(2, RoundingMode.HALF_UP).toPlainString()));
        }

        if (showInsuranceTotals) {
            totalsRows.append("""
                    <div class="total-row" style="color: #2563eb;">
                      <span>Insurance Coverage:</span>
                      <span>-$%s</span>
                    </div>
                    <div class="total-row">
                      <span>Client Copay:</span>
                      <span>$%s</span>
                    </div>
                    """.formatted(
                    insuranceCoverageDisplay.toPlainString(),
                    clientCopay.toPlainString()));
            if (clientPaid.compareTo(BigDecimal.ZERO) > 0
                    && clientPaid.compareTo(clientCopay) != 0) {
                totalsRows.append("""
                        <div class="total-row">
                          <span>Client Payments:</span>
                          <span>-$%s</span>
                        </div>
                        """.formatted(clientPaid.toPlainString()));
            }
        }

        // Avoid double-counting when insurance/client buckets already explain the paid total.
        boolean sourceBreakdownShown = showInsuranceTotals
                && (insurancePaid.compareTo(BigDecimal.ZERO) > 0 || clientPaid.compareTo(BigDecimal.ZERO) > 0);
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0 && !sourceBreakdownShown) {
            totalsRows.append("""
                    <div class="total-row">
                      <span>Payments Received:</span>
                      <span>-$%s</span>
                    </div>
                    """.formatted(paidAmount.setScale(2, RoundingMode.HALF_UP).toPlainString()));
        }

        totalsRows.append("""
                <div class="total-row total-due">
                  <span>Total Due:</span>
                  <span>$%s</span>
                </div>
                <div class="total-row" style="margin-top: 8px;">
                  <span>Status:</span>
                  <span style="color: %s; font-weight: bold;">%s</span>
                </div>
                """.formatted(
                totalDueAfterPayments.toPlainString(),
                statusColor,
                escapeHtml(paymentStatusLabel)));

        java.time.format.DateTimeFormatter invoiceDateFmt =
                java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy");
        String billingDateStr = billing.getBillingDate() != null
                ? billing.getBillingDate().format(invoiceDateFmt)
                : "N/A";
        String serviceDateStr = billing.getSession() != null && billing.getSession().getSessionDate() != null
                ? java.time.LocalDateTime.ofInstant(
                                billing.getSession().getSessionDate(),
                                resolveBillingZoneId())
                        .toLocalDate()
                        .format(invoiceDateFmt)
                : "N/A";

        String invoiceNumber = "INV-%s-%d".formatted(
                client.getClientId() != null ? escapeHtml(client.getClientId()) : "CL",
                billing.getId());
        String pageTitle = "Invoice - %s - %s".formatted(
                escapeHtml(clientName),
                escapeHtml(serviceLabel));

        String practiceName = escapeHtml(valueOrFallback(practice.getPracticeName(), "N/A"));
        String practiceAddress = escapeHtml(valueOrEmpty(practice.getPracticeAddress())).replace("\n", "<br/>");
        String practicePhone = escapeHtml(valueOrEmpty(practice.getPracticePhone()));
        String practiceEmail = escapeHtml(valueOrEmpty(practice.getPracticeEmail()));
        String practiceWebsite = escapeHtml(valueOrEmpty(practice.getPracticeWebsite()));

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8"/>
                  <title>%s</title>
                  <style>
                    body {
                      font-family: 'Times New Roman', Times, serif;
                      margin: 40px;
                      font-size: 11pt;
                      line-height: 1.4;
                      color: #000000;
                    }
                    .header { display: flex; justify-content: space-between; margin-bottom: 40px; }
                    .invoice-title {
                      font-size: 26px;
                      font-weight: bold;
                      color: #000000;
                      font-family: 'Times New Roman', Times, serif;
                      text-transform: uppercase;
                      letter-spacing: 1px;
                      margin: 0 0 12px 0;
                    }
                    .company-info {
                      text-align: right;
                      color: #333333;
                      font-size: 10pt;
                      line-height: 1.3;
                      font-family: 'Times New Roman', Times, serif;
                    }
                    .company-info h3 {
                      font-size: 13pt;
                      font-weight: bold;
                      color: #000000;
                      margin: 0 0 8px 0;
                      font-family: 'Times New Roman', Times, serif;
                    }
                    .company-info p { margin: 0 0 2px 0; }
                    .meta p { margin: 0 0 4px 0; }
                    .client-info { display: flex; gap: 60px; margin-bottom: 40px; }
                    .client-info > div { flex: 1; min-width: 0; }
                    .section-title {
                      font-size: 13pt;
                      font-weight: bold;
                      color: #000000;
                      margin: 0 0 12px 0;
                      font-family: 'Times New Roman', Times, serif;
                      text-transform: uppercase;
                      border-bottom: 1px solid #000000;
                      padding-bottom: 4px;
                    }
                    .client-info p { margin: 0 0 4px 0; }
                    table {
                      width: 100%%;
                      border-collapse: collapse;
                      margin-bottom: 30px;
                      font-size: 10pt;
                    }
                    th, td {
                      border: 1px solid #000000;
                      padding: 10px 12px;
                      text-align: left;
                      font-family: 'Times New Roman', Times, serif;
                    }
                    th {
                      background-color: #f5f5f5;
                      font-weight: bold;
                      color: #000000;
                    }
                    .totals { width: 300px; margin-left: auto; }
                    .total-row {
                      display: flex;
                      justify-content: space-between;
                      margin-bottom: 8px;
                      font-size: 10pt;
                      font-family: 'Times New Roman', Times, serif;
                    }
                    .total-due {
                      font-weight: bold;
                      font-size: 13pt;
                      border-top: 2px solid #000000;
                      padding-top: 8px;
                      color: #000000;
                      font-family: 'Times New Roman', Times, serif;
                    }
                    .provider-box {
                      margin-top: 40px;
                      padding: 20px;
                      border-top: 2px solid #e2e8f0;
                      background-color: #f8fafc;
                      font-size: 12px;
                      color: #64748b;
                    }
                    .provider-box h4 {
                      color: #1e293b;
                      margin: 0 0 15px 0;
                      font-size: 13px;
                    }
                    .provider-box p { margin: 0 0 6px 0; color: #64748b; }
                    @media print {
                      body { margin: 0.5in; font-size: 10pt; }
                      .header { margin-bottom: 20px; }
                      .invoice-title { font-size: 22pt; }
                      .section-title { font-size: 11pt; }
                      .client-info { margin-bottom: 20px; }
                      table { font-size: 9pt; }
                      th, td { padding: 8px; }
                    }
                  </style>
                </head>
                <body>
                  <div class="header">
                    <div class="meta">
                      <h1 class="invoice-title">INVOICE</h1>
                      <p>Invoice #: %s</p>
                      <p>Date: %s</p>
                      <p>Service Date: %s</p>
                    </div>
                    <div class="company-info">
                      <h3>%s</h3>
                      <div style="margin-top: 10px; font-size: 0.9em;">
                        <p>%s</p>
                        <p>Phone: %s</p>
                        <p>Email: %s</p>
                        <p>Website: %s</p>
                      </div>
                    </div>
                  </div>

                  <div class="client-info">
                    <div>
                      <h3 class="section-title">Bill To:</h3>
                      <p>%s</p>
                      <p></p>
                      <p>%s</p>
                      <p>%s</p>
                    </div>
                    <div>
                      <h3 class="section-title">Insurance Info:</h3>
                      <p>Provider: %s</p>
                      <p>Policy: %s</p>
                      <p>Group: %s</p>
                    </div>
                  </div>

                  <table>
                    <thead>
                      <tr>
                        <th>Service</th>
                        <th>CPT Code</th>
                        <th>Date</th>
                        <th style="text-align: right;">Amount</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%s</td>
                        <td style="text-align: right;">$%s</td>
                      </tr>
                    </tbody>
                  </table>

                  <div class="totals">
                    %s
                  </div>

                  <div class="provider-box">
                    <h4>Provider Information for Insurance Reimbursement</h4>
                    <div>
                      <p><strong>Provider Name:</strong> %s</p>
                      <p><strong>License Name:</strong> %s</p>
                      <p><strong>License Number:</strong> %s</p>
                    </div>
                  </div>
                </body>
                </html>
                """
                .formatted(
                        pageTitle,
                        invoiceNumber,
                        billingDateStr,
                        serviceDateStr,
                        practiceName,
                        practiceAddress,
                        practicePhone,
                        practiceEmail,
                        practiceWebsite,
                        escapeHtml(clientName),
                        escapeHtml(clientPhone),
                        escapeHtml(clientEmail),
                        escapeHtml(insuranceProvider),
                        escapeHtml(insurancePolicy),
                        escapeHtml(insuranceGroup),
                        escapeHtml(serviceLabel),
                        escapeHtml(cptCode),
                        serviceDateStr,
                        serviceAmount.toPlainString(),
                        totalsRows.toString(),
                        escapeHtml(therapistName),
                        escapeHtml(licenseName),
                        escapeHtml(licenseNumber));
    }

    private String valueOrEmpty(String value) {
        return value != null && !value.trim().isEmpty() ? value.trim() : "";
    }

    private byte[] generateInvoicePdf(SessionBilling billing, com.smart.therapy.flow.client.entity.Client client) {
        try {
            return HtmlToPdfConverter.toPdfBytes(generateInvoiceHtml(billing, client));
        } catch (RuntimeException ex) {
            log.error("Failed to generate invoice PDF for billingId={}", billing.getId(), ex);
            throw new BadRequestException("Failed to generate invoice PDF");
        }
    }

    private UserProfile resolveTherapistProfile(SessionBilling billing) {
        if (billing.getSession() == null || billing.getSession().getTherapist() == null
                || billing.getSession().getTherapist().getId() == null) {
            return null;
        }
        try {
            return userProfileRepository.findByUserId(billing.getSession().getTherapist().getId()).orElse(null);
        } catch (RuntimeException ex) {
            log.warn("Unable to resolve therapist profile for therapistId={}. Continuing without license details.",
                    billing.getSession().getTherapist().getId(), ex);
            return null;
        }
    }

    private String valueOrFallback(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
    }

    private String getClientPrimaryEmailSafe(com.smart.therapy.flow.client.entity.Client client) {
        if (client == null) {
            return null;
        }
        try {
            return client.getPrimaryEmail();
        } catch (RuntimeException ex) {
            log.warn("Unable to read primary email for clientId={}", client.getId(), ex);
            return null;
        }
    }

    private String getClientPrimaryPhoneSafe(com.smart.therapy.flow.client.entity.Client client) {
        if (client == null) {
            return null;
        }
        try {
            return client.getPrimaryPhone();
        } catch (RuntimeException ex) {
            log.warn("Unable to read primary phone for clientId={}", client.getId(), ex);
            return null;
        }
    }

    private com.smart.therapy.flow.client.entity.ClientInsurance getClientInsuranceSafe(
            com.smart.therapy.flow.client.entity.Client client) {
        if (client == null) {
            return null;
        }
        try {
            return client.getInsurance();
        } catch (RuntimeException ex) {
            log.warn("Unable to read insurance details for clientId={}", client.getId(), ex);
            return null;
        }
    }

    /**
     * Convert insurance provider backend key to user-friendly display name.
     * Examples: "sun_life" -> "Sun Life", "blue_cross_blue_shield" -> "Blue Cross Blue Shield"
     */
    private String formatInsuranceProviderName(String providerKey) {
        if (providerKey == null || providerKey.trim().isEmpty()) {
            return "N/A";
        }

        // Replace underscores with spaces
        String formatted = providerKey.replace("_", " ");

        // Capitalize each word
        String[] words = formatted.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) {
                continue;
            }

            // Capitalize first letter, lowercase the rest
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1).toLowerCase());
            }

            // Add space between words (but not after the last word)
            if (i < words.length - 1) {
                result.append(" ");
            }
        }

        return result.toString();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // ========== PAYMENT TRACKING METHODS ==========

    /**
     * Get all payments for a session billing (ordered by payment date DESC, then createdAt DESC)
     */
    @Transactional(readOnly = true)
    public List<com.smart.therapy.flow.billing.dto.PaymentResponse> getPaymentsForBilling(Long billingId) {
        return paymentRepository.findBySessionBillingIdOrderByPaymentDateDescCreatedAtDesc(billingId)
                .stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get latest payment for a session billing (newest payment by date, with createdAt as tiebreaker)
     */
    @Transactional(readOnly = true)
    public com.smart.therapy.flow.billing.dto.PaymentResponse getLatestPayment(Long billingId) {
        return paymentRepository.findFirstBySessionBillingIdOrderByPaymentDateDescCreatedAtDesc(billingId)
                .map(this::toPaymentResponse)
                .orElse(null);
    }

    /**
     * Get refund history for a session billing.
     */
    @Transactional(readOnly = true)
    public RefundHistoryResponse getRefundsForBilling(Long billingId) {
        Objects.requireNonNull(billingId, "Billing id is required");
        if (!sessionBillingRepository.existsById(billingId)) {
            throw new ResourceNotFoundException("Session billing not found");
        }

        List<com.smart.therapy.flow.billing.dto.PaymentResponse> refunds = paymentRepository
                .findBySessionBillingIdAndStatusOrderByPaymentDateDesc(billingId, PaymentStatus.REFUNDED)
                .stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());

        BigDecimal totalRefunded = refunds.stream()
                .map(PaymentResponse::getAmount)
                .map(amount -> amount != null ? amount.abs() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return RefundHistoryResponse.builder()
                .billingId(billingId)
                .refundCount((long) refunds.size())
                .totalRefunded(totalRefunded)
                .refunds(refunds)
                .build();
    }

    /**
     * Get payment transactions for a billing record ordered by newest first.
     */
    @Transactional(readOnly = true)
    public List<com.smart.therapy.flow.billing.dto.PaymentTransactionResponse> getTransactionsForBilling(Long billingId) {
        Objects.requireNonNull(billingId, "Billing id is required");
        if (!sessionBillingRepository.existsById(billingId)) {
            throw new ResourceNotFoundException("Session billing not found");
        }

        return paymentTransactionRepository.findBySessionBillingIdOrderByCreatedAtDesc(billingId)
                .stream()
                .map(this::toPaymentTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Void a payment transaction and exclude its payment entry from billing rollups.
     */
    @Transactional
    public SessionBillingResponse voidPaymentTransaction(
            Long billingId,
            Long transactionId,
            VoidPaymentTransactionRequest request,
            AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(billingId, "Billing id is required");
        Objects.requireNonNull(transactionId, "Transaction id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        SessionBilling billing = sessionBillingRepository.findByIdForUpdate(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Session billing not found"));

        billingGuard.requireTenantContext();
        billingGuard.assertStaffBillingAccess(billing, requester);

        com.smart.therapy.flow.billing.entity.PaymentTransaction transaction = paymentTransactionRepository
                .findByIdAndBillingIdForUpdate(transactionId, billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found"));

        if (Boolean.TRUE.equals(transaction.getIsVoided())) {
            throw new BadRequestException("Payment transaction is already voided");
        }

        com.smart.therapy.flow.billing.entity.Payment payment = transaction.getPayment();
        if (payment == null || payment.getSessionBilling() == null
                || !Objects.equals(payment.getSessionBilling().getId(), billingId)) {
            throw new BadRequestException("Payment transaction does not belong to provided billing record");
        }

        String voidReason = request.getVoidReason() != null ? request.getVoidReason().trim() : null;
        if (voidReason == null || voidReason.isEmpty()) {
            throw new BadRequestException("Void reason is required");
        }

        Long actorId = currentUserService.requireCurrentUser(requester).getId();

        transaction.setIsVoided(true);
        transaction.setVoidedAt(Instant.now());
        transaction.setVoidedBy(actorId);
        transaction.setVoidReason(voidReason);
        transaction.setStatus(PaymentStatus.FAILED);
        paymentTransactionRepository.save(transaction);

        payment.setStatus(PaymentStatus.FAILED);
        String existingNotes = payment.getNotes();
        String voidNote = "Voided transaction #" + transactionId + ": " + voidReason;
        payment.setNotes(existingNotes != null && !existingNotes.isBlank()
                ? existingNotes + "\n" + voidNote
                : voidNote);
        paymentRepository.save(payment);

        recalculateBillingAmountsAndStatus(billing);
        SessionBilling updated = sessionBillingRepository.save(billing);

        recordAuditEvent(actorId, BillingAuditActions.PAYMENT_VOIDED, billingId, ipAddress);
        return toSessionBillingResponse(updated);
    }

    private void createManualPaymentLeg(
            SessionBilling billing,
            SplitPaymentLegRequest leg,
            String sharedNotes,
            PaymentSource paymentSource) {
        if (leg == null || leg.getAmount() == null || leg.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (leg.getPaymentMethod() == null) {
            throw new BadRequestException("Payment method is required for each non-zero payment leg");
        }
        String combinedNotes = sharedNotes;
        if (combinedNotes != null && !combinedNotes.trim().isEmpty() && paymentSource == PaymentSource.INSURANCE_PORTAL) {
            combinedNotes = "[insurance] " + combinedNotes.trim();
        } else if (combinedNotes != null && !combinedNotes.trim().isEmpty() && paymentSource == PaymentSource.MANUAL) {
            combinedNotes = "[client] " + combinedNotes.trim();
        }

        com.smart.therapy.flow.billing.entity.Payment savedPayment = paymentRepository.save(
                com.smart.therapy.flow.billing.entity.Payment.builder()
                        .sessionBilling(billing)
                        .amount(leg.getAmount().setScale(2, RoundingMode.HALF_UP))
                        .paymentMethod(leg.getPaymentMethod())
                        .paymentSource(paymentSource)
                        .status(PaymentStatus.PAID)
                        .paymentDate(leg.getPaymentDate() != null ? leg.getPaymentDate() : Instant.now())
                        .reference(leg.getReferenceNumber())
                        .notes(combinedNotes)
                        .build());

        paymentTransactionRepository.save(
                com.smart.therapy.flow.billing.entity.PaymentTransaction.builder()
                        .payment(savedPayment)
                        .provider(PaymentSource.MANUAL)
                        .transactionType(com.smart.therapy.flow.billing.enums.TransactionType.CHARGE)
                        .amount(savedPayment.getAmount())
                        .status(PaymentStatus.PAID)
                        .providerIntentId(leg.getReferenceNumber())
                        .build());
    }

    private PaymentSource resolvePaymentSource(String paymentSide, PaymentMethod paymentMethod) {
        if (paymentSide != null) {
            String normalized = paymentSide.trim().toLowerCase();
            if ("insurance".equals(normalized)) {
                return PaymentSource.INSURANCE_PORTAL;
            }
            if ("client".equals(normalized)) {
                return PaymentSource.MANUAL;
            }
        }
        return paymentMethod == PaymentMethod.INSURANCE ? PaymentSource.INSURANCE_PORTAL : PaymentSource.MANUAL;
    }

    private void enforceZeroBillGuard(
            SessionBilling billing,
            BigDecimal paymentAmount,
            boolean allowZeroBillOverpayment,
            String overrideReason,
            AuthPrincipal requester,
            String ipAddress) {
        BigDecimal amountDue = calculateAmountDue(billing);
        if (amountDue.compareTo(BigDecimal.ZERO) > 0 || paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (!allowZeroBillOverpayment) {
            throw new BadRequestException(
                    "This bill has a total due of 0.00. Payment recording is blocked unless an explicit override is provided.");
        }
        if (overrideReason == null || overrideReason.trim().length() < 3) {
            throw new BadRequestException("overrideReason is required when allowZeroBillOverpayment is true");
        }
        Long actorId = currentUserService.requireCurrentUser(requester).getId();
        recordAuditEvent(actorId, "payment_zero_bill_override", requireBillingId(billing), ipAddress);
        log.warn("Zero-bill payment override used: billingId={}, actorId={}, reason={}",
                billing.getId(), actorId, overrideReason);
    }

    /**
     * Calculate the refundable balance from active ledger entries, including signed refunds.
     */
    private BigDecimal calculateTotalPaidAmount(Long billingId) {
        return paymentTransactionRepository.findBySessionBillingIdOrderByCreatedAtDesc(billingId).stream()
                .filter(t -> !Boolean.TRUE.equals(t.getIsVoided()))
                .filter(t -> t.getStatus() == PaymentStatus.PAID || t.getStatus() == PaymentStatus.PARTIAL || t.getStatus() == PaymentStatus.REFUNDED)
                .map(PaymentTransaction::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void requireLedgerStatus(SessionBilling billing, PaymentStatus requested) {
        BigDecimal paid = calculateTotalPaidAmount(billing.getId());
        BigDecimal due = calculateAmountDue(billing);
        boolean matches = switch (requested) {
            case PAID -> paid.compareTo(due) >= 0;
            case PARTIAL -> paid.signum() > 0 && paid.compareTo(due) < 0;
            case PENDING -> paid.signum() == 0 && due.signum() > 0;
            default -> false;
        };
        if (!matches) throw new BadRequestException("Record a payment or refund first; the requested status does not match the payment ledger");
    }

    private void recalculateBillingAmountsAndStatus(SessionBilling billing) {
        Long billingId = requireBillingId(billing);
        List<com.smart.therapy.flow.billing.entity.PaymentTransaction> transactions =
                paymentTransactionRepository.findBySessionBillingIdOrderByCreatedAtDesc(billingId);

        BigDecimal totalPaid = transactions.stream()
                .filter(txn -> !Boolean.TRUE.equals(txn.getIsVoided()))
                .filter(txn -> txn.getStatus() == PaymentStatus.PAID
                        || txn.getStatus() == PaymentStatus.PARTIAL
                        || txn.getStatus() == PaymentStatus.REFUNDED)
                .map(com.smart.therapy.flow.billing.entity.PaymentTransaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        billing.setPaidAmount(totalPaid.setScale(2, RoundingMode.HALF_UP));

        BigDecimal insurancePaid = transactions.stream()
                .filter(txn -> !Boolean.TRUE.equals(txn.getIsVoided()))
                .filter(txn -> txn.getStatus() == PaymentStatus.PAID
                        || txn.getStatus() == PaymentStatus.PARTIAL
                        || txn.getStatus() == PaymentStatus.REFUNDED)
                .filter(txn -> txn.getPayment() != null && (
                        txn.getPayment().getPaymentMethod() == PaymentMethod.INSURANCE
                                || txn.getPayment().getPaymentSource() == PaymentSource.INSURANCE_PORTAL))
                .map(com.smart.therapy.flow.billing.entity.PaymentTransaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal clientPaid = totalPaid.subtract(insurancePaid).setScale(2, RoundingMode.HALF_UP);

        billing.setInsurancePaidAmount(insurancePaid);
        billing.setClientPaidAmount(clientPaid);

        BigDecimal amountDue = calculateAmountDue(billing);
        BigDecimal remainingDue = amountDue.subtract(totalPaid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        billing.setOutstandingAmount(remainingDue);

        if (totalPaid.compareTo(amountDue) >= 0) {
            billing.setBillingStatus(BillingStatus.PAID);
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            billing.setBillingStatus(BillingStatus.BILLED);
        } else if (billing.getBillingStatus() == null || billing.getBillingStatus() == BillingStatus.PAID) {
            billing.setBillingStatus(BillingStatus.PENDING);
        }
    }

    private void applyAvailableClientCredit(SessionBilling targetBilling) {
        if (targetBilling.getSession() == null || targetBilling.getSession().getClient() == null) {
            return;
        }

        Long targetBillingId = requireBillingId(targetBilling);
        Long clientId = targetBilling.getSession().getClient().getId();
        if (clientId == null) {
            return;
        }

        BigDecimal targetDue = calculateRemainingDue(targetBilling);
        if (targetDue.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        List<SessionBilling> clientBillings = sessionBillingRepository.findByClientIdForUpdate(clientId);
        List<SessionBilling> creditSources = clientBillings.stream()
                .filter(b -> b.getId() != null && !b.getId().equals(targetBillingId))
                .filter(b -> calculateCreditAmount(b).compareTo(BigDecimal.ZERO) > 0)
                .sorted(java.util.Comparator
                        .comparing((SessionBilling b) -> b.getBillingDate(), java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                        .thenComparing(SessionBilling::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();

        BigDecimal remainingTargetDue = targetDue;
        for (SessionBilling sourceBilling : creditSources) {
            if (remainingTargetDue.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal sourceCredit = calculateCreditAmount(sourceBilling);
            if (sourceCredit.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal transferAmount = sourceCredit.min(remainingTargetDue).setScale(2, RoundingMode.HALF_UP);
            if (transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Consume credit from source billing with a negative adjustment payment.
            saveCreditTransfer(com.smart.therapy.flow.billing.entity.Payment.builder()
                    .sessionBilling(sourceBilling)
                    .amount(transferAmount.negate())
                    .paymentMethod(PaymentMethod.CREDIT_BALANCE)
                    .paymentSource(PaymentSource.CREDIT_BALANCE_TRANSFER)
                    .status(PaymentStatus.PAID)
                    .paymentDate(Instant.now())
                    .reference("CREDIT-TRANSFER-OUT-" + sourceBilling.getId() + "-" + targetBillingId)
                    .notes("Credit applied to billing #" + targetBillingId)
                    .build());

            // Apply credit to target billing as an internal payment.
            saveCreditTransfer(com.smart.therapy.flow.billing.entity.Payment.builder()
                    .sessionBilling(targetBilling)
                    .amount(transferAmount)
                    .paymentMethod(PaymentMethod.CREDIT_BALANCE)
                    .paymentSource(PaymentSource.CREDIT_BALANCE_TRANSFER)
                    .status(PaymentStatus.PAID)
                    .paymentDate(Instant.now())
                    .reference("CREDIT-TRANSFER-IN-" + sourceBilling.getId() + "-" + targetBillingId)
                    .notes("Credit received from billing #" + sourceBilling.getId())
                    .build());

            recalculateBillingAmountsAndStatus(sourceBilling);
            sessionBillingRepository.save(sourceBilling);

            remainingTargetDue = remainingTargetDue.subtract(transferAmount).max(BigDecimal.ZERO);
        }

        recalculateBillingAmountsAndStatus(targetBilling);
        sessionBillingRepository.save(targetBilling);
    }

    private void saveCreditTransfer(Payment payment) {
        Payment saved = paymentRepository.save(payment);
        paymentTransactionRepository.save(PaymentTransaction.builder()
                .payment(saved).provider(PaymentSource.CREDIT_BALANCE_TRANSFER)
                .transactionType(TransactionType.ADJUSTMENT).status(PaymentStatus.PAID)
                .amount(saved.getAmount()).providerIntentId(saved.getReference()).build());
    }

    private BigDecimal calculateRemainingDue(SessionBilling billing) {
        BigDecimal amountDue = calculateAmountDue(billing);
        BigDecimal paidAmount = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
        return amountDue.subtract(paidAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCreditAmount(SessionBilling billing) {
        BigDecimal amountDue = calculateAmountDue(billing);
        BigDecimal paidAmount = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
        return paidAmount.subtract(amountDue).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Convert Payment entity to PaymentResponse DTO
     */
    private com.smart.therapy.flow.billing.dto.PaymentResponse toPaymentResponse(
            com.smart.therapy.flow.billing.entity.Payment payment) {
        // Get associated transactions
        List<com.smart.therapy.flow.billing.dto.PaymentTransactionResponse> transactions = paymentTransactionRepository
                .findByPaymentIdOrderByCreatedAtDesc(payment.getId())
                .stream()
                .map(this::toPaymentTransactionResponse)
                .collect(Collectors.toList());

        return com.smart.therapy.flow.billing.dto.PaymentResponse.builder()
                .id(payment.getId())
                .sessionBillingId(payment.getSessionBilling() != null ? payment.getSessionBilling().getId() : null)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().getValue() : null)
                .paymentSource(payment.getPaymentSource() != null ? payment.getPaymentSource().getValue() : null)
                .status(payment.getStatus() != null ? payment.getStatus().getValue() : null)
                .paymentDate(payment.getPaymentDate())
                .reference(payment.getReference())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .transactions(transactions)
                .build();
    }

    /**
     * Convert PaymentTransaction entity to PaymentTransactionResponse DTO
     */
    private com.smart.therapy.flow.billing.dto.PaymentTransactionResponse toPaymentTransactionResponse(
            com.smart.therapy.flow.billing.entity.PaymentTransaction transaction) {
        return com.smart.therapy.flow.billing.dto.PaymentTransactionResponse.builder()
                .id(transaction.getId())
                .provider(transaction.getProvider() != null ? transaction.getProvider().getValue() : null)
                .transactionType(transaction.getTransactionType() != null ? transaction.getTransactionType().getValue() : null)
                .amount(transaction.getAmount())
                .providerIntentId(transaction.getProviderIntentId())
                .providerChargeId(transaction.getProviderChargeId())
                .providerCustomerId(transaction.getProviderCustomerId())
                .providerPaymentMethodId(transaction.getProviderPaymentMethodId())
                .status(transaction.getStatus() != null ? transaction.getStatus().getValue() : null)
                .failureReason(transaction.getFailureReason())
                .voided(Boolean.TRUE.equals(transaction.getIsVoided()))
                .voidReason(transaction.getVoidReason())
                .voidedAt(transaction.getVoidedAt())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
