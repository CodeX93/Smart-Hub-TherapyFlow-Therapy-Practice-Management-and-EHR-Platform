package com.smart.therapy.flow.organisation.repository;

import com.smart.therapy.flow.organisation.entity.Organisation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganisationRepository extends JpaRepository<Organisation, Long> {

    /**
     * Tenant orgs that still need migration work.
     * Includes orgs with no tracking row (legacy), non-success statuses, or schema version below
     * the latest tenant migration bundled with the app.
     */
    @Query(value = """
            SELECT o.*
            FROM public.organisations o
            LEFT JOIN public.tenant_schema_versions tsv ON tsv.organisation_id = o.id
            WHERE o.schema_name IS NOT NULL
              AND LOWER(o.schema_name) != 'public'
              AND COALESCE(o.is_deleted, false) = false
              AND UPPER(COALESCE(o.status, '')) NOT IN ('ARCHIVED', 'DELETED')
              AND (
                    tsv.id IS NULL
                    OR UPPER(COALESCE(tsv.status, '')) IN ('PENDING', 'FAILED', 'IN_PROGRESS')
                    OR COALESCE(NULLIF(regexp_replace(COALESCE(tsv.version, '0'), '[^0-9]', '', 'g'), ''), '0')::integer < :targetVersion
              )
              AND UPPER(COALESCE(tsv.status, '')) != 'SKIPPED'
            ORDER BY tsv.migrated_at ASC NULLS FIRST, o.id ASC
            """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM public.organisations o
                    LEFT JOIN public.tenant_schema_versions tsv ON tsv.organisation_id = o.id
                    WHERE o.schema_name IS NOT NULL
                      AND LOWER(o.schema_name) != 'public'
                      AND COALESCE(o.is_deleted, false) = false
                      AND UPPER(COALESCE(o.status, '')) NOT IN ('ARCHIVED', 'DELETED')
                      AND (
                            tsv.id IS NULL
                            OR UPPER(COALESCE(tsv.status, '')) IN ('PENDING', 'FAILED', 'IN_PROGRESS')
                            OR COALESCE(NULLIF(regexp_replace(COALESCE(tsv.version, '0'), '[^0-9]', '', 'g'), ''), '0')::integer < :targetVersion
                      )
                      AND UPPER(COALESCE(tsv.status, '')) != 'SKIPPED'
                    """,
            nativeQuery = true)
    Page<Organisation> findOrganisationsForMigration(@Param("targetVersion") int targetVersion, Pageable pageable);

    Optional<Organisation> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Organisation> findBySubdomain(String subdomain);

    boolean existsBySubdomain(String subdomain);

    Optional<Organisation> findBySchemaName(String schemaName);

    long countByIsDeletedFalse();
}
