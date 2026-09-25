package com.smart.therapy.flow.notification.service;

import java.util.List;
import java.util.Set;

/**
 * Canonical notification event catalog used across emitters and setup sync.
 * Keep this list additive and backward compatible.
 */
public final class NotificationEventCatalog {

    private NotificationEventCatalog() {
    }

    public static final String SESSION_SCHEDULED = "session_scheduled";
    public static final String SESSION_SERIES_SCHEDULED = "session_series_scheduled";
    public static final String SESSION_RESCHEDULED = "session_rescheduled";
    public static final String SESSION_CANCELLED = "session_cancelled";
    public static final String SESSION_REMINDER = "session_reminder";
    public static final String SESSION_OVERDUE = "session_overdue";
    public static final String SESSION_COMPLETED = "session_completed";

    public static final String BILL_GENERATED = "bill_generated";
    public static final String BILL_DUE_REMINDER = "bill_due_reminder";
    public static final String PAYMENT_RECEIVED = "payment_received";
    public static final String PAYMENT_FAILED = "payment_failed";

    public static final String ORGANIZATION_USER_CREATED = "organization_user_created";
    public static final String ORGANIZATION_USER_UPDATED = "organization_user_updated";
    public static final String ORGANIZATION_USER_DELETED = "organization_user_deleted";
    public static final String SUPERVISOR_ASSIGNMENT_CREATED = "supervisor_assignment_created";
    public static final String SUPERVISOR_ASSIGNMENT_UPDATED = "supervisor_assignment_updated";
    public static final String SUPERVISOR_ASSIGNMENT_DELETED = "supervisor_assignment_deleted";
    public static final String CLIENT_CREATED = "client_created";
    public static final String CLIENT_UPDATED = "client_updated";
    public static final String CLIENT_DELETED = "client_deleted";
    public static final String CLIENT_ASSIGNED = "client_assigned";
    public static final String TASK_ASSIGNED = "task_assigned";
    public static final String TASK_OVERDUE = "task_overdue";
    public static final String TASK_COMMENT_ADDED = "task_comment_added";
    public static final String CHECKLIST_ASSIGNED = "checklist_assigned";
    public static final String CHECKLIST_COMPLETED = "checklist_completed";
    public static final String CHECKLIST_ITEM_COMPLETED = "checklist_item_completed";
    public static final String FORM_ASSIGNED = "form_assigned";
    public static final String FORM_COMPLETED = "form_completed";
    public static final String CLINICAL_FORM_ASSIGNED = "clinical_form_assigned";
    public static final String CLINICAL_FORM_COMPLETED = "clinical_form_completed";
    public static final String DOCUMENT_UPLOADED = "document_uploaded";
    public static final String ASSESSMENT_ASSIGNED = "assessment_assigned";
    public static final String ASSESSMENT_COMPLETED = "assessment_completed";
    public static final String SESSION_NOTE_CREATED = "session_note_created";
    public static final String SESSION_NOTE_UPDATED = "session_note_updated";
    public static final String COMMENT_ADDED = "comment_added";

    public static final Set<String> REQUIRED_EVENTS = Set.of(
            SESSION_SCHEDULED,
            SESSION_SERIES_SCHEDULED,
            SESSION_RESCHEDULED,
            SESSION_CANCELLED,
            SESSION_REMINDER,
            SESSION_OVERDUE,
            SESSION_COMPLETED,
            BILL_GENERATED,
            BILL_DUE_REMINDER,
            PAYMENT_RECEIVED,
            PAYMENT_FAILED,
            ORGANIZATION_USER_CREATED,
            ORGANIZATION_USER_UPDATED,
            ORGANIZATION_USER_DELETED,
            SUPERVISOR_ASSIGNMENT_CREATED,
            SUPERVISOR_ASSIGNMENT_UPDATED,
            SUPERVISOR_ASSIGNMENT_DELETED,
            CLIENT_CREATED,
            CLIENT_UPDATED,
            CLIENT_DELETED,
            CLIENT_ASSIGNED,
            TASK_ASSIGNED,
            TASK_OVERDUE,
            TASK_COMMENT_ADDED,
            CHECKLIST_ASSIGNED,
            CHECKLIST_COMPLETED,
            CHECKLIST_ITEM_COMPLETED,
            FORM_ASSIGNED,
            FORM_COMPLETED,
            CLINICAL_FORM_ASSIGNED,
            CLINICAL_FORM_COMPLETED,
            DOCUMENT_UPLOADED,
            ASSESSMENT_ASSIGNED,
            ASSESSMENT_COMPLETED,
            SESSION_NOTE_CREATED,
            SESSION_NOTE_UPDATED,
            COMMENT_ADDED
    );

    public static final Set<String> SESSION_HEALTH_EVENTS = Set.of(
            SESSION_SCHEDULED,
            SESSION_SERIES_SCHEDULED,
            SESSION_RESCHEDULED,
            SESSION_CANCELLED,
            SESSION_REMINDER,
            SESSION_COMPLETED,
            BILL_GENERATED
    );

    public static final List<String> DEFAULT_CHANNELS = List.of("in_app", "email");

    public static final List<String> ORDERED_EVENTS = List.of(
            SESSION_SCHEDULED,
            SESSION_SERIES_SCHEDULED,
            SESSION_RESCHEDULED,
            SESSION_CANCELLED,
            SESSION_REMINDER,
            SESSION_OVERDUE,
            SESSION_COMPLETED,
            BILL_GENERATED,
            BILL_DUE_REMINDER,
            PAYMENT_RECEIVED,
            PAYMENT_FAILED,
            ORGANIZATION_USER_CREATED,
            ORGANIZATION_USER_UPDATED,
            ORGANIZATION_USER_DELETED,
            SUPERVISOR_ASSIGNMENT_CREATED,
            SUPERVISOR_ASSIGNMENT_UPDATED,
            SUPERVISOR_ASSIGNMENT_DELETED,
            CLIENT_CREATED,
            CLIENT_UPDATED,
            CLIENT_DELETED,
            CLIENT_ASSIGNED,
            TASK_ASSIGNED,
            TASK_OVERDUE,
            TASK_COMMENT_ADDED,
            CHECKLIST_ASSIGNED,
            CHECKLIST_COMPLETED,
            CHECKLIST_ITEM_COMPLETED,
            FORM_ASSIGNED,
            FORM_COMPLETED,
            CLINICAL_FORM_ASSIGNED,
            CLINICAL_FORM_COMPLETED,
            DOCUMENT_UPLOADED,
            ASSESSMENT_ASSIGNED,
            ASSESSMENT_COMPLETED,
            SESSION_NOTE_CREATED,
            SESSION_NOTE_UPDATED,
            COMMENT_ADDED
    );
}
