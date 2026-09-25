package com.smart.therapy.flow.document.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.document.entity.LibraryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface LibraryCategoryRepository extends JpaRepository<LibraryCategory, Long> {
    Optional<LibraryCategory> findByName(String name);

    List<LibraryCategory> findByParentCategory_Id(Long parentCategoryId);
}





