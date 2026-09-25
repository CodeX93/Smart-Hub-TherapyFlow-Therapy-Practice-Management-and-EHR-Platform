package com.smart.therapy.flow.task.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@TenantScoped
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    List<Task> findByAssignedToId(Long assignedToId);
    List<Task> findByStatus(String status);
    List<Task> findByPriority(com.smart.therapy.flow.client.enums.Priority priority);
    List<Task> findByClientId(Long clientId);

    @Query("SELECT t FROM Task t WHERE t.dueDate < :now AND t.status <> 'completed'")
    List<Task> findOverdueTasks(@Param("now") Instant now);
    
    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.assignedTo LEFT JOIN FETCH t.client WHERE t.assignedTo.id = :assignedToId")
    List<Task> findByAssignedToIdWithRelations(@Param("assignedToId") Long assignedToId);
    
    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.assignedTo LEFT JOIN FETCH t.client WHERE t.client.id = :clientId")
    List<Task> findByClientIdWithRelations(@Param("clientId") Long clientId);
    
    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.assignedTo LEFT JOIN FETCH t.client WHERE t.id = :id")
    java.util.Optional<Task> findByIdWithRelations(@Param("id") Long id);
}





