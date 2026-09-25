package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.superadmin.dto.SuperAdminDashboardKpiCardsResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminDashboardTierAliasItemResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminDashboardTierAliasesReplaceRequest;
import com.smart.therapy.flow.superadmin.dto.SuperAdminDashboardTierAliasesResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminMrrBreakdownChartResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminPlanDistributionChartResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminSystemHealthPanelResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminTenantGrowthChartResponse;
import com.smart.therapy.flow.superadmin.entity.PlatformDashboardTierAlias;
import com.smart.therapy.flow.superadmin.entity.PlatformIncident;
import com.smart.therapy.flow.superadmin.repository.PlatformDashboardTierAliasRepository;
import com.smart.therapy.flow.superadmin.repository.PlatformIncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SuperAdminDashboardOverviewService {

    private static final String CURRENCY_USD = "USD";
    private static final long UPTIME_WINDOW_DAYS = 30L;
    private static final long UPTIME_WINDOW_MS = UPTIME_WINDOW_DAYS * 24L * 60L * 60L * 1000L;
    /**
     * Severities that mean the platform was unavailable rather than merely degraded. Anything else
     * (minor, degraded, warning, ...) is tracked as an incident but does not burn availability.
     */
    private static final Set<String> OUTAGE_SEVERITIES = Set.of("critical", "outage", "major");
    private static final String CURRENT_SUBSCRIPTION_WINDOW_SQL =
            "os.start_at <= NOW() AND (os.end_at IS NULL OR os.end_at > NOW()) ";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final SuperAdminSystemService superAdminSystemService;
    private final PlatformIncidentRepository platformIncidentRepository;
    private final PlatformDashboardTierAliasRepository platformDashboardTierAliasRepository;
    private final com.smart.therapy.flow.organisation.service.PlatformAuditService platformAuditService;

    @Transactional
    public SuperAdminDashboardKpiCardsResponse getKpiCards(String timezone) {
        ZoneId zone = parseZone(timezone);
        LocalDate today = LocalDate.now(zone);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEndExclusive = monthStart.plusMonths(1);

        Instant monthStartInstant = monthStart.atStartOfDay(zone).toInstant();
        Instant monthEndInstant = monthEndExclusive.atStartOfDay(zone).toInstant();

        long activeTenants = queryCount(
                "SELECT COUNT(DISTINCT os.organisation_id) " +
                        "FROM public.org_subscriptions os " +
                        "JOIN public.organisations o ON o.id = os.organisation_id " +
                        "WHERE " + CURRENT_SUBSCRIPTION_WINDOW_SQL +
                        "AND LOWER(os.status) IN ('active', 'trialing', 'past_due') " +
                        "AND COALESCE(o.is_deleted, false) = false",
                new MapSqlParameterSource()
        );

        // Platform identities (super admins) carry no organisation on either side of the join, so they
        // are excluded here: they administer the platform, they are not tenant end users.
        List<Map<String, Object>> endUserRows = namedParameterJdbcTemplate.queryForList(
                "SELECT UPPER(ai.identity_type) AS identity_type, COUNT(DISTINCT ai.id) AS identities " +
                        "FROM public.auth_identities ai " +
                        "LEFT JOIN public.user_organisations uo ON uo.auth_id = ai.id " +
                        "JOIN public.organisations o ON o.id = COALESCE(uo.organisation_id, ai.organisation_id) " +
                        "WHERE UPPER(ai.identity_type) IN ('STAFF', 'CLIENT') " +
                        "AND ai.is_active = true " +
                        "AND COALESCE(ai.is_deleted, false) = false " +
                        "AND COALESCE(o.is_deleted, false) = false " +
                        "GROUP BY UPPER(ai.identity_type)",
                new MapSqlParameterSource()
        );
        long staffUsers = 0L;
        long portalClients = 0L;
        for (Map<String, Object> row : endUserRows) {
            long identities = readLong(row.get("identities"));
            if ("CLIENT".equals(readString(row.get("identity_type")))) {
                portalClients = identities;
            } else {
                staffUsers = identities;
            }
        }
        long totalEndUsers = staffUsers + portalClients;

        BigDecimal mrr = queryDecimal(
                "SELECT COALESCE(SUM( " +
                        "   CASE " +
                        "       WHEN LOWER(COALESCE(os.billing_cycle_at_time, 'monthly')) IN ('yearly', 'annual') " +
                        "           THEN COALESCE(os.price_at_time, 0) / 12.0 " +
                        "       ELSE COALESCE(os.price_at_time, 0) " +
                        "   END " +
                        "), 0) " +
                        "FROM public.org_subscriptions os " +
                        "JOIN public.organisations o ON o.id = os.organisation_id " +
                        "WHERE " + CURRENT_SUBSCRIPTION_WINDOW_SQL +
                        "AND LOWER(os.status) IN ('active', 'trialing', 'past_due') " +
                        "AND COALESCE(o.is_deleted, false) = false",
                new MapSqlParameterSource()
        ).setScale(2, RoundingMode.HALF_UP);

        MapSqlParameterSource churnParams = new MapSqlParameterSource()
                .addValue("monthStart", Timestamp.from(monthStartInstant), Types.TIMESTAMP)
                .addValue("monthEnd", Timestamp.from(monthEndInstant), Types.TIMESTAMP);
        long churnThisMonth = queryCount(
                "SELECT COUNT(DISTINCT os.organisation_id) " +
                        "FROM public.org_subscriptions os " +
                        "JOIN public.organisations o ON o.id = os.organisation_id " +
                        "WHERE os.end_at >= :monthStart AND os.end_at < :monthEnd " +
                        "AND COALESCE(o.is_deleted, false) = false",
                churnParams
        );

        UptimeWindow uptime = computeUptime(Instant.now());

        SuperAdminDashboardKpiCardsResponse response = new SuperAdminDashboardKpiCardsResponse();
        response.setTimezone(zone.getId());
        response.setCurrency(CURRENCY_USD);
        response.setMonthStart(monthStartInstant);
        response.setMonthEnd(monthEndInstant);
        response.setActiveTenants(activeTenants);
        response.setTotalEndUsers(totalEndUsers);
        response.setStaffUsers(staffUsers);
        response.setPortalClients(portalClients);
        response.setMrr(mrr);
        response.setChurnThisMonth(churnThisMonth);
        response.setPlatformUptimePercent(uptime.percent());
        response.setUptimeWindowDays(UPTIME_WINDOW_DAYS);
        response.setDowntimeMinutes(uptime.downtimeMinutes());
        response.setIncidentsInWindow(uptime.incidents());
        response.setGeneratedAt(Instant.now());
        return response;
    }

    @Transactional
    public SuperAdminTenantGrowthChartResponse getTenantGrowth(String range, String timezone) {
        GrowthRange growthRange = GrowthRange.fromValue(range);
        ZoneId zone = parseZone(timezone);
        List<Bucket> buckets = growthRange.buildBuckets(zone);

        List<SuperAdminTenantGrowthChartResponse.Point> points = new ArrayList<>();
        long totalNew = 0L;
        long totalChurned = 0L;

        for (Bucket bucket : buckets) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("from", Timestamp.from(bucket.from()), Types.TIMESTAMP)
                    .addValue("to", Timestamp.from(bucket.to()), Types.TIMESTAMP);

            long newTenants = queryCount(
                    "SELECT COUNT(*) " +
                            "FROM public.organisations o " +
                            "WHERE o.created_at >= :from AND o.created_at < :to " +
                            "AND COALESCE(o.is_deleted, false) = false",
                    params
            );
            long churnedTenants = queryCount(
                    "SELECT COUNT(DISTINCT os.organisation_id) " +
                            "FROM public.org_subscriptions os " +
                            "JOIN public.organisations o ON o.id = os.organisation_id " +
                            "WHERE os.end_at >= :from AND os.end_at < :to " +
                            "AND COALESCE(o.is_deleted, false) = false",
                    params
            );

            totalNew += newTenants;
            totalChurned += churnedTenants;

            SuperAdminTenantGrowthChartResponse.Point point = new SuperAdminTenantGrowthChartResponse.Point();
            point.setLabel(bucket.label());
            point.setFrom(bucket.from());
            point.setTo(bucket.to());
            point.setNewTenants(newTenants);
            point.setChurnedTenants(churnedTenants);
            point.setNetGrowth(newTenants - churnedTenants);
            points.add(point);
        }

        SuperAdminTenantGrowthChartResponse response = new SuperAdminTenantGrowthChartResponse();
        response.setRange(growthRange.apiValue());
        response.setTimezone(zone.getId());
        response.setFrom(buckets.isEmpty() ? Instant.now() : buckets.get(0).from());
        response.setTo(buckets.isEmpty() ? Instant.now() : buckets.get(buckets.size() - 1).to());
        response.setPoints(points);
        response.setTotalNewTenants(totalNew);
        response.setTotalChurnedTenants(totalChurned);
        response.setNetGrowth(totalNew - totalChurned);
        response.setGeneratedAt(Instant.now());
        return response;
    }

    @Transactional
    public SuperAdminPlanDistributionChartResponse getPlanDistribution() {
        List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(
                "SELECT UPPER(COALESCE(sp.code, 'UNASSIGNED')) AS plan_code, " +
                        "COALESCE(NULLIF(TRIM(sp.name), ''), UPPER(COALESCE(sp.code, 'UNASSIGNED'))) AS plan_name, " +
                        "COUNT(DISTINCT os.organisation_id) AS tenants " +
                        "FROM public.org_subscriptions os " +
                        "JOIN public.organisations o ON o.id = os.organisation_id " +
                        "LEFT JOIN public.subscription_plans sp ON sp.id = os.plan_id " +
                        "WHERE " + CURRENT_SUBSCRIPTION_WINDOW_SQL +
                        "AND LOWER(os.status) IN ('active', 'trialing', 'past_due') " +
                        "AND COALESCE(o.is_deleted, false) = false " +
                        "GROUP BY UPPER(COALESCE(sp.code, 'UNASSIGNED')), " +
                        "COALESCE(NULLIF(TRIM(sp.name), ''), UPPER(COALESCE(sp.code, 'UNASSIGNED'))) " +
                        "ORDER BY tenants DESC, plan_name ASC",
                new MapSqlParameterSource()
        );

        long total = rows.stream().mapToLong(row -> readLong(row.get("tenants"))).sum();
        List<SuperAdminPlanDistributionChartResponse.PlanDistributionItem> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            long tenants = readLong(row.get("tenants"));
            BigDecimal percentage = total > 0
                    ? BigDecimal.valueOf((tenants * 100.0) / total).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            SuperAdminPlanDistributionChartResponse.PlanDistributionItem item =
                    new SuperAdminPlanDistributionChartResponse.PlanDistributionItem();
            item.setPlanCode(readString(row.get("plan_code")));
            item.setPlanName(readString(row.get("plan_name")));
            item.setTenants(tenants);
            item.setPercentage(percentage);
            items.add(item);
        }

        SuperAdminPlanDistributionChartResponse response = new SuperAdminPlanDistributionChartResponse();
        response.setTotalTenants(total);
        response.setPlans(items);
        response.setGeneratedAt(Instant.now());
        return response;
    }

    @Transactional
    public SuperAdminMrrBreakdownChartResponse getMrrBreakdown() {
        List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(
                "SELECT UPPER(COALESCE(sp.code, 'UNASSIGNED')) AS plan_code, " +
                        "COALESCE(NULLIF(TRIM(sp.name), ''), UPPER(COALESCE(sp.code, 'UNASSIGNED'))) AS plan_name, " +
                        "COALESCE(SUM( " +
                        "   CASE " +
                        "       WHEN LOWER(COALESCE(os.billing_cycle_at_time, 'monthly')) IN ('yearly', 'annual') " +
                        "           THEN COALESCE(os.price_at_time, 0) / 12.0 " +
                        "       ELSE COALESCE(os.price_at_time, 0) " +
                        "   END " +
                        "), 0) AS mrr " +
                        "FROM public.org_subscriptions os " +
                        "JOIN public.organisations o ON o.id = os.organisation_id " +
                        "LEFT JOIN public.subscription_plans sp ON sp.id = os.plan_id " +
                        "WHERE " + CURRENT_SUBSCRIPTION_WINDOW_SQL +
                        "AND LOWER(os.status) IN ('active', 'trialing', 'past_due') " +
                        "AND COALESCE(o.is_deleted, false) = false " +
                        "GROUP BY UPPER(COALESCE(sp.code, 'UNASSIGNED')), " +
                        "COALESCE(NULLIF(TRIM(sp.name), ''), UPPER(COALESCE(sp.code, 'UNASSIGNED'))) " +
                        "ORDER BY mrr DESC, plan_name ASC",
                new MapSqlParameterSource()
        );

        BigDecimal totalMrr = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            BigDecimal mrr = readDecimal(row.get("mrr"));
            totalMrr = totalMrr.add(mrr);
        }

        List<SuperAdminMrrBreakdownChartResponse.PlanMrrItem> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            BigDecimal mrr = readDecimal(row.get("mrr")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal percentage = totalMrr.compareTo(BigDecimal.ZERO) > 0
                    ? mrr.multiply(BigDecimal.valueOf(100))
                    .divide(totalMrr, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            items.add(new SuperAdminMrrBreakdownChartResponse.PlanMrrItem(
                    readString(row.get("plan_code")),
                    readString(row.get("plan_name")),
                    mrr,
                    percentage
            ));
        }

        SuperAdminMrrBreakdownChartResponse response = new SuperAdminMrrBreakdownChartResponse();
        response.setCurrency(CURRENCY_USD);
        response.setPlans(items);
        response.setTotalMrr(totalMrr.setScale(2, RoundingMode.HALF_UP));
        response.setGeneratedAt(Instant.now());
        return response;
    }

    @Transactional(readOnly = true)
    public SuperAdminDashboardTierAliasesResponse getTierAliases() {
        List<PlatformDashboardTierAlias> rows = platformDashboardTierAliasRepository.findByIsDeletedFalseOrderByTierNameAscPlanCodeAsc();
        List<SuperAdminDashboardTierAliasItemResponse> aliases = rows.stream()
                .map(row -> new SuperAdminDashboardTierAliasItemResponse(row.getTierName(), row.getPlanCode()))
                .toList();

        SuperAdminDashboardTierAliasesResponse response = new SuperAdminDashboardTierAliasesResponse();
        response.setAliases(aliases);
        response.setGeneratedAt(Instant.now());
        return response;
    }

    @Transactional
    public SuperAdminDashboardTierAliasesResponse replaceTierAliases(
            SuperAdminDashboardTierAliasesReplaceRequest request,
            Long actorAuthId
    ) {
        if (request == null || request.getAliases() == null || request.getAliases().isEmpty()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "aliases is required");
        }

        List<PlatformDashboardTierAlias> existing = platformDashboardTierAliasRepository.findByIsDeletedFalseOrderByTierNameAscPlanCodeAsc();
        Map<String, Object> before = Map.of(
                "aliases", existing.stream()
                        .map(it -> Map.of("tierName", it.getTierName(), "planCode", it.getPlanCode()))
                        .toList()
        );

        Set<String> uniq = new HashSet<>();
        List<PlatformDashboardTierAlias> next = new ArrayList<>();
        for (SuperAdminDashboardTierAliasesReplaceRequest.Item item : request.getAliases()) {
            String tierName = normalizeTierName(item.getTierName());
            String planCode = normalizePlanCode(item.getPlanCode());
            String uniqKey = tierName + "|" + planCode;
            if (!uniq.add(uniqKey)) {
                continue;
            }

            PlatformDashboardTierAlias row = new PlatformDashboardTierAlias();
            row.setTierName(tierName);
            row.setPlanCode(planCode);
            row.setIsDeleted(false);
            next.add(row);
        }
        if (next.isEmpty()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "aliases cannot be empty after normalization");
        }

        platformDashboardTierAliasRepository.deleteAllInBatch();
        platformDashboardTierAliasRepository.saveAll(next);

        Map<String, Object> after = Map.of(
                "aliases", next.stream()
                        .map(it -> Map.of("tierName", it.getTierName(), "planCode", it.getPlanCode()))
                        .toList()
        );
        platformAuditService.logWithSnapshots(
                actorAuthId,
                "DASHBOARD_TIER_ALIASES_REPLACED",
                "PlatformDashboardTierAlias",
                "dashboard",
                before,
                after,
                "count=" + next.size()
        );

        return getTierAliases();
    }

    @Transactional
    public SuperAdminSystemHealthPanelResponse getSystemHealthPanel() {
        Map<String, Object> health = superAdminSystemService.getSystemHealth();
        String overallHealthCode = readString(health.get("status")).toLowerCase(Locale.ROOT);
        long openIncidents = readLong(health.get("openIncidents"));

        @SuppressWarnings("unchecked")
        Map<String, Object> details = health.get("details") instanceof Map<?, ?> detailMap
                ? (Map<String, Object>) detailMap
                : Map.of();

        List<PlatformIncident> activeIncidents = platformIncidentRepository.findByStatusOrderByStartedAtDesc("open");
        Map<String, String> serviceStates = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : details.entrySet()) {
            String componentName = toDisplayName(entry.getKey());
            if (componentName.isBlank()) {
                continue;
            }
            serviceStates.put(componentName, toPanelStatus(resolveHealthStatusCode(entry.getValue())));
        }

        for (PlatformIncident incident : activeIncidents) {
            String serviceName = readString(incident.getServiceName()).trim();
            if (serviceName.isEmpty()) {
                serviceName = readString(incident.getTitle()).trim();
            }
            if (serviceName.isEmpty()) {
                serviceName = "Platform Incident";
            }
            serviceStates.put(serviceName, "Degraded");
        }

        if (serviceStates.isEmpty()) {
            serviceStates.put("Platform", toPanelStatus(overallHealthCode));
        }

        List<SuperAdminSystemHealthPanelResponse.ServiceIndicator> services = new ArrayList<>();
        for (Map.Entry<String, String> entry : serviceStates.entrySet()) {
            services.add(new SuperAdminSystemHealthPanelResponse.ServiceIndicator(entry.getKey(), entry.getValue()));
        }
        boolean anyDegraded = services.stream().anyMatch(service -> "Degraded".equalsIgnoreCase(service.getStatus()));

        SuperAdminSystemHealthPanelResponse response = new SuperAdminSystemHealthPanelResponse();
        response.setOverallStatus(anyDegraded ? "Degraded" : "Operational");
        response.setOpenIncidents(openIncidents);
        response.setServices(services);
        response.setGeneratedAt(Instant.now());
        return response;
    }

    private static String toPanelStatus(String statusCode) {
        if ("up".equalsIgnoreCase(statusCode)) {
            return "Operational";
        }
        return "Degraded";
    }

    @SuppressWarnings("unchecked")
    private String resolveHealthStatusCode(Object component) {
        if (component instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = (Map<String, Object>) rawMap;
            Object status = map.get("status");
            return readString(status).toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private static String toDisplayName(String key) {
        if (key == null) {
            return "";
        }
        String normalized = key.replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .trim();
        if (normalized.isEmpty()) {
            return "";
        }
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.length() <= 3 && Set.of("api", "db", "cdn", "cpu", "ram", "smtp").contains(part.toLowerCase(Locale.ROOT))) {
                builder.append(part.toUpperCase(Locale.ROOT));
            } else {
                builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT))
                        .append(part.substring(1).toLowerCase(Locale.ROOT));
            }
            if (i < parts.length - 1) {
                builder.append(' ');
            }
        }
        return builder.toString();
    }

    private static long queryCount(String sql, MapSqlParameterSource params, NamedParameterJdbcTemplate jdbcTemplate) {
        Long value = jdbcTemplate.queryForObject(sql, params, Long.class);
        return value == null ? 0L : value;
    }

    private long queryCount(String sql, MapSqlParameterSource params) {
        return queryCount(sql, params, namedParameterJdbcTemplate);
    }

    private BigDecimal queryDecimal(String sql, MapSqlParameterSource params) {
        BigDecimal value = namedParameterJdbcTemplate.queryForObject(sql, params, BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    private static long readLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private static String readString(Object value) {
        return value == null ? "" : value.toString();
    }

    private static BigDecimal readDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
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

    private String normalizeTierName(String input) {
        if (input == null || input.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "tierName is required");
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "enterprise" -> "Enterprise";
            case "professional", "pro" -> "Professional";
            case "basic", "starter", "free" -> "Starter";
            default -> throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "tierName must be one of Enterprise, Professional, Starter");
        };
    }

    private String normalizePlanCode(String input) {
        if (input == null || input.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "planCode is required");
        }
        return input.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Availability over the trailing 30 days, measured from recorded platform incidents: the union of
     * outage windows (clipped to the window, open incidents running to now) is the downtime. JVM uptime
     * is deliberately not used — a restart does not mean the platform was unavailable for 30 days, and
     * a long-lived process does not prove it was up.
     */
    private UptimeWindow computeUptime(Instant now) {
        Instant windowStart = now.minusMillis(UPTIME_WINDOW_MS);
        List<PlatformIncident> incidents = platformIncidentRepository.findOverlapping(windowStart, now);

        List<long[]> outages = new ArrayList<>();
        for (PlatformIncident incident : incidents) {
            String severity = readString(incident.getSeverity()).trim().toLowerCase(Locale.ROOT);
            if (!OUTAGE_SEVERITIES.contains(severity)) {
                continue;
            }
            Instant startedAt = incident.getStartedAt();
            if (startedAt == null) {
                continue;
            }
            Instant resolvedAt = incident.getResolvedAt() != null ? incident.getResolvedAt() : now;
            long from = Math.max(startedAt.toEpochMilli(), windowStart.toEpochMilli());
            long to = Math.min(resolvedAt.toEpochMilli(), now.toEpochMilli());
            if (to > from) {
                outages.add(new long[]{from, to});
            }
        }

        // Overlapping outages are one stretch of downtime, not two, so merge before summing.
        outages.sort((left, right) -> Long.compare(left[0], right[0]));
        long downtimeMs = 0L;
        long mergedFrom = 0L;
        long mergedTo = 0L;
        boolean open = false;
        for (long[] outage : outages) {
            if (!open) {
                mergedFrom = outage[0];
                mergedTo = outage[1];
                open = true;
            } else if (outage[0] <= mergedTo) {
                mergedTo = Math.max(mergedTo, outage[1]);
            } else {
                downtimeMs += mergedTo - mergedFrom;
                mergedFrom = outage[0];
                mergedTo = outage[1];
            }
        }
        if (open) {
            downtimeMs += mergedTo - mergedFrom;
        }

        downtimeMs = Math.min(downtimeMs, UPTIME_WINDOW_MS);
        BigDecimal percent = BigDecimal.valueOf((UPTIME_WINDOW_MS - downtimeMs) * 100.0 / UPTIME_WINDOW_MS)
                .setScale(2, RoundingMode.HALF_UP);
        long downtimeMinutes = Math.round(downtimeMs / 60000.0);
        return new UptimeWindow(percent, downtimeMinutes, incidents.size());
    }

    private record UptimeWindow(BigDecimal percent, long downtimeMinutes, long incidents) {
    }

    private enum GrowthRange {
        W7("7w"),
        M1("1m"),
        M3("3m");

        private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("yyyy-MM");

        private final String apiValue;

        GrowthRange(String apiValue) {
            this.apiValue = apiValue;
        }

        public String apiValue() {
            return apiValue;
        }

        static GrowthRange fromValue(String value) {
            if (value == null || value.isBlank()) {
                return M1;
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (GrowthRange range : values()) {
                if (range.apiValue.equals(normalized)) {
                    return range;
                }
            }
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_RANGE", "range must be 7w, 1m, or 3m");
        }

        List<Bucket> buildBuckets(ZoneId zone) {
            LocalDate today = LocalDate.now(zone);
            List<Bucket> buckets = new ArrayList<>();

            if (this == W7) {
                LocalDate currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate start = currentWeekStart.minusWeeks(6);
                for (int i = 0; i < 7; i++) {
                    LocalDate bucketStart = start.plusWeeks(i);
                    LocalDate bucketEnd = bucketStart.plusWeeks(1);
                    buckets.add(new Bucket(
                            DATE_LABEL.format(bucketStart),
                            bucketStart.atStartOfDay(zone).toInstant(),
                            bucketEnd.atStartOfDay(zone).toInstant()
                    ));
                }
                return buckets;
            }

            if (this == M3) {
                YearMonth currentMonth = YearMonth.from(today);
                YearMonth startMonth = currentMonth.minusMonths(2);
                for (int i = 0; i < 3; i++) {
                    YearMonth month = startMonth.plusMonths(i);
                    LocalDate bucketStart = month.atDay(1);
                    LocalDate bucketEnd = month.plusMonths(1).atDay(1);
                    buckets.add(new Bucket(
                            MONTH_LABEL.format(bucketStart),
                            bucketStart.atStartOfDay(zone).toInstant(),
                            bucketEnd.atStartOfDay(zone).toInstant()
                    ));
                }
                return buckets;
            }

            LocalDate endExclusive = today.plusDays(1);
            LocalDate start = endExclusive.minusDays(30);
            for (int i = 0; i < 30; i++) {
                LocalDate bucketStart = start.plusDays(i);
                LocalDate bucketEnd = bucketStart.plusDays(1);
                buckets.add(new Bucket(
                        DATE_LABEL.format(bucketStart),
                        bucketStart.atStartOfDay(zone).toInstant(),
                        bucketEnd.atStartOfDay(zone).toInstant()
                ));
            }
            return buckets;
        }
    }

    private record Bucket(String label, Instant from, Instant to) {
    }
}
