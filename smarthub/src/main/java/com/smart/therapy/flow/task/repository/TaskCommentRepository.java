package com.smart.therapy.flow.task.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.task.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    interface TaskCommentCountView {
        Long getTaskId();
        Long getCommentCount();
    }

    @Query("SELECT c FROM TaskComment c WHERE c.task.id = :taskId ORDER BY c.createdAt ASC")
    List<TaskComment> findByTaskIdOrderByCreatedAtAsc(@Param("taskId") Long taskId);

    @Query("""
            SELECT c.task.id AS taskId, COUNT(c.id) AS commentCount
            FROM TaskComment c
            WHERE c.task.id IN :taskIds
            GROUP BY c.task.id
            """)
    List<TaskCommentCountView> countByTaskIds(@Param("taskIds") List<Long> taskIds);

    Long countByTaskId(Long taskId);
}




