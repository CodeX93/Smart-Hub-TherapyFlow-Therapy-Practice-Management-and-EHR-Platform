package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.entity.OrgStripeAccount;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrgStripeAccountRepository;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.subscription.feature.CoreFeature;
import com.smart.therapy.flow.superadmin.dto.SuperAdminDashboardSummaryResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminDashboardUsageMetricResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminKpiResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminOrganisationDashboardResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminOrganisationStatusCountResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminTopTenantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import static com.smart.therapy.flow.common.config.AnalyticsDataSourceConfig.ANALYTICS_JDBC_TEMPLATE;

@Service
@RequiredArgsConstructor
public class SuperAdminKpiService {

    private static final int REPLICA_LAG_THRESHOLD_SECONDS = 30;

    private final OrganisationRepository organisationRepository;
    private final OrgStripeAccountRepository orgStripeAccountRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Qualifier(ANALYTICS_JDBC_TEMPLATE)
    private final ObjectProvider<NamedParameterJdbcTemplate> analyticsJdbcTemplateProvider;

    @Transactional(readOnly = true)
    public boolean isReplicaLagged() {
        NamedParameterJdbcTemplate analyticsTemplate = resolveAnalyticsTemplate().orElse(null);
        if (analyticsTemplate == null) {
            return false;
        }
        try {
            Long lagSeconds = analyticsTemplate.getJdbcTemplate().queryForObject(
                    "SELECT EXTRACT(EPOCH FROM (now() - pg_last_xact_replay_timestamp()))::bigint",
                    Long.class
            );
            if (lagSeconds == null) {
                return false;
            }
            return lagSeconds > REPLICA_LAG_THRESHOLD_SECONDS;
        } catch (Exception ex) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "superAdminKpis", key = "T(java.util.Objects).hash(#period,#orgId,#timezone)")
    public SuperAdminKpiResponse getKpis(String period, Long orgId, String timezone) {
        QueryWindow window = QueryWindow.from(period, timezone);
        Organisation org = requireOrganisation(orgId);
        MapSqlParameterSource params = baseParams(window, orgId);
        NamedParameterJdbcTemplate template = resolveAnalyticsTemplate().orElse(namedParameterJdbcTemplate);

        long activeUsers = queryCount(template,
                "SELECT COUNT(DISTINCT ai.id) " +
                        "FROM public.auth_identities ai " +
                        "LEFT JOIN public.user_organisations uo ON uo.auth_id = ai.id " +
                        "WHERE UPPER(ai.identity_type) = 'STAFF' " +
                        "AND ai.is_active = true " +
                        "AND ( " +
                        "   (ai.last_successful_login >= :from AND ai.last_successful_login < :to) " +
                        "   OR EXISTS ( " +
                        "       SELECT 1 FROM public.auth_sessions s " +
                        "       WHERE s.auth_id = ai.id " +
                        "       AND s.issued_at >= :from AND s.issued_at < :to " +
                        "       AND COALESCE(s.revoked, false) = false " +
                        "   ) " +
                        ") " +
                        (orgId != null
                                ? "AND (ai.organisation_id = :orgId OR uo.organisation_id = :orgId) "
                                : ""),
                params
        );

        long activeClients = queryCount(template,
                "SELECT COUNT(DISTINCT ai.id) " +
                        "FROM public.auth_identities ai " +
                        "LEFT JOIN public.user_organisations uo ON uo.auth_id = ai.id " +
                        "WHERE UPPER(ai.identity_type) = 'CLIENT' " +
                        "AND ai.is_active = true " +
                        "AND ai.last_successful_login >= :from AND ai.last_successful_login < :to " +
                        (orgId != null
                                ? "AND (ai.organisation_id = :orgId OR uo.organisation_id = :orgId) "
                                : ""),
                params
        );

        long sessionsCompleted = queryCount(template,
                "SELECT COALESCE(SUM(fu.usage_count), 0) " +
                        "FROM public.feature_usage fu " +
                        "JOIN public.app_features af ON af.id = fu.feature_id " +
                        "JOIN public.org_subscriptions os ON os.id = fu.subscription_id " +
                        "WHERE af.code = :featureCode " +
                        "AND fu.period_start >= :from AND fu.period_start < :to " +
                        (orgId != null ? "AND os.organisation_id = :orgId " : ""),
                params
        );

        BigDecimal revenue = queryDecimal(template,
                "SELECT COALESCE(SUM(i.amount), 0) " +
                        "FROM public.invoices i " +
                        "JOIN public.org_subscriptions os ON os.id = i.subscription_id " +
                        "WHERE UPPER(i.status) = 'PAID' " +
                        "AND i.paid_at >= :from AND i.paid_at < :to " +
                        (orgId != null ? "AND os.organisation_id = :orgId " : ""),
                params
        );

        SuperAdminKpiResponse response = new SuperAdminKpiResponse();
        response.setPeriod(window.period().code());
        response.setOrgId(orgId);
        response.setOrgName(org != null ? org.getName() : null);
        response.setCurrency(resolveCurrency(orgId));
        response.setTimezone(window.zone().getId());
        response.setFrom(window.from());
        response.setTo(window.to());
        response.setActiveUsers(activeUsers);
        response.setActiveClients(activeClients);
        response.setSessionsCompleted(sessionsCompleted);
        response.setMonthlyRevenueUsd(revenue);
        response.setGeneratedAt(Instant.now());
        return response;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "superAdminDashboard", key = "T(java.util.Objects).hash(#period,#orgId,#timezone)")
    public SuperAdminDashboardSummaryResponse getDashboardSummary(String period, Long orgId, String timezone) {
        QueryWindow window = QueryWindow.from(period, timezone);
        Organisation org = requireOrganisation(orgId);
        MapSqlParameterSource params = baseParams(window, orgId)
                .addValue("fromDate", java.sql.Date.valueOf(window.startDate()), Types.DATE)
                .addValue("toDate", java.sql.Date.valueOf(window.endDate()), Types.DATE);

        NamedParameterJdbcTemplate template = resolveAnalyticsTemplate().orElse(namedParameterJdbcTemplate);

        long delinquentOrgs = queryCount(template,
                "SELECT COUNT(DISTINCT os.organisation_id) " +
                        "FROM public.org_subscriptions os " +
                        "WHERE os.end_at IS NULL " +
                        "AND LOWER(os.status) IN ('past_due', 'failed') " +
                        (orgId != null ? "AND os.organisation_id = :orgId " : ""),
                params
        );

        long delinquentInvoices = queryCount(template,
                "SELECT COUNT(*) " +
                        "FROM public.invoices i " +
                        "JOIN public.org_subscriptions os ON os.id = i.subscription_id " +
                        "WHERE UPPER(i.status) IN ('PAST_DUE', 'FAILED') " +
                        "AND i.due_date >= :fromDate AND i.due_date < :toDate " +
                        (orgId != null ? "AND os.organisation_id = :orgId " : ""),
                params
        );

        List<SuperAdminDashboardUsageMetricResponse> usage = template.query(
                "SELECT af.code, COALESCE(SUM(fu.usage_count), 0) AS used " +
                        "FROM public.feature_usage fu " +
                        "JOIN public.app_features af ON af.id = fu.feature_id " +
                        "JOIN public.org_subscriptions os ON os.id = fu.subscription_id " +
                        "WHERE fu.period_start >= :from AND fu.period_start < :to " +
                        (orgId != null ? "AND os.organisation_id = :orgId " : "") +
                        "GROUP BY af.code " +
                        "ORDER BY af.code",
                params,
                (rs, idx) -> new SuperAdminDashboardUsageMetricResponse(
                        rs.getString("code"),
                        rs.getLong("used")
                )
        );

        long totalOrganisations = orgId == null
                ? organisationRepository.countByIsDeletedFalse()
                : 1L;

        SuperAdminDashboardSummaryResponse response = new SuperAdminDashboardSummaryResponse();
        response.setKpis(getKpis(window.period().code(), orgId, window.zone().getId()));
        response.setDelinquentOrganisations(delinquentOrgs);
        response.setDelinquentInvoices(delinquentInvoices);
        response.setTotalOrganisations(totalOrganisations);
        response.setUsageMetrics(usage != null ? usage : new ArrayList<>());
        response.setGeneratedAt(Instant.now());
        return response;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "superAdminOrgDashboard", key = "T(java.util.Objects).hash(#period,#timezone)")
    public SuperAdminOrganisationDashboardResponse getOrganisationDashboard(String period, String timezone) {
        QueryWindow window = QueryWindow.from(period, timezone);
        MapSqlParameterSource params = baseParams(window, null);
        NamedParameterJdbcTemplate template = resolveAnalyticsTemplate().orElse(namedParameterJdbcTemplate);

        long totalOrganisations = organisationRepository.countByIsDeletedFalse();

        long activeCount = queryCount(template,
                "SELECT COUNT(DISTINCT os.organisation_id) " +
                        "FROM public.org_subscriptions os " +
                        "JOIN public.organisations o ON o.id = os.organisation_id " +
                        "WHERE os.end_at IS NULL " +
                        "AND LOWER(os.status) = 'active' " +
                        "AND COALESCE(o.is_deleted, false) = false",
                params
        );

        long trialCount = queryCount(template,
                "SELECT COUNT(DISTINCT os.organisation_id) " +
                        "FROM public.org_subscriptions os " +
                        "JOIN public.organisations o ON o.id = os.organisation_id " +
                        "WHERE os.end_at IS NULL " +
                        "AND LOWER(os.status) = 'trialing' " +
                        "AND COALESCE(o.is_deleted, false) = false",
                params
        );

        long pastDueCount = queryCount(template,
                "SELECT COUNT(DISTINCT os.organisation_id) " +
                        "FROM public.org_subscriptions os " +
                        "JOIN public.organisations o ON o.id = os.organisation_id " +
                        "WHERE os.end_at IS NULL " +
                        "AND LOWER(os.status) IN ('past_due', 'failed', 'unpaid') " +
                        "AND COALESCE(o.is_deleted, false) = false",
                params
        );

        long suspendedCount = queryCount(template,
                "SELECT COUNT(*) " +
                        "FROM public.organisations o " +
                        "WHERE UPPER(o.status) = 'LOCKED' " +
                        "AND COALESCE(o.is_deleted, false) = false",
                params
        );

        List<SuperAdminOrganisationStatusCountResponse> statusCounts = List.of(
                new SuperAdminOrganisationStatusCountResponse("Active", activeCount),
                new SuperAdminOrganisationStatusCountResponse("Trial", trialCount),
                new SuperAdminOrganisationStatusCountResponse("Past Due", pastDueCount),
                new SuperAdminOrganisationStatusCountResponse("Suspended", suspendedCount)
        );

        // Active staff only (is_active), not period login activity and not inactive memberships
        List<SuperAdminTopTenantResponse> topTenants = template.query(
                "SELECT o.id, o.name, sp.name AS plan_name, sp.code AS plan_code, " +
                        "COUNT(DISTINCT ai.id) AS active_users " +
                        "FROM public.organisations o " +
                        "JOIN public.user_organisations uo ON uo.organisation_id = o.id " +
                        "JOIN public.auth_identities ai ON ai.id = uo.auth_id " +
                        "LEFT JOIN LATERAL ( " +
                        "   SELECT os.plan_id " +
                        "   FROM public.org_subscriptions os " +
                        "   WHERE os.organisation_id = o.id " +
                        "   AND os.end_at IS NULL " +
                        "   ORDER BY os.start_at DESC NULLS LAST, os.id DESC " +
                        "   LIMIT 1 " +
                        ") os ON true " +
                        "LEFT JOIN public.subscription_plans sp ON sp.id = os.plan_id " +
                        "WHERE COALESCE(o.is_deleted, false) = false " +
                        "AND UPPER(ai.identity_type) = 'STAFF' " +
                        "AND ai.is_active = true " +
                        "GROUP BY o.id, o.name, sp.name, sp.code " +
                        "ORDER BY active_users DESC, o.id DESC " +
                        "LIMIT 3",
                params,
                (rs, idx) -> new SuperAdminTopTenantResponse(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("plan_name"),
                        rs.getString("plan_code"),
                        rs.getLong("active_users")
                )
        );

        SuperAdminOrganisationDashboardResponse response = new SuperAdminOrganisationDashboardResponse();
        response.setPeriod(window.period().code());
        response.setTimezone(window.zone().getId());
        response.setFrom(window.from());
        response.setTo(window.to());
        response.setTotalOrganisations(totalOrganisations);
        response.setStatusCounts(statusCounts);
        response.setTopTenants(topTenants != null ? topTenants : new ArrayList<>());
        response.setGeneratedAt(Instant.now());
        return response;
    }

