package com.smart.therapy.flow.task.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.task.entity.ClientChecklist;
import com.smart.therapy.flow.task.enums.CheckListCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface ClientChecklistRepository extends JpaRepository<ClientChecklist, Long> {
    
    List<ClientChecklist> findByClientIdOrderByCreatedAtDesc(Long clientId);
    
    long countByClientIdAndIsDeletedFalse(Long clientId);
    
    @Query("SELECT cc FROM ClientChecklist cc " +
           "LEFT JOIN FETCH cc.template " +
           "LEFT JOIN FETCH cc.items " +
           "WHERE cc.id = :id")
    Optional<ClientChecklist> findByIdWithDetails(@Param("id") Long id);
    
    @Query("SELECT cc FROM ClientChecklist cc " +
           "LEFT JOIN FETCH cc.template " +
           "LEFT JOIN FETCH cc.items " +
           "WHERE cc.client.id = :clientId ORDER BY cc.createdAt DESC")
    List<ClientChecklist> findByClientIdWithDetails(@Param("clientId") Long clientId);
    
    boolean existsByClientIdAndTemplateId(Long clientId, Long templateId);
    
    @Query("SELECT COUNT(cc) FROM ClientChecklist cc WHERE cc.template.id = :templateId AND cc.isDeleted = false")
    long countByTemplateIdAndNotDeleted(@Param("templateId") Long templateId);
    
    @Query("SELECT cc FROM ClientChecklist cc WHERE cc.template.id = :templateId AND cc.isDeleted = false")
    List<ClientChecklist> findByTemplateIdAndNotDeleted(@Param("templateId") Long templateId);
    
    // Advanced filtering query
    // Note: Using CAST for PostgreSQL date conversion from Instant to LocalDate
    @Query("SELECT DISTINCT cc FROM ClientChecklist cc " +
           "LEFT JOIN FETCH cc.template t " +
           "LEFT JOIN FETCH cc.items " +
           "WHERE cc.isDeleted = false " +
           "AND (:clientId IS NULL OR cc.client.id = :clientId) " +
           "AND (:templateId IS NULL OR cc.template.id = :templateId) " +
           "AND (:category IS NULL OR EXISTS (" +
           "    SELECT 1 FROM ClientChecklistItem cci " +
           "    WHERE cci.clientChecklist = cc " +
           "    AND cci.checklistItem.category = :category" +
           ")) " +
           "AND (:isCompleted IS NULL OR cc.isCompleted = :isCompleted) " +
           "AND (:completedDateFrom IS NULL OR (cc.completedAt IS NOT NULL AND CAST(cc.completedAt AS date) >= :completedDateFrom)) " +
           "AND (:completedDateTo IS NULL OR (cc.completedAt IS NOT NULL AND CAST(cc.completedAt AS date) <= :completedDateTo)) " +
           "AND (:dueDateFrom IS NULL OR cc.dueDate >= :dueDateFrom) " +
           "AND (:dueDateTo IS NULL OR cc.dueDate <= :dueDateTo) " +
           "AND (:createdDateFrom IS NULL OR CAST(cc.createdAt AS date) >= :createdDateFrom) " +
           "AND (:createdDateTo IS NULL OR CAST(cc.createdAt AS date) <= :createdDateTo) " +
           "ORDER BY cc.createdAt DESC")
    List<ClientChecklist> findWithFilters(
            @Param("clientId") Long clientId,
            @Param("templateId") Long templateId,
            @Param("category") CheckListCategory category,
            @Param("isCompleted") Boolean isCompleted,
            @Param("completedDateFrom") LocalDate completedDateFrom,
            @Param("completedDateTo") LocalDate completedDateTo,
            @Param("dueDateFrom") LocalDate dueDateFrom,
            @Param("dueDateTo") LocalDate dueDateTo,
            @Param("createdDateFrom") LocalDate createdDateFrom,
            @Param("createdDateTo") LocalDate createdDateTo
    );
}




