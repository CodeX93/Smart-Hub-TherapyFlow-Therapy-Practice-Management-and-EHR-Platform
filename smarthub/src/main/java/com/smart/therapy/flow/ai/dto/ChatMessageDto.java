package com.smart.therapy.flow.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

    @NotBlank
    private String role;

    @NotBlank
    private String content;
}

