package com.smart.therapy.flow.organisation.repository;

import com.smart.therapy.flow.organisation.entity.UserOrganisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserOrganisationRepository extends JpaRepository<UserOrganisation, Long> {

    interface OrganisationUserCountView {
        Long getOrganisationId();
        Long getUsersCount();
    }

    List<UserOrganisation> findByAuth_Id(Long authId);

    List<UserOrganisation> findByOrganisation_Id(Long organisationId);

    @Query("SELECT DISTINCT uo.auth.id FROM UserOrganisation uo WHERE uo.organisation.id = :organisationId")
    List<Long> findDistinctAuthIdsByOrganisationId(@Param("organisationId") Long organisationId);

    boolean existsByAuth_IdAndOrganisation_Id(Long authId, Long organisationId);

    Optional<UserOrganisation> findByAuth_IdAndOrganisation_Id(Long authId, Long organisationId);

    @Query("SELECT u.organisation.id as organisationId, COUNT(u.id) as usersCount FROM UserOrganisation u WHERE u.organisation.id IN :organisationIds GROUP BY u.organisation.id")
    List<OrganisationUserCountView> countUsersByOrganisationIds(@Param("organisationIds") List<Long> organisationIds);

    @Query("""
            SELECT COUNT(uo) > 0
            FROM UserOrganisation uo
            JOIN uo.auth.roles air
            JOIN air.role r
            WHERE uo.organisation.id = :organisationId
              AND uo.auth.id = :targetAuthId
              AND UPPER(r.name) = 'THERAPIST'
              AND (air.organisation IS NULL OR air.organisation.id = :organisationId)
              AND air.isDeleted = false
              AND uo.auth.isDeleted = false
            """)
    boolean existsTherapistInOrganisation(@Param("targetAuthId") Long targetAuthId,
                                          @Param("organisationId") Long organisationId);
}
