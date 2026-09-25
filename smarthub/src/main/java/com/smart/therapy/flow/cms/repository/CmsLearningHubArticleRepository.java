package com.smart.therapy.flow.cms.repository;

import com.smart.therapy.flow.cms.entity.CmsLearningHubArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CmsLearningHubArticleRepository extends JpaRepository<CmsLearningHubArticle, Long> {

    Optional<CmsLearningHubArticle> findBySlug(String slug);

    List<CmsLearningHubArticle> findByIsPublishedTrueOrderBySortOrderAscIdAsc();

    Optional<CmsLearningHubArticle> findBySlugAndIsPublishedTrue(String slug);

    List<CmsLearningHubArticle> findAllByOrderBySortOrderAscIdAsc();

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsBySlug(String slug);
}
