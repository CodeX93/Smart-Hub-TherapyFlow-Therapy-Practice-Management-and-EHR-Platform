package com.smart.therapy.flow.auth.repository;

import com.smart.therapy.flow.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {
    Optional<Role> findByName(String name);
    boolean existsByName(String name);

    @Query("SELECT r FROM Role r WHERE r.organisation IS NULL OR r.organisation.id = :orgId")
    List<Role> findAllForOrganisation(@Param("orgId") Long orgId);

    @Query("SELECT r FROM Role r WHERE r.id = :roleId AND (r.organisation IS NULL OR r.organisation.id = :orgId)")
    Optional<Role> findByIdForOrganisation(@Param("roleId") Long roleId, @Param("orgId") Long orgId);

    @Query("SELECT r FROM Role r WHERE LOWER(r.name) = LOWER(:name) AND (r.organisation IS NULL OR r.organisation.id = :orgId)")
    Optional<Role> findByNameForOrganisation(@Param("name") String name, @Param("orgId") Long orgId);

    @Query("""
            SELECT DISTINCT r
            FROM Role r
            LEFT JOIN FETCH r.rolePermissions rp
            LEFT JOIN FETCH rp.permission p
            """)
    List<Role> findAllWithPermissions();

    List<Role> findByOrganisationIsNull();

    List<Role> findByOrganisation_Id(Long organisationId);

    Optional<Role> findByIdAndOrganisationIsNull(Long id);

    Optional<Role> findByIdAndOrganisation_Id(Long id, Long organisationId);

    Optional<Role> findByNameIgnoreCaseAndOrganisationIsNull(String name);

    Optional<Role> findByNameIgnoreCaseAndOrganisation_Id(String name, Long organisationId);

    default Optional<Role> findByNameAndOrganisationIsNullIgnoreCase(String name) {
        return findByNameIgnoreCaseAndOrganisationIsNull(name);
    }
}

