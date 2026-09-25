package com.smart.therapy.flow.task.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.task.entity.ChecklistTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface ChecklistTemplateRepository extends JpaRepository<ChecklistTemplate, Long> {
    
    // ========== Soft Delete Filtering ==========
    
    @Override
    @Query("SELECT ct FROM ChecklistTemplate ct WHERE ct.id = :id AND ct.isDeleted = false")
    @NonNull
    Optional<ChecklistTemplate> findById(@NonNull @Param("id") Long id);
    
    @Override
    @Query("SELECT ct FROM ChecklistTemplate ct WHERE ct.isDeleted = false")
    @NonNull
    List<ChecklistTemplate> findAll();
    
    @Query("SELECT ct FROM ChecklistTemplate ct WHERE ct.id = :id")
    Optional<ChecklistTemplate> findByIdIncludingDeleted(@Param("id") Long id);
    
    // ========== Custom Queries (with soft delete filter) ==========
    
    @Query("SELECT ct FROM ChecklistTemplate ct WHERE ct.isActive = true AND ct.isDeleted = false ORDER BY ct.sortOrder ASC, ct.name ASC")
    List<ChecklistTemplate> findByIsActiveTrueOrderBySortOrderAscNameAsc();
    
    @Query("SELECT ct FROM ChecklistTemplate ct LEFT JOIN FETCH ct.items WHERE ct.id = :id AND ct.isDeleted = false")
    Optional<ChecklistTemplate> findByIdWithItems(@Param("id") Long id);
    
    @Query("SELECT ct FROM ChecklistTemplate ct WHERE ct.clientType = :clientType AND ct.isActive = true AND ct.isDeleted = false")
    List<ChecklistTemplate> findByClientTypeAndIsActiveTrue(@Param("clientType") String clientType);
}




