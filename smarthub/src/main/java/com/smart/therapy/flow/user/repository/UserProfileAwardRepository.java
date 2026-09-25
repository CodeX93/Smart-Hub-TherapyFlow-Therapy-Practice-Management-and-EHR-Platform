package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.entity.UserProfileAward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface UserProfileAwardRepository extends JpaRepository<UserProfileAward, Long> {

    List<UserProfileAward> findByUserProfile(UserProfile userProfile);
}




