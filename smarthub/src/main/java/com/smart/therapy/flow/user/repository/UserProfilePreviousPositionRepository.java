package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.entity.UserProfilePreviousPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface UserProfilePreviousPositionRepository extends JpaRepository<UserProfilePreviousPosition, Long> {

    List<UserProfilePreviousPosition> findByUserProfile(UserProfile userProfile);

    List<UserProfilePreviousPosition> findByUserProfileAndIsCurrent(UserProfile userProfile, Boolean isCurrent);
}




