package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.TherapistBlockedTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface TherapistBlockedTimeRepository extends JpaRepository<TherapistBlockedTime, Long> {
    List<TherapistBlockedTime> findByTherapistId(Long therapistId);
    List<TherapistBlockedTime> findByTherapistIdAndIsActiveTrueOrderByStartTimeAsc(Long therapistId);
}





