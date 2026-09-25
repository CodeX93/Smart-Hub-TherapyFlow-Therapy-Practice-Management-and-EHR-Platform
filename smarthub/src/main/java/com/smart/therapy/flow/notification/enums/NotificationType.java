package com.smart.therapy.flow.notification.enums;

/**
 * Types of notifications in the therapy flow system.
 * 
 * HIPAA Compliance Note:
 * - All notification types should avoid including PHI in titles/messages
 * - Use generic language that doesn't reveal the nature of the healthcare service
 * - Actual PHI details should only be visible after authentication
 */
public enum NotificationType {
    
    // ========== Appointment Notifications ==========
    APPOINTMENT_REMINDER("Upcoming Appointment Reminder", "You have an upcoming appointment.", NotificationCategory.APPOINTMENT),
    APPOINTMENT_CONFIRMED("Appointment Confirmed", "Your appointment has been confirmed.", NotificationCategory.APPOINTMENT),
    APPOINTMENT_CANCELLED("Appointment Cancelled", "An appointment has been cancelled.", NotificationCategory.APPOINTMENT),
    APPOINTMENT_RESCHEDULED("Appointment Rescheduled", "An appointment has been rescheduled.", NotificationCategory.APPOINTMENT),
    APPOINTMENT_24H_REMINDER("24-Hour Appointment Reminder", "You have an appointment tomorrow.", NotificationCategory.APPOINTMENT),
    APPOINTMENT_1H_REMINDER("1-Hour Appointment Reminder", "You have an appointment in 1 hour.", NotificationCategory.APPOINTMENT),
    
    // ========== Form & Document Notifications ==========
    FORM_ASSIGNED("New Form Assigned", "A new form has been assigned to you.", NotificationCategory.FORM),
    FORM_DUE_SOON("Form Due Soon", "A form is due soon. Please complete it.", NotificationCategory.FORM),
    FORM_OVERDUE("Form Overdue", "A form is now overdue.", NotificationCategory.FORM),
    FORM_SUBMITTED("Form Submitted", "Your form has been submitted successfully.", NotificationCategory.FORM),
    FORM_REVIEWED("Form Reviewed", "Your form has been reviewed.", NotificationCategory.FORM),
    DOCUMENT_SHARED("New Document Shared", "A document has been shared with you.", NotificationCategory.DOCUMENT),
    DOCUMENT_UPDATED("Document Updated", "A shared document has been updated.", NotificationCategory.DOCUMENT),
    
    // ========== Session Notes & Progress ==========
    SESSION_NOTES_AVAILABLE("Session Notes Available", "Session notes are now available.", NotificationCategory.SESSION),
    PROGRESS_REPORT_AVAILABLE("Progress Report Available", "Your progress report is ready.", NotificationCategory.SESSION),
    
    // ========== Billing & Payment ==========
    PAYMENT_DUE("Payment Due", "You have a payment due.", NotificationCategory.BILLING),
    PAYMENT_OVERDUE("Payment Overdue", "A payment is overdue.", NotificationCategory.BILLING),
    PAYMENT_RECEIVED("Payment Received", "Your payment has been received.", NotificationCategory.BILLING),
    PAYMENT_FAILED("Payment Failed", "A payment attempt failed.", NotificationCategory.BILLING),
    INVOICE_GENERATED("New Invoice", "A new invoice is available.", NotificationCategory.BILLING),
    
    // ========== Portal & Account ==========
    PORTAL_ACCESS_GRANTED("Portal Access Granted", "Your portal access is now active.", NotificationCategory.ACCOUNT),
    PASSWORD_RESET_REQUESTED("Password Reset", "A password reset was requested.", NotificationCategory.ACCOUNT),
    PASSWORD_CHANGED("Password Changed", "Your password has been changed.", NotificationCategory.ACCOUNT),
    ACCOUNT_LOCKED("Account Locked", "Your account has been locked.", NotificationCategory.SECURITY),
    ACCOUNT_UNLOCKED("Account Unlocked", "Your account has been unlocked.", NotificationCategory.SECURITY),
    
    // ========== Messages & Communication ==========
    NEW_MESSAGE("New Message", "You have a new message.", NotificationCategory.MESSAGE),
    MESSAGE_REPLY("Message Reply", "You received a reply to your message.", NotificationCategory.MESSAGE),
    
    // ========== Emergency & Crisis ==========
    CRISIS_RESOURCES_SHARED("Important Resources", "Important resources have been shared with you.", NotificationCategory.EMERGENCY),
    EMERGENCY_CONTACT_UPDATED("Emergency Contact Updated", "Your emergency contact has been updated.", NotificationCategory.EMERGENCY),
    
    // ========== System & Administrative ==========
    SYSTEM_MAINTENANCE("System Maintenance", "Scheduled system maintenance notification.", NotificationCategory.SYSTEM),
    SYSTEM_UPGRADE("System Upgrade", "System upgrade notification.", NotificationCategory.SYSTEM),
    POLICY_UPDATE("Policy Update", "Important policy update notification.", NotificationCategory.SYSTEM),
    
    // ========== Insurance & Authorization ==========
    INSURANCE_VERIFICATION_NEEDED("Insurance Verification Needed", "Please verify your insurance information.", NotificationCategory.INSURANCE),
    INSURANCE_AUTHORIZATION_EXPIRING("Authorization Expiring Soon", "Your insurance authorization is expiring soon.", NotificationCategory.INSURANCE),
    INSURANCE_CLAIM_PROCESSED("Claim Processed", "Your insurance claim has been processed.", NotificationCategory.INSURANCE),

    // ========== Integrations ==========
    ONLINE_BOOKING_REQUESTED("Online session requested",
            "A client asked about online sessions, but your Zoom account isn't connected.",
            NotificationCategory.SYSTEM);
    
    private final String defaultTitle;
    private final String defaultMessage;
    private final NotificationCategory category;
    
    NotificationType(String defaultTitle, String defaultMessage, NotificationCategory category) {
        this.defaultTitle = defaultTitle;
        this.defaultMessage = defaultMessage;
        this.category = category;
    }
    
    public String getDefaultTitle() {
        return defaultTitle;
    }
    
    public String getDefaultMessage() {
        return defaultMessage;
    }
    
    public NotificationCategory getCategory() {
        return category;
    }
    
    /**
     * Determines if this notification type requires immediate delivery
     * (bypassing quiet hours)
     */
    public boolean isUrgent() {
        return this == APPOINTMENT_1H_REMINDER 
            || this == APPOINTMENT_CANCELLED
            || this.category == NotificationCategory.EMERGENCY
            || this.category == NotificationCategory.SECURITY;
    }
    
    /**
     * Determines if this notification type contains time-sensitive information
     */
    public boolean isTimeSensitive() {
        return this.category == NotificationCategory.APPOINTMENT
            || this == FORM_DUE_SOON
            || this == FORM_OVERDUE
            || this == PAYMENT_DUE
            || this == PAYMENT_OVERDUE;
    }
}

