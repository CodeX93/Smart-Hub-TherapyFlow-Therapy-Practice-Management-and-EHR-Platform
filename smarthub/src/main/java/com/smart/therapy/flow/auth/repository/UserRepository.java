package com.smart.therapy.flow.auth.repository;

import com.smart.therapy.flow.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.smart.therapy.flow.common.tenant.TenantScoped;

@Repository
@TenantScoped
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    interface AuditActorView {
        Long getId();
        String getEmail();
        String getFullName();
        String getLoginIdentifier();
    }

    /** Audit writes need actor metadata, without hydrating profile/session relationships. */
    @Query("""
            SELECT u.id AS id, u.email AS email, u.fullName AS fullName, a.loginIdentifier AS loginIdentifier
            FROM User u LEFT JOIN u.authIdentity a
            WHERE u.id = :id AND u.isDeleted = false
            """)
    Optional<AuditActorView> findAuditActorById(@Param("id") Long id);

    /** Batch actor labels for audit list display (stable loginIdentifier preferred). */
    @Query("""
            SELECT u.id AS id, u.email AS email, u.fullName AS fullName, a.loginIdentifier AS loginIdentifier
            FROM User u LEFT JOIN u.authIdentity a
            WHERE u.id IN :ids AND u.isDeleted = false
            """)
    List<AuditActorView> findAuditActorsByIdIn(@Param("ids") Collection<Long> ids);

    @Query("SELECT u FROM User u WHERE u.authIdentity.id = :authId AND u.isDeleted = false")
    Optional<User> findByAuthId(@Param("authId") Long authId);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isDeleted = false")
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * Resolve {@code auth_id} from the tenant {@code users} table without initializing
     * {@code public.auth_identities} (avoids cross-schema lazy-load failures during login).
     */
    @Query(value = """
            SELECT auth_id
            FROM users
            WHERE lower(email) = lower(:email)
              AND COALESCE(is_deleted, false) = false
            LIMIT 1
            """, nativeQuery = true)
    Optional<Long> findAuthIdByEmail(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.isDeleted = false")
    boolean existsByEmail(@Param("email") String email);

    /** Schema-per-tenant: current schema isolates data; organisationId kept for API compatibility. */
    @Query("SELECT u FROM User u WHERE u.isDeleted = false")
    List<User> findByOrganisationId(@Param("organisationId") Long organisationId);

    /**
     * Find users that have any of the given roles (via auth_identity_roles).
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.authIdentity a JOIN a.roles ar JOIN ar.role r " +
           "WHERE r.name IN :roleNames AND u.isDeleted = false")
    List<User> findDistinctByAuthIdentityRolesRoleNameIn(@Param("roleNames") Collection<String> roleNames);

    @Override
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.isDeleted = false")
    @NonNull
    Optional<User> findById(@NonNull @Param("id") Long id);

    @Override
    @Query("SELECT u FROM User u WHERE u.isDeleted = false")
    @NonNull
    List<User> findAll();

    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdIncludingDeleted(@Param("id") Long id);

    /** Count non-deleted users (for plan limit THERAPIST_SEATS). */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isDeleted = false")
    long countNonDeleted();
}
