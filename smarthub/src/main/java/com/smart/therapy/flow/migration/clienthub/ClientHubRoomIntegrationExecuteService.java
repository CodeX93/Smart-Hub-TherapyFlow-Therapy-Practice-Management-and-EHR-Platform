package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceRoomRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSessionIntegrationRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import com.smart.therapy.flow.session.entity.Room;
import com.smart.therapy.flow.session.entity.SessionIntegration;
import com.smart.therapy.flow.session.enums.RoomType;
import com.smart.therapy.flow.session.repository.RoomRepository;
import com.smart.therapy.flow.session.repository.SessionIntegrationRepository;
import com.smart.therapy.flow.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ClientHubRoomIntegrationExecuteService {

    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final JdbcTemplate jdbcTemplate;
    private final RoomRepository roomRepository;
    private final SessionRepository sessionRepository;
    private final SessionIntegrationRepository sessionIntegrationRepository;

    public RoomIntegrationExecuteResult execute(
            List<SourceRoomRecord> sourceRooms,
            List<SourceSessionIntegrationRecord> sourceIntegrations,
            TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required for room/integration execution");
        }
        return tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(),
                () -> executeInTenant(sourceRooms, sourceIntegrations, target));
    }

    private RoomIntegrationExecuteResult executeInTenant(
            List<SourceRoomRecord> sourceRooms,
            List<SourceSessionIntegrationRecord> sourceIntegrations,
            TargetInventory target) {
        Map<String, Long> roomMappings = loadMappings(target.organisationId(), "rooms");
        Map<String, Long> sessionMappings = loadMappings(target.organisationId(), "sessions");
        Map<String, Long> integrationMappings = loadMappings(target.organisationId(), "session_integrations");

        int roomsCreated = 0;
        int roomsUpdated = 0;
        int roomMappedReruns = 0;
        for (SourceRoomRecord source : sourceRooms) {
            Optional<Long> mappedRoomId = Optional.ofNullable(roomMappings.get(source.legacyRoomPk()));
            Room room = mappedRoomId.flatMap(roomRepository::findById)
                    .or(() -> roomRepository.findByRoomNumber(source.roomNumber().trim()))
                    .orElseGet(Room::new);
            boolean existing = room.getId() != null;

            applyRoomFields(room, source);
            Room saved = roomRepository.save(room);
            roomMappings.put(source.legacyRoomPk(), saved.getId());
            upsertLegacyMapping(target, "rooms", "rooms", source.legacyRoomPk(), saved.getId(), roomChecksum(source));

            if (existing) {
                roomsUpdated++;
                if (mappedRoomId.isPresent()) {
                    roomMappedReruns++;
                }
            } else {
                roomsCreated++;
            }
        }

        int integrationsCreated = 0;
        int integrationsUpdated = 0;
        int integrationMappedReruns = 0;
        for (SourceSessionIntegrationRecord source : sourceIntegrations) {
            Long targetSessionId = requiredMapping(sessionMappings, source.legacySessionPk(), "sessions");
            Optional<Long> mappedIntegrationId = Optional.ofNullable(integrationMappings.get(source.legacyIntegrationKey()));
            SessionIntegration integration = mappedIntegrationId.flatMap(sessionIntegrationRepository::findById)
                    .or(() -> sessionIntegrationRepository.findBySessionIdAndProvider(targetSessionId, "zoom"))
                    .orElseGet(SessionIntegration::new);
            boolean existing = integration.getId() != null;

            applyIntegrationFields(integration, source, targetSessionId);
            SessionIntegration saved = sessionIntegrationRepository.save(integration);
            upsertLegacyMapping(target, "session_integrations", "session_integrations",
                    source.legacyIntegrationKey(), saved.getId(), integrationChecksum(source));

            if (existing) {
                integrationsUpdated++;
                if (mappedIntegrationId.isPresent()) {
                    integrationMappedReruns++;
                }
            } else {
                integrationsCreated++;
            }
        }

        return new RoomIntegrationExecuteResult(
                sourceRooms.size(),
                roomsCreated,
                roomsUpdated,
                roomMappedReruns,
                sourceIntegrations.size(),
                integrationsCreated,
                integrationsUpdated,
                integrationMappedReruns);
    }

    private void applyRoomFields(Room room, SourceRoomRecord source) {
        room.setRoomNumber(source.roomNumber().trim());
        room.setRoomName(trimTo(source.roomName(), 255));
        room.setCapacity(source.capacity());
        room.setEquipment(trim(source.equipment()));
        room.setIsActive(source.active());
        room.setRoomType(RoomType.PHYSICAL);
        room.setIsDeleted(false);
        room.setDeletedAt(null);
        room.setCreatedBy(0L);
        room.setUpdatedBy(0L);
    }

    private void applyIntegrationFields(
            SessionIntegration integration,
            SourceSessionIntegrationRecord source,
            Long targetSessionId) {
        integration.setSession(sessionRepository.getReferenceById(targetSessionId));
        integration.setProvider("zoom");
        integration.setMeetingId(trimTo(source.meetingId(), 100));
        integration.setJoinUrl(trim(source.joinUrl()));
        integration.setPassword(trimTo(source.password(), 100));
        integration.setMetadata(null);
    }

    private Map<String, Long> loadMappings(Long organisationId, String entityName) {
        Map<String, Long> mappings = new HashMap<>();
        jdbcTemplate.query("""
                SELECT source_id, target_id
                FROM public.clienthub_legacy_id_mappings
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                  AND entity_name = ?
                """,
                (RowCallbackHandler) rs -> mappings.put(rs.getString("source_id"), rs.getLong("target_id")),
                organisationId,
                entityName);
        return mappings;
    }

    private void upsertLegacyMapping(
            TargetInventory target,
            String entityName,
            String targetTable,
            String sourceId,
            Long targetId,
            String checksum) {
        jdbcTemplate.update("""
                INSERT INTO public.clienthub_legacy_id_mappings (
                    organisation_id, source_system, entity_name, source_id,
                    target_schema, target_table, target_id, source_checksum_sha256,
                    first_seen_run_id, last_seen_run_id
                )
                VALUES (?, 'ClientHubAI', ?, ?, ?, ?, ?, ?, NULL, NULL)
                ON CONFLICT (organisation_id, source_system, entity_name, source_id)
                DO UPDATE SET
                    target_schema = EXCLUDED.target_schema,
                    target_table = EXCLUDED.target_table,
                    target_id = EXCLUDED.target_id,
                    source_checksum_sha256 = EXCLUDED.source_checksum_sha256,
                    updated_at = CURRENT_TIMESTAMP
                """,
                target.organisationId(),
                entityName,
                sourceId,
                target.schemaName(),
                targetTable,
                targetId,
                checksum);
    }

    private Long requiredMapping(Map<String, Long> mappings, String sourceId, String entityName) {
        Long targetId = mappings.get(sourceId);
        if (targetId == null) {
            throw new IllegalStateException("Missing ClientHubAI " + entityName + " mapping for source id " + sourceId);
        }
        return targetId;
    }

    private String roomChecksum(SourceRoomRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyRoomPk(),
                source.roomNumber(),
                source.roomName(),
                String.valueOf(source.capacity()),
                Boolean.toString(source.active())));
    }

    private String integrationChecksum(SourceSessionIntegrationRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyIntegrationKey(),
                String.valueOf(source.meetingId()),
                String.valueOf(source.joinUrl())));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimTo(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    public record RoomIntegrationExecuteResult(
            int sourceRooms,
            int roomsCreated,
            int roomsUpdated,
            int roomMappedReruns,
            int sourceIntegrations,
            int integrationsCreated,
            int integrationsUpdated,
            int integrationMappedReruns) {
    }
}
