package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@TenantScoped
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(Long userId);

    @org.springframework.data.jpa.repository.Query("""
            SELECT DISTINCT p FROM UserProfile p
            LEFT JOIN FETCH p.virtualRoom
            LEFT JOIN FETCH p.availablePhysicalRooms pr
            LEFT JOIN FETCH pr.room
            WHERE p.user.id = :userId
            """)
    Optional<UserProfile> findByUserIdWithRooms(
            @org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM UserProfile p WHERE p.user.id IN :userIds")
    java.util.List<UserProfile> findByUserIdIn(
            @org.springframework.data.repository.query.Param("userIds") java.util.Collection<Long> userIds);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM UserProfile p WHERE p.virtualRoom.id = :roomId")
    long countByVirtualRoomId(@org.springframework.data.repository.query.Param("roomId") Long roomId);
}




