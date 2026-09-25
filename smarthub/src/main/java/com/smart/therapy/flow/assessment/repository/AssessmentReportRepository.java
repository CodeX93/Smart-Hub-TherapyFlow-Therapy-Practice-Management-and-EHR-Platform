package com.smart.therapy.flow.assessment.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.assessment.entity.AssessmentReport;
import com.smart.therapy.flow.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface AssessmentReportRepository extends JpaRepository<AssessmentReport, Long> {
    Optional<AssessmentReport> findByAssignmentId(Long assignmentId);
    List<AssessmentReport> findByAssignment_ClientId(Long clientId);
    List<AssessmentReport> findByAssignment_Client(Client client);
}





