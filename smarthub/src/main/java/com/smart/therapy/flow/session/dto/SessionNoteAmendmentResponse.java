package com.smart.therapy.flow.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionNoteAmendmentResponse {

    private Long id;
    private Long sessionNoteId;
    private String amendmentText;
    private String reason;
    private Long createdByUserId;
    private String createdByUserName;
    private Instant signedAt;
    private Instant createdAt;
}
