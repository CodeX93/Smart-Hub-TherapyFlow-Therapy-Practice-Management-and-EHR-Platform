package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.UserContact;
import com.smart.therapy.flow.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@TenantScoped
public interface UserContactRepository extends JpaRepository<UserContact, Long> {

    @Query("SELECT uc FROM UserContact uc WHERE uc.userProfile = :profile AND uc.type = :type")
    Optional<UserContact> findByUserProfileAndType(@Param("profile") UserProfile profile, @Param("type") String type);

    @Query("SELECT uc FROM UserContact uc WHERE uc.userProfile.id = :profileId AND uc.type = :type")
    Optional<UserContact> findByUserProfileIdAndType(@Param("profileId") Long profileId, @Param("type") String type);
}




