package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.NotificationTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationPreferenceRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationTemplateRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationTriggerRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceScheduledNotificationRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ClientHubNotificationMigrationPlanner {

    NotificationMigrationPlan buildPlan(
            SourceNotificationInventory inventory,
            List<SourceNotificationTemplateRecord> templates,
            List<SourceNotificationTriggerRecord> triggers,
            List<SourceNotificationPreferenceRecord> preferences,
            List<SourceNotificationRecord> notifications,
            List<SourceScheduledNotificationRecord> scheduled,
            NotificationTargetState targetState) {

        int templatesWouldCreate = 0;
        int templatesWouldUpdateMapped = 0;
        int templatesWouldUpdateExistingName = 0;
        int templatesBlocked = 0;
        for (SourceNotificationTemplateRecord source : templates) {
            if (source.missingRequiredFields()) {
                templatesBlocked++;
                continue;
            }
            if (targetState.mappedTemplateIds().contains(source.legacyTemplatePk())) {
                templatesWouldUpdateMapped++;
            } else if (targetState.existingTemplateNames().contains(source.name())) {
                templatesWouldUpdateExistingName++;
            } else {
                templatesWouldCreate++;
            }
        }

        Set<String> projectedTemplateIds = new HashSet<>(targetState.mappedTemplateIds());
        for (SourceNotificationTemplateRecord source : templates) {
            if (!source.missingRequiredFields()) {
                projectedTemplateIds.add(source.legacyTemplatePk());
            }
        }

        int triggersWouldCreate = 0;
        int triggersWouldUpdateMapped = 0;
        int triggersWouldUpdateExistingName = 0;
        int triggersBlocked = 0;
        int triggersUnmappedTemplate = 0;
        for (SourceNotificationTriggerRecord source : triggers) {
            if (source.missingRequiredFields()) {
                triggersBlocked++;
                continue;
            }
            if (source.templateLegacyId() != null && !projectedTemplateIds.contains(source.templateLegacyId())) {
                triggersBlocked++;
                triggersUnmappedTemplate++;
                continue;
            }
            if (targetState.mappedTriggerIds().contains(source.legacyTriggerPk())) {
                triggersWouldUpdateMapped++;
            } else if (targetState.existingTriggerNames().contains(source.name())) {
                triggersWouldUpdateExistingName++;
            } else {
                triggersWouldCreate++;
            }
        }

        Set<String> projectedTriggerIds = new HashSet<>(targetState.mappedTriggerIds());
        for (SourceNotificationTriggerRecord source : triggers) {
            if (!source.missingRequiredFields()
                    && (source.templateLegacyId() == null || projectedTemplateIds.contains(source.templateLegacyId()))) {
                projectedTriggerIds.add(source.legacyTriggerPk());
            }
        }

        int preferencesWouldCreate = 0;
        int preferencesWouldUpdateMapped = 0;
        int preferencesBlocked = 0;
        int preferencesSkippedGlobal = 0;
        int preferencesSkippedUnmappedType = 0;
        int preferencesUnmappedUser = 0;
        for (SourceNotificationPreferenceRecord source : preferences) {
            if (source.missingRequiredFields()) {
                preferencesBlocked++;
                continue;
            }
            if (ClientHubNotificationTypeMapper.isGlobalPreferenceTrigger(source.triggerType())) {
                preferencesSkippedGlobal++;
                continue;
            }
            if (ClientHubNotificationTypeMapper.mapType(source.triggerType()).isEmpty()) {
                preferencesSkippedUnmappedType++;
                continue;
            }
            if (!targetState.mappedUserIds().contains(source.userLegacyId())) {
                preferencesBlocked++;
                preferencesUnmappedUser++;
                continue;
            }
            if (targetState.mappedPreferenceIds().contains(source.legacyPreferencePk())) {
                preferencesWouldUpdateMapped++;
            } else {
                preferencesWouldCreate++;
            }
        }

        int notificationsWouldCreate = 0;
        int notificationsWouldUpdateMapped = 0;
        int notificationsBlocked = 0;
        int notificationsUnmappedUser = 0;
        int notificationsFallbackType = 0;
        for (SourceNotificationRecord source : notifications) {
            if (source.missingRequiredFields()) {
                notificationsBlocked++;
                continue;
            }
            if (!targetState.mappedUserIds().contains(source.userLegacyId())) {
                notificationsBlocked++;
                notificationsUnmappedUser++;
                continue;
            }
            if (ClientHubNotificationTypeMapper.mapType(source.type()).isEmpty()) {
                notificationsFallbackType++;
            }
            if (targetState.mappedNotificationIds().contains(source.legacyNotificationPk())) {
                notificationsWouldUpdateMapped++;
            } else {
                notificationsWouldCreate++;
            }
        }

        int scheduledWouldCreate = 0;
        int scheduledWouldUpdateMapped = 0;
        int scheduledBlocked = 0;
        int scheduledSkipped = 0;
        int scheduledUnmappedTrigger = 0;
        int scheduledUnmappedSession = 0;
        for (SourceScheduledNotificationRecord source : scheduled) {
            if (source.missingRequiredFields()) {
                scheduledBlocked++;
                continue;
            }
            if (!projectedTriggerIds.contains(source.triggerLegacyId())
                    && !targetState.mappedTriggerIds().contains(source.triggerLegacyId())) {
                scheduledBlocked++;
                scheduledUnmappedTrigger++;
                continue;
            }
            if (source.sessionLegacyId() != null
                    && !targetState.mappedSessionIds().contains(source.sessionLegacyId())) {
                // Sessions with no SmartHub mapping are skipped, not hard-blocked, so the rest of
                // the notification import can proceed (mirrors the 1-2 unmapped sessions elsewhere).
                scheduledSkipped++;
                scheduledUnmappedSession++;
                continue;
            }
            if (targetState.mappedScheduledIds().contains(source.legacyScheduledPk())) {
                scheduledWouldUpdateMapped++;
            } else {
                scheduledWouldCreate++;
            }
        }

        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        addCount(blockers, inventory.blankTemplateNameRows(), "Templates missing name");
        addCount(blockers, inventory.blankTemplateTypeRows(), "Templates missing type");
        addCount(blockers, inventory.blankTemplateSubjectRows(), "Templates missing subject");
        addCount(blockers, inventory.blankTemplateBodyRows(), "Templates missing body_template");
        addCount(blockers, inventory.blankTriggerNameRows(), "Triggers missing name");
        addCount(blockers, inventory.blankTriggerEventTypeRows(), "Triggers missing event_type");
        addCount(blockers, inventory.blankTriggerEntityTypeRows(), "Triggers missing entity_type");
        addCount(blockers, triggersUnmappedTemplate, "Triggers reference unmapped templates");
        addCount(blockers, inventory.preferenceMissingUserRows(), "Preferences missing user_id");
        addCount(blockers, inventory.preferenceBlankTriggerTypeRows(), "Preferences missing trigger_type");
        addCount(blockers, preferencesUnmappedUser, "Preferences reference unmapped users");
        addCount(blockers, inventory.notificationMissingUserRows(), "Notifications missing user_id");
        addCount(blockers, inventory.notificationBlankTypeRows(), "Notifications missing type");
        addCount(blockers, inventory.notificationBlankTitleRows(), "Notifications missing title");
        addCount(blockers, inventory.notificationBlankMessageRows(), "Notifications missing message");
        addCount(blockers, notificationsUnmappedUser, "Notifications reference unmapped users");
        addCount(blockers, inventory.scheduledMissingTriggerRows(), "Scheduled notifications missing trigger_id");
        addCount(blockers, inventory.scheduledBlankEntityTypeRows(), "Scheduled notifications missing entity_type");
        addCount(blockers, inventory.scheduledMissingEntityIdRows(), "Scheduled notifications missing entity_id");
        addCount(blockers, inventory.scheduledMissingExecuteAtRows(), "Scheduled notifications missing execute_at");
        addCount(blockers, scheduledUnmappedTrigger, "Scheduled notifications reference unmapped triggers");

        addCount(warnings, inventory.duplicateTemplateNameGroups(),
                "Duplicate source template name groups; execute keeps first match");
        addCount(warnings, preferencesSkippedGlobal,
                "Global preference rows skipped (__global__ quiet-hours settings are not ported as NotificationType rows)");
        addCount(warnings, preferencesSkippedUnmappedType,
                "Preference rows skipped because trigger_type has no SmartHub NotificationType mapping");
        addCount(warnings, notificationsFallbackType,
                "Notifications with unmapped type will import as SYSTEM_MAINTENANCE");
        addCount(warnings, scheduledUnmappedSession,
                "Scheduled notifications skipped because session is unmapped");

        return new NotificationMigrationPlan(
                templates.size(),
                templatesWouldCreate,
                templatesWouldUpdateMapped,
                templatesWouldUpdateExistingName,
                templatesBlocked,
                triggers.size(),
                triggersWouldCreate,
                triggersWouldUpdateMapped,
                triggersWouldUpdateExistingName,
                triggersBlocked,
                preferences.size(),
                preferencesWouldCreate,
                preferencesWouldUpdateMapped,
                preferencesBlocked,
                preferencesSkippedGlobal + preferencesSkippedUnmappedType,
                notifications.size(),
                notificationsWouldCreate,
                notificationsWouldUpdateMapped,
                notificationsBlocked,
                scheduled.size(),
                scheduledWouldCreate,
                scheduledWouldUpdateMapped,
                scheduledBlocked,
                scheduledSkipped,
                blockers,
                warnings);
    }

    private void addCount(List<String> items, long count, String label) {
        if (count > 0) {
            items.add(label + ": " + count);
        }
    }

    record NotificationMigrationPlan(
            int sourceTemplates,
            int templatesWouldCreate,
            int templatesWouldUpdateMapped,
            int templatesWouldUpdateExistingName,
            int templatesBlocked,
            int sourceTriggers,
            int triggersWouldCreate,
            int triggersWouldUpdateMapped,
            int triggersWouldUpdateExistingName,
            int triggersBlocked,
            int sourcePreferences,
            int preferencesWouldCreate,
            int preferencesWouldUpdateMapped,
            int preferencesBlocked,
            int preferencesSkipped,
            int sourceNotifications,
            int notificationsWouldCreate,
            int notificationsWouldUpdateMapped,
            int notificationsBlocked,
            int sourceScheduled,
            int scheduledWouldCreate,
            int scheduledWouldUpdateMapped,
            int scheduledBlocked,
            int scheduledSkipped,
            List<String> blockers,
            List<String> warnings) {

        boolean blocked() {
            return templatesBlocked > 0
                    || triggersBlocked > 0
                    || preferencesBlocked > 0
                    || notificationsBlocked > 0
                    || scheduledBlocked > 0
                    || !blockers.isEmpty();
        }
    }
}
