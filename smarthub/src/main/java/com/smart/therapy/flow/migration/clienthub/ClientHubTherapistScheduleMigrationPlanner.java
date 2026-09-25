package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTherapistBlockedTimeRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTherapistScheduleInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceUserProfileScheduleRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TherapistScheduleTargetState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClientHubTherapistScheduleMigrationPlanner {

    public TherapistScheduleMigrationPlan buildPlan(
            SourceTherapistScheduleInventory inventory,
            List<SourceUserProfileScheduleRecord> profiles,
            List<SourceTherapistBlockedTimeRecord> blockedTimes,
            TherapistScheduleTargetState targetState) {
        int profilesWouldCreate = 0;
        int profilesWouldUpdateMapped = 0;
        int profilesWouldUpdateExistingUser = 0;
        int profilesBlocked = 0;
        int profilesWithUnmappedTherapist = 0;
        int profilesWithInvalidWorkingHours = 0;
        int profilesWithWorkingHours = 0;
        int profilesWithWorkingDaysFallback = 0;

        for (SourceUserProfileScheduleRecord source : profiles) {
            if (source.missingRequiredFields()) {
                profilesBlocked++;
                continue;
            }
            if (!targetState.mappedUserIds().contains(source.userLegacyId())) {
                profilesBlocked++;
                profilesWithUnmappedTherapist++;
                continue;
            }
            try {
                List<ClientHubWorkingHoursParser.ParsedShift> shifts =
                        ClientHubWorkingHoursParser.parse(source.workingHoursJson());
                if (!shifts.isEmpty()) {
                    profilesWithWorkingHours++;
                } else if (source.workingDays() != null && !source.workingDays().isEmpty()) {
                    profilesWithWorkingDaysFallback++;
                }
            } catch (IllegalArgumentException ex) {
                profilesWithInvalidWorkingHours++;
            }

            if (targetState.mappedProfileIds().contains(source.legacyProfilePk())) {
                profilesWouldUpdateMapped++;
            } else if (targetState.existingProfileUserLegacyIds().contains(source.userLegacyId())) {
                profilesWouldUpdateExistingUser++;
            } else {
                profilesWouldCreate++;
            }
        }

        int blockedWouldCreate = 0;
        int blockedWouldUpdateMapped = 0;
        int blockedBlocked = 0;
        int blockedWithUnmappedTherapist = 0;
        for (SourceTherapistBlockedTimeRecord source : blockedTimes) {
            if (source.missingRequiredFields()) {
                blockedBlocked++;
                continue;
            }
            if (!targetState.mappedUserIds().contains(source.therapistLegacyId())) {
                blockedBlocked++;
                blockedWithUnmappedTherapist++;
                continue;
            }
            if (targetState.mappedBlockedTimeIds().contains(source.legacyBlockedPk())) {
                blockedWouldUpdateMapped++;
            } else {
                blockedWouldCreate++;
            }
        }

        List<String> blockers = new ArrayList<>();
        addBlocker(blockers, inventory.blankUserIdRows(), "Profiles missing user_id");
        addBlocker(blockers, profilesWithUnmappedTherapist, "Profiles reference unmapped therapists");
        addBlocker(blockers, inventory.blankTherapistIdRows(), "Blocked times missing therapist_id");
        addBlocker(blockers, inventory.missingStartRows(), "Blocked times missing start_time");
        addBlocker(blockers, inventory.missingEndRows(), "Blocked times missing end_time");
        addBlocker(blockers, inventory.blankBlockTypeRows(), "Blocked times missing block_type");
        addBlocker(blockers, blockedWithUnmappedTherapist, "Blocked times reference unmapped therapists");

        List<String> warnings = new ArrayList<>();
        addWarning(warnings, profilesWithInvalidWorkingHours, "Profiles with unparseable working_hours JSON");
        addWarning(warnings, inventory.profilesWithVirtualRoom(), "Profiles with virtual_room_id (requires room mapping)");
        addWarning(warnings, inventory.profilesWithPhysicalRooms(),
                "Profiles with available_physical_rooms (requires room mapping)");
        addWarning(warnings, inventory.profilesWithLicense(), "Profiles carrying license fields");
        addWarning(warnings, inventory.profilesWithSpecializations(), "Profiles carrying specializations");
        addWarning(warnings, inventory.profilesWithEmergencyContact(), "Profiles carrying emergency contacts");

        return new TherapistScheduleMigrationPlan(
                profiles.size(),
                profilesWouldCreate,
                profilesWouldUpdateExistingUser,
                profilesWouldUpdateMapped,
                profilesBlocked,
                profilesWithWorkingHours,
                profilesWithWorkingDaysFallback,
                profilesWithInvalidWorkingHours,
                (int) inventory.profilesWithLicense(),
                (int) inventory.profilesWithSpecializations(),
                (int) inventory.profilesWithEmergencyContact(),
                blockedTimes.size(),
                blockedWouldCreate,
                blockedWouldUpdateMapped,
                blockedBlocked,
                blockers,
                warnings);
    }

    private void addBlocker(List<String> blockers, long count, String label) {
        if (count > 0) {
            blockers.add(label + ": " + count);
        }
    }

    private void addWarning(List<String> warnings, long count, String label) {
        if (count > 0) {
            warnings.add(label + ": " + count);
        }
    }

    public record TherapistScheduleMigrationPlan(
            int sourceProfiles,
            int profilesWouldCreate,
            int profilesWouldUpdateExistingUser,
            int profilesWouldUpdateMapped,
            int profilesBlocked,
            int profilesWithWorkingHours,
            int profilesWithWorkingDaysFallback,
            int profilesWithInvalidWorkingHours,
            int profilesWithLicense,
            int profilesWithSpecializations,
            int profilesWithEmergencyContact,
            int sourceBlockedTimes,
            int blockedWouldCreate,
            int blockedWouldUpdateMapped,
            int blockedBlocked,
            List<String> blockers,
            List<String> warnings) {

        public boolean blocked() {
            return profilesBlocked > 0 || blockedBlocked > 0 || !blockers.isEmpty();
        }
    }
}
