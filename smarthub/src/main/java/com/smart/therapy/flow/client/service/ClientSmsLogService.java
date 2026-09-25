package com.smart.therapy.flow.client.service;

import com.smart.therapy.flow.auth.entity.AuditLog;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.client.dto.ClientSmsLogResponse;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.service.AuditService;
import com.smart.therapy.flow.notification.service.SmsNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientSmsLogService {

    private final AuditLogRepository auditLogRepository;
    private final ClientRepository clientRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<ClientSmsLogResponse> getSmsLog(Long clientId, Instant from, Instant to, Pageable pageable, AuthPrincipal requester) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        auditService.recordAuditEvent(builder -> builder
                .action("client_viewed")
                .resourceType("client")
                .resourceId(String.valueOf(clientId))
                .client(client)
                .result("success")
                .hipaaRelevant(true)
                .riskLevel("medium")
                .details("{\"view\":\"sms_log\"}"));

        return auditLogRepository.findAll(buildSpecification(clientId, from, to), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public String exportSmsLogCsv(Long clientId, Instant from, Instant to, AuthPrincipal requester) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        auditService.recordAuditEvent(builder -> builder
                .action("client_sms_log_export")
                .resourceType("client")
                .resourceId(String.valueOf(clientId))
                .client(client)
                .result("success")
                .hipaaRelevant(true)
                .riskLevel("high")
                .details("{\"export\":\"sms_log\"}"));

        List<AuditLog> rows = auditLogRepository.findAll(buildSpecification(clientId, from, to));
        StringBuilder csv = new StringBuilder("id,action,result,resourceId,timestamp,details\n");
        for (AuditLog row : rows) {
            csv.append(row.getId()).append(',')
                    .append(csvEscape(row.getAction())).append(',')
                    .append(csvEscape(row.getResult())).append(',')
                    .append(csvEscape(row.getResourceId())).append(',')
                    .append(row.getTimestamp() != null ? row.getTimestamp() : "").append(',')
                    .append(csvEscape(row.getDetails())).append('\n');
        }
        return csv.toString();
    }

    public String buildExportFileName(Long clientId) {
        String date = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE);
        return "sms-log-client-" + clientId + "-" + date + ".csv";
    }

    private Specification<AuditLog> buildSpecification(Long clientId, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("client").get("id"), clientId));
            predicates.add(cb.equal(root.get("resourceType"), SmsNotificationService.RESOURCE_TYPE_SMS));
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            }
            if (query != null) {
                query.orderBy(cb.desc(root.get("timestamp")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ClientSmsLogResponse toResponse(AuditLog log) {
        return ClientSmsLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .result(log.getResult())
                .resourceId(log.getResourceId())
                .timestamp(log.getTimestamp())
                .details(log.getDetails())
                .build();
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
