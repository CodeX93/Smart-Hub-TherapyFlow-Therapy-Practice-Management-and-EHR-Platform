package com.smart.therapy.flow.client.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.client.entity.ClientReferral;
import com.smart.therapy.flow.client.enums.ReferralSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface ClientReferralRepository extends JpaRepository<ClientReferral, Long> {

    @Query("SELECT r FROM ClientReferral r WHERE r.client.id = :clientId")
    Optional<ClientReferral> findByClientId(@Param("clientId") Long clientId);

    @Query("SELECT r FROM ClientReferral r WHERE r.referrerName = :referrerName ORDER BY r.referralDate DESC")
    List<ClientReferral> findByReferrerName(@Param("referrerName") String referrerName);

    @Query("SELECT r FROM ClientReferral r WHERE r.referralSource = :source ORDER BY r.referralDate DESC")
    List<ClientReferral> findByReferralSource(@Param("source") ReferralSource source);

    @Query("SELECT r FROM ClientReferral r WHERE r.isCourtOrdered = true ORDER BY r.referralDate DESC")
    List<ClientReferral> findCourtOrderedReferrals();

    @Query("SELECT r FROM ClientReferral r WHERE r.requiresReporting = true ORDER BY r.referralDate DESC")
    List<ClientReferral> findReferralsRequiringReporting();
}




