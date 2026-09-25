package com.smart.therapy.flow.organisation.repository;

import com.smart.therapy.flow.organisation.entity.OrgStripeAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrgStripeAccountRepository extends JpaRepository<OrgStripeAccount, Long> {

    Optional<OrgStripeAccount> findByOrganisationId(Long organisationId);

    Optional<OrgStripeAccount> findByConnectAccountId(String connectAccountId);

    Optional<OrgStripeAccount> findByOauthState(String oauthState);

    boolean existsByConnectAccountIdAndOrganisationIdNot(String connectAccountId, Long organisationId);
}
