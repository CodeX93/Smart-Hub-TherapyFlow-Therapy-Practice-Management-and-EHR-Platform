package com.smart.therapy.flow.auth.repository;

import com.smart.therapy.flow.auth.entity.UserActivityLog;
import com.smart.therapy.flow.common.tenant.TenantScoped;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {
    List<UserActivityLog> findByUserId(Long userId);
}

