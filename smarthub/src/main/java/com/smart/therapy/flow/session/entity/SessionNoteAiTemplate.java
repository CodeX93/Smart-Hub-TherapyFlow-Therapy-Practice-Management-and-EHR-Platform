package com.smart.therapy.flow.session.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "session_note_ai_templates", indexes = {
        @Index(name = "idx_session_note_ai_templates_owner", columnList = "created_by")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SessionNoteAiTemplate extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;
}
