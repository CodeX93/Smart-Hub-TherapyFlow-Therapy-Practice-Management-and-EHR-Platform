package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.entity.UserProfileEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface UserProfileEducationRepository extends JpaRepository<UserProfileEducation, Long> {

    List<UserProfileEducation> findByUserProfile(UserProfile userProfile);

    List<UserProfileEducation> findByUserProfileAndIsAccredited(UserProfile userProfile, Boolean isAccredited);
}




