package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.OrgSubscription;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrgSubscriptionRepository extends JpaRepository<OrgSubscription, Long> {

    /**
     * Current subscription at query time.
     * Supports scheduled plan changes where current row has a future end_at.
     */
    @Query("""
            SELECT s FROM OrgSubscription s
            JOIN FETCH s.plan
            JOIN FETCH s.organisation
            WHERE s.organisation.id = :orgId
              AND s.startAt <= CURRENT_TIMESTAMP
              AND (s.endAt IS NULL OR s.endAt > CURRENT_TIMESTAMP)
            """)
    Optional<OrgSubscription> findCurrentByOrganisationId(@Param("orgId") Long orgId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("SELECT s FROM OrgSubscription s WHERE s.organisation.id = :orgId AND s.startAt <= CURRENT_TIMESTAMP AND (s.endAt IS NULL OR s.endAt > CURRENT_TIMESTAMP)")
    Optional<OrgSubscription> findCurrentByOrganisationIdForUpdateNowait(@Param("orgId") Long orgId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("SELECT s FROM OrgSubscription s WHERE s.id = :id")
    Optional<OrgSubscription> findByIdForUpdateNowait(@Param("id") Long id);

    @Query("SELECT s FROM OrgSubscription s WHERE s.organisation.id = :orgId ORDER BY s.startAt DESC")
    List<OrgSubscription> findAllByOrganisationIdOrderByStartAtDesc(@Param("orgId") Long orgId);

    @Query("SELECT s FROM OrgSubscription s JOIN FETCH s.plan WHERE s.startAt <= CURRENT_TIMESTAMP AND (s.endAt IS NULL OR s.endAt > CURRENT_TIMESTAMP) AND s.organisation.id IN :organisationIds")
    List<OrgSubscription> findCurrentByOrganisationIds(@Param("organisationIds") List<Long> organisationIds);

    @Query("SELECT s FROM OrgSubscription s JOIN FETCH s.organisation JOIN FETCH s.plan WHERE s.startAt <= CURRENT_TIMESTAMP AND (s.endAt IS NULL OR s.endAt > CURRENT_TIMESTAMP)")
    List<OrgSubscription> findAllCurrentWithOrganisationAndPlan();

    @Query("SELECT s FROM OrgSubscription s WHERE s.providerSubscriptionId = :providerSubscriptionId AND s.startAt <= CURRENT_TIMESTAMP AND (s.endAt IS NULL OR s.endAt > CURRENT_TIMESTAMP)")
    Optional<OrgSubscription> findByProviderSubscriptionIdAndEndAtIsNull(@Param("providerSubscriptionId") String providerSubscriptionId);

    @Query("SELECT s FROM OrgSubscription s WHERE s.providerCustomerId = :providerCustomerId AND s.startAt <= CURRENT_TIMESTAMP AND (s.endAt IS NULL OR s.endAt > CURRENT_TIMESTAMP)")
    Optional<OrgSubscription> findByProviderCustomerIdAndEndAtIsNull(@Param("providerCustomerId") String providerCustomerId);

    @Query("SELECT s FROM OrgSubscription s WHERE s.status = :status AND s.startAt <= CURRENT_TIMESTAMP AND (s.endAt IS NULL OR s.endAt > CURRENT_TIMESTAMP) AND s.trialEndAt IS NOT NULL AND s.trialEndAt <= :at")
    List<OrgSubscription> findTrialingExpired(@Param("status") String status, @Param("at") Instant at);

    @Query("SELECT s FROM OrgSubscription s WHERE s.status = :status AND s.startAt <= CURRENT_TIMESTAMP AND (s.endAt IS NULL OR s.endAt > CURRENT_TIMESTAMP) AND s.nextDunningAt IS NOT NULL AND s.nextDunningAt <= :at")
    List<OrgSubscription> findPastDueReadyForDunning(@Param("status") String status, @Param("at") Instant at);

    @Query("SELECT s FROM OrgSubscription s WHERE s.status = :status AND s.startAt <= CURRENT_TIMESTAMP AND (s.endAt IS NULL OR s.endAt > CURRENT_TIMESTAMP) AND s.trialEndAt IS NOT NULL AND s.trialEndAt > :fromAt AND s.trialEndAt <= :toAt")
    List<OrgSubscription> findTrialEndingBetween(@Param("status") String status, @Param("fromAt") Instant fromAt, @Param("toAt") Instant toAt);

    @Query("SELECT s FROM OrgSubscription s WHERE s.status = :status AND s.startAt <= CURRENT_TIMESTAMP AND (s.endAt IS NULL OR s.endAt > CURRENT_TIMESTAMP) AND s.trialEndAt IS NOT NULL AND s.trialEndAt > :fromAt AND s.trialEndAt <= :toAt AND COALESCE(s.notifiedTrial, false) = false")
    List<OrgSubscription> findTrialEndingBetweenNotifiedFalse(@Param("status") String status, @Param("fromAt") Instant fromAt, @Param("toAt") Instant toAt);

    @Query("SELECT s FROM OrgSubscription s WHERE s.status = :status AND s.startAt <= CURRENT_TIMESTAMP AND (s.endAt IS NULL OR s.endAt > CURRENT_TIMESTAMP) AND s.lastPaymentFailedAt IS NOT NULL")
    List<OrgSubscription> findPastDueForDunning(@Param("status") String status);

    @Query("SELECT s.id FROM OrgSubscription s WHERE s.status = :status AND s.startAt <= CURRENT_TIMESTAMP AND (s.endAt IS NULL OR s.endAt > CURRENT_TIMESTAMP) AND s.lastPaymentFailedAt IS NOT NULL ORDER BY CASE WHEN s.nextDunningAt IS NULL THEN 1 ELSE 0 END, s.nextDunningAt ASC, s.id ASC")
    List<Long> findPastDueForDunningIds(@Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(s) FROM OrgSubscription s WHERE s.startAt < :at AND (s.endAt IS NULL OR s.endAt >= :at)")
    long countActiveAt(@Param("at") Instant at);

    @Query("SELECT COUNT(s) FROM OrgSubscription s WHERE s.endAt >= :fromAt AND s.endAt < :toAt")
    long countEndedBetween(@Param("fromAt") Instant fromAt, @Param("toAt") Instant toAt);
}
