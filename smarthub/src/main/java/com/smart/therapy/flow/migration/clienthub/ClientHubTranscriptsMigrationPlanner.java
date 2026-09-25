package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTranscriptRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTranscriptsInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TranscriptsTargetState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClientHubTranscriptsMigrationPlanner {

    TranscriptsMigrationPlan buildPlan(
            SourceTranscriptsInventory inventory,
            List<SourceTranscriptRecord> transcripts,
            TranscriptsTargetState targetState) {
        int wouldCreate = 0;
        int wouldUpdateMapped = 0;
        int blocked = 0;
        int unmappedSession = 0;
        int unmappedClient = 0;
        int unmappedTherapist = 0;

        for (SourceTranscriptRecord source : transcripts) {
            if (source.missingRequiredFields()) {
                blocked++;
                continue;
            }
            boolean sessionMapped = targetState.mappedSessionIds().contains(source.sessionLegacyId());
            boolean clientMapped = targetState.mappedClientIds().contains(source.clientLegacyId());
            boolean therapistMapped = targetState.mappedUserIds().contains(source.therapistLegacyId());
            if (!sessionMapped || !clientMapped || !therapistMapped) {
                blocked++;
                if (!sessionMapped) {
                    unmappedSession++;
                }
                if (!clientMapped) {
                    unmappedClient++;
                }
                if (!therapistMapped) {
                    unmappedTherapist++;
                }
                continue;
            }
            if (targetState.mappedTranscriptIds().contains(source.legacyTranscriptPk())) {
                wouldUpdateMapped++;
            } else {
                wouldCreate++;
            }
        }

        List<String> blockers = new ArrayList<>();
        addBlocker(blockers, inventory.blankSessionRows(), "Transcripts missing session_id");
        addBlocker(blockers, inventory.blankClientRows(), "Transcripts missing client_id");
        addBlocker(blockers, inventory.blankTherapistRows(), "Transcripts missing therapist_id");
        addBlocker(blockers, unmappedSession, "Transcripts reference unmapped sessions");
        addBlocker(blockers, unmappedClient, "Transcripts reference unmapped clients");
        addBlocker(blockers, unmappedTherapist, "Transcripts reference unmapped therapists");

        return new TranscriptsMigrationPlan(
                transcripts.size(),
                wouldCreate,
                wouldUpdateMapped,
                blocked,
                blockers);
    }

    private void addBlocker(List<String> blockers, long count, String label) {
        if (count > 0) {
            blockers.add(label + ": " + count);
        }
    }

    record TranscriptsMigrationPlan(
            int sourceTranscripts,
            int wouldCreate,
            int wouldUpdateMapped,
            int blockedRows,
            List<String> blockers) {

        boolean blocked() {
            return blockedRows > 0 || !blockers.isEmpty();
        }
    }
}
