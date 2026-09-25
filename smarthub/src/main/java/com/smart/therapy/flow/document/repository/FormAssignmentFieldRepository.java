package com.smart.therapy.flow.document.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.document.entity.FormAssignmentField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface FormAssignmentFieldRepository extends JpaRepository<FormAssignmentField, Long> {
    
    List<FormAssignmentField> findByAssignmentIdAndIsDeletedFalseOrderBySortOrderAsc(Long assignmentId);
    
    Optional<FormAssignmentField> findByAssignmentIdAndFieldIdAndIsDeletedFalse(
        Long assignmentId, 
        Long fieldId
    );
    
    @Query("SELECT faf FROM FormAssignmentField faf " +
           "LEFT JOIN FETCH faf.responses " +
           "WHERE faf.assignment.id = :assignmentId " +
           "AND faf.isDeleted = false " +
           "ORDER BY faf.sortOrder ASC")
    List<FormAssignmentField> findByAssignmentIdWithResponses(@Param("assignmentId") Long assignmentId);
    
    void deleteByAssignmentId(Long assignmentId);
    
    @Query("SELECT COUNT(faf) FROM FormAssignmentField faf WHERE faf.field.id = :fieldId AND faf.isDeleted = false")
    long countByFieldIdAndNotDeleted(@Param("fieldId") Long fieldId);
}




