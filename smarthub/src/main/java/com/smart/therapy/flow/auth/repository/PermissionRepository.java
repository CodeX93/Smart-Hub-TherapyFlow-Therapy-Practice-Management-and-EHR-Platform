package com.smart.therapy.flow.auth.repository;

import com.smart.therapy.flow.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(String name);
    boolean existsByName(String name);

    @Query("SELECT p FROM Permission p WHERE p.organisation IS NULL OR p.organisation.id = :orgId")
    List<Permission> findAllForOrganisation(@Param("orgId") Long orgId);

    @Query("SELECT p FROM Permission p WHERE p.id = :permissionId AND (p.organisation IS NULL OR p.organisation.id = :orgId)")
    Optional<Permission> findByIdForOrganisation(@Param("permissionId") Long permissionId, @Param("orgId") Long orgId);

    @Query("SELECT p FROM Permission p WHERE LOWER(p.name) = LOWER(:name) AND (p.organisation IS NULL OR p.organisation.id = :orgId)")
    Optional<Permission> findByNameForOrganisation(@Param("name") String name, @Param("orgId") Long orgId);

    List<Permission> findByOrganisationIsNull();

    List<Permission> findByOrganisation_Id(Long organisationId);

    Optional<Permission> findByIdAndOrganisationIsNull(Long id);

    Optional<Permission> findByIdAndOrganisation_Id(Long id, Long organisationId);

    Optional<Permission> findByNameIgnoreCaseAndOrganisationIsNull(String name);

    Optional<Permission> findByNameIgnoreCaseAndOrganisation_Id(String name, Long organisationId);

    default Optional<Permission> findByNameAndOrganisationIsNullIgnoreCase(String name) {
        return findByNameIgnoreCaseAndOrganisationIsNull(name);
    }
}

