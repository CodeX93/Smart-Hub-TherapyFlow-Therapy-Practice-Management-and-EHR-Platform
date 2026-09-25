package com.smart.therapy.flow.notification.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.notification.entity.NotificationPreference;
import com.smart.therapy.flow.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    List<NotificationPreference> findByUserId(Long userId);
    Optional<NotificationPreference> findByUserIdAndNotificationType(Long userId, NotificationType notificationType);

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
            @Param("notificationType") NotificationType notificationType
    );
}





