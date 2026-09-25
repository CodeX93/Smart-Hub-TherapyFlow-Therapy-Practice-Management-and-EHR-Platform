package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.RoomIntegrationTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceRoomIntegrationInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceRoomRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSessionIntegrationRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClientHubRoomIntegrationMigrationPlanner {

    RoomIntegrationMigrationPlan buildPlan(
            SourceRoomIntegrationInventory inventory,
            List<SourceRoomRecord> sourceRooms,
            List<SourceSessionIntegrationRecord> sourceIntegrations,
            RoomIntegrationTargetState targetState) {
        int roomsWouldCreate = 0;
        int roomsWouldUpdateExistingNumber = 0;
        int roomsWouldUpdateMapped = 0;
        int roomsBlocked = 0;

        for (SourceRoomRecord source : sourceRooms) {
            if (source.missingRequiredFields()) {
                roomsBlocked++;
                continue;
            }
            if (targetState.mappedRoomIds().contains(source.legacyRoomPk())) {
                roomsWouldUpdateMapped++;
            } else if (targetState.existingRoomNumbers().contains(source.roomNumber())) {
                roomsWouldUpdateExistingNumber++;
            } else {
                roomsWouldCreate++;
            }
        }

        int integrationsWouldCreate = 0;
        int integrationsWouldUpdateMapped = 0;
        int integrationsBlocked = 0;
        int integrationsWithUnmappedSession = 0;
        for (SourceSessionIntegrationRecord source : sourceIntegrations) {
            if (!targetState.mappedSessionIds().contains(source.legacySessionPk())) {
                integrationsBlocked++;
                integrationsWithUnmappedSession++;
                continue;
            }
            if (targetState.mappedSessionIntegrationIds().contains(source.legacyIntegrationKey())) {
                integrationsWouldUpdateMapped++;
            } else {
                integrationsWouldCreate++;
            }
        }

        List<String> blockers = new ArrayList<>();
        addBlocker(blockers, inventory.blankRoomNumberRows(), "Rooms missing room_number");
        addBlocker(blockers, inventory.blankRoomNameRows(), "Rooms missing room_name");
        addBlocker(blockers, inventory.duplicateRoomNumberGroups(), "Duplicate source room_number groups");
        addBlocker(blockers, integrationsWithUnmappedSession, "Zoom integrations reference unmapped sessions");

        return new RoomIntegrationMigrationPlan(
                sourceRooms.size(),
                roomsWouldCreate,
                roomsWouldUpdateExistingNumber,
                roomsWouldUpdateMapped,
                roomsBlocked,
                inventory.sessionsWithRoomRows(),
                sourceIntegrations.size(),
                integrationsWouldCreate,
                integrationsWouldUpdateMapped,
                integrationsBlocked,
                inventory.zoomEnabledMissingMeetingRows(),
                blockers);
    }

    private void addBlocker(List<String> blockers, long count, String label) {
        if (count > 0) {
            blockers.add(label + ": " + count);
        }
    }

    record RoomIntegrationMigrationPlan(
            int sourceRooms,
            int roomsWouldCreate,
            int roomsWouldUpdateExistingNumber,
            int roomsWouldUpdateMapped,
            int roomsBlocked,
            long sessionsWithRoomRows,
            int sourceZoomIntegrations,
            int integrationsWouldCreate,
            int integrationsWouldUpdateMapped,
            int integrationsBlocked,
            long zoomEnabledMissingMeetingRows,
            List<String> blockers) {

        boolean blocked() {
            return roomsBlocked > 0 || integrationsBlocked > 0 || !blockers.isEmpty();
        }
    }
}
