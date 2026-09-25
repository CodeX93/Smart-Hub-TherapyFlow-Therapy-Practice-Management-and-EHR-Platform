package com.smart.therapy.flow.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class SessionNoteTemplateRequest {

    @NotNull
    private ClientInfo client;

    @NotNull
    private SessionInfo session;

    private Map<String, String> formData;

    private String customInstructions;

    @Data
    public static class ClientInfo {
        @NotBlank
        private String fullName;
        private String clientId;
        private Instant dateOfBirth;
        private String gender;
        private String stage;
    }

    @Data
    public static class SessionInfo {
        private String sessionType;
        private Instant sessionDate;
        private Integer duration;
    }
}

