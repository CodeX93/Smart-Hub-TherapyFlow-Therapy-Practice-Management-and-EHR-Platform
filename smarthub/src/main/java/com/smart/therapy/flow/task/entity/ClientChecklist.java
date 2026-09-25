package com.smart.therapy.flow.task.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "client_checklists", indexes = {
    @Index(name = "idx_client_checklist_client_completed", columnList = "client_id, is_completed"),
    @Index(name = "idx_client_checklist_template", columnList = "template_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ClientChecklist extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ChecklistTemplate template;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private User completedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(columnDefinition = "TEXT")
    private String description; // Optional description for this checklist instance

    @OneToMany(mappedBy = "clientChecklist", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClientChecklistItem> items = new ArrayList<>();
}
