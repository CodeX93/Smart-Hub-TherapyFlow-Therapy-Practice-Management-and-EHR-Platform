package com.smart.therapy.flow.session.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class TranscribeFinalizeRequest {

    @NotBlank(message = "uploadId is required")
    private String uploadId;

    @NotNull(message = "expectedChunks is required")
    @Positive(message = "expectedChunks must be > 0")
    @Max(value = 5000, message = "expectedChunks must be <= 5000")
    private Integer expectedChunks;

    private Integer totalChunks;

    @Valid
    private List<SilentChunkDto> silentChunks;
}
