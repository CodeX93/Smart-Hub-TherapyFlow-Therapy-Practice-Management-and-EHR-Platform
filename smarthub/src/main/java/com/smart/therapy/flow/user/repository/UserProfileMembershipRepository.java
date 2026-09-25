package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.entity.UserProfileMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface UserProfileMembershipRepository extends JpaRepository<UserProfileMembership, Long> {

    List<UserProfileMembership> findByUserProfile(UserProfile userProfile);

    List<UserProfileMembership> findByUserProfileAndStatus(
            UserProfile userProfile,
            UserProfileMembership.MembershipStatus status);
}




