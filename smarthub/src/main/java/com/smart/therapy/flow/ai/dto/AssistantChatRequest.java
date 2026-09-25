package com.smart.therapy.flow.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AssistantChatRequest {

    @NotBlank
    private String userMessage;

    private List<ChatMessageDto> conversationHistory = new ArrayList<>();

    private String userRole = "therapist";
}

