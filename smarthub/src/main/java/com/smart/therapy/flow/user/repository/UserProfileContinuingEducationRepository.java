package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.entity.UserProfileContinuingEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface UserProfileContinuingEducationRepository extends JpaRepository<UserProfileContinuingEducation, Long> {

    List<UserProfileContinuingEducation> findByUserProfile(UserProfile userProfile);

    @Query("SELECT SUM(ce.ceCredits) FROM UserProfileContinuingEducation ce " +
            "WHERE ce.userProfile = :profile AND ce.isDeleted = false")
    Double getTotalCECredits(@Param("profile") UserProfile profile);
}




