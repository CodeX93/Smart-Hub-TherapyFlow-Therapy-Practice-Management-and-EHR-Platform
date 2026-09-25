package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.entity.UserProfileCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@TenantScoped
public interface UserProfileCertificationRepository extends JpaRepository<UserProfileCertification, Long> {

    List<UserProfileCertification> findByUserProfile(UserProfile userProfile);

    List<UserProfileCertification> findByUserProfileAndStatus(
            UserProfile userProfile,
            UserProfileCertification.CertificationStatus status);

    // Find expiring certifications (within next N days)
    @Query("SELECT c FROM UserProfileCertification c " +
            "WHERE c.expiryDate BETWEEN :now AND :futureDate " +
            "AND c.status = 'VERIFIED' AND c.isDeleted = false")
    List<UserProfileCertification> findExpiringCertifications(
            @Param("now") LocalDate now,
            @Param("futureDate") LocalDate futureDate);

    // Find active certifications for a user profile
    @Query("SELECT c FROM UserProfileCertification c " +
            "WHERE c.userProfile = :profile AND c.status = 'VERIFIED' " +
            "AND (c.expiryDate IS NULL OR c.expiryDate > :now) " +
            "AND c.isDeleted = false")
    List<UserProfileCertification> findActiveCertifications(
            @Param("profile") UserProfile profile,
            @Param("now") LocalDate now);
}




