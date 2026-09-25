package com.smart.therapy.flow.client.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.client.entity.ClientPortalSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface ClientPortalSettingsRepository extends JpaRepository<ClientPortalSettings, Long> {

    @Query("SELECT s FROM ClientPortalSettings s WHERE s.client.id = :clientId")
    Optional<ClientPortalSettings> findByClientId(@Param("clientId") Long clientId);

    @Query("SELECT s FROM ClientPortalSettings s WHERE s.client.id = :clientId AND s.hasPortalAccess = true AND s.isActivated = true")
    Optional<ClientPortalSettings> findActiveByClientId(@Param("clientId") Long clientId);

    /**
     * Requests still owed an email: asked at some point, and either never included in a digest
     * or asked again since the last one.
     */
    @Query("SELECT s FROM ClientPortalSettings s "
            + "JOIN FETCH s.client c "
            + "JOIN FETCH c.assignedTherapist "
            + "WHERE s.onlineBookingRequestedAt IS NOT NULL "
            + "AND c.assignedTherapist IS NOT NULL "
            + "AND (s.onlineBookingNotifiedAt IS NULL OR s.onlineBookingNotifiedAt < s.onlineBookingRequestedAt) "
            + "ORDER BY s.onlineBookingRequestedAt ASC")
    List<ClientPortalSettings> findPendingOnlineBookingRequests();

    /** Last digest sent to this therapist, so the job can hold to one email a day. */
    @Query("SELECT MAX(s.onlineBookingNotifiedAt) FROM ClientPortalSettings s "
            + "WHERE s.client.assignedTherapist.id = :therapistId")
    java.time.Instant findLastDigestSentAt(@Param("therapistId") Long therapistId);
}




