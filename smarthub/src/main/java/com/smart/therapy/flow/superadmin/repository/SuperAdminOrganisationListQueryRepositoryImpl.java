package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.dto.SuperAdminOrganisationListItem;
import com.smart.therapy.flow.superadmin.dto.SuperAdminOrganisationListRequest;
import com.smart.therapy.flow.superadmin.dto.SuperAdminOrganisationListResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class SuperAdminOrganisationListQueryRepositoryImpl implements SuperAdminOrganisationListQueryRepository {

    private static final Map<String, String> SORT_SQL = Map.of(
            "name", "o.name",
            "createdat", "o.created_at",
            "userscount", "COALESCE(uc.users_count, 0)",
            "plan", "COALESCE(ps.plan_name, LOWER(o.organisation_type))"
    );

    private static final Map<String, String> EXTERNAL_TO_INTERNAL_STATUS = Map.of(
            "active", "ACTIVE",
            "suspended", "LOCKED",
            "termination_scheduled", "ARCHIVED",
            "terminated", "DELETED"
    );

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public SuperAdminOrganisationListResult search(SuperAdminOrganisationListRequest req) {
        String normalizedSort = normalizeOrDefault(req.getSort(), "createdat");
        String sortSql = SORT_SQL.getOrDefault(normalizedSort, "o.created_at");
        String order = "asc".equals(normalizeOrDefault(req.getOrder(), "desc")) ? "ASC" : "DESC";
        boolean pendingActivationFilter = StringUtils.hasText(req.getStatus())
                && "pending_activation".equals(normalizeStatusKey(req.getStatus()));
        boolean planFilter = StringUtils.hasText(req.getPlan());

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (StringUtils.hasText(req.getSearch())) {
            where.append(" AND (LOWER(o.name) LIKE ? OR LOWER(o.slug) LIKE ?) ");
            String like = "%" + req.getSearch().trim().toLowerCase(Locale.ROOT) + "%";
            params.add(like);
            params.add(like);
        }
        if (StringUtils.hasText(req.getStatus())) {
            String status = normalizeStatusKey(req.getStatus());
            if ("pending_activation".equals(status)) {
                where.append(" AND (tsv.id IS NULL OR UPPER(tsv.status) = 'PENDING') ");
            } else {
                String internal = EXTERNAL_TO_INTERNAL_STATUS.get(status);
                if (internal == null) {
                    where.append(" AND 1=0 ");
                } else {
                    where.append(" AND UPPER(o.status) = ? ");
                    params.add(internal);
                }
            }
        }
        if (StringUtils.hasText(req.getPlan())) {
            where.append(" AND LOWER(COALESCE(ps.plan_name, LOWER(o.organisation_type))) = ? ");
            params.add(req.getPlan().trim().toLowerCase(Locale.ROOT));
        }
        if (req.getCreatedFrom() != null) {
            params.add(Timestamp.from(req.getCreatedFrom().atStartOfDay().toInstant(ZoneOffset.UTC)));
            where.append(" AND o.created_at >= ? ");
        }
        if (req.getCreatedTo() != null) {
            params.add(Timestamp.from(req.getCreatedTo().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
            where.append(" AND o.created_at < ? ");
        }
        if (StringUtils.hasText(req.getRegion())) {
            where.append(" AND LOWER(COALESCE(o.region,'')) = ? ");
            params.add(req.getRegion().trim().toLowerCase(Locale.ROOT));
        }
        if (StringUtils.hasText(req.getDataResidency())) {
            where.append(" AND LOWER(COALESCE(o.data_residency,'')) = ? ");
            params.add(req.getDataResidency().trim().toLowerCase(Locale.ROOT));
        }

        String latestSchemaVersionJoin = """
                LEFT JOIN (
                    SELECT DISTINCT ON (tsv.organisation_id)
                        tsv.organisation_id,
                        tsv.id,
                        tsv.status
                    FROM public.tenant_schema_versions tsv
                    ORDER BY tsv.organisation_id, tsv.version DESC NULLS LAST, tsv.id DESC
                ) tsv ON tsv.organisation_id = o.id
                """;
        String currentPlanJoin = """
                LEFT JOIN (
                    SELECT DISTINCT ON (os.organisation_id)
                        os.organisation_id,
                        LOWER(sp.name) AS plan_name
                    FROM public.org_subscriptions os
                    JOIN public.subscription_plans sp ON sp.id = os.plan_id
                    WHERE os.end_at IS NULL
                    ORDER BY os.organisation_id, os.start_at DESC
                ) ps ON ps.organisation_id = o.id
                """;
        String userCountJoin = """
                LEFT JOIN (
                    SELECT
                        uo.organisation_id,
                        COUNT(*)::bigint AS users_count
                    FROM public.user_organisations uo
                    GROUP BY uo.organisation_id
                ) uc ON uc.organisation_id = o.id
                """;

        StringBuilder countFromSql = new StringBuilder(" FROM public.organisations o ");
        if (pendingActivationFilter) {
            countFromSql.append(latestSchemaVersionJoin);
        }
        if (planFilter) {
            countFromSql.append(currentPlanJoin);
        }

        String countSql = "SELECT COUNT(*) " + countFromSql + where;
        Query countQuery = entityManager.createNativeQuery(countSql);
        bindParams(countQuery, params);
        Number total = (Number) countQuery.getSingleResult();

        String selectSql = """
                SELECT o.id, o.name, o.slug, o.status, o.created_at,
                       COALESCE(ps.plan_name, LOWER(o.organisation_type)) AS plan_name,
                       COALESCE(uc.users_count, 0) AS users_count,
                       o.region, o.data_residency, o.timezone,
                       CASE
                         WHEN (tsv.id IS NULL OR UPPER(tsv.status) = 'PENDING') THEN 'pending_activation'
                         WHEN UPPER(o.status) = 'ACTIVE' THEN 'active'
                         WHEN UPPER(o.status) = 'LOCKED' THEN 'suspended'
                         WHEN UPPER(o.status) = 'ARCHIVED' THEN 'termination_scheduled'
                         WHEN UPPER(o.status) = 'DELETED' THEN 'terminated'
                         ELSE LOWER(o.status)
                       END AS external_status
                FROM public.organisations o
                """ + latestSchemaVersionJoin + currentPlanJoin + userCountJoin + where
                + " ORDER BY " + sortSql + " " + order + ", o.id DESC ";

        int page = req.getPage() == null || req.getPage() < 1 ? 1 : req.getPage();
        int pageSize = req.getPageSize() == null ? 25 : Math.min(Math.max(req.getPageSize(), 1), 100);
        if (!Boolean.TRUE.equals(req.getExportCsv())) {
            selectSql += " LIMIT " + pageSize + " OFFSET " + ((page - 1) * pageSize);
        } else {
            selectSql += " LIMIT 10000";
        }

        Query dataQuery = entityManager.createNativeQuery(selectSql);
        bindParams(dataQuery, params);
        List<Object[]> rows = dataQuery.getResultList();

        List<SuperAdminOrganisationListItem> items = rows.stream().map(this::toItem).toList();
        return SuperAdminOrganisationListResult.builder()
                .items(items)
                .total(total.longValue())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    private SuperAdminOrganisationListItem toItem(Object[] row) {
        SuperAdminOrganisationListItem item = new SuperAdminOrganisationListItem();
        item.setId(((Number) row[0]).longValue());
        item.setName((String) row[1]);
        item.setSlug((String) row[2]);
        item.setStatus((String) row[10]);
        item.setCreatedAt(toInstant(row[4]));
        item.setPlan((String) row[5]);
        item.setUsersCount(((Number) row[6]).longValue());
        item.setRegion((String) row[7]);
        item.setDataResidency((String) row[8]);
        item.setTimezone((String) row[9]);
        return item;
    }

    private static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        throw new IllegalStateException("Unsupported temporal type: " + value.getClass().getName());
    }

    private static void bindParams(Query query, List<Object> params) {
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }
    }

    private static String normalizeOrDefault(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeStatusKey(String rawStatus) {
        if (!StringUtils.hasText(rawStatus)) {
            return "";
        }
        return rawStatus.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }
}
