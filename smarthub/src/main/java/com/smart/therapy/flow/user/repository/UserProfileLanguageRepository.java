package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.entity.UserProfileLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface UserProfileLanguageRepository extends JpaRepository<UserProfileLanguage, Long> {

    List<UserProfileLanguage> findByUserProfile(UserProfile userProfile);

    List<UserProfileLanguage> findByUserProfileAndIsVerified(UserProfile userProfile, Boolean isVerified);

    @Query("SELECT l FROM UserProfileLanguage l WHERE l.language = :language " +
            "AND l.proficiencyLevel IN ('FLUENT', 'NATIVE', 'MEDICAL_FLUENT') " +
            "AND l.isVerified = true AND l.isDeleted = false")
    List<UserProfileLanguage> findClinicallyQualifiedByLanguage(@Param("language") String language);

    @Query("SELECT DISTINCT l.userProfile FROM UserProfileLanguage l " +
            "WHERE l.language = :language AND l.isVerified = true AND l.isDeleted = false")
    List<UserProfile> findUserProfilesByLanguage(@Param("language") String language);
}




