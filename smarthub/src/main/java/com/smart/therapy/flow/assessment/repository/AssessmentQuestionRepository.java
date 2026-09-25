package com.smart.therapy.flow.assessment.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.assessment.entity.AssessmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, Long> {
    List<AssessmentQuestion> findBySectionId(Long sectionId);
}





