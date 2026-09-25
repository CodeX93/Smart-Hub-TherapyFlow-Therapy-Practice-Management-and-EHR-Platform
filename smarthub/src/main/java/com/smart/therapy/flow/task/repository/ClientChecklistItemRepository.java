package com.smart.therapy.flow.task.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.task.entity.ClientChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface ClientChecklistItemRepository extends JpaRepository<ClientChecklistItem, Long> {

    @Query("""
            SELECT i FROM ClientChecklistItem i
            LEFT JOIN i.checklistItem ci
            WHERE i.clientChecklist.id = :checklistId
            ORDER BY ci.sortOrder ASC, ci.itemOrder ASC, i.id ASC
            """)
    List<ClientChecklistItem> findByClientChecklistIdOrderByChecklistItemSortOrderAsc(
            @Param("checklistId") Long clientChecklistId);

    void deleteByClientChecklistId(Long clientChecklistId);
}
