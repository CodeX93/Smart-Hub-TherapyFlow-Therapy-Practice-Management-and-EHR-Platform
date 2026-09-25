package com.smart.therapy.flow.document.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.document.entity.LibraryEntry;
import com.smart.therapy.flow.document.entity.LibraryEntryConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@TenantScoped
public interface LibraryEntryConnectionRepository extends JpaRepository<LibraryEntryConnection, Long> {
    // Find by the associated LibraryEntry entity
    List<LibraryEntryConnection> findByFromEntry(LibraryEntry fromEntry);

    List<LibraryEntryConnection> findByToEntry(LibraryEntry toEntry);

    // Optional: If you want to find by id of the related entry
    List<LibraryEntryConnection> findByFromEntry_Id(Long fromEntryId);

    List<LibraryEntryConnection> findByToEntry_Id(Long toEntryId);

    boolean existsByFromEntry_IdAndToEntry_Id(Long fromEntryId, Long toEntryId);

    List<LibraryEntryConnection> findByFromEntry_IdInOrToEntry_IdIn(Collection<Long> fromEntryIds, Collection<Long> toEntryIds);
}





