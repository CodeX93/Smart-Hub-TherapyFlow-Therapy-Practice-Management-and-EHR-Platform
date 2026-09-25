package com.smart.therapy.flow.notification.enums;

/**
 * Delivery channels for notifications.
 * 
 * HIPAA Compliance Notes:
 * - EMAIL: Must use secure, encrypted email (TLS)
 * - PUSH: Must use encrypted push services
 * - SMS: Should be opt-in and avoid PHI in message content
 * - IN_APP: Most secure, requires authentication to view
 */
public enum NotificationChannel {
    /**
     * In-app notifications (most secure, requires authentication)
     * Always available, stores full notification details
     */
    IN_APP("In-App", true, true),
    
    /**
     * Email notifications (requires TLS encryption)
     * Can include more details, but should minimize PHI
     */
    EMAIL("Email", true, false),
    
    /**
     * Push notifications (via FCM/APNs)
     * Should contain minimal information, no PHI
     */
    PUSH("Push Notification", true, false),
    
    /**
     * SMS notifications (opt-in only)
     * Should contain only generic alerts, no PHI
     * Most expensive, use sparingly
     */
    SMS("SMS", false, false);
    
    private final String displayName;
    private final boolean enabledByDefault;
    private final boolean storesFullContent;
    
    NotificationChannel(String displayName, boolean enabledByDefault, boolean storesFullContent) {
        this.displayName = displayName;
        this.enabledByDefault = enabledByDefault;
        this.storesFullContent = storesFullContent;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }
    
    public boolean storesFullContent() {
        return storesFullContent;
    }
    
    /**
     * Returns whether this channel is suitable for PHI
     */
    public boolean isPhiSecure() {
        return this == IN_APP;
    }
}

