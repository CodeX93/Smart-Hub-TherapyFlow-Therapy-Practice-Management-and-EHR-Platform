package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubNotificationMigrationPlanner.NotificationMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.NotificationTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationPreferenceRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationTemplateRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationTriggerRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceScheduledNotificationRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHubNotificationMigrationPlannerTest {

    private final ClientHubNotificationMigrationPlanner planner = new ClientHubNotificationMigrationPlanner();

    @Test
    void countsCreatesMappedUpdatesAndNameMatches() {
        NotificationMigrationPlan plan = planner.buildPlan(
                inventory(0),
                List.of(
                        template("1", "Session Template"),
                        template("2", "Existing Template")),
                List.of(
                        trigger("10", "Session Scheduled Notification", null),
                        trigger("11", "Existing Trigger", "1")),
                List.of(
                        preference("20", "100", "session_scheduled"),
                        preference("21", "100", "__global__"),
                        preference("22", "100", "custom_unknown_type")),
                List.of(
                        notification("30", "100", "session_scheduled"),
                        notification("31", "100", "weird_legacy_type")),
                List.of(scheduled("40", "10", "200")),
                new NotificationTargetState(
                        Set.of("100"),
                        Set.of(),
                        Set.of("200"),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of("10"),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of("Existing Template"),
                        Set.of("Existing Trigger")));

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.templatesWouldCreate()).isEqualTo(1);
        assertThat(plan.templatesWouldUpdateMapped()).isEqualTo(0);
        assertThat(plan.templatesWouldUpdateExistingName()).isEqualTo(1);
        assertThat(plan.triggersWouldCreate()).isEqualTo(0);
        assertThat(plan.triggersWouldUpdateMapped()).isEqualTo(1);
        assertThat(plan.triggersWouldUpdateExistingName()).isEqualTo(1);
        assertThat(plan.preferencesWouldCreate()).isEqualTo(1);
        assertThat(plan.preferencesSkipped()).isEqualTo(2);
        assertThat(plan.notificationsWouldCreate()).isEqualTo(2);
        assertThat(plan.scheduledWouldCreate()).isEqualTo(1);
        assertThat(plan.warnings())
                .anyMatch(warning -> warning.contains("SYSTEM_MAINTENANCE"))
                .anyMatch(warning -> warning.contains("__global__"));
    }

    @Test
    void blocksUnmappedUsersTriggersAndSessions() {
        NotificationMigrationPlan plan = planner.buildPlan(
                inventory(1),
                List.of(new SourceNotificationTemplateRecord(
                        "1", "", "type", "subject", "body", null, null, null, null,
                        false, true, Instant.now(), Instant.now())),
                List.of(trigger("10", "Blocked Trigger", "missing-template"),
                        trigger("11", "Valid Trigger", null)),
                List.of(preference("20", "missing-user", "session_scheduled")),
                List.of(notification("30", "missing-user", "session_scheduled")),
                List.of(
                        scheduled("40", "missing-trigger", "200"),
                        scheduled("41", "10", "missing-session"),
                        scheduled("42", "11", "missing-session")),
                new NotificationTargetState(
                        Set.of(), Set.of(), Set.of("200"), Set.of(), Set.of(),
                        Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                        Set.of(), Set.of()));

        assertThat(plan.blocked()).isTrue();
        assertThat(plan.templatesBlocked()).isEqualTo(1);
        assertThat(plan.triggersBlocked()).isEqualTo(1);
        assertThat(plan.preferencesBlocked()).isEqualTo(1);
        assertThat(plan.notificationsBlocked()).isEqualTo(1);
        assertThat(plan.scheduledBlocked()).isEqualTo(2);
        assertThat(plan.scheduledSkipped()).isEqualTo(1);
        assertThat(plan.blockers())
                .anyMatch(blocker -> blocker.contains("Templates missing name"))
                .anyMatch(blocker -> blocker.contains("unmapped templates"))
                .anyMatch(blocker -> blocker.contains("unmapped users"))
                .anyMatch(blocker -> blocker.contains("unmapped triggers"));
        assertThat(plan.warnings())
                .anyMatch(warning -> warning.contains("session is unmapped"));
    }

    private SourceNotificationInventory inventory(long blankTemplateNames) {
        return new SourceNotificationInventory(
                2, blankTemplateNames, 0, 0, 0, 0,
                2, 0, 0, 0,
                3, 0, 0, 1,
                2, 0, 0, 0, 0,
                1, 0, 0, 0, 0);
    }

    private SourceNotificationTemplateRecord template(String id, String name) {
        return new SourceNotificationTemplateRecord(
                id, name, "email", "Subject", "Body {{name}}", null, null, null, null,
                false, true, Instant.now(), Instant.now());
    }

    private SourceNotificationTriggerRecord trigger(String id, String name, String templateId) {
        return new SourceNotificationTriggerRecord(
                id, name, "desc", "session_scheduled", "session", "{}", "{}", templateId,
                "medium", 0, 5, 10, false, true, Instant.now(), Instant.now());
    }

    private SourceNotificationPreferenceRecord preference(String id, String userId, String triggerType) {
        return new SourceNotificationPreferenceRecord(
                id, userId, triggerType, "[\"in_app\"]", "immediate",
                true, false, false, null, null, true, Instant.now(), Instant.now());
    }

    private SourceNotificationRecord notification(String id, String userId, String type) {
        return new SourceNotificationRecord(
                id, userId, type, "Title", "Message", null, "medium", false, null,
                null, null, null, null, "session", "200", Instant.now());
    }

    private SourceScheduledNotificationRecord scheduled(String id, String triggerId, String sessionId) {
        return new SourceScheduledNotificationRecord(
                id, triggerId, sessionId, "session", sessionId, "{}", Instant.now(),
                "pending", 0, null, Instant.now(), null);
    }
}
