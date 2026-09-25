package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TableInventory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ClientHubMigrationReadinessPlanner {

    private static final List<EntityRequirement> REQUIREMENTS = List.of(
            new EntityRequirement(
                    "roles",
                    "roles",
                    "public.roles and tenant role assignments",
                    List.of("id", "name", "display_name", "is_system", "is_active")),
            new EntityRequirement(
                    "permissions",
                    "permissions",
                    "public.permissions and role permission links",
                    List.of("id", "name", "display_name", "category", "is_active")),
            new EntityRequirement(
                    "users",
                    "users",
                    "public.auth_identities plus tenant.users",
                    List.of("id", "username", "password", "full_name", "email", "role", "status")),
            new EntityRequirement(
                    "clients",
                    "clients",
                    "tenant.clients plus normalized contact/address/insurance tables",
                    List.of("id", "client_id", "full_name", "assigned_therapist_id", "status")),
            new EntityRequirement(
                    "services",
                    "services",
                    "tenant.services and legacy service mappings",
                    List.of("id", "service_code", "service_name", "duration", "base_rate")),
            new EntityRequirement(
                    "rooms",
                    "rooms",
                    "tenant.rooms and optional session room mappings",
                    List.of("id", "room_number", "room_name", "is_active")),
            new EntityRequirement(
                    "user_profiles",
                    "user_profiles",
                    "tenant.user_profiles scheduling + professional profile fields",
                    List.of("id", "user_id",
                            "license_number", "license_type", "license_state", "license_expiry", "license_status",
                            "specializations", "treatment_approaches", "age_groups", "languages",
                            "certifications", "education", "years_of_experience",
                            "working_hours", "working_days", "max_clients_per_day",
                            "session_duration", "availability_status", "virtual_room_id",
                            "available_physical_rooms",
                            "emergency_contact_name", "emergency_contact_phone",
                            "emergency_contact_relationship",
                            "previous_positions", "clinical_experience", "research_background",
                            "publications", "professional_memberships", "continuing_education",
                            "supervisory_experience", "award_recognitions", "professional_references",
                            "career_objectives")),
            new EntityRequirement(
                    "therapist_blocked_times",
                    "therapist_blocked_times",
                    "tenant.therapist_blocked_times",
                    List.of("id", "therapist_id", "start_time", "end_time", "all_day", "block_type",
                            "is_active")),
            new EntityRequirement(
                    "sessions",
                    "sessions",
                    "tenant.sessions",
                    List.of("id", "client_id", "therapist_id", "service_id", "session_date", "session_type", "status")),
            new EntityRequirement(
                    "session_integrations",
                    "sessions",
                    "tenant.session_integrations from V1 Zoom fields",
                    List.of("id", "zoom_enabled", "zoom_meeting_id", "zoom_join_url", "zoom_password")),
            new EntityRequirement(
                    "session_notes",
                    "session_notes",
                    "tenant.session_notes with encrypted clinical text",
                    List.of("id", "session_id", "client_id", "therapist_id", "date")),
            new EntityRequirement(
                    "documents",
                    "documents",
                    "tenant.documents plus file storage reconciliation",
                    List.of("id", "client_id", "file_name", "original_name", "file_size", "mime_type", "category")),
            new EntityRequirement(
                    "session_billing",
                    "session_billing",
                    "tenant.session_billing",
                    List.of("id", "session_id", "service_code", "units", "rate_per_unit", "total_amount", "payment_status")),
            new EntityRequirement(
                    "payment_transactions",
                    "payment_transactions",
                    "tenant.payments or payment allocation history",
                    List.of("id", "session_billing_id", "source", "amount", "recorded_at")),
            new EntityRequirement(
                    "tasks",
                    "tasks",
                    "tenant.tasks",
                    List.of("id", "client_id", "title", "status", "priority")),
            new EntityRequirement(
                    "task_comments",
                    "task_comments",
                    "tenant.task_comments",
                    List.of("id", "task_id", "author_id", "content")),
            new EntityRequirement(
                    "notification_templates",
                    "notification_templates",
                    "tenant.notification_templates",
                    List.of("id", "name", "type", "subject", "body_template")),
            new EntityRequirement(
                    "notification_triggers",
                    "notification_triggers",
                    "tenant.notification_triggers",
                    List.of("id", "name", "event_type", "entity_type", "is_active")),
            new EntityRequirement(
                    "notification_preferences",
                    "notification_preferences",
                    "tenant.notification_preferences",
                    List.of("id", "user_id", "trigger_type", "timing", "enable_in_app", "enable_email", "enable_sms")),
            new EntityRequirement(
                    "notifications",
                    "notifications",
                    "tenant.notifications",
                    List.of("id", "user_id", "type", "title", "message", "priority", "is_read")),
            new EntityRequirement(
                    "scheduled_notifications",
                    "scheduled_notifications",
                    "tenant.scheduled_notifications",
                    List.of("id", "trigger_id", "entity_type", "entity_id", "entity_data", "execute_at", "status")),
            new EntityRequirement(
                    "patient_consents",
                    "patient_consents",
                    "tenant.patient_consents",
                    List.of("id", "client_id", "consent_type", "consent_version", "granted", "granted_at")),
            new EntityRequirement(
                    "supervisor_assignments",
                    "supervisor_assignments",
                    "tenant.supervisor_assignments",
                    List.of("id", "supervisor_id", "therapist_id", "assigned_date", "is_active")),
            new EntityRequirement(
                    "client_portal",
                    "clients",
                    "public.auth_identities (CLIENT) plus tenant.client_portal_settings",
                    List.of("id", "has_portal_access", "portal_email", "portal_password", "last_login",
                            "activation_token")),
            new EntityRequirement(
                    "checklist_templates",
                    "checklist_templates",
                    "tenant.checklist_templates",
                    List.of("id", "name", "is_active", "sort_order")),
            new EntityRequirement(
                    "checklist_items",
                    "checklist_items",
                    "tenant.checklist_items",
                    List.of("id", "template_id", "title", "category", "is_required", "sort_order")),
            new EntityRequirement(
                    "client_checklists",
                    "client_checklists",
                    "tenant.client_checklists",
                    List.of("id", "client_id", "template_id", "is_completed")),
            new EntityRequirement(
                    "client_checklist_items",
                    "client_checklist_items",
                    "tenant.client_checklist_items",
                    List.of("id", "client_checklist_id", "checklist_item_id", "is_completed")),
            new EntityRequirement(
                    "user_integrations_zoom",
                    "users",
                    "tenant.user_integrations (zoom)",
                    List.of("id", "zoom_account_id", "zoom_client_id", "zoom_client_secret",
                            "zoom_access_token", "zoom_token_expiry")),
            new EntityRequirement(
                    "assessment_templates",
                    "assessment_templates",
                    "tenant.assessment_templates",
                    List.of("id", "name", "description", "category", "is_standardized", "is_active",
                            "created_by_id", "version")),
            new EntityRequirement(
                    "assessment_sections",
                    "assessment_sections",
                    "tenant.assessment_sections",
                    List.of("id", "template_id", "title", "access_level", "sort_order")),
            new EntityRequirement(
                    "assessment_questions",
                    "assessment_questions",
                    "tenant.assessment_questions",
                    List.of("id", "section_id", "question_text", "question_type", "is_required", "sort_order")),
            new EntityRequirement(
                    "assessment_question_options",
                    "assessment_question_options",
                    "tenant.assessment_question_options",
                    List.of("id", "question_id", "option_text", "option_value", "sort_order")),
            new EntityRequirement(
                    "assessment_assignments",
                    "assessment_assignments",
                    "tenant.assessment_assignments",
                    List.of("id", "template_id", "client_id", "assigned_by_id", "status")),
            new EntityRequirement(
                    "assessment_responses",
                    "assessment_responses",
                    "tenant.assessment_responses",
                    List.of("id", "assignment_id", "question_id", "responder_id")),
            new EntityRequirement(
                    "assessment_reports",
                    "assessment_reports",
                    "tenant.assessment_reports",
                    List.of("id", "assignment_id", "is_draft", "is_finalized", "created_by_id")),
            new EntityRequirement(
                    "session_transcripts",
                    "session_transcripts",
                    "tenant.session_transcripts (text only; no audio)",
                    List.of("id", "session_id", "client_id", "therapist_id", "content", "status", "created_at"))
    );

    ClientHubMigrationReadinessPlan buildPlan(SourceInventory inventory) {
        Map<String, TableInventory> tables = inventory.tablesByName();
        List<EntityReadiness> entities = new ArrayList<>(REQUIREMENTS.size());

        for (EntityRequirement requirement : REQUIREMENTS) {
            TableInventory table = tables.get(requirement.sourceTable());
            if (table == null) {
                entities.add(EntityReadiness.blocked(
                        requirement.entityName(),
                        requirement.sourceTable(),
                        requirement.targetArea(),
                        "Missing source table"));
                continue;
            }

            List<String> missingColumns = requirement.requiredColumns()
                    .stream()
                    .filter(column -> !table.hasColumn(column))
                    .toList();
            if (!missingColumns.isEmpty()) {
                entities.add(EntityReadiness.blocked(
                        requirement.entityName(),
                        requirement.sourceTable(),
                        requirement.targetArea(),
                        "Missing source columns: " + String.join(", ", missingColumns)));
                continue;
            }

            entities.add(EntityReadiness.ready(
                    requirement.entityName(),
                    requirement.sourceTable(),
                    requirement.targetArea(),
                    table.rowCount()));
        }

        return new ClientHubMigrationReadinessPlan(entities);
    }

    record ClientHubMigrationReadinessPlan(List<EntityReadiness> entities) {
        boolean readyForImportDesign() {
            return entities.stream().noneMatch(EntityReadiness::blocked);
        }

        long totalPlannedRows() {
            return entities.stream().mapToLong(EntityReadiness::sourceRows).sum();
        }
    }

    record EntityReadiness(
            String entityName,
            String sourceTable,
            String targetArea,
            long sourceRows,
            boolean blocked,
            String reason) {

        static EntityReadiness ready(String entityName, String sourceTable, String targetArea, long sourceRows) {
            return new EntityReadiness(entityName, sourceTable, targetArea, sourceRows, false, null);
        }

        static EntityReadiness blocked(String entityName, String sourceTable, String targetArea, String reason) {
            return new EntityReadiness(entityName, sourceTable, targetArea, 0, true, reason);
        }
    }

    private record EntityRequirement(
            String entityName,
            String sourceTable,
            String targetArea,
            List<String> requiredColumns) {
    }
}
