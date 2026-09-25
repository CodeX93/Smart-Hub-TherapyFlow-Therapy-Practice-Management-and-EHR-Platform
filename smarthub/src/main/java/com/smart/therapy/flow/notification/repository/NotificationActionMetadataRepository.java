package com.smart.therapy.flow.notification.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.notification.entity.NotificationActionMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface NotificationActionMetadataRepository extends JpaRepository<NotificationActionMetadata, Long> {
    List<NotificationActionMetadata> findByIsDeletedFalseAndIsActiveTrueOrderBySortOrderAscRelatedEntityTypeAsc();
    Optional<NotificationActionMetadata> findByRelatedEntityTypeIgnoreCaseAndIsDeletedFalse(String relatedEntityType);
}

