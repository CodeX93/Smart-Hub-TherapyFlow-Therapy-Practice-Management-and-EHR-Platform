package com.smart.therapy.flow.auth.repository;

import com.smart.therapy.flow.auth.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    Optional<RolePermission> findByRole_IdAndPermission_Id(Long roleId, Long permissionId);

    @Transactional
    void deleteByRole_Id(Long roleId);
}

