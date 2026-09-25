package com.smart.therapy.flow.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldOptionDto {
    private String key;
    private String label;
    private String template;
}

