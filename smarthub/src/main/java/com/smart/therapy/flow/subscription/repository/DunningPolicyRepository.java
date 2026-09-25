package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.DunningPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DunningPolicyRepository extends JpaRepository<DunningPolicy, Long> {

    Optional<DunningPolicy> findByOrganisation_Id(Long organisationId);

    @Query("SELECT p FROM DunningPolicy p WHERE p.organisation IS NULL")
    Optional<DunningPolicy> findGlobalDefault();
}
