package com.smart.therapy.flow.superadmin.listener;

import com.smart.therapy.flow.superadmin.event.OrganisationOnboardedEvent;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.superadmin.service.SuperAdminNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrganisationOnboardedListener {

    private final SuperAdminNotificationService notificationService;
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrganisationOnboarded(OrganisationOnboardedEvent event) {
        try {
            emailService.sendOrganisationOnboardingEmail(
                    event.adminEmail(),
                    event.adminName(),
                    event.organisationName(),
                    event.adminEmail(),
                    event.temporaryPassword()
            );
        } catch (Exception ex) {
            log.error("Failed to send onboarding credentials email for organisation {}", event.organisationId(), ex);
        }

        try {
            // Always create an in-app notification job (no extra email sent).
            notificationService.createJob(
                    "TARGETED",
                    "Welcome to SmartHub",
                    "Welcome! Your organisation " + event.organisationName() + " has been onboarded.",
                    "in_app",
                    Instant.now(),
                    Map.of(
                            "organisationIds", java.util.List.of(event.organisationId()),
                            "recipientEmail", event.adminEmail()
                    ),
                    event.actorAuthId()
            );
        } catch (Exception ex) {
            log.error("Failed to enqueue onboarding in-app notification for organisation {}", event.organisationId(), ex);
        }
    }
}
