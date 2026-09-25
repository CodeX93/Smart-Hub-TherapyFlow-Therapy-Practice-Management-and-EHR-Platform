package com.smart.therapy.flow.notification.enums;

/**
 * Status of a notification throughout its lifecycle.
 * Used for tracking delivery and read status.
 */
public enum NotificationStatus {
    /**
     * Notification created but not yet sent
     */
    PENDING("Pending", false, false),
    
    /**
     * Notification is being sent (in queue)
     */
    SENDING("Sending", false, false),
    
    /**
     * Notification successfully sent to delivery service
     */
    SENT("Sent", true, false),
    
    /**
     * Notification delivered to recipient's device/inbox
     */
    DELIVERED("Delivered", true, false),
    
    /**
     * Notification was read/viewed by recipient
     */
    READ("Read", true, true),
    
    /**
     * Notification delivery failed
     */
    FAILED("Failed", false, false),
    
    /**
     * Notification delivery failed after retries
     */
    FAILED_PERMANENTLY("Failed Permanently", false, false),
    
    /**
     * Notification was dismissed by user without reading
     */
    DISMISSED("Dismissed", true, false),
    
    /**
     * Notification expired before delivery (time-sensitive)
     */
    EXPIRED("Expired", false, false),
    
    /**
     * Notification was cancelled before sending
     */
    CANCELLED("Cancelled", false, false);
    
    private final String displayName;
    private final boolean successfullyDelivered;
    private final boolean userInteracted;
    
    NotificationStatus(String displayName, boolean successfullyDelivered, boolean userInteracted) {
        this.displayName = displayName;
        this.successfullyDelivered = successfullyDelivered;
        this.userInteracted = userInteracted;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isSuccessfullyDelivered() {
        return successfullyDelivered;
    }
    
    public boolean isUserInteracted() {
        return userInteracted;
    }
    
    public boolean isFinal() {
        return this == DELIVERED 
            || this == READ 
            || this == FAILED_PERMANENTLY 
            || this == DISMISSED
            || this == EXPIRED
            || this == CANCELLED;
    }
}

