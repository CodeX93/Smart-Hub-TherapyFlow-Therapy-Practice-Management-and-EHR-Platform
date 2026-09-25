package com.smart.therapy.flow.billing.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.billing.entity.SessionBilling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface SessionBillingRepository extends JpaRepository<SessionBilling, Long>, JpaSpecificationExecutor<SessionBilling> {
    Optional<SessionBilling> findBySessionId(Long sessionId);
    boolean existsBySessionId(Long sessionId);

    @Query("SELECT b FROM SessionBilling b WHERE b.session.id IN :sessionIds")
    List<SessionBilling> findBySessionIdIn(@Param("sessionIds") java.util.Collection<Long> sessionIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM SessionBilling b WHERE b.id = :id")
    Optional<SessionBilling> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT b.session.id FROM SessionBilling b WHERE b.id = :billingId")
    Optional<Long> findSessionIdByBillingId(@Param("billingId") Long billingId);

    @Query("SELECT b FROM SessionBilling b WHERE b.billingStatus = :status")
    List<SessionBilling> findByPaymentStatus(@Param("status") String status);

    @Query("SELECT b FROM SessionBilling b WHERE b.billingDate BETWEEN :startDate AND :endDate")
    List<SessionBilling> findByBillingDateBetween(@Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT b FROM SessionBilling b WHERE b.session.client.id = :clientId ORDER BY b.createdAt DESC")
    List<SessionBilling> findByClientId(@Param("clientId") Long clientId);

    /** Serialize credit allocation and refresh each source balance before spending it. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM SessionBilling b WHERE b.session.client.id = :clientId ORDER BY b.id")
    List<SessionBilling> findByClientIdForUpdate(@Param("clientId") Long clientId);

    @Query("SELECT b.id FROM SessionBilling b WHERE b.session.client.id = :clientId")
    List<Long> findIdsByClientId(@Param("clientId") Long clientId);

    @Query("SELECT b FROM SessionBilling b WHERE b.session.therapist.id = :therapistId ORDER BY b.createdAt DESC")
    List<SessionBilling> findByTherapistId(@Param("therapistId") Long therapistId);

    @Query("SELECT b FROM SessionBilling b WHERE b.billingDate >= :startDate AND b.billingDate <= :endDate ORDER BY b.billingDate DESC")
    List<SessionBilling> findByBillingDateRange(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT b FROM SessionBilling b WHERE b.totalAmount >= :minAmount AND b.totalAmount <= :maxAmount ORDER BY b.totalAmount DESC")
    List<SessionBilling> findByAmountRange(@Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount);

    @Query("SELECT COUNT(b) FROM SessionBilling b WHERE b.serviceCode = :serviceCode")
    long countByServiceCode(@Param("serviceCode") String serviceCode);

    @Query("SELECT COUNT(b) FROM SessionBilling b WHERE b.session.client.id = :clientId")
    long countByClientId(@Param("clientId") Long clientId);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM SessionBilling b WHERE b.session.client.id = :clientId")
    BigDecimal sumTotalAmountByClientId(@Param("clientId") Long clientId);

    @Query("""
            SELECT COALESCE(SUM(
                CASE
                    WHEN b.discountAmount IS NOT NULL AND b.discountAmount > 0
                        THEN CASE
                            WHEN (b.totalAmount - b.discountAmount) > 0 THEN (b.totalAmount - b.discountAmount)
                            ELSE 0
                        END
                    ELSE b.totalAmount
                END
            ), 0)
            FROM SessionBilling b
            WHERE b.session.client.id = :clientId
            """)
    BigDecimal sumAmountDueByClientId(@Param("clientId") Long clientId);

    @Query("SELECT COALESCE(SUM(b.paidAmount), 0) FROM SessionBilling b WHERE b.session.client.id = :clientId")
    BigDecimal sumPaidAmountByClientId(@Param("clientId") Long clientId);

    @Query("""
            SELECT COALESCE(SUM(b.totalAmount), 0)
            FROM SessionBilling b
            WHERE b.billingDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumTotalAmountByBillingDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(b.paidAmount), 0) FROM SessionBilling b")
    BigDecimal sumPaidAmount();

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN b.outstandingAmount > 0 THEN b.outstandingAmount ELSE 0 END), 0)
            FROM SessionBilling b
            """)
    BigDecimal sumPositiveOutstandingAmount();

    @Query("""
            SELECT COALESCE(SUM(b.totalAmount), 0)
            FROM SessionBilling b
            WHERE b.session.therapist.id = :therapistId
              AND b.billingDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumTotalAmountByTherapistIdAndBillingDateBetween(
            @Param("therapistId") Long therapistId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(b.paidAmount), 0)
            FROM SessionBilling b
            WHERE b.session.therapist.id = :therapistId
            """)
    BigDecimal sumPaidAmountByTherapistId(@Param("therapistId") Long therapistId);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN b.outstandingAmount > 0 THEN b.outstandingAmount ELSE 0 END), 0)
            FROM SessionBilling b
            WHERE b.session.therapist.id = :therapistId
            """)
    BigDecimal sumPositiveOutstandingAmountByTherapistId(@Param("therapistId") Long therapistId);

    /**
     * Dashboard / ClientHub parity: month windows are DATE(session_date) in practice TZ,
     * expressed as an Instant half-open range {@code [startInclusive, endExclusive)}.
     */
    @Query("""
            SELECT COALESCE(SUM(b.totalAmount), 0)
            FROM SessionBilling b
            WHERE b.session.sessionDate >= :startInclusive
              AND b.session.sessionDate < :endExclusive
            """)
    BigDecimal sumTotalAmountForSessionDateRange(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive);

    @Query("""
            SELECT COALESCE(SUM(b.paidAmount), 0)
            FROM SessionBilling b
            WHERE b.session.sessionDate >= :startInclusive
              AND b.session.sessionDate < :endExclusive
            """)
    BigDecimal sumPaidAmountForSessionDateRange(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN b.outstandingAmount > 0 THEN b.outstandingAmount ELSE 0 END), 0)
            FROM SessionBilling b
            WHERE b.session.sessionDate >= :startInclusive
              AND b.session.sessionDate < :endExclusive
            """)
    BigDecimal sumPositiveOutstandingAmountForSessionDateRange(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive);

    @Query("""
            SELECT COALESCE(SUM(b.totalAmount), 0)
            FROM SessionBilling b
            WHERE b.session.therapist.id = :therapistId
              AND b.session.sessionDate >= :startInclusive
              AND b.session.sessionDate < :endExclusive
            """)
    BigDecimal sumTotalAmountByTherapistIdForSessionDateRange(
            @Param("therapistId") Long therapistId,
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive);

    @Query("""
            SELECT COALESCE(SUM(b.paidAmount), 0)
            FROM SessionBilling b
            WHERE b.session.therapist.id = :therapistId
              AND b.session.sessionDate >= :startInclusive
              AND b.session.sessionDate < :endExclusive
            """)
    BigDecimal sumPaidAmountByTherapistIdForSessionDateRange(
            @Param("therapistId") Long therapistId,
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN b.outstandingAmount > 0 THEN b.outstandingAmount ELSE 0 END), 0)
            FROM SessionBilling b
            WHERE b.session.therapist.id = :therapistId
              AND b.session.sessionDate >= :startInclusive
              AND b.session.sessionDate < :endExclusive
            """)
    BigDecimal sumPositiveOutstandingAmountByTherapistIdForSessionDateRange(
            @Param("therapistId") Long therapistId,
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive);

    /**
     * Date-bounded aggregates must use BETWEEN (not {@code :param IS NULL OR ...}).
     * PostgreSQL cannot infer JDBC types for the null-check form and fails with
     * "could not determine data type of parameter".
     */
    @Query("""
            SELECT COALESCE(SUM(b.totalAmount), 0)
            FROM SessionBilling b
            WHERE b.billingDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumTotalAmountForDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(b.paidAmount), 0)
            FROM SessionBilling b
            WHERE b.billingDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumPaidAmountForDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN b.outstandingAmount > 0 THEN b.outstandingAmount ELSE 0 END), 0)
            FROM SessionBilling b
            WHERE b.billingDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumPositiveOutstandingAmountForDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(CASE
                WHEN b.paidAmount IS NOT NULL AND (
                    CASE
                        WHEN b.discountAmount IS NOT NULL AND b.discountAmount > 0
                            THEN CASE
                                WHEN (b.totalAmount - b.discountAmount) > 0 THEN (b.totalAmount - b.discountAmount)
                                ELSE 0
                            END
                        ELSE COALESCE(b.totalAmount, 0)
                    END
                ) < b.paidAmount
                THEN b.paidAmount - (
                    CASE
                        WHEN b.discountAmount IS NOT NULL AND b.discountAmount > 0
                            THEN CASE
                                WHEN (b.totalAmount - b.discountAmount) > 0 THEN (b.totalAmount - b.discountAmount)
                                ELSE 0
                            END
                        ELSE COALESCE(b.totalAmount, 0)
                    END
                )
                ELSE 0
            END), 0)
            FROM SessionBilling b
            WHERE b.billingDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumCreditBalanceForDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COUNT(b)
            FROM SessionBilling b
            WHERE b.billingDate BETWEEN :startDate AND :endDate
            """)
    long countForDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COUNT(b)
            FROM SessionBilling b
            WHERE b.billingStatus IN :statuses
              AND b.billingDate BETWEEN :startDate AND :endDate
            """)
    long countByBillingStatusInForDateRange(
            @Param("statuses") java.util.Collection<com.smart.therapy.flow.billing.enums.BillingStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COUNT(b)
            FROM SessionBilling b
            WHERE b.billingDate BETWEEN :startDate AND :endDate
              AND COALESCE(b.paidAmount, 0) > 0
              AND b.billingStatus <> :paidStatus
            """)
    long countPartiallyPaidForDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("paidStatus") com.smart.therapy.flow.billing.enums.BillingStatus paidStatus);

    @Query("""
            SELECT COUNT(DISTINCT b.session.client.id)
            FROM SessionBilling b
            WHERE b.billingDate BETWEEN :startDate AND :endDate
            """)
    long countDistinctClientsForDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
