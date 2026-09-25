package com.smart.therapy.flow.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteResponse {
    private Long id;
    private Long clientId;
    private Long authorId;
    private String authorName;
    private String title;
    private String content;
    private String noteType;
    private Instant eventDate;
    private Boolean isPrivate;
    private Instant createdAt;
    private Instant updatedAt;
}

