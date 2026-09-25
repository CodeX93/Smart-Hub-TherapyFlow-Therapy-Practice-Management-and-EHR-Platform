package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.notification.entity.Notification;
import com.smart.therapy.flow.notification.enums.NotificationCategory;
import com.smart.therapy.flow.notification.enums.NotificationPriority;
import com.smart.therapy.flow.notification.enums.NotificationType;
import com.smart.therapy.flow.notification.repository.NotificationRepository;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.organisation.service.TenantSchemaHealthService;
import com.smart.therapy.flow.payment.service.StripeRefundService;
import com.smart.therapy.flow.subscription.entity.BillingContact;
import com.smart.therapy.flow.subscription.entity.BillingExportJob;
import com.smart.therapy.flow.subscription.entity.BillingNotificationLog;
import com.smart.therapy.flow.subscription.entity.BillingNotificationTemplate;
import com.smart.therapy.flow.subscription.entity.Invoice;
import com.smart.therapy.flow.subscription.entity.InvoiceAdjustment;
import com.smart.therapy.flow.subscription.entity.InvoiceDispute;
import com.smart.therapy.flow.subscription.entity.RefundRetryTask;
import com.smart.therapy.flow.subscription.enums.InvoiceAdjustmentStatus;
import com.smart.therapy.flow.subscription.enums.InvoiceAdjustmentType;
import com.smart.therapy.flow.subscription.enums.InvoiceDisputeStatus;
import com.smart.therapy.flow.subscription.enums.InvoiceStatus;
import com.smart.therapy.flow.subscription.enums.RefundRetryStatus;
import com.smart.therapy.flow.subscription.enums.BillingExportType;
import com.smart.therapy.flow.subscription.repository.BillingContactRepository;
import com.smart.therapy.flow.subscription.repository.BillingExportJobRepository;
import com.smart.therapy.flow.subscription.repository.BillingNotificationLogRepository;
import com.smart.therapy.flow.subscription.repository.BillingNotificationTemplateRepository;
import com.smart.therapy.flow.subscription.repository.InvoiceAdjustmentRepository;
import com.smart.therapy.flow.subscription.repository.InvoiceDisputeRepository;
import com.smart.therapy.flow.subscription.repository.InvoiceRepository;
import com.smart.therapy.flow.subscription.repository.OrgSubscriptionRepository;
import com.smart.therapy.flow.subscription.repository.RefundRetryTaskRepository;
import com.smart.therapy.flow.subscription.service.BillingExportService;
import com.smart.therapy.flow.subscription.service.BillingNotificationService;
import com.smart.therapy.flow.superadmin.dto.SuperAdminRevenueByPlanResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminRevenueOverviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SuperAdminBillingService {

    private final OrganisationRepository organisationRepository;
    private final BillingContactRepository billingContactRepository;
    private final BillingNotificationTemplateRepository billingNotificationTemplateRepository;
    private final BillingNotificationLogRepository billingNotificationLogRepository;
    private final BillingNotificationService billingNotificationService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceAdjustmentRepository invoiceAdjustmentRepository;
    private final InvoiceDisputeRepository invoiceDisputeRepository;
    private final OrgSubscriptionRepository orgSubscriptionRepository;
    private final RefundRetryTaskRepository refundRetryTaskRepository;
    private final BillingExportJobRepository billingExportJobRepository;
    private final BillingExportService billingExportService;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final PlatformAuditService platformAuditService;
    private final StripeRefundService stripeRefundService;
    private final AuthIdentityRoleRepository authIdentityRoleRepository;
    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final TenantSchemaHealthService tenantSchemaHealthService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public boolean organisationExists(Long organisationId) {
        return organisationRepository.existsById(organisationId);
    }

    @Transactional(readOnly = true)
    public List<BillingContact> listBillingContacts(Long organisationId) {
        return billingContactRepository.findByOrganisation_IdAndIsActiveTrueOrderByIsPrimaryDescCreatedAtAsc(organisationId);
    }

    @Transactional
    public UpsertBillingContactResult upsertBillingContact(Long organisationId,
                                                           String emailInput,
                                                           String fullName,
                                                           Boolean primary,
                                                           Boolean active,
                                                           Long actorAuthId) {
        Organisation org = organisationRepository.findById(organisationId).orElse(null);
        if (org == null) {
            return UpsertBillingContactResult.notFoundResult();
        }
        if (emailInput == null || emailInput.isBlank()) {
            return UpsertBillingContactResult.error("email is required");
        }
        String email = emailInput.trim().toLowerCase(Locale.ROOT);

        BillingContact existing = billingContactRepository
                .findByOrganisation_IdAndIsActiveTrueOrderByIsPrimaryDescCreatedAtAsc(organisationId)
                .stream()
                .filter(c -> email.equalsIgnoreCase(c.getEmail()))
                .findFirst()
                .orElse(null);

        BillingContact contact = existing != null ? existing : new BillingContact();
        contact.setOrganisation(org);
        contact.setEmail(email);
        contact.setFullName(fullName);
        if (primary != null) {
            contact.setIsPrimary(primary);
        }
        if (active != null) {
            contact.setIsActive(active);
        }
        contact = billingNotificationService.saveContact(contact);
        if (Boolean.TRUE.equals(contact.getIsPrimary())) {
            BillingContact savedContact = contact;
            billingContactRepository
                    .findByOrganisation_IdAndIsActiveTrueOrderByIsPrimaryDescCreatedAtAsc(organisationId)
                    .stream()
                    .filter(c -> !savedContact.getId().equals(c.getId()))
                    .filter(c -> Boolean.TRUE.equals(c.getIsPrimary()))
                    .forEach(c -> {
                        c.setIsPrimary(false);
                        c.setUpdatedAt(Instant.now());
                        billingContactRepository.save(c);
                    });
        }

        platformAuditService.log(
                actorAuthId,
                "BILLING_CONTACT_UPSERTED",
                "Organisation",
                String.valueOf(organisationId),
                "email=" + contact.getEmail() + ", primary=" + contact.getIsPrimary() + ", active=" + contact.getIsActive()
        );
        return UpsertBillingContactResult.success(contact, existing == null);
    }

    @Transactional
    public RemoveBillingContactResult removeBillingContact(Long organisationId, Long contactId, Long actorAuthId) {
        if (!organisationRepository.existsById(organisationId)) {
            return RemoveBillingContactResult.orgNotFoundResult();
        }
        BillingContact contact = billingContactRepository.findById(contactId).orElse(null);
        if (contact == null || !contact.getOrganisation().getId().equals(organisationId)) {
            return RemoveBillingContactResult.contactNotFoundResult();
        }
        billingNotificationService.removeContact(contactId);
        platformAuditService.log(
                actorAuthId,
                "BILLING_CONTACT_REMOVED",
                "Organisation",
                String.valueOf(organisationId),
                "contactId=" + contactId + ", email=" + contact.getEmail()
        );
        return RemoveBillingContactResult.successResult();
    }

    @Transactional(readOnly = true)
    public List<BillingNotificationTemplate> listTemplates() {
        return billingNotificationTemplateRepository.findByIsActiveTrueOrderByEventKeyAsc();
    }

    @Transactional
    public UpsertTemplateResult upsertTemplate(String eventKey,
                                               String subjectTemplate,
                                               String bodyTemplate,
                                               Boolean active,
                                               Long actorAuthId) {
        if (subjectTemplate == null || subjectTemplate.isBlank()) {
            return UpsertTemplateResult.error("subjectTemplate is required");
        }
        if (bodyTemplate == null || bodyTemplate.isBlank()) {
            return UpsertTemplateResult.error("bodyTemplate is required");
        }
        BillingNotificationTemplate template = billingNotificationService.upsertTemplate(
                eventKey.trim().toUpperCase(Locale.ROOT),
                subjectTemplate,
                bodyTemplate,
                active
        );
        platformAuditService.log(
                actorAuthId,
                "BILLING_TEMPLATE_UPSERTED",
                "BillingNotificationTemplate",
                template.getEventKey(),
                "active=" + template.getIsActive()
        );
        return UpsertTemplateResult.success(template);
    }

    @Transactional(readOnly = true)
    public ListBillingLogsResult listLogs(Long organisationId) {
        if (!organisationRepository.existsById(organisationId)) {
            return ListBillingLogsResult.notFoundResult();
        }
        return ListBillingLogsResult.success(
                billingNotificationLogRepository.findTop200ByOrganisation_IdOrderByCreatedAtDesc(organisationId)
        );
    }

    @Transactional(readOnly = true)
    public Page<Invoice> listInvoices(Long organisationId, String status, String sort, int page, int pageSize) {
        InvoiceStatus statusFilter = parseInvoiceStatus(status);
        Sort sorting;
        if (sort == null || sort.isBlank()) {
            sorting = InvoiceStatus.PAST_DUE.equals(statusFilter)
                    ? Sort.by(Sort.Direction.ASC, "dueDate")
                    : Sort.by(Sort.Direction.DESC, "createdAt");
        } else {
            sorting = switch (sort.trim().toLowerCase(Locale.ROOT)) {
                case "duedate_asc" -> Sort.by(Sort.Direction.ASC, "dueDate");
                case "duedate_desc" -> Sort.by(Sort.Direction.DESC, "dueDate");
                case "createdat_asc" -> Sort.by(Sort.Direction.ASC, "createdAt");
                case "createdat_desc" -> Sort.by(Sort.Direction.DESC, "createdAt");
                default -> Sort.by(Sort.Direction.ASC, "dueDate");
            };
        }
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(pageSize, 1), 200), sorting);
        return invoiceRepository.search(organisationId, statusFilter, pageable);
    }

    @Transactional(readOnly = true)
    public List<Invoice> listInvoicesForExport(Long organisationId, String status) {
        InvoiceStatus statusFilter = parseInvoiceStatus(status);
        return invoiceRepository.search(
                organisationId,
                statusFilter,
                PageRequest.of(0, 5000, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
    }

    @Transactional(readOnly = true)
    public Invoice getInvoiceDetailed(Long invoiceId) {
        return invoiceRepository.findDetailedById(invoiceId)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found"));
    }

    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Long invoiceId) {
        Invoice invoice = getInvoiceDetailed(invoiceId);
        try {
            return com.smart.therapy.flow.report.util.HtmlToPdfConverter.toPdfBytes(buildInvoiceHtml(invoice));
        } catch (RuntimeException ex) {
            throw new StoryApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVOICE_PDF_FAILED",
                    "Failed to generate invoice PDF");
        }
    }

    /**
     * Sends payment reminder email to org admins + billing contacts, and creates in-app
     * notifications for tenant admin users.
     */
    @Transactional
    public InvoiceReminderResult sendInvoicePaymentReminder(Long invoiceId, Long actorAuthId) {
        Invoice invoice = getInvoiceDetailed(invoiceId);
        InvoiceStatus status = invoice.getStatus();
        if (InvoiceStatus.PAID.equals(status) || InvoiceStatus.VOID.equals(status)) {
            throw new StoryApiException(HttpStatus.CONFLICT, "INVOICE_NOT_REMINDABLE",
                    "Cannot send reminder for " + status + " invoices");
        }

        Organisation organisation = invoice.getSubscription().getOrganisation();
        Long organisationId = organisation.getId();

        if (billingNotificationService.wasEventSentRecently(
                organisationId,
                BillingNotificationService.EVENT_SUBSCRIPTION_INVOICE_PAYMENT_REMINDER,
                Duration.ofMinutes(15))) {
            throw new StoryApiException(HttpStatus.TOO_MANY_REQUESTS, "REMINDER_RATE_LIMITED",
                    "A payment reminder was already sent recently. Try again in a few minutes.");
        }

        BillingNotificationService.EmailDeliveryResult emailResult =
                billingNotificationService.notifySubscriptionInvoicePaymentReminder(invoice);

        int inAppCount = createAdminInAppReminders(invoice, organisation);

        platformAuditService.log(
                actorAuthId,
                "INVOICE_PAYMENT_REMINDER_SENT",
                "Invoice",
                String.valueOf(invoiceId),
                "emailsSent=" + emailResult.sent()
                        + ", emailsFailed=" + emailResult.failed()
                        + ", inApp=" + inAppCount
                        + ", organisationId=" + organisationId
        );

        return new InvoiceReminderResult(
                invoiceId,
                organisationId,
                emailResult.sent(),
                emailResult.failed(),
                inAppCount,
                emailResult.recipients(),
                emailResult.warning()
        );
    }

    private int createAdminInAppReminders(Invoice invoice, Organisation organisation) {
        String schema = organisation.getSchemaName();
        if (schema == null || schema.isBlank() || "public".equalsIgnoreCase(schema)
                || !tenantSchemaHealthService.schemaExists(schema)) {
            return 0;
        }

        List<AuthIdentity> admins = authIdentityRoleRepository.findActiveAdminsByOrganisationId(organisation.getId());
        if (admins.isEmpty()) {
            return 0;
        }

        String amount = invoice.getAmount() != null ? invoice.getAmount().toPlainString() : "0";
        String dueDate = invoice.getDueDate() != null ? invoice.getDueDate().toString() : "-";
        String title = "Subscription payment reminder";
        String message = "Please pay invoice #" + invoice.getId()
                + " (" + amount + " USD). Due date: " + dueDate + ".";
        NotificationType type = InvoiceStatus.PAST_DUE.equals(invoice.getStatus())
                ? NotificationType.PAYMENT_OVERDUE
                : NotificationType.PAYMENT_DUE;

        return tenantTransactionExecutor.executeWrite(organisation.getId(), schema, () -> {
            int created = 0;
            for (AuthIdentity admin : admins) {
                var userOpt = userRepository.findByAuthId(admin.getId());
                if (userOpt.isEmpty()) {
                    continue;
                }
                Notification notification = Notification.builder()
                        .user(userOpt.get())
                        .type(type)
                        .category(NotificationCategory.BILLING)
                        .title(title)
                        .message(message)
                        .priority(NotificationPriority.HIGH)
                        .isRead(false)
                        .relatedEntityType("subscription_invoice")
                        .relatedEntityId(invoice.getId())
                        .actionUrl("/billing/subscription")
                        .actionLabel("Pay invoice")
                        .data("{\"invoiceId\":" + invoice.getId()
                                + ",\"organisationId\":" + organisation.getId() + "}")
                        .emailSent(false)
                        .build();
                notificationRepository.save(notification);
                created++;
            }
            return created;
        });
    }

    private String buildInvoiceHtml(Invoice invoice) {
        var subscription = invoice.getSubscription();
        var organisation = subscription != null ? subscription.getOrganisation() : null;
        var plan = subscription != null ? subscription.getPlan() : null;

        String orgName = organisation != null && organisation.getName() != null ? organisation.getName() : "Organisation";
        String planName = plan != null && plan.getName() != null ? plan.getName() : "Subscription";
        String planCode = plan != null && plan.getCode() != null ? plan.getCode() : "-";
        String cycle = subscription != null && subscription.getBillingCycleAtTime() != null
                ? subscription.getBillingCycleAtTime() : "-";
        String status = invoice.getStatus() != null ? invoice.getStatus().name() : "-";
        String amount = formatMoney(invoice.getAmount());
        String outstanding = formatMoney(invoice.getOutstandingBalance());
        String paid = formatMoney(invoice.getTotalPaid());
        String refunded = formatMoney(invoice.getRefundedAmount());
        String dueDate = invoice.getDueDate() != null ? invoice.getDueDate().toString() : "-";
        String periodStart = invoice.getBillingPeriodStart() != null ? invoice.getBillingPeriodStart().toString() : "-";
        String periodEnd = invoice.getBillingPeriodEnd() != null ? invoice.getBillingPeriodEnd().toString() : "-";
        String paidAt = invoice.getPaidAt() != null ? invoice.getPaidAt().toString() : "-";
        String createdAt = invoice.getCreatedAt() != null ? invoice.getCreatedAt().toString() : "-";
        String providerInvoiceId = blankToDash(invoice.getProviderInvoiceId());

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8"/>
                  <style>
                    body { font-family: Arial, Helvetica, sans-serif; color: #1f2937; margin: 32px; font-size: 12px; }
                    h1 { font-size: 22px; margin: 0 0 4px 0; color: #111827; }
                    .muted { color: #6b7280; margin-bottom: 24px; }
                    .header { display: flex; justify-content: space-between; margin-bottom: 28px; }
                    .badge { display: inline-block; padding: 4px 10px; border-radius: 999px; background: #eef2ff; color: #3730a3; font-weight: 600; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 16px; }
                    th, td { text-align: left; padding: 10px 8px; border-bottom: 1px solid #e5e7eb; vertical-align: top; }
                    th { width: 34%%; color: #6b7280; font-weight: 600; }
                    .amount { font-size: 20px; font-weight: 700; color: #111827; }
                    .footer { margin-top: 36px; color: #9ca3af; font-size: 11px; }
                  </style>
                </head>
                <body>
                  <div class="header">
                    <div>
                      <h1>TherapyFlow Invoice</h1>
                      <div class="muted">Subscription invoice #%s</div>
                    </div>
                    <div><span class="badge">%s</span></div>
                  </div>
                  <div class="amount">%s USD</div>
                  <table>
                    <tr><th>Organisation</th><td>%s</td></tr>
                    <tr><th>Plan</th><td>%s (%s)</td></tr>
                    <tr><th>Billing cycle</th><td>%s</td></tr>
                    <tr><th>Subscription ID</th><td>%s</td></tr>
                    <tr><th>Billing period</th><td>%s → %s</td></tr>
                    <tr><th>Due date</th><td>%s</td></tr>
                    <tr><th>Paid at</th><td>%s</td></tr>
                    <tr><th>Created at</th><td>%s</td></tr>
                    <tr><th>Total paid</th><td>%s</td></tr>
                    <tr><th>Outstanding</th><td>%s</td></tr>
                    <tr><th>Refunded</th><td>%s</td></tr>
                    <tr><th>Provider invoice</th><td>%s</td></tr>
                  </table>
                  <div class="footer">Generated by TherapyFlow super-admin billing.</div>
                </body>
                </html>
                """.formatted(
                escapeHtml(String.valueOf(invoice.getId())),
                escapeHtml(status),
                escapeHtml(amount),
                escapeHtml(orgName),
                escapeHtml(planName),
                escapeHtml(planCode),
                escapeHtml(cycle),
                escapeHtml(subscription != null ? String.valueOf(subscription.getId()) : "-"),
                escapeHtml(periodStart),
                escapeHtml(periodEnd),
                escapeHtml(dueDate),
                escapeHtml(paidAt),
                escapeHtml(createdAt),
                escapeHtml(paid),
                escapeHtml(outstanding),
                escapeHtml(refunded),
                escapeHtml(providerInvoiceId)
        );
    }

    private static String formatMoney(BigDecimal value) {
        BigDecimal safe = value != null ? value : BigDecimal.ZERO;
        return safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private static String escapeHtml(String value) {
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

    @Transactional(readOnly = true)
    public ListInvoiceAdjustmentsResult listInvoiceAdjustments(Long invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            return ListInvoiceAdjustmentsResult.notFoundResult();
        }
        return ListInvoiceAdjustmentsResult.success(invoiceAdjustmentRepository.findByInvoice_IdOrderByCreatedAtDesc(invoiceId));
    }

    @Transactional
    public InvoiceAdjustment applyCredit(Long invoiceId,
                                         BigDecimal amountUsd,
                                         String reason,
                                         Long actorAuthId) {
        Invoice invoice = loadInvoiceOrThrow(invoiceId);
        Map<String, Object> before = invoiceSnapshot(invoice);
        assertMutationAllowed(invoice);
        validateReason(reason);
        if (amountUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "amountUsd must be > 0");
        }
        BigDecimal outstanding = normalizeOutstanding(invoice);
        if (amountUsd.compareTo(outstanding) > 0) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CREDIT_EXCEEDS_BALANCE",
                    "Credit amount exceeds outstanding balance");
        }

        invoice.setOutstandingBalance(outstanding.subtract(amountUsd));
        invoiceRepository.save(invoice);
        Map<String, Object> after = invoiceSnapshot(invoice);

        InvoiceAdjustment adjustment = new InvoiceAdjustment();
        adjustment.setInvoice(invoice);
        adjustment.setAdjustmentType(InvoiceAdjustmentType.CREDIT);
        adjustment.setAmount(amountUsd);
        adjustment.setReason(reason);
        adjustment.setStatus(InvoiceAdjustmentStatus.APPLIED);
        adjustment.setCreatedAt(Instant.now());
        adjustment.setUpdatedAt(Instant.now());
        adjustment.setCreatedBy(actorAuthId);
        adjustment = invoiceAdjustmentRepository.save(adjustment);

        platformAuditService.logWithSnapshots(actorAuthId, "INVOICE_CREDIT_APPLIED", "Invoice", String.valueOf(invoiceId),
                before, after, "adjustmentId=" + adjustment.getId() + ", amountUsd=" + amountUsd);
        return adjustment;
    }

    @Transactional
    public InvoiceAdjustment applyRefund(Long invoiceId,
                                         BigDecimal amountUsd,
                                         String reason,
                                         Long actorAuthId) {
        Invoice invoice = loadInvoiceOrThrow(invoiceId);
        Map<String, Object> before = invoiceSnapshot(invoice);
        assertMutationAllowed(invoice);
        validateReason(reason);
        if (amountUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "amountUsd must be > 0");
        }
        BigDecimal totalPaid = invoice.getTotalPaid() != null ? invoice.getTotalPaid() : BigDecimal.ZERO;
        if (amountUsd.compareTo(totalPaid) > 0) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REFUND_EXCEEDS_PAID",
                    "Refund amount exceeds total paid amount");
        }

        try {
            String providerRefundId = stripeRefundService.createRefund(invoice, amountUsd, reason);
            InvoiceAdjustment adjustment = new InvoiceAdjustment();
            adjustment.setInvoice(invoice);
            adjustment.setAdjustmentType(InvoiceAdjustmentType.REFUND);
            adjustment.setAmount(amountUsd);
            adjustment.setReason(reason);
            adjustment.setStatus(InvoiceAdjustmentStatus.SUBMITTED);
            adjustment.setProviderRefId(providerRefundId);
            adjustment.setCreatedAt(Instant.now());
            adjustment.setUpdatedAt(Instant.now());
            adjustment.setCreatedBy(actorAuthId);
            adjustment = invoiceAdjustmentRepository.save(adjustment);

            Map<String, Object> after = invoiceSnapshot(invoiceRepository.findById(invoiceId).orElse(invoice));
            platformAuditService.logWithSnapshots(actorAuthId, "INVOICE_REFUND_SUBMITTED", "Invoice", String.valueOf(invoiceId),
                    before, after, "adjustmentId=" + adjustment.getId() + ", providerRefundId=" + providerRefundId + ", amountUsd=" + amountUsd);
            return adjustment;
        } catch (StoryApiException ex) {
            if (!"STRIPE_UNAVAILABLE".equals(ex.getCode())) {
                throw ex;
            }
            queueRefundRetry(invoice, amountUsd, reason, actorAuthId, ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public ListInvoiceDisputesResult listInvoiceDisputes(Long invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            return ListInvoiceDisputesResult.notFoundResult();
        }
        return ListInvoiceDisputesResult.success(invoiceDisputeRepository.findByInvoice_IdOrderByOpenedAtDesc(invoiceId));
    }

    @Transactional
    public DisputeActionResult openDispute(Long invoiceId,
                                           String externalCaseId,
                                           BigDecimal amountUsd,
                                           String reason,
                                           String status,
                                           Long actorAuthId) {
        Invoice invoice = loadInvoiceOrThrow(invoiceId);
        Map<String, Object> before = invoiceSnapshot(invoice);
        validateReason(reason);
        if (amountUsd == null || amountUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "amountUsd must be > 0");
        }
        InvoiceDispute dispute = new InvoiceDispute();
        dispute.setInvoice(invoice);
        dispute.setExternalCaseId(externalCaseId);
        dispute.setReason(reason);
        dispute.setAmountUsd(amountUsd);
        dispute.setStatus(parseDisputeStatus(status));
        dispute.setOpenedAt(Instant.now());
        dispute.setResolvedAt(null);
        dispute.setCreatedAt(Instant.now());
        dispute.setUpdatedAt(Instant.now());
        dispute.setCreatedBy(actorAuthId);
        dispute = invoiceDisputeRepository.save(dispute);

        Map<String, Object> after = invoiceSnapshot(invoiceRepository.findById(invoiceId).orElse(invoice));
        platformAuditService.logWithSnapshots(
                actorAuthId,
                "INVOICE_DISPUTE_OPENED",
                "Invoice",
                String.valueOf(invoiceId),
                before,
                after,
                "disputeId=" + dispute.getId() + ", status=" + dispute.getStatus()
        );
        return DisputeActionResult.success(dispute, HttpStatus.CREATED.value());
    }

    @Transactional
    public DisputeActionResult updateDispute(Long invoiceId,
                                             Long disputeId,
                                             String externalCaseId,
                                             BigDecimal amountUsd,
                                             String reason,
                                             String status,
                                             Long actorAuthId) {
        InvoiceDispute dispute = invoiceDisputeRepository.findById(disputeId).orElse(null);
        if (dispute == null || !dispute.getInvoice().getId().equals(invoiceId)) {
            return DisputeActionResult.notFoundWithMessage("Dispute not found");
        }
        Map<String, Object> before = invoiceSnapshot(dispute.getInvoice());
        if (status != null && !status.isBlank()) {
            InvoiceDisputeStatus s = parseDisputeStatus(status);
            dispute.setStatus(s);
            if (InvoiceDisputeStatus.RESOLVED.equals(s)) {
                dispute.setResolvedAt(Instant.now());
            }
        }
        if (reason != null) {
            validateReason(reason);
            dispute.setReason(reason);
        }
        if (amountUsd != null) {
            if (amountUsd.compareTo(BigDecimal.ZERO) <= 0) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "amountUsd must be > 0");
            }
            dispute.setAmountUsd(amountUsd);
        }
        if (externalCaseId != null) {
            dispute.setExternalCaseId(externalCaseId);
        }
        dispute.setUpdatedAt(Instant.now());
        dispute = invoiceDisputeRepository.save(dispute);

        Map<String, Object> after = invoiceSnapshot(dispute.getInvoice());
        platformAuditService.logWithSnapshots(
                actorAuthId,
                "INVOICE_DISPUTE_UPDATED",
                "Invoice",
                String.valueOf(invoiceId),
                before,
                after,
                "disputeId=" + dispute.getId() + ", status=" + dispute.getStatus()
        );
        return DisputeActionResult.success(dispute, HttpStatus.OK.value());
    }

    @Transactional(readOnly = true)
    public RevenueReportResult getRevenueReport(String period, String groupBy) {
        YearMonth month = parsePeriod(period);
        RevenueGroupBy grouping = parseRevenueGroupBy(groupBy);
        LocalDate monthStartDate = month.atDay(1);
        Instant fromAt = monthStartDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toAtExclusive = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Invoice> invoices = invoiceRepository.findByCreatedRange(fromAt, toAtExclusive);
        List<Invoice> paidInvoices = invoices.stream()
                .filter(inv -> InvoiceStatus.PAID.equals(inv.getStatus()))
                .toList();

        BigDecimal mrr = paidInvoices.stream()
                .map(inv -> inv.getAmount() != null ? inv.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal arr = mrr.multiply(BigDecimal.valueOf(12));

        long activeStart = orgSubscriptionRepository.countActiveAt(fromAt);
        long endedThisMonth = orgSubscriptionRepository.countEndedBetween(fromAt, toAtExclusive);
        BigDecimal churnRate = activeStart > 0
                ? BigDecimal.valueOf((endedThisMonth * 100.0) / activeStart).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        Map<String, MovementAccumulator> movementMap = new LinkedHashMap<>();
        for (Invoice invoice : paidInvoices) {
            String key = resolveMovementKey(invoice, grouping);
            MovementAccumulator movement = movementMap.computeIfAbsent(key, k -> new MovementAccumulator());
            movement.amount = movement.amount.add(invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO);
            movement.paidInvoices++;
        }

        List<RevenueMovementRow> movements = movementMap.entrySet().stream()
                .map(e -> new RevenueMovementRow(e.getKey(), e.getValue().amount, e.getValue().paidInvoices))
                .sorted(Comparator.comparing(RevenueMovementRow::amount).reversed())
                .toList();

        return new RevenueReportResult(month.toString(), grouping.apiValue, mrr, arr, churnRate, movements);
    }

    @Transactional(readOnly = true)
    public RevenueAnalyticsResult getRevenueAnalytics(Integer months) {
        int window = Math.max(1, Math.min(months != null ? months : 12, 36));
        Instant generatedAt = Instant.now();
        List<RevenueMonthRow> rows = new ArrayList<>();

        for (int i = window - 1; i >= 0; i--) {
            LocalDate monthStartDate = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).minusMonths(i);
            Instant monthStart = monthStartDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant monthEnd = monthStartDate.plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant();

            List<Invoice> monthInvoices = invoiceRepository.findByCreatedRange(monthStart, monthEnd);
            BigDecimal paidInMonth = monthInvoices.stream()
                    .filter(inv -> InvoiceStatus.PAID.equals(inv.getStatus()))
                    .map(inv -> inv.getAmount() != null ? inv.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long activeStart = orgSubscriptionRepository.countActiveAt(monthStart);
            long endedThisMonth = orgSubscriptionRepository.countEndedBetween(monthStart, monthEnd);
            double churnRate = activeStart > 0 ? (endedThisMonth * 100.0) / activeStart : 0.0;

            rows.add(new RevenueMonthRow(
                    monthStartDate.toString(),
                    paidInMonth,
                    paidInMonth.multiply(BigDecimal.valueOf(12)),
                    activeStart,
                    endedThisMonth,
                    BigDecimal.valueOf(churnRate).setScale(2, java.math.RoundingMode.HALF_UP)
            ));
        }
        return new RevenueAnalyticsResult(window, generatedAt, rows);
    }

    @Transactional(readOnly = true)
    public SuperAdminRevenueOverviewResponse getRevenueOverview(String period, String timezone) {
        QueryWindow window = QueryWindow.from(period, timezone);
        Instant fromAt = window.from();
        Instant toAtExclusive = window.to();

        List<Invoice> invoices = invoiceRepository.findByCreatedRangeWithPlan(fromAt, toAtExclusive);
        List<Invoice> paidInvoices = invoices.stream()
                .filter(inv -> InvoiceStatus.PAID.equals(inv.getStatus()))
                .toList();

        BigDecimal mrr = paidInvoices.stream()
                .map(inv -> inv.getAmount() != null ? inv.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal arr = mrr.multiply(BigDecimal.valueOf(12));

        long activeStart = orgSubscriptionRepository.countActiveAt(fromAt);
        long endedInWindow = orgSubscriptionRepository.countEndedBetween(fromAt, toAtExclusive);
        BigDecimal churnRate = activeStart > 0
                ? BigDecimal.valueOf((endedInWindow * 100.0) / activeStart).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> revenueByPlan = new LinkedHashMap<>();
        for (Invoice invoice : paidInvoices) {
            String planName = resolvePlanName(invoice);
            BigDecimal amount = invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO;
            revenueByPlan.merge(planName, amount, BigDecimal::add);
        }

        List<SuperAdminRevenueByPlanResponse> revenueRows = revenueByPlan.entrySet().stream()
                .map(entry -> new SuperAdminRevenueByPlanResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(SuperAdminRevenueByPlanResponse::getAmount).reversed())
                .toList();

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", java.sql.Timestamp.from(fromAt))
                .addValue("to", java.sql.Timestamp.from(toAtExclusive));

        Map<String, Object> movementRow = namedParameterJdbcTemplate.queryForMap(
                "WITH ordered AS ( " +
                        "   SELECT os.organisation_id, os.start_at, os.price_at_time, " +
                        "          LAG(os.price_at_time) OVER (PARTITION BY os.organisation_id ORDER BY os.start_at) AS prev_price " +
                        "   FROM public.org_subscriptions os " +
                        "   WHERE os.start_at >= :from AND os.start_at < :to " +
                        ") " +
                        "SELECT " +
                        "   COALESCE(SUM(CASE WHEN prev_price IS NOT NULL AND price_at_time > prev_price THEN 1 ELSE 0 END), 0) AS upgrades, " +
                        "   COALESCE(SUM(CASE WHEN prev_price IS NOT NULL AND price_at_time < prev_price THEN 1 ELSE 0 END), 0) AS downgrades " +
                        "FROM ordered",
                params
        );

        long upgrades = toLong(movementRow.get("upgrades"));
        long downgrades = toLong(movementRow.get("downgrades"));

        SuperAdminRevenueOverviewResponse response = new SuperAdminRevenueOverviewResponse();
        response.setPeriod(window.period().code());
        response.setTimezone(window.zone().getId());
        response.setFrom(fromAt);
        response.setTo(toAtExclusive);
        response.setMrr(mrr);
        response.setArr(arr);
        response.setChurnRate(churnRate);
        response.setRevenueByPlan(revenueRows);
        response.setUpgrades(upgrades);
        response.setDowngrades(downgrades);
        response.setGeneratedAt(Instant.now());
        return response;
    }

    private Map<String, Object> invoiceSnapshot(Invoice invoice) {
        if (invoice == null) {
            return Map.of();
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", invoice.getId());
        snapshot.put("subscriptionId", invoice.getSubscription() != null ? invoice.getSubscription().getId() : null);
        snapshot.put("amount", invoice.getAmount());
        snapshot.put("outstandingBalance", invoice.getOutstandingBalance());
        snapshot.put("totalPaid", invoice.getTotalPaid());
        snapshot.put("refundedAmount", invoice.getRefundedAmount());
        snapshot.put("status", invoice.getStatus() != null ? invoice.getStatus().name() : null);
        snapshot.put("dueDate", invoice.getDueDate());
        snapshot.put("billingPeriodStart", invoice.getBillingPeriodStart());
        snapshot.put("billingPeriodEnd", invoice.getBillingPeriodEnd());
        snapshot.put("paidAt", invoice.getPaidAt());
        snapshot.put("providerInvoiceId", invoice.getProviderInvoiceId());
        snapshot.put("providerChargeId", invoice.getProviderChargeId());
        snapshot.put("providerPaymentIntentId", invoice.getProviderPaymentIntentId());
        return snapshot;
    }

    private static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number num) {
            return num.longValue();
        }
        return 0L;
    }

    private static String resolvePlanName(Invoice invoice) {
        if (invoice == null || invoice.getSubscription() == null || invoice.getSubscription().getPlan() == null) {
            return "Trial / Other";
        }
        String planName = invoice.getSubscription().getPlan().getName();
        if (planName == null || planName.isBlank()) {
            return "Trial / Other";
        }
        return planName;
    }

    private static ZoneId parseZone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of("UTC");
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (DateTimeException ex) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_TIMEZONE", "Invalid timezone: " + timezone);
        }
    }

    private record QueryWindow(PeriodWindow period, ZoneId zone, Instant from, Instant to) {
        static QueryWindow from(String periodText, String timezone) {
            PeriodWindow period = PeriodWindow.fromValue(periodText);
            ZoneId zone = parseZone(timezone);
            LocalDate today = LocalDate.now(zone);
            LocalDate startDate = today.minusDays(period.days() - 1L);
            ZonedDateTime startZdt = startDate.atStartOfDay(zone);
            ZonedDateTime endZdt = today.plusDays(1).atStartOfDay(zone);
            return new QueryWindow(period, zone, startZdt.toInstant(), endZdt.toInstant());
        }
    }

    private enum PeriodWindow {
        D7("7d", 7),
        D30("30d", 30),
        D90("90d", 90);

        private final String code;
        private final int days;

        PeriodWindow(String code, int days) {
            this.code = code;
            this.days = days;
        }

        public String code() {
            return code;
        }

        public int days() {
            return days;
        }

        public static PeriodWindow fromValue(String input) {
            if (input == null || input.isBlank()) {
                return D30;
            }
            String normalized = input.trim().toLowerCase(Locale.ROOT);
            for (PeriodWindow window : values()) {
                if (Objects.equals(window.code, normalized)) {
                    return window;
                }
            }
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "period must be 7d, 30d, or 90d");
        }
    }

    @Transactional
    public CreateExportJobResult createExportJob(Long actorAuthId,
                                                 BillingExportType exportType,
                                                 Long organisationId,
                                                 String status,
                                                 Integer months) {
        Map<String, Object> payload = new HashMap<>();
        if (organisationId != null) payload.put("organisationId", organisationId);
        if (status != null) payload.put("status", status);
        if (months != null) payload.put("months", months);

        try {
            BillingExportService.CreatedExportJob created = billingExportService.createJob(actorAuthId, exportType, payload);
            BillingExportJob job = created.job();
            platformAuditService.log(
                    actorAuthId,
                    "BILLING_EXPORT_JOB_CREATED",
                    "BillingExportJob",
                    String.valueOf(job.getId()),
                    "type=" + job.getExportType()
            );
            return CreateExportJobResult.success(job, created.downloadToken());
        } catch (IllegalArgumentException e) {
            return CreateExportJobResult.error(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public BillingExportJob getExportJob(Long jobId) {
        return billingExportJobRepository.findById(jobId).orElse(null);
    }

    public DownloadExportResult downloadExport(Long jobId, String token) {
        BillingExportService.DownloadTokenValidationResult validation = billingExportService.validateAndConsumeDownloadToken(jobId, token);
        if (validation.state() == BillingExportService.DownloadTokenState.JOB_NOT_FOUND) {
            return DownloadExportResult.notFound("Export job not found");
        }
        if (validation.state() == BillingExportService.DownloadTokenState.TOKEN_EXPIRED) {
            return DownloadExportResult.gone("Download token expired");
        }
        if (validation.state() == BillingExportService.DownloadTokenState.TOKEN_CONSUMED) {
            return DownloadExportResult.gone("Download token already consumed");
        }
        if (validation.state() == BillingExportService.DownloadTokenState.EXPORT_NOT_READY) {
            return DownloadExportResult.conflict("Export is not ready for download");
        }
        if (validation.state() != BillingExportService.DownloadTokenState.VALID) {
            return DownloadExportResult.forbidden("Invalid download token");
        }
        BillingExportJob job = validation.job();
        if (job.getFilePath() == null || job.getFilePath().isBlank()) {
            return DownloadExportResult.notFound("Export artifact missing");
        }
        try {
            byte[] bytes = billingExportService.readArtifact(job);
            return DownloadExportResult.success(job, bytes);
        } catch (Exception e) {
            return DownloadExportResult.internalError(e.getMessage());
        }
    }

    public record UpsertBillingContactResult(boolean success, boolean notFound, String error, boolean created, BillingContact contact) {
        public static UpsertBillingContactResult success(BillingContact contact, boolean created) {
            return new UpsertBillingContactResult(true, false, null, created, contact);
        }
        public static UpsertBillingContactResult error(String error) {
            return new UpsertBillingContactResult(false, false, error, false, null);
        }
        public static UpsertBillingContactResult notFoundResult() {
            return new UpsertBillingContactResult(false, true, null, false, null);
        }
    }

    public record RemoveBillingContactResult(boolean success, boolean orgNotFound, boolean contactNotFound) {
        public static RemoveBillingContactResult successResult() { return new RemoveBillingContactResult(true, false, false); }
        public static RemoveBillingContactResult orgNotFoundResult() { return new RemoveBillingContactResult(false, true, false); }
        public static RemoveBillingContactResult contactNotFoundResult() { return new RemoveBillingContactResult(false, false, true); }
    }

    public record UpsertTemplateResult(boolean success, String error, BillingNotificationTemplate template) {
        public static UpsertTemplateResult success(BillingNotificationTemplate template) {
            return new UpsertTemplateResult(true, null, template);
        }
        public static UpsertTemplateResult error(String error) {
            return new UpsertTemplateResult(false, error, null);
        }
    }

    public record ListBillingLogsResult(boolean success, boolean notFound, List<BillingNotificationLog> logs) {
        public static ListBillingLogsResult success(List<BillingNotificationLog> logs) {
            return new ListBillingLogsResult(true, false, logs);
        }
        public static ListBillingLogsResult notFoundResult() {
            return new ListBillingLogsResult(false, true, List.of());
        }
    }

    public record ListInvoiceAdjustmentsResult(boolean success, boolean notFound, List<InvoiceAdjustment> adjustments) {
        public static ListInvoiceAdjustmentsResult success(List<InvoiceAdjustment> adjustments) {
            return new ListInvoiceAdjustmentsResult(true, false, adjustments);
        }
        public static ListInvoiceAdjustmentsResult notFoundResult() {
            return new ListInvoiceAdjustmentsResult(false, true, List.of());
        }
    }

    public record InvoiceReminderResult(
            Long invoiceId,
            Long organisationId,
            int emailsSent,
            int emailsFailed,
            int inAppNotificationsCreated,
            List<String> emailRecipients,
            String warning
    ) {}

    public record InvoiceAdjustmentActionResult(boolean success, boolean notFound, String error, InvoiceAdjustment adjustment) {
        public static InvoiceAdjustmentActionResult success(InvoiceAdjustment adjustment) {
            return new InvoiceAdjustmentActionResult(true, false, null, adjustment);
        }
        public static InvoiceAdjustmentActionResult notFoundResult() {
            return new InvoiceAdjustmentActionResult(false, true, null, null);
        }
        public static InvoiceAdjustmentActionResult error(String error) {
            return new InvoiceAdjustmentActionResult(false, false, error, null);
        }
    }

    public record ListInvoiceDisputesResult(boolean success, boolean notFound, List<InvoiceDispute> disputes) {
        public static ListInvoiceDisputesResult success(List<InvoiceDispute> disputes) {
            return new ListInvoiceDisputesResult(true, false, disputes);
        }
        public static ListInvoiceDisputesResult notFoundResult() {
            return new ListInvoiceDisputesResult(false, true, List.of());
        }
    }

    public record DisputeActionResult(boolean success, int statusCode, String error, InvoiceDispute dispute) {
        public static DisputeActionResult success(InvoiceDispute dispute, int statusCode) {
            return new DisputeActionResult(true, statusCode, null, dispute);
        }
        public static DisputeActionResult notFound() {
            return new DisputeActionResult(false, HttpStatus.NOT_FOUND.value(), null, null);
        }
        public static DisputeActionResult notFoundWithMessage(String error) {
            return new DisputeActionResult(false, HttpStatus.NOT_FOUND.value(), error, null);
        }
        public static DisputeActionResult error(String error) {
            return new DisputeActionResult(false, HttpStatus.BAD_REQUEST.value(), error, null);
        }
    }

    public record RevenueReportResult(String period, String groupBy, BigDecimal mrr,
                                      BigDecimal arr, BigDecimal churnRate, List<RevenueMovementRow> movements) {}

    public record RevenueMovementRow(String key, BigDecimal amount, long paidInvoices) {}

    public record RevenueMonthRow(String month, BigDecimal mrr, BigDecimal arr, long activeSubscriptions,
                                  long endedSubscriptions, BigDecimal churnRatePct) {}

    public record RevenueAnalyticsResult(int months, Instant generatedAt, List<RevenueMonthRow> rows) {}

    public record CreateExportJobResult(boolean success, String error, BillingExportJob job, String downloadToken) {
        public static CreateExportJobResult success(BillingExportJob job, String downloadToken) {
            return new CreateExportJobResult(true, null, job, downloadToken);
        }
        public static CreateExportJobResult error(String error) {
            return new CreateExportJobResult(false, error, null, null);
        }
    }

    public record DownloadExportResult(boolean success, int statusCode, String error, BillingExportJob job, byte[] bytes) {
        public static DownloadExportResult success(BillingExportJob job, byte[] bytes) {
            return new DownloadExportResult(true, HttpStatus.OK.value(), null, job, bytes);
        }
        public static DownloadExportResult forbidden(String error) {
            return new DownloadExportResult(false, HttpStatus.FORBIDDEN.value(), error, null, null);
        }
        public static DownloadExportResult notFound(String error) {
            return new DownloadExportResult(false, HttpStatus.NOT_FOUND.value(), error, null, null);
        }
        public static DownloadExportResult gone(String error) {
            return new DownloadExportResult(false, HttpStatus.GONE.value(), error, null, null);
        }
        public static DownloadExportResult conflict(String error) {
            return new DownloadExportResult(false, HttpStatus.CONFLICT.value(), error, null, null);
        }
        public static DownloadExportResult internalError(String error) {
            return new DownloadExportResult(false, HttpStatus.INTERNAL_SERVER_ERROR.value(), error, null, null);
        }
    }

    private Invoice loadInvoiceOrThrow(Long invoiceId) {
        return invoiceRepository.findDetailedById(invoiceId)
                .or(() -> invoiceRepository.findById(invoiceId))
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found"));
    }

    private void assertMutationAllowed(Invoice invoice) {
        if (InvoiceStatus.VOID.equals(invoice.getStatus())) {
            throw new StoryApiException(HttpStatus.CONFLICT, "INVOICE_VOID", "Invoice is void");
        }
    }

    private BigDecimal normalizeOutstanding(Invoice invoice) {
        if (invoice.getOutstandingBalance() != null && invoice.getOutstandingBalance().compareTo(BigDecimal.ZERO) >= 0) {
            return invoice.getOutstandingBalance();
        }
        BigDecimal amount = invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO;
        BigDecimal paid = invoice.getTotalPaid() != null ? invoice.getTotalPaid() : BigDecimal.ZERO;
        BigDecimal refunded = invoice.getRefundedAmount() != null ? invoice.getRefundedAmount() : BigDecimal.ZERO;
        BigDecimal outstanding = amount.subtract(paid).add(refunded);
        if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return outstanding;
    }

    private void queueRefundRetry(Invoice invoice,
                                  BigDecimal amountUsd,
                                  String reason,
                                  Long actorAuthId,
                                  String error) {
        RefundRetryTask task = new RefundRetryTask();
        task.setInvoice(invoice);
        task.setAmountUsd(amountUsd);
        task.setReason(reason);
        task.setStatus(RefundRetryStatus.PENDING);
        task.setRetryCount(0);
        task.setNextRetryAt(Instant.now().plusSeconds(300));
        task.setLastError(error);
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        task.setCreatedBy(actorAuthId);
        refundRetryTaskRepository.save(task);
    }

    private void validateReason(String reason) {
        if (reason == null) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "reason is required (5-500 chars)");
        }
        String trimmed = reason.trim();
        if (trimmed.length() < 5 || trimmed.length() > 500) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "reason is required (5-500 chars)");
        }
    }

    private InvoiceStatus parseInvoiceStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if ("VOID".equals(normalized)) {
            return InvoiceStatus.VOID;
        }
        try {
            return InvoiceStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid invoice status");
        }
    }

    private InvoiceDisputeStatus parseDisputeStatus(String status) {
        if (status == null || status.isBlank()) {
            return InvoiceDisputeStatus.OPEN;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return InvoiceDisputeStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid dispute status");
        }
    }

    private YearMonth parsePeriod(String period) {
        if (period == null || period.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "period must be YYYY-MM");
        }
        try {
            return YearMonth.parse(period.trim());
        } catch (Exception ex) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "period must be YYYY-MM");
        }
    }

    private RevenueGroupBy parseRevenueGroupBy(String groupBy) {
        if (groupBy == null || groupBy.isBlank()) {
            return RevenueGroupBy.PLAN;
        }
        String normalized = groupBy.trim().toUpperCase(Locale.ROOT);
        try {
            return RevenueGroupBy.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "groupBy must be org or plan");
        }
    }

    private String resolveMovementKey(Invoice invoice, RevenueGroupBy groupBy) {
        if (RevenueGroupBy.ORG.equals(groupBy)) {
            Long organisationId = invoice.getSubscription().getOrganisation().getId();
            return organisationId != null ? String.valueOf(organisationId) : "UNKNOWN_ORG";
        }
        String planName = invoice.getSubscription().getPlan() != null ? invoice.getSubscription().getPlan().getName() : null;
        return (planName == null || planName.isBlank()) ? "UNKNOWN_PLAN" : planName;
    }

    private enum RevenueGroupBy {
        ORG("org"),
        PLAN("plan");

        private final String apiValue;

        RevenueGroupBy(String apiValue) {
            this.apiValue = apiValue;
        }
    }

    private static final class MovementAccumulator {
        private BigDecimal amount = BigDecimal.ZERO;
        private long paidInvoices = 0;
    }
}
