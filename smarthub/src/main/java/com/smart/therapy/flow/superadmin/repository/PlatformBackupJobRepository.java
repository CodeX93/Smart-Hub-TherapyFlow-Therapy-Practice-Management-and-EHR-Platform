package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.entity.PlatformBackupJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformBackupJobRepository extends JpaRepository<PlatformBackupJob, Long> {
    List<PlatformBackupJob> findTop100ByOrderByCreatedAtDesc();

    List<PlatformBackupJob> findByOrganisation_IdOrderByCreatedAtDesc(Long organisationId);

    List<PlatformBackupJob> findByStatusInOrderByCreatedAtAsc(List<String> statuses);
}
