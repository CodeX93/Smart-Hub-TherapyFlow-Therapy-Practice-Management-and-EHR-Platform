package com.smart.therapy.flow.subscription.feature;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core feature codes enforced by backend logic.
 * These must be stable and referenced in plan entitlements and code gates.
 */
public enum CoreFeature {
    ADVANCED_BILLING("ADVANCED_BILLING", "Advanced billing features", ValueType.TOGGLE, false, null, null),
    CLIENT_LIMIT("CLIENT_LIMIT", "Maximum active clients", ValueType.LIMIT, false, 50, 1),
    THERAPIST_LIMIT("THERAPIST_LIMIT", "Maximum active therapists", ValueType.LIMIT, false, 1, 0),
    SUPERVISOR_LIMIT("SUPERVISOR_LIMIT", "Maximum active supervisors", ValueType.LIMIT, false, null, 0),
    SESSIONS_PER_MONTH("SESSIONS_PER_MONTH", "Monthly session limit", ValueType.LIMIT, false, null, 0),
    FORM_TEMPLATES("FORM_TEMPLATES", "Form template limit", ValueType.LIMIT, false, null, 0),
    CLIENT_PORTAL("CLIENT_PORTAL", "Client portal access", ValueType.TOGGLE, false, null, null),
    ROLES_PERMISSIONS("ROLES_PERMISSIONS", "Roles and permissions module", ValueType.TOGGLE, false, null, null),
    BILLING_MODULE("BILLING_MODULE", "Billing module access", ValueType.TOGGLE, false, null, null),
    AUDIT_EXPORT("AUDIT_EXPORT", "Audit export access", ValueType.TOGGLE, false, null, null),
    ASSESSMENT_TEMPLATES("ASSESSMENT_TEMPLATES", "Assessment template limit", ValueType.LIMIT, false, null, 0),
    AI_REPORTS_PER_MONTH("AI_REPORTS_PER_MONTH", "AI report generation limit per month", ValueType.LIMIT, false, 0, 0),
    AI_CONTENT_GENERATIONS_PER_MONTH("AI_CONTENT_GENERATIONS_PER_MONTH", "AI content generation limit per month", ValueType.LIMIT, false, null, 0),
    DOCUMENT_UPLOAD_GB("DOCUMENT_UPLOAD_GB", "Document upload storage limit (GB)", ValueType.LIMIT, false, null, 0),
    TASK_LIMIT("TASK_LIMIT", "Task creation limit", ValueType.LIMIT, false, null, 0),
    ZOOM_SESSIONS_PER_MONTH("ZOOM_SESSIONS_PER_MONTH", "Zoom sessions per month", ValueType.LIMIT, false, null, 0),
    STRIPE_PAYMENTS("STRIPE_PAYMENTS", "Stripe payment processing", ValueType.TOGGLE, false, null, null);

    public enum ValueType {
        TOGGLE,
        LIMIT
    }

    private final String code;
    private final String description;
    private final ValueType valueType;
    private final boolean catalogDefaultEnabled;
    private final Integer catalogDefaultUsageLimit;
    private final Integer minUsageLimit;

    CoreFeature(String code,
                String description,
                ValueType valueType,
                boolean catalogDefaultEnabled,
                Integer catalogDefaultUsageLimit,
                Integer minUsageLimit) {
        this.code = code;
        this.description = description;
        this.valueType = valueType;
        this.catalogDefaultEnabled = catalogDefaultEnabled;
        this.catalogDefaultUsageLimit = catalogDefaultUsageLimit;
        this.minUsageLimit = minUsageLimit;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public boolean isToggleType() {
        return valueType == ValueType.TOGGLE;
    }

    public boolean isLimitType() {
        return valueType == ValueType.LIMIT;
    }

    public boolean getCatalogDefaultEnabled() {
        return catalogDefaultEnabled;
    }

    public Integer getCatalogDefaultUsageLimit() {
        return catalogDefaultUsageLimit;
    }

    public Integer getMinUsageLimit() {
        return minUsageLimit;
    }

    private static final Map<String, CoreFeature> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toMap(CoreFeature::getCode, v -> v));

    public static boolean isCoreCode(String code) {
        return normalize(code) != null && BY_CODE.containsKey(normalize(code));
    }

    public static CoreFeature fromCode(String code) {
        String normalized = normalize(code);
        return normalized == null ? null : BY_CODE.get(normalized);
    }

    private static String normalize(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim().toUpperCase();
    }
}
