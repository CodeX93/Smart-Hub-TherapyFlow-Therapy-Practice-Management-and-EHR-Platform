package com.smart.therapy.flow.document.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.document.entity.FormResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface FormResponseRepository extends JpaRepository<FormResponse, Long> {
    
    List<FormResponse> findByAssignmentId(Long assignmentId);
    
    List<FormResponse> findByAssignmentFieldId(Long assignmentFieldId);
    
    @Query("SELECT fr FROM FormResponse fr " +
           "WHERE fr.assignment.id = :assignmentId " +
           "AND fr.assignmentField.id = :assignmentFieldId")
    Optional<FormResponse> findByAssignmentIdAndAssignmentFieldId(
        @Param("assignmentId") Long assignmentId, 
        @Param("assignmentFieldId") Long assignmentFieldId
    );
    
    void deleteByAssignmentId(Long assignmentId);
}





