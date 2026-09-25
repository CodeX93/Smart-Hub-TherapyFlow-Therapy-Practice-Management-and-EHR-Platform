package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.entity.UserProfileWorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface UserProfileWorkingHoursRepository extends JpaRepository<UserProfileWorkingHours, Long> {

    List<UserProfileWorkingHours> findByUserProfile(UserProfile userProfile);

    List<UserProfileWorkingHours> findByUserProfileAndDay(UserProfile userProfile, String day);

    void deleteByUserProfile(UserProfile userProfile);
}




