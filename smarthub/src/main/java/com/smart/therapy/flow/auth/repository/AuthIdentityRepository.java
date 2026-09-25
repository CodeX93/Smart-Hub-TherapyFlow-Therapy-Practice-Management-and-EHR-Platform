package com.smart.therapy.flow.auth.repository;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthProvider;
import com.smart.therapy.flow.auth.entity.IdentityType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

@Repository
public interface AuthIdentityRepository extends JpaRepository<AuthIdentity, Long>, JpaSpecificationExecutor<AuthIdentity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AuthIdentity a WHERE a.id = :id")
    Optional<AuthIdentity> findByIdForUpdate(@Param("id") Long id);

    /**
     * Legacy unscoped lookup by login alias. Prefer {@link #findAllByEmailOrUsername(String)}.
     * May return multiple rows after tenant-scoped uniqueness.
     */
    @Query("""
            SELECT a
            FROM AuthIdentity a
            WHERE a.normalisedLoginIdentifier = :identifier
               OR LOWER(a.loginIdentifier) = :identifier
               OR a.normalisedEmail = :identifier
               OR a.normalisedUsername = :identifier
            """)
    List<AuthIdentity> findAllByAnyLoginIdentifier(@Param("identifier") String identifier);

    /** @deprecated use {@link #findAllByAnyLoginIdentifier(String)} — kept for transitional callers. */
    @Deprecated
    default Optional<AuthIdentity> findByAnyLoginIdentifier(String identifier) {
        List<AuthIdentity> all = findAllByAnyLoginIdentifier(identifier);
        if (all.isEmpty()) {
            return Optional.empty();
        }
        if (all.size() == 1) {
            return Optional.of(all.get(0));
        }
        // Prefer platform (org-null) when multiple; otherwise first — callers should use org-scoped APIs.
        return all.stream()
                .filter(a -> a.getOrganisation() == null)
                .findFirst()
                .or(() -> Optional.of(all.get(0)));
    }

    @Query("""
            SELECT a
            FROM AuthIdentity a
            WHERE a.normalisedEmail = :identifier
               OR a.normalisedUsername = :identifier
               OR a.normalisedLoginIdentifier = :identifier
            """)
    List<AuthIdentity> findAllByEmailOrUsername(@Param("identifier") String identifier);

    @Query("""
            SELECT a
            FROM AuthIdentity a
            WHERE a.organisation.id = :organisationId
              AND (
                    a.normalisedEmail = :identifier
                 OR a.normalisedUsername = :identifier
                 OR a.normalisedLoginIdentifier = :identifier
              )
            """)
    List<AuthIdentity> findAllByEmailOrUsernameForOrganisation(
            @Param("identifier") String identifier,
            @Param("organisationId") Long organisationId);

    @Query("""
            SELECT a
            FROM AuthIdentity a
            WHERE a.organisation.id = :organisationId
              AND a.identityType = :identityType
              AND (
                    a.normalisedEmail = :identifier
                 OR a.normalisedUsername = :identifier
                 OR a.normalisedLoginIdentifier = :identifier
              )
            """)
    List<AuthIdentity> findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
            @Param("identifier") String identifier,
            @Param("identityType") IdentityType identityType,
            @Param("organisationId") Long organisationId);

    @Query("""
            SELECT COUNT(a) > 0
            FROM AuthIdentity a
            WHERE a.organisation.id = :organisationId
              AND a.normalisedEmail = :normalisedEmail
              AND (:excludeId IS NULL OR a.id <> :excludeId)
            """)
    boolean existsByOrganisationIdAndNormalisedEmail(
            @Param("organisationId") Long organisationId,
            @Param("normalisedEmail") String normalisedEmail,
            @Param("excludeId") Long excludeId);

    @Query("""
            SELECT COUNT(a) > 0
            FROM AuthIdentity a
            WHERE a.organisation.id = :organisationId
              AND a.normalisedUsername = :normalisedUsername
              AND (:excludeId IS NULL OR a.id <> :excludeId)
            """)
    boolean existsByOrganisationIdAndNormalisedUsername(
            @Param("organisationId") Long organisationId,
            @Param("normalisedUsername") String normalisedUsername,
            @Param("excludeId") Long excludeId);

    @Query("""
            SELECT COUNT(a) > 0
            FROM AuthIdentity a
            WHERE a.organisation IS NULL
              AND a.normalisedEmail = :normalisedEmail
              AND (:excludeId IS NULL OR a.id <> :excludeId)
            """)
    boolean existsPlatformByNormalisedEmail(
            @Param("normalisedEmail") String normalisedEmail,
            @Param("excludeId") Long excludeId);

    @Query("""
            SELECT COUNT(a) > 0
            FROM AuthIdentity a
            WHERE a.organisation IS NULL
              AND a.normalisedUsername = :normalisedUsername
              AND (:excludeId IS NULL OR a.id <> :excludeId)
            """)
    boolean existsPlatformByNormalisedUsername(
            @Param("normalisedUsername") String normalisedUsername,
            @Param("excludeId") Long excludeId);

    Optional<AuthIdentity> findByNormalisedLoginIdentifier(String normalisedLoginIdentifier);

    boolean existsByNormalisedLoginIdentifier(String normalisedLoginIdentifier);

    @Query("SELECT a FROM AuthIdentity a WHERE a.normalisedLoginIdentifier = :normalised AND a.identityType = :identityType")
    Optional<AuthIdentity> findByNormalisedLoginIdentifierAndIdentityType(
            @Param("normalised") String normalised, @Param("identityType") IdentityType identityType);

    List<AuthIdentity> findAllByNormalisedLoginIdentifierAndIdentityType(String normalisedLoginIdentifier, IdentityType identityType);

    @Query("""
            SELECT a
            FROM AuthIdentity a
            WHERE (
                    a.normalisedLoginIdentifier = :normalised
                 OR a.normalisedEmail = :normalised
                 OR a.normalisedUsername = :normalised
              )
              AND a.identityType = :identityType
              AND (
                    (a.organisation IS NOT NULL AND a.organisation.id = :organisationId)
                    OR EXISTS (
                        SELECT 1
                        FROM UserOrganisation uo
                        WHERE uo.auth.id = a.id
                          AND uo.organisation.id = :organisationId
                    )
              )
            """)
    List<AuthIdentity> findAllByNormalisedLoginIdentifierAndIdentityTypeForOrganisation(
            @Param("normalised") String normalised,
            @Param("identityType") IdentityType identityType,
            @Param("organisationId") Long organisationId);

    @Query("SELECT COUNT(a) > 0 FROM AuthIdentity a WHERE a.normalisedLoginIdentifier = :normalised AND a.identityType = :identityType")
    boolean existsByNormalisedLoginIdentifierAndIdentityType(
            @Param("normalised") String normalised, @Param("identityType") IdentityType identityType);

    @Query("SELECT a FROM AuthIdentity a WHERE a.emailVerificationToken = :token")
    Optional<AuthIdentity> findByEmailVerificationToken(@Param("token") String token);

    @Query("SELECT a FROM AuthIdentity a WHERE a.passwordResetToken = :token")
    Optional<AuthIdentity> findByPasswordResetToken(@Param("token") String token);

    Optional<AuthIdentity> findByAuthProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    List<AuthIdentity> findByOrganisation_Id(Long organisationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE AuthIdentity a
            SET a.emailVerificationToken = :token,
                a.emailVerificationExpiry = :expiry,
                a.updatedAt = CURRENT_TIMESTAMP
            WHERE a.id = :id
            """)
    int updateEmailVerificationToken(
            @Param("id") Long id,
            @Param("token") String token,
            @Param("expiry") Instant expiry);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE AuthIdentity a
            SET a.passwordResetToken = :token,
                a.passwordResetExpiry = :expiry,
                a.updatedAt = CURRENT_TIMESTAMP
            WHERE a.id = :id
            """)
    int updatePasswordResetToken(
            @Param("id") Long id,
            @Param("token") String token,
            @Param("expiry") Instant expiry);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE AuthIdentity a
            SET a.passwordResetToken = NULL,
                a.passwordResetExpiry = NULL,
                a.updatedAt = CURRENT_TIMESTAMP
            WHERE a.id = :id
            """)
    int clearPasswordResetTokenById(@Param("id") Long id);
}
