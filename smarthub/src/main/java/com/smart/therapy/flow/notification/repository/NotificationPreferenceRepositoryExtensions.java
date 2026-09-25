package com.smart.therapy.flow.notification.repository;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Extended repository methods for NotificationPreference entity with client support.
 */
@Repository
@TenantScoped
public interface NotificationPreferenceRepositoryExtensions extends JpaRepository<NotificationPreference, Long> {

    // ========== Client Preference Queries ==========

    /**
     * Find all preferences for a client
     */
    @Query("SELECT np FROM NotificationPreference np WHERE np.client = :client")
    List<NotificationPreference> findByClientId(@Param("client") Client client);

    /**
     * Find preference for a client by notification type
     */
    @Query("SELECT np FROM NotificationPreference np WHERE np.client = :client AND np.notificationType = :notificationType")
    Optional<NotificationPreference> findByClientIdAndNotificationType(
            @Param("client") Client client,
            @Param("notificationType") String notificationType
    );

    // ========== User Preference Queries (Existing, for reference) ==========

    /**
     * Find all preferences for a user
     */
    @Query("SELECT np FROM NotificationPreference np WHERE np.user = :user")
    List<NotificationPreference> findByUserId(@Param("user") User user);

    /**
     * Find preference for a user by notification type
     */
    @Query("SELECT np FROM NotificationPreference np WHERE np.user = :user AND np.notificationType = :notificationType")
    Optional<NotificationPreference> findByUserIdAndNotificationType(
            @Param("user") User user,
            @Param("notificationType") String notificationType
    );
}