    private Optional<NamedParameterJdbcTemplate> resolveAnalyticsTemplate() {
        return Optional.ofNullable(analyticsJdbcTemplateProvider.getIfAvailable());
    }

    private Organisation requireOrganisation(Long orgId) {
        if (orgId == null) {
            return null;
        }
        return organisationRepository.findById(orgId)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", "Organisation not found"));
    }

    private String resolveCurrency(Long orgId) {
        if (orgId == null) {
            return "USD";
        }
        Optional<OrgStripeAccount> stripeAccount = orgStripeAccountRepository.findByOrganisationId(orgId);
        String currency = stripeAccount.map(OrgStripeAccount::getDefaultCurrency).orElse(null);
        if (currency == null || currency.isBlank()) {
            return "USD";
        }
        return currency.toUpperCase(Locale.ROOT);
    }

    private static MapSqlParameterSource baseParams(QueryWindow window, Long orgId) {
        Timestamp fromTs = Timestamp.from(window.from());
        Timestamp toTs = Timestamp.from(window.to());
        return new MapSqlParameterSource()
                .addValue("from", fromTs, Types.TIMESTAMP)
                .addValue("to", toTs, Types.TIMESTAMP)
                .addValue("orgId", orgId)
                .addValue("featureCode", CoreFeature.SESSIONS_PER_MONTH.getCode());
    }

