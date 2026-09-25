package com.smart.therapy.flow.task.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "task_comments", indexes = {
    @Index(name = "idx_task_comment_task", columnList = "task_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class TaskComment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String commentText;

    @Column(name = "is_internal", nullable = false)
    @Builder.Default
    private Boolean isInternal = false; // Internal staff notes vs client-visible

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user")
    private User createdByUser;
}
