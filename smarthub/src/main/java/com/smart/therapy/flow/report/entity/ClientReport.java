package com.smart.therapy.flow.report.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.common.exception.BusinessLogicException;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "client_reports", indexes = {
        @Index(name = "idx_client_reports_client", columnList = "client_id"),
        @Index(name = "idx_client_reports_status", columnList = "is_draft,is_finalized")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ClientReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnore
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    @JsonIgnore
    private ReportTemplate template;

    @Column(name = "template_name", length = 255)
    private String templateName;

    @Column(name = "generated_content", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String generatedContent;

    @Column(name = "draft_content", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String draftContent;

    @Column(name = "final_content", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String finalContent;

    @Column(name = "is_draft", nullable = false)
    @Builder.Default
    private Boolean isDraft = true;

    @Column(name = "is_finalized", nullable = false)
    @Builder.Default
    private Boolean isFinalized = false;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    @JsonIgnore
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finalized_by_id")
    @JsonIgnore
    private User finalizedByUser;

    public boolean canEdit() {
        return !Boolean.TRUE.equals(isFinalized);
    }

    public void updateDraft(String content) {
        if (!canEdit()) {
            throw new BusinessLogicException("Cannot edit finalized report");
        }
        draftContent = content;
        editedAt = Instant.now();
        isDraft = true;
    }

    public void finalizeReport(User finalizedBy) {
        if (Boolean.TRUE.equals(isFinalized)) {
            throw new BusinessLogicException("Report is already finalized");
        }
        String content = draftContent != null ? draftContent : generatedContent;
        finalContent = content;
        isFinalized = true;
        isDraft = false;
        finalizedAt = Instant.now();
        finalizedByUser = finalizedBy;
    }

    public String resolveEditorContent() {
        if (finalContent != null) {
            return finalContent;
        }
        if (draftContent != null) {
            return draftContent;
        }
        return generatedContent;
    }
}
