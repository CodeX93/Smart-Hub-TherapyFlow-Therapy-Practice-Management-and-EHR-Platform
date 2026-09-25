package com.smart.therapy.flow.document.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.document.entity.FormAssignment;
import com.smart.therapy.flow.document.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface FormAssignmentRepository
        extends JpaRepository<FormAssignment, Long>, JpaSpecificationExecutor<FormAssignment> {
    
    @Query("SELECT fa FROM FormAssignment fa WHERE fa.client.id = :clientId ORDER BY fa.createdAt DESC")
    List<FormAssignment> findByClientIdOrderByCreatedAtDesc(@Param("clientId") Long clientId);
    
    @Query("SELECT fa FROM FormAssignment fa " +
           "LEFT JOIN FETCH fa.templateVersion tv " +
           "LEFT JOIN FETCH tv.template " +
           "WHERE fa.id = :id")
    Optional<FormAssignment> findByIdWithTemplateDetails(@Param("id") Long id);
    
    List<FormAssignment> findByStatus(Status status);
    
    @Query("SELECT fa FROM FormAssignment fa WHERE fa.client.id = :clientId AND fa.status = :status")
    List<FormAssignment> findByClientIdAndStatus(@Param("clientId") Long clientId, @Param("status") Status status);
}





