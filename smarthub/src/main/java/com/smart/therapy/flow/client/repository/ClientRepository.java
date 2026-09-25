package com.smart.therapy.flow.client.repository;

import com.smart.therapy.flow.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.smart.therapy.flow.common.tenant.TenantScoped;

@Repository
@TenantScoped
public interface ClientRepository extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {

    @Query("SELECT c FROM Client c WHERE c.authIdentity.id = :authId AND c.isDeleted = false")
    Optional<Client> findByAuthId(@Param("authId") Long authId);

    /** Schema-per-tenant: current schema isolates data; organisationId kept for API compatibility. */
    @Query("SELECT c FROM Client c WHERE c.isDeleted = false")
    List<Client> findByOrganisationId(@Param("organisationId") Long organisationId);

    /** Scan MRNs without hydrating client graphs when legacy blind indexes are absent or stale. */
    @Query("SELECT c.id, c.clientId FROM Client c WHERE c.isDeleted = false")
    List<Object[]> findActiveMrnCandidates();

    @Query("SELECT c.id, c.clientType FROM Client c WHERE c.isDeleted = false")
    List<Object[]> findActiveClientTypeCandidates();

    /** Only the searchable numbers; avoid loading/decrypting full client and referral graphs. */
    @Query("SELECT c.id, c.clientId, r.referenceNumber FROM Client c LEFT JOIN c.referral r WHERE c.isDeleted = false")
    List<Object[]> findActiveClientNumberCandidates();


    @Query("SELECT c FROM Client c WHERE c.clientId = :clientId AND c.isDeleted = false")
    Optional<Client> findByClientId(@Param("clientId") String clientId);

    @Query("SELECT c FROM Client c WHERE c.clientIdBlindIdx = :blindIdx AND c.isDeleted = false")
    Optional<Client> findByClientIdBlindIdxAndIsDeletedFalse(@Param("blindIdx") byte[] blindIdx);

    @Query("SELECT COUNT(c) > 0 FROM Client c WHERE c.clientIdBlindIdx = :blindIdx AND c.isDeleted = false")
    boolean existsByClientIdBlindIdxAndIsDeletedFalse(@Param("blindIdx") byte[] blindIdx);

    @Query("SELECT COUNT(c) > 0 FROM Client c WHERE c.clientIdBlindIdx = :blindIdx")
    boolean existsByClientIdBlindIdx(@Param("blindIdx") byte[] blindIdx);
    
    // Note: This method uses deprecated field (email) which no longer exists in Client entity.
    // Use ClientContactRepository.findByContactValue() with ContactType.EMAIL instead.
    @Deprecated
    @Query("""
        SELECT DISTINCT c
        FROM Client c
        JOIN c.contacts cc
        WHERE cc.contactType = com.smart.therapy.flow.client.enums.ContactType.EMAIL
          AND LOWER(cc.contactValue) = LOWER(:email)
          AND c.isDeleted = false
        """)
    Optional<Client> findByEmail(@Param("email") String email);
    
    @Query("SELECT c FROM Client c WHERE c.assignedTherapist.id = :therapistId AND c.isDeleted = false")
    List<Client> findByAssignedTherapistId(@Param("therapistId") Long therapistId);
    
    // Legacy String-based methods (kept for backward compatibility)
    @Query("SELECT c FROM Client c WHERE c.status = :status AND c.isDeleted = false")
    List<Client> findByStatus(@Param("status") String status);
    
    // New enum-based methods (preferred)
    @Query("SELECT c FROM Client c WHERE c.status = :status AND c.isDeleted = false")
    List<Client> findByStatusEnum(@Param("status") String status);
    
    @Query("SELECT COUNT(c) > 0 FROM Client c WHERE c.clientId = :clientId AND c.isDeleted = false")
    boolean existsByClientId(@Param("clientId") String clientId);
    
    // Note: This method uses deprecated field (email) which no longer exists in Client entity.
    // Use ClientContactRepository.findByContactValue() with ContactType.EMAIL instead.
    @Deprecated
    @Query("""
        SELECT COUNT(DISTINCT c) > 0
        FROM Client c
        JOIN c.contacts cc
        WHERE cc.contactType = com.smart.therapy.flow.client.enums.ContactType.EMAIL
          AND LOWER(cc.contactValue) = LOWER(:email)
          AND c.isDeleted = false
        """)
    boolean existsByEmail(@Param("email") String email);
    
