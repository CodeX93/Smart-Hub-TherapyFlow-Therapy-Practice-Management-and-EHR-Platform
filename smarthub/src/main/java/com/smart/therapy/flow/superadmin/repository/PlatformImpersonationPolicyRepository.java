package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.entity.PlatformImpersonationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformImpersonationPolicyRepository extends JpaRepository<PlatformImpersonationPolicy, Long> {
}
