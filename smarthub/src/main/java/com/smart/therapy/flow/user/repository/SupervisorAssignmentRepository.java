package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.SupervisorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface SupervisorAssignmentRepository extends JpaRepository<SupervisorAssignment, Long> {
    List<SupervisorAssignment> findBySupervisorId(Long supervisorId);
    List<SupervisorAssignment> findByTherapistId(Long therapistId);
}





