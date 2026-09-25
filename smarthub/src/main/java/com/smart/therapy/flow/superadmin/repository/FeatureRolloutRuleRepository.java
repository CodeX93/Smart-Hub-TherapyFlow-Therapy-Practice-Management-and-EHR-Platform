package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.entity.FeatureRolloutRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureRolloutRuleRepository extends JpaRepository<FeatureRolloutRule, Long> {

    @Query("""
            SELECT r
            FROM FeatureRolloutRule r
            WHERE r.organisation.id = :orgId
              AND r.featureKey = :featureKey
              AND r.scope = :scope
              AND ((:targetId IS NULL AND r.targetId IS NULL) OR r.targetId = :targetId)
              AND r.startAt <= :at
              AND (r.endAt IS NULL OR r.endAt > :at)
            ORDER BY r.startAt DESC
            """)
    List<FeatureRolloutRule> findActiveRules(
            @Param("orgId") Long orgId,
            @Param("scope") FeatureRolloutRule.Scope scope,
            @Param("targetId") Long targetId,
            @Param("featureKey") String featureKey,
            @Param("at") Instant at
    );

    @Query("""
            SELECT r
            FROM FeatureRolloutRule r
            WHERE r.organisation.id = :orgId
              AND r.featureKey = :featureKey
              AND r.scope = :scope
              AND ((:targetKey IS NULL AND r.targetKey IS NULL) OR r.targetKey = :targetKey)
              AND r.startAt <= :at
              AND (r.endAt IS NULL OR r.endAt > :at)
            ORDER BY r.startAt DESC
            """)
    List<FeatureRolloutRule> findActiveRulesByTargetKey(
            @Param("orgId") Long orgId,
            @Param("scope") FeatureRolloutRule.Scope scope,
            @Param("targetKey") String targetKey,
            @Param("featureKey") String featureKey,
            @Param("at") Instant at
    );

    @Query("""
            SELECT r
            FROM FeatureRolloutRule r
            WHERE r.scope = :scope
              AND r.featureKey = :featureKey
              AND r.startAt <= :at
              AND (r.endAt IS NULL OR r.endAt > :at)
            ORDER BY r.startAt DESC
            """)
    List<FeatureRolloutRule> findActiveGlobalRules(
            @Param("scope") FeatureRolloutRule.Scope scope,
            @Param("featureKey") String featureKey,
            @Param("at") Instant at
    );

    default Optional<FeatureRolloutRule> findLatestActiveRule(
            Long orgId,
            FeatureRolloutRule.Scope scope,
            Long targetId,
            String featureKey,
            Instant at
    ) {
        List<FeatureRolloutRule> rows = findActiveRules(orgId, scope, targetId, featureKey, at);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    default Optional<FeatureRolloutRule> findLatestActiveRuleByTargetKey(
            Long orgId,
            FeatureRolloutRule.Scope scope,
            String targetKey,
            String featureKey,
            Instant at
    ) {
        List<FeatureRolloutRule> rows = findActiveRulesByTargetKey(orgId, scope, targetKey, featureKey, at);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    default Optional<FeatureRolloutRule> findLatestActiveGlobalRule(
            FeatureRolloutRule.Scope scope,
            String featureKey,
            Instant at
    ) {
        List<FeatureRolloutRule> rows = findActiveGlobalRules(scope, featureKey, at);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    @Query("""
            SELECT r
            FROM FeatureRolloutRule r
            WHERE r.organisation.id = :orgId
            ORDER BY r.scope ASC, r.featureKey ASC, r.startAt DESC
            """)
    List<FeatureRolloutRule> findAllByOrganisationId(@Param("orgId") Long orgId);

    @Query("""
            SELECT r
            FROM FeatureRolloutRule r
            WHERE r.scope = :scope
            ORDER BY r.featureKey ASC, r.startAt DESC
            """)
    List<FeatureRolloutRule> findAllByScope(@Param("scope") FeatureRolloutRule.Scope scope);

    Optional<FeatureRolloutRule> findByIdAndScope(Long id, FeatureRolloutRule.Scope scope);

    List<FeatureRolloutRule> findByScopeAndFeatureKey(FeatureRolloutRule.Scope scope, String featureKey);

    long deleteByScopeAndFeatureKey(FeatureRolloutRule.Scope scope, String featureKey);

    Optional<FeatureRolloutRule> findByIdAndOrganisationId(Long id, Long organisationId);
}