    @Query("""
        SELECT COUNT(c) > 0
        FROM Client c
        JOIN c.authIdentity a
        WHERE a.normalisedLoginIdentifier = LOWER(TRIM(:portalEmail))
          AND c.isDeleted = false
        """)
    boolean existsByPortalEmail(@Param("portalEmail") String portalEmail);

    @Query("SELECT c FROM Client c WHERE c.createdAt >= :startDate AND c.createdAt < :endDate AND c.isDeleted = false")
    List<Client> findByCreatedAtBetween(@Param("startDate") java.time.Instant startDate, @Param("endDate") java.time.Instant endDate);

    // Legacy String-based method (kept for backward compatibility)
    @Query("SELECT COUNT(c) FROM Client c WHERE c.status = :status AND c.isDeleted = false")
    Long countByStatus(@Param("status") String status);
    
    // New enum-based method (preferred)
    @Query("SELECT COUNT(c) FROM Client c WHERE c.status = :status AND c.isDeleted = false")
    Long countByStatusEnum(@Param("status") String status);

    /** Count all non-deleted clients (for plan limit enforcement). */
    @Query("SELECT COUNT(c) FROM Client c WHERE c.isDeleted = false")
    long countNonDeleted();

    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.assignedTherapist WHERE (c.assignedTherapist.id = :therapistId OR c.assignedTherapist IS NULL) AND c.isDeleted = false")
    List<Client> findByAssignedTherapistIdOrUnassigned(@Param("therapistId") Long therapistId);
    
    // Portal login = authIdentity.loginIdentifier; contact email = contacts with ContactType.EMAIL.
    @Query("""
        SELECT DISTINCT c
        FROM Client c
        LEFT JOIN FETCH c.assignedTherapist
        LEFT JOIN c.authIdentity auth
        LEFT JOIN c.contacts cc
        WHERE c.isDeleted = false
          AND (
            (auth IS NOT NULL AND LOWER(auth.normalisedLoginIdentifier) = LOWER(TRIM(:email)))
            OR
            (cc.contactType = com.smart.therapy.flow.client.enums.ContactType.EMAIL AND LOWER(cc.contactValue) = LOWER(:email))
          )
        """)
    Optional<Client> findByPortalEmailOrEmail(@Param("email") String email);
    
    @Query("""
        SELECT c FROM Client c
        LEFT JOIN FETCH c.assignedTherapist
        LEFT JOIN FETCH c.authIdentity
        LEFT JOIN FETCH c.insurance
        LEFT JOIN FETCH c.referral
        LEFT JOIN FETCH c.employment
        LEFT JOIN FETCH c.portalSettings
        WHERE c.id = :id AND c.isDeleted = false
        """)
    Optional<Client> findByIdWithTherapist(@Param("id") Long id);
    
    /** Fetch everything used after commit, before the repository persistence context closes. */
    @Query("""
        SELECT DISTINCT c FROM Client c
        LEFT JOIN FETCH c.contacts
        LEFT JOIN FETCH c.assignedTherapist
        LEFT JOIN FETCH c.referral
        WHERE c.id = :id AND c.isDeleted = false
        """)
    Optional<Client> findForCreatedEvent(@Param("id") Long id);

    // Legacy String-based method (kept for backward compatibility)
    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.assignedTherapist WHERE c.status = :status AND c.isDeleted = false")
    List<Client> findByStatusWithRelations(@Param("status") String status);
    
    // New enum-based method (preferred)
    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.assignedTherapist WHERE c.status = :status AND c.isDeleted = false")
    List<Client> findByStatusWithRelationsEnum(@Param("status") String status);
    
    @Query("""
        SELECT c
        FROM Client c
        LEFT JOIN FETCH c.assignedTherapist
        JOIN c.authIdentity a
        WHERE a.emailVerificationToken = :token
          AND c.isDeleted = false
        """)
    Optional<Client> findByActivationToken(@Param("token") String activationToken);

    @Query("""
        SELECT c
        FROM Client c
        JOIN c.authIdentity a
        WHERE a.passwordResetToken = :token
          AND c.isDeleted = false
        """)
    Optional<Client> findByPasswordResetToken(@Param("token") String passwordResetToken);
    
    @Query("SELECT COUNT(c) FROM Client c WHERE c.assignedTherapist.id = :therapistId AND c.isDeleted = false")
    Long countByAssignedTherapistId(@Param("therapistId") Long therapistId);
    
