package com.smart.therapy.flow.transcription.ws;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LiveTranscriptMessage {
    private String type;
    private String text;
    private Boolean isFinal;
    private Boolean speechFinal;
    private String message;

    public static LiveTranscriptMessage transcript(String text, boolean isFinal, boolean speechFinal) {
        return LiveTranscriptMessage.builder()
                .type("transcript")
                .text(text)
                .isFinal(isFinal)
                .speechFinal(speechFinal)
                .build();
    }

    public static LiveTranscriptMessage error(String message) {
        return LiveTranscriptMessage.builder()
                .type("error")
                .message(message)
                .build();
    }
}
