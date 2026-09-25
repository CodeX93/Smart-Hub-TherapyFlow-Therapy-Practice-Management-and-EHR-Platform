package com.smart.therapy.flow.notification.enums;

/**
 * High-level categories for grouping notification types.
 * Used for user preference management and filtering.
 */
public enum NotificationCategory {
    APPOINTMENT("Appointment Notifications", "Notifications about scheduled appointments"),
    FORM("Form Notifications", "Notifications about forms and assessments"),
    DOCUMENT("Document Notifications", "Notifications about shared documents"),
    SESSION("Session Notifications", "Notifications about therapy sessions"),
    BILLING("Billing Notifications", "Notifications about payments and invoices"),
    ACCOUNT("Account Notifications", "Notifications about your account"),
    SECURITY("Security Notifications", "Security and authentication notifications"),
    MESSAGE("Message Notifications", "Notifications about messages"),
    EMERGENCY("Emergency Notifications", "Critical notifications"),
    SYSTEM("System Notifications", "System and maintenance notifications"),
    INSURANCE("Insurance Notifications", "Insurance and authorization notifications");
    
    private final String displayName;
    private final String description;
    
    NotificationCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}

