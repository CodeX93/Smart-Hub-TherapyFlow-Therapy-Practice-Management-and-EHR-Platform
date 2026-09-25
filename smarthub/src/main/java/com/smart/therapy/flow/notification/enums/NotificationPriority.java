package com.smart.therapy.flow.notification.enums;

/**
 * Priority levels for notifications.
 * Determines urgency and delivery behavior.
 */
public enum NotificationPriority {
    /**
     * Low priority - can be batched, delivered during normal hours
     * Examples: System updates, optional information
     */
    LOW(1, "Low", false, false),
    
    /**
     * Medium priority - standard delivery
     * Examples: Form reminders, document shared
     */
    MEDIUM(2, "Medium", false, false),
    
    /**
     * High priority - deliver promptly, may bypass quiet hours
     * Examples: Appointment reminders, payment due
     */
    HIGH(3, "High", true, false),
    
    /**
     * Urgent priority - immediate delivery, bypasses all restrictions
     * Examples: Appointment cancelled, emergency notifications
     */
    URGENT(4, "Urgent", true, true);
    
    private final int level;
    private final String displayName;
    private final boolean promptDelivery;
    private final boolean bypassQuietHours;
    
    NotificationPriority(int level, String displayName, boolean promptDelivery, boolean bypassQuietHours) {
        this.level = level;
        this.displayName = displayName;
        this.promptDelivery = promptDelivery;
        this.bypassQuietHours = bypassQuietHours;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isPromptDelivery() {
        return promptDelivery;
    }
    
    public boolean shouldBypassQuietHours() {
        return bypassQuietHours;
    }
    
    /**
     * Determines if this priority level is higher than another
     */
    public boolean isHigherThan(NotificationPriority other) {
        return this.level > other.level;
    }
}

