package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.AppFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppFeatureRepository extends JpaRepository<AppFeature, Long> {

    Optional<AppFeature> findByCode(String code);

    Optional<AppFeature> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
            UPDATE AppFeature f
            SET f.defaultEnabled = false
            WHERE f.isDeleted = false
              AND f.isDeprecated = false
            """)
    int disableAllActiveFeatures();

    long countByIsDeletedFalseAndIsDeprecatedFalse();
}
