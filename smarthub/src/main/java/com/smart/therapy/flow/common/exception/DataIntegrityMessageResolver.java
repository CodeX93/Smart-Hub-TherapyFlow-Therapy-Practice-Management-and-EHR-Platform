package com.smart.therapy.flow.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates database constraint violations into frontend-friendly messages.
 */
public final class DataIntegrityMessageResolver {

    private static final Pattern UNIQUE_CONSTRAINT = Pattern.compile(
            "duplicate key value violates unique constraint \"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern UNIQUE_KEY_DETAIL = Pattern.compile(
            "Key \\(([^)]+)\\)=\\(([^)]*)\\) already exists",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NOT_NULL = Pattern.compile(
            "null value in column \"([^\"]+)\" .* violates not-null constraint",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUE_TOO_LONG = Pattern.compile(
            "value too long for type character varying\\((\\d+)\\)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FOREIGN_KEY = Pattern.compile(
            "violates foreign key constraint \"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CHECK_CONSTRAINT = Pattern.compile(
            "violates check constraint \"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);

    private static final Map<String, String> CONSTRAINT_MESSAGES = Map.ofEntries(
            Map.entry("uq_users_email", "This email address is already in use by another user."),
            Map.entry("users_email_key", "This email address is already in use by another user."),
            Map.entry("idx_user_email", "This email address is already in use by another user."),
            Map.entry("ux_auth_identities_org_email", "This email address is already in use in this organisation."),
            Map.entry("ux_auth_identities_org_username", "This username is already in use in this organisation."),
            Map.entry("ux_auth_identities_platform_email", "This email address is already in use."),
            Map.entry("ux_auth_identities_platform_username", "This username is already in use."),
            Map.entry("uq_roles_org_name", "A role with this name already exists in your organisation."),
            Map.entry("roles_name_key", "A role with this name already exists."),
            Map.entry("ux_auth_identity_roles_auth_role_org", "This user already has the selected role."),
            Map.entry("ux_auth_identity_roles_auth_role_platform", "This user already has the selected role."),
            Map.entry("role_permissions_role_id_permission_id_key",
                    "One or more permissions are already assigned to this role."),
            Map.entry("uq_permissions_org_name", "A permission with this name already exists in your organisation."),
            Map.entry("permissions_name_key", "A permission with this name already exists."),
            Map.entry("uk_client_insurance_policy",
                    "This insurance policy number is already registered for this provider. Use a different policy number.")
    );

    private static final Map<String, String> COLUMN_LABELS = Map.ofEntries(
            Map.entry("email", "email"),
            Map.entry("full_name", "full name"),
            Map.entry("name", "name"),
            Map.entry("display_name", "display name"),
            Map.entry("description", "description"),
            Map.entry("permission_id", "permission"),
            Map.entry("role_id", "role"),
            Map.entry("organisation_id", "organisation"),
            Map.entry("license_number", "license number"),
            Map.entry("license_type", "license type"),
            Map.entry("license_state", "license state"),
            Map.entry("timezone", "timezone"),
            Map.entry("phone", "phone number"),
            Map.entry("virtual_room_id", "virtual room"),
            Map.entry("emergency_contact_name", "emergency contact name"),
            Map.entry("emergency_contact_phone", "emergency contact phone"),
            Map.entry("emergency_contact_email", "emergency contact email"),
            Map.entry("emergency_contact_relationship", "emergency contact relationship"),
            Map.entry("insurance_provider", "insurance provider"),
            Map.entry("policy_number", "policy number"),
            Map.entry("group_number", "group number"),
            Map.entry("insurance_phone", "insurance phone"),
            Map.entry("event_type", "event type"),
            Map.entry("entity_type", "entity type")
    );

    private DataIntegrityMessageResolver() {
    }

    public record ResolvedViolation(
            String message,
            String field,
            HttpStatus status,
            String code) {
    }

    public static ResolvedViolation resolve(Throwable throwable) {
        String rootMessage = extractRootMessage(throwable);
        if (!StringUtils.hasText(rootMessage)) {
            return genericViolation();
        }

        String lower = rootMessage.toLowerCase(Locale.ROOT);

        if (lower.contains("invalid input syntax for type json")) {
            return new ResolvedViolation(
                    "Invalid JSON format in request fields. Use valid JSON with double quotes.",
                    null,
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST.getCode());
        }

        Matcher uniqueConstraint = UNIQUE_CONSTRAINT.matcher(rootMessage);
        if (uniqueConstraint.find()) {
            return resolveUniqueViolation(uniqueConstraint.group(1), rootMessage);
        }

        Matcher notNull = NOT_NULL.matcher(rootMessage);
        if (notNull.find()) {
            String column = notNull.group(1);
            String field = toApiField(column);
            return new ResolvedViolation(
                    labelFor(column) + " is required and cannot be empty.",
                    field,
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.VALIDATION_FAILED.getCode());
        }

        Matcher tooLong = VALUE_TOO_LONG.matcher(rootMessage);
        if (tooLong.find()) {
            String maxLength = tooLong.group(1);
            String column = inferColumnFromSqlSnippet(rootMessage);
            String field = column != null ? toApiField(column) : null;
            String label = column != null ? labelFor(column) : "One of the fields";
            return new ResolvedViolation(
                    label + " cannot exceed " + maxLength + " characters.",
                    field,
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.VALIDATION_FAILED.getCode());
        }

        Matcher foreignKey = FOREIGN_KEY.matcher(rootMessage);
        if (foreignKey.find()) {
            String column = inferColumnFromSqlSnippet(rootMessage);
            String field = column != null ? toApiField(column) : null;
            String label = column != null ? labelFor(column) : "A related record";
            return new ResolvedViolation(
                    label + " is invalid or no longer exists. Please choose a valid value.",
                    field,
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.VALIDATION_FAILED.getCode());
        }

        Matcher checkConstraint = CHECK_CONSTRAINT.matcher(rootMessage);
        if (checkConstraint.find()) {
            String constraintName = checkConstraint.group(1);
            if (constraintName != null && constraintName.contains("notification_triggers_entity_type")) {
                return new ResolvedViolation(
                        "Invalid entity type. Allowed values: GENERAL, CLIENT, SESSION, TASK, CHECKLIST, BILLING, FORM, DOCUMENT, ASSESSMENT, USER, SUPERVISOR_ASSIGNMENT.",
                        "entityType",
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.VALIDATION_FAILED.getCode());
            }
            return new ResolvedViolation(
                    "One or more values are invalid. Please review your input and try again.",
                    null,
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.VALIDATION_FAILED.getCode());
        }

        if (lower.contains("duplicate key") || lower.contains("unique constraint")) {
            return resolveUniqueViolation(null, rootMessage);
        }

        return genericViolation();
    }

    private static ResolvedViolation resolveUniqueViolation(String constraintName, String rootMessage) {
        if (StringUtils.hasText(constraintName)) {
            String normalizedConstraint = constraintName.toLowerCase(Locale.ROOT);
            String known = CONSTRAINT_MESSAGES.get(normalizedConstraint);
            if (known != null) {
                return new ResolvedViolation(
                        known,
                        fieldForConstraint(normalizedConstraint),
                        HttpStatus.CONFLICT,
                        errorCodeForConstraint(normalizedConstraint));
            }
        }

        Matcher keyDetail = UNIQUE_KEY_DETAIL.matcher(rootMessage);
        if (keyDetail.find()) {
            String columns = keyDetail.group(1);
            String primaryColumn = primaryColumnFromKey(columns);
            String field = primaryColumn != null ? toApiField(primaryColumn) : null;
            String message = uniqueMessageForContext(rootMessage, primaryColumn, constraintName);
            return new ResolvedViolation(
                    message,
                    field,
                    HttpStatus.CONFLICT,
                    errorCodeForField(field));
        }

        return new ResolvedViolation(
                "A value you entered is already in use. Please use a different value.",
                null,
                HttpStatus.CONFLICT,
                ErrorCode.DB_CONSTRAINT_VIOLATION.getCode());
    }

    private static String fieldForConstraint(String constraintName) {
        if (constraintName.contains("email")) {
            return "email";
        }
        if (constraintName.contains("roles") && constraintName.contains("name")) {
            return "name";
        }
        if (constraintName.contains("auth_identity_roles")) {
            return "roles";
        }
        if (constraintName.contains("permissions") && constraintName.contains("name")) {
            return "name";
        }
        if (constraintName.contains("role_permissions")) {
            return "permissions";
        }
        return null;
    }

    private static String errorCodeForConstraint(String constraintName) {
        if (constraintName.contains("email")) {
            return ErrorCode.USER_EMAIL_EXISTS.getCode();
        }
        return ErrorCode.DB_CONSTRAINT_VIOLATION.getCode();
    }

    private static String errorCodeForField(String field) {
        if ("email".equals(field)) {
            return ErrorCode.USER_EMAIL_EXISTS.getCode();
        }
        return ErrorCode.DB_CONSTRAINT_VIOLATION.getCode();
    }

    private static String uniqueMessageForContext(String rootMessage, String primaryColumn, String constraintName) {
        String lower = rootMessage.toLowerCase(Locale.ROOT);
        if (lower.contains("role_permissions") || (constraintName != null && constraintName.contains("role_permissions"))) {
            return "One or more permissions are already assigned to this role.";
        }
        if (lower.contains(" roles") || lower.contains("public.roles") || lower.contains("into roles")
                || (constraintName != null && constraintName.contains("roles"))) {
            return "A role with this name already exists. Please choose a different role name.";
        }
        if (lower.contains(" permissions") || lower.contains("public.permissions")
                || (constraintName != null && constraintName.contains("permissions"))) {
            return "A permission with this name already exists. Please choose a different permission name.";
        }
        if (primaryColumn != null) {
            return "This " + labelFor(primaryColumn) + " is already in use. Please use a different value.";
        }
        return "A value you entered is already in use. Please use a different value.";
    }

    private static String primaryColumnFromKey(String columns) {
        if (!StringUtils.hasText(columns)) {
            return null;
        }
        String normalized = columns.replace(" ", "");
        if (normalized.contains("name")) {
            return "name";
        }
        if (normalized.contains("permission_id")) {
            return "permission_id";
        }
        if (normalized.contains("email")) {
            return "email";
        }
        int comma = normalized.indexOf(',');
        return comma > 0 ? normalized.substring(0, comma) : normalized;
    }

    private static String inferColumnFromSqlSnippet(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("insert into roles") || lower.contains("update roles")) {
            for (String column : List.of("display_name", "name", "description")) {
                if (message.contains(column + "=?") || message.contains(column + " =")) {
                    return column;
                }
            }
        }
        if (lower.contains("insert into role_permissions") || lower.contains("update role_permissions")) {
            if (message.contains("permission_id=?") || message.contains("permission_id =")) {
                return "permission_id";
            }
        }
        for (String column : COLUMN_LABELS.keySet()) {
            if (message.contains(column + "=?") || message.contains(column + " =")) {
                return column;
            }
        }
        return null;
    }

    private static String toApiField(String column) {
        if (!StringUtils.hasText(column)) {
            return null;
        }
        return switch (column) {
            case "full_name" -> "fullName";
            case "display_name" -> "displayName";
            case "license_number" -> "licenseNumber";
            case "license_type" -> "licenseType";
            case "license_state" -> "licenseState";
            case "license_expiry" -> "licenseExpiry";
            case "virtual_room_id" -> "virtualRoomId";
            case "permission_id" -> "permissions";
            case "role_id" -> "roleId";
            case "emergency_contact_name" -> "emergencyContactName";
            case "emergency_contact_phone" -> "emergencyContactPhone";
            case "emergency_contact_email" -> "emergencyContactEmail";
            case "emergency_contact_relationship" -> "emergencyContactRelationship";
            case "event_type" -> "eventType";
            case "entity_type" -> "entityType";
            default -> column;
        };
    }

    private static String labelFor(String column) {
        return COLUMN_LABELS.getOrDefault(column, column.replace('_', ' '));
    }

    private static String extractRootMessage(Throwable throwable) {
        Throwable current = throwable;
        String message = null;
        while (current != null) {
            if (StringUtils.hasText(current.getMessage())) {
                message = current.getMessage();
            }
            current = current.getCause();
        }
        return message;
    }

    private static ResolvedViolation genericViolation() {
        return new ResolvedViolation(
                "The request could not be saved because one or more values are invalid. Please review your input and try again.",
                null,
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_FAILED.getCode());
    }
}
