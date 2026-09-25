package com.smart.therapy.flow.document.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.document.entity.LibraryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface LibraryEntryRepository extends JpaRepository<LibraryEntry, Long> {
    List<LibraryEntry> findByCategoryId(Long categoryId);
}





