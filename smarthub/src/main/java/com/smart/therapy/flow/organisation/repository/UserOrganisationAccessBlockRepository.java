package com.smart.therapy.flow.organisation.repository;

import com.smart.therapy.flow.organisation.entity.UserOrganisationAccessBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserOrganisationAccessBlockRepository extends JpaRepository<UserOrganisationAccessBlock, Long> {

    boolean existsByAuth_IdAndOrganisation_Id(Long authId, Long organisationId);

    Optional<UserOrganisationAccessBlock> findByAuth_IdAndOrganisation_Id(Long authId, Long organisationId);

    @Query("SELECT b.organisation.id FROM UserOrganisationAccessBlock b WHERE b.auth.id = :authId")
    List<Long> findBlockedOrganisationIds(@Param("authId") Long authId);
}
