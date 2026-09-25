package com.smart.therapy.flow.task.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "client_checklist_items", indexes = {
    @Index(name = "idx_client_checklist_item_checklist", columnList = "client_checklist_id"),
    @Index(name = "idx_client_checklist_item_item", columnList = "checklist_item_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ClientChecklistItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_checklist_id", nullable = false)
    @NotNull(message = "Client checklist is required")
    @JsonIgnore
    private ClientChecklist clientChecklist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_item_id", nullable = false)
    @NotNull(message = "Checklist item is required")
    @JsonIgnore
    private ChecklistItem checklistItem;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    @JsonIgnore
    private User completedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Helper methods
    public void markCompleted(User completedByUser) {
        this.isCompleted = true;
        this.completedAt = Instant.now();
        this.completedBy = completedByUser;
    }

    public void markIncomplete() {
        this.isCompleted = false;
        this.completedAt = null;
        this.completedBy = null;
    }

    public boolean isCompleted() {
        return Boolean.TRUE.equals(this.isCompleted);
    }

    public String getItemTitle() {
        return this.checklistItem != null ? this.checklistItem.getTitle() : null;
    }

    public String getItemText() {
        return this.checklistItem != null ? this.checklistItem.getItemText() : null;
    }
}