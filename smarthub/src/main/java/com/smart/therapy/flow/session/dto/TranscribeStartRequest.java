package com.smart.therapy.flow.session.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TranscribeStartRequest {
    /**
     * Optional at start for backward compatibility with older clients.
     * Also required again on {@code /transcribe-finalize}.
     */
    @Positive(message = "expectedChunks must be > 0")
    @Max(value = 5000, message = "expectedChunks must be <= 5000")
    private Integer expectedChunks;

    private String language;

    private Boolean translateToEnglish;

    @Min(value = 1, message = "retentionDays must be at least 1")
    @Max(value = 90, message = "retentionDays must be <= 90")
    private Integer retentionDays;
}
