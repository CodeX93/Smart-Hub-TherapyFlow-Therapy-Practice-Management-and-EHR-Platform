package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.BillingContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingContactRepository extends JpaRepository<BillingContact, Long> {

    List<BillingContact> findByOrganisation_IdAndIsActiveTrueOrderByIsPrimaryDescCreatedAtAsc(Long organisationId);
}
