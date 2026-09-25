package com.smart.therapy.flow.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smart.therapy.flow.auth.entity.AuditLog;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.audit.dto.AuditLogFilterRequest;
import com.smart.therapy.flow.audit.dto.AuditDashboardResponse;
import com.smart.therapy.flow.audit.dto.AuditLogResponse;
import com.smart.therapy.flow.audit.dto.AuditLogStatisticsResponse;
import com.smart.therapy.flow.audit.dto.AuditPageMetaResponse;
import com.smart.therapy.flow.audit.dto.AuditRiskDistributionResponse;
import com.smart.therapy.flow.audit.dto.UserActivitySummary;
import com.smart.therapy.flow.audit.enums.AuditResult;
import com.smart.therapy.flow.audit.enums.AuditRiskLevel;
import com.smart.therapy.flow.audit.support.AfterCommitAudit;
import com.smart.therapy.flow.audit.support.AuditEventDraft;
import com.smart.therapy.flow.audit.util.AuditLogLevelResolver;
import com.smart.therapy.flow.common.security.ImpersonationContext;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.audit.HipaaAuditLabels;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Canonical HIPAA / business audit trail writer.
 * <p>
 * <b>All application code must persist audit rows through this service</b>
 * ({@link #write}, {@link #writeImmediate}, {@link #record}, or the specialized
 * {@code log*Access} helpers). Do not call {@code AuditLogRepository.save} from
 * domain services — private {@code @Transactional(REQUIRES_NEW)} helpers do not
 * open a new transaction (Spring self-invocation) and poison read-only GETs.
 * <p>
 * Semantics:
 * <ul>
 *   <li>{@link #write} — success path: after outer TX commit (or immediately if none).
 *       Same outcome as before for successful requests; never INSERTs inside read-only TXs.</li>
 *   <li>{@link #writeImmediate} — security path: REQUIRES_NEW now so auth failures /
 *       unauthorized access survive outer rollbacks.</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {
    private static final Pattern DETAILS_TAIL_PATTERN = Pattern.compile("(?:^|,\\s*)details=(.*)$");

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final ObjectProvider<AuditLogService> selfProvider;

    /**
     * Low-level persist in {@code REQUIRES_NEW}. Prefer {@link #write} /
     * {@link #writeImmediate} so callers always go through the Spring proxy.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(AuditLog auditLog) {
        try {
            if (auditLog.getTimestamp() == null) {
                auditLog.setTimestamp(Instant.now());
            }
            // Re-bind inside REQUIRES_NEW using scalar lookups. Loading whole entities
            // here also hydrates inverse clinical/profile relationships on every audit.
            if (auditLog.getUser() != null && auditLog.getUser().getId() != null) {
                Long userId = auditLog.getUser().getId();
                var actor = userRepository.findAuditActorById(userId);
                auditLog.setUser(actor.map(value -> entityManager.getReference(User.class, value.getId())).orElse(null));
                // Prefer stable loginIdentifier over email / Spring authId username.
                if (actor.isPresent() && needsActorUsernameNormalization(auditLog.getUsername())) {
                    String label = resolveStaffActorLabel(actor.get());
                    if (StringUtils.hasText(label)) {
                        auditLog.setUsername(label);
                    }
                }
            }
            if (auditLog.getClient() != null && auditLog.getClient().getId() != null) {
                Long clientId = auditLog.getClient().getId();
                // Include soft-deleted clients so delete/restore audits keep the client FK + MRN.
                auditLog.setClient(clientRepository.findExistingIdIncludingDeleted(clientId)
                        .map(id -> entityManager.getReference(Client.class, id)).orElse(null));
            }
            if (auditLog.getImpersonatorAuthId() == null) {
                auditLog.setImpersonatorAuthId(ImpersonationContext.getImpersonatorAuthId());
            }
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            recordAuditWriteFailure("log_action", e);
            log.error("CRITICAL: Audit log write failed: action={}, resourceType={}, errorType={}",
                    auditLog.getAction(), auditLog.getResourceType(), e.getClass().getSimpleName());
        }
    }

    /**
     * Success-path write for PHI access and business events. Defers until the current
     * transaction commits so audits match completed operations and never abort
     * PostgreSQL read-only connections.
     */
    public void write(AuditLog auditLog) {
        if (auditLog == null) {
            return;
        }
        AfterCommitAudit.run(() -> selfProvider.getObject().logAction(auditLog));
    }

    /**
     * Security/auth write that must persist even if the outer transaction rolls back.
     */
    public void writeImmediate(AuditLog auditLog) {
        if (auditLog == null) {
            return;
        }
        selfProvider.getObject().logAction(auditLog);
    }

    /**
     * Build from a scalar draft and {@link #write}.
     */
    public void record(AuditEventDraft draft) {
        if (draft == null) {
            return;
        }
        write(draft.toAuditLog());
    }

    /**
     * Build from a scalar draft and {@link #writeImmediate}.
     */
    public void recordImmediate(AuditEventDraft draft) {
        if (draft == null) {
            return;
        }
        writeImmediate(draft.toAuditLog());
    }

    /**
     * Convenience for the common staff-actor pattern used across domain services.
     */
    public void recordStaffEvent(Long actorId, String action, String resourceType, Long resourceId,
            Long clientId, String ipAddress, boolean hipaaRelevant) {
        recordStaffEvent(actorId, action, resourceType, resourceId, clientId, ipAddress, hipaaRelevant, null);
    }

    public void recordStaffEvent(Long actorId, String action, String resourceType, Long resourceId,
            Long clientId, String ipAddress, boolean hipaaRelevant, String details) {
        recordStaffEvent(actorId, action, resourceType, resourceId, clientId, ipAddress, hipaaRelevant, details, null);
    }

    public void recordStaffEvent(Long actorId, String action, String resourceType, Long resourceId,
            Long clientId, String ipAddress, boolean hipaaRelevant, String details, String userAgent) {
        if (actorId == null) {
            log.warn("Audit skipped because actor id was absent: action={}", action);
            return;
        }
        AuditEventDraft draft = AuditEventDraft.of(action, resourceType)
                .actorId(actorId)
                .clientId(clientId)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .hipaaRelevant(hipaaRelevant)
                .details(details)
                .impersonatorAuthId(ImpersonationContext.getImpersonatorAuthId());
        if ("client_deleted".equals(action) || "session_deleted".equals(action)) {
            draft.riskLevel(AuditRiskLevel.HIGH.dbValue());
        }
        record(draft);
    }

    public void recordStaffEventWithStates(Long actorId, String action, String resourceType, Long resourceId,
            String ipAddress, boolean hipaaRelevant, String beforeState, String afterState, String changedFields) {
        recordStaffEventWithStates(actorId, action, resourceType, resourceId, resourceId, ipAddress,
                hipaaRelevant, beforeState, afterState, changedFields);
    }

    public void recordStaffEventWithStates(Long actorId, String action, String resourceType, Long resourceId,
            Long clientId, String ipAddress, boolean hipaaRelevant, String beforeState, String afterState,
            String changedFields) {
        if (actorId == null) {
            return;
        }
        record(AuditEventDraft.of(action, resourceType)
                .actorId(actorId)
                .clientId(clientId)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .hipaaRelevant(hipaaRelevant)
                .beforeState(beforeState)
                .afterState(afterState)
                .changedFields(changedFields)
                .result("success"));
    }

    /** @deprecated use {@link #write}; kept for call-site compatibility during migration */
    @Deprecated
    public void logActionAfterCommit(AuditLog auditLog) {
        write(auditLog);
    }

    // ========== SPECIALIZED LOGGING METHODS ==========

    public void logClientAccess(Long userId, String username, Long clientId,
            String action, String ipAddress, String userAgent,
            Map<String, Object> details) {
        String riskLevel = "client_deleted".equals(action)
                ? AuditRiskLevel.HIGH.dbValue()
                : AuditRiskLevel.MEDIUM.dbValue();

        AuditLog auditLog = buildAuditLog(userId, username, "client",
                clientId != null ? clientId.toString() : null, clientId, action,
                ipAddress, userAgent, true, riskLevel,
                details, "Clinical care and treatment");

        write(auditLog);
    }

    public void logSessionAccess(Long userId, String username, Long sessionId, Long clientId,
            String action, String ipAddress, String userAgent,
            Map<String, Object> details) {
        String riskLevel = "session_deleted".equals(action)
                ? AuditRiskLevel.HIGH.dbValue()
                : AuditRiskLevel.MEDIUM.dbValue();

        AuditLog auditLog = buildAuditLog(userId, username, "session",
                sessionId != null ? sessionId.toString() : null, clientId, action,
                ipAddress, userAgent, true, riskLevel,
                details, "Clinical documentation and care");

        write(auditLog);
    }

    public void logDocumentAccess(Long userId, String username, Long documentId, Long clientId,
            String action, String ipAddress, String userAgent,
            Map<String, Object> details) {
        AuditLog auditLog = buildAuditLog(userId, username, "document",
                documentId != null ? documentId.toString() : null, clientId, action,
                ipAddress, userAgent, true, AuditRiskLevel.HIGH.dbValue(),
                details, "Clinical documentation review");

        write(auditLog);
    }

    public void logSessionNoteAccess(Long userId, String username, Long noteId, Long clientId,
            String action, String ipAddress, String userAgent,
            Map<String, Object> details) {
        AuditLog auditLog = buildAuditLog(userId, username, "session_note",
                noteId != null ? noteId.toString() : null, clientId, action,
                ipAddress, userAgent, true, AuditRiskLevel.HIGH.dbValue(),
                details, "Clinical documentation and care");

        write(auditLog);
    }

    public void logAssessmentAccess(Long userId, String username, Long assessmentId, Long clientId,
            String action, String ipAddress, String userAgent,
            Map<String, Object> details) {
        AuditLog auditLog = buildAuditLog(userId, username, "assessment",
                assessmentId != null ? assessmentId.toString() : null, clientId, action,
                ipAddress, userAgent, true, AuditRiskLevel.HIGH.dbValue(),
                details, "Clinical assessment and evaluation");

        write(auditLog);
    }

    public void logClientReportAccess(Long userId, String username, Long reportId, Long clientId,
            String action, String ipAddress, String userAgent,
            Map<String, Object> details) {
        AuditLog auditLog = buildAuditLog(userId, username, "client_report",
                reportId != null ? reportId.toString() : null, clientId, action,
                ipAddress, userAgent, true, AuditRiskLevel.HIGH.dbValue(),
                details, "AI client report generation and review");

        write(auditLog);
    }

    public void logReportTemplateAccess(Long userId, String username, Long templateId,
            String action, String ipAddress, String userAgent,
            Map<String, Object> details) {
        AuditLog auditLog = buildAuditLog(userId, username, "report_template",
                templateId != null ? templateId.toString() : null, null, action,
                ipAddress, userAgent, true, AuditRiskLevel.MEDIUM.dbValue(),
                details, "Report template administration");

        write(auditLog);
    }

    public void logBillingAccess(Long userId, String username, Long billingId, Long clientId,
            String action, String ipAddress, String userAgent,
            Map<String, Object> details) {
        AuditLog auditLog = buildAuditLog(userId, username, "billing",
                billingId != null ? billingId.toString() : null, clientId, action,
                ipAddress, userAgent, true, AuditRiskLevel.MEDIUM.dbValue(),
                details, "Billing and financial services");

        write(auditLog);
    }

    public void logAuthEvent(Long userId, String username, String action, String ipAddress,
            String userAgent, String result, Map<String, Object> details) {
        AuditResult eventResult = AuditResult.fromDbValueOrDefault(result, AuditResult.SUCCESS);
        String riskLevel = eventResult.isFailureLike()
                ? AuditRiskLevel.HIGH.dbValue()
                : AuditRiskLevel.LOW.dbValue();

        AuditLog auditLog = buildAuditLog(userId, username, "authentication",
                username, null, action, ipAddress, userAgent, false, riskLevel,
                details, null);
        auditLog.setResult(eventResult.dbValue());

        // Immediate write: auth failures must persist even if the outer TX rolls back.
        writeImmediate(auditLog);
    }

    public void logUnauthorizedAccess(Long userId, String username, String resourceType,
            String resourceId, String ipAddress, String userAgent,
            Map<String, Object> details) {
        Map<String, Object> enhancedDetails = new HashMap<>();
        if (details != null) {
            enhancedDetails.putAll(details);
        }
        enhancedDetails.put("blocked_at", Instant.now().toString());
        enhancedDetails.put("requires_review", true);

        AuditLog auditLog = buildAuditLog(userId, username, resourceType, resourceId, null,
                "unauthorized_access", ipAddress, userAgent, true, AuditRiskLevel.CRITICAL.dbValue(),
                enhancedDetails, null);
        auditLog.setResult(AuditResult.BLOCKED.dbValue());

        writeImmediate(auditLog);
    }

    public void logDataExport(Long userId, String username, String exportType,
            List<Long> clientIds, String ipAddress, String userAgent,
            Map<String, Object> details) {
        Map<String, Object> enhancedDetails = new HashMap<>();
        if (details != null) {
            enhancedDetails.putAll(details);
        }
        enhancedDetails.put("export_type", exportType);
        enhancedDetails.put("client_count", clientIds != null ? clientIds.size() : 0);
        enhancedDetails.put("export_timestamp", Instant.now().toString());

        AuditLog auditLog = buildAuditLog(userId, username, "export",
                exportType + "_" + System.currentTimeMillis(), null,
                "data_exported", ipAddress, userAgent, true, AuditRiskLevel.CRITICAL.dbValue(),
                enhancedDetails, "Authorized data export for clinical purposes");

        write(auditLog);
    }

    // ========== QUERY METHODS ==========

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(AuditLogFilterRequest filterRequest, Pageable pageable) {
        Specification<AuditLog> spec = buildSpecification(filterRequest);
        Page<AuditLog> logs = auditLogRepository.findAll(spec, pageable);
        Map<Long, String> staffActorLabels = resolveStaffActorLabels(logs.getContent());
        Map<String, Long> activityCounts = countActivitiesByUsername(spec, logs.getContent(), staffActorLabels);
        List<AuditLogResponse> mapped = logs.getContent().stream()
                .map(log -> toAuditLogResponse(log, activityCounts, staffActorLabels))
                .toList();
        return new PageImpl<>(mapped, pageable, logs.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AuditDashboardResponse getAuditDashboard(AuditLogFilterRequest filterRequest, String period, Pageable pageable) {
        AuditLogFilterRequest effectiveFilter = applyPeriodDefaults(filterRequest, period);
        AuditLogStatisticsResponse stats = getAuditStatistics(effectiveFilter);
        Page<AuditLogResponse> logsPage = getAuditLogs(effectiveFilter, pageable);

        long lowRiskEvents = defaultLong(stats.getLowRiskEvents());
        long mediumRiskEvents = defaultLong(stats.getMediumRiskEvents());
        long highRiskEvents = defaultLong(stats.getHighRiskEvents());
        long totalByRisk = lowRiskEvents + mediumRiskEvents + highRiskEvents;

        AuditRiskDistributionResponse riskDistribution = AuditRiskDistributionResponse.builder()
                .totalEvents(totalByRisk)
                .lowRiskEvents(lowRiskEvents)
                .mediumRiskEvents(mediumRiskEvents)
                .highRiskEvents(highRiskEvents)
                .lowRiskPercentage(calculatePercentage(lowRiskEvents, totalByRisk))
                .mediumRiskPercentage(calculatePercentage(mediumRiskEvents, totalByRisk))
                .highRiskPercentage(calculatePercentage(highRiskEvents, totalByRisk))
                .build();

        AuditPageMetaResponse pagination = AuditPageMetaResponse.builder()
                .page(logsPage.getNumber())
                .size(logsPage.getSize())
                .totalElements(logsPage.getTotalElements())
                .totalPages(logsPage.getTotalPages())
                .hasNext(logsPage.hasNext())
                .hasPrevious(logsPage.hasPrevious())
                .build();

        return AuditDashboardResponse.builder()
                .startDate(effectiveFilter.getStartDate())
                .endDate(effectiveFilter.getEndDate())
                .period(normalizePeriod(period))
                .totalActivities(defaultLong(stats.getTotalActivities()))
                .phiAccessEvents(defaultLong(stats.getPhiAccess()))
                .highRiskEvents(highRiskEvents)
                .failedAttempts(defaultLong(stats.getFailedAttempts()))
                .lowRiskEvents(lowRiskEvents)
                .mediumRiskEvents(mediumRiskEvents)
                .criticalRiskEvents(defaultLong(stats.getCriticalRiskEvents()))
                .userActivitySummary(stats.getUserActivity())
                .riskDistribution(riskDistribution)
                .logs(logsPage.getContent())
                .pagination(pagination)
                .build();
    }

    @Transactional(readOnly = true)
    public AuditLogStatisticsResponse getAuditStatistics(AuditLogFilterRequest filterRequest) {
        Specification<AuditLog> spec = buildSpecification(filterRequest);

        long totalActivities = auditLogRepository.count(spec);

        Specification<AuditLog> phiSpec = spec.and((root, query, cb) -> cb.equal(root.get("hipaaRelevant"), true));
        long phiAccess = auditLogRepository.count(phiSpec);

        long lowRiskEvents = countByRiskLevel(spec, AuditRiskLevel.LOW);
        long mediumRiskEvents = countByRiskLevel(spec, AuditRiskLevel.MEDIUM);
        long highOnlyRiskEvents = countByRiskLevel(spec, AuditRiskLevel.HIGH);
        long criticalRiskEvents = countByRiskLevel(spec, AuditRiskLevel.CRITICAL);
        long highRiskEvents = highOnlyRiskEvents + criticalRiskEvents;
        long failedAttempts = countByFailureResult(spec);

        List<UserActivitySummary> userActivity = getTopActiveUsers(filterRequest, 10);

        return AuditLogStatisticsResponse.builder()
                .totalActivities(totalActivities)
                .phiAccess(phiAccess)
                .highRiskEvents(highRiskEvents)
                .failedAttempts(failedAttempts)
                .lowRiskEvents(lowRiskEvents)
                .mediumRiskEvents(mediumRiskEvents)
                .criticalRiskEvents(criticalRiskEvents)
                .userActivity(userActivity)
                .build();
    }

    @Transactional(readOnly = true)
    public String exportAuditLogsAsCsv(AuditLogFilterRequest filterRequest, int limit) {
        Specification<AuditLog> spec = buildSpecification(filterRequest);
        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,User,Action,Resource,Result,Risk Level,PHI Relevant,IP Address,Client ID\n");

        int pageSize = 100;
        int totalProcessed = 0;

        while (totalProcessed < limit) {
            int currentBatchSize = Math.min(pageSize, limit - totalProcessed);
            if (currentBatchSize <= 0)
                break;

            Page<AuditLog> page = auditLogRepository.findAll(spec,
                    org.springframework.data.domain.PageRequest.of(totalProcessed / pageSize, pageSize));

            List<AuditLog> logs = page.getContent();
            if (logs.isEmpty())
                break;

            Map<Long, String> staffActorLabels = resolveStaffActorLabels(logs);
            for (AuditLog log : logs) {
                if (totalProcessed >= limit)
                    break;
                csv.append(csvCell(log.getTimestamp())).append(',')
                        .append(csvCell(resolveDisplayUsername(log, staffActorLabels))).append(',')
                        .append(csvCell(log.getAction())).append(',')
                        .append(csvCell(log.getResourceType())).append(',')
                        .append(csvCell(log.getResult())).append(',')
                        .append(csvCell(log.getRiskLevel())).append(',')
                        .append(csvCell(log.getHipaaRelevant())).append(',')
                        .append(csvCell(log.getIpAddress())).append(',')
                        .append(csvCell(log.getClient() != null ? log.getClient().getId() : null))
                        .append('\n');
                totalProcessed++;
            }

            // Clear persistence context to release memory for large exports
            entityManager.clear();

            if (!page.hasNext())
                break;
        }

        return csv.toString();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getClientAuditHistory(Long clientId, Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            return cb.equal(root.get("client").get("id"), clientId);
        };

        Page<AuditLog> logs = auditLogRepository.findAll(spec, pageable);
        Map<Long, String> staffActorLabels = resolveStaffActorLabels(logs.getContent());
        Map<String, Long> activityCounts = countActivitiesByUsername(spec, logs.getContent(), staffActorLabels);
        return logs.getContent().stream()
                .map(log -> toAuditLogResponse(log, activityCounts, staffActorLabels))
                .collect(Collectors.toList());
    }

    // ========== PRIVATE HELPER METHODS ==========

    private AuditLog buildAuditLog(Long userId, String username, String resourceType,
            String resourceId, Long clientId, String action,
            String ipAddress, String userAgent, Boolean hipaaRelevant,
            String riskLevel, Map<String, Object> details, String accessReason) {

        AuditLog auditLog = AuditLog.builder()
                .username(username)
                .action(action)
                .result(AuditResult.SUCCESS.dbValue())
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .hipaaRelevant(hipaaRelevant)
                .riskLevel(riskLevel)
                .details(details != null ? convertToJson(details) : null)
                .accessReason(accessReason)
                .timestamp(Instant.now())
                .build();

        if (userId != null) {
            userRepository.findById(userId).ifPresent(user -> {
                try {
                    auditLog.setUser(user);
                } catch (Exception e) {
                    log.error("Failed to set user on audit log", e);
                }
            });
        }

        if (clientId != null) {
            clientRepository.findByIdIncludingDeleted(clientId).ifPresent(client -> {
                try {
                    auditLog.setClient(client);
                } catch (Exception e) {
                    log.error("Failed to set client on audit log", e);
                }
            });
        }

        return auditLog;
    }

    private Specification<AuditLog> buildSpecification(AuditLogFilterRequest filterRequest) {
        return (root, query, cb) -> {
            List<Predicate> predicates = buildPredicates(filterRequest, root, cb);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private long countByRiskLevel(Specification<AuditLog> baseSpec, AuditRiskLevel riskLevel) {
        Specification<AuditLog> riskSpec = baseSpec.and(
                (root, query, cb) -> cb.equal(cb.lower(root.get("riskLevel")), riskLevel.dbValue()));
        return auditLogRepository.count(riskSpec);
    }

    private long countByFailureResult(Specification<AuditLog> baseSpec) {
        Specification<AuditLog> failureSpec = baseSpec.and((root, query, cb) -> cb.or(
                cb.equal(cb.lower(root.get("result")), AuditResult.FAILURE.dbValue()),
                cb.equal(cb.lower(root.get("result")), AuditResult.FAILED.dbValue()),
                cb.equal(cb.lower(root.get("result")), AuditResult.BLOCKED.dbValue())
        ));
        return auditLogRepository.count(failureSpec);
    }

    private String normalizeActionFilter(String actionFilter) {
        if (!StringUtils.hasText(actionFilter)) {
            return "all";
        }

        String normalized = actionFilter.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "client viewed" -> "client_viewed";
            case "document accessed" -> "document_accessed";
            case "data exported" -> "data_exported";
            default -> normalized.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        };
    }

    private List<UserActivitySummary> getTopActiveUsers(AuditLogFilterRequest filterRequest, int limit) {
        var cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<AuditLog> root = query.from(AuditLog.class);

        List<Predicate> predicates = buildPredicates(filterRequest, root, cb);
        predicates.add(cb.isNotNull(root.get("username")));
        predicates.add(cb.notEqual(cb.trim(root.get("username")), ""));

        query.multiselect(
                root.get("username").alias("username"),
                cb.count(root).alias("activityCount"),
                cb.greatest(root.get("timestamp").as(Instant.class)).alias("lastActivity"))
                .where(cb.and(predicates.toArray(new Predicate[0])))
                .groupBy(root.get("username"))
                .orderBy(cb.desc(cb.count(root)));

        List<Tuple> rows = entityManager.createQuery(query)
                .setMaxResults(limit)
                .getResultList();

        return rows.stream()
                .map(row -> UserActivitySummary.builder()
                        .username(row.get("username", String.class))
                        .activityCount(row.get("activityCount", Number.class).longValue())
                        .lastActivity(row.get("lastActivity", Instant.class))
                        .build())
                .collect(Collectors.toList());
    }

    private List<Predicate> buildPredicates(AuditLogFilterRequest filterRequest, Root<AuditLog> root,
            jakarta.persistence.criteria.CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (filterRequest == null) {
            return predicates;
        }

        if (filterRequest.getStartDate() != null) {
            Instant startInstant = filterRequest.getStartDate()
                    .atStartOfDay(ZoneId.systemDefault()).toInstant();
            predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startInstant));
        }

        if (filterRequest.getEndDate() != null) {
            LocalDateTime endDateTime = filterRequest.getEndDate()
                    .atTime(23, 59, 59, 999_999_999);
            Instant endInstant = endDateTime.atZone(ZoneId.systemDefault()).toInstant();
            predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endInstant));
        }

        if (StringUtils.hasText(filterRequest.getRiskLevel())) {
            AuditRiskLevel.tryFromFilter(filterRequest.getRiskLevel())
                    .filter(level -> !level.isAll())
                    .ifPresent(level -> predicates.add(
                            cb.equal(cb.lower(root.get("riskLevel")), level.dbValue())));
        }

        if (StringUtils.hasText(filterRequest.getAction()) &&
                !"all".equalsIgnoreCase(filterRequest.getAction())) {
            String normalizedAction = normalizeActionFilter(filterRequest.getAction());
            if (!"all".equalsIgnoreCase(normalizedAction)) {
                predicates.add(cb.equal(cb.lower(root.get("action")), normalizedAction.toLowerCase(Locale.ROOT)));
            }
        }

        if (StringUtils.hasText(filterRequest.getUsername())) {
            String escapedUsername = filterRequest.getUsername()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            predicates.add(cb.like(cb.lower(root.get("username")),
                    "%" + escapedUsername.toLowerCase() + "%"));
        }

        if (Boolean.TRUE.equals(filterRequest.getHipaaOnly())) {
            predicates.add(cb.equal(root.get("hipaaRelevant"), true));
        }

        if (filterRequest.getClientId() != null) {
            predicates.add(cb.equal(root.get("client").get("id"), filterRequest.getClientId()));
        }

        if (StringUtils.hasText(filterRequest.getResourceType())) {
            predicates.add(cb.equal(root.get("resourceType"), filterRequest.getResourceType()));
        }

        String normalizedLogLevel = AuditLogLevelResolver.normalizeFilter(filterRequest.getLogLevel());
        if (normalizedLogLevel != null) {
            switch (normalizedLogLevel) {
                case "ERROR" -> predicates.add(cb.or(
                        cb.equal(cb.lower(root.get("result")), AuditResult.FAILURE.dbValue()),
                        cb.equal(cb.lower(root.get("result")), AuditResult.FAILED.dbValue()),
                        cb.like(cb.lower(root.get("details")), "%status=5%"),
                        cb.like(cb.lower(root.get("details")), "%\"status\":5%"),
                        cb.like(cb.lower(root.get("details")), "%error=%")));
                case "WARN" -> predicates.add(cb.or(
                        cb.equal(cb.lower(root.get("result")), AuditResult.BLOCKED.dbValue()),
                        cb.like(cb.lower(root.get("details")), "%status=4%"),
                        cb.like(cb.lower(root.get("details")), "%\"status\":4%"),
                        cb.like(cb.lower(root.get("details")), "%forbidden%"),
                        cb.like(cb.lower(root.get("details")), "%unauthorized%")));
                case "TRACE" -> predicates.add(cb.and(
                        cb.like(cb.lower(root.get("action")), "api\\_%", '\\'),
                        cb.or(
                                cb.like(cb.lower(root.get("details")), "%details=[%"),
                                cb.like(cb.lower(root.get("details")), "%\"details\":[%"),
                                cb.like(cb.lower(root.get("details")), "%source=global_activity_aspect%"))));
                case "DEBUG" -> predicates.add(cb.like(cb.lower(root.get("action")), "api\\_%", '\\'));
                case "INFO" -> predicates.add(cb.and(
                        cb.equal(cb.lower(root.get("result")), AuditResult.SUCCESS.dbValue()),
                        cb.notLike(cb.lower(root.get("action")), "api\\_%", '\\')));
                default -> {
                }
            }
        }

        return predicates;
    }

    private AuditLogFilterRequest applyPeriodDefaults(AuditLogFilterRequest filterRequest, String period) {
        AuditLogFilterRequest base = filterRequest != null ? filterRequest : new AuditLogFilterRequest();

        LocalDate startDate = base.getStartDate();
        LocalDate endDate = base.getEndDate();

        String normalizedPeriod = normalizePeriod(period);
        if (startDate == null || endDate == null) {
            LocalDate now = LocalDate.now();
            LocalDate defaultEnd = endDate != null ? endDate : now;
            LocalDate defaultStart = startDate != null ? startDate : switch (normalizedPeriod) {
                case "monthly" -> defaultEnd.minusDays(29);
                case "yearly" -> defaultEnd.minusDays(364);
                default -> defaultEnd.minusDays(6);
            };
            startDate = defaultStart;
            endDate = defaultEnd;
        }

        if (startDate.isAfter(endDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }

        AuditLogFilterRequest request = new AuditLogFilterRequest();
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setRiskLevel(base.getRiskLevel());
        request.setHipaaOnly(base.getHipaaOnly());
        request.setAction(base.getAction());
        request.setUsername(base.getUsername());
        request.setClientId(base.getClientId());
        request.setResourceType(base.getResourceType());
        request.setLogLevel(base.getLogLevel());
        return request;
    }

    private String normalizePeriod(String period) {
        if (!StringUtils.hasText(period)) {
            return "weekly";
        }

        String normalized = period.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "monthly", "yearly", "weekly" -> normalized;
            default -> "weekly";
        };
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private double calculatePercentage(long value, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(value)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private AuditLogResponse toAuditLogResponse(AuditLog auditLog, Map<String, Long> activityCounts) {
        return toAuditLogResponse(auditLog, activityCounts, Map.of());
    }

    private AuditLogResponse toAuditLogResponse(AuditLog auditLog, Map<String, Long> activityCounts,
            Map<Long, String> staffActorLabels) {
        Long clientId = null;
        String clientMrn = null;
        if (auditLog.getClient() != null) {
            clientId = auditLog.getClient().getId();
            clientMrn = auditLog.getClient().getClientId();
        }
        if (clientId == null) {
            clientId = resolveClientPkFromAuditContext(auditLog);
        }
        if (!StringUtils.hasText(clientMrn)) {
            clientMrn = extractClientMrnFromDetails(auditLog.getDetails());
        }
        if (!StringUtils.hasText(clientMrn)) {
            clientMrn = resolveClientMrnFromAuditContext(auditLog);
        }

        Long userId = null;
        if (auditLog.getUser() != null) {
            userId = auditLog.getUser().getId();
        }

        String displayUsername = resolveDisplayUsername(auditLog, staffActorLabels);
        String actorKey = buildActorKey(displayUsername, userId);
        long userActivityCount = actorKey != null ? activityCounts.getOrDefault(actorKey, 0L) : 0L;
        String rawAction = auditLog.getAction();
        String logLevel = AuditLogLevelResolver.resolveTenantLogLevel(auditLog.getAction(), auditLog.getResult(), auditLog.getDetails());

        String displayAction = humanizeToken(rawAction);
        if ("data_exported".equalsIgnoreCase(rawAction)) {
            String exportType = null;
            try {
                JsonNode root = objectMapper.readTree(auditLog.getDetails());
                if (root != null && root.has("export_type")) {
                    exportType = root.get("export_type").asText();
                }
            } catch (Exception e) {
                // Ignore
            }
            if (!StringUtils.hasText(exportType)) {
                exportType = extractKv(auditLog.getDetails(), "export_type");
            }
            if (StringUtils.hasText(exportType)) {
                displayAction = humanizeToken(exportType);
            }
        }

        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .userId(userId)
                .username(displayUsername)
                .action(displayAction)
                .rawAction(rawAction)
                .logLevel(logLevel)
                .result(auditLog.getResult())
                .resourceType(auditLog.getResourceType())
                .resourceId(auditLog.getResourceId())
                .clientId(clientId)
                .clientMrn(clientMrn)
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .sessionId(auditLog.getSessionId())
                .riskLevel(auditLog.getRiskLevel())
                .hipaaRelevant(auditLog.getHipaaRelevant())
                .details(toHumanReadableDetails(auditLog))
                .userActivityCount(userActivityCount)
                .dataFields(auditLog.getDataFields())
                .accessReason(auditLog.getAccessReason())
                .timestamp(auditLog.getTimestamp())
                .build();
    }

    private Map<String, Long> countActivitiesByUsername(Specification<AuditLog> baseSpec, List<AuditLog> logs) {
        return countActivitiesByUsername(baseSpec, logs, Map.of());
    }

    private Map<String, Long> countActivitiesByUsername(Specification<AuditLog> baseSpec, List<AuditLog> logs,
            Map<Long, String> staffActorLabels) {
        Set<String> actorKeys = logs.stream()
                .map(log -> {
                    Long userId = log.getUser() != null ? log.getUser().getId() : null;
                    String displayUsername = resolveDisplayUsername(log, staffActorLabels);
                    return buildActorKey(displayUsername, userId);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (actorKeys.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> counts = new HashMap<>();
        for (String actorKey : actorKeys) {
            if (actorKey.startsWith("u:")) {
                String normalizedUsername = actorKey.substring(2);
                Specification<AuditLog> userSpec = baseSpec.and((root, query, cb) ->
                        cb.equal(cb.lower(root.get("username")), normalizedUsername));
                counts.put(actorKey, auditLogRepository.count(userSpec));
                continue;
            }
            if (actorKey.startsWith("id:")) {
                Long id = Long.valueOf(actorKey.substring(3));
                Specification<AuditLog> userSpec = baseSpec.and((root, query, cb) ->
                        cb.equal(root.get("user").get("id"), id));
                counts.put(actorKey, auditLogRepository.count(userSpec));
            }
        }
        return counts;
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveDisplayUsername(AuditLog auditLog) {
        return resolveDisplayUsername(auditLog, Map.of());
    }

    private String resolveDisplayUsername(AuditLog auditLog, Map<Long, String> staffActorLabels) {
        Long userId = auditLog.getUser() != null ? auditLog.getUser().getId() : null;
        if (userId != null && staffActorLabels.containsKey(userId)
                && StringUtils.hasText(staffActorLabels.get(userId))) {
            return staffActorLabels.get(userId);
        }

        String stored = auditLog.getUsername();
        if (StringUtils.hasText(stored)) {
            if ("anonymous".equalsIgnoreCase(stored)) {
                return "System";
            }
            // Never surface client emails as the HIPAA actor (e.g. historical Stripe webhook rows).
            // Staff emails that match the linked user are still shown when loginIdentifier is unavailable.
            if (stored.contains("@") && !isStaffActorEmail(auditLog, stored)) {
                String mrn = resolveClientMrnFromAuditContext(auditLog);
                if (StringUtils.hasText(mrn)) {
                    return mrn;
                }
                return HipaaAuditLabels.clientActorFallback();
            }
            // Historical rows mistakenly stored Spring Security authId as username.
            if (!isAuthIdUsername(stored)) {
                return stored;
            }
        }
        // System/portal rows with no username but a linked client → MRN only.
        if (auditLog.getUser() == null && auditLog.getClient() != null
                && StringUtils.hasText(auditLog.getClient().getClientId())) {
            return auditLog.getClient().getClientId();
        }
        if (auditLog.getUser() != null) {
            String fromLinkedUser = resolveStaffActorLabelFromUser(auditLog.getUser());
            if (StringUtils.hasText(fromLinkedUser)) {
                return fromLinkedUser;
            }
            if (auditLog.getUser().getId() != null) {
                return "User #" + auditLog.getUser().getId();
            }
        }
        return "System";
    }

    private Map<Long, String> resolveStaffActorLabels(List<AuditLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return Map.of();
        }
        Set<Long> userIds = logs.stream()
                .map(AuditLog::getUser)
                .filter(Objects::nonNull)
                .map(User::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> labels = new HashMap<>();
        for (UserRepository.AuditActorView actor : userRepository.findAuditActorsByIdIn(userIds)) {
            String label = resolveStaffActorLabel(actor);
            if (StringUtils.hasText(label)) {
                labels.put(actor.getId(), label);
            }
        }
        return labels;
    }

    private static String resolveStaffActorLabel(UserRepository.AuditActorView actor) {
        if (actor == null) {
            return null;
        }
        if (StringUtils.hasText(actor.getLoginIdentifier())) {
            return actor.getLoginIdentifier().trim();
        }
        if (StringUtils.hasText(actor.getEmail())) {
            return actor.getEmail().trim();
        }
        if (StringUtils.hasText(actor.getFullName())) {
            return actor.getFullName().trim();
        }
        return null;
    }

    private static String resolveStaffActorLabelFromUser(User user) {
        if (user == null) {
            return null;
        }
        try {
            if (user.getAuthIdentity() != null && StringUtils.hasText(user.getAuthIdentity().getLoginIdentifier())) {
                return user.getAuthIdentity().getLoginIdentifier().trim();
            }
        } catch (Exception ignored) {
            // Cross-schema lazy load can fail; fall back to denormalized fields.
        }
        if (StringUtils.hasText(user.getEmail())) {
            return user.getEmail().trim();
        }
        if (StringUtils.hasText(user.getFullName())) {
            return user.getFullName().trim();
        }
        return null;
    }

    /** True when username was never set, or was wrongly set to Spring Security authId. */
    private static boolean needsActorUsernameNormalization(String username) {
        if (!StringUtils.hasText(username)) {
            return true;
        }
        return isAuthIdUsername(username);
    }

    /** AuthPrincipal#getUsername() returns String.valueOf(authId) — digits only. */
    private static boolean isAuthIdUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        String trimmed = username.trim();
        if (trimmed.length() > 18) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (!Character.isDigit(trimmed.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** True when stored email is the staff user on this row (safe to show as actor). */
    private boolean isStaffActorEmail(AuditLog auditLog, String email) {
        if (auditLog.getUser() == null || !StringUtils.hasText(email)) {
            return false;
        }
        String staffEmail = auditLog.getUser().getEmail();
        return StringUtils.hasText(staffEmail) && staffEmail.equalsIgnoreCase(email);
    }

    /**
     * Resolve MRN from linked client, details payload, or resource id (including soft-deleted clients).
     */
    private String resolveClientMrnFromAuditContext(AuditLog auditLog) {
        if (auditLog.getClient() != null && StringUtils.hasText(auditLog.getClient().getClientId())) {
            return auditLog.getClient().getClientId();
        }
        String fromDetails = extractClientMrnFromDetails(auditLog.getDetails());
        if (StringUtils.hasText(fromDetails)) {
            return fromDetails;
        }
        Long clientId = resolveClientPkFromAuditContext(auditLog);
        if (clientId == null) {
            return null;
        }
        try {
            return clientRepository.findByIdIncludingDeleted(clientId)
                    .map(HipaaAuditLabels::clientActor)
                    .orElse(null);
        } catch (Exception e) {
            log.debug("Could not resolve MRN for audit display clientId={}: {}", clientId, e.getMessage());
            return null;
        }
    }

    private Long resolveClientPkFromAuditContext(AuditLog auditLog) {
        if (auditLog.getClient() != null && auditLog.getClient().getId() != null) {
            return auditLog.getClient().getId();
        }
        Long fromDetails = extractClientIdFromDetails(auditLog.getDetails());
        if (fromDetails != null) {
            return fromDetails;
        }
        if (!StringUtils.hasText(auditLog.getResourceId())) {
            return null;
        }
        String resourceType = auditLog.getResourceType();
        if (!"client".equalsIgnoreCase(resourceType)
                && !"client_portal".equalsIgnoreCase(resourceType)
                && !"clients".equalsIgnoreCase(resourceType)) {
            return null;
        }
        try {
            return Long.parseLong(auditLog.getResourceId().trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long extractClientIdFromDetails(String details) {
        if (!StringUtils.hasText(details)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(details);
            if (root != null && root.isObject()) {
                for (String key : List.of("internal_client_id", "internalClientId", "clientPk")) {
                    JsonNode node = root.get(key);
                    if (node != null && node.isNumber()) {
                        return node.asLong();
                    }
                    if (node != null && node.isTextual() && node.asText().chars().allMatch(Character::isDigit)) {
                        return Long.parseLong(node.asText());
                    }
                }
            }
        } catch (Exception ignored) {
            // fall through to kv / regex forms
        }
        Matcher matcher = Pattern.compile(
                        "(?i)\\b(?:internal_client_id|internalClientId|clientPk)\\s*[:=]\\s*\"?(\\d+)\"?\\b")
                .matcher(details);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        // Legacy: numeric clientId=2256 (not MRN strings like CL-WEB-...)
        Matcher legacy = Pattern.compile("(?i)\\bclientId\\s*[:=]\\s*(\\d+)\\b").matcher(details);
        if (legacy.find()) {
            try {
                return Long.parseLong(legacy.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String extractClientMrnFromDetails(String details) {
        if (!StringUtils.hasText(details)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(details);
            if (root != null && root.isObject()) {
                for (String key : List.of("client_mrn", "clientMrn")) {
                    JsonNode mrn = root.get(key);
                    if (mrn != null && !mrn.isNull() && StringUtils.hasText(mrn.asText())) {
                        return mrn.asText();
                    }
                }
                // Older payloads sometimes stored MRN under clientId when it was a string.
                JsonNode clientIdNode = root.get("clientId");
                if (clientIdNode != null && clientIdNode.isTextual() && StringUtils.hasText(clientIdNode.asText())
                        && !clientIdNode.asText().chars().allMatch(Character::isDigit)) {
                    return clientIdNode.asText();
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        String kv = extractKv(details, "client_mrn");
        if (!StringUtils.hasText(kv)) {
            kv = extractKv(details, "clientMrn");
        }
        return StringUtils.hasText(kv) ? kv : null;
    }

    private String buildActorKey(String displayUsername, Long userId) {
        String normalizedUsername = normalizeUsername(displayUsername);
        if (normalizedUsername != null) {
            return "u:" + normalizedUsername;
        }
        if (userId != null) {
            return "id:" + userId;
        }
        return null;
    }

    private String toHumanReadableDetails(AuditLog auditLog) {
        String trimmed = auditLog.getDetails() != null ? auditLog.getDetails().trim() : null;
        if (!StringUtils.hasText(trimmed)) {
            return null;
        }

        if ("data_exported".equalsIgnoreCase(auditLog.getAction())) {
            return trimmed;
        }

        if (trimmed.contains("source=global_activity_aspect")) {
            return parseGlobalActivityDetails(trimmed);
        }

        String parsed = parseDetailsText(trimmed);
        if (StringUtils.hasText(parsed)) {
            return parsed;
        }
        return buildFallbackDetails(auditLog);
    }

    private String buildFallbackDetails(AuditLog auditLog) {
        String action = humanizeToken(auditLog.getAction());
        String resourceType = humanizeToken(auditLog.getResourceType());
        String resourceId = auditLog.getResourceId();

        if (StringUtils.hasText(resourceType) && StringUtils.hasText(resourceId)) {
            return action + " on " + resourceType + " (ID " + resourceId + ")";
        }
        if (StringUtils.hasText(resourceType)) {
            return action + " on " + resourceType;
        }
        return action;
    }

    private String parseDetailsText(String rawDetails) {
        if (!StringUtils.hasText(rawDetails)) {
            return null;
        }
        String trimmed = rawDetails.trim();
        String globalActivitySummary = parseGlobalActivityDetails(trimmed);
        if (StringUtils.hasText(globalActivitySummary)) {
            return globalActivitySummary;
        }

        try {
            JsonNode root = objectMapper.readTree(trimmed);
            if (root == null || root.isNull()) {
                return null;
            }
            if (!root.isObject()) {
                return root.asText(trimmed);
            }

            List<String> fragments = new ArrayList<>();
            JsonNode summary = root.get("details");
            if (summary != null && !summary.isNull() && StringUtils.hasText(summary.asText())) {
                fragments.add(summary.asText());
            }

            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                if ("details".equalsIgnoreCase(key)) {
                    continue;
                }
                String value = summarizeDetailValue(field.getValue());
                if (StringUtils.hasText(value)) {
                    fragments.add(humanizeToken(key) + ": " + value);
                }
            }

            return fragments.isEmpty() ? trimmed : String.join("; ", fragments);
        } catch (Exception ignored) {
            return summarizePlainDetails(trimmed);
        }
    }
    private String parseGlobalActivityDetails(String details) {
        if (!StringUtils.hasText(details) || !details.contains("source=global_activity_aspect")) {
            return null;
        }

        String method = extractKv(details, "method");
        String path = extractKv(details, "path");
        String status = extractKv(details, "status");
        String duration = extractKv(details, "durationMs");
        String extra = extractTrailingDetails(details);

        try {
            ObjectNode root = objectMapper.createObjectNode();
            if (StringUtils.hasText(method)) root.put("method", method);
            if (StringUtils.hasText(path)) root.put("path", path);
            if (StringUtils.hasText(status)) root.put("status", status);
            if (StringUtils.hasText(duration)) root.put("durationMs", duration);
            
            if (StringUtils.hasText(extra)) {
                try {
                    JsonNode extraNode = objectMapper.readTree(extra);
                    root.set("payload", extraNode);
                } catch (Exception e) {
                    root.put("payload", extra);
                }
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return null;
        }
    }

    private String summarizePlainDetails(String details) {
        if (!StringUtils.hasText(details)) {
            return null;
        }
        String compact = details.replaceAll("\\s+", " ").trim();
        if (compact.length() > 220) {
            return compact.substring(0, 217) + "...";
        }
        return compact;
    }

    private String summarizeGlobalExtra(String extra) {
        if (!StringUtils.hasText(extra)) {
            return null;
        }
        String normalized = extra.trim();
        if (normalized.isBlank() || "null".equalsIgnoreCase(normalized)) {
            return null;
        }
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            String inside = normalized.substring(1, normalized.length() - 1).trim();
            if (inside.isBlank()) {
                return null;
            }
            List<String> meaningful = Arrays.stream(inside.split(","))
                    .map(String::trim)
                    .filter(token -> !token.isBlank())
                    .filter(token -> !"null".equalsIgnoreCase(token))
                    .toList();
            if (meaningful.isEmpty()) {
                return null;
            }
            String preview = meaningful.stream().limit(3).collect(Collectors.joining(", "));
            return meaningful.size() > 3
                    ? "Args: " + preview + " +" + (meaningful.size() - 3) + " more"
                    : "Args: " + preview;
        }
        if (normalized.length() > 120) {
            return normalized.substring(0, 117) + "...";
        }
        return normalized;
    }

    private String extractKv(String input, String key) {
        if (!StringUtils.hasText(input) || !StringUtils.hasText(key)) {
            return null;
        }
        Pattern pattern = Pattern.compile("(?:^|,\\s*)" + Pattern.quote(key) + "=([^,]+)");
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String extractTrailingDetails(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        Matcher matcher = DETAILS_TAIL_PATTERN.matcher(input);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String summarizeDetailValue(JsonNode valueNode) {
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        if (valueNode.isTextual() || valueNode.isNumber() || valueNode.isBoolean()) {
            return valueNode.asText();
        }
        if (valueNode.isArray()) {
            int size = valueNode.size();
            if (size == 0) {
                return "none";
            }
            List<String> samples = new ArrayList<>();
            for (int i = 0; i < Math.min(size, 3); i++) {
                JsonNode child = valueNode.get(i);
                if (child != null && (child.isTextual() || child.isNumber() || child.isBoolean())) {
                    samples.add(child.asText());
                }
            }
            return samples.isEmpty() ? size + " item(s)" : String.join(", ", samples) + (size > 3 ? " +" + (size - 3) + " more" : "");
        }
        if (valueNode.isObject()) {
            JsonNode id = valueNode.get("id");
            JsonNode name = valueNode.get("name");
            if (name != null && !name.isNull() && StringUtils.hasText(name.asText())) {
                if (id != null && !id.isNull()) {
                    return name.asText() + " (ID " + id.asText() + ")";
                }
                return name.asText();
            }
            if (id != null && !id.isNull()) {
                return "ID " + id.asText();
            }
            return "object";
        }
        return valueNode.toString();
    }

    private String humanizeToken(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        String[] parts = raw.trim().toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(switch (part) {
                case "hipaa" -> "HIPAA";
                case "phi" -> "PHI";
                case "api" -> "API";
                case "ip" -> "IP";
                case "id" -> "ID";
                case "mrn" -> "MRN";
                case "sso" -> "SSO";
                default -> Character.toUpperCase(part.charAt(0)) + part.substring(1);
            });
        }
        return words.isEmpty() ? raw : String.join(" ", words);
    }

    private String convertToJson(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            log.warn("Failed to convert details to JSON", e);
            return "{}";
        }
    }

    private String csvCell(Object value) {
        String text = value != null ? value.toString() : "";
        String stripped = text.stripLeading();
        if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private void recordAuditWriteFailure(String operation, Exception failure) {
        MeterRegistry registry = meterRegistryProvider != null ? meterRegistryProvider.getIfAvailable() : null;
        if (registry != null) {
            registry.counter("clinical.audit.write.failures",
                    "component", "audit_log_service",
                    "operation", operation,
                    "error", failure.getClass().getSimpleName()).increment();
        }
    }
}
