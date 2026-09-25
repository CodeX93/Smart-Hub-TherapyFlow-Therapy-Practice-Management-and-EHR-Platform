package com.smart.therapy.flow.common.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.notification.entity.Notification;
import com.smart.therapy.flow.notification.entity.NotificationTemplate;

import java.util.Map;

/**
 * Unified interface for email service providers.
 * Implementations can use SparkPost, Amazon SES, or other email services.
 */
public interface EmailProviderService {
    
    /**
     * Send a notification email to a user
     * 
     * @param recipient The user to send the email to
     * @param inAppNotification The in-app notification (can be null)
     * @param emailTemplate The email template with subject and body
     * @param entityData Template variables for rendering
     */
    void sendNotificationEmail(User recipient,
                              Notification inAppNotification,
                              NotificationTemplate emailTemplate,
                              Map<String, Object> entityData);
    
    /**
     * Send a simple email with subject and HTML body
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param htmlBody HTML email body
     */
    void sendEmail(String to, String subject, String htmlBody);
    
    /**
     * Get the from email address for this provider
     * 
     * @return From email address
     */
    String getFromAddress();
}

