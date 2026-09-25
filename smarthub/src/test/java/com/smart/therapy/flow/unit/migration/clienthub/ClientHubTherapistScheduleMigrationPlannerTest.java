package com.smart.therapy.flow.unit.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubTherapistScheduleMigrationPlanner;
import com.smart.therapy.flow.migration.clienthub.ClientHubTherapistScheduleMigrationPlanner.TherapistScheduleMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTherapistBlockedTimeRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTherapistScheduleInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceUserProfileScheduleRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TherapistScheduleTargetState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHubTherapistScheduleMigrationPlannerTest {

    private final ClientHubTherapistScheduleMigrationPlanner planner = new ClientHubTherapistScheduleMigrationPlanner();

    @Test
    void countsCreateAndUpdateProfileAndBlockedTimeActions() {
        TherapistScheduleMigrationPlan plan = planner.buildPlan(
                inventory(2, 0, 1, 0, 0, 1, 1, 1, 2, 0, 0, 0, 0),
                List.of(
                        profile("1", "10", "{\"Monday\":{\"start\":\"09:00\",\"end\":\"17:00\"}}"),
                        profile("2", "11", null)),
                List.of(
                        blocked("100", "10"),
                        blocked("101", "11")),
                new TherapistScheduleTargetState(
                        Set.of("10", "11"),
                        Set.of("2"),
                        Set.of("101"),
                        Set.of(),
                        Set.of("11")));

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.profilesWouldCreate()).isEqualTo(1);
        assertThat(plan.profilesWouldUpdateMapped()).isEqualTo(1);
        assertThat(plan.profilesWithWorkingHours()).isEqualTo(1);
        assertThat(plan.profilesWithLicense()).isEqualTo(1);
        assertThat(plan.blockedWouldCreate()).isEqualTo(1);
        assertThat(plan.blockedWouldUpdateMapped()).isEqualTo(1);
    }

    @Test
    void blocksUnmappedTherapistsAndMissingFields() {
        TherapistScheduleMigrationPlan plan = planner.buildPlan(
                inventory(2, 1, 0, 1, 1, 0, 0, 0, 2, 1, 1, 1, 1),
                List.of(
                        blankUserProfile("1"),
                        profile("2", "missing", null)),
                List.of(
                        new SourceTherapistBlockedTimeRecord("1", null, null, null, false, null, null, false, null, true),
                        blocked("2", "missing")),
                new TherapistScheduleTargetState(Set.of(), Set.of(), Set.of(), Set.of(), Set.of()));

        assertThat(plan.blocked()).isTrue();
        assertThat(plan.profilesBlocked()).isEqualTo(2);
        assertThat(plan.blockedBlocked()).isEqualTo(2);
        assertThat(plan.blockers())
                .anyMatch(blocker -> blocker.contains("Profiles missing user_id"))
                .anyMatch(blocker -> blocker.contains("Profiles reference unmapped therapists"))
                .anyMatch(blocker -> blocker.contains("Blocked times missing therapist_id"));
    }

    private SourceTherapistScheduleInventory inventory(
            long profiles,
            long blankUser,
            long withHours,
            long withVirtual,
            long withPhysical,
            long withLicense,
            long withSpecs,
            long withEmergency,
            long blocked,
            long blankTherapist,
            long missingStart,
            long missingEnd,
            long blankType) {
        return new SourceTherapistScheduleInventory(
                profiles, blankUser, withHours, withVirtual, withPhysical,
                withLicense, withSpecs, withEmergency,
                blocked, blankTherapist, missingStart, missingEnd, blankType);
    }

    private SourceUserProfileScheduleRecord profile(String id, String userId, String hours) {
        return new SourceUserProfileScheduleRecord(
                id, userId,
                "LIC-1", "LCSW", "CA", null, "active",
                List.of("Anxiety"), List.of("CBT"), List.of("Adults"), List.of("English"),
                List.of("EMDR"), List.of("MSW University"), 5,
                List.of("Monday"), hours, 8, 50, "available", null, List.of(),
                "Jane Doe", "555-0100", "spouse",
                List.of(), null, null, List.of(), List.of(), List.of(), null, List.of(), List.of(), null);
    }

    private SourceUserProfileScheduleRecord blankUserProfile(String id) {
        return new SourceUserProfileScheduleRecord(
                id, null,
                null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), null,
                List.of(), null, null, null, null, null, List.of(),
                null, null, null,
                List.of(), null, null, List.of(), List.of(), List.of(), null, List.of(), List.of(), null);
    }

    private SourceTherapistBlockedTimeRecord blocked(String id, String therapistId) {
        return new SourceTherapistBlockedTimeRecord(
                id,
                therapistId,
                Instant.parse("2026-01-15T10:00:00Z"),
                Instant.parse("2026-01-15T11:00:00Z"),
                false,
                "vacation",
                "Time off",
                false,
                null,
                true);
    }
}
