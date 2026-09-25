package com.smart.therapy.flow.assessment.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.assessment.entity.AssessmentSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface AssessmentSectionRepository extends JpaRepository<AssessmentSection, Long> {
    List<AssessmentSection> findByTemplateId(Long templateId);
}





