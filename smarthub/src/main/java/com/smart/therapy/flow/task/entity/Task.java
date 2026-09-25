package com.smart.therapy.flow.task.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks", indexes = {
    @Index(name = "idx_task_client", columnList = "client_id"),
    @Index(name = "idx_task_assigned_to", columnList = "assigned_to_id"),
    @Index(name = "idx_task_status", columnList = "status"),
    @Index(name = "idx_task_priority", columnList = "priority")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Task extends BaseEntity {

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "title_key", length = 100)
    private String titleKey;

    @Column(name = "task_type", length = 100)
    private String taskType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String status = "pending";

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String priority = "medium";

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TaskComment> comments = new ArrayList<>();
}

