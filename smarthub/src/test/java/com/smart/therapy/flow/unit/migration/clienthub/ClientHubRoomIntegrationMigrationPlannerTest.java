package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubRoomIntegrationMigrationPlanner.RoomIntegrationMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.RoomIntegrationTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceRoomIntegrationInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceRoomRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSessionIntegrationRecord;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHubRoomIntegrationMigrationPlannerTest {

    private final ClientHubRoomIntegrationMigrationPlanner planner = new ClientHubRoomIntegrationMigrationPlanner();

    @Test
    void countsRoomsAndZoomIntegrations() {
        RoomIntegrationMigrationPlan plan = planner.buildPlan(
                inventory(3, 0, 0, 0, 2, 2, 0),
                List.of(
                        room("1", "A"),
                        room("2", "B"),
                        room("3", "C")),
                List.of(
                        zoom("10"),
                        zoom("11")),
                new RoomIntegrationTargetState(Set.of("3"), Set.of("10", "11"), Set.of("11:zoom"), Set.of("B")));

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.roomsWouldCreate()).isEqualTo(1);
        assertThat(plan.roomsWouldUpdateExistingNumber()).isEqualTo(1);
        assertThat(plan.roomsWouldUpdateMapped()).isEqualTo(1);
        assertThat(plan.integrationsWouldCreate()).isEqualTo(1);
        assertThat(plan.integrationsWouldUpdateMapped()).isEqualTo(1);
    }

    @Test
    void blocksInvalidRoomsAndUnmappedZoomSessions() {
        RoomIntegrationMigrationPlan plan = planner.buildPlan(
                inventory(2, 1, 1, 1, 0, 1, 1),
                List.of(
                        new SourceRoomRecord("1", "", "", null, null, true),
                        room("2", "B")),
                List.of(zoom("missing-session")),
                new RoomIntegrationTargetState(Set.of(), Set.of(), Set.of(), Set.of()));

        assertThat(plan.blocked()).isTrue();
        assertThat(plan.roomsBlocked()).isEqualTo(1);
        assertThat(plan.integrationsBlocked()).isEqualTo(1);
        assertThat(plan.blockers())
                .contains("Rooms missing room_number: 1")
                .contains("Rooms missing room_name: 1")
                .contains("Duplicate source room_number groups: 1")
                .contains("Zoom integrations reference unmapped sessions: 1");
    }

    private SourceRoomIntegrationInventory inventory(
            long rooms,
            long blankNumber,
            long blankName,
            long duplicateNumbers,
            long sessionsWithRoom,
            long zoomEnabled,
            long zoomMissingMeeting) {
        return new SourceRoomIntegrationInventory(
                rooms,
                blankNumber,
                blankName,
                duplicateNumbers,
                sessionsWithRoom,
                zoomEnabled,
                zoomMissingMeeting);
    }

    private SourceRoomRecord room(String legacyPk, String number) {
        return new SourceRoomRecord(legacyPk, number, "Room " + number, 2, null, true);
    }

    private SourceSessionIntegrationRecord zoom(String legacySessionPk) {
        return new SourceSessionIntegrationRecord(legacySessionPk, "123", "https://zoom.example/j/123", "secret");
    }
}
