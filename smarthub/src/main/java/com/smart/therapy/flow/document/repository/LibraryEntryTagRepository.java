package com.smart.therapy.flow.document.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.document.entity.LibraryEntry;
import com.smart.therapy.flow.document.entity.LibraryEntryTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface LibraryEntryTagRepository extends JpaRepository<LibraryEntryTag, LibraryEntryTag.LibraryEntryTagId> {
    
    /**
     * Find all tags for a specific library entry
     */
    List<LibraryEntryTag> findByLibraryEntryId(Long libraryEntryId);
    
    /**
     * Delete all tags for a specific library entry
     */
    @Modifying
    @Query("DELETE FROM LibraryEntryTag et WHERE et.libraryEntry.id = :entryId")
    void deleteByLibraryEntryId(@Param("entryId") Long entryId);
    
    /**
     * Delete all tags for a specific library entry object
     */
    void deleteByLibraryEntry(LibraryEntry entry);
    
    /**
     * Count how many entries use a specific tag
     */
    long countByTagId(Long tagId);
}




