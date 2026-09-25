package com.smart.therapy.flow.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.service.EmailAppLinks;
import com.smart.therapy.flow.common.service.EmailHtmlComponents;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientContactService;
import com.smart.therapy.flow.notification.dto.*;
import com.smart.therapy.flow.notification.entity.NotificationPreference;
import com.smart.therapy.flow.notification.entity.NotificationActionMetadata;
import com.smart.therapy.flow.notification.repository.NotificationPreferenceRepository;
import com.smart.therapy.flow.notification.entity.Notification;
import com.smart.therapy.flow.notification.entity.NotificationDeliveryLog;
import com.smart.therapy.flow.notification.entity.NotificationTemplate;
import com.smart.therapy.flow.notification.entity.NotificationTrigger;
import com.smart.therapy.flow.notification.entity.ScheduledNotification;
import com.smart.therapy.flow.notification.enums.NotificationChannel;
import com.smart.therapy.flow.notification.enums.NotificationCategory;
import com.smart.therapy.flow.notification.enums.NotificationPriority;
import com.smart.therapy.flow.notification.enums.NotificationStatus;
import com.smart.therapy.flow.notification.enums.NotificationType;
import com.smart.therapy.flow.notification.enums.ScheduledNotificationStatus;
import com.smart.therapy.flow.notification.repository.NotificationDeliveryLogRepository;
import com.smart.therapy.flow.notification.repository.NotificationRepository;
import com.smart.therapy.flow.notification.repository.NotificationActionMetadataRepository;
import com.smart.therapy.flow.notification.repository.NotificationTemplateRepository;
import com.smart.therapy.flow.notification.repository.NotificationTriggerRepository;
import com.smart.therapy.flow.notification.repository.ScheduledNotificationRepository;
import com.smart.therapy.flow.common.tenant.TenantExecutionService;
import com.smart.therapy.flow.user.entity.SupervisorAssignment;
import com.smart.therapy.flow.user.repository.SupervisorAssignmentRepository;
import com.smart.therapy.flow.system.entity.SystemOption;
import com.smart.therapy.flow.system.dto.PracticeConfigurationResponse;
import com.smart.therapy.flow.system.repository.OptionCategoryRepository;
import com.smart.therapy.flow.system.repository.SystemOptionRepository;
import com.smart.therapy.flow.system.service.PracticeConfigurationService;
import com.smart.therapy.flow.session.repository.SessionRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.hibernate.exception.SQLGrammarException;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private static final int BROADCAST_BATCH_SIZE = 500;
    private static final ScheduledNotificationStatus STATUS_PENDING = ScheduledNotificationStatus.PENDING;
    private static final ScheduledNotificationStatus STATUS_SENT = ScheduledNotificationStatus.COMPLETED;
    private static final ScheduledNotificationStatus STATUS_FAILED = ScheduledNotificationStatus.FAILED;

    /**
     * Session/clinical events: admins, supervisors, the assigned therapist, their supervisor, and the client.
     * Therapists are NOT broadcast by role — only the therapist linked in the event payload is notified.
     */
    private static final String CLINICAL_EVENT_RECIPIENT_RULES = """
            {"roles":["ADMIN","SUPERVISOR"],"assignedTherapist":true,"supervisorOfTherapist":true,"sessionClient":true}
            """;

    /**
     * Client/workflow events (assessments, tasks, forms, etc.): scoped staff only, plus assigned therapist.
     */
    private static final String WORKFLOW_EVENT_RECIPIENT_RULES = """
            {"roles":["ADMIN","SUPERVISOR"],"assignedTherapist":true,"supervisorOfTherapist":true}
            """;

    /**
     * Events whose payload clientId may be used as a recipient when a trigger carries no recipient
     * rules at all. Everything else (tasks, comments, internal workflow chatter) must never fall back
     * to emailing the client: their payloads carry clientId only to say which chart the work is about.
     */
    private static final Set<String> CLIENT_FALLBACK_EVENTS = Set.of(
            NotificationEventCatalog.SESSION_SCHEDULED,
            NotificationEventCatalog.SESSION_SERIES_SCHEDULED,
            NotificationEventCatalog.SESSION_RESCHEDULED,
            NotificationEventCatalog.SESSION_CANCELLED,
            NotificationEventCatalog.SESSION_REMINDER,
            NotificationEventCatalog.SESSION_OVERDUE,
            NotificationEventCatalog.SESSION_COMPLETED,
            NotificationEventCatalog.BILL_GENERATED,
            NotificationEventCatalog.BILL_DUE_REMINDER,
            NotificationEventCatalog.PAYMENT_RECEIVED,
            NotificationEventCatalog.PAYMENT_FAILED);

    private final NotificationRepository notificationRepository;
    private final NotificationActionMetadataRepository notificationActionMetadataRepository;
    private final NotificationTriggerRepository triggerRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationDeliveryLogRepository deliveryLogRepository;
    private final ScheduledNotificationRepository scheduledNotificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ClientContactService clientContactService;
    private final SupervisorAssignmentRepository supervisorAssignmentRepository;
    private final OptionCategoryRepository optionCategoryRepository;
    private final SystemOptionRepository systemOptionRepository;
    private final PracticeConfigurationService practiceConfigurationService;
    private final SessionRepository sessionRepository;
    private final TenantExecutionService tenantExecutionService;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock clock = Clock.systemUTC();
    private volatile PracticeSettings practiceSettingsCache;
    private final Set<String> repairedTherapistRuleTenants = ConcurrentHashMap.newKeySet();

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired(required = false)
    private com.smart.therapy.flow.common.service.EmailProviderService emailProviderService;

    @Autowired(required = false)
    private com.smart.therapy.flow.common.service.EmailService commonEmailService;

    @Autowired(required = false)
    @Qualifier("notificationExecutor")
    private Executor notificationExecutor;

    @Autowired(required = false)
    private SmsNotificationService smsNotificationService;

    @Autowired(required = false)
    private com.smart.therapy.flow.common.service.TimezoneService timezoneService;

    @Value("${app.frontend.staff-login-url:${APP_FRONTEND_STAFF_LOGIN_URL:https://app.therapyflow.pro/auth/therapist/login}}")
    private String staffLoginUrl;

    @Transactional
    public void processEvent(String eventType, Map<String, Object> entityData) {
        processEvent(eventType, entityData, new HashSet<>(), new HashSet<>());
    }

    /**
     * Process several event types raised by one domain action (e.g. task_comment_added and its
     * legacy alias comment_added for a single comment) while sharing the claimed-recipient sets,
     * so a recipient reached by an earlier event type is not notified again by a later one. Event
     * types are processed in order, making the first one canonical; later ones still fire for any
     * recipients only their own triggers resolve (e.g. a tenant-customized comment_added trigger).
     */
    @Transactional
    public void processEvents(List<String> eventTypes, Map<String, Object> entityData) {
        Set<Long> claimedUserIds = new HashSet<>();
        Set<Long> claimedClientIds = new HashSet<>();
        for (String eventType : eventTypes) {
            processEvent(eventType, entityData, claimedUserIds, claimedClientIds);
        }
    }

    private void processEvent(String eventType, Map<String, Object> entityData, Set<Long> claimedUserIds,
            Set<Long> claimedClientIds) {
        ensureDefaultNotificationSetup(eventType);
        Map<String, Object> payload = prepareEntityData(eventType, entityData);
        List<NotificationTrigger> triggers = triggerRepository.findByEventType(eventType);

        // Guard against duplicate notifications: a single recipient must receive at most one
        // notification per event. Multiple active trigger rows can exist for the same event type
        // (e.g. legacy/duplicate rows, or the non-atomic check-then-insert in ensureTrigger), and
        // without this de-duplication each duplicate trigger would create its own copy, producing
        // several identical notifications for one action (e.g. one session creation). The claimed
        // sets are passed in so processEvents can extend the guard across alias event types too.
        for (NotificationTrigger trigger : triggers) {
            if (!Boolean.TRUE.equals(trigger.getIsActive())) {
                continue;
            }

            if (!matchesConditionRules(trigger.getConditionRules(), payload)) {
                continue;
            }

            RecipientTargets recipients = determineRecipients(trigger, payload);
            if (recipients.isEmpty()) {
                log.debug("No recipients resolved for event {} and trigger {}", eventType, trigger.getName());
                continue;
            }

            RecipientTargets freshRecipients = claimNewRecipients(recipients, claimedUserIds, claimedClientIds);
            if (freshRecipients.isEmpty()) {
                log.debug("Skipping duplicate trigger {} for event {}; all recipients already notified by an earlier trigger",
                        trigger.getName(), eventType);
                continue;
            }

            if (Boolean.TRUE.equals(trigger.getIsScheduled())) {
                scheduleNotifications(trigger, freshRecipients, payload);
            } else {
                dispatchDelivery(trigger, freshRecipients, payload);
            }
        }
    }

    /**
     * Returns the subset of recipients that have not yet been claimed for the current event, while
     * recording the returned recipients as claimed. This ensures that when several triggers resolve
     * to overlapping (or identical) recipients for the same event, each recipient is notified only
     * once.
     */
    private RecipientTargets claimNewRecipients(RecipientTargets recipients, Set<Long> claimedUserIds,
            Set<Long> claimedClientIds) {
        List<Long> freshUserIds = new ArrayList<>();
        for (Long userId : recipients.getUserIds()) {
            if (userId != null && claimedUserIds.add(userId)) {
                freshUserIds.add(userId);
            }
        }
        List<Long> freshClientIds = new ArrayList<>();
        for (Long clientId : recipients.getClientIds()) {
            if (clientId != null && claimedClientIds.add(clientId)) {
                freshClientIds.add(clientId);
            }
        }
        return new RecipientTargets(freshUserIds, freshClientIds);
    }

    /**
     * Process a notification event without holding the caller's transaction.
     * When invoked inside an active TX (e.g. after billing/session save), work is deferred
     * until after commit so nested writes cannot deadlock on locked parent rows.
     */
    public void processEventInNewTransaction(String eventType, Map<String, Object> entityData) {
        processEventsInNewTransaction(List.of(eventType), entityData);
    }

    /**
     * Multi-event variant of {@link #processEventInNewTransaction}: all event types run in one
     * new transaction through {@link #processEvents}, so recipients are de-duplicated across the
     * alias event types of a single domain action instead of being notified once per event type.
     */
    public void processEventsInNewTransaction(List<String> eventTypes, Map<String, Object> entityData) {
        Map<String, Object> payload = entityData == null ? new HashMap<>() : new HashMap<>(entityData);
        Runnable run = () -> {
            try {
                // afterCommit still has the original transaction resources bound. REQUIRED
                // would reuse them, so explicitly suspend them for notification writes.
                TransactionTemplate notificationTransaction = new TransactionTemplate(
                        Objects.requireNonNull(transactionTemplate.getTransactionManager()));
                notificationTransaction.setPropagationBehavior(
                        org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                notificationTransaction.executeWithoutResult(status -> processEvents(eventTypes, payload));
            } catch (Exception e) {
                log.error("Failed to process notification event types={}: {}", eventTypes, e.getMessage(), e);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    run.run();
                }
            });
            return;
        }

        run.run();
    }

    /**
     * Dispatch notification delivery. Because {@link #processEvent} runs inside a transaction that may
     * have just auto-created the trigger/template rows (lazy setup), delivery MUST run only after that
     * transaction commits; otherwise the async delivery thread runs in a separate transaction and cannot
     * see the freshly-created templates, silently dropping the first notification per event type.
     */
    private void dispatchDelivery(NotificationTrigger trigger, RecipientTargets recipients,
            Map<String, Object> payload) {
        final Long orgId = com.smart.therapy.flow.common.tenant.TenantContext.getOrganisationId();
        final String schemaName = com.smart.therapy.flow.common.tenant.TenantContext.getSchemaName();
        Runnable submit = () -> {
            if (notificationExecutor != null) {
                notificationExecutor.execute(() -> {
                    try {
                        com.smart.therapy.flow.common.tenant.TenantContext.setOrganisationId(orgId);
                        com.smart.therapy.flow.common.tenant.TenantContext.setSchemaName(schemaName);
                        deliverNotifications(trigger, recipients, payload);
                    } catch (Exception e) {
                        log.error("Error processing notification asynchronously: trigger={}, correlationId={}",
                                trigger.getName(), org.slf4j.MDC.get("correlationId"), e);
                    } finally {
                        com.smart.therapy.flow.common.tenant.TenantContext.clear();
                    }
                });
            } else {
                try {
                    com.smart.therapy.flow.common.tenant.TenantContext.setOrganisationId(orgId);
                    com.smart.therapy.flow.common.tenant.TenantContext.setSchemaName(schemaName);
                    deliverNotifications(trigger, recipients, payload);
                } catch (Exception e) {
                    log.error("Error processing notification: trigger={}", trigger.getName(), e);
                } finally {
                    com.smart.therapy.flow.common.tenant.TenantContext.clear();
                }
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submit.run();
                }
            });
        } else {
            submit.run();
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(AuthPrincipal requester, Boolean unreadOnly) {
        Objects.requireNonNull(requester, "Requester is required");
        return getUserNotifications(resolveNotificationUserId(requester), unreadOnly);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId, Boolean unreadOnly) {
        Objects.requireNonNull(userId, "User id is required");

        List<Notification> notifications;
        if (Boolean.TRUE.equals(unreadOnly)) {
            notifications = notificationRepository.findByUserIdAndIsReadAndIsDeletedOrderByCreatedAtDesc(userId, false, false);
        } else {
            notifications = notificationRepository.findByUserIdAndIsDeletedOrderByCreatedAtDesc(userId, false);
        }

        return notifications.stream()
                .map(this::toNotificationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getTenantNotifications(AuthPrincipal requester, Long userId, Boolean unreadOnly) {
        Objects.requireNonNull(requester, "Requester is required");
        if (shouldRestrictToOwnNotificationsOnly(requester)) {
            Long requesterUserId = resolveNotificationUserId(requester);
            if (userId != null && !Objects.equals(userId, requesterUserId)) {
                throw new ForbiddenException("You can only view your own notifications");
            }
            return getUserNotifications(requesterUserId, unreadOnly);
        }

        final List<Notification> notifications;
        if (userId != null) {
            notifications = Boolean.TRUE.equals(unreadOnly)
                    ? notificationRepository.findByUserIdAndIsReadAndIsDeletedOrderByCreatedAtDesc(userId, false, false)
                    : notificationRepository.findByUserIdAndIsDeletedOrderByCreatedAtDesc(userId, false);
        } else if (Boolean.TRUE.equals(unreadOnly)) {
            notifications = notificationRepository.findByIsRead(false).stream()
                    .filter(n -> !Boolean.TRUE.equals(n.getIsDeleted()))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .collect(Collectors.toList());
        } else {
            notifications = notificationRepository.findByIsDeletedOrderByCreatedAtDesc(false);
        }
        return notifications.stream().map(this::toNotificationResponse).collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId, AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");
        markAsRead(notificationId, resolveNotificationUserId(requester));
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Objects.requireNonNull(userId, "User id is required");
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (notification.getUser() == null || !Objects.equals(notification.getUser().getId(), userId)) {
            throw new ForbiddenException("Notification does not belong to user");
        }

        notification.setIsRead(true);
        notification.setReadAt(Instant.now(clock));
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");
        markAllAsRead(resolveNotificationUserId(requester));
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        Objects.requireNonNull(userId, "User id is required");
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadAndIsDeletedOrderByCreatedAtDesc(userId, false, false);
        Instant now = Instant.now(clock);
        for (Notification notification : notifications) {
            notification.setIsRead(true);
            notification.setReadAt(now);
        }
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void markAllAsReadForUser(Long targetUserId) {
        markAllAsRead(targetUserId);
    }

    @Transactional
    public void markAsReadForUser(Long notificationId, Long targetUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (notification.getUser() == null || !Objects.equals(notification.getUser().getId(), targetUserId)) {
            throw new RuntimeException("Notification does not belong to user");
        }
        notification.setIsRead(true);
        notification.setReadAt(Instant.now(clock));
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");
        return getUnreadCount(resolveNotificationUserId(requester));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        Objects.requireNonNull(userId, "User id is required");
        return notificationRepository.findByUserIdAndIsReadAndIsDeletedOrderByCreatedAtDesc(userId, false, false).size();
    }

    @Transactional(readOnly = true)
    public List<NotificationTriggerResponse> getAllTriggers() {
        return triggerRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt", "id"))
                .stream()
                .map(this::toTriggerResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationTriggerResponse createTrigger(NotificationTriggerRequest request) {
        NotificationTrigger trigger = NotificationTrigger.builder()
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .eventType(request.getEventType().trim())
                .entityType(request.getEntityType())
                .conditionRules(normalizeJson(request.getConditionRules()))
                .recipientRules(normalizeJson(request.getRecipientRules()))
                .priority(resolveTriggerPriority(request.getPriority()))
                .isScheduled(Optional.ofNullable(request.getIsScheduled()).orElse(false))
                .delayMinutes(Optional.ofNullable(request.getScheduleOffsetMinutes()).orElse(0))
                .batchWindowMinutes(Optional.ofNullable(request.getBatchWindowMinutes()).orElse(5))
                .maxBatchSize(Optional.ofNullable(request.getMaxBatchSize()).orElse(10))
                .isActive(Optional.ofNullable(request.getIsActive()).orElse(true))
                .build();
        return toTriggerResponse(triggerRepository.save(trigger));
    }

    @Transactional
    public NotificationTriggerResponse updateTrigger(Long triggerId, NotificationTriggerRequest request) {
        NotificationTrigger trigger = triggerRepository.findById(triggerId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification trigger not found"));

        if (!isBlank(request.getName())) {
            trigger.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            trigger.setDescription(trimToNull(request.getDescription()));
        }
        if (!isBlank(request.getEventType())) {
            trigger.setEventType(request.getEventType().trim());
        }
        if (request.getEntityType() != null) {
            trigger.setEntityType(request.getEntityType());
        }
        if (request.getConditionRules() != null) {
            trigger.setConditionRules(normalizeJson(request.getConditionRules()));
        }
        if (request.getRecipientRules() != null) {
            trigger.setRecipientRules(normalizeJson(request.getRecipientRules()));
        }
        if (request.getPriority() != null) {
            trigger.setPriority(resolveTriggerPriority(request.getPriority()));
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

        return toTriggerResponse(triggerRepository.save(trigger));
    }

    @Transactional
    public void deleteTrigger(Long triggerId) {
        triggerRepository.deleteById(triggerId);
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> getAllTemplates() {
        return templateRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt", "id"))
                .stream()
                .map(this::toTemplateResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationTemplateResponse createTemplate(NotificationTemplateRequest request) {
        String channelType = resolveTemplateChannelType(request.getType(), request.getEventType());
        String eventType = resolveTemplateEventType(request.getEventType(), request.getName(), channelType);
        NotificationTemplate template = NotificationTemplate.builder()
                .name(request.getName())
                .type(channelType)
                .eventType(eventType)
                .subject(Optional.ofNullable(request.getSubject()).orElse(""))
                .bodyTemplate(Optional.ofNullable(request.getBodyTemplate()).orElse(""))
                .isSystem(Optional.ofNullable(request.getIsSystem()).orElse(false))
                .isActive(Optional.ofNullable(request.getIsActive()).orElse(true))
                .build();
        return toTemplateResponse(templateRepository.save(template));
    }

    @Transactional
    public NotificationTemplateResponse updateTemplate(Long templateId, NotificationTemplateRequest request) {
        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Notification template not found"));

        if (!isBlank(request.getName())) {
            template.setName(request.getName());
        }
        if (!isBlank(request.getType())) {
            template.setType(normalizeTemplateChannelType(request.getType()));
        }
        if (request.getEventType() != null) {
            String channelType = !isBlank(request.getType())
                    ? normalizeTemplateChannelType(request.getType())
                    : template.getType();
            template.setEventType(resolveTemplateEventType(
                    request.getEventType(),
                    template.getName(),
                    channelType));
        }
        if (request.getSubject() != null) {
            template.setSubject(request.getSubject());
        }
        if (request.getBodyTemplate() != null) {
            template.setBodyTemplate(request.getBodyTemplate());
        }
        if (request.getIsSystem() != null) {
            template.setIsSystem(request.getIsSystem());
        }
        if (request.getIsActive() != null) {
            template.setIsActive(request.getIsActive());
        }

        return toTemplateResponse(templateRepository.save(template));
    }

    @Transactional
    public void deleteTemplate(Long templateId) {
        templateRepository.deleteById(templateId);
    }

    // ========== MISSING ENDPOINT METHODS ==========

    /**
     * Server-initiated notification with no requester behind it, for flows a client action
     * triggers but a client may not author (they cannot write to a therapist's notification
     * feed). Callers must resolve the target user themselves; there is no permission check here,
     * so never expose this straight to a controller.
     */
    @Transactional
    public void createSystemNotification(User user, NotificationType type, String title, String message,
            String actionUrl, String actionLabel) {
        if (user == null || type == null) {
            return;
        }
        notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .category(type.getCategory())
                .title(title)
                .message(message)
                .priority(NotificationPriority.MEDIUM)
                .actionUrl(actionUrl)
                .actionLabel(actionLabel)
                .isRead(false)
                .build());
    }

    @Transactional
    public NotificationResponse createNotification(NotificationRequest request, Long requesterId,
            String requesterRole) {
        String normalizedRole = requesterRole != null ? requesterRole.trim().toUpperCase() : "";
        boolean canCreate = "ADMIN".equals(normalizedRole)
                || "SUPERVISOR".equals(normalizedRole)
                || "ROLE_ADMIN".equals(normalizedRole)
                || "ROLE_SUPERVISOR".equals(normalizedRole)
                || "USER_MANAGE".equals(normalizedRole);
        if (!canCreate) {
            throw new StoryApiException(HttpStatus.FORBIDDEN, "NOTIFICATION_FORBIDDEN",
                    "Insufficient permissions - admin/supervisor required");
        }

        User user = null;
        Client client = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        } else if (request.getClientId() != null) {
            client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "CLIENT_NOT_FOUND", "Client not found"));
        } else {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "NOTIFICATION_TARGET_REQUIRED",
                    "Either userId or clientId is required");
        }

        NotificationCategory category = request.getCategory() != null
                ? request.getCategory()
                : (request.getType() != null ? request.getType().getCategory() : NotificationCategory.SYSTEM);

        NotificationPriority resolvedPriority = NotificationPriority.MEDIUM;
        if (StringUtils.hasText(request.getPriority())) {
            try {
                resolvedPriority = NotificationPriority.valueOf(request.getPriority().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_NOTIFICATION_PRIORITY",
                        "Invalid priority. Allowed values: LOW, MEDIUM, HIGH, URGENT");
            }
        }

        Notification notification = Notification.builder()
                .user(user)
                .client(client)
                .type(request.getType())
                .category(category)
                .title(request.getTitle())
                .message(request.getMessage())
                .data(request.getData())
                .priority(resolvedPriority)
                .actionUrl(request.getActionUrl())
                .actionLabel(request.getActionLabel())
                .relatedEntityType(request.getRelatedEntityType())
                .relatedEntityId(request.getRelatedEntityId())
                .expiresAt(request.getExpiresAt())
                .isRead(false)
                .build();

        return toNotificationResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void createBroadcastNotification(NotificationBroadcastRequest request, Long requesterId, String requesterRole) {
        validateBroadcastAccess(requesterRole);

        Long organisationId = TenantContext.getOrganisationId();
        String schemaName = TenantContext.getSchemaName();
        if (organisationId == null || !StringUtils.hasText(schemaName)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "TENANT_CONTEXT_REQUIRED",
                    "Tenant context is required for broadcast notifications");
        }

        NotificationPriority priority = resolvePriorityOrThrow(request.getPriority());
        NotificationCategory category = request.getCategory() != null
                ? request.getCategory()
                : request.getType().getCategory();
        NotificationTargetType targetType = request.getTargetType();

        Runnable fanoutTask = () -> {
            TenantContext.setOrganisationId(organisationId);
            TenantContext.setSchemaName(schemaName);
            try {
                if (targetType == NotificationTargetType.USERS || targetType == NotificationTargetType.BOTH) {
                    fanoutToUsers(request, category, priority);
                }
                if (targetType == NotificationTargetType.CLIENTS || targetType == NotificationTargetType.BOTH) {
                    fanoutToClients(request, category, priority);
                }
                log.info("Broadcast notification delivered: orgId={}, schema={}, targetType={}, requestedBy={}",
                        organisationId, schemaName, targetType, requesterId);
            } finally {
                TenantContext.clear();
            }
        };

        if (notificationExecutor != null) {
            notificationExecutor.execute(fanoutTask);
        } else {
            fanoutTask.run();
        }
    }

    private void fanoutToUsers(NotificationBroadcastRequest request,
                               NotificationCategory category,
                               NotificationPriority priority) {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .toList();

        List<Notification> batch = new ArrayList<>(BROADCAST_BATCH_SIZE);
        for (User user : users) {
            batch.add(buildBroadcastNotificationForUser(request, user, category, priority));
            flushBatchIfNeeded(batch);
        }
        flushRemaining(batch);
    }

    private void fanoutToClients(NotificationBroadcastRequest request,
                                 NotificationCategory category,
                                 NotificationPriority priority) {
        List<Client> clients = clientRepository.findAll().stream()
                .filter(Client::isActive)
                .toList();

        List<Notification> batch = new ArrayList<>(BROADCAST_BATCH_SIZE);
        for (Client client : clients) {
            batch.add(buildBroadcastNotificationForClient(request, client, category, priority));
            flushBatchIfNeeded(batch);
        }
        flushRemaining(batch);
    }

    private Notification buildBroadcastNotificationForUser(NotificationBroadcastRequest request,
                                                           User user,
                                                           NotificationCategory category,
                                                           NotificationPriority priority) {
        return Notification.builder()
                .user(user)
                .type(request.getType())
                .category(category)
                .title(request.getTitle())
                .message(request.getMessage())
                .data(request.getData())
                .priority(priority)
                .actionUrl(request.getActionUrl())
                .actionLabel(request.getActionLabel())
                .relatedEntityType(request.getRelatedEntityType())
                .relatedEntityId(request.getRelatedEntityId())
                .expiresAt(request.getExpiresAt())
                .isRead(false)
                .build();
    }

    private Notification buildBroadcastNotificationForClient(NotificationBroadcastRequest request,
                                                             Client client,
                                                             NotificationCategory category,
                                                             NotificationPriority priority) {
        return Notification.builder()
                .client(client)
                .type(request.getType())
                .category(category)
                .title(request.getTitle())
                .message(request.getMessage())
                .data(request.getData())
                .priority(priority)
                .actionUrl(request.getActionUrl())
                .actionLabel(request.getActionLabel())
                .relatedEntityType(request.getRelatedEntityType())
                .relatedEntityId(request.getRelatedEntityId())
                .expiresAt(request.getExpiresAt())
                .isRead(false)
                .build();
    }

    private void flushBatchIfNeeded(List<Notification> batch) {
        if (batch.size() >= BROADCAST_BATCH_SIZE) {
            notificationRepository.saveAll(batch);
            batch.clear();
        }
    }

    private void flushRemaining(List<Notification> batch) {
        if (!batch.isEmpty()) {
            notificationRepository.saveAll(batch);
            batch.clear();
        }
    }

    private void validateBroadcastAccess(String requesterRole) {
        String normalizedRole = requesterRole != null ? requesterRole.trim().toUpperCase() : "";
        boolean canCreate = "ADMIN".equals(normalizedRole)
                || "SUPERVISOR".equals(normalizedRole)
                || "ROLE_ADMIN".equals(normalizedRole)
                || "ROLE_SUPERVISOR".equals(normalizedRole)
                || "USER_MANAGE".equals(normalizedRole);
        if (!canCreate) {
            throw new StoryApiException(HttpStatus.FORBIDDEN, "NOTIFICATION_FORBIDDEN",
                    "Insufficient permissions - admin/supervisor required");
        }
    }

    private NotificationPriority resolvePriorityOrThrow(String priorityRaw) {
        if (!StringUtils.hasText(priorityRaw)) {
            return NotificationPriority.MEDIUM;
        }
        try {
            return NotificationPriority.valueOf(priorityRaw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_NOTIFICATION_PRIORITY",
                    "Invalid priority. Allowed values: LOW, MEDIUM, HIGH, URGENT");
        }
    }

    @Transactional
    public void deleteNotification(Long notificationId, AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");
        deleteNotification(notificationId, resolveNotificationUserId(requester));
    }

    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Objects.requireNonNull(userId, "User id is required");
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (notification.getUser() == null || !Objects.equals(notification.getUser().getId(), userId)) {
            throw new ForbiddenException("Notification does not belong to user");
        }

        // Soft-delete to avoid FK/integrity violations with related records.
        notification.setIsDeleted(true);
        notification.setDeletedAt(Instant.now(clock));
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getUserPreferences(Long userId) {
        List<NotificationPreference> preferences = preferenceRepository.findByUserId(userId);
        return preferences.stream()
                .map(this::toPreferenceResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationPreferenceResponse setUserPreference(Long userId, NotificationType notificationType,
            NotificationPreferenceRequest request) {
        if (notificationType == null) {
            throw new RuntimeException("notificationType is required");
        }

        Optional<NotificationPreference> existing = preferenceRepository.findByUserIdAndNotificationType(userId,
                notificationType);

        NotificationPreference preference = existing.orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return NotificationPreference.builder()
                    .user(user)
                    .notificationType(notificationType)
                    .build();
        });

        if (request.getEmailEnabled() != null) {
            preference.setEmailEnabled(request.getEmailEnabled());
        }
        if (request.getSmsEnabled() != null) {
            preference.setSmsEnabled(request.getSmsEnabled());
        }
        if (request.getPushEnabled() != null) {
            preference.setPushEnabled(request.getPushEnabled());
        }
        if (request.getInAppEnabled() != null) {
            preference.setInAppEnabled(request.getInAppEnabled());
        }
        if (request.getTiming() != null) {
            preference.setTiming(request.getTiming());
        }
        if (request.getQuietHoursStart() != null) {
            preference.setQuietHoursStart(request.getQuietHoursStart());
        }
        if (request.getQuietHoursEnd() != null) {
            preference.setQuietHoursEnd(request.getQuietHoursEnd());
        }
        if (request.getWeekendsEnabled() != null) {
            preference.setWeekendsEnabled(request.getWeekendsEnabled());
        }

        return toPreferenceResponse(preferenceRepository.save(preference));
    }

    @Transactional(readOnly = true)
    public NotificationStatsResponse getStats() {
        long total = notificationRepository.count();
        long unread = notificationRepository.findByIsRead(false).size();
        return NotificationStatsResponse.builder()
                .total(total)
                .unread(unread)
                .build();
    }

    @Transactional(readOnly = true)
    public NotificationSetupHealthResponse getSessionNotificationSetupHealth() {
        List<String> events = List.of(
                NotificationEventCatalog.SESSION_SCHEDULED,
                NotificationEventCatalog.SESSION_RESCHEDULED,
                NotificationEventCatalog.SESSION_CANCELLED,
                NotificationEventCatalog.SESSION_REMINDER,
                NotificationEventCatalog.SESSION_COMPLETED,
                NotificationEventCatalog.BILL_GENERATED
        );

        List<NotificationSetupHealthResponse.EventSetupHealth> rows = events.stream()
                .map(this::buildEventSetupHealth)
                .toList();

        return NotificationSetupHealthResponse.builder()
                .scope("tenant")
                .events(rows)
                .build();
    }

    @Transactional
    public void syncTenantDefaults() {
        for (String eventType : NotificationEventCatalog.REQUIRED_EVENTS) {
            ensureDefaultNotificationSetup(eventType);
        }
        repairOverBroadTherapistRecipientRules();
    }

    @Transactional(readOnly = true)
    public NotificationSetupHealthResponse getCoverageHealth() {
        List<NotificationSetupHealthResponse.EventSetupHealth> rows = NotificationEventCatalog.REQUIRED_EVENTS.stream()
                .sorted()
                .map(this::buildEventSetupHealth)
                .toList();

        return NotificationSetupHealthResponse.builder()
                .scope("tenant")
                .events(rows)
                .build();
    }

    @Transactional(readOnly = true)
    public NotificationEventCatalogResponse getEventCatalog() {
        List<NotificationEventCatalogResponse.EventDefinition> events = NotificationEventCatalog.ORDERED_EVENTS.stream()
                .map(eventType -> NotificationEventCatalogResponse.EventDefinition.builder()
                        .eventType(eventType)
                        .defaultChannels(NotificationEventCatalog.DEFAULT_CHANNELS)
                        .required(NotificationEventCatalog.REQUIRED_EVENTS.contains(eventType))
                        .sessionHealthEvent(NotificationEventCatalog.SESSION_HEALTH_EVENTS.contains(eventType))
                        .build())
                .toList();

        return NotificationEventCatalogResponse.builder()
                .scope("tenant")
                .events(events)
                .build();
    }

    @Transactional(readOnly = true)
    public NotificationActionMetadataResponse getActionMetadata() {
        try {
            List<NotificationActionMetadata> configured = notificationActionMetadataRepository
                    .findByIsDeletedFalseAndIsActiveTrueOrderBySortOrderAscRelatedEntityTypeAsc();
            if (!configured.isEmpty()) {
                return NotificationActionMetadataResponse.builder()
                        .scope("tenant")
                        .entities(configured.stream()
                                .map(this::toActionMetadataResponse)
                                .toList())
                        .build();
            }
        } catch (RuntimeException ex) {
            // Empty/missing table or schema mismatch must not 500 the setup UI.
            log.warn("Action metadata unavailable in tenant schema, falling back to defaults: {}", ex.getMessage());
        }

        return NotificationActionMetadataResponse.builder()
                .scope("tenant")
                .entities(defaultActionDefinitions())
                .build();
    }

    @Transactional
    public NotificationActionMetadataResponse.EntityActionDefinition upsertActionMetadata(
            String relatedEntityType,
            NotificationActionMetadataUpsertRequest request) {
        try {
            String normalizedType = normalizeRelatedEntityType(relatedEntityType);
            NotificationActionMetadata row = notificationActionMetadataRepository
                    .findByRelatedEntityTypeIgnoreCaseAndIsDeletedFalse(normalizedType)
                    .orElseGet(() -> NotificationActionMetadata.builder()
                            .relatedEntityType(normalizedType)
                            .build());

            row.setRelatedEntityType(normalizedType);
            row.setActionUrlTemplate(request.getActionUrlTemplate().trim());
            row.setDefaultActionLabel(request.getDefaultActionLabel().trim());
            row.setExampleActionUrl(StringUtils.hasText(request.getExampleActionUrl()) ? request.getExampleActionUrl().trim() : null);
            row.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
            row.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
            row.setIsDeleted(false);
            row.setDeletedAt(null);

            NotificationActionMetadata saved = notificationActionMetadataRepository.save(row);
            return toActionMetadataResponse(saved);
        } catch (InvalidDataAccessResourceUsageException | SQLGrammarException ex) {
            throw new StoryApiException(HttpStatus.SERVICE_UNAVAILABLE, "ACTION_METADATA_TABLE_MISSING",
                    "notification_action_metadata table is missing in this tenant schema");
        }
    }

    @Transactional
    public void deleteActionMetadata(String relatedEntityType) {
        try {
            String normalizedType = normalizeRelatedEntityType(relatedEntityType);
            NotificationActionMetadata row = notificationActionMetadataRepository
                    .findByRelatedEntityTypeIgnoreCaseAndIsDeletedFalse(normalizedType)
                    .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "ACTION_METADATA_NOT_FOUND",
                            "Action metadata not found for entity type: " + normalizedType));
            row.softDelete();
            row.setIsActive(false);
            notificationActionMetadataRepository.save(row);
        } catch (InvalidDataAccessResourceUsageException | SQLGrammarException ex) {
            throw new StoryApiException(HttpStatus.SERVICE_UNAVAILABLE, "ACTION_METADATA_TABLE_MISSING",
                    "notification_action_metadata table is missing in this tenant schema");
        }
    }

    @Transactional
    public NotificationActionMetadataResponse.SeedResult seedDefaultActionMetadata(boolean overwriteExisting) {
        try {
            int created = 0;
            int updated = 0;
            int skipped = 0;
            for (NotificationActionMetadataResponse.EntityActionDefinition def : defaultActionDefinitions()) {
                Optional<NotificationActionMetadata> existing = notificationActionMetadataRepository
                        .findByRelatedEntityTypeIgnoreCaseAndIsDeletedFalse(def.getRelatedEntityType());
                if (existing.isPresent()) {
                    if (!overwriteExisting) {
                        skipped++;
                        continue;
                    }
                    NotificationActionMetadata row = existing.get();
                    row.setActionUrlTemplate(def.getActionUrlTemplate());
                    row.setDefaultActionLabel(def.getDefaultActionLabel());
                    row.setExampleActionUrl(def.getExampleActionUrl());
                    row.setIsActive(true);
                    row.setIsDeleted(false);
                    row.setDeletedAt(null);
                    row.setSortOrder(resolveSortOrder(def.getRelatedEntityType()));
                    notificationActionMetadataRepository.save(row);
                    updated++;
                } else {
                    NotificationActionMetadata row = NotificationActionMetadata.builder()
                            .relatedEntityType(def.getRelatedEntityType())
                            .actionUrlTemplate(def.getActionUrlTemplate())
                            .defaultActionLabel(def.getDefaultActionLabel())
                            .exampleActionUrl(def.getExampleActionUrl())
                            .sortOrder(resolveSortOrder(def.getRelatedEntityType()))
                            .isActive(true)
                            .build();
                    notificationActionMetadataRepository.save(row);
                    created++;
                }
            }
            return NotificationActionMetadataResponse.SeedResult.builder()
                    .created(created)
                    .updated(updated)
                    .skipped(skipped)
                    .build();
        } catch (InvalidDataAccessResourceUsageException | SQLGrammarException ex) {
            throw new StoryApiException(HttpStatus.SERVICE_UNAVAILABLE, "ACTION_METADATA_TABLE_MISSING",
                    "notification_action_metadata table is missing in this tenant schema");
        }
    }

    private NotificationActionMetadataResponse.EntityActionDefinition toActionMetadataResponse(NotificationActionMetadata row) {
        return NotificationActionMetadataResponse.EntityActionDefinition.builder()
                .relatedEntityType(row.getRelatedEntityType())
                .actionUrlTemplate(row.getActionUrlTemplate())
                .defaultActionLabel(row.getDefaultActionLabel())
                .exampleActionUrl(row.getExampleActionUrl())
                .build();
    }

    private String normalizeRelatedEntityType(String relatedEntityType) {
        String normalized = relatedEntityType != null ? relatedEntityType.trim().toLowerCase() : "";
        if (!StringUtils.hasText(normalized) || !normalized.matches("^[a-z][a-z0-9_\\-]{1,49}$")) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_RELATED_ENTITY_TYPE",
                    "relatedEntityType must match pattern ^[a-z][a-z0-9_\\-]{1,49}$");
        }
        return normalized;
    }

    private int resolveSortOrder(String relatedEntityType) {
        List<NotificationActionMetadataResponse.EntityActionDefinition> defs = defaultActionDefinitions();
        for (int i = 0; i < defs.size(); i++) {
            if (defs.get(i).getRelatedEntityType().equalsIgnoreCase(relatedEntityType)) {
                return (i + 1) * 10;
            }
        }
        return 9999;
    }

    private List<NotificationActionMetadataResponse.EntityActionDefinition> defaultActionDefinitions() {
        return List.of(
                NotificationActionMetadataResponse.EntityActionDefinition.builder()
                        .relatedEntityType("session")
                        .actionUrlTemplate("/sessions/{id}")
                        .defaultActionLabel("View session")
                        .exampleActionUrl("/sessions/123")
                        .build(),
                NotificationActionMetadataResponse.EntityActionDefinition.builder()
                        .relatedEntityType("client")
                        .actionUrlTemplate("/clients/{id}")
                        .defaultActionLabel("View client")
                        .exampleActionUrl("/clients/456")
                        .build(),
                NotificationActionMetadataResponse.EntityActionDefinition.builder()
                        .relatedEntityType("task")
                        .actionUrlTemplate("/tasks/{id}")
                        .defaultActionLabel("View task")
                        .exampleActionUrl("/tasks/654")
                        .build(),
                NotificationActionMetadataResponse.EntityActionDefinition.builder()
                        .relatedEntityType("checklist")
                        .actionUrlTemplate("/checklists/{id}")
                        .defaultActionLabel("View checklist")
                        .exampleActionUrl("/checklists/222")
                        .build(),
                NotificationActionMetadataResponse.EntityActionDefinition.builder()
                        .relatedEntityType("billing")
                        .actionUrlTemplate("/billing/{id}")
                        .defaultActionLabel("View billing")
                        .exampleActionUrl("/billing/789")
                        .build(),
                NotificationActionMetadataResponse.EntityActionDefinition.builder()
                        .relatedEntityType("form")
                        .actionUrlTemplate("/forms/{id}")
                        .defaultActionLabel("View form")
                        .exampleActionUrl("/forms/333")
                        .build(),
                NotificationActionMetadataResponse.EntityActionDefinition.builder()
                        .relatedEntityType("document")
                        .actionUrlTemplate("/documents/{id}")
                        .defaultActionLabel("View document")
                        .exampleActionUrl("/documents/987")
                        .build(),
                NotificationActionMetadataResponse.EntityActionDefinition.builder()
                        .relatedEntityType("assessment")
                        .actionUrlTemplate("/assessments/{id}")
                        .defaultActionLabel("View assessment")
                        .exampleActionUrl("/assessments/321")
                        .build(),
                NotificationActionMetadataResponse.EntityActionDefinition.builder()
                        .relatedEntityType("user")
                        .actionUrlTemplate("/users/{id}")
                        .defaultActionLabel("View profile")
                        .exampleActionUrl("/users/111")
                        .build(),
                NotificationActionMetadataResponse.EntityActionDefinition.builder()
                        .relatedEntityType("room")
                        .actionUrlTemplate("/rooms/{id}")
                        .defaultActionLabel("View room")
                        .exampleActionUrl("/rooms/444")
                        .build(),
                NotificationActionMetadataResponse.EntityActionDefinition.builder()
                        .relatedEntityType("general")
                        .actionUrlTemplate("/notifications")
                        .defaultActionLabel("View details")
                        .exampleActionUrl("/notifications")
                        .build());
    }

    private NotificationSetupHealthResponse.EventSetupHealth buildEventSetupHealth(String eventType) {
        List<NotificationTrigger> triggers = triggerRepository.findByEventType(eventType);
        long activeTriggerCount = triggers.stream().filter(t -> Boolean.TRUE.equals(t.getIsActive())).count();

        List<NotificationTemplate> templates = findTemplatesForEvent(eventType);
        boolean hasInAppTemplate = templates.stream()
                .anyMatch(t -> isTemplateChannel(t, "in_app"));
        boolean hasActiveInAppTemplate = templates.stream()
                .anyMatch(t -> isTemplateChannel(t, "in_app") && Boolean.TRUE.equals(t.getIsActive()));
        boolean hasEmailTemplate = templates.stream()
                .anyMatch(t -> isTemplateChannel(t, "email"));
        boolean hasActiveEmailTemplate = templates.stream()
                .anyMatch(t -> isTemplateChannel(t, "email") && Boolean.TRUE.equals(t.getIsActive()));

        return NotificationSetupHealthResponse.EventSetupHealth.builder()
                .eventType(eventType)
                .hasAnyTrigger(!triggers.isEmpty())
                .hasActiveTrigger(activeTriggerCount > 0)
                .hasInAppTemplate(hasInAppTemplate)
                .hasActiveInAppTemplate(hasActiveInAppTemplate)
                .hasEmailTemplate(hasEmailTemplate)
                .hasActiveEmailTemplate(hasActiveEmailTemplate)
                .triggerCount(triggers.size())
                .activeTriggerCount((int) activeTriggerCount)
                .build();
    }

    private boolean isTemplateChannel(NotificationTemplate template, String channel) {
        if (template == null || !StringUtils.hasText(channel)) {
            return false;
        }
        String type = template.getType() != null ? template.getType().trim() : "";
        String name = template.getName() != null ? template.getName().trim().toLowerCase() : "";
        return channel.equalsIgnoreCase(type) || name.endsWith("_" + channel.toLowerCase());
    }

    @Transactional
    public void cleanupExpiredNotifications() {
        Instant now = Instant.now(clock);
        List<Notification> expired = notificationRepository.findAll().stream()
                .filter(n -> n.getExpiresAt() != null && n.getExpiresAt().isBefore(now))
                .collect(Collectors.toList());
        notificationRepository.deleteAll(expired);
        log.info("Cleaned up {} expired notifications", expired.size());
    }

    // ========== PRIVATE HELPER METHODS ==========

    private NotificationPreferenceResponse toPreferenceResponse(NotificationPreference preference) {
        return NotificationPreferenceResponse.builder()
                .id(preference.getId())
                .userId(preference.getUser() != null ? preference.getUser().getId() : null)
                .clientId(preference.getClient() != null ? preference.getClient().getId() : null)
                .notificationType(preference.getNotificationType())
                .emailEnabled(preference.getEmailEnabled())
                .smsEnabled(preference.getSmsEnabled())
                .pushEnabled(preference.getPushEnabled())
                .inAppEnabled(preference.getInAppEnabled())
                .timing(preference.getTiming())
                .quietHoursStart(preference.getQuietHoursStart())
                .quietHoursEnd(preference.getQuietHoursEnd())
                .weekendsEnabled(preference.getWeekendsEnabled())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }

    @Scheduled(fixedDelayString = "${app.notifications.schedule-interval:60000}")
    public void processScheduledNotifications() {
        tenantExecutionService.runForEachActiveTenant("scheduled-notifications", tenant ->
                transactionTemplate.execute(status -> {
                    processScheduledNotificationsForTenant();
                    return null;
                })
        );
    }

    @Transactional
    protected void processScheduledNotificationsForTenant() {
        if (!isScheduledNotificationsTablePresentInCurrentSchema()) {
            log.debug("scheduled_notifications table not available yet, skipping scheduled notification processing");
            return;
        }
        try {
            Instant now = Instant.now(clock);
            List<ScheduledNotification> pending = scheduledNotificationRepository
                    .findByStatusAndExecuteAtBefore(STATUS_PENDING, now);

            if (pending.isEmpty()) {
                return;
            }

            for (ScheduledNotification scheduled : pending) {
                try {
                    Map<String, Object> payload = deserializeEntityData(scheduled.getEntityData());
                    payload.putIfAbsent("entityType", scheduled.getEntityType());
                    payload.putIfAbsent("entityId", scheduled.getEntityId());

                    // Calculate recipients from trigger rules (user + client recipients)
                    RecipientTargets recipients = determineRecipients(scheduled.getTrigger(), payload);
                    if (recipients.isEmpty()) {
                        log.warn("No recipients found for scheduled notification {}", scheduled.getId());
                        scheduled.setStatus(STATUS_FAILED);
                        scheduled.setProcessedAt(now);
                        scheduled.setLastError("No recipients resolved from trigger rules");
                        scheduled.setRetryCount(scheduled.getRetryCount() + 1);
                        continue;
                    }

                    deliverNotifications(scheduled.getTrigger(), recipients, payload);
                    scheduled.setStatus(STATUS_SENT);
                    scheduled.setProcessedAt(now);
                    scheduled.setLastError(null);
                } catch (Exception ex) {
                    scheduled.setStatus(STATUS_FAILED);
                    scheduled.setProcessedAt(now);
                    scheduled.setLastError(ex.getMessage());
                    scheduled.setRetryCount(scheduled.getRetryCount() + 1);
                    log.error("Failed to process scheduled notification {}", scheduled.getId(), ex);
                }
            }

            scheduledNotificationRepository.saveAll(pending);
        } catch (InvalidDataAccessResourceUsageException ex) {
            if (isMissingScheduledNotificationsTable(ex.getMessage())) {
                log.debug("scheduled_notifications table not available yet, skipping scheduled notification processing: {}",
                        ex.getMessage());
            } else {
                log.error("Scheduled notification query failed due to schema mismatch", ex);
            }
        } catch (SQLGrammarException ex) {
            if (isMissingScheduledNotificationsTable(ex.getMessage())) {
                log.debug("scheduled_notifications table not available yet, skipping scheduled notification processing: {}",
                        ex.getMessage());
            } else {
                log.error("Scheduled notification query failed due to schema mismatch", ex);
            }
        } catch (Exception ex) {
            log.error("Unexpected error processing scheduled notifications", ex);
        }
    }

    private boolean isMissingScheduledNotificationsTable(String message) {
        return message != null
                && message.contains("relation \"scheduled_notifications\" does not exist");
    }

    private boolean isScheduledNotificationsTablePresentInCurrentSchema() {
        try {
            Object result = entityManager.createNativeQuery(
                            "select to_regclass(current_schema() || '.scheduled_notifications')")
                    .getSingleResult();
            return result != null;
        } catch (RuntimeException ex) {
            log.debug("Could not verify scheduled_notifications table presence in current schema: {}", ex.getMessage());
            return false;
        }
    }

    private Map<String, Object> prepareEntityData(String eventType, Map<String, Object> entityData) {
        Map<String, Object> payload = new HashMap<>();
        if (entityData != null) {
            payload.putAll(entityData);
        }
        applyPracticeSettings(payload);
        ensureSessionDateFormatted(payload);
        ensureDueDateFormatted(payload);
        EmailAppLinks.absolutizeKnownUrlKeys(payload, staffLoginUrl);

        payload.putIfAbsent("eventType", eventType);
        payload.putIfAbsent("entityType", resolveEntityType(eventType, payload));
        if (!payload.containsKey("entityId")) {
            payload.put("entityId", resolveEntityId(payload));
        }
        return payload;
    }

    /**
     * Templates use {{sessionDateFormatted}}; callers often only supply sessionDate (Instant).
     * Uses Administration practice timezone (falls back to UTC when unset).
     */
    private void ensureSessionDateFormatted(Map<String, Object> payload) {
        if (payload.get("sessionDateFormatted") != null) {
            return;
        }
        Instant sessionInstant = toInstant(payload.get("sessionDate"));
        if (sessionInstant == null) {
            return;
        }
        payload.put("sessionDateFormatted", formatInstantInPracticeTimezone(sessionInstant));
    }

    /**
     * Templates use {{dueDateFormatted}}; callers often supply dueDate as Instant or LocalDate.
     */
    private void ensureDueDateFormatted(Map<String, Object> payload) {
        if (payload.get("dueDateFormatted") != null) {
            return;
        }
        Object raw = payload.get("dueDate");
        if (raw == null) {
            payload.putIfAbsent("dueDateFormatted", "N/A");
            return;
        }
        Instant dueInstant = toInstant(raw);
        if (dueInstant != null) {
            payload.put("dueDateFormatted", formatInstantInPracticeTimezone(dueInstant));
            return;
        }
        if (raw instanceof java.time.LocalDate localDate) {
            payload.put("dueDateFormatted",
                    localDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
            return;
        }
        if (raw instanceof java.time.LocalDateTime localDateTime) {
            payload.put("dueDateFormatted",
                    localDateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")));
            return;
        }
        payload.put("dueDateFormatted", String.valueOf(raw));
    }

    /**
     * The timezone a staff recipient reads their notifications in: their own profile
     * setting, falling back to the Administration practice timezone.
     */
    private ZoneId resolveRecipientZone(User user) {
        if (timezoneService == null || user == null || user.getId() == null) {
            return null;
        }
        return timezoneService.getTherapistTimezone(user.getId()).orElse(null);
    }

    /** As above, for a client recipient: their portal setting, else the clinic default. */
    private ZoneId resolveRecipientZone(Client client) {
        if (timezoneService == null || client == null || client.getId() == null) {
            return null;
        }
        return timezoneService.resolveClientPortalZone(client.getId(), null);
    }

    /**
     * Session times are rendered per recipient — the client reads their own timezone, the
     * therapist theirs. Formatting deliberately happens here, at delivery, rather than at
     * the call site: a reminder scheduled days earlier still goes out in whatever timezone
     * the recipient has set today, and one event reaching two people no longer forces them
     * to share one zone. Events carrying no session instant pass through untouched.
     */
    private Map<String, Object> localizeForRecipient(Map<String, Object> payload, ZoneId zone) {
        if (payload == null || zone == null || timezoneService == null) {
            return payload;
        }
        Instant sessionInstant = toInstant(payload.get("sessionDate"));
        if (sessionInstant == null) {
            return payload;
        }

        Integer duration = toDurationMinutes(payload.get("duration"));
        String dateOnly = timezoneService.formatSessionDateOnly(sessionInstant, zone);
        String timeRange = timezoneService.formatSessionTimeRange(sessionInstant, duration, zone);

        Map<String, Object> localized = new HashMap<>(payload);
        localized.put("recipientTimezone", zone.getId());
        localized.put("sessionDateFormatted", timezoneService.formatSessionDateTime(sessionInstant, zone));
        localized.put("sessionDateOnlyFormatted", dateOnly);
        localized.put("sessionTimeRangeFormatted", timeRange);
        if (payload.get("sessionDetailsHtml") != null) {
            localized.put("sessionDetailsHtml", sessionDetailsHtmlFor(payload, dateOnly, timeRange));
        }
        return localized;
    }

    /**
     * Rebuild the session card around the recipient's own date and time. locationLabel is
     * passed through as-is because callers legitimately put markup there (a meeting link).
     */
    private String sessionDetailsHtmlFor(Map<String, Object> payload, String dateOnly, String timeRange) {
        String zoomHtml = asPlainText(payload.get("zoomMeetingHtml"));
        String card = com.smart.therapy.flow.common.service.EmailHtmlComponents.sessionDetailsCardHtml(
                dateOnly,
                timeRange,
                escapeForEmail(payload.get("clientMrn")),
                escapeForEmail(payload.get("therapistName")),
                asPlainText(payload.get("locationLabel")),
                escapeForEmail(payload.get("serviceName")));
        return zoomHtml.isEmpty() ? card : card + zoomHtml;
    }

    private String asPlainText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String escapeForEmail(Object value) {
        return org.springframework.web.util.HtmlUtils.htmlEscape(asPlainText(value));
    }

    private Integer toDurationMinutes(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Integer.valueOf(str.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String formatInstantInPracticeTimezone(Instant instant) {
        if (timezoneService != null) {
            return timezoneService.formatSessionDateTimeForPractice(instant);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a z")
                .withZone(ZoneOffset.UTC);
        return formatter.format(instant) + " (UTC)";
    }

    private void applyPracticeSettings(Map<String, Object> payload) {
        PracticeSettings settings = getPracticeSettings();
        payload.putIfAbsent("practiceName", settings.getName());
        payload.putIfAbsent("practiceAddress", settings.getAddress());
        payload.putIfAbsent("practicePhone", settings.getPhone());
        payload.putIfAbsent("practiceEmail", settings.getEmail());
        payload.putIfAbsent("practiceWebsite", settings.getWebsite());
        if (timezoneService != null) {
            payload.putIfAbsent("practiceTimezone", timezoneService.getPracticeTimezone().getId());
        }
    }

    private PracticeSettings getPracticeSettings() {
        PracticeSettings cached = practiceSettingsCache;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (practiceSettingsCache == null) {
                practiceSettingsCache = resolvePracticeSettings();
            }
            return practiceSettingsCache;
        }
    }

    private PracticeSettings resolvePracticeSettings() {
        PracticeSettings settings = PracticeSettings.builder()
                .name("Resilience Counseling Research & Consultation")
                .address("111 Waterloo St Unit 406, London, ON N6B 2M4")
                .phone("+1 (548)866-0366")
                .email("mail@resiliencec.com")
                .website("www.resiliencec.com")
                .build();

        try {
            PracticeConfigurationResponse config = practiceConfigurationService.getPracticeConfiguration();
            if (config != null) {
                if (StringUtils.hasText(config.getPracticeName())) {
                    settings.setName(config.getPracticeName());
                }
                if (StringUtils.hasText(config.getPracticeAddress())) {
                    settings.setAddress(config.getPracticeAddress());
                }
                if (StringUtils.hasText(config.getPracticePhone())) {
                    settings.setPhone(config.getPracticePhone());
                }
                if (StringUtils.hasText(config.getPracticeEmail())) {
                    settings.setEmail(config.getPracticeEmail());
                }
                if (StringUtils.hasText(config.getPracticeWebsite())) {
                    settings.setWebsite(config.getPracticeWebsite());
                }
            }
        } catch (Exception ex) {
            log.debug("Failed to load canonical practice configuration, falling back to system options", ex);
        }

        optionCategoryRepository.findByCategoryKey("practice_settings").ifPresent(category -> {
            List<SystemOption> options = systemOptionRepository.findByCategoryId(category.getId());
            Map<String, SystemOption> optionsByKey = options.stream()
                    .filter(option -> Boolean.TRUE.equals(option.getIsActive()))
                    .collect(Collectors.toMap(SystemOption::getOptionKey, Function.identity(), (a, b) -> b));

            SystemOption nameOption = optionsByKey.get("practice_name");
            SystemOption addressOption = optionsByKey.get("practice_address");
            SystemOption phoneOption = optionsByKey.get("practice_phone");
            SystemOption emailOption = optionsByKey.get("practice_email");
            SystemOption websiteOption = optionsByKey.get("practice_website");

            if (nameOption != null && StringUtils.hasText(nameOption.getOptionLabel())) {
                settings.setName(nameOption.getOptionLabel());
            }
            if (addressOption != null && StringUtils.hasText(addressOption.getOptionLabel())) {
                settings.setAddress(addressOption.getOptionLabel());
            }
            if (phoneOption != null && StringUtils.hasText(phoneOption.getOptionLabel())) {
                settings.setPhone(phoneOption.getOptionLabel());
            }
            if (emailOption != null && StringUtils.hasText(emailOption.getOptionLabel())) {
                settings.setEmail(emailOption.getOptionLabel());
            }
            if (websiteOption != null && StringUtils.hasText(websiteOption.getOptionLabel())) {
                settings.setWebsite(websiteOption.getOptionLabel());
            }
        });

        return settings;
    }

    @Data
    @Builder
    private static class PracticeSettings {
        private String name;
        private String address;
        private String phone;
        private String email;
        private String website;
    }

    private boolean matchesConditionRules(String rawRules, Map<String, Object> payload) {
        if (isBlank(rawRules)) {
            return true;
        }
        try {
            JsonNode root = objectMapper.readTree(rawRules);
            if (root.isArray()) {
                for (JsonNode node : root) {
                    if (!matchesConditionNode(node, payload)) {
                        return false;
                    }
                }
                return true;
            }
            return matchesConditionNode(root, payload);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            // Rules can contain client values. Never include their contents in logs.
            log.warn("Skipping notification with invalid condition rules");
            return false;
        }
    }

    private boolean matchesConditionNode(JsonNode node, Map<String, Object> payload) {
        if (node == null || !node.isObject()) {
            return false;
        }
        if (node.has("field") || node.has("operator") || node.has("value")) {
            return evaluateConditionRule(objectMapper.convertValue(node, ConditionRule.class), payload);
        }
        // Older triggers store equality conditions as {"sessionType":"assessment"}.
        // An empty object is the default unconditional rule; multiple keys are ANDed.
        var fields = node.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            ConditionRule rule = new ConditionRule();
            rule.field = entry.getKey();
            rule.operator = "equals";
            rule.value = objectMapper.convertValue(entry.getValue(), Object.class);
            if (!evaluateConditionRule(rule, payload)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateConditionRule(ConditionRule rule, Map<String, Object> payload) {
        if (rule == null || isBlank(rule.getField())) {
            return false;
        }

        Object fieldValue = resolveFieldValue(payload, rule.getField());
        Object comparisonValue = rule.getValue();
        String operator = rule.getOperator() != null ? rule.getOperator().toLowerCase() : "equals";

        switch (operator) {
            case "equals":
                return Objects.equals(normalizeValue(fieldValue), normalizeValue(comparisonValue));
            case "not_equals":
                return !Objects.equals(normalizeValue(fieldValue), normalizeValue(comparisonValue));
            case "contains":
                if (fieldValue instanceof Collection) {
                    return ((Collection<?>) fieldValue).contains(comparisonValue);
                }
                return fieldValue != null && comparisonValue != null
                        && fieldValue.toString().toLowerCase().contains(comparisonValue.toString().toLowerCase());
            case "greater_than":
                return compareAsNumbers(fieldValue, comparisonValue) > 0;
            case "less_than":
                return compareAsNumbers(fieldValue, comparisonValue) < 0;
            case "in_array":
                if (comparisonValue instanceof Collection) {
                    return ((Collection<?>) comparisonValue).contains(fieldValue);
                }
                if (comparisonValue instanceof Object[]) {
                    for (Object o : (Object[]) comparisonValue) {
                        if (Objects.equals(o, fieldValue)) {
                            return true;
                        }
                    }
                    return false;
                }
                if (comparisonValue instanceof String) {
                    Set<String> parts = Arrays.stream(comparisonValue.toString().split(","))
                            .map(String::trim)
                            .filter(part -> !part.isEmpty())
                            .collect(Collectors.toSet());
                    String candidate = fieldValue != null ? fieldValue.toString() : null;
                    return candidate != null && parts.contains(candidate);
                }
                return false;
            default:
                return false;
        }
    }

    private Object resolveFieldValue(Map<String, Object> payload, String fieldPath) {
        if (!fieldPath.contains(".")) {
            return payload.get(fieldPath);
        }

        String[] parts = fieldPath.split("\\.");
        Object current = payload;
        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    private int compareAsNumbers(Object fieldValue, Object comparisonValue) {
        BigDecimal left = toBigDecimal(fieldValue);
        BigDecimal right = toBigDecimal(comparisonValue);
        if (left == null || right == null) {
            return 0;
        }
        return left.compareTo(right);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private RecipientTargets determineRecipients(NotificationTrigger trigger, Map<String, Object> payload) {
        RecipientRules recipientRules = parseRecipientRules(trigger.getRecipientRules());
        Set<Long> userRecipients = new LinkedHashSet<>();
        Set<Long> clientRecipients = new LinkedHashSet<>();

        if (recipientRules != null) {
            if (recipientRules.getRoles() != null && !recipientRules.getRoles().isEmpty()) {
                List<RoleName> roleNames = recipientRules.getRoles().stream()
                        .map(String::toUpperCase)
                        .map(name -> {
                            try {
                                return RoleName.valueOf(name);
                            } catch (IllegalArgumentException ex) {
                                log.warn("Unknown role '{}' in notification recipient rules", name);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (!roleNames.isEmpty()) {
                    // Never broadcast to every therapist in the tenant by role. Therapists are notified only
                    // when they are the assigned therapist (or supervisor of that therapist) for the event.
                    roleNames = roleNames.stream()
                            .filter(role -> role != RoleName.THERAPIST)
                            .collect(Collectors.toList());
                }
                if (!roleNames.isEmpty()) {
                    userRepository.findDistinctByAuthIdentityRolesRoleNameIn(roleNames.stream().map(com.smart.therapy.flow.common.util.RoleName::name).toList())
                            .forEach(user -> userRecipients.add(user.getId()));
                }
            }

            if (recipientRules.getSpecificUsers() != null) {
                for (Long userId : recipientRules.getSpecificUsers()) {
                    userRepository.findById(userId).ifPresent(user -> userRecipients.add(user.getId()));
                }
            }

            if (Boolean.TRUE.equals(recipientRules.getAssignedTherapist())) {
                Long therapistId = firstNonNull(
                        getLongValue(payload.get("therapistId")),
                        getLongValue(payload.get("assignedTherapistId")),
                        getLongValue(payload.get("assignedToId")));
                addIfPresent(userRecipients, therapistId);
            }

            if (Boolean.TRUE.equals(recipientRules.getSupervisorOfTherapist())) {
                Long therapistId = firstNonNull(
                        getLongValue(payload.get("therapistId")),
                        getLongValue(payload.get("assignedTherapistId")));
                if (therapistId != null) {
                    supervisorAssignmentRepository.findByTherapistId(therapistId).stream()
                            .map(SupervisorAssignment::getSupervisor)
                            .filter(Objects::nonNull)
                            .map(User::getId)
                            .forEach(userRecipients::add);
                }
            }

            if (Boolean.TRUE.equals(recipientRules.getSessionClient())) {
                Long clientId = getLongValue(payload.get("clientId"));
                if (clientId != null) {
                    addIfPresent(clientRecipients, clientId);
                } else {
                    log.debug(
                            "Recipient rule sessionClient requested but client id not provided in payload: {}",
                            payload.keySet());
                }
            }

            if (Boolean.TRUE.equals(recipientRules.getUploader())) {
                addIfPresent(userRecipients, getLongValue(payload.get("uploadedById")));
            }
        }

        if (userRecipients.isEmpty()) {
            userRecipients.addAll(resolveDefaultUserRecipients(payload));
        }
        if (clientRecipients.isEmpty() && clientFallbackAllowed(trigger, recipientRules)) {
            addIfPresent(clientRecipients, getLongValue(payload.get("clientId")));
        }

        return new RecipientTargets(new ArrayList<>(userRecipients), new ArrayList<>(clientRecipients));
    }

    /**
     * The client is a recipient only when the trigger's rules request it via sessionClient (handled
     * above). The payload-clientId fallback exists solely for legacy triggers with no recipient rules,
     * and even then only for events that are addressed to the client. Rules that resolved no client —
     * because sessionClient is absent or false — are an explicit audience, not a gap to fill.
     */
    private boolean clientFallbackAllowed(NotificationTrigger trigger, RecipientRules recipientRules) {
        if (recipientRules != null) {
            return false;
        }
        String eventType = trigger.getEventType();
        return eventType != null && CLIENT_FALLBACK_EVENTS.contains(eventType.trim().toLowerCase(Locale.ROOT));
    }

    private RecipientRules parseRecipientRules(String rawRules) {
        if (isBlank(rawRules)) {
            return null;
        }
        try {
            return objectMapper.readValue(rawRules, RecipientRules.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse notification recipient rules: {}", rawRules, e);
            return null;
        }
    }

    private Collection<Long> resolveDefaultUserRecipients(Map<String, Object> payload) {
        Set<Long> defaults = new LinkedHashSet<>();
        addIfPresent(defaults, getLongValue(payload.get("therapistId")));
        addIfPresent(defaults, getLongValue(payload.get("assignedToId")));
        addIfPresent(defaults, getLongValue(payload.get("assignedTherapistId")));
        addIfPresent(defaults, getLongValue(payload.get("uploadedById")));

        if (defaults.isEmpty()) {
            userRepository.findDistinctByAuthIdentityRolesRoleNameIn(List.of(RoleName.ADMIN.name(), RoleName.SUPER_ADMIN.name()))
                    .forEach(user -> defaults.add(user.getId()));
        }
        return defaults;
    }

    private void scheduleNotifications(NotificationTrigger trigger, RecipientTargets recipients,
            Map<String, Object> payload) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }
        Instant calculated = calculateExecutionInstant(trigger, payload);
        if (calculated.isBefore(Instant.now(clock))) {
            calculated = Instant.now(clock);
        }
        final Instant executeAt = calculated;

        String entityDataJson = serializeEntityData(payload);

        // Get session ID if this is a session-related event
        // approach)
        Long sessionId = firstNonNull(
                getLongValue(payload.get("sessionId")),
                getLongValue(payload.get("id"))); // Backward compatibility for legacy payloads using id as session ID

        // Check for duplicate to prevent multiple scheduled notifications for same
        // session+trigger
        if (scheduledNotificationRepository.existsBySessionAndTriggerAndStatus(
                sessionId, trigger.getId(), STATUS_PENDING)) {
            log.debug("Scheduled notification already exists for session {} and trigger {}", sessionId,
                    trigger.getId());
            return;
        }

        ScheduledNotification scheduled = ScheduledNotification.builder()
                .trigger(trigger)
                .session(sessionId != null ? sessionRepository.findById(sessionId).orElse(null) : null)
                .entityType(parseEntityType(String.valueOf(payload.get("entityType"))))
                .entityId(getLongValue(payload.get("entityId")))
                .executeAt(executeAt)
                .entityData(entityDataJson)
                .retryCount(0)
                .build();
        scheduledNotificationRepository.save(scheduled);
    }

    private void deliverNotifications(NotificationTrigger trigger, RecipientTargets recipients,
            Map<String, Object> payload) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }

        List<NotificationTemplate> templates = findTemplatesForEvent(trigger.getEventType());
        NotificationTemplate inAppTemplate = resolveTemplate(templates, "in_app");
        NotificationTemplate emailTemplate = resolveTemplate(templates, "email");

        if (inAppTemplate == null && emailTemplate == null) {
            log.warn("No templates available for event {}", trigger.getEventType());
            return;
        }
        if (emailTemplate == null) {
            // Otherwise a deactivated/missing email template degrades to bell-only with no trace.
            log.warn("No active email template for event {}; delivering in-app only", trigger.getEventType());
        }

        // One person may have multiple staff accounts (admin + therapist). Still create
        // per-user in-app rows, but send at most one email per address for this event.
        Set<String> emailedAddresses = new HashSet<>();

        Set<Long> uniqueUserRecipients = new LinkedHashSet<>(recipients.getUserIds());
        for (Long userId : uniqueUserRecipients) {
            User recipient = userRepository.findById(userId).orElse(null);
            if (recipient == null) {
                log.warn("Skipping notification delivery. Recipient {} not found", userId);
                continue;
            }

            Map<String, Object> recipientPayload = localizeForRecipient(payload, resolveRecipientZone(recipient));

            Notification created = null;
            if (inAppTemplate != null) {
                created = createInAppNotification(recipient, trigger.getEventType(), inAppTemplate, recipientPayload,
                        trigger.getPriority());
                recordDelivery(created, NotificationChannel.IN_APP, NotificationStatus.DELIVERED, null, null,
                        recipient.getEmail());
            }
            if (emailTemplate != null) {
                String email = recipient.getEmail();
                if (!claimEmailAddress(emailedAddresses, email)) {
                    log.debug("Skipping duplicate email for event {} user {} (already sent to {})",
                            trigger.getEventType(), userId, email);
                } else {
                    try {
                        Map<String, Object> staffPayload = EmailAppLinks.forStaffRecipient(recipientPayload, staffLoginUrl);
                        String subject = processTemplate(emailTemplate.getSubject(), staffPayload, "TherapyFlow Notification");
                        String body = processTemplate(emailTemplate.getBodyTemplate(), staffPayload, subject);

                        if (commonEmailService != null) {
                            commonEmailService.sendEmail(email, subject, body);
                        } else if (emailService != null) {
                            // Fallback to legacy EmailService interface
                            emailService.sendNotificationEmail(recipient, created, emailTemplate, staffPayload);
                        }
                        recordDelivery(created, NotificationChannel.EMAIL, NotificationStatus.SENT, null, null, email);
                    } catch (Exception ex) {
                        emailedAddresses.remove(normalizeEmail(email));
                        log.error("Failed to send email notification for event {} to user {}", trigger.getEventType(),
                                userId, ex);
                        recordDelivery(created, NotificationChannel.EMAIL, NotificationStatus.FAILED, ex.getMessage(),
                                "EMAIL_SEND_FAILED", email);
                    }
                }
            }
        }

        Set<Long> uniqueClientRecipients = new LinkedHashSet<>(recipients.getClientIds());
        for (Long clientId : uniqueClientRecipients) {
            Client client = clientRepository.findById(clientId).orElse(null);
            if (client == null) {
                log.warn("Skipping notification delivery. Client recipient {} not found", clientId);
                continue;
            }
            String clientEmail = resolveClientEmail(client);
            Map<String, Object> recipientPayload = localizeForRecipient(payload, resolveRecipientZone(client));

            Notification created = null;
            if (inAppTemplate != null) {
                created = createInAppNotification(client, trigger.getEventType(), inAppTemplate, recipientPayload,
                        trigger.getPriority());
                recordDelivery(created, NotificationChannel.IN_APP, NotificationStatus.DELIVERED, null, null,
                        clientEmail);
            }
            if (emailTemplate != null) {
                if (!claimEmailAddress(emailedAddresses, clientEmail)) {
                    log.debug("Skipping duplicate client email for event {} client {} (already sent to {})",
                            trigger.getEventType(), clientId, clientEmail);
                } else {
                    try {
                        sendEmailToClient(client, clientEmail, created, emailTemplate, recipientPayload);
                        recordDelivery(created, NotificationChannel.EMAIL, NotificationStatus.SENT, null, null,
                                clientEmail);
                    } catch (Exception ex) {
                        emailedAddresses.remove(normalizeEmail(clientEmail));
                        log.error("Failed to send email notification for event {} to client {}", trigger.getEventType(),
                                clientId, ex);
                        recordDelivery(created, NotificationChannel.EMAIL, NotificationStatus.FAILED, ex.getMessage(),
                                "EMAIL_SEND_FAILED", clientEmail);
                    }
                }
            }
            if (smsNotificationService != null && smsNotificationService.supportsEvent(trigger.getEventType())) {
                smsNotificationService.sendClientSessionSms(
                        clientId,
                        trigger.getEventType(),
                        trigger.getIsScheduled(),
                        recipientPayload);
            }
        }
    }

    private static boolean claimEmailAddress(Set<String> emailedAddresses, String email) {
        String normalized = normalizeEmail(email);
        if (normalized == null) {
            return false;
        }
        return emailedAddresses.add(normalized);
    }

    private static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private NotificationTemplate resolveTemplate(List<NotificationTemplate> templates, String type) {
        if (templates == null) {
            return null;
        }
        return templates.stream()
                .filter(template -> {
                    String name = template.getName() != null ? template.getName().toLowerCase() : "";
                    String channelSuffix = "_" + type.toLowerCase();
                    return type.equalsIgnoreCase(template.getType()) || name.endsWith(channelSuffix);
                })
                .filter(template -> Boolean.TRUE.equals(template.getIsActive()))
                .findFirst()
                .orElse(null);
    }

    private List<NotificationTemplate> findTemplatesForEvent(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return List.of();
        }
        List<NotificationTemplate> byEventType =
                templateRepository.findByEventTypeIgnoreCase(eventType.trim());
        if (byEventType != null && !byEventType.isEmpty()) {
            return byEventType;
        }
        String prefix = eventType.toLowerCase() + "_";
        List<NotificationTemplate> byName = templateRepository.findByNameStartingWith(prefix);
        if (byName != null && !byName.isEmpty()) {
            return byName;
        }
        return List.of();
    }

    private Notification createInAppNotification(User user, String eventType, NotificationTemplate template,
            Map<String, Object> entityData, String priority) {
        // Prefer MRN in stored in-app title/message/data; email still uses original payload.
        Map<String, Object> durableData = NotificationPayloadFactory.forDurableInAppStorage(entityData);
        String title = processTemplate(template.getSubject(), durableData, "Notification");
        String message = processTemplate(template.getBodyTemplate(), durableData, "");

        Notification notification = Notification.builder()
                .user(user)
                .type(resolveNotificationType(eventType))
                .title(title)
                .message(message)
                .priority(priority != null ? NotificationPriority.valueOf(priority.toUpperCase()) : NotificationPriority.MEDIUM)
                .isRead(false)
                .data(serializeEntityData(durableData))
                .relatedEntityType((String) entityData.get("entityType"))
                .relatedEntityId(getLongValue(entityData.get("entityId")))
                .build();

        return notificationRepository.save(notification);
    }

    private Notification createInAppNotification(Client client, String eventType, NotificationTemplate template,
            Map<String, Object> entityData, String priority) {
        Map<String, Object> durableData = NotificationPayloadFactory.forDurableInAppStorage(entityData);
        String title = processTemplate(template.getSubject(), durableData, "Notification");
        String message = processTemplate(template.getBodyTemplate(), durableData, "");

        Notification notification = Notification.builder()
                .client(client)
                .type(resolveNotificationType(eventType))
                .title(title)
                .message(message)
                .priority(priority != null ? NotificationPriority.valueOf(priority.toUpperCase()) : NotificationPriority.MEDIUM)
                .isRead(false)
                .data(serializeEntityData(durableData))
                .relatedEntityType((String) entityData.get("entityType"))
                .relatedEntityId(getLongValue(entityData.get("entityId")))
                .build();

        return notificationRepository.save(notification);
    }

    private com.smart.therapy.flow.notification.enums.NotificationType resolveNotificationType(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return com.smart.therapy.flow.notification.enums.NotificationType.SYSTEM_MAINTENANCE;
        }
        try {
            return com.smart.therapy.flow.notification.enums.NotificationType.valueOf(eventType.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return switch (eventType.toLowerCase()) {
                case "session_scheduled" -> com.smart.therapy.flow.notification.enums.NotificationType.APPOINTMENT_CONFIRMED;
                case "session_rescheduled" -> com.smart.therapy.flow.notification.enums.NotificationType.APPOINTMENT_RESCHEDULED;
                case "session_cancelled" -> com.smart.therapy.flow.notification.enums.NotificationType.APPOINTMENT_CANCELLED;
                case "session_reminder" -> com.smart.therapy.flow.notification.enums.NotificationType.APPOINTMENT_24H_REMINDER;
                case "session_overdue" -> com.smart.therapy.flow.notification.enums.NotificationType.APPOINTMENT_REMINDER;
                default -> com.smart.therapy.flow.notification.enums.NotificationType.SYSTEM_MAINTENANCE;
            };
        }
    }

    private void sendEmailToClient(
            Client client,
            String clientEmail,
            Notification inAppNotification,
            NotificationTemplate emailTemplate,
            Map<String, Object> entityData) {
        if (!StringUtils.hasText(clientEmail)) {
            return;
        }
        Map<String, Object> clientPayload = EmailAppLinks.forClientRecipient(entityData, staffLoginUrl);
        String subject = processTemplate(emailTemplate.getSubject(), clientPayload, "TherapyFlow Notification");
        String body = processTemplate(emailTemplate.getBodyTemplate(), clientPayload, subject);

        if (commonEmailService != null) {
            commonEmailService.sendEmail(clientEmail, subject, body);
        } else if (emailProviderService != null) {
            emailProviderService.sendEmail(clientEmail, subject, body);
        } else {
            log.warn("No email sender configured for client notification email clientId={}", client.getId());
        }

        if (inAppNotification != null) {
            inAppNotification.setEmailSent(true);
            inAppNotification.setEmailSentAt(Instant.now(clock));
            notificationRepository.save(inAppNotification);
        }
    }

    private String resolveClientEmail(Client client) {
        if (client == null || client.getId() == null) {
            return null;
        }
        return clientContactService.getPrimaryEmail(client.getId())
                .map(c -> c.getContactValue())
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    private void ensureDefaultNotificationSetup(String eventType) {
        repairOverBroadTherapistRecipientRulesOnce();
        if (!StringUtils.hasText(eventType)) {
            return;
        }
        String normalized = eventType.toLowerCase();
        if (!NotificationEventCatalog.REQUIRED_EVENTS.contains(normalized)) {
            return;
        }

        String recipientRulesJson = CLINICAL_EVENT_RECIPIENT_RULES;

        switch (normalized) {
            case NotificationEventCatalog.SESSION_SCHEDULED -> {
                ensureTemplate(normalized, "in_app", "Session Scheduled",
                        "Session with {{clientMrn}} on {{sessionDateFormatted}} ({{sessionType}}).");
                ensureTemplate(normalized, "email", "Session Scheduled: {{clientMrn}}",
                        EmailHtmlComponents.sessionScheduledEmailBody());
                ensureTrigger(normalized, "Session Scheduled Trigger", "session", recipientRulesJson, false, 0);
            }
            case NotificationEventCatalog.SESSION_SERIES_SCHEDULED -> {
                ensureTemplate(normalized, "in_app", "Recurring Series Scheduled",
                        "{{sessionCount}} sessions scheduled for {{clientMrn}} with {{therapistName}}.");
                ensureTemplate(normalized, "email", "Recurring Series Scheduled: {{clientMrn}}",
                        EmailHtmlComponents.standardEventEmailBody(
                                "Recurring Series Scheduled",
                                "A recurring session series has been booked for client <strong>{{clientMrn}}</strong>.",
                                EmailHtmlComponents.kvTable(
                                        EmailHtmlComponents.invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Sessions", "{{sessionCount}}", false),
                                        EmailHtmlComponents.invoiceKvRow("First", "{{firstSessionDateFormatted}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Last", "{{lastSessionDateFormatted}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Service", "{{serviceName}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Therapist", "{{therapistName}}", true)),
                                null,
                                null));
                ensureTrigger(normalized, "Session Series Scheduled Trigger", "session", recipientRulesJson, false, 0);
            }
            case NotificationEventCatalog.SESSION_RESCHEDULED -> {
                ensureTemplate(normalized, "in_app", "Session Rescheduled",
                        "Session with {{clientMrn}} moved to {{sessionDateFormatted}}.");
                ensureTemplate(normalized, "email", "Session Rescheduled: {{clientMrn}}",
                        EmailHtmlComponents.sessionRescheduledEmailBody());
                ensureTrigger(normalized, "Session Rescheduled Trigger", "session", recipientRulesJson, false, 0);
            }
            case NotificationEventCatalog.SESSION_CANCELLED -> {
                ensureTemplate(normalized, "in_app", "Session Cancelled",
                        "Session with {{clientMrn}} on {{sessionDateFormatted}} was cancelled.");
                ensureTemplate(normalized, "email", "Session Cancelled: {{clientMrn}}",
                        EmailHtmlComponents.sessionCancelledEmailBody());
                ensureTrigger(normalized, "Session Cancelled Trigger", "session", recipientRulesJson, false, 0);
            }
            case NotificationEventCatalog.SESSION_REMINDER -> {
                ensureTemplate(normalized, "in_app", "Session Reminder",
                        "Reminder: Session with {{clientMrn}} is on {{sessionDateFormatted}}.");
                ensureTemplate(normalized, "email", "Session Reminder: {{clientMrn}}",
                        EmailHtmlComponents.sessionReminderEmailBody());
                ensureTrigger(normalized, "Session Reminder Trigger", "session", recipientRulesJson, false, 0);
            }
            case NotificationEventCatalog.SESSION_OVERDUE -> {
                ensureTemplate(normalized, "in_app", "Session Overdue",
                        "Session with {{clientMrn}} appears overdue for follow-up.");
                ensureTemplate(normalized, "email", "Session Overdue: {{clientMrn}}",
                        EmailHtmlComponents.standardEventEmailBody(
                                "Session Overdue",
                                "An overdue session requires attention.",
                                EmailHtmlComponents.kvTable(
                                        EmailHtmlComponents.invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Last session", "{{sessionDateFormatted}}", true)),
                                null,
                                null));
                ensureTrigger(normalized, "Session Overdue Trigger", "session", recipientRulesJson, false, 0);
            }
            case NotificationEventCatalog.SESSION_COMPLETED -> {
                ensureTemplate(normalized, "in_app", "Session Completed",
                        "Session with {{clientMrn}} was completed on {{sessionDateFormatted}}.");
                ensureTemplate(normalized, "email", "Session Completed: {{clientMrn}}",
                        EmailHtmlComponents.standardEventEmailBody(
                                "Session Completed",
                                "A session for client <strong>{{clientMrn}}</strong> has been marked as completed:",
                                EmailHtmlComponents.kvTable(
                                        EmailHtmlComponents.invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Date/Time", "{{sessionDateFormatted}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Service", "{{serviceName}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Therapist", "{{therapistName}}", true)),
                                null,
                                null));
                ensureTrigger(normalized, "Session Completed Trigger", "session", recipientRulesJson, false, 0);
            }
            case NotificationEventCatalog.BILL_GENERATED -> {
                String billingRecipientRulesJson = """
                        {"roles":["ADMIN","BILLING_SPECIALIST"],"assignedTherapist":true,"sessionClient":true}
                        """;
                ensureTemplate(normalized, "in_app", "Bill Generated",
                        "A new bill was generated for {{clientMrn}} ({{serviceName}}): {{totalAmount}}.");
                ensureTemplate(normalized, "email", "Bill Generated: {{clientMrn}}",
                        EmailHtmlComponents.invoiceReadyEmailBody());
                ensureTrigger(normalized, "Bill Generated Trigger", "session", billingRecipientRulesJson, false, 0);
            }
            case NotificationEventCatalog.BILL_DUE_REMINDER -> {
                String billingRecipientRulesJson = """
                        {"roles":["ADMIN","BILLING_SPECIALIST"],"sessionClient":true}
                        """;
                ensureTemplate(normalized, "in_app", "Billing Reminder",
                        "Billing reminder for {{clientMrn}}. Amount due: {{amountDue}}.");
                ensureTemplate(normalized, "email", "Billing Reminder: {{clientMrn}}",
                        EmailHtmlComponents.invoiceReminderEmailBody());
                ensureTrigger(normalized, "Bill Due Reminder Trigger", "session", billingRecipientRulesJson, true, 0);
            }
            case NotificationEventCatalog.PAYMENT_RECEIVED -> {
                String paymentRecipientRulesJson = """
                        {"roles":["ADMIN","BILLING_SPECIALIST"],"sessionClient":true}
                        """;
                ensureTemplate(normalized, "in_app", "Payment Received",
                        "Payment received for {{clientMrn}}. Paid amount: {{paidAmount}}.");
                ensureTemplate(normalized, "email", "Payment Received: {{clientMrn}}",
                        EmailHtmlComponents.paymentReceivedEmailBody());
                ensureTrigger(normalized, "Payment Received Trigger", "session", paymentRecipientRulesJson, false, 0);
            }
            case NotificationEventCatalog.PAYMENT_FAILED -> {
                String paymentRecipientRulesJson = """
                        {"roles":["ADMIN","BILLING_SPECIALIST"],"sessionClient":true}
                        """;
                ensureTemplate(normalized, "in_app", "Payment Failed",
                        "Payment failed for {{clientMrn}}. Please follow up.");
                ensureTemplate(normalized, "email", "Payment Failed: {{clientMrn}}",
                        EmailHtmlComponents.paymentFailedEmailBody());
                ensureTrigger(normalized, "Payment Failed Trigger", "session", paymentRecipientRulesJson, false, 0);
            }
            case NotificationEventCatalog.ORGANIZATION_USER_CREATED -> {
                String adminRecipientRulesJson = """
                        {"roles":["ADMIN","SUPER_ADMIN"]}
                        """;
                ensureTemplate(normalized, "in_app", "Organisation User Created",
                        "User {{createdUserName}} ({{createdUserEmail}}) was created.");
                ensureTemplate(normalized, "email", "New Organisation User: {{createdUserName}}",
                        EmailHtmlComponents.standardEventEmailBody(
                                "Organisation User Created",
                                "A new user account has been created.",
                                EmailHtmlComponents.kvTable(
                                        EmailHtmlComponents.invoiceKvRow("Name", "{{createdUserName}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Email", "{{createdUserEmail}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Created by", "{{actorName}}", true)),
                                null, null));
                ensureTrigger(normalized, "Organisation User Created Trigger", "user",
                        adminRecipientRulesJson, false, 0);
            }
            case NotificationEventCatalog.ORGANIZATION_USER_UPDATED -> {
                String adminRecipientRulesJson = """
                        {"roles":["ADMIN","SUPER_ADMIN"]}
                        """;
                ensureTemplate(normalized, "in_app", "Organisation User Updated",
                        "User {{userName}} ({{userEmail}}) was updated.");
                ensureTemplate(normalized, "email", "Organisation User Updated: {{userName}}",
                        EmailHtmlComponents.standardEventEmailBody(
                                "Organisation User Updated",
                                "A user account has been updated.",
                                EmailHtmlComponents.kvTable(
                                        EmailHtmlComponents.invoiceKvRow("Name", "{{userName}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Email", "{{userEmail}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Updated by", "{{actorName}}", true)),
                                null, null));
                ensureTrigger(normalized, "Organisation User Updated Trigger", "user",
                        adminRecipientRulesJson, false, 0);
            }
            case NotificationEventCatalog.ORGANIZATION_USER_DELETED -> {
                String adminRecipientRulesJson = """
                        {"roles":["ADMIN","SUPER_ADMIN"]}
                        """;
                ensureTemplate(normalized, "in_app", "Organisation User Deleted",
                        "User {{userName}} ({{userEmail}}) was deleted.");
                ensureTemplate(normalized, "email", "Organisation User Deleted: {{userName}}",
                        EmailHtmlComponents.standardEventEmailBody(
                                "Organisation User Deleted",
                                "A user account has been deleted.",
                                EmailHtmlComponents.kvTable(
                                        EmailHtmlComponents.invoiceKvRow("Name", "{{userName}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Email", "{{userEmail}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Deleted by", "{{actorName}}", true)),
                                null, null));
                ensureTrigger(normalized, "Organisation User Deleted Trigger", "user",
                        adminRecipientRulesJson, false, 0);
            }
            case NotificationEventCatalog.SUPERVISOR_ASSIGNMENT_CREATED -> {
                String supervisorRecipientRulesJson = """
                        {"roles":["ADMIN"],"assignedTherapist":true,"supervisorOfTherapist":true}
                        """;
                ensureTemplate(normalized, "in_app", "Supervisor Assignment Created",
                        "{{supervisorName}} assigned to therapist {{therapistName}}.");
                ensureTemplate(normalized, "email", "Supervisor Assignment Created",
                        EmailHtmlComponents.standardEventEmailBody(
                                "Supervisor Assignment Created",
                                "A new supervisor assignment has been created.",
                                EmailHtmlComponents.kvTable(
                                        EmailHtmlComponents.invoiceKvRow("Supervisor", "{{supervisorName}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Therapist", "{{therapistName}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Assignment type", "{{assignmentType}}", true)),
                                null, null));
                ensureTrigger(normalized, "Supervisor Assignment Created Trigger", "supervisor_assignment",
                        supervisorRecipientRulesJson, false, 0);
            }
            case NotificationEventCatalog.SUPERVISOR_ASSIGNMENT_UPDATED -> {
                String supervisorRecipientRulesJson = """
                        {"roles":["ADMIN"],"assignedTherapist":true,"supervisorOfTherapist":true}
                        """;
                ensureTemplate(normalized, "in_app", "Supervisor Assignment Updated",
                        "Supervisor assignment for {{therapistName}} ({{supervisorName}}) was updated.");
                ensureTemplate(normalized, "email", "Supervisor Assignment Updated",
                        EmailHtmlComponents.standardEventEmailBody(
                                "Supervisor Assignment Updated",
                                "A supervisor assignment has been updated.",
                                EmailHtmlComponents.kvTable(
                                        EmailHtmlComponents.invoiceKvRow("Supervisor", "{{supervisorName}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Therapist", "{{therapistName}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Assignment type", "{{assignmentType}}", true)),
                                null, null));
                ensureTrigger(normalized, "Supervisor Assignment Updated Trigger", "supervisor_assignment",
                        supervisorRecipientRulesJson, false, 0);
            }
            case NotificationEventCatalog.SUPERVISOR_ASSIGNMENT_DELETED -> {
                String supervisorRecipientRulesJson = """
                        {"roles":["ADMIN"],"assignedTherapist":true,"supervisorOfTherapist":true}
                        """;
                ensureTemplate(normalized, "in_app", "Supervisor Assignment Deleted",
                        "Supervisor assignment for {{therapistName}} ({{supervisorName}}) was deleted.");
                ensureTemplate(normalized, "email", "Supervisor Assignment Deleted",
                        EmailHtmlComponents.standardEventEmailBody(
                                "Supervisor Assignment Deleted",
                                "A supervisor assignment has been deleted.",
                                EmailHtmlComponents.kvTable(
                                        EmailHtmlComponents.invoiceKvRow("Supervisor", "{{supervisorName}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Therapist", "{{therapistName}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Assignment type", "{{assignmentType}}", true)),
                                null, null));
                ensureTrigger(normalized, "Supervisor Assignment Deleted Trigger", "supervisor_assignment",
                        supervisorRecipientRulesJson, false, 0);
            }
            case NotificationEventCatalog.CLIENT_CREATED -> {
                ensureTemplate(normalized, "in_app", "Client Created",
                        "New client {{clientMrn}} was created.");
                ensureTemplate(normalized, "email", "Client Created: {{clientMrn}}",
                        EmailHtmlComponents.standardEventEmailBody(
                                "Client Created",
                                "A new client profile has been created.",
                                EmailHtmlComponents.kvTable(
                                        EmailHtmlComponents.invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Client ID", "{{clientId}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Status", "{{status}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Stage", "{{stage}}", true)),
                                null, null));
                ensureTrigger(normalized, "Client Created Trigger", "client", WORKFLOW_EVENT_RECIPIENT_RULES, false, 0);
            }
            case NotificationEventCatalog.CLIENT_UPDATED -> {
                ensureTemplate(normalized, "in_app", "Client Updated",
                        "Client {{clientMrn}} was updated.");
                ensureTemplate(normalized, "email", "Client Updated: {{clientMrn}}",
                        EmailHtmlComponents.standardEventEmailBody(
                                "Client Updated",
                                "A client profile has been updated.",
                                EmailHtmlComponents.kvTable(
                                        EmailHtmlComponents.invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Client ID", "{{clientId}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Status", "{{status}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Stage", "{{stage}}", true)),
                                null, null));
                ensureTrigger(normalized, "Client Updated Trigger", "client", WORKFLOW_EVENT_RECIPIENT_RULES, false, 0);
            }
            case NotificationEventCatalog.CLIENT_DELETED -> {
                ensureTemplate(normalized, "in_app", "Client Deleted",
                        "Client {{clientMrn}} was deleted.");
                ensureTemplate(normalized, "email", "Client Deleted: {{clientMrn}}",
                        EmailHtmlComponents.standardEventEmailBody(
                                "Client Deleted",
                                "A client profile has been deleted.",
                                EmailHtmlComponents.kvTable(
                                        EmailHtmlComponents.invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                                        EmailHtmlComponents.invoiceKvRow("Client ID", "{{clientId}}", true)),
                                null, null));
                ensureTrigger(normalized, "Client Deleted Trigger", "client", WORKFLOW_EVENT_RECIPIENT_RULES, false, 0);
            }
            case NotificationEventCatalog.CHECKLIST_ASSIGNED -> {
                ensureTemplate(normalized, "in_app", "Checklist Assigned",
                        "Checklist \"{{checklistName}}\" assigned to {{clientMrn}} (therapist: {{therapistName}}).");
                ensureTemplate(normalized, "email", "Checklist Assigned: {{checklistName}} ({{clientMrn}})",
                        EmailHtmlComponents.checklistAssignedEmailBody());
                ensureTrigger(normalized, "Checklist Assigned Trigger", "checklist", WORKFLOW_EVENT_RECIPIENT_RULES, false, 0);
            }
            case NotificationEventCatalog.CHECKLIST_COMPLETED -> {
                ensureTemplate(normalized, "in_app", "Checklist Completed",
                        "Checklist \"{{checklistName}}\" completed for {{clientMrn}} (therapist: {{therapistName}}).");
                ensureTemplate(normalized, "email", "Checklist Completed: {{checklistName}} ({{clientMrn}})",
                        EmailHtmlComponents.checklistCompletedEmailBody());
                ensureTrigger(normalized, "Checklist Completed Trigger", "checklist", WORKFLOW_EVENT_RECIPIENT_RULES, false, 0);
            }
            case NotificationEventCatalog.CHECKLIST_ITEM_COMPLETED -> {
                ensureTemplate(normalized, "in_app", "Checklist Item Completed",
                        "Checklist item \"{{itemTitle}}\" has been marked complete for client {{clientMrn}} on checklist \"{{checklistName}}\".");
                ensureTemplate(normalized, "email", "Checklist Item Completed: {{clientMrn}}",
                        EmailHtmlComponents.checklistItemCompletedEmailBody());
                ensureTrigger(normalized, "Checklist Item Completed Trigger", "checklist", WORKFLOW_EVENT_RECIPIENT_RULES, false, 0);
            }
            case NotificationEventCatalog.FORM_ASSIGNED, NotificationEventCatalog.CLINICAL_FORM_ASSIGNED -> {
                ensureTemplate(normalized, "in_app", "Form Assigned",
                        "Form \"{{templateName}}\" assigned to {{clientMrn}} (therapist: {{therapistName}}).");
                ensureTemplate(normalized, "email", "Form Assigned: {{templateName}} ({{clientMrn}})",
                        EmailHtmlComponents.formAssignedEmailBody());
                ensureTrigger(normalized, "Form Assigned Trigger", "form", WORKFLOW_EVENT_RECIPIENT_RULES, false, 0);
            }
            case NotificationEventCatalog.FORM_COMPLETED, NotificationEventCatalog.CLINICAL_FORM_COMPLETED -> {
                ensureTemplate(normalized, "in_app", "Form Completed",
                        "Form \"{{templateName}}\" completed for {{clientMrn}} (therapist: {{therapistName}}).");
                ensureTemplate(normalized, "email", "Form Completed: {{templateName}} ({{clientMrn}})",
                        EmailHtmlComponents.formCompletedEmailBody());
                ensureTrigger(normalized, "Form Completed Trigger", "form", WORKFLOW_EVENT_RECIPIENT_RULES, false, 0);
            }
            case NotificationEventCatalog.CLIENT_ASSIGNED -> {
                ensureTemplate(normalized, "in_app", "Client Assigned",
                        "Client {{clientMrn}} assigned to {{therapistName}}.");
                ensureTemplate(normalized, "email", "Client Assigned: {{clientMrn}}",
                        EmailHtmlComponents.clientAssignedEmailBody());
                ensureTrigger(normalized, "Client Assigned Trigger", "client", WORKFLOW_EVENT_RECIPIENT_RULES, false, 0);
            }
            case NotificationEventCatalog.TASK_ASSIGNED -> {
                ensureTemplate(normalized, "in_app", "Task Assigned",
                        "Task \"{{title}}\" for {{clientMrn}} assigned to {{assignedToName}}.");
                ensureTemplate(normalized, "email", "Task Assigned: {{title}} ({{clientMrn}})",
                        EmailHtmlComponents.taskAssignedEmailBody());
                ensureTrigger(normalized, "Task Assigned Trigger", "task", WORKFLOW_EVENT_RECIPIENT_RULES, false, 0);
            }
            case NotificationEventCatalog.TASK_OVERDUE -> {
                ensureTemplate(normalized, "in_app", "Task Overdue",
                        "Task \"{{title}}\" for {{clientMrn}} is overdue (due {{dueDateFormatted}}).");
                ensureTemplate(normalized, "email", "Task Overdue: {{title}} ({{clientMrn}})",
                        EmailHtmlComponents.taskOverdueEmailBody());
                ensureTrigger(normalized, "Task Overdue Trigger", "task", WORKFLOW_EVENT_RECIPIENT_RULES, false, 0);
            }
            case NotificationEventCatalog.TASK_COMMENT_ADDED, NotificationEventCatalog.COMMENT_ADDED -> {
                ensureTemplate(normalized, "in_app", "Task Comment Added",
                        "{{authorName}} commented on \"{{title}}\" for {{clientMrn}}.");
                ensureTemplate(normalized, "email", "Task Comment: {{title}} ({{clientMrn}})",
                        EmailHtmlComponents.taskCommentEmailBody());
                ensureTrigger(normalized, "Task Comment Added Trigger", "task", WORKFLOW_EVENT_RECIPIENT_RULES, false, 0);
            }
            case NotificationEventCatalog.DOCUMENT_UPLOADED -> {
                ensureTemplate(normalized, "in_app", "Document Uploaded",
                        "Document \"{{fileName}}\" uploaded for {{clientMrn}}.");
                ensureTemplate(normalized, "email", "Document Uploaded: {{clientMrn}}",
                        EmailHtmlComponents.documentUploadedEmailBody());
                ensureTrigger(normalized, "Document Uploaded Trigger", "document", WORKFLOW_EVENT_RECIPIENT_RULES, false, 0);
            }
            case NotificationEventCatalog.ASSESSMENT_ASSIGNED -> {
                ensureTemplate(normalized, "in_app", "Assessment Assigned",
                        "Assessment \"{{templateName}}\" assigned to {{clientMrn}} (therapist: {{therapistName}}).");
                ensureTemplate(normalized, "email", "Assessment Assigned: {{templateName}} ({{clientMrn}})",
                        EmailHtmlComponents.assessmentAssignedStaffEmailBody());
                ensureTrigger(normalized, "Assessment Assigned Trigger", "assessment", WORKFLOW_EVENT_RECIPIENT_RULES, false, 0);
            }
            case NotificationEventCatalog.ASSESSMENT_COMPLETED -> {
                ensureTemplate(normalized, "in_app", "Assessment Completed",
                        "Assessment \"{{templateName}}\" completed for {{clientMrn}}.");
                ensureTemplate(normalized, "email", "Assessment Completed: {{templateName}} ({{clientMrn}})",
                        EmailHtmlComponents.assessmentCompletedStaffEmailBody());
                ensureTrigger(normalized, "Assessment Completed Trigger", "assessment", WORKFLOW_EVENT_RECIPIENT_RULES, false, 0);
            }
            case NotificationEventCatalog.SESSION_NOTE_CREATED -> {
                ensureTemplate(normalized, "in_app", "Session Note Created",
                        "Session note created for {{clientMrn}} by {{therapistName}}.");
                ensureTemplate(normalized, "email", "Session Note Created: {{clientMrn}}",
                        EmailHtmlComponents.sessionNoteEmailBody(
                                "Session Note Created",
                                "A session note was created for client <strong>{{clientMrn}}</strong>."));
                ensureTrigger(normalized, "Session Note Created Trigger", "session_note", WORKFLOW_EVENT_RECIPIENT_RULES, false, 0);
            }
            case NotificationEventCatalog.SESSION_NOTE_UPDATED -> {
                ensureTemplate(normalized, "in_app", "Session Note Updated",
                        "Session note updated for {{clientMrn}} by {{therapistName}}.");
                ensureTemplate(normalized, "email", "Session Note Updated: {{clientMrn}}",
                        EmailHtmlComponents.sessionNoteEmailBody(
                                "Session Note Updated",
                                "A session note was updated for client <strong>{{clientMrn}}</strong>."));
                ensureTrigger(normalized, "Session Note Updated Trigger", "session_note", WORKFLOW_EVENT_RECIPIENT_RULES, false, 0);
            }
            default -> {
                ensureGenericRequiredEventDefaults(normalized, WORKFLOW_EVENT_RECIPIENT_RULES);
            }
        }
    }

    private String emailBody(String heading, String introHtml, String detailInnerHtml,
            String buttonLabel, String buttonHrefPlaceholder) {
        return EmailHtmlComponents.standardEventEmailBody(
                heading,
                introHtml,
                StringUtils.hasText(detailInnerHtml) ? EmailHtmlComponents.detailCard(detailInnerHtml) : null,
                buttonLabel,
                buttonHrefPlaceholder);
    }

    /**
     * Repair legacy triggers that broadcast to every THERAPIST in the tenant. Existing tenants created
     * before scoped rules were introduced may still have those broad recipient rules in the database.
     */
    private void repairOverBroadTherapistRecipientRulesOnce() {
        String tenantKey = StringUtils.hasText(TenantContext.getSchemaName())
                ? TenantContext.getSchemaName()
                : (TenantContext.getOrganisationId() != null
                        ? "org:" + TenantContext.getOrganisationId()
                        : null);
        if (tenantKey == null || !repairedTherapistRuleTenants.add(tenantKey)) {
            return;
        }
        repairOverBroadTherapistRecipientRules();
    }

    private void repairOverBroadTherapistRecipientRules() {
        for (NotificationTrigger trigger : triggerRepository.findAll()) {
            String rawRules = trigger.getRecipientRules();
            if (!StringUtils.hasText(rawRules) || !rawRules.toUpperCase(Locale.ROOT).contains("THERAPIST")) {
                continue;
            }
            try {
                Map<String, Object> rules = objectMapper.readValue(rawRules, new TypeReference<Map<String, Object>>() {});
                Object rolesObj = rules.get("roles");
                if (!(rolesObj instanceof List<?> roles)) {
                    continue;
                }
                List<String> updatedRoles = roles.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .filter(role -> !RoleName.THERAPIST.name().equalsIgnoreCase(role.trim()))
                        .collect(Collectors.toList());
                if (updatedRoles.size() == roles.size()) {
                    continue;
                }
                rules.put("roles", updatedRoles);
                if (!Boolean.TRUE.equals(rules.get("assignedTherapist"))) {
                    rules.put("assignedTherapist", true);
                }
                trigger.setRecipientRules(objectMapper.writeValueAsString(rules));
                triggerRepository.save(trigger);
                log.info("Repaired notification trigger {} (event={}) to stop broadcasting to all therapists",
                        trigger.getName(), trigger.getEventType());
            } catch (JsonProcessingException ex) {
                log.warn("Failed to repair recipient rules for trigger id={}: {}", trigger.getId(), ex.getMessage());
            }
        }
    }

    private void ensureTemplate(String eventType, String channel, String subject, String bodyTemplate) {
        String name = eventType + "_" + channel;
        var existingOpt = templateRepository.findByName(name);
        if (existingOpt.isPresent()) {
            NotificationTemplate template = existingOpt.get();
            if (Boolean.TRUE.equals(template.getIsSystem())) {
                template.setSubject(subject);
                template.setBodyTemplate(bodyTemplate);
                if (!StringUtils.hasText(template.getEventType())) {
                    template.setEventType(eventType);
                }
                if (!StringUtils.hasText(template.getType())) {
                    template.setType(channel);
                }
                templateRepository.save(template);
            }
            return;
        }
        
        NotificationTemplate template = NotificationTemplate.builder()
                .name(name)
                .type(channel)
                .eventType(eventType)
                .subject(subject)
                .bodyTemplate(bodyTemplate)
                .isSystem(true)
                .isActive(true)
                .build();
        templateRepository.save(template);
    }

    private void ensureTrigger(String eventType, String triggerName, String entityType, String recipientRules,
            boolean isScheduled, int delayMinutes) {
        List<NotificationTrigger> existing = triggerRepository.findByEventType(eventType);
        if (existing != null && !existing.isEmpty()) {
            // Legacy ClientHub imports left multiple active triggers for the same event
            // (e.g. "Session Scheduled Trigger" + "Session Scheduled Notification"). Keep one
            // immediate canonical row active so admins are not confused by duplicate trigger names.
            deactivateDuplicateImmediateTriggers(eventType, existing, triggerName);
            return;
        }
        NotificationTrigger trigger = NotificationTrigger.builder()
                .name(triggerName)
                .eventType(eventType)
                .entityType(parseEntityType(entityType))
                .recipientRules(recipientRules)
                .priority("medium")
                .isScheduled(isScheduled)
                .delayMinutes(delayMinutes)
                .isActive(true)
                .build();
        triggerRepository.save(trigger);
    }

    /**
     * Deactivate extra immediate (non-scheduled) triggers for an event when more than one is active.
     * Prefers the canonical auto-created name; otherwise keeps the oldest active row.
     * Scheduled / conditioned reminder triggers are left alone.
     */
    private void deactivateDuplicateImmediateTriggers(
            String eventType, List<NotificationTrigger> existing, String preferredName) {
        List<NotificationTrigger> immediateActive = existing.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .filter(t -> !Boolean.TRUE.equals(t.getIsScheduled()))
                .filter(t -> !StringUtils.hasText(t.getConditionRules())
                        || "{}".equals(t.getConditionRules().trim()))
                .sorted(Comparator.comparing(NotificationTrigger::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        if (immediateActive.size() <= 1) {
            return;
        }

        NotificationTrigger keep = immediateActive.stream()
                .filter(t -> preferredName != null && preferredName.equalsIgnoreCase(t.getName()))
                .findFirst()
                .orElse(immediateActive.get(0));

        for (NotificationTrigger trigger : immediateActive) {
            if (Objects.equals(trigger.getId(), keep.getId())) {
                continue;
            }
            trigger.setIsActive(false);
            triggerRepository.save(trigger);
            log.info("Deactivated duplicate notification trigger id={} name={} for eventType={}",
                    trigger.getId(), trigger.getName(), eventType);
        }
    }

    private void ensureGenericRequiredEventDefaults(String eventType, String recipientRulesJson) {
        String title = toEventTitle(eventType);
        String inApp = title + " for {{clientMrn}} (therapist: {{therapistName}}).";
        String intro = "A <strong>" + title.toLowerCase(Locale.ROOT)
                + "</strong> notification was generated. Review the details below.";
        ensureTemplate(eventType, "in_app", title, inApp);
        ensureTemplate(eventType, "email", title + ": {{clientMrn}}",
                EmailHtmlComponents.standardEventEmailBody(
                        title,
                        intro,
                        EmailHtmlComponents.kvTable(
                                EmailHtmlComponents.invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                                EmailHtmlComponents.invoiceKvRow("Therapist", "{{therapistName}}", true)),
                        null,
                        null));
        String entityType = resolveEntityType(eventType, new HashMap<>());
        ensureTrigger(eventType, title + " Trigger", entityType, recipientRulesJson, false, 0);
    }

    private String toEventTitle(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return "Notification";
        }
        return Arrays.stream(eventType.split("_"))
                .filter(StringUtils::hasText)
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
    }

    private String processTemplate(String template, Map<String, Object> data, String fallback) {
        if (template == null) {
            return fallback;
        }

        String result = template;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            Object value = entry.getValue();
            result = result.replace(placeholder, value != null ? value.toString() : "");
        }
        return result;
    }

    private void recordDelivery(Notification notification, NotificationChannel channel, NotificationStatus status,
            String errorMessage, String errorCode, String recipientEmail) {
        if (notification == null || channel == null || status == null) {
            return;
        }
        NotificationDeliveryLog logEntry = NotificationDeliveryLog.builder()
                .notification(notification)
                .channel(channel)
                .status(status)
                .recipientEmail(recipientEmail)
                .provider(emailProviderService != null ? "EMAIL_PROVIDER" : (emailService != null ? "EMAIL_SERVICE" : "IN_APP"))
                .sentAt(status == NotificationStatus.SENT || status == NotificationStatus.DELIVERED ? Instant.now(clock) : null)
                .deliveredAt(status == NotificationStatus.DELIVERED ? Instant.now(clock) : null)
                .failedAt(status == NotificationStatus.FAILED ? Instant.now(clock) : null)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .build();
        deliveryLogRepository.save(logEntry);
    }

    private Instant calculateExecutionInstant(NotificationTrigger trigger, Map<String, Object> payload) {
        Instant baseInstant = extractEventInstant(payload);
        if (baseInstant == null) {
            return Instant.now(clock);
        }

        int offset = Optional.ofNullable(trigger.getDelayMinutes()).orElse(0);
        return baseInstant.minus(Duration.ofMinutes(offset));
    }

    private Instant extractEventInstant(Map<String, Object> payload) {
        Object value = firstNonNull(
                payload.get("executeAt"),
                payload.get("sessionDate"),
                payload.get("dueDate"),
                payload.get("scheduledAt"));
        return toInstant(value);
    }

    private Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        if (value instanceof String str) {
            try {
                return Instant.parse(str);
            } catch (DateTimeParseException ignored) {
                // fall through
            }
        }
        return null;
    }

    private String serializeEntityData(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(normalizeForSerialization(data));
            } catch (Exception fallbackException) {
                throw new IllegalStateException("Unable to serialize notification payload", fallbackException);
            }
        }
    }

    private Map<String, Object> normalizeForSerialization(Map<String, Object> payload) {
        Map<String, Object> normalized = new HashMap<>();
        if (payload == null) {
            return normalized;
        }
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            normalized.put(entry.getKey(), normalizeSerializableValue(entry.getValue()));
        }
        return normalized;
    }

    @SuppressWarnings("unchecked")
    private Object normalizeSerializableValue(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof Instant instantValue) {
            return instantValue.toString();
        }
        if (value instanceof java.time.temporal.TemporalAccessor temporalValue) {
            return temporalValue.toString();
        }
        if (value instanceof java.util.Date dateValue) {
            return dateValue.toInstant().toString();
        }
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> nested = new HashMap<>();
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                nested.put(String.valueOf(entry.getKey()), normalizeSerializableValue(entry.getValue()));
            }
            return nested;
        }
        if (value instanceof Collection<?> collectionValue) {
            List<Object> normalized = new ArrayList<>(collectionValue.size());
            for (Object item : collectionValue) {
                normalized.add(normalizeSerializableValue(item));
            }
            return normalized;
        }
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            List<Object> normalized = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                normalized.add(normalizeSerializableValue(java.lang.reflect.Array.get(value, i)));
            }
            return normalized;
        }

        try {
            return objectMapper.treeToValue(objectMapper.valueToTree(value), Object.class);
        } catch (Exception ignored) {
            return String.valueOf(value);
        }
    }

    private Map<String, Object> deserializeEntityData(String json) {
        if (isBlank(json)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to deserialize notification payload", e);
        }
    }

    private Object normalizeValue(Object value) {
        if (value instanceof String str) {
            return str.trim();
        }
        return value;
    }

    private <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private void addIfPresent(Set<Long> target, Long value) {
        if (value != null) {
            target.add(value);
        }
    }

    private String resolveEntityType(String eventType, Map<String, Object> payload) {
        if (payload.containsKey("entityType")) {
            return String.valueOf(payload.get("entityType"));
        }
        if (eventType == null) {
            return "general";
        }
        return switch (eventType) {
            case "client_created", "client_updated", "client_deleted", "client_assigned" -> "client";
            case "session_scheduled", "session_rescheduled", "session_completed", "session_cancelled",
                    "session_reminder", "session_overdue", "session_note_created", "session_note_updated" -> "session";
            case "task_assigned", "task_overdue", "task_comment_added", "comment_added" -> "task";
            case "checklist_assigned", "checklist_completed", "checklist_item_completed" -> "checklist";
            case "bill_generated", "bill_due_reminder", "payment_received", "payment_failed" -> "billing";
            case "form_assigned", "form_completed", "clinical_form_assigned", "clinical_form_completed" -> "form";
            case "document_uploaded", "document_needs_review" -> "document";
            case "assessment_assigned", "assessment_completed" -> "assessment";
            default -> "general";
        };
    }

    private com.smart.therapy.flow.notification.enums.EntityType parseEntityType(String value) {
        if (!StringUtils.hasText(value)) {
            return com.smart.therapy.flow.notification.enums.EntityType.GENERAL;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return com.smart.therapy.flow.notification.enums.EntityType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown notification entityType '{}'; defaulting to GENERAL", value);
            return com.smart.therapy.flow.notification.enums.EntityType.GENERAL;
        }
    }

    private Long resolveEntityId(Map<String, Object> payload) {
        Long directId = getLongValue(payload.get("id"));
        if (directId != null) {
            return directId;
        }
        return firstNonNull(
                getLongValue(payload.get("clientId")),
                getLongValue(payload.get("sessionId")),
                getLongValue(payload.get("documentId")),
                getLongValue(payload.get("assessmentId")));
    }

    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getIntegerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private NotificationResponse toNotificationResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUser() != null ? notification.getUser().getId() : null)
                .clientId(notification.getClient() != null ? notification.getClient().getId() : null)
                .type(notification.getType())
                .category(notification.getCategory())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .data(notification.getData())
                .priority(notification.getPriority() != null ? notification.getPriority().getDisplayName() : null)
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .actionUrl(notification.getActionUrl())
                .actionLabel(notification.getActionLabel())
                .relatedEntityType(notification.getRelatedEntityType())
                .relatedEntityId(notification.getRelatedEntityId())
                .expiresAt(notification.getExpiresAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private NotificationTriggerResponse toTriggerResponse(NotificationTrigger trigger) {
        return NotificationTriggerResponse.builder()
                .id(trigger.getId())
                .name(trigger.getName())
                .description(trigger.getDescription())
                .eventType(trigger.getEventType())
                .entityType(trigger.getEntityType())
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

    private String resolveTriggerPriority(String priorityRaw) {
        if (!StringUtils.hasText(priorityRaw)) {
            return "medium";
        }
        try {
            NotificationPriority.valueOf(priorityRaw.trim().toUpperCase());
            return priorityRaw.trim().toLowerCase();
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid priority. Allowed values: LOW, MEDIUM, HIGH, URGENT");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private NotificationTemplateResponse toTemplateResponse(NotificationTemplate template) {
        String channelType = template.getType();
        String eventType = StringUtils.hasText(template.getEventType())
                ? template.getEventType().trim()
                : deriveEventTypeFromTemplateName(template.getName(), channelType);
        return NotificationTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .type(channelType)
                .eventType(eventType)
                .subject(template.getSubject())
                .bodyTemplate(template.getBodyTemplate())
                .isSystem(template.getIsSystem())
                .isActive(template.getIsActive())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    private String resolveTemplateChannelType(String type, String eventType) {
        if (StringUtils.hasText(type) && isTemplateChannelValue(type)) {
            return normalizeTemplateChannelType(type);
        }
        if (StringUtils.hasText(eventType) && isTemplateChannelValue(eventType)) {
            return normalizeTemplateChannelType(eventType);
        }
        if (StringUtils.hasText(type)) {
            return normalizeTemplateChannelType(type);
        }
        return "IN_APP";
    }

    private String resolveTemplateEventType(String eventType, String name, String channelType) {
        if (StringUtils.hasText(eventType) && !isTemplateChannelValue(eventType)) {
            return eventType.trim();
        }
        String derived = deriveEventTypeFromTemplateName(name, channelType);
        return StringUtils.hasText(derived) ? derived : null;
    }

    private boolean isTemplateChannelValue(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return "in_app".equals(normalized) || "email".equals(normalized) || "sms".equals(normalized);
    }

    private String normalizeTemplateChannelType(String value) {
        if (!StringUtils.hasText(value)) {
            return "IN_APP";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "in_app" -> "IN_APP";
            case "email" -> "EMAIL";
            case "sms" -> "SMS";
            default -> value.trim().toUpperCase(Locale.ROOT);
        };
    }

    private String deriveEventTypeFromTemplateName(String name, String channelType) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String normalizedName = name.trim().toLowerCase(Locale.ROOT);
        for (String suffix : List.of("_in_app", "_email", "_sms", "_emaill")) {
            if (normalizedName.endsWith(suffix)) {
                String event = name.trim().substring(0, name.trim().length() - suffix.length()).trim();
                return StringUtils.hasText(event) ? event : null;
            }
        }
        if (StringUtils.hasText(channelType)
                && normalizedName.endsWith("_" + channelType.trim().toLowerCase(Locale.ROOT))) {
            int cut = name.trim().length() - (channelType.trim().length() + 1);
            if (cut > 0) {
                String event = name.trim().substring(0, cut).trim();
                return StringUtils.hasText(event) ? event : null;
            }
        }
        return null;
    }

    private String normalizeJson(String json) {
        return isBlank(json) ? null : json.trim();
    }

    private Long resolveNotificationUserId(AuthPrincipal requester) {
        return currentUserService.requireCurrentUser(requester).getId();
    }

    private boolean shouldRestrictToOwnNotificationsOnly(AuthPrincipal requester) {
        return permissionChecker.hasRole(requester, RoleName.THERAPIST.name())
                && !permissionChecker.hasConsentAdminModuleAccess(requester)
                && !permissionChecker.hasRole(requester, RoleName.SUPERVISOR.name())
                && !permissionChecker.hasPermission(requester, "USER_MANAGE");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class ConditionRule {
        private String field;
        private String operator;
        private Object value;

        public String getField() {
            return field;
        }

        public String getOperator() {
            return operator;
        }

        public Object getValue() {
            return value;
        }
    }

    private static class RecipientRules {
        private List<String> roles;
        private List<Long> specificUsers;
        private Boolean assignedTherapist;
        private Boolean supervisorOfTherapist;
        private Boolean sessionClient;
        private Boolean uploader;

        public List<String> getRoles() {
            return roles;
        }

        public List<Long> getSpecificUsers() {
            return specificUsers;
        }

        public Boolean getAssignedTherapist() {
            return assignedTherapist;
        }

        public Boolean getSupervisorOfTherapist() {
            return supervisorOfTherapist;
        }

        public Boolean getSessionClient() {
            return sessionClient;
        }

        public Boolean getUploader() {
            return uploader;
        }
    }

    private static class RecipientTargets {
        private final List<Long> userIds;
        private final List<Long> clientIds;

        private RecipientTargets(List<Long> userIds, List<Long> clientIds) {
            this.userIds = userIds != null ? userIds : List.of();
            this.clientIds = clientIds != null ? clientIds : List.of();
        }

        public List<Long> getUserIds() {
            return userIds;
        }

        public List<Long> getClientIds() {
            return clientIds;
        }

        public boolean isEmpty() {
            return userIds.isEmpty() && clientIds.isEmpty();
        }
    }

    public interface EmailService {
        void sendNotificationEmail(User recipient,
                Notification inAppNotification,
                NotificationTemplate emailTemplate,
                Map<String, Object> entityData);
    }
}
