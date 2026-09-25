package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.entity.UserProfilePublication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface UserProfilePublicationRepository extends JpaRepository<UserProfilePublication, Long> {

    List<UserProfilePublication> findByUserProfile(UserProfile userProfile);

    List<UserProfilePublication> findByPublicationType(String publicationType);
}