    private static long queryCount(NamedParameterJdbcTemplate template, String sql, MapSqlParameterSource params) {
        Long value = template.queryForObject(sql, params, Long.class);
        return value == null ? 0L : value;
    }

    private static BigDecimal queryDecimal(NamedParameterJdbcTemplate template, String sql, MapSqlParameterSource params) {
        BigDecimal value = template.queryForObject(sql, params, BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    private static ZoneId parseZone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of("UTC");
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (DateTimeException ex) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_TIMEZONE", "Invalid timezone: " + timezone);
        }
    }

    private record QueryWindow(PeriodWindow period, ZoneId zone, Instant from, Instant to, LocalDate startDate, LocalDate endDate) {
        static QueryWindow from(String periodText, String timezone) {
            PeriodWindow period = PeriodWindow.fromValue(periodText);
            ZoneId zone = parseZone(timezone);
            LocalDate today = LocalDate.now(zone);
            LocalDate startDate = today.minusDays(period.days() - 1L);
            ZonedDateTime startZdt = startDate.atStartOfDay(zone);
            ZonedDateTime endZdt = today.plusDays(1).atStartOfDay(zone);
            return new QueryWindow(period, zone, startZdt.toInstant(), endZdt.toInstant(), startDate, today.plusDays(1));
        }
    }

    private enum PeriodWindow {
        D7("7d", 7),
        D30("30d", 30),
        D90("90d", 90);

        private final String code;
        private final int days;

        PeriodWindow(String code, int days) {
            this.code = code;
            this.days = days;
        }

        public String code() {
            return code;
        }

        public int days() {
            return days;
        }

        public static PeriodWindow fromValue(String input) {
            if (input == null || input.isBlank()) {
                return D30;
            }
            String normalized = input.trim().toLowerCase(Locale.ROOT);
            for (PeriodWindow window : values()) {
                if (Objects.equals(window.code, normalized)) {
                    return window;
                }
            }
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "period must be 7d, 30d, or 90d");
        }
    }
}
