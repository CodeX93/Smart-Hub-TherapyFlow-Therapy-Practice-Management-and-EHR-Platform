package com.smart.therapy.flow.client.portal.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientPortalSettings;
import com.smart.therapy.flow.client.service.ClientPortalSettingsService;
import com.smart.therapy.flow.common.audit.HipaaAuditLabels;
import com.smart.therapy.flow.common.service.EmailHtmlComponents;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.common.tenant.TenantExecutionService;
import com.smart.therapy.flow.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Emails therapists whose clients asked for online sessions while their Zoom account was not
 * connected. Batched on purpose: a therapist with ten waiting clients gets one message listing
 * them, not ten. Clients are named by MRN only.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OnlineBookingRequestDigestService {

    /** One digest per therapist per day, however many clients ask in between. */
    private static final Duration DIGEST_INTERVAL = Duration.ofDays(1);

    private final ClientPortalSettingsService portalSettingsService;
    private final EmailService emailService;
    private final TenantExecutionService tenantExecutionService;

    @Autowired(required = false)
    private SessionService.ZoomService zoomService;

    @Value("${app.frontend.therapist-zoom-setup-url:https://app.therapyflow.pro/therapist/dashboard}")
    private String zoomSetupUrl;

    @Scheduled(cron = "0 15 * * * *") // Every hour at minute 15, offset from the session reminder job
    public void sendOnlineBookingDigests() {
        tenantExecutionService.runForEachActiveTenant("online-booking-digests",
                tenant -> sendOnlineBookingDigestsForTenant());
    }

    @Transactional
    protected void sendOnlineBookingDigestsForTenant() {
        List<ClientPortalSettings> pending;
        try {
            pending = portalSettingsService.findPendingOnlineBookingRequests();
        } catch (Exception e) {
            log.error("Failed to load pending online booking requests", e);
            return;
        }
        if (pending.isEmpty()) {
            return;
        }

        Map<Long, List<ClientPortalSettings>> byTherapist = new LinkedHashMap<>();
        Map<Long, User> therapists = new LinkedHashMap<>();
        for (ClientPortalSettings settings : pending) {
            Client client = settings.getClient();
            User therapist = client != null ? client.getAssignedTherapist() : null;
            if (therapist == null || therapist.getId() == null) {
                continue;
            }
            therapists.putIfAbsent(therapist.getId(), therapist);
            byTherapist.computeIfAbsent(therapist.getId(), k -> new ArrayList<>()).add(settings);
        }

        Instant now = Instant.now();
        for (Map.Entry<Long, List<ClientPortalSettings>> entry : byTherapist.entrySet()) {
            User therapist = therapists.get(entry.getKey());
            List<ClientPortalSettings> waiting = entry.getValue();
            try {
                // Connected since the request: the ask is moot, so clear it rather than mail about it.
                if (zoomService != null && zoomService.isTherapistConfigured(therapist)) {
                    portalSettingsService.markOnlineBookingRequestsNotified(waiting, now);
                    continue;
                }

                Instant lastDigest = portalSettingsService.findLastOnlineBookingDigestAt(therapist.getId());
                if (lastDigest != null && lastDigest.isAfter(now.minus(DIGEST_INTERVAL))) {
                    // Already mailed today; these clients ride along in tomorrow's digest.
                    continue;
                }

                if (!StringUtils.hasText(therapist.getEmail())) {
                    portalSettingsService.markOnlineBookingRequestsNotified(waiting, now);
                    continue;
                }

                emailService.sendEmail(therapist.getEmail(), buildSubject(waiting.size()), buildBody(waiting));
                portalSettingsService.markOnlineBookingRequestsNotified(waiting, now);
                log.info("Sent online booking digest: therapistId={}, clients={}",
                        therapist.getId(), waiting.size());
            } catch (Exception e) {
                // Leave the requests pending so the next run retries them.
                log.error("Failed to send online booking digest for therapist {}", therapist.getId(), e);
            }
        }
    }

    private String buildSubject(int clientCount) {
        return clientCount == 1
                ? "A client tried to book an online session"
                : clientCount + " clients tried to book online sessions";
    }

    private String buildBody(List<ClientPortalSettings> waiting) {
        String intro = waiting.size() == 1
                ? "Client <strong>" + mrn(waiting.get(0)) + "</strong> tried to book an online appointment "
                        + "with you, but your Zoom account isn't connected — so online booking is turned "
                        + "off on your profile."
                : "These clients tried to book an online appointment with you, but your Zoom account isn't "
                        + "connected — so online booking is turned off on your profile:";

        StringBuilder body = new StringBuilder()
                .append(EmailHtmlComponents.heading(buildSubject(waiting.size())))
                .append(EmailHtmlComponents.paragraph(intro));

        if (waiting.size() > 1) {
            StringBuilder list = new StringBuilder();
            for (ClientPortalSettings settings : waiting) {
                list.append(mrn(settings)).append("<br>");
            }
            body.append(EmailHtmlComponents.paragraph(list.toString()));
        }

        return body
                .append(EmailHtmlComponents.paragraph(
                        "Connect Zoom from your profile settings to let your clients book video appointments again."))
                .append(EmailHtmlComponents.primaryButton("Set up Zoom", zoomSetupUrl))
                .toString();
    }

    private String mrn(ClientPortalSettings settings) {
        return HipaaAuditLabels.clientActor(settings.getClient());
    }
}
