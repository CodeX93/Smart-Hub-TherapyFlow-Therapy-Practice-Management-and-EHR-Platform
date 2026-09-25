package com.smart.therapy.flow.session.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "session_note_amendments", indexes = {
        @Index(name = "idx_session_note_amendment_note", columnList = "session_note_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SessionNoteAmendment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_note_id", nullable = false)
    private SessionNote sessionNote;

    @Column(name = "amendment_text", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String amendmentText;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;
}
