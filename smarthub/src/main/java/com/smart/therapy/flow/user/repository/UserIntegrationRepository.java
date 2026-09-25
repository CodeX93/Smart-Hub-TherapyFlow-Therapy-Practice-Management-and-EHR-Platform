package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.user.entity.UserIntegration;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface UserIntegrationRepository extends JpaRepository<UserIntegration, Long> {

    /**
     * Find integration by user and integration type
     */
    @Query("SELECT ui FROM UserIntegration ui WHERE ui.user = :user AND ui.integrationType = :integrationType AND ui.isDeleted = false")
    Optional<UserIntegration> findByUserAndIntegrationType(@Param("user") User user, @Param("integrationType") String integrationType);

    /**
     * Find all integrations for a user
     */
    @Query("SELECT ui FROM UserIntegration ui WHERE ui.user = :user AND ui.isDeleted = false")
    List<UserIntegration> findByUser(@Param("user") User user);

    /**
     * Find all active integrations for a user
     */
    @Query("SELECT ui FROM UserIntegration ui WHERE ui.user = :user AND ui.isActive = true AND ui.isDeleted = false")
    List<UserIntegration> findByUserAndIsActiveTrue(@Param("user") User user);

    /**
     * Find integration by external user ID (e.g., Zoom account ID)
     */
    @Query("SELECT ui FROM UserIntegration ui WHERE ui.externalUserId = :externalUserId AND ui.isDeleted = false")
    Optional<UserIntegration> findByExternalUserId(@Param("externalUserId") String externalUserId);

    /**
     * Find by user ID and integration type
     */
    @Query("SELECT ui FROM UserIntegration ui WHERE ui.user.id = :userId AND ui.integrationType = :integrationType AND ui.isDeleted = false")
    Optional<UserIntegration> findByUserIdAndIntegrationType(@Param("userId") Long userId, @Param("integrationType") String integrationType);

    /**
     * Check if integration exists
     */
    @Query("SELECT CASE WHEN COUNT(ui) > 0 THEN true ELSE false END FROM UserIntegration ui WHERE ui.user = :user AND ui.integrationType = :integrationType AND ui.isDeleted = false")
    boolean existsByUserAndIntegrationType(@Param("user") User user, @Param("integrationType") String integrationType);

    /**
     * Hard-delete all integrations by user and type (handles accidental duplicates).
     */
    @Modifying
    @Query("DELETE FROM UserIntegration ui WHERE ui.user = :user AND ui.integrationType = :integrationType")
    int deleteAllByUserAndIntegrationType(@Param("user") User user, @Param("integrationType") String integrationType);
}




