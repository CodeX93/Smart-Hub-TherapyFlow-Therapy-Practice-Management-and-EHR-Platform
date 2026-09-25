package com.smart.therapy.flow.client.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.client.entity.ClientHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface ClientHistoryRepository extends JpaRepository<ClientHistory, Long> {
    @Query("SELECT h FROM ClientHistory h WHERE h.client.id = :clientId ORDER BY h.createdAt DESC")
    List<ClientHistory> findByClientIdOrderByCreatedAtDesc(@Param("clientId") Long clientId);

    @Query("SELECT h FROM ClientHistory h WHERE h.client.id = :clientId AND h.eventType = :eventType ORDER BY h.createdAt DESC")
    List<ClientHistory> findByClientIdAndEventType(@Param("clientId") Long clientId, @Param("eventType") String eventType);
}





