package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.entity.UserProfileTreatmentApproach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface UserProfileTreatmentApproachRepository extends JpaRepository<UserProfileTreatmentApproach, Long> {

    List<UserProfileTreatmentApproach> findByUserProfile(UserProfile userProfile);

    List<UserProfileTreatmentApproach> findByApproach(String approach);

    List<UserProfileTreatmentApproach> findByUserProfileAndIsPrimary(UserProfile userProfile, Boolean isPrimary);
}




