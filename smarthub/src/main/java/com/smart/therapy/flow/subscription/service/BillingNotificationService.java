package com.smart.therapy.flow.subscription.service;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.subscription.entity.BillingContact;
import com.smart.therapy.flow.subscription.entity.BillingNotificationLog;
import com.smart.therapy.flow.subscription.entity.BillingNotificationTemplate;
import com.smart.therapy.flow.subscription.entity.Invoice;
import com.smart.therapy.flow.subscription.entity.OrgSubscription;
import com.smart.therapy.flow.subscription.repository.BillingContactRepository;
import com.smart.therapy.flow.subscription.repository.BillingNotificationLogRepository;
import com.smart.therapy.flow.subscription.repository.BillingNotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingNotificationService {

    public static final String EVENT_TRIAL_EXPIRY_NOTICE = "TRIAL_EXPIRY_NOTICE";
    public static final String EVENT_TRIAL_EXPIRED_PAST_DUE = "TRIAL_EXPIRED_PAST_DUE";
    public static final String EVENT_DUNNING_ATTEMPT = "DUNNING_ATTEMPT";
    public static final String EVENT_SUBSCRIPTION_CANCELLED_NON_PAYMENT = "SUBSCRIPTION_CANCELLED_NON_PAYMENT";
    public static final String EVENT_PAYMENT_RECOVERED = "PAYMENT_RECOVERED";
    public static final String EVENT_SUBSCRIPTION_TERM_ENDING_SOON = "SUBSCRIPTION_TERM_ENDING_SOON";
    public static final String EVENT_SUBSCRIPTION_TERM_ENDED = "SUBSCRIPTION_TERM_ENDED";
    public static final String EVENT_SUBSCRIPTION_INVOICE_READY = "SUBSCRIPTION_INVOICE_READY";
    public static final String EVENT_SUBSCRIPTION_INVOICE_PAYMENT_REMINDER = "SUBSCRIPTION_INVOICE_PAYMENT_REMINDER";
    public static final String EVENT_SUBSCRIPTION_CHECKOUT_REQUIRED = "SUBSCRIPTION_CHECKOUT_REQUIRED";

    private final BillingContactRepository billingContactRepository;
    private final BillingNotificationTemplateRepository templateRepository;
    private final BillingNotificationLogRepository logRepository;
    private final AuthIdentityRoleRepository authIdentityRoleRepository;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<BillingContact> getContacts(Long organisationId) {
        return billingContactRepository.findByOrganisation_IdAndIsActiveTrueOrderByIsPrimaryDescCreatedAtAsc(organisationId);
    }

    @Transactional
    public BillingContact saveContact(BillingContact contact) {
        Instant now = Instant.now();
        if (contact.getCreatedAt() == null) {
            contact.setCreatedAt(now);
        }
        contact.setUpdatedAt(now);
        if (contact.getIsActive() == null) {
            contact.setIsActive(true);
        }
        if (contact.getIsPrimary() == null) {
            contact.setIsPrimary(false);
        }
        return billingContactRepository.save(contact);
    }

    @Transactional
    public void removeContact(Long contactId) {
        billingContactRepository.findById(contactId).ifPresent(contact -> {
            contact.setIsActive(false);
            contact.setUpdatedAt(Instant.now());
            billingContactRepository.save(contact);
        });
    }

    @Transactional(readOnly = true)
    public List<BillingNotificationTemplate> listTemplates() {
        return templateRepository.findByIsActiveTrueOrderByEventKeyAsc();
    }

    @Transactional
    public BillingNotificationTemplate upsertTemplate(String eventKey, String subjectTemplate, String bodyTemplate, Boolean active) {
        BillingNotificationTemplate template = templateRepository.findByEventKey(eventKey).orElseGet(BillingNotificationTemplate::new);
        template.setEventKey(eventKey);
        if (subjectTemplate != null) {
            template.setSubjectTemplate(subjectTemplate);
        }
        if (bodyTemplate != null) {
            template.setBodyTemplate(bodyTemplate);
        }
        if (active != null) {
            template.setIsActive(active);
        }
        Instant now = Instant.now();
        if (template.getCreatedAt() == null) {
            template.setCreatedAt(now);
        }
        template.setUpdatedAt(now);
        return templateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public List<BillingNotificationLog> getLogs(Long organisationId) {
        return logRepository.findTop200ByOrganisation_IdOrderByCreatedAtDesc(organisationId);
    }

    public void notifyTrialExpiryNotice(OrgSubscription subscription, int daysBefore) {
        Map<String, String> vars = buildVars(subscription);
        vars.put("daysBefore", String.valueOf(daysBefore));
        sendToBillingContacts(subscription.getOrganisation(), EVENT_TRIAL_EXPIRY_NOTICE, vars);
    }

    public void notifyTrialExpiredPastDue(OrgSubscription subscription) {
        sendToBillingContacts(subscription.getOrganisation(), EVENT_TRIAL_EXPIRED_PAST_DUE, buildVars(subscription));
    }

    public void notifyDunningAttempt(OrgSubscription subscription, int attempt) {
        Map<String, String> vars = buildVars(subscription);
        vars.put("attempt", String.valueOf(attempt));
        vars.put("nextDunningAt", subscription.getNextDunningAt() != null ? subscription.getNextDunningAt().toString() : "-");
        sendToBillingContacts(subscription.getOrganisation(), EVENT_DUNNING_ATTEMPT, vars);
    }

    public void notifyCancelledForNonPayment(OrgSubscription subscription) {
        sendToBillingContacts(subscription.getOrganisation(), EVENT_SUBSCRIPTION_CANCELLED_NON_PAYMENT, buildVars(subscription));
    }

    public void notifyPaymentRecovered(OrgSubscription subscription) {
        sendToBillingContacts(subscription.getOrganisation(), EVENT_PAYMENT_RECOVERED, buildVars(subscription));
    }

    public void notifySubscriptionTermEndingSoon(OrgSubscription subscription, int daysBefore, Instant termEndAt) {
        Map<String, String> vars = buildVars(subscription);
        vars.put("daysBefore", String.valueOf(daysBefore));
        vars.put("termEndAt", termEndAt != null ? termEndAt.toString() : "-");
        sendToBillingContacts(subscription.getOrganisation(), EVENT_SUBSCRIPTION_TERM_ENDING_SOON, vars);
    }

    public void notifySubscriptionTermEnded(OrgSubscription subscription, Instant termEndAt) {
        Map<String, String> vars = buildVars(subscription);
        vars.put("termEndAt", termEndAt != null ? termEndAt.toString() : "-");
        sendToBillingContacts(subscription.getOrganisation(), EVENT_SUBSCRIPTION_TERM_ENDED, vars);
    }

    public void notifySubscriptionInvoiceReady(Invoice invoice) {
        if (invoice == null || invoice.getSubscription() == null || invoice.getSubscription().getOrganisation() == null) {
            return;
        }
        Map<String, String> vars = buildInvoiceVars(invoice);
        sendToBillingContacts(invoice.getSubscription().getOrganisation(), EVENT_SUBSCRIPTION_INVOICE_READY, vars);
    }

    /**
     * Manual payment reminder for a subscription invoice.
     * Emails billing contacts and organisation ADMIN identities.
     */
    public EmailDeliveryResult notifySubscriptionInvoicePaymentReminder(Invoice invoice) {
        if (invoice == null || invoice.getSubscription() == null || invoice.getSubscription().getOrganisation() == null) {
            return EmailDeliveryResult.empty();
        }
        Organisation organisation = invoice.getSubscription().getOrganisation();
        Map<String, String> vars = buildInvoiceVars(invoice);
        Set<String> recipients = new LinkedHashSet<>();

        for (BillingContact contact : billingContactRepository
                .findByOrganisation_IdAndIsActiveTrueOrderByIsPrimaryDescCreatedAtAsc(organisation.getId())) {
            if (StringUtils.hasText(contact.getEmail())) {
                recipients.add(contact.getEmail().trim().toLowerCase(Locale.ROOT));
            }
        }
        for (AuthIdentity admin : authIdentityRoleRepository.findActiveAdminsByOrganisationId(organisation.getId())) {
            if (StringUtils.hasText(admin.getLoginIdentifier())) {
                recipients.add(admin.getLoginIdentifier().trim().toLowerCase(Locale.ROOT));
            }
        }

        return sendEmails(organisation, EVENT_SUBSCRIPTION_INVOICE_PAYMENT_REMINDER, vars, new ArrayList<>(recipients));
    }

    public void notifySubscriptionCheckoutRequired(OrgSubscription subscription) {
        sendToBillingContacts(subscription.getOrganisation(), EVENT_SUBSCRIPTION_CHECKOUT_REQUIRED, buildVars(subscription));
    }

    @Transactional(readOnly = true)
    public boolean wasEventSentRecently(Long organisationId, String eventKey, Duration within) {
        if (organisationId == null || eventKey == null || within == null || within.isNegative() || within.isZero()) {
            return false;
        }
        Instant fromAt = Instant.now().minus(within);
        return logRepository.existsByOrganisation_IdAndEventKeyAndCreatedAtAfter(organisationId, eventKey, fromAt);
    }

    private Map<String, String> buildVars(OrgSubscription subscription) {
        Map<String, String> vars = new HashMap<>();
        vars.put("organisationName", subscription.getOrganisation().getName());
        vars.put("trialEndAt", subscription.getTrialEndAt() != null ? subscription.getTrialEndAt().toString() : "-");
        vars.put("status", subscription.getStatus());
        return vars;
    }

    private Map<String, String> buildInvoiceVars(Invoice invoice) {
        Map<String, String> vars = buildVars(invoice.getSubscription());
        vars.put("invoiceId", invoice.getId() != null ? String.valueOf(invoice.getId()) : "-");
        vars.put("amount", invoice.getAmount() != null ? invoice.getAmount().toPlainString() : "0");
        vars.put("outstandingBalance",
                invoice.getOutstandingBalance() != null ? invoice.getOutstandingBalance().toPlainString() : "0");
        vars.put("dueDate", invoice.getDueDate() != null ? invoice.getDueDate().toString() : "-");
        vars.put("status", invoice.getStatus() != null ? invoice.getStatus().name() : "-");
        return vars;
    }

    private void sendToBillingContacts(Organisation organisation, String eventKey, Map<String, String> vars) {
        if (organisation == null || organisation.getId() == null) {
            return;
        }
        List<String> recipients = billingContactRepository
                .findByOrganisation_IdAndIsActiveTrueOrderByIsPrimaryDescCreatedAtAsc(organisation.getId())
                .stream()
                .map(BillingContact::getEmail)
                .filter(StringUtils::hasText)
                .map(email -> email.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
        if (recipients.isEmpty()) {
            log.info("No billing contacts configured for org={} event={}", organisation.getId(), eventKey);
            return;
        }
        sendEmails(organisation, eventKey, vars, recipients);
    }

    private EmailDeliveryResult sendEmails(Organisation organisation,
                                           String eventKey,
                                           Map<String, String> vars,
                                           List<String> recipients) {
        if (organisation == null || organisation.getId() == null) {
            return EmailDeliveryResult.empty();
        }
        if (recipients == null || recipients.isEmpty()) {
            log.info("No email recipients for org={} event={}", organisation.getId(), eventKey);
            return EmailDeliveryResult.empty();
        }

        BillingNotificationTemplate template = templateRepository.findByEventKey(eventKey).orElse(null);
        if (template == null || !Boolean.TRUE.equals(template.getIsActive())) {
            log.warn("No active billing template for event={} org={}", eventKey, organisation.getId());
            return new EmailDeliveryResult(0, 0, List.of(), "No active email template for " + eventKey);
        }

        String subject = applyTemplate(template.getSubjectTemplate(), vars);
        String body = applyTemplate(template.getBodyTemplate(), vars);
        int sent = 0;
        int failed = 0;
        List<String> deliveredTo = new ArrayList<>();

        for (String email : recipients) {
            if (!StringUtils.hasText(email)) {
                continue;
            }
            BillingNotificationLog deliveryLog = BillingNotificationLog.builder()
                    .organisation(organisation)
                    .eventKey(eventKey)
                    .channel("EMAIL")
                    .recipient(email)
                    .status("SENT")
                    .createdAt(Instant.now())
                    .build();
            try {
                emailService.sendEmail(email, subject, "<p>" + body + "</p>");
                sent++;
                deliveredTo.add(email);
            } catch (Exception e) {
                failed++;
                deliveryLog.setStatus("FAILED");
                deliveryLog.setErrorMessage(e.getMessage());
                log.warn("Billing notification failed org={} event={}: {}",
                        organisation.getId(), eventKey, e.getMessage());
            }
            logRepository.save(deliveryLog);
        }
        return new EmailDeliveryResult(sent, failed, deliveredTo, null);
    }

    private String applyTemplate(String text, Map<String, String> vars) {
        String out = text != null ? text : "";
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            out = out.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return out;
    }

    public record EmailDeliveryResult(int sent, int failed, List<String> recipients, String warning) {
        public static EmailDeliveryResult empty() {
            return new EmailDeliveryResult(0, 0, List.of(), null);
        }
    }
}
