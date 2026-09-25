package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.entity.UserProfileReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface UserProfileReferenceRepository extends JpaRepository<UserProfileReference, Long> {

    List<UserProfileReference> findByUserProfile(UserProfile userProfile);

    List<UserProfileReference> findByReferenceType(String referenceType);
}




