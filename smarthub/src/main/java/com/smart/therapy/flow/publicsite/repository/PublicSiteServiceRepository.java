package com.smart.therapy.flow.publicsite.repository;

import com.smart.therapy.flow.publicsite.entity.PublicSiteService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PublicSiteServiceRepository extends JpaRepository<PublicSiteService, Long> {

    List<PublicSiteService> findByIsDeletedFalseOrderByDisplayOrderAscNameAsc();

    List<PublicSiteService> findByEnabledTrueAndIsDeletedFalseOrderByDisplayOrderAscNameAsc();

    Optional<PublicSiteService> findByIdAndIsDeletedFalse(Long id);

    Optional<PublicSiteService> findBySlugAndIsDeletedFalse(String slug);

    boolean existsBySlugAndIsDeletedFalse(String slug);
}
