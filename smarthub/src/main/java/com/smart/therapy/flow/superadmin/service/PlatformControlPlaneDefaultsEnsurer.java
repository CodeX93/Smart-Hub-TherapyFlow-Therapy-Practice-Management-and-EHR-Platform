package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.superadmin.entity.PlatformTenantRoutingSettings;
import com.smart.therapy.flow.superadmin.repository.PlatformTenantRoutingSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Ensures critical public control-plane defaults exist after wipe/restore scenarios
 * where Flyway history is retained but seed rows were deleted.
 */
@Component
@Order(40)
@RequiredArgsConstructor
@Slf4j
public class PlatformControlPlaneDefaultsEnsurer implements ApplicationRunner {

    private final PlatformTenantRoutingSettingsRepository routingSettingsRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureRoutingSettings();
        ensureNotificationTemplates();
        log.info("Platform control-plane defaults verified");
    }

    private void ensureRoutingSettings() {
        if (routingSettingsRepository.findTopByOrderByIdAsc().isPresent()) {
            return;
        }
        Instant now = Instant.now();
        routingSettingsRepository.save(PlatformTenantRoutingSettings.builder()
                .emailAutoRouting(true)
                .pathBasedRouting(false)
                .pathPrefix("/t")
                .orgIdentifier("slug")
                .createdAt(now)
                .updatedAt(now)
                .build());
        log.warn("Restored missing platform_tenant_routing_settings (email_auto_routing=true)");
    }

    private void ensureNotificationTemplates() {
        // Billing templates
        jdbcTemplate.update("""
                INSERT INTO public.billing_notification_templates
                    (event_key, subject_template, body_template, is_active, created_at, updated_at)
                VALUES
                ('TRIAL_EXPIRY_NOTICE', 'Your trial expires soon ({daysBefore} days)', 'Hello {organisationName}, your trial will end on {trialEndAt}. To avoid interruption, please add a payment method and choose a paid plan.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('TRIAL_EXPIRED_PAST_DUE', 'Trial ended: account moved to past_due', 'Hello {organisationName}, your trial ended on {trialEndAt}. Your subscription is now past_due. Please complete payment to keep access.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('DUNNING_ATTEMPT', 'Payment retry attempt #{attempt}', 'Hello {organisationName}, we could not process your payment. This is retry attempt #{attempt}. Next retry is scheduled at {nextDunningAt}.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('SUBSCRIPTION_CANCELLED_NON_PAYMENT', 'Subscription cancelled due to non-payment', 'Hello {organisationName}, your subscription has been cancelled after multiple failed payment retries. Access is now restricted. Contact support to reactivate.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('PAYMENT_RECOVERED', 'Payment received: access restored', 'Hello {organisationName}, payment was received successfully and your account access has been restored.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('SUBSCRIPTION_TERM_ENDING_SOON', 'Subscription ends in {daysBefore} day(s)', 'Hello {organisationName}, your current subscription term ends on {termEndAt}. To avoid service interruption, renew or change your plan before the end time.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('SUBSCRIPTION_TERM_ENDED', 'Subscription term ended', 'Hello {organisationName}, your subscription term ended on {termEndAt}. Access is now restricted. Please renew or contact support to restore access.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('SUBSCRIPTION_INVOICE_READY', 'Subscription invoice ready for payment', 'Hello {organisationName}, your subscription invoice for {amount} is ready. Sign in to your billing settings to pay before {dueDate}.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('SUBSCRIPTION_INVOICE_PAYMENT_REMINDER', 'Payment reminder: invoice #{invoiceId} due {dueDate}', 'Hello {organisationName}, this is a reminder to pay subscription invoice #{invoiceId} for {amount}. Outstanding balance: {outstandingBalance}. Please sign in to billing settings and complete payment before {dueDate}.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                ('SUBSCRIPTION_CHECKOUT_REQUIRED', 'Action required: complete subscription setup', 'Hello {organisationName}, your trial has ended or your subscription requires payment. Please complete checkout in billing settings to restore full access.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (event_key) DO NOTHING
                """);

        // Platform notification templates
        Object[][] platformTemplates = {
                {"maintenance", "Scheduled maintenance notice", "We will perform maintenance at {{startAt}}. Expected duration: {{duration}}."},
                {"broadcast", "Platform update", "{{message}}"},
                {"platform_email", "Platform Notification", "<div>Hi {{recipientName}},</div><div>{{message}}</div>"},
                {"platform_notification_email", "Platform Notification", "<div>Hi {{recipientName}},</div><div>{{message}}</div>"},
                {"platform_broadcast_email", "Platform update", "<div>Hi {{recipientName}},</div><div>{{message}}</div>"},
                {"broadcast_email", "Platform update", "<div>Hi {{recipientName}},</div><div>{{message}}</div>"},
                {"platform_notification", "Platform Notification", "{{message}}"}
        };
        for (Object[] row : platformTemplates) {
            jdbcTemplate.update("""
                    INSERT INTO public.platform_notification_templates
                        (template_key, subject_template, body_template, is_active, created_at, updated_at)
                    SELECT ?, ?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                    WHERE NOT EXISTS (
                        SELECT 1 FROM public.platform_notification_templates WHERE template_key = ?
                    )
                    """, row[0], row[1], row[2], row[0]);
        }
    }
}
