package com.smart.therapy.flow.task.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.task.entity.ChecklistItem;
import com.smart.therapy.flow.task.enums.CheckListCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@TenantScoped
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
    
    List<ChecklistItem> findByTemplateIdOrderBySortOrderAsc(Long templateId);

    boolean existsByTemplateIdAndCategory(Long templateId, CheckListCategory category);
    
    void deleteByTemplateId(Long templateId);

    @Query("""
            SELECT ci.template.id, COUNT(ci)
            FROM ChecklistItem ci
            WHERE ci.template.id IN :templateIds
              AND (ci.isDeleted = false OR ci.isDeleted IS NULL)
            GROUP BY ci.template.id
            """)
    List<Object[]> countActiveItemsByTemplateIds(@Param("templateIds") Collection<Long> templateIds);
}




