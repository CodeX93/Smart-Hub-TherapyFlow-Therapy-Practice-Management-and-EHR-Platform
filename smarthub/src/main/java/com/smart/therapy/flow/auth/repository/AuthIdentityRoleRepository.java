package com.smart.therapy.flow.auth.repository;

import com.smart.therapy.flow.auth.entity.AuthIdentityRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthIdentityRoleRepository extends JpaRepository<AuthIdentityRole, Long> {

    interface RoleUsageCount {
        String getRoleName();
        Long getCount();
    }

    @Query("SELECT air FROM AuthIdentityRole air " +
           "JOIN FETCH air.role r " +
           "LEFT JOIN FETCH r.rolePermissions rp " +
           "LEFT JOIN FETCH rp.permission " +
           "WHERE air.authIdentity.id = :authId AND air.organisation.id = :organisationId")
    List<AuthIdentityRole> findByAuthIdAndOrganisationIdWithRolesAndPermissions(
        @Param("authId") Long authId,
        @Param("organisationId") Long organisationId);

    @Query("SELECT air FROM AuthIdentityRole air " +
           "JOIN FETCH air.role r " +
           "LEFT JOIN FETCH r.rolePermissions rp " +
           "LEFT JOIN FETCH rp.permission " +
           "WHERE air.authIdentity.id = :authId")
    List<AuthIdentityRole> findByAuthIdWithRolesAndPermissions(@Param("authId") Long authId);

    @Query("SELECT air FROM AuthIdentityRole air " +
           "JOIN FETCH air.role r " +
           "LEFT JOIN FETCH r.rolePermissions rp " +
           "LEFT JOIN FETCH rp.permission " +
           "WHERE air.authIdentity.id = :authId AND air.organisation IS NULL")
    List<AuthIdentityRole> findByAuthIdWithRolesAndPermissionsForPlatform(@Param("authId") Long authId);

    @Query("SELECT UPPER(r.name) AS roleName, COUNT(air.id) AS count " +
           "FROM AuthIdentityRole air " +
           "JOIN air.role r " +
           "WHERE air.organisation.id = :organisationId " +
           "GROUP BY UPPER(r.name)")
    List<RoleUsageCount> countByRoleForOrganisation(@Param("organisationId") Long organisationId);

    @Query("""
            SELECT COUNT(DISTINCT air.authIdentity.id)
            FROM AuthIdentityRole air
            JOIN air.role r
            WHERE air.organisation.id = :organisationId
              AND UPPER(r.name) = UPPER(:roleName)
              AND air.authIdentity.isActive = true
              AND air.authIdentity.isDeleted = false
            """)
    long countActiveByRoleForOrganisation(@Param("organisationId") Long organisationId,
                                          @Param("roleName") String roleName);

    @Query("""
            SELECT COUNT(DISTINCT air.authIdentity.id)
            FROM AuthIdentityRole air
            JOIN air.role r
            WHERE air.organisation IS NULL
              AND UPPER(r.name) = 'SUPER_ADMIN'
              AND air.authIdentity.isActive = true
            """)
    long countActivePlatformSuperAdmins();

    @Query("""
            SELECT DISTINCT air.authIdentity
            FROM AuthIdentityRole air
            JOIN air.role r
            WHERE air.organisation.id = :organisationId
              AND UPPER(r.name) = 'ADMIN'
              AND air.authIdentity.isActive = true
              AND (air.authIdentity.isDeleted = false OR air.authIdentity.isDeleted IS NULL)
            """)
    List<com.smart.therapy.flow.auth.entity.AuthIdentity> findActiveAdminsByOrganisationId(
            @Param("organisationId") Long organisationId);

    /**
     * Admin email addresses for an organisation, the primary administrator first: the identity
     * created by onboarding has the lowest id, and a deactivated admin sorts behind an active one.
     */
    @Query("""
            SELECT ai.email
            FROM AuthIdentityRole air
            JOIN air.role r
            JOIN air.authIdentity ai
            WHERE air.organisation.id = :organisationId
              AND UPPER(r.name) = 'ADMIN'
              AND (ai.isDeleted = false OR ai.isDeleted IS NULL)
            ORDER BY CASE WHEN ai.isActive = true THEN 0 ELSE 1 END, ai.id ASC
            """)
    List<String> findAdminEmailsByOrganisationId(@Param("organisationId") Long organisationId);

    boolean existsByAuthIdentity_IdAndRole_IdAndOrganisation_Id(Long authId, Long roleId, Long organisationId);
}
