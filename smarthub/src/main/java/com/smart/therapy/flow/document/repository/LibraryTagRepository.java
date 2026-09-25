package com.smart.therapy.flow.document.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.document.entity.LibraryTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface LibraryTagRepository extends JpaRepository<LibraryTag, Long> {
    
    /**
     * Find a tag by its exact name (case-sensitive)
     */
    Optional<LibraryTag> findByName(String name);
    
    /**
     * Check if a tag with the given name exists
     */
    boolean existsByName(String name);
    
    /**
     * Get all tags ordered by name
     */
    List<LibraryTag> findAllByOrderByNameAsc();
    
    /**
     * Find tag by name ignoring case
     */
    Optional<LibraryTag> findByNameIgnoreCase(String name);
}