    /**
     * Find the client with the highest clientId for the given prefix.
     * Used for efficient client ID generation without loading all clients.
     * Performance: Uses database index on clientId instead of loading all clients.
     * Note: Spring Data JPA automatically limits to 1 result for methods starting with "findTop"
     */
    @Query(value = "SELECT * FROM clients WHERE client_id LIKE CONCAT(:prefix, '%') AND is_deleted = false ORDER BY client_id DESC LIMIT 1", nativeQuery = true)
    Optional<Client> findTopByClientIdStartingWithOrderByClientIdDesc(@Param("prefix") String prefix);

    @Query(value = "SELECT * FROM clients WHERE client_id LIKE CONCAT(:prefix, '%') ORDER BY client_id DESC LIMIT 1", nativeQuery = true)
    Optional<Client> findTopByClientIdStartingWithOrderByClientIdDescIncludingDeleted(@Param("prefix") String prefix);

    @Query("SELECT COUNT(c) > 0 FROM Client c WHERE c.clientId = :clientId")
    boolean existsByClientIdIncludingDeleted(@Param("clientId") String clientId);
    
    // Override default findById to filter deleted records
    @Override
    @Query("SELECT c FROM Client c WHERE c.id = :id AND c.isDeleted = false")
    @NonNull
    Optional<Client> findById(@NonNull @Param("id") Long id);
    
    // Override default findAll to filter deleted records
    @Override
    @Query("SELECT c FROM Client c WHERE c.isDeleted = false")
    @NonNull
    List<Client> findAll();
    
    // Method to find by ID including deleted (for admin recovery purposes)
    @Query("SELECT c FROM Client c WHERE c.id = :id")
    Optional<Client> findByIdIncludingDeleted(@Param("id") Long id);

    /** Resolve an audit FK without reading the client's clinical relationships. */
    @Query("SELECT c.id FROM Client c WHERE c.id = :id")
    Optional<Long> findExistingIdIncludingDeleted(@Param("id") Long id);
    
    // New methods for enum-based queries
    @Query("SELECT c FROM Client c WHERE c.stage = :stage AND c.isDeleted = false")
    List<Client> findByStage(@Param("stage") String stage);
    
    @Query("SELECT c FROM Client c WHERE c.status = :status AND c.stage = :stage AND c.isDeleted = false")
    List<Client> findByStatusAndStage(@Param("status") String status, @Param("stage") String stage);
    
    /**
     * Golden Rule 2: Find clients by full name and date of birth for idempotency check.
     * Prefer blind-index equality when Approach C digests are present (encrypted DOB/name).
     */
    @Query("""
        SELECT c FROM Client c
        WHERE c.fullNameBlindIdx = :fullNameBlindIdx
          AND (
            (:dateOfBirthBlindIdx IS NOT NULL AND c.dateOfBirthBlindIdx = :dateOfBirthBlindIdx)
            OR (:dateOfBirthBlindIdx IS NULL AND c.dateOfBirthBlindIdx IS NULL)
          )
          AND c.isDeleted = false
        """)
    List<Client> findByFullNameBlindIdxAndDateOfBirthBlindIdx(
            @Param("fullNameBlindIdx") byte[] fullNameBlindIdx,
            @Param("dateOfBirthBlindIdx") byte[] dateOfBirthBlindIdx);

    /**
     * Legacy fallback when blind indexes are not written ({@code search.mode=legacy}).
     * After DOB encryption this path only works for unreencrypted plaintext rows.
     */
    @Query("""
        SELECT c FROM Client c 
        WHERE c.fullName = :fullName 
          AND (c.dateOfBirth = :dateOfBirth OR (:dateOfBirth IS NULL AND c.dateOfBirth IS NULL))
          AND c.isDeleted = false
        """)
    List<Client> findByFullNameAndDateOfBirth(@Param("fullName") String fullName, @Param("dateOfBirth") java.time.LocalDate dateOfBirth);

    /**
     * Soft-deleted clients whose deletion timestamp is past the retention cutoff.
     * Used by the client-purge job (see ClientPurgeScheduler) to anonymize PHI without
     * dropping rows or the tenant schema. Filters only on non-PHI columns; the scheduler
     * skips clients already anonymized by inspecting the (decrypted) fullName in Java.
     */
    @Query("SELECT c FROM Client c WHERE c.isDeleted = true AND c.deletedAt IS NOT NULL AND c.deletedAt < :cutoff")
    List<Client> findSoftDeletedBefore(@Param("cutoff") Instant cutoff);
}

