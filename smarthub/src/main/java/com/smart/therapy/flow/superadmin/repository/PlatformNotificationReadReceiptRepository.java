package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.entity.PlatformNotificationReadReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformNotificationReadReceiptRepository extends JpaRepository<PlatformNotificationReadReceipt, Long> {
    Optional<PlatformNotificationReadReceipt> findByAuthIdAndNotificationJob_Id(Long authId, Long notificationJobId);

    List<PlatformNotificationReadReceipt> findByAuthIdAndNotificationJob_IdIn(Long authId, Collection<Long> notificationJobIds);

    long countByAuthIdAndNotificationJob_IdIn(Long authId, Collection<Long> notificationJobIds);
}
