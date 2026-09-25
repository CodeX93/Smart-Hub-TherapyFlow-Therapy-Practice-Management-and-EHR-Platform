package com.smart.therapy.flow.assessment.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.assessment.entity.AssessmentQuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface AssessmentQuestionOptionRepository extends JpaRepository<AssessmentQuestionOption, Long> {

    List<AssessmentQuestionOption> findByQuestionId(Long questionId);

    void deleteByQuestionId(Long questionId);
}





