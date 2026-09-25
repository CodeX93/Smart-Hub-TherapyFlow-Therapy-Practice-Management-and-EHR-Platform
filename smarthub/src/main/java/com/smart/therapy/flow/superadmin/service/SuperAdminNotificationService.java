package com.smart.therapy.flow.superadmin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.organisation.service.TenantSchemaHealthService;
import com.smart.therapy.flow.notification.entity.Notification;
import com.smart.therapy.flow.notification.enums.NotificationCategory;
import com.smart.therapy.flow.notification.enums.NotificationPriority;
import com.smart.therapy.flow.notification.enums.NotificationType;
import com.smart.therapy.flow.notification.repository.NotificationRepository;
import com.smart.therapy.flow.superadmin.dto.SuperAdminNotificationHistoryItemResponse;
import com.smart.therapy.flow.superadmin.entity.PlatformNotificationJob;
import com.smart.therapy.flow.superadmin.entity.PlatformNotificationReadReceipt;
import com.smart.therapy.flow.superadmin.entity.PlatformNotificationTrigger;
import com.smart.therapy.flow.superadmin.entity.PlatformNotificationTemplate;
import com.smart.therapy.flow.superadmin.repository.PlatformNotificationJobRepository;
import com.smart.therapy.flow.superadmin.repository.PlatformNotificationReadReceiptRepository;
import com.smart.therapy.flow.superadmin.repository.PlatformNotificationTriggerRepository;
import com.smart.therapy.flow.superadmin.repository.PlatformNotificationTemplateRepository;
import com.smart.therapy.flow.notification.dto.NotificationTriggerRequest;
import com.smart.therapy.flow.notification.dto.NotificationTriggerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.HashMap;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuperAdminNotificationService {

    private final PlatformNotificationJobRepository jobRepository;
    private final PlatformNotificationReadReceiptRepository readReceiptRepository;
    private final PlatformNotificationTriggerRepository triggerRepository;
    private final PlatformNotificationTemplateRepository templateRepository;
    private final OrganisationRepository organisationRepository;
    private final TenantSchemaHealthService tenantSchemaHealthService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PlatformAuditService platformAuditService;
    private final ObjectMapper objectMapper;
    private final TenantTransactionExecutor tenantTransactionExecutor;
    private static final String DEFAULT_EMAIL_TEMPLATE_KEY = "platform_email";
    private static final Set<String> TEMPLATE_KEY_FALLBACKS = Set.of(
            "platform_notification_email",
            "platform_notification",
            "platform_broadcast_email"
    );
    @Value("${superadmin.jobs.notifications.enabled:true}")
    private boolean notificationsEnabled;
    private volatile Boolean notificationsEnabledOverride;
    private static final List<String> PLATFORM_TRIGGER_EVENT_TYPES = List.of(
            "client_created",
            "client_assigned",
            "session_scheduled",
            "session_rescheduled",
            "session_cancelled",
            "session_overdue",
            "task_assigned",
            "task_overdue",
            "checklist_assigned",
            "checklist_completed",
            "form_assigned",
            "form_completed",
            "document_uploaded",
            "assessment_assigned",
            "assessment_completed"
    );

    @Transactional
    public PlatformNotificationJob createJob(
            String type,
            String title,
            String message,
            String channel,
            Instant scheduledAt,
            Map<String, Object> target,
            Long actorAuthId
    ) {
        Instant now = Instant.now();
        PlatformNotificationJob job = PlatformNotificationJob.builder()
                .jobType(type.toUpperCase(Locale.ROOT))
                .title(title)
                .message(message)
                .channel(channel != null && !channel.isBlank() ? channel : "in_app")
                .status("queued")
                .scheduledAt(scheduledAt != null ? scheduledAt : now)
                .createdByAuthId(actorAuthId)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            job.setTargetJson(objectMapper.writeValueAsString(target != null ? target : Map.of()));
        } catch (Exception e) {
            job.setTargetJson("{}");
        }
        return jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public List<PlatformNotificationJob> getHistory() {
        return jobRepository.findTop200ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<PlatformNotificationJob> getHistory(Long organisationId) {
        List<PlatformNotificationJob> jobs = getHistory();
        if (organisationId == null) {
            return jobs;
        }
        return jobs.stream()
                .filter(job -> matchesOrganisation(job, organisationId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SuperAdminNotificationHistoryItemResponse> getHistoryWithReadState(Long organisationId, Long authId) {
        List<PlatformNotificationJob> jobs = getHistory(organisationId);
        if (jobs.isEmpty()) {
            return List.of();
        }

        Map<Long, PlatformNotificationReadReceipt> receiptByJobId = new HashMap<>();
        if (authId != null) {
            List<Long> ids = jobs.stream().map(PlatformNotificationJob::getId).toList();
            readReceiptRepository.findByAuthIdAndNotificationJob_IdIn(authId, ids)
                    .forEach(r -> receiptByJobId.put(r.getNotificationJob().getId(), r));
        }

        return jobs.stream()
                .map(job -> {
                    PlatformNotificationReadReceipt receipt = receiptByJobId.get(job.getId());
                    return SuperAdminNotificationHistoryItemResponse.builder()
                            .id(job.getId())
                            .jobType(job.getJobType())
                            .title(job.getTitle())
                            .message(job.getMessage())
                            .targetJson(job.getTargetJson())
                            .channel(job.getChannel())
                            .status(job.getStatus())
                            .scheduledAt(job.getScheduledAt())
                            .sentAt(job.getSentAt())
                            .errorMessage(job.getErrorMessage())
                            .createdByAuthId(job.getCreatedByAuthId())
                            .createdAt(job.getCreatedAt())
                            .updatedAt(job.getUpdatedAt())
                            .isRead(receipt != null)
                            .readAt(receipt != null ? receipt.getReadAt() : null)
                            .build();
                })
                .toList();
    }

    @Transactional
    public boolean markJobAsRead(Long jobId, Long authId) {
        if (authId == null) {
            return false;
        }
        PlatformNotificationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Notification job not found"));

        Optional<PlatformNotificationReadReceipt> existing = readReceiptRepository.findByAuthIdAndNotificationJob_Id(authId, jobId);
        if (existing.isPresent()) {
            return false;
        }

        Instant now = Instant.now();
        readReceiptRepository.save(PlatformNotificationReadReceipt.builder()
                .notificationJob(job)
                .authId(authId)
                .readAt(now)
                .createdAt(now)
                .build());
        return true;
    }

    @Transactional
    public long markAllAsRead(Long authId, Long organisationId) {
        if (authId == null) {
            return 0;
        }
        List<PlatformNotificationJob> jobs = getHistory(organisationId);
        if (jobs.isEmpty()) {
            return 0;
        }

        List<Long> ids = jobs.stream().map(PlatformNotificationJob::getId).toList();
        List<Long> alreadyReadIds = readReceiptRepository.findByAuthIdAndNotificationJob_IdIn(authId, ids).stream()
                .map(r -> r.getNotificationJob().getId())
                .toList();
        Set<Long> readSet = Set.copyOf(alreadyReadIds);

        Instant now = Instant.now();
        List<PlatformNotificationReadReceipt> toSave = jobs.stream()
                .filter(job -> !readSet.contains(job.getId()))
                .map(job -> PlatformNotificationReadReceipt.builder()
                        .notificationJob(job)
                        .authId(authId)
                        .readAt(now)
                        .createdAt(now)
                        .build())
                .toList();

        if (toSave.isEmpty()) {
            return 0;
        }
        readReceiptRepository.saveAll(toSave);
        return toSave.size();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long authId, Long organisationId) {
        if (authId == null) {
            return 0;
        }
        List<PlatformNotificationJob> jobs = getHistory(organisationId);
        if (jobs.isEmpty()) {
            return 0;
        }
        List<Long> ids = jobs.stream().map(PlatformNotificationJob::getId).toList();
        long readCount = readReceiptRepository.countByAuthIdAndNotificationJob_IdIn(authId, ids);
        return Math.max(0, jobs.size() - readCount);
    }

    @Transactional
    public PlatformNotificationTemplate upsertTemplate(
            String templateKey,
            String subject,
            String body,
            Boolean active,
            Long actorAuthId
    ) {
        Instant now = Instant.now();
        PlatformNotificationTemplate row = templateRepository.findByTemplateKey(templateKey)
                .orElseGet(() -> PlatformNotificationTemplate.builder()
                        .templateKey(templateKey)
                        .createdAt(now)
                        .build());
        row.setSubjectTemplate(subject);
        row.setBodyTemplate(body);
        row.setIsActive(active != null ? active : Boolean.TRUE);
        row.setUpdatedByAuthId(actorAuthId);
        row.setUpdatedAt(now);
        return templateRepository.save(row);
    }

    @Transactional(readOnly = true)
    public List<PlatformNotificationTemplate> getTemplates() {
        return templateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PlatformNotificationTemplate getTemplateByKey(String templateKey) {
        return templateRepository.findByTemplateKey(templateKey)
                .orElseThrow(() -> new StoryApiException(
                        HttpStatus.NOT_FOUND,
                        "TEMPLATE_NOT_FOUND",
                        "Email template not found: " + templateKey
                ));
    }

    @Transactional
    public void deleteTemplateByKey(String templateKey) {
        PlatformNotificationTemplate template = templateRepository.findByTemplateKey(templateKey)
                .orElseThrow(() -> new StoryApiException(
                        HttpStatus.NOT_FOUND,
                        "TEMPLATE_NOT_FOUND",
                        "Email template not found: " + templateKey
                ));
        templateRepository.delete(template);
    }

    @Scheduled(fixedDelayString = "${superadmin.notifications.dispatch-ms:30000}")
    @Transactional
    public void dispatchQueuedJobs() {
        if (!isNotificationsEnabled()) {
            return;
        }
        dispatchQueuedJobsInternal();
    }

    @Transactional
    public void runNotificationsDispatchNow() {
        dispatchQueuedJobsInternal();
    }

    public void setNotificationsEnabledOverride(Boolean enabled) {
        this.notificationsEnabledOverride = enabled;
    }

    public Boolean getNotificationsEnabledOverride() {
        return notificationsEnabledOverride;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabledOverride != null ? notificationsEnabledOverride : notificationsEnabled;
    }

    private void dispatchQueuedJobsInternal() {
        Instant now = Instant.now();
        List<PlatformNotificationJob> queue = jobRepository
                .findByStatusInAndScheduledAtLessThanEqualOrderByScheduledAtAsc(List.of("queued", "scheduled"), now);
        for (PlatformNotificationJob job : queue) {
            try {
                List<Long> targets = resolveTargets(job);
                int deliveredOrgs = 0;
                int deliveredInApp = 0;
                int deliveredEmails = 0;
                for (Long organisationId : targets) {
                    DeliverySummary summary = dispatchToOrganisation(job, organisationId);
                    if (summary != null) {
                        deliveredOrgs++;
                        deliveredInApp += summary.inAppCount();
                        deliveredEmails += summary.emailCount();
                    }
                }
                job.setStatus("sent");
                job.setSentAt(now);
                job.setUpdatedAt(now);
                jobRepository.save(job);
                platformAuditService.log(
                        job.getCreatedByAuthId(),
                        "PLATFORM_NOTIFICATION_SENT",
                        "PlatformNotificationJob",
                        String.valueOf(job.getId()),
                        "type=" + job.getJobType() + ", targets=" + targets.size() + ", deliveredOrgs=" + deliveredOrgs +
                                ", inApp=" + deliveredInApp + ", emails=" + deliveredEmails + ", channel=" + job.getChannel()
                );
            } catch (Exception e) {
                log.error("Failed to dispatch platform notification job id={}", job.getId(), e);
                job.setStatus("failed");
                job.setErrorMessage(e.getMessage());
                job.setUpdatedAt(now);
                jobRepository.save(job);
            }
        }
    }

    private List<Long> resolveTargets(PlatformNotificationJob job) {
        try {
            Map<?, ?> map = objectMapper.readValue(job.getTargetJson(), Map.class);
            if ("TARGETED".equalsIgnoreCase(job.getJobType())) {
                Object ids = map.get("organisationIds");
                if (ids instanceof List<?> list) {
                    return list.stream().map(v -> Long.valueOf(String.valueOf(v))).collect(Collectors.toList());
                }
            }
            if ("BROADCAST".equalsIgnoreCase(job.getJobType()) || "SCHEDULED".equalsIgnoreCase(job.getJobType())) {
                return organisationRepository.findAll().stream()
                        .filter(o -> "ACTIVE".equalsIgnoreCase(o.getStatus()))
                        .map(Organisation::getId)
                        .collect(Collectors.toList());
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private boolean matchesOrganisation(PlatformNotificationJob job, Long organisationId) {
        if (job == null || organisationId == null) {
            return false;
        }
        String type = job.getJobType() != null ? job.getJobType().trim().toUpperCase(Locale.ROOT) : "";
        if ("BROADCAST".equals(type) || "SCHEDULED".equals(type)) {
            return true;
        }
        List<Long> targets = resolveTargets(job);
        return targets.contains(organisationId);
    }

    private DeliverySummary dispatchToOrganisation(PlatformNotificationJob job, Long organisationId) {
        Organisation organisation = organisationRepository.findById(organisationId).orElse(null);
        if (organisation == null) {
            return null;
        }
        String schema = organisation.getSchemaName();
        if (schema == null || schema.isBlank() || "public".equalsIgnoreCase(schema) || !tenantSchemaHealthService.schemaExists(schema)) {
            return null;
        }
        return tenantTransactionExecutor.executeWrite(organisationId, schema, () -> {
            List<User> users = userRepository.findAll().stream()
                    .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                    .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                    .toList();
            int inAppCount = 0;
            int emailCount = 0;
            String normalizedChannel = job.getChannel() != null ? job.getChannel().trim().toLowerCase(Locale.ROOT) : "in_app";
            boolean pushInApp = "in_app".equals(normalizedChannel) || "both".equals(normalizedChannel) || "all".equals(normalizedChannel);
            boolean pushEmail = "email".equals(normalizedChannel) || "both".equals(normalizedChannel) || "all".equals(normalizedChannel);

            if (pushInApp) {
                for (User user : users) {
                    Notification notification = Notification.builder()
                            .user(user)
                            .type(NotificationType.SYSTEM_UPGRADE)
                            .category(NotificationCategory.SYSTEM)
                            .title(job.getTitle())
                            .message(job.getMessage())
                            .priority(NotificationPriority.MEDIUM)
                            .isRead(false)
                            .relatedEntityType("platform_notification_job")
                            .relatedEntityId(job.getId())
                            .data("{\"organisationId\":" + organisationId + "}")
                            .emailSent(false)
                            .build();
                    notificationRepository.save(notification);
                    inAppCount++;
                }
            }

            if (pushEmail) {
                PlatformNotificationTemplate emailTemplate = resolveEmailTemplate(job);
                String subjectTemplate = emailTemplate != null ? emailTemplate.getSubjectTemplate() : null;
                String bodyTemplate = emailTemplate != null ? emailTemplate.getBodyTemplate() : null;
                for (User user : users) {
                    String to = user.getEmail();
                    if (to == null || to.isBlank()) {
                        continue;
                    }
                    Map<String, Object> templateData = Map.of(
                            "recipientName", Objects.requireNonNullElse(user.getFullName(), "Team"),
                            "message", Objects.requireNonNullElse(job.getMessage(), ""),
                            "title", Objects.requireNonNullElse(job.getTitle(), "Platform Notification"),
                            "orgName", Objects.requireNonNullElse(organisation.getName(), ""),
                            "organisationName", Objects.requireNonNullElse(organisation.getName(), "")
                    );
                    String subject = renderTemplate(subjectTemplate, templateData, job.getTitle());
                    String body = bodyTemplate != null
                            ? renderTemplate(bodyTemplate, templateData, "")
                            : buildBroadcastEmailBody(user.getFullName(), job.getMessage());
                    emailService.sendEmail(to, subject, body);
                    emailCount++;
                }
            }
            return new DeliverySummary(inAppCount, emailCount);
        });
    }

    private String buildBroadcastEmailBody(String recipientName, String message) {
        String name = Objects.requireNonNullElse(recipientName, "Team");
        return """
                <div style="font-family: Arial, sans-serif; max-width: 640px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #1f2937;">Platform Notification</h2>
                    <p>Hi %s,</p>
                    <p>%s</p>
                    <p style="margin-top: 24px; color: #6b7280; font-size: 12px;">
                        This message was sent by your platform administrator.
                    </p>
                </div>
                """.formatted(name, message == null ? "" : message);
    }

    private PlatformNotificationTemplate resolveEmailTemplate(PlatformNotificationJob job) {
        if (job == null) {
            return null;
        }
        String jobType = job.getJobType() != null ? job.getJobType().trim().toLowerCase(Locale.ROOT) : "";
        List<String> candidates = List.of(
                jobType + "_email",
                jobType,
                DEFAULT_EMAIL_TEMPLATE_KEY,
                "platform_notification_email",
                "platform_notification"
        );
        for (String key : candidates) {
            if (key == null || key.isBlank()) {
                continue;
            }
            Optional<PlatformNotificationTemplate> template = templateRepository.findByTemplateKey(key);
            if (template.isPresent() && Boolean.TRUE.equals(template.get().getIsActive())) {
                return template.get();
            }
        }
        for (String fallbackKey : TEMPLATE_KEY_FALLBACKS) {
            Optional<PlatformNotificationTemplate> template = templateRepository.findByTemplateKey(fallbackKey);
            if (template.isPresent() && Boolean.TRUE.equals(template.get().getIsActive())) {
                return template.get();
            }
        }
        return null;
    }

    private String renderTemplate(String template, Map<String, Object> data, String fallback) {
        if (template == null || template.isBlank()) {
            return fallback;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return result;
    }

    private record DeliverySummary(int inAppCount, int emailCount) {
    }

    @Transactional(readOnly = true)
    public List<NotificationTriggerResponse> getGlobalTriggers() {
        return triggerRepository.findAllOrdered().stream()
                .map(this::toTriggerResponse)
                .toList();
    }

    @Transactional
    public NotificationTriggerResponse createGlobalTrigger(NotificationTriggerRequest request) {
        Instant now = Instant.now();
        PlatformNotificationTrigger trigger = PlatformNotificationTrigger.builder()
                .name(request.getName())
                .description(request.getDescription())
                .eventType(request.getEventType())
                .entityType(request.getEntityType() != null ? request.getEntityType().name() : null)
                .conditionRules(request.getConditionRules())
                .recipientRules(request.getRecipientRules())
                .priority(request.getPriority() != null ? request.getPriority() : "medium")
                .isScheduled(request.getIsScheduled() != null ? request.getIsScheduled() : false)
                .delayMinutes(request.getScheduleOffsetMinutes() != null ? request.getScheduleOffsetMinutes() : 0)
                .batchWindowMinutes(request.getBatchWindowMinutes() != null ? request.getBatchWindowMinutes() : 5)
                .maxBatchSize(request.getMaxBatchSize() != null ? request.getMaxBatchSize() : 10)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return toTriggerResponse(triggerRepository.save(trigger));
    }

    @Transactional
    public NotificationTriggerResponse updateGlobalTrigger(Long triggerId, NotificationTriggerRequest request) {
        PlatformNotificationTrigger trigger = triggerRepository.findById(triggerId)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "TRIGGER_NOT_FOUND", "Notification trigger not found"));

        if (request.getName() != null && !request.getName().isBlank()) {
            trigger.setName(request.getName());
        }
        if (request.getDescription() != null) {
            trigger.setDescription(request.getDescription());
        }
        if (request.getEventType() != null && !request.getEventType().isBlank()) {
            trigger.setEventType(request.getEventType());
        }
        if (request.getEntityType() != null) {
            trigger.setEntityType(request.getEntityType().name());
        }
        if (request.getConditionRules() != null) {
            trigger.setConditionRules(request.getConditionRules());
        }
        if (request.getRecipientRules() != null) {
            trigger.setRecipientRules(request.getRecipientRules());
        }
        if (request.getPriority() != null) {
            trigger.setPriority(request.getPriority());
        }
        if (request.getIsScheduled() != null) {
            trigger.setIsScheduled(request.getIsScheduled());
        }
        if (request.getScheduleOffsetMinutes() != null) {
            trigger.setDelayMinutes(request.getScheduleOffsetMinutes());
        }
        if (request.getBatchWindowMinutes() != null) {
            trigger.setBatchWindowMinutes(request.getBatchWindowMinutes());
        }
        if (request.getMaxBatchSize() != null) {
            trigger.setMaxBatchSize(request.getMaxBatchSize());
        }
        if (request.getIsActive() != null) {
            trigger.setIsActive(request.getIsActive());
        }
        trigger.setUpdatedAt(Instant.now());
        return toTriggerResponse(triggerRepository.save(trigger));
    }

    @Transactional
    public void deleteGlobalTrigger(Long triggerId) {
        if (!triggerRepository.existsById(triggerId)) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "TRIGGER_NOT_FOUND", "Notification trigger not found");
        }
        triggerRepository.deleteById(triggerId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTriggerMetadata() {
        return Map.of(
                "eventTypes", PLATFORM_TRIGGER_EVENT_TYPES,
                "entityTypes", Arrays.stream(com.smart.therapy.flow.notification.enums.EntityType.values())
                        .map(Enum::name)
                        .toList()
        );
    }

    private NotificationTriggerResponse toTriggerResponse(PlatformNotificationTrigger trigger) {
        com.smart.therapy.flow.notification.enums.EntityType entityType = null;
        if (trigger.getEntityType() != null && !trigger.getEntityType().isBlank()) {
            try {
                entityType = com.smart.therapy.flow.notification.enums.EntityType.valueOf(trigger.getEntityType());
            } catch (IllegalArgumentException ignored) {
                entityType = null;
            }
        }
        return NotificationTriggerResponse.builder()
                .id(trigger.getId())
                .name(trigger.getName())
                .description(trigger.getDescription())
                .eventType(trigger.getEventType())
                .entityType(entityType)
                .conditionRules(trigger.getConditionRules())
                .recipientRules(trigger.getRecipientRules())
                .priority(trigger.getPriority())
                .isScheduled(trigger.getIsScheduled())
                .scheduleOffsetMinutes(trigger.getDelayMinutes())
                .batchWindowMinutes(trigger.getBatchWindowMinutes())
                .maxBatchSize(trigger.getMaxBatchSize())
                .isActive(trigger.getIsActive())
                .createdAt(trigger.getCreatedAt())
                .updatedAt(trigger.getUpdatedAt())
                .build();
    }
}
