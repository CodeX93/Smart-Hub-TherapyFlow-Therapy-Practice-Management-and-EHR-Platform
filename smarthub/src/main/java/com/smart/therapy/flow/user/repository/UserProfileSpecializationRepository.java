package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.entity.UserProfileSpecialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface UserProfileSpecializationRepository extends JpaRepository<UserProfileSpecialization, Long> {

    List<UserProfileSpecialization> findByUserProfile(UserProfile userProfile);

    List<UserProfileSpecialization> findBySpecialization(String specialization);

    @Query("SELECT DISTINCT s.userProfile FROM UserProfileSpecialization s " +
            "WHERE s.specialization = :specialization " +
            "AND s.expertiseLevel IN ('ADVANCED', 'EXPERT') " +
            "AND s.isDeleted = false")
    List<UserProfile> findExpertsBySpecialization(@Param("specialization") String specialization);

    @Query("SELECT s FROM UserProfileSpecialization s " +
            "WHERE s.userProfile = :profile AND s.isPrimary = true AND s.isDeleted = false")
    List<UserProfileSpecialization> findPrimarySpecializations(@Param("profile") UserProfile profile);
}




