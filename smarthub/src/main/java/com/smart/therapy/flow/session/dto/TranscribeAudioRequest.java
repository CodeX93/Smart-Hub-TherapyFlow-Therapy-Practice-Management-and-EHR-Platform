package com.smart.therapy.flow.session.dto;

import lombok.Data;

@Data
public class TranscribeAudioRequest {
    private Long sessionNoteId; // Optional - may not exist for new unsaved notes
}

