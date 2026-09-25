package com.smart.therapy.flow.notification.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.exception.UnauthorizedException;
import com.smart.therapy.flow.notification.dto.NotificationPreferenceRequest;
import com.smart.therapy.flow.notification.dto.NotificationPreferenceResponse;
import com.smart.therapy.flow.notification.dto.NotificationResponse;
import com.smart.therapy.flow.notification.entity.Notification;
import com.smart.therapy.flow.notification.entity.NotificationPreference;
import com.smart.therapy.flow.notification.repository.NotificationPreferenceRepository;
import com.smart.therapy.flow.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extension to NotificationService for client-specific operations.
 * 
 * HIPAA Compliance:
 * - Clients can only access their own notifications
 * - All operations are logged
 * - Soft delete for data retention
 * - Access control enforced
 * 
 * This service provides methods for:
 * - Getting client notifications
 * - Marking client notifications as read
 * - Managing client notification preferences
 * - Soft deleting client notifications
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceExtension {

        private static final int DEFAULT_PAGE = 1;
        private static final int DEFAULT_PAGE_SIZE = 20;
        private static final int MAX_PAGE_SIZE = 100;

        private final NotificationRepository notificationRepository;
        private final NotificationPreferenceRepository preferenceRepository;
        private final ClientRepository clientRepository;

        // ========== Client Notification Operations ==========

        /**
         * Get notifications for a client
         */
        @Transactional(readOnly = true)
        public PaginatedResponse<NotificationResponse> getClientNotifications(
                Long clientId,
                boolean unreadOnly,
                int page,
                int pageSize) {
                log.debug("Getting notifications for client: clientId={}, unreadOnly={}, page={}, pageSize={}",
                        clientId, unreadOnly, page, pageSize);

                Client client = clientRepository.findById(clientId)
                                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

                int safePage = Math.max(page, DEFAULT_PAGE);
                int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
                PageRequest pageRequest = PageRequest.of(
                        safePage - 1,
                        safePageSize,
                        Sort.by(Sort.Direction.DESC, "createdAt"));

                Page<Notification> notifications = unreadOnly
                        ? notificationRepository.findByClientIdAndIsReadAndIsDeleted(client, false, false, pageRequest)
                        : notificationRepository.findByClientAndIsDeleted(client, false, pageRequest);

                List<NotificationResponse> items = notifications.getContent().stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());

                log.info("Found {} notifications for client: clientId={}, page={}",
                        items.size(), clientId, safePage);

                return PaginatedResponse.of(items, notifications.getTotalElements(), safePage, safePageSize);
        }

        /**
         * Get unread notification count for a client
         */
        @Transactional(readOnly = true)
        public long getClientUnreadCount(Long clientId) {
                log.debug("Getting unread count for client: clientId={}", clientId);

                Client client = clientRepository.findById(clientId)
                                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

                long count = notificationRepository.countByClientIdAndIsReadAndIsDeleted(client, false, false);
                log.debug("Client unread count: clientId={}, count={}", clientId, count);

                return count;
        }

        /**
         * Mark a client notification as read
         */
        @Transactional
        public void markClientNotificationAsRead(Long notificationId, Long clientId) {
                log.info("Marking client notification as read: notificationId={}, clientId={}", notificationId,
                                clientId);

                Notification notification = notificationRepository.findById(notificationId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Notification not found: " + notificationId));

                // Security check: ensure notification belongs to this client
                if (notification.getClient() == null || !notification.getClient().getId().equals(clientId)) {
                        log.warn("Client attempted to access notification of another client: clientId={}, notificationId={}",
                                        clientId, notificationId);
                        throw new UnauthorizedException("You can only access your own notifications");
                }

                if (!notification.getIsRead()) {
                        notification.markAsRead();
                        notificationRepository.save(notification);
                        log.info("Client notification marked as read: notificationId={}, clientId={}", notificationId,
                                        clientId);
                }
        }

        /**
         * Mark all client notifications as read
         */
        @Transactional
        public void markAllClientNotificationsAsRead(Long clientId) {
                log.info("Marking all client notifications as read: clientId={}", clientId);

                Client client = clientRepository.findById(clientId)
                                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

                List<Notification> unreadNotifications = notificationRepository
                                .findByClientIdAndIsReadAndIsDeletedOrderByCreatedAtDesc(client, false, false);

                Instant now = Instant.now();
                for (Notification notification : unreadNotifications) {
                        notification.setIsRead(true);
                        notification.setReadAt(now);
                }

                notificationRepository.saveAll(unreadNotifications);
                log.info("Marked {} notifications as read for client: clientId={}",
                                unreadNotifications.size(), clientId);
        }

        /**
         * Soft delete a client notification
         */
        @Transactional
        public void deleteClientNotification(Long notificationId, Long clientId) {
                log.info("Soft deleting client notification: notificationId={}, clientId={}", notificationId, clientId);

                Notification notification = notificationRepository.findById(notificationId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Notification not found: " + notificationId));

                // Security check: ensure notification belongs to this client
                if (notification.getClient() == null || !notification.getClient().getId().equals(clientId)) {
                        log.warn("Client attempted to delete notification of another client: clientId={}, notificationId={}",
                                        clientId, notificationId);
                        throw new UnauthorizedException("You can only delete your own notifications");
                }

                if (!notification.getIsDeleted()) {
                        notification.softDelete();
                        notificationRepository.save(notification);
                        log.info("Client notification soft deleted: notificationId={}, clientId={}", notificationId,
                                        clientId);
                }
        }

        // ========== Client Preference Operations ==========

        /**
         * Get notification preferences for a client
         */
        @Transactional(readOnly = true)
        public List<NotificationPreferenceResponse> getClientPreferences(Long clientId) {
                log.debug("Getting notification preferences for client: clientId={}", clientId);

                Client client = clientRepository.findById(clientId)
                                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

                List<NotificationPreference> preferences = preferenceRepository.findByClientId(client);

                return preferences.stream()
                                .map(this::mapPreferenceToResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Set notification preference for a client
         */
        @Transactional
        public NotificationPreferenceResponse setClientPreference(
                        Long clientId,
                        String notificationType,
                        NotificationPreferenceRequest request) {
                log.info("Setting notification preference for client: clientId={}, type={}", clientId,
                                notificationType);

                Client client = clientRepository.findById(clientId)
                                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

                // Find or create preference
                com.smart.therapy.flow.notification.enums.NotificationType type = com.smart.therapy.flow.notification.enums.NotificationType
                                .valueOf(notificationType.toUpperCase());

                NotificationPreference preference = preferenceRepository
                                .findByClientIdAndNotificationType(client, type)
                                .orElse(NotificationPreference.builder()
                                                .client(client)
                                                .notificationType(type)
                                                .build());

                // Update preference
                preference.setInAppEnabled(request.getInAppEnabled() != null ? request.getInAppEnabled() : true);
                preference.setEmailEnabled(request.getEmailEnabled() != null ? request.getEmailEnabled() : false);
                preference.setSmsEnabled(request.getSmsEnabled() != null ? request.getSmsEnabled() : false);
                preference.setPushEnabled(false); // Push not supported yet
                preference.setQuietHoursStart(request.getQuietHoursStart());
                preference.setQuietHoursEnd(request.getQuietHoursEnd());
                preference.setWeekendsEnabled(
                                request.getWeekendsEnabled() != null ? request.getWeekendsEnabled() : true);
                preference.setTiming(request.getTiming() != null ? request.getTiming()
                                : com.smart.therapy.flow.notification.enums.NotificationTiming.IMMEDIATE);

                preference = preferenceRepository.save(preference);
                log.info("Client notification preference updated: clientId={}, type={}", clientId, notificationType);

                return mapPreferenceToResponse(preference);
        }

        // ========== Helper Methods ==========

        private NotificationResponse mapToResponse(Notification notification) {
                return NotificationResponse.builder()
                                .id(notification.getId())
                                .type(notification.getType())
                                .title(notification.getTitle())
                                .message(notification.getMessage())
                                .priority(notification.getPriority() != null ? notification.getPriority().name() : null)
                                .isRead(notification.getIsRead())
                                .readAt(notification.getReadAt())
                                .actionUrl(notification.getActionUrl())
                                .actionLabel(notification.getActionLabel())
                                .relatedEntityType(notification.getRelatedEntityType())
                                .relatedEntityId(notification.getRelatedEntityId() != null
                                                ? notification.getRelatedEntityId().longValue()
                                                : null)
                                .createdAt(notification.getCreatedAt())
                                .build();
        }

        private NotificationPreferenceResponse mapPreferenceToResponse(NotificationPreference preference) {
                return NotificationPreferenceResponse.builder()
                                .id(preference.getId())
                                .notificationType(preference.getNotificationType()) // Map notificationType to
                                                                                    // triggerType
                                                                                    // for response
                                .inAppEnabled(preference.getInAppEnabled())
                                .emailEnabled(preference.getEmailEnabled())
                                .smsEnabled(preference.getSmsEnabled())
                                .quietHoursStart(preference.getQuietHoursStart())
                                .quietHoursEnd(preference.getQuietHoursEnd())
                                .weekendsEnabled(preference.getWeekendsEnabled())
                                .timing(preference.getTiming())
                                .createdAt(preference.getCreatedAt())
                                .updatedAt(preference.getUpdatedAt())
                                .build();
        }
}
