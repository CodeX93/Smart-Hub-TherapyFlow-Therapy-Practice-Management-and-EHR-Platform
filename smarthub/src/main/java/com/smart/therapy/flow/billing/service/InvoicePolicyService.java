package com.smart.therapy.flow.billing.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.audit.support.AuditEventDraft;
import com.smart.therapy.flow.billing.dto.InvoicePolicyRateResult;
import com.smart.therapy.flow.billing.dto.InvoicePolicyRequest;
import com.smart.therapy.flow.billing.dto.InvoicePolicyResponse;
import com.smart.therapy.flow.billing.dto.InvoicePolicyServiceOptionResponse;
import com.smart.therapy.flow.billing.entity.InvoicePolicy;
import com.smart.therapy.flow.billing.enums.InvoicePolicyPriceType;
import com.smart.therapy.flow.billing.repository.InvoicePolicyRepository;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.enums.SessionStatus;
import com.smart.therapy.flow.system.dto.OptionCategoryResponse;
import com.smart.therapy.flow.system.dto.SystemOptionResponse;
import com.smart.therapy.flow.system.service.SystemOptionKeyMatcher;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InvoicePolicyService {

    private static final String RESOURCE_TYPE_INVOICE_POLICY = "invoice_policy";

    public static final String DEFAULT_CLIENT_TYPE_CATEGORY = "client_type";
    public static final String APPOINTMENT_STATUS_CATEGORY = "session_status";
    /** Wildcard key meaning the policy applies to every client type / session status. */
    public static final String ALL_SCOPE_KEY = "all";

    private final InvoicePolicyRepository invoicePolicyRepository;
    private final ServiceRepository serviceRepository;
    private final SystemOptionResolverService systemOptionResolverService;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;
    private final TimezoneService timezoneService;

    @Transactional(readOnly = true)
    public List<InvoicePolicyResponse> listPolicies() {
        return invoicePolicyRepository.findAll().stream()
                .sorted((a, b) -> {
                    int left = a.getClientTypeLabel().compareToIgnoreCase(b.getClientTypeLabel());
                    if (left != 0) {
                        return left;
                    }
                    return a.getAppointmentStatusLabel().compareToIgnoreCase(b.getAppointmentStatusLabel());
                })
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoicePolicyResponse getPolicy(Long id) {
        return toResponse(requirePolicy(id));
    }

    @Transactional
    public InvoicePolicyResponse createPolicy(InvoicePolicyRequest request, AuthPrincipal requester, String ipAddress) {
        String clientTypeKey = normalizeKey(request.getClientTypeKey());
        String appointmentStatusKey = normalizeKey(request.getAppointmentStatusKey());
        Long serviceId = resolveServiceId(request);

        if (invoicePolicyRepository.findByScope(clientTypeKey, appointmentStatusKey, serviceId).isPresent()) {
            throw new BadRequestException(
                    "Invoice policy already exists for selected client type, appointment status, and service scope");
        }

        validateBusinessRules(request, serviceId);
        InvoicePolicy policy = InvoicePolicy.builder()
                .clientTypeKey(clientTypeKey)
                .clientTypeLabel(request.getClientTypeLabel().trim())
                .appointmentStatusKey(appointmentStatusKey)
                .appointmentStatusLabel(request.getAppointmentStatusLabel().trim())
                .enabled(request.getEnabled())
                .priceType(request.getPriceType())
                .invoicePrice(request.getInvoicePrice().setScale(2, RoundingMode.HALF_UP))
                .policyName(trimToNull(request.getPolicyName()))
                .serviceId(serviceId)
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .build();

        InvoicePolicy saved = invoicePolicyRepository.save(policy);
        recordAuditEvent(requester, "invoice_policy_created", saved.getId(), ipAddress);
        return toResponse(saved);
    }

    @Transactional
    public InvoicePolicyResponse updatePolicy(Long id, InvoicePolicyRequest request, AuthPrincipal requester, String ipAddress) {
        InvoicePolicy policy = requirePolicy(id);

        String clientTypeKey = normalizeKey(request.getClientTypeKey());
        String appointmentStatusKey = normalizeKey(request.getAppointmentStatusKey());
        Long serviceId = resolveServiceId(request);
        Optional<InvoicePolicy> duplicate = invoicePolicyRepository.findByScope(clientTypeKey, appointmentStatusKey, serviceId);
        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
            throw new BadRequestException(
                    "Invoice policy already exists for selected client type, appointment status, and service scope");
        }

        validateBusinessRules(request, serviceId);
        policy.setClientTypeKey(clientTypeKey);
        policy.setClientTypeLabel(request.getClientTypeLabel().trim());
        policy.setAppointmentStatusKey(appointmentStatusKey);
        policy.setAppointmentStatusLabel(request.getAppointmentStatusLabel().trim());
        policy.setEnabled(request.getEnabled());
        policy.setPriceType(request.getPriceType());
        policy.setInvoicePrice(request.getInvoicePrice().setScale(2, RoundingMode.HALF_UP));
        policy.setPolicyName(trimToNull(request.getPolicyName()));
        policy.setServiceId(serviceId);
        policy.setEffectiveFrom(request.getEffectiveFrom());
        policy.setEffectiveTo(request.getEffectiveTo());
        policy.setPriority(request.getPriority() != null ? request.getPriority() : 0);

        InvoicePolicy saved = invoicePolicyRepository.save(policy);
        recordAuditEvent(requester, "invoice_policy_updated", saved.getId(), ipAddress);
        return toResponse(saved);
    }

    @Transactional
    public InvoicePolicyResponse activatePolicy(Long id, AuthPrincipal requester, String ipAddress) {
        InvoicePolicy policy = requirePolicy(id);
        policy.setEnabled(true);
        InvoicePolicy saved = invoicePolicyRepository.save(policy);
        recordAuditEvent(requester, "invoice_policy_activated", saved.getId(), ipAddress);
        return toResponse(saved);
    }

    @Transactional
    public InvoicePolicyResponse deactivatePolicy(Long id, AuthPrincipal requester, String ipAddress) {
        InvoicePolicy policy = requirePolicy(id);
        policy.setEnabled(false);
        InvoicePolicy saved = invoicePolicyRepository.save(policy);
        recordAuditEvent(requester, "invoice_policy_deactivated", saved.getId(), ipAddress);
        return toResponse(saved);
    }

    @Transactional
    public void deletePolicy(Long id, AuthPrincipal requester, String ipAddress) {
        InvoicePolicy policy = requirePolicy(id);
        invoicePolicyRepository.delete(policy);
        recordAuditEvent(requester, "invoice_policy_deleted", id, ipAddress);
    }

    @Transactional(readOnly = true)
    public Optional<InvoicePolicy> findEffectivePolicy(
            Set<String> clientTypeCandidates,
            Set<String> appointmentStatusCandidates,
            Long serviceId,
            LocalDate billingDate) {
        if (clientTypeCandidates.isEmpty() || appointmentStatusCandidates.isEmpty()) {
            return Optional.empty();
        }
        LocalDate effectiveDate = billingDate != null
                ? billingDate
                : LocalDate.now(timezoneService.getPracticeTimezone());
        List<String> clientKeys = expandPolicyMatchKeys(clientTypeCandidates);
        List<String> appointmentKeys = expandPolicyMatchKeys(appointmentStatusCandidates);
        // Always consider wildcard "all" policies for client type and session status.
        if (!clientKeys.contains(ALL_SCOPE_KEY)) {
            clientKeys = withAllScope(clientKeys);
        }
        if (!appointmentKeys.contains(ALL_SCOPE_KEY)) {
            appointmentKeys = withAllScope(appointmentKeys);
        }
        return invoicePolicyRepository
                .findEnabledMatches(clientKeys, appointmentKeys, serviceId, effectiveDate)
                .stream()
                .findFirst();
    }

    @Transactional(readOnly = true)
    public InvoicePolicyRateResult resolveBillingRate(Session session, BigDecimal baseRate) {
        if (session == null || baseRate == null) {
            return InvoicePolicyRateResult.fromBaseRate(baseRate);
        }
        Long serviceId = session.getService() != null ? session.getService().getId() : null;
        java.time.ZoneId practiceZone = timezoneService.getPracticeTimezone();
        LocalDate billingDate = session.getSessionDate() != null
                ? session.getSessionDate().atZone(practiceZone).toLocalDate()
                : LocalDate.now(practiceZone);
        return resolveBillingRate(session, baseRate, serviceId, billingDate);
    }

    @Transactional(readOnly = true)
    public InvoicePolicyRateResult resolveBillingRate(
            Session session,
            BigDecimal baseRate,
            Long serviceId,
            LocalDate billingDate
    ) {
        if (session == null || baseRate == null) {
            return InvoicePolicyRateResult.fromBaseRate(baseRate);
        }

        Client client = session.getClient();
        if (client == null) {
            return InvoicePolicyRateResult.fromBaseRate(baseRate);
        }

        Set<String> clientTypeCandidates = buildClientTypeCandidates(client);
        Set<String> appointmentStatusCandidates = SessionAppointmentStatusMapper.toPolicyAppointmentStatusKeys(session.getStatus());
        LocalDate effectiveBillingDate = billingDate != null
                ? billingDate
                : LocalDate.now(timezoneService.getPracticeTimezone());

        Optional<InvoicePolicy> policyOpt = findEffectivePolicy(
                clientTypeCandidates, appointmentStatusCandidates, serviceId, effectiveBillingDate);
        if (policyOpt.isEmpty()) {
            return InvoicePolicyRateResult.fromBaseRate(baseRate);
        }

        InvoicePolicy policy = policyOpt.get();
        BigDecimal resolvedRate = calculatePolicyRate(policy, baseRate);
        return InvoicePolicyRateResult.builder()
                .ratePerUnit(resolvedRate)
                .invoicePolicyId(policy.getId())
                .policyApplied(true)
                .build();
    }

    public BigDecimal calculatePolicyRate(InvoicePolicy policy, BigDecimal baseRate) {
        Objects.requireNonNull(policy, "Policy is required");
        Objects.requireNonNull(baseRate, "Base rate is required");
        if (policy.getPriceType() == InvoicePolicyPriceType.FIXED) {
            return policy.getInvoicePrice().setScale(2, RoundingMode.HALF_UP);
        }
        return baseRate.multiply(policy.getInvoicePrice())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<SystemOptionResponse> getClientTypeOptions() {
        return withAllOption(resolveOptions(DEFAULT_CLIENT_TYPE_CATEGORY), "All client types");
    }

    @Transactional(readOnly = true)
    public List<SystemOptionResponse> getAppointmentStatusOptions() {
        return withAllOption(resolveOptions(APPOINTMENT_STATUS_CATEGORY), "All session statuses");
    }

    @Transactional(readOnly = true)
    public List<InvoicePolicyServiceOptionResponse> getServiceOptions() {
        List<InvoicePolicyServiceOptionResponse> options = new ArrayList<>();
        options.add(InvoicePolicyServiceOptionResponse.builder()
                .serviceId(null)
                .optionKey(ALL_SCOPE_KEY)
                .optionLabel("All services")
                .serviceCode(ALL_SCOPE_KEY)
                .allServices(true)
                .build());
        for (com.smart.therapy.flow.billing.entity.Service service : serviceRepository.findByIsActive(true)) {
            options.add(InvoicePolicyServiceOptionResponse.builder()
                    .serviceId(service.getId())
                    .optionKey(service.getServiceCode())
                    .optionLabel(service.getServiceName())
                    .serviceCode(service.getServiceCode())
                    .baseRate(service.getBaseRate())
                    .allServices(false)
                    .build());
        }
        return options;
    }

    public Set<String> buildClientTypeCandidates(String clientTypeName, String clientTypeValue) {
        Set<String> candidates = new LinkedHashSet<>();
        if (StringUtils.hasText(clientTypeName)) {
            candidates.add(clientTypeName);
        }
        if (StringUtils.hasText(clientTypeValue)) {
            candidates.add(clientTypeValue);
        }
        return candidates;
    }

    public Set<String> buildClientTypeCandidates(Client client) {
        Set<String> candidates = new LinkedHashSet<>();
        if (client == null || !StringUtils.hasText(client.getClientType())) {
            return candidates;
        }
        String clientType = client.getClientType().trim();
        candidates.add(clientType);
        candidates.add(normalizeKey(clientType));
        String resolvedKey = systemOptionResolverService.resolveOptionKey(DEFAULT_CLIENT_TYPE_CATEGORY, clientType);
        if (StringUtils.hasText(resolvedKey)) {
            candidates.add(resolvedKey);
            candidates.add(normalizeKey(resolvedKey));
        }
        resolveOptionLabel(DEFAULT_CLIENT_TYPE_CATEGORY, clientType)
                .ifPresent(label -> candidates.add(normalizeKey(label)));
        return candidates;
    }

    private Optional<String> resolveOptionLabel(String categoryKey, String inputKey) {
        try {
            String normalizedInput = normalizeKey(inputKey);
            return resolveOptions(categoryKey).stream()
                    .filter(option -> normalizeKey(option.getOptionKey()).equals(normalizedInput)
                            || normalizeKey(option.getOptionLabel()).equals(normalizedInput))
                    .map(SystemOptionResponse::getOptionLabel)
                    .findFirst();
        } catch (ResourceNotFoundException ex) {
            return Optional.empty();
        }
    }

    @Deprecated
    public Set<String> buildAppointmentStatusCandidates(String statusName, String statusValue, String statusDisplayName) {
        Set<String> candidates = new LinkedHashSet<>();
        if (StringUtils.hasText(statusName)) {
            candidates.add(statusName);
        }
        if (StringUtils.hasText(statusValue)) {
            candidates.add(statusValue);
        }
        if (StringUtils.hasText(statusDisplayName)) {
            candidates.add(statusDisplayName);
        }
        SessionStatus status = null;
        if (StringUtils.hasText(statusValue)) {
            try {
                status = SessionStatus.fromValue(statusValue);
            } catch (IllegalArgumentException ignored) {
                // keep legacy candidates only
            }
        }
        if (status != null) {
            candidates.addAll(SessionAppointmentStatusMapper.toPolicyAppointmentStatusKeys(status));
        }
        return candidates;
    }

    private InvoicePolicy requirePolicy(Long id) {
        return invoicePolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice policy not found"));
    }

    private List<SystemOptionResponse> resolveOptions(String categoryKey) {
        try {
            OptionCategoryResponse category = systemOptionResolverService.resolveCategoryWithOptions(categoryKey);
            return category != null && category.getOptions() != null ? category.getOptions() : new ArrayList<>();
        } catch (ResourceNotFoundException ex) {
            return new ArrayList<>();
        }
    }

    private void validateBusinessRules(InvoicePolicyRequest request, Long serviceId) {
        if (!StringUtils.hasText(request.getClientTypeKey()) || !StringUtils.hasText(request.getClientTypeLabel())) {
            throw new BadRequestException("Client type is required");
        }
        if (!StringUtils.hasText(request.getAppointmentStatusKey()) || !StringUtils.hasText(request.getAppointmentStatusLabel())) {
            throw new BadRequestException("Appointment status is required");
        }
        if (request.getInvoicePrice() == null || request.getInvoicePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Invoice price must be greater than or equal to 0");
        }
        if (request.getPriceType() == InvoicePolicyPriceType.PERCENTAGE
                && request.getInvoicePrice().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("Percentage invoice price cannot exceed 100");
        }
        if (request.getEffectiveFrom() != null && request.getEffectiveTo() != null
                && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
            throw new BadRequestException("Effective end date cannot be before effective start date");
        }
        if (serviceId != null && serviceRepository.findById(serviceId).isEmpty()) {
            throw new BadRequestException("Selected service was not found");
        }

        if (!isAllScopeKey(request.getClientTypeKey())) {
            validateOptionBelongsToCategory(
                    request.getClientTypeKey(),
                    DEFAULT_CLIENT_TYPE_CATEGORY,
                    request.getClientTypeLabel(),
                    "client type"
            );
        }
        if (!isAllScopeKey(request.getAppointmentStatusKey())) {
            validateOptionBelongsToCategory(
                    request.getAppointmentStatusKey(),
                    APPOINTMENT_STATUS_CATEGORY,
                    request.getAppointmentStatusLabel(),
                    "appointment status"
            );
        }
    }

    private Long resolveServiceId(InvoicePolicyRequest request) {
        if (isAllScopeKey(request.getServiceScopeKey())) {
            return null;
        }
        return request.getServiceId();
    }

    private void validateOptionBelongsToCategory(
            String inputKey,
            String categoryKey,
            String inputLabel,
            String fieldName
    ) {
        List<SystemOptionResponse> options = resolveOptions(categoryKey);
        String normalizedInputKey = normalizeKey(inputKey);
        boolean exists = options.stream().anyMatch(option ->
                normalizeKey(option.getOptionKey()).equals(normalizedInputKey)
                        || normalizeKey(option.getOptionLabel()).equals(normalizedInputKey)
                        || normalizeKey(option.getOptionLabel()).equals(normalizeKey(inputLabel))
        );
        if (!exists) {
            throw new BadRequestException("Invalid " + fieldName + " for category: " + categoryKey);
        }
    }

    private InvoicePolicyResponse toResponse(InvoicePolicy policy) {
        return InvoicePolicyResponse.builder()
                .id(policy.getId())
                .clientTypeKey(policy.getClientTypeKey())
                .clientTypeLabel(policy.getClientTypeLabel())
                .appointmentStatusKey(policy.getAppointmentStatusKey())
                .appointmentStatusLabel(policy.getAppointmentStatusLabel())
                .enabled(policy.getEnabled())
                .priceType(policy.getPriceType())
                .invoicePrice(policy.getInvoicePrice())
                .policyName(policy.getPolicyName())
                .serviceId(policy.getServiceId())
                .serviceScopeKey(policy.getServiceId() == null ? ALL_SCOPE_KEY : null)
                .effectiveFrom(policy.getEffectiveFrom())
                .effectiveTo(policy.getEffectiveTo())
                .priority(policy.getPriority())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }

    private void recordAuditEvent(AuthPrincipal requester, String action, Long resourceId, String ipAddress) {
        if (requester == null) {
            return;
        }
        try {
            Long actorId = currentUserService.requireCurrentUser(requester).getId();
            auditLogService.record(AuditEventDraft.of(action, RESOURCE_TYPE_INVOICE_POLICY)
                    .actorId(actorId)
                    .resourceId(resourceId)
                    .ipAddress(ipAddress)
                    .hipaaRelevant(false)
                    .riskLevel("medium"));
        } catch (Exception ignored) {
            // Audit must not block policy management
        }
    }

    private String normalizeKey(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        if (isAllScopeKey(value)) {
            return ALL_SCOPE_KEY;
        }
        return SystemOptionKeyMatcher.normalize(value);
    }

    public static boolean isAllScopeKey(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return ALL_SCOPE_KEY.equals(normalized)
                || "*".equals(normalized)
                || "any".equals(normalized);
    }

    private List<String> withAllScope(List<String> keys) {
        List<String> withAll = new ArrayList<>(keys);
        withAll.add(ALL_SCOPE_KEY);
        return List.copyOf(withAll);
    }

    private List<SystemOptionResponse> withAllOption(List<SystemOptionResponse> options, String label) {
        List<SystemOptionResponse> result = new ArrayList<>();
        result.add(SystemOptionResponse.builder()
                .optionKey(ALL_SCOPE_KEY)
                .optionLabel(label)
                .sortOrder(0)
                .isDefault(false)
                .isSystem(true)
                .isActive(true)
                .build());
        if (options != null) {
            result.addAll(options);
        }
        return result;
    }

    private List<String> expandPolicyMatchKeys(Collection<String> rawKeys) {
        Set<String> expanded = new LinkedHashSet<>();
        for (String key : rawKeys) {
            if (!StringUtils.hasText(key)) {
                continue;
            }
            if (isAllScopeKey(key)) {
                expanded.add(ALL_SCOPE_KEY);
                continue;
            }
            String trimmed = key.trim();
            String normalized = normalizeKey(trimmed);
            expanded.add(normalized);
            expanded.add(trimmed.toLowerCase(Locale.ROOT));
            expanded.add(normalized.replace('_', '-'));
            expanded.add(normalized.replace('-', '_'));
        }
        expanded.remove("");
        return List.copyOf(expanded);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
