package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SessionTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSessionInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSessionNoteRef;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSessionRef;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ClientHubSessionMigrationPlanner {

    SessionMigrationPlan buildPlan(
            SourceSessionInventory inventory,
            List<SourceSessionRef> sessions,
            List<SourceSessionNoteRef> notes,
            SessionTargetState targetState) {
        int sessionsWouldCreate = 0;
        int sessionsWouldUpdateMapped = 0;
        int sessionsBlocked = 0;
        Set<String> projectedSessionMappings = new HashSet<>(targetState.mappedSessionIds());

        for (SourceSessionRef session : sessions) {
            if (session.clientLegacyId() == null
                    || session.therapistLegacyId() == null
                    || session.serviceLegacyId() == null
                    || !session.hasSessionDate()
                    || !session.hasSessionType()
                    || !targetState.mappedClientIds().contains(session.clientLegacyId())
                    || !targetState.mappedUserIds().contains(session.therapistLegacyId())
                    || !targetState.mappedServiceIds().contains(session.serviceLegacyId())) {
                sessionsBlocked++;
                continue;
            }
            projectedSessionMappings.add(session.legacySessionPk());
            if (targetState.mappedSessionIds().contains(session.legacySessionPk())) {
                sessionsWouldUpdateMapped++;
            } else {
                sessionsWouldCreate++;
            }
        }

        int notesWouldCreate = 0;
        int notesWouldUpdateMapped = 0;
        int notesBlocked = 0;
        for (SourceSessionNoteRef note : notes) {
            if (note.sessionLegacyId() == null
                    || note.clientLegacyId() == null
                    || note.therapistLegacyId() == null
                    || !note.hasDate()
                    || !projectedSessionMappings.contains(note.sessionLegacyId())
                    || !targetState.mappedClientIds().contains(note.clientLegacyId())
                    || !targetState.mappedUserIds().contains(note.therapistLegacyId())) {
                notesBlocked++;
                continue;
            }
            if (targetState.mappedSessionNoteIds().contains(note.legacyNotePk())) {
                notesWouldUpdateMapped++;
            } else {
                notesWouldCreate++;
            }
        }

        List<String> blockers = new ArrayList<>();
        addBlocker(blockers, inventory.missingClientRows(), "Sessions missing client_id");
        addBlocker(blockers, inventory.missingTherapistRows(), "Sessions missing therapist_id");
        addBlocker(blockers, inventory.missingServiceRows(), "Sessions missing service_id");
        addBlocker(blockers, inventory.missingDateRows(), "Sessions missing session_date");
        addBlocker(blockers, inventory.missingTypeRows(), "Sessions missing session_type");
        addBlocker(blockers, inventory.notesMissingSessionRows(), "Session notes missing session_id");
        addBlocker(blockers, inventory.notesMissingClientRows(), "Session notes missing client_id");
        addBlocker(blockers, inventory.notesMissingTherapistRows(), "Session notes missing therapist_id");
        addBlocker(blockers, inventory.notesMissingDateRows(), "Session notes missing date");
        addBlocker(blockers, sessionsBlocked, "Sessions blocked by missing dependency mappings");
        addBlocker(blockers, notesBlocked, "Session notes blocked by missing dependency mappings");

        List<String> warnings = new ArrayList<>();
        if (targetState.mappedServiceIds().isEmpty() && inventory.sessionRows() > 0) {
            warnings.add("No ClientHubAI service mappings exist; session execution must wait for service catalog mapping");
        }

        return new SessionMigrationPlan(
                sessions.size(),
                sessionsWouldCreate,
                sessionsWouldUpdateMapped,
                sessionsBlocked,
                notes.size(),
                notesWouldCreate,
                notesWouldUpdateMapped,
                notesBlocked,
                blockers,
                warnings);
    }

    private void addBlocker(List<String> blockers, long count, String label) {
        if (count > 0) {
            blockers.add(label + ": " + count);
        }
    }

    record SessionMigrationPlan(
            int sourceSessions,
            int sessionsWouldCreate,
            int sessionsWouldUpdateMapped,
            int sessionsBlocked,
            int sourceSessionNotes,
            int notesWouldCreate,
            int notesWouldUpdateMapped,
            int notesBlocked,
            List<String> blockers,
            List<String> warnings) {

        boolean blocked() {
            return sessionsBlocked > 0 || notesBlocked > 0 || !blockers.isEmpty();
        }
    }
}
