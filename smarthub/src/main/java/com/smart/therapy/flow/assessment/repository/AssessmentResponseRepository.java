package com.smart.therapy.flow.assessment.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.assessment.entity.AssessmentResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface AssessmentResponseRepository extends JpaRepository<AssessmentResponse, Long> {
    List<AssessmentResponse> findByAssignmentId(Long assignmentId);
    List<AssessmentResponse> findByQuestionId(Long questionId);

    @Query("SELECT r FROM AssessmentResponse r WHERE r.assignment.id = :assignmentId ORDER BY r.question.sortOrder")
    List<AssessmentResponse> findByAssignmentIdOrderByQuestionSortOrder(@Param("assignmentId") Long assignmentId);

    @Query("SELECT r FROM AssessmentResponse r WHERE r.assignment.id = :assignmentId AND r.question.id = :questionId")
    List<AssessmentResponse> findByAssignmentIdAndQuestionId(@Param("assignmentId") Long assignmentId, @Param("questionId") Long questionId);

    // Note: Use findExistingResponse in service instead - this method is deprecated
    // Keeping for backward compatibility but should use service method with responder_type
    @Query("SELECT r FROM AssessmentResponse r WHERE r.assignment.id = :assignmentId AND r.question.id = :questionId " +
           "AND ((r.responderType = 'USER' AND r.responderUserId = :responderId) OR " +
           "(r.responderType = 'CLIENT' AND r.responderClientId = :responderId))")
    List<AssessmentResponse> findByAssignmentIdAndQuestionIdAndResponderId(
            @Param("assignmentId") Long assignmentId, 
            @Param("questionId") Long questionId,
            @Param("responderId") Long responderId);

    @Query("SELECT COUNT(r) FROM AssessmentResponse r WHERE r.question.id = :questionId")
    long countByQuestionId(@Param("questionId") Long questionId);

    @Query("SELECT COUNT(r) FROM AssessmentResponse r WHERE r.question.section.id = :sectionId")
    long countByQuestionSectionId(@Param("sectionId") Long sectionId);
}





