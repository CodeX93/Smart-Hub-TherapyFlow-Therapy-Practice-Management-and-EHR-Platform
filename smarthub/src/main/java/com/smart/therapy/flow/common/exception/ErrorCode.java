package com.smart.therapy.flow.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // Client errors (CLIENT_001 - CLIENT_099)
    CLIENT_NOT_FOUND("CLIENT_001", "Client not found"),
    CLIENT_EMAIL_EXISTS("CLIENT_002", "Email already exists"),
    CLIENT_DUPLICATE_DETECTED("CLIENT_003", "Duplicate client detected"),
    CLIENT_INVALID_STATUS_TRANSITION("CLIENT_004", "Invalid status transition"),
    CLIENT_PORTAL_ALREADY_ACTIVATED("CLIENT_005", "Client portal already activated"),
    CLIENT_PORTAL_DISABLED("CLIENT_006", "Portal is disabled. You cannot log in until the portal is activated."),
    CLIENT_PORTAL_ACTIVATION_PENDING("CLIENT_007", "Portal activation is pending. Please check your email to activate your portal and try logging in again."),
    
    // User errors (USER_001 - USER_099)
    USER_NOT_FOUND("USER_001", "User not found"),
    USER_EMAIL_EXISTS("USER_002", "Email already exists"),
    USER_USERNAME_EXISTS("USER_003", "Username already exists"),
    USER_INVALID_CREDENTIALS("USER_004", "Invalid credentials"),
    USER_ACCOUNT_DISABLED("USER_005", "User account is disabled"),
    USER_CANNOT_DELETE_SELF("USER_006", "You cannot delete your own account"),
    
    // Session errors (SESSION_001 - SESSION_099)
    SESSION_NOT_FOUND("SESSION_001", "Session not found"),
    SESSION_CONFLICT_DETECTED("SESSION_002", "Scheduling conflict detected"),
    SESSION_INVALID_BUSINESS_HOURS("SESSION_003", "Session scheduled outside business hours"),
    SESSION_CANNOT_CANCEL("SESSION_004", "Session cannot be cancelled"),
    SESSION_INVALID_STATUS_TRANSITION("SESSION_005", "Invalid status transition"),
    
    // Billing errors (BILLING_001 - BILLING_099)
    BILLING_SERVICE_NOT_FOUND("BILLING_001", "Service not found"),
    BILLING_ALREADY_EXISTS("BILLING_002", "Billing already exists for this session"),
    BILLING_INVOICE_NOT_FOUND("BILLING_003", "Invoice not found"),
    BILLING_PAYMENT_FAILED("BILLING_004", "Payment processing failed"),
    
    // Document errors (DOCUMENT_001 - DOCUMENT_099)
    DOCUMENT_NOT_FOUND("DOCUMENT_001", "Document not found"),
    DOCUMENT_UPLOAD_FAILED("DOCUMENT_002", "Document upload failed"),
    DOCUMENT_INVALID_TYPE("DOCUMENT_003", "Invalid document type"),
    DOCUMENT_ACCESS_DENIED("DOCUMENT_004", "Access denied to document"),
    
    // Assessment errors (ASSESSMENT_001 - ASSESSMENT_099)
    ASSESSMENT_TEMPLATE_NOT_FOUND("ASSESSMENT_001", "Assessment template not found"),
    ASSESSMENT_ASSIGNMENT_NOT_FOUND("ASSESSMENT_002", "Assessment assignment not found"),
    ASSESSMENT_ALREADY_SUBMITTED("ASSESSMENT_003", "Assessment already submitted"),
    ASSESSMENT_INVALID_RESPONSE("ASSESSMENT_004", "Invalid assessment response"),
    
    // Task errors (TASK_001 - TASK_099)
    TASK_NOT_FOUND("TASK_001", "Task not found"),
    TASK_ALREADY_COMPLETED("TASK_002", "Task already completed"),
    TASK_INVALID_STATUS_TRANSITION("TASK_003", "Invalid task status transition"),
    
    // Authentication errors (AUTH_001 - AUTH_099)
    AUTH_INVALID_TOKEN("AUTH_001", "Invalid or expired token"),
    AUTH_UNAUTHORIZED("AUTH_002", "Unauthorized access"),
    AUTH_FORBIDDEN("AUTH_003", "Access forbidden"),
    AUTH_INVALID_CREDENTIALS("AUTH_004", "Invalid credentials"),
    AUTH_ACCOUNT_LOCKED("AUTH_005", "Account locked due to multiple failed login attempts"),
    
    // Validation errors (VALIDATION_001 - VALIDATION_099)
    VALIDATION_FAILED("VALIDATION_001", "Validation failed"),
    VALIDATION_INVALID_EMAIL("VALIDATION_002", "Invalid email format"),
    VALIDATION_INVALID_PHONE("VALIDATION_003", "Invalid phone format"),
    VALIDATION_REQUIRED_FIELD("VALIDATION_004", "Required field is missing"),
    
    // Business logic errors (BUSINESS_001 - BUSINESS_099)
    BUSINESS_LOGIC_ERROR("BUSINESS_001", "Business logic error"),
    BUSINESS_INVALID_OPERATION("BUSINESS_002", "Invalid operation"),
    BUSINESS_CONSTRAINT_VIOLATION("BUSINESS_003", "Business constraint violated"),
    
    // Database errors (DB_001 - DB_099)
    DB_CONNECTION_ERROR("DB_001", "Database connection error"),
    DB_QUERY_ERROR("DB_002", "Database query error"),
    DB_CONSTRAINT_VIOLATION("DB_003", "Database constraint violation"),
    
    // External service errors (EXTERNAL_001 - EXTERNAL_099)
    EXTERNAL_SERVICE_UNAVAILABLE("EXTERNAL_001", "External service unavailable"),
    EXTERNAL_API_ERROR("EXTERNAL_002", "External API error"),
    EXTERNAL_TIMEOUT("EXTERNAL_003", "External service timeout"),
    EXTERNAL_STRIPE_ERROR("EXTERNAL_004", "Stripe payment error"),
    EXTERNAL_ZOOM_ERROR("EXTERNAL_005", "Zoom API error"),
    EXTERNAL_OPENAI_ERROR("EXTERNAL_006", "OpenAI API error"),
    EXTERNAL_SPARKPOST_ERROR("EXTERNAL_007", "SparkPost email error"),
    
    // Tenant errors (TENANT_001 - TENANT_099)
    TENANT_MAINTENANCE("TENANT_001", "Tenant is unavailable"),
    TENANT_FORCE_DISABLED("TENANT_002", "Tenant has been disabled"),

    // System errors (SYSTEM_001 - SYSTEM_099)
    SYSTEM_ERROR("SYSTEM_001", "Internal system error"),
    SYSTEM_CONFIGURATION_ERROR("SYSTEM_002", "System configuration error"),
    SYSTEM_MAINTENANCE("SYSTEM_003", "System under maintenance"),

    // Generic errors
    RESOURCE_NOT_FOUND("GENERIC_001", "Resource not found"),
    BAD_REQUEST("GENERIC_002", "Bad request"),
    INTERNAL_SERVER_ERROR("GENERIC_003", "Internal server error"),
    CONFLICT("GENERIC_004", "Resource conflict");
    
    private final String code;
    private final String message;
    
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}

