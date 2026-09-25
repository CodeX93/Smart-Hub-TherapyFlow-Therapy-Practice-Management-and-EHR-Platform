package com.smart.therapy.flow.document.enums;

/**
 * Status of a form template version.
 * - DRAFT: Version is being created/edited, not yet ready for use
 * - ACTIVE: Version is live and can be assigned to clients
 * - ARCHIVED: Version is no longer active but preserved for historical records
 */
public enum FormTemplateVersionStatus {
    DRAFT,
    ACTIVE,
    ARCHIVED
}
