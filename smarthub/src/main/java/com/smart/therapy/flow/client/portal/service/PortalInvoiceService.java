package com.smart.therapy.flow.client.portal.service;

import com.smart.therapy.flow.auth.entity.AuditLog;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.billing.entity.Payment;
import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.billing.enums.BillingStatus;
import com.smart.therapy.flow.billing.repository.PaymentRepository;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.billing.repository.SessionBillingRepository;
import com.smart.therapy.flow.billing.service.BillingService;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.billing.audit.BillingAuditActions;
import com.smart.therapy.flow.client.portal.dto.PortalInvoiceResponse;
import com.smart.therapy.flow.client.portal.dto.PortalInvoiceStatsResponse;
import com.smart.therapy.flow.client.portal.enums.PortalInvoicePaymentStatusFilter;
import com.smart.therapy.flow.client.portal.util.PortalInvoiceMapper;
import com.smart.therapy.flow.common.audit.HipaaAuditLabels;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.service.TimezoneService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortalInvoiceService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final SessionBillingRepository sessionBillingRepository;
    private final PaymentRepository paymentRepository;
    private final ServiceRepository serviceRepository;
    private final CurrentUserService currentUserService;
    private final TimezoneService timezoneService;
    private final BillingService billingService;
    private final AuditLogService auditLogService;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public PortalInvoiceStatsResponse getInvoiceStats(AuthPrincipal principal) {
        Client client = requireCurrentClient(principal);
        long totalInvoices = sessionBillingRepository.countByClientId(client.getId());
        BigDecimal totalBilled = sessionBillingRepository.sumAmountDueByClientId(client.getId());
        BigDecimal totalPaid = sessionBillingRepository.sumPaidAmountByClientId(client.getId());

        return PortalInvoiceStatsResponse.builder()
                .totalInvoices(totalInvoices)
                .totalBilled(totalBilled != null ? totalBilled : BigDecimal.ZERO)
                .totalPaid(totalPaid != null ? totalPaid : BigDecimal.ZERO)
                .build();
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<PortalInvoiceResponse> getInvoices(
            AuthPrincipal principal,
            int page,
            int pageSize,
            String paymentStatus,
            Boolean insuranceCovered,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            String ipAddress,
            String userAgent) {
        Client client = requireCurrentClient(principal);
        int safePage = Math.max(page, DEFAULT_PAGE);
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);

        PortalInvoicePaymentStatusFilter statusFilter = null;
        if (StringUtils.hasText(paymentStatus)) {
            statusFilter = PortalInvoicePaymentStatusFilter.from(paymentStatus);
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date.");
        }

        Specification<SessionBilling> specification = buildSpecification(
                client.getId(), statusFilter, insuranceCovered, startDate, endDate, search);
        Pageable pageable = PageRequest.of(
                safePage - 1,
                safePageSize,
                Sort.by(Sort.Direction.DESC, "billingDate").and(Sort.by(Sort.Direction.DESC, "createdAt")));

        Page<SessionBilling> results = sessionBillingRepository.findAll(specification, pageable);
        List<PortalInvoiceResponse> items = mapInvoices(results.getContent(), client);

        recordAudit(client, BillingAuditActions.INVOICES_VIEWED, ipAddress, userAgent,
                "Portal invoice list - page: " + safePage + ", count: " + items.size());

        return PaginatedResponse.of(items, results.getTotalElements(), safePage, safePageSize);
    }

    @Transactional(readOnly = true)
    public byte[] downloadInvoiceReceipt(AuthPrincipal principal, Long invoiceId, String ipAddress, String userAgent) {
        Objects.requireNonNull(invoiceId, "Invoice ID is required");
        Client client = requireCurrentClient(principal);
        SessionBilling billing = sessionBillingRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found or access denied"));
        verifyClientOwnership(billing, client.getId());
        recordAudit(client, BillingAuditActions.INVOICE_VIEWED, ipAddress, userAgent,
                "Portal invoice receipt download - invoiceId: " + invoiceId);

        String paymentStatus = PortalInvoiceMapper.derivePaymentStatus(
                billing,
                billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO,
                PortalInvoiceMapper.calculateAmountDue(billing));
        if (!"paid".equals(paymentStatus) && !"partial".equals(paymentStatus)) {
            throw new BadRequestException("Receipt is only available for paid or partially paid invoices.");
        }

        byte[] pdf = billingService.getInvoicePdfForClientPortal(invoiceId, client.getId());
        recordAudit(client, "invoice_receipt_downloaded", ipAddress, userAgent,
                "Portal invoice receipt download - invoiceId: " + invoiceId);
        return pdf;
    }

    @Transactional(readOnly = true)
    public String downloadInvoiceReceiptHtml(AuthPrincipal principal, Long invoiceId, String ipAddress, String userAgent) {
        Objects.requireNonNull(invoiceId, "Invoice ID is required");
        Client client = requireCurrentClient(principal);
        SessionBilling billing = sessionBillingRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found or access denied"));
        verifyClientOwnership(billing, client.getId());
        recordAudit(client, BillingAuditActions.INVOICE_VIEWED, ipAddress, userAgent,
                "Portal invoice receipt HTML - invoiceId: " + invoiceId);

        String paymentStatus = PortalInvoiceMapper.derivePaymentStatus(
                billing,
                billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO,
                PortalInvoiceMapper.calculateAmountDue(billing));
        if (!"paid".equals(paymentStatus) && !"partial".equals(paymentStatus)) {
            throw new BadRequestException("Receipt is only available for paid or partially paid invoices.");
        }

        String html = billingService.getInvoiceHtmlForClientPortal(invoiceId, client.getId());
        recordAudit(client, "invoice_receipt_downloaded", ipAddress, userAgent,
                "Portal invoice receipt HTML - invoiceId: " + invoiceId);
        return html;
    }

    private List<PortalInvoiceResponse> mapInvoices(List<SessionBilling> billingRecords, Client client) {
        if (billingRecords.isEmpty()) {
            return List.of();
        }

        Map<String, com.smart.therapy.flow.billing.entity.Service> serviceByCode = serviceRepository.findAll().stream()
                .collect(Collectors.toMap(
                        com.smart.therapy.flow.billing.entity.Service::getServiceCode,
                        s -> s,
                        (left, right) -> left));

        List<Long> billingIds = billingRecords.stream().map(SessionBilling::getId).toList();
        Map<Long, List<Payment>> paymentsByBillingId = paymentRepository.findBySessionBillingIdIn(billingIds).stream()
                .collect(Collectors.groupingBy(p -> p.getSessionBilling().getId()));

        ZoneId displayZone = timezoneService.resolveClientPortalZone(client.getId(), null);

        return billingRecords.stream()
                .map(billing -> PortalInvoiceMapper.toResponse(
                        billing,
                        serviceByCode,
                        PortalInvoiceMapper.resolveLatestPayment(paymentsByBillingId.get(billing.getId())),
                        displayZone))
                .toList();
    }

    private Specification<SessionBilling> buildSpecification(
            Long clientId,
            PortalInvoicePaymentStatusFilter paymentStatusFilter,
            Boolean insuranceCovered,
            LocalDate startDate,
            LocalDate endDate,
            String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("session").get("client").get("id"), clientId));

            if (insuranceCovered != null) {
                predicates.add(cb.equal(root.get("insuranceCovered"), insuranceCovered));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("billingDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("billingDate"), endDate));
            }
            if (StringUtils.hasText(search)) {
                String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                Join<Object, Object> sessionJoin = root.join("session", JoinType.LEFT);
                Join<Object, Object> serviceJoin = sessionJoin.join("service", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("serviceCode")), like),
                        cb.like(cb.lower(serviceJoin.get("serviceName")), like)));
            }
            if (paymentStatusFilter != null) {
                predicates.add(paymentStatusPredicate(root, cb, paymentStatusFilter));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Predicate paymentStatusPredicate(
            jakarta.persistence.criteria.Root<SessionBilling> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            PortalInvoicePaymentStatusFilter filter) {
        // amountDue ≈ totalAmount - COALESCE(discountAmount, 0)
        var discount = cb.coalesce(root.get("discountAmount"), BigDecimal.ZERO);
        var amountDue = cb.diff(root.get("totalAmount"), discount);
        var paid = cb.coalesce(root.get("paidAmount"), BigDecimal.ZERO);

        return switch (filter) {
            case PAID -> cb.or(
                    cb.equal(root.get("billingStatus"), BillingStatus.PAID),
                    cb.and(
                            cb.greaterThan(amountDue, BigDecimal.ZERO),
                            cb.greaterThanOrEqualTo(paid, amountDue)));
            case PARTIAL -> cb.and(
                    cb.greaterThan(paid, BigDecimal.ZERO),
                    cb.lessThan(paid, amountDue));
            case DENIED -> cb.equal(root.get("billingStatus"), BillingStatus.DENIED);
            case CANCELLED -> cb.equal(root.get("billingStatus"), BillingStatus.CANCELLED);
            case UNPAID -> cb.and(
                    cb.or(
                            cb.isNull(root.get("paidAmount")),
                            cb.equal(root.get("paidAmount"), BigDecimal.ZERO)),
                    cb.not(root.get("billingStatus").in(
                            BillingStatus.PAID, BillingStatus.DENIED, BillingStatus.CANCELLED)));
        };
    }

    private Client requireCurrentClient(AuthPrincipal principal) {
        return currentUserService.requireCurrentClient(principal);
    }

    private void verifyClientOwnership(SessionBilling billing, Long clientId) {
        if (billing.getSession() == null
                || billing.getSession().getClient() == null
                || !clientId.equals(billing.getSession().getClient().getId())) {
            throw new ResourceNotFoundException("Invoice not found or access denied");
        }
    }

    private void recordAudit(Client client, String action, String ipAddress, String userAgent, String details) {
        if (client == null || client.getId() == null) {
            return;
        }
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .result("success")
                    .resourceType("billing")
                    .username(HipaaAuditLabels.clientActor(client))
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .hipaaRelevant(true)
                    .riskLevel("medium")
                    .timestamp(Instant.now())
                    .details(details)
                    .build();
            clientRepository.findById(client.getId()).ifPresent(auditLog::setClient);
            auditLogService.write(auditLog);
        } catch (Exception e) {
            log.warn("Failed to record audit event for {}: {}", action, e.getMessage());
        }
    }
}
