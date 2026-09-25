package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.entity.UserProfilePhysicalRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface UserProfilePhysicalRoomRepository extends JpaRepository<UserProfilePhysicalRoom, Long> {

    List<UserProfilePhysicalRoom> findByUserProfile(UserProfile userProfile);

    List<UserProfilePhysicalRoom> findByUserProfileAndIsPrimary(UserProfile userProfile, Boolean isPrimary);

    @Query("SELECT COUNT(p) FROM UserProfilePhysicalRoom p WHERE p.room.id = :roomId")
    long countByRoomId(@Param("roomId") Long roomId);
}




