package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.entity.UserProfileAgeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface UserProfileAgeGroupRepository extends JpaRepository<UserProfileAgeGroup, Long> {

    List<UserProfileAgeGroup> findByUserProfile(UserProfile userProfile);

    List<UserProfileAgeGroup> findByAgeGroup(String ageGroup);
}




