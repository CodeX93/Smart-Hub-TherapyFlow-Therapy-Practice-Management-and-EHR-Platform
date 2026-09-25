package com.smart.therapy.flow.document.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.document.entity.FormSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface FormSignatureRepository extends JpaRepository<FormSignature, Long> {
    
    List<FormSignature> findByAssignmentId(Long assignmentId);
    
    Optional<FormSignature> findFirstByAssignmentId(Long assignmentId);
    
    Optional<FormSignature> findByAssignmentIdAndSignerRole(Long assignmentId, String signerRole);
    
    void deleteByAssignmentId(Long assignmentId);
}





