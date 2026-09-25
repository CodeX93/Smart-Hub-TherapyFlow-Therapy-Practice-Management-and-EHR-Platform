package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.entity.PlatformPartnerAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformPartnerAccessRepository extends JpaRepository<PlatformPartnerAccess, Long> {
    List<PlatformPartnerAccess> findByPartnerIdOrderByCreatedAtDesc(String partnerId);

    List<PlatformPartnerAccess> findByPartnerIdAndOrganisation_Id(String partnerId, Long organisationId);
}
