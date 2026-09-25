package com.smart.therapy.flow.document.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.document.enums.NoteType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notes", indexes = {
    @Index(name = "idx_note_client", columnList = "client_id"),
    @Index(name = "idx_note_type", columnList = "note_type"),
    @Index(name = "idx_note_pinned", columnList = "is_pinned")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Note extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user", nullable = false)
    private User createdByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, length = 50)
    private NoteType noteType; //call, email, note, general, clinical, supervisor

    @Column(length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_private")
    private Boolean isPrivate;

    @Column(name = "is_pinned")
    private Boolean isPinned;

    @Column(name = "event_date", nullable = false)
    private Instant eventDate;
}


