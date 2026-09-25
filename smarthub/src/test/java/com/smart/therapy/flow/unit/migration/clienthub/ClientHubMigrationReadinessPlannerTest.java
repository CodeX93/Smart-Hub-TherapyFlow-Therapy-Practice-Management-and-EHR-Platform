package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubMigrationReadinessPlanner.ClientHubMigrationReadinessPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.ColumnInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TableInventory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHubMigrationReadinessPlannerTest {

    private final ClientHubMigrationReadinessPlanner planner = new ClientHubMigrationReadinessPlanner();

    @Test
    void marksCoreEntitiesReadyWhenRequiredTablesAndColumnsExist() {
        SourceInventory inventory = new SourceInventory("public", List.of(
                table("roles", 2, "id", "name", "display_name", "is_system", "is_active"),
                table("permissions", 10, "id", "name", "display_name", "category", "is_active"),
                table("users", 4, "id", "username", "password", "full_name", "email", "role", "status",
                        "zoom_account_id", "zoom_client_id", "zoom_client_secret", "zoom_access_token",
                        "zoom_token_expiry"),
                table("clients", 20, "id", "client_id", "full_name", "assigned_therapist_id", "status",
                        "has_portal_access", "portal_email", "portal_password", "last_login", "activation_token"),
                table("services", 5, "id", "service_code", "service_name", "duration", "base_rate"),
                table("rooms", 3, "id", "room_number", "room_name", "is_active"),
                table("user_profiles", 4, "id", "user_id",
                        "license_number", "license_type", "license_state", "license_expiry", "license_status",
                        "specializations", "treatment_approaches", "age_groups", "languages",
                        "certifications", "education", "years_of_experience",
                        "working_hours", "working_days", "max_clients_per_day",
                        "session_duration", "availability_status", "virtual_room_id", "available_physical_rooms",
                        "emergency_contact_name", "emergency_contact_phone", "emergency_contact_relationship",
                        "previous_positions", "clinical_experience", "research_background", "publications",
                        "professional_memberships", "continuing_education", "supervisory_experience",
                        "award_recognitions", "professional_references", "career_objectives"),
                table("therapist_blocked_times", 2, "id", "therapist_id", "start_time", "end_time", "all_day",
                        "block_type", "is_active"),
                table("sessions", 40, "id", "client_id", "therapist_id", "service_id", "session_date", "session_type",
                        "status", "zoom_enabled", "zoom_meeting_id", "zoom_join_url", "zoom_password"),
                table("session_notes", 35, "id", "session_id", "client_id", "therapist_id", "date"),
                table("documents", 5, "id", "client_id", "file_name", "original_name", "file_size", "mime_type", "category"),
                table("session_billing", 40, "id", "session_id", "service_code", "units", "rate_per_unit", "total_amount", "payment_status"),
                table("payment_transactions", 8, "id", "session_billing_id", "source", "amount", "recorded_at"),
                table("tasks", 3, "id", "client_id", "title", "status", "priority"),
                table("task_comments", 2, "id", "task_id", "author_id", "content"),
                table("notification_templates", 4, "id", "name", "type", "subject", "body_template"),
                table("notification_triggers", 4, "id", "name", "event_type", "entity_type", "is_active"),
                table("notification_preferences", 6, "id", "user_id", "trigger_type", "timing",
                        "enable_in_app", "enable_email", "enable_sms"),
                table("notifications", 12, "id", "user_id", "type", "title", "message", "priority", "is_read"),
                table("scheduled_notifications", 5, "id", "trigger_id", "entity_type", "entity_id",
                        "entity_data", "execute_at", "status"),
                table("patient_consents", 7, "id", "client_id", "consent_type", "consent_version", "granted",
                        "granted_at"),
                table("supervisor_assignments", 3, "id", "supervisor_id", "therapist_id", "assigned_date",
                        "is_active"),
                table("checklist_templates", 2, "id", "name", "is_active", "sort_order"),
                table("checklist_items", 5, "id", "template_id", "title", "category", "is_required", "sort_order"),
                table("client_checklists", 4, "id", "client_id", "template_id", "is_completed"),
                table("client_checklist_items", 6, "id", "client_checklist_id", "checklist_item_id", "is_completed"),
                table("assessment_templates", 3, "id", "name", "description", "category", "is_standardized",
                        "is_active", "created_by_id", "version"),
                table("assessment_sections", 5, "id", "template_id", "title", "access_level", "sort_order"),
                table("assessment_questions", 10, "id", "section_id", "question_text", "question_type",
                        "is_required", "sort_order"),
                table("assessment_question_options", 8, "id", "question_id", "option_text", "option_value",
                        "sort_order"),
                table("assessment_assignments", 6, "id", "template_id", "client_id", "assigned_by_id", "status"),
                table("assessment_responses", 12, "id", "assignment_id", "question_id", "responder_id"),
                table("assessment_reports", 4, "id", "assignment_id", "is_draft", "is_finalized", "created_by_id"),
                table("session_transcripts", 160, "id", "session_id", "client_id", "therapist_id", "content",
                        "status", "created_at")
        ), "fingerprint");

        ClientHubMigrationReadinessPlan plan = planner.buildPlan(inventory);

        assertThat(plan.readyForImportDesign()).isTrue();
        assertThat(plan.totalPlannedRows()).isEqualTo(513);
        assertThat(plan.entities()).allMatch(entity -> !entity.blocked());
    }

    @Test
    void blocksEntityWhenRequiredColumnsAreMissing() {
        SourceInventory inventory = new SourceInventory("public", List.of(
                table("roles", 2, "id", "name", "display_name", "is_system", "is_active"),
                table("permissions", 10, "id", "name", "display_name", "category", "is_active"),
                table("users", 4, "id", "username", "password", "email", "role", "status")
        ), "fingerprint");

        ClientHubMigrationReadinessPlan plan = planner.buildPlan(inventory);

        assertThat(plan.readyForImportDesign()).isFalse();
        assertThat(plan.entities())
                .filteredOn(entity -> entity.entityName().equals("users"))
                .singleElement()
                .satisfies(entity -> {
                    assertThat(entity.blocked()).isTrue();
                    assertThat(entity.reason()).contains("full_name");
                });
        assertThat(plan.entities())
                .filteredOn(entity -> entity.entityName().equals("clients"))
                .singleElement()
                .satisfies(entity -> assertThat(entity.reason()).contains("Missing source table"));
    }

    private TableInventory table(String tableName, long rowCount, String... columnNames) {
        List<ColumnInventory> columns = List.of(columnNames)
                .stream()
                .map(column -> new ColumnInventory(column, "text", true, null))
                .toList();
        return new TableInventory(tableName, rowCount, columns);
    }
}
