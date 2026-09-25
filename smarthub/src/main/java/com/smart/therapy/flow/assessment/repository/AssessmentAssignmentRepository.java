package com.smart.therapy.flow.assessment.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.assessment.entity.AssessmentAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface AssessmentAssignmentRepository extends JpaRepository<AssessmentAssignment, Long> {
    List<AssessmentAssignment> findByClientId(Long clientId);
    List<AssessmentAssignment> findByAssignedById(Long assignedById);
    List<AssessmentAssignment> findByTemplateId(Long templateId);
    List<AssessmentAssignment> findByStatus(String status);

    @Query("SELECT a FROM AssessmentAssignment a WHERE a.client.id = :clientId ORDER BY a.createdAt DESC")
    List<AssessmentAssignment> findByClientIdOrderByAssignedDateDesc(@Param("clientId") Long clientId);
    
    @Query("SELECT a FROM AssessmentAssignment a LEFT JOIN FETCH a.client LEFT JOIN FETCH a.template LEFT JOIN FETCH a.assignedBy WHERE a.client.id = :clientId")
    List<AssessmentAssignment> findByClientIdWithRelations(@Param("clientId") Long clientId);
    
    @Query("SELECT a FROM AssessmentAssignment a LEFT JOIN FETCH a.client LEFT JOIN FETCH a.template LEFT JOIN FETCH a.assignedBy WHERE a.id = :id")
    java.util.Optional<AssessmentAssignment> findByIdWithRelations(@Param("id") Long id);

    @Query("SELECT COUNT(a) FROM AssessmentAssignment a WHERE a.template.id = :templateId")
    long countByTemplateId(@Param("templateId") Long templateId);

    @Query("SELECT COUNT(a) FROM AssessmentAssignment a WHERE a.template.id = :templateId AND a.status NOT IN :statuses")
    long countByTemplateIdAndStatusNotIn(@Param("templateId") Long templateId, @Param("statuses") List<String> statuses);

    boolean existsByClientIdAndTemplateIdAndIsDeletedFalse(Long clientId, Long templateId);
}





