package com.smart.therapy.flow.common.service;

import com.smart.therapy.flow.auth.entity.AuditLog;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.system.service.SystemOptionCategories;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.user.entity.UserProfile;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * Convenience builders for audit events. All persistence goes through
 * {@link AuditLogService} so domain code shares one write contract
 * ({@code write} / {@code writeImmediate}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final SystemOptionResolverService systemOptionResolverService;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final AuditLogService auditLogService;

    /**
     * Record an audit event via {@link AuditLogService#write}.
     * Safe from {@code @Transactional(readOnly = true)} methods.
     */
    public void recordAuditEvent(Consumer<AuditLog.AuditLogBuilder> builderConsumer) {
        try {
            var builder = AuditLog.builder();
            builderConsumer.accept(builder);
            AuditLog auditLog = builder.build();
            if (auditLog.getTimestamp() == null) {
                auditLog.setTimestamp(Instant.now());
            }
            auditLogService.write(auditLog);
        } catch (Exception e) {
            recordAuditFailure("record_event", e);
        }
    }

    /**
     * Record an audit event with user id rebound inside the write transaction.
     */
    public void recordAuditEventWithUser(Long userId, Consumer<AuditLog.AuditLogBuilder> builderConsumer) {
        if (userId == null) {
            recordInvalidAuditInvocation("record_event_with_user");
            return;
        }

        try {
            AuditLog.AuditLogBuilder builder = AuditLog.builder();
            User userRef = new User();
            userRef.setId(userId);
            builder.user(userRef);
            builderConsumer.accept(builder);
            AuditLog auditLog = builder.build();
            if (auditLog.getTimestamp() == null) {
                auditLog.setTimestamp(Instant.now());
            }
            auditLogService.write(auditLog);
        } catch (Exception e) {
            recordAuditFailure("record_event_with_user", e);
        }
    }

    /**
     * Record an audit event with user and client ids rebound inside the write transaction.
     */
    public void recordAuditEventWithUserAndClient(Long userId, Long clientId,
            Consumer<AuditLog.AuditLogBuilder> builderConsumer) {
        if (userId == null && clientId == null) {
            recordInvalidAuditInvocation("record_event_with_user_and_client");
            return;
        }

        try {
            var builder = AuditLog.builder();
            if (userId != null) {
                User userRef = new User();
                userRef.setId(userId);
                builder.user(userRef);
            }
            if (clientId != null) {
                Client clientRef = new Client();
                clientRef.setId(clientId);
                builder.client(clientRef);
            }
            builderConsumer.accept(builder);
            AuditLog auditLog = builder.build();
            if (auditLog.getTimestamp() == null) {
                auditLog.setTimestamp(Instant.now());
            }
            auditLogService.write(auditLog);
        } catch (Exception e) {
            recordAuditFailure("record_event_with_user_and_client", e);
        }
    }

    /**
     * Record client creation audit event with state snapshots.
     * Used by ClientCreatedListener for post-commit audit logging.
     */
    public void recordClientCreated(Client client, Long userId, String ipAddress) {
        if (userId == null || client == null) {
            return;
        }

        try {
            String afterState = serializeClientState(client);
            User userRef = new User();
            userRef.setId(userId);
            Client clientRef = new Client();
            clientRef.setId(client.getId());
            AuditLog auditLog = AuditLog.builder()
                    .user(userRef)
                    .client(clientRef)
                    .action("client_created")
                    .result("success")
                    .resourceType("client")
                    .resourceId(String.valueOf(client.getId()))
                    .hipaaRelevant(true)
                    .ipAddress(ipAddress)
                    .beforeState(null)
                    .afterState(afterState)
                    .changedFields(null)
                    .timestamp(Instant.now())
                    .build();
            auditLogService.writeImmediate(auditLog);
        } catch (Exception e) {
            log.error("Failed to record client creation audit event: userId={}, clientId={}",
                    userId, client != null ? client.getId() : null, e);
        }
    }

    /**
     * Serialize client entity state to JSON for audit trail.
     */
    private String serializeClientState(Client client) {
        if (client == null) {
            return null;
        }
        try {
            // Use simple string concatenation to avoid Jackson dependency in common service
            // In production, you might want to use ObjectMapper here
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"id\":").append(client.getId()).append(",");
            sb.append("\"clientMrn\":\"").append(client.getClientId() != null ? client.getClientId() : "").append("\",");
            sb.append("\"status\":\"").append(client.getStatus() != null ? systemOptionResolverService.resolveOptionLabel(SystemOptionCategories.CLIENT_STATUS, client.getStatus()) : "").append("\",");
            sb.append("\"stage\":\"").append(client.getStage() != null ? client.getStage() : "").append("\"");
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to serialize client state for audit trail", e);
            return null;
        }
    }

    /**
     * Record user creation audit event with state snapshots.
     * Used by UserCreatedListener for post-commit audit logging.
     */
    public void recordUserCreated(User user, Long userId, String ipAddress) {
        if (userId == null || user == null) {
            return;
        }

        try {
            String afterState = serializeUserState(user);
            User actorRef = new User();
            actorRef.setId(userId);
            AuditLog auditLog = AuditLog.builder()
                    .user(actorRef)
                    .action("user_created")
                    .result("success")
                    .resourceType("user")
                    .resourceId(String.valueOf(user.getId()))
                    .hipaaRelevant(false)
                    .ipAddress(ipAddress)
                    .beforeState(null)
                    .afterState(afterState)
                    .changedFields(null)
                    .timestamp(Instant.now())
                    .build();
            auditLogService.writeImmediate(auditLog);
        } catch (Exception e) {
            log.error("Failed to record user creation audit event: userId={}, targetUserId={}",
                    userId, user != null ? user.getId() : null, e);
        }
    }

    /**
     * Record user update audit event with before/after state snapshots.
     */
    public void recordUserUpdated(User user, String beforeStateJson, String afterStateJson,
            java.util.List<String> changedFields, Long userId, String ipAddress) {
        if (userId == null || user == null) {
            return;
        }

        try {
            User actorRef = new User();
            actorRef.setId(userId);
            AuditLog auditLog = AuditLog.builder()
                    .user(actorRef)
                    .action("user_updated")
                    .result("success")
                    .resourceType("user")
                    .resourceId(String.valueOf(user.getId()))
                    .hipaaRelevant(false)
                    .ipAddress(ipAddress)
                    .beforeState(beforeStateJson)
                    .afterState(afterStateJson)
                    .changedFields(changedFields != null ? String.join(",", changedFields) : null)
                    .timestamp(Instant.now())
                    .build();
            auditLogService.writeImmediate(auditLog);
        } catch (Exception e) {
            log.error("Failed to record user update audit event: userId={}, targetUserId={}",
                    userId, user != null ? user.getId() : null, e);
        }
    }

    /**
     * Serialize user entity state to JSON for audit trail.
     */
    public String serializeUserState(User user) {
        if (user == null) {
            return null;
        }
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"id\":").append(user.getId()).append(",");
            String loginId = user.getAuthIdentity() != null ? user.getAuthIdentity().getLoginIdentifier() : "";
            sb.append("\"loginIdentifier\":\"").append(escapeJson(loginId)).append("\",");
            sb.append("\"email\":\"").append(escapeJson(user.getEmail())).append("\",");
            sb.append("\"fullName\":\"").append(escapeJson(user.getFullName())).append("\",");
            sb.append("\"isActive\":").append(user.getIsActive() != null ? user.getIsActive() : false).append(",");
            sb.append("\"status\":\"").append(user.getStatus() != null ? user.getStatus().name() : "").append("\",");
            boolean emailVerified = user.getAuthIdentity() != null && Boolean.TRUE.equals(user.getAuthIdentity().getEmailVerified());
            sb.append("\"emailVerified\":").append(emailVerified).append(",");
            String lastLogin = user.getAuthIdentity() != null && user.getAuthIdentity().getLastSuccessfulLogin() != null
                    ? "\"" + user.getAuthIdentity().getLastSuccessfulLogin().toString() + "\"" : "null";
            sb.append("\"lastSuccessfulLogin\":").append(lastLogin);

            Long orgId = TenantContext.getOrganisationId();
            if (user.getAuthIdentity() != null && user.getAuthIdentity().getRoles() != null) {
                String rolesJson = user.getAuthIdentity().getRoles().stream()
                        .filter(ar -> orgId != null && ar.getOrganisation() != null && java.util.Objects.equals(orgId, ar.getOrganisation().getId()))
                        .map(ar -> ar.getRole())
                        .filter(r -> r != null && r.getName() != null)
                        .map(r -> "\"" + escapeJson(r.getName()) + "\"")
                        .reduce((a, b) -> a + "," + b)
                        .orElse("");
                if (!rolesJson.isEmpty()) {
                    sb.append(",\"roles\":[").append(rolesJson).append("]");
                }
            }
            
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to serialize user state for audit: userId={}", user.getId(), e);
            return "{\"error\":\"serialization_failed\"}";
        }
    }

    /**
     * Record user profile creation audit event with state snapshots.
     * Joins the caller's transaction (must not use REQUIRES_NEW): nested audit inserts with
     * a user FK while the outer TX holds the users row lock can idle-in-transaction deadlock.
     */
    @Transactional
    public void recordUserProfileCreated(UserProfile profile, Long userId, String ipAddress) {
        if (userId == null || profile == null) {
            return;
        }

        try {
            userRepository.findById(userId).ifPresent(actor -> {
                try {
                    String afterState = serializeUserProfileState(profile);

                    AuditLog auditLog = AuditLog.builder()
                            .user(actor)
                            .action("user_profile_created")
                            .result("success")
                            .resourceType("user_profile")
                            .resourceId(String.valueOf(profile.getId()))
                            .hipaaRelevant(false)
                            .ipAddress(ipAddress)
                            .beforeState(null)
                            .afterState(afterState)
                            .changedFields(null)
                            .timestamp(Instant.now())
                            .build();

                    auditLogRepository.save(auditLog);
                } catch (Exception e) {
                    log.error("Failed to record user profile creation audit event for user: {}", userId, e);
                }
            });
        } catch (Exception e) {
            log.error("Failed to record user profile creation audit event: userId={}, profileId={}",
                    userId, profile != null ? profile.getId() : null, e);
        }
    }

    /**
     * Record user profile update audit event with before/after state snapshots.
     */
    public void recordUserProfileUpdated(UserProfile profile, String beforeStateJson, String afterStateJson,
            java.util.List<String> changedFields, Long userId, String ipAddress) {
        if (userId == null || profile == null) {
            return;
        }

        try {
            User actorRef = new User();
            actorRef.setId(userId);
            AuditLog auditLog = AuditLog.builder()
                    .user(actorRef)
                    .action("user_profile_updated")
                    .result("success")
                    .resourceType("user_profile")
                    .resourceId(String.valueOf(profile.getId()))
                    .hipaaRelevant(false)
                    .ipAddress(ipAddress)
                    .beforeState(beforeStateJson)
                    .afterState(afterStateJson)
                    .changedFields(changedFields != null ? String.join(",", changedFields) : null)
                    .timestamp(Instant.now())
                    .build();
            auditLogService.write(auditLog);
        } catch (Exception e) {
            log.error("Failed to record user profile update audit event: userId={}, profileId={}",
                    userId, profile != null ? profile.getId() : null, e);
        }
    }

    /**
     * Serialize user profile entity state to JSON for audit trail.
     */
    public String serializeUserProfileState(UserProfile profile) {
        if (profile == null) {
            return null;
        }
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"id\":").append(profile.getId()).append(",");
            sb.append("\"userId\":").append(profile.getUser() != null ? profile.getUser().getId() : "null").append(",");
            sb.append("\"licenseNumber\":\"").append(escapeJson(profile.getLicenseNumber())).append("\",");
            sb.append("\"licenseType\":\"").append(escapeJson(profile.getLicenseType())).append("\",");
            sb.append("\"licenseState\":\"").append(escapeJson(profile.getLicenseState())).append("\",");
            sb.append("\"licenseStatus\":\"").append(profile.getLicenseStatus() != null ? profile.getLicenseStatus().name() : "").append("\",");
            sb.append("\"maxClientsPerDay\":").append(profile.getMaxClientsPerDay() != null ? profile.getMaxClientsPerDay() : "null").append(",");
            sb.append("\"sessionDuration\":").append(profile.getSessionDuration() != null ? profile.getSessionDuration() : "null").append(",");
            sb.append("\"availabilityStatus\":\"").append(profile.getAvailabilityStatus() != null ? profile.getAvailabilityStatus().name() : "").append("\",");
            sb.append("\"timezone\":\"").append(escapeJson(profile.getTimezone())).append("\",");
            sb.append("\"yearsOfExperience\":").append(profile.getYearsOfExperience() != null ? profile.getYearsOfExperience() : "null");
            
            // Specializations (names only)
            if (profile.getSpecializations() != null && !profile.getSpecializations().isEmpty()) {
                sb.append(",\"specializations\":[");
                String specsJson = profile.getSpecializations().stream()
                        .map(spec -> "\"" + escapeJson(spec.getSpecialization()) + "\"")
                        .reduce((a, b) -> a + "," + b)
                        .orElse("");
                sb.append(specsJson);
                sb.append("]");
            }
            
            // Languages (names only)
            if (profile.getLanguages() != null && !profile.getLanguages().isEmpty()) {
                sb.append(",\"languages\":[");
                String langsJson = profile.getLanguages().stream()
                        .map(lang -> "\"" + escapeJson(lang.getLanguage()) + "\"")
                        .reduce((a, b) -> a + "," + b)
                        .orElse("");
                sb.append(langsJson);
                sb.append("]");
            }
            
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to serialize user profile state for audit: profileId={}", profile.getId(), e);
            return "{\"error\":\"serialization_failed\"}";
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void recordAuditFailure(String operation, Exception failure) {
        MeterRegistry registry = meterRegistryProvider != null ? meterRegistryProvider.getIfAvailable() : null;
        if (registry != null) {
            registry.counter("clinical.audit.write.failures",
                    "component", "audit_service",
                    "operation", operation,
                    "error", failure.getClass().getSimpleName()).increment();
        }
        log.error("Audit write failed: operation={}, errorType={}",
                operation, failure.getClass().getSimpleName());
    }

    private void recordInvalidAuditInvocation(String operation) {
        MeterRegistry registry = meterRegistryProvider != null ? meterRegistryProvider.getIfAvailable() : null;
        if (registry != null) {
            registry.counter("clinical.audit.invalid.invocations",
                    "component", "audit_service",
                    "operation", operation).increment();
        }
        log.warn("Audit event skipped because required identifiers were absent: operation={}", operation);
    }
}

