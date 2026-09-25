package com.smart.therapy.flow.session.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.service.ClientPortalSettingsService;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.common.tenant.TenantExecutionService;
import com.smart.therapy.flow.session.entity.SessionIntegration;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.repository.SessionIntegrationRepository;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.notification.service.NotificationEventCatalog;
import com.smart.therapy.flow.notification.service.NotificationPayloadFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for sending session reminder emails 24 hours before appointments.
 * Runs every hour to check for sessions that need reminders.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SessionReminderService {

    private final SessionRepository sessionRepository;
    private final SessionIntegrationRepository sessionIntegrationRepository;
    private final EmailService emailService;
    private final ClientPortalSettingsService portalSettingsService;
    private final TenantExecutionService tenantExecutionService;
    private final com.smart.therapy.flow.notification.service.NotificationService notificationService;

    /**
     * Scheduled job that runs every hour to send reminder emails for sessions
     * that are approximately 24 hours away.
     * 
     * Cron expression: "0 0 * * * *" means "at minute 0 of every hour"
     * This ensures reminders are sent consistently without being too frequent.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at minute 0
    public void sendSessionReminders() {
        tenantExecutionService.runForEachActiveTenant("session-reminders", tenant ->
                sendSessionRemindersForTenant());
    }

    @Transactional(readOnly = true)
    protected void sendSessionRemindersForTenant() {
        try {
            Instant now = Instant.now();
            
            // Calculate time window: 23-25 hours from now
            // This accounts for:
            // - Scheduling delays (job runs every hour)
            // - Timezone differences
            // - Ensures each session gets exactly one reminder
            Instant reminderStartTime = now.plus(23, ChronoUnit.HOURS);
            Instant reminderEndTime = now.plus(25, ChronoUnit.HOURS);

            log.debug("Checking for sessions needing reminders between {} and {}", 
                reminderStartTime, reminderEndTime);

            // Find sessions that need reminders
            List<Session> sessionsForReminder = sessionRepository.findSessionsForReminder(
                reminderStartTime, 
                reminderEndTime
            );

            if (sessionsForReminder.isEmpty()) {
                log.debug("No sessions found for reminder emails");
                return;
            }

            hydrateIntegrations(sessionsForReminder);

            log.info("Found {} session(s) needing reminder emails", sessionsForReminder.size());

            int successCount = 0;
            int failureCount = 0;

            for (Session session : sessionsForReminder) {
                try {
                    sendReminderEmails(session);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to send reminder emails for sessionId: {}", 
                        session.getId(), e);
                    // Continue with other sessions even if one fails
                }
            }

            log.info("Reminder email job completed: {} sent, {} failed", 
                successCount, failureCount);

        } catch (Exception e) {
            log.error("Error in scheduled reminder email job", e);
            // Don't throw - let the job run again next hour
        }
    }

    /**
     * Load integrations without SELECT DISTINCT on json metadata
     * (Postgres cannot DISTINCT json columns).
     */
    private void hydrateIntegrations(List<Session> sessions) {
        List<Long> sessionIds = sessions.stream()
                .map(Session::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (sessionIds.isEmpty()) {
            return;
        }
        Map<Long, List<SessionIntegration>> integrationsBySessionId = sessionIntegrationRepository
                .findBySessionIdIn(sessionIds)
                .stream()
                .filter(si -> si.getSession() != null && si.getSession().getId() != null)
                .collect(java.util.stream.Collectors.groupingBy(si -> si.getSession().getId()));
        for (Session session : sessions) {
            List<SessionIntegration> integrations = integrationsBySessionId.get(session.getId());
            session.setIntegrations(integrations != null
                    ? new java.util.ArrayList<>(integrations)
                    : new java.util.ArrayList<>());
        }
    }

    /**
     * Send reminder emails to both client and therapist for a session.
     */
    private void sendReminderEmails(Session session) {
        if (emailService == null) {
            log.warn("EmailService not available, cannot send reminder emails");
            return;
        }

        try {
            Client client = session.getClient();
            com.smart.therapy.flow.auth.entity.User therapist = session.getTherapist();

            if (client == null || therapist == null) {
                log.warn("Session {} missing client or therapist, skipping reminder", session.getId());
                return;
            }

            if (notificationService != null) {
                notificationService.processEvent(NotificationEventCatalog.SESSION_REMINDER, buildSessionReminderEventData(session));
                return;
            }

            // Send reminder to client (if email notifications enabled)
            String clientEmail = client.getPrimaryEmail();
            Boolean emailNotifications = portalSettingsService.findByClientId(client.getId())
                    .map(s -> s.getEmailNotifications())
                    .orElse(null);

            if (clientEmail != null && Boolean.TRUE.equals(emailNotifications)) {
                try {
                    emailService.sendSessionReminderEmail(client, session, therapist);
                    log.debug("Reminder email sent to client: clientId={}, sessionId={}", 
                        client.getId(), session.getId());
                } catch (Exception e) {
                    log.error("Failed to send reminder email to client: clientId={}, sessionId={}", 
                        client.getId(), session.getId(), e);
                }
            } else {
                log.debug("Skipping client reminder: clientId={}, hasEmail={}, notificationsEnabled={}",
                    client.getId(), clientEmail != null, emailNotifications);
            }

            // Send reminder to therapist
            if (therapist.getEmail() != null) {
                try {
                    emailService.sendSessionReminderEmail(therapist, session, client);
                    log.debug("Reminder email sent to therapist: therapistId={}, sessionId={}", 
                        therapist.getId(), session.getId());
                } catch (Exception e) {
                    log.error("Failed to send reminder email to therapist: therapistId={}, sessionId={}", 
                        therapist.getId(), session.getId(), e);
                }
            } else {
                log.debug("Skipping therapist reminder: no email configured, therapistId={}", 
                    therapist.getId());
            }

        } catch (Exception e) {
            log.error("Error sending reminder emails for sessionId: {}", session.getId(), e);
            throw e; // Re-throw to be caught by caller
        }
    }

    private Map<String, Object> buildSessionReminderEventData(Session session) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", session.getId());
        data.put("clientId", session.getClient() != null ? session.getClient().getId() : null);
        NotificationPayloadFactory.putClientIdentity(data, session.getClient());
        data.put("therapistId", session.getTherapist() != null ? session.getTherapist().getId() : null);
        data.put("therapistName", session.getTherapist() != null ? session.getTherapist().getFullName() : null);
        data.put("sessionDate", session.getSessionDate());
        data.put("duration", session.getDuration());
        data.put("sessionType", session.getClinicalSessionType() != null ? session.getClinicalSessionType()
                : (session.getService() != null
                        ? (session.getService().getCategory() != null ? session.getService().getCategory()
                                : session.getService().getServiceName())
                        : null));
        data.put("sessionMode", session.getSessionType());
        data.put("serviceName", session.getService() != null ? session.getService().getServiceName() : null);
        data.put("roomName", session.getRoom() != null ? session.getRoom().getRoomName() : null);

        SessionIntegration zoomIntegration = getZoomIntegration(session);
        data.put("zoomEnabled", zoomIntegration != null);
        if (zoomIntegration != null) {
            data.put("zoomMeetingId", zoomIntegration.getMeetingId());
            data.put("zoomJoinUrl", zoomIntegration.getJoinUrl());
            data.put("zoomPassword", zoomIntegration.getPassword());
        }
        return data;
    }

    private SessionIntegration getZoomIntegration(Session session) {
        if (session.getIntegrations() == null || session.getIntegrations().isEmpty()) {
            return null;
        }
        return session.getIntegrations().stream()
                .filter(integration -> "zoom".equalsIgnoreCase(integration.getProvider()))
                .findFirst()
                .orElse(null);
    }
}
