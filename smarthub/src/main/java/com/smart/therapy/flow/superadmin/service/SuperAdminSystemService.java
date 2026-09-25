package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.superadmin.entity.PlatformIncident;
import com.smart.therapy.flow.superadmin.repository.PlatformIncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SuperAdminSystemService {

    private final PlatformIncidentRepository incidentRepository;
    private final Optional<HealthEndpoint> healthEndpoint;
    private final Instant bootAt = Instant.now();

    @Transactional(readOnly = true)
    public Map<String, Object> getSystemHealth() {
        String status = "unknown";
        Object details = Map.of();
        if (healthEndpoint.isPresent()) {
            HealthComponent component = healthEndpoint.get().health();
            status = component.getStatus().getCode().toLowerCase(Locale.ROOT);
            details = extractHealthDetails(component);
        }

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long openIncidents = incidentRepository.findByStatusOrderByStartedAtDesc("open").size();

        return Map.of(
                "status", status,
                "uptimeMs", uptimeMs,
                "bootAt", bootAt,
                "openIncidents", openIncidents,
                "details", details
        );
    }

    @SuppressWarnings("unchecked")
    private Object extractHealthDetails(HealthComponent component) {
        if (component instanceof Health health) {
            return health.getDetails();
        }
        // For composite/system health, expose per-component status details when available.
        try {
            Method getComponents = component.getClass().getMethod("getComponents");
            Object raw = getComponents.invoke(component);
            if (raw instanceof Map<?, ?> components) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : components.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    Object value = entry.getValue();
                    if (value instanceof Health child) {
                        normalized.put(key, Map.of(
                                "status", child.getStatus().getCode().toLowerCase(Locale.ROOT),
                                "details", child.getDetails()
                        ));
                    } else if (value instanceof HealthComponent childComponent) {
                        normalized.put(key, Map.of(
                                "status", childComponent.getStatus().getCode().toLowerCase(Locale.ROOT)
                        ));
                    } else {
                        normalized.put(key, value);
                    }
                }
                return normalized;
            }
        } catch (Exception ignored) {
            // Best effort only; keep endpoint resilient.
        }
        return Map.of();
    }

    @Transactional(readOnly = true)
    public List<PlatformIncident> listIncidents(String status) {
        if (status == null || status.isBlank()) {
            return incidentRepository.findAll();
        }
        return incidentRepository.findByStatusOrderByStartedAtDesc(status.trim().toLowerCase(Locale.ROOT));
    }

    @Transactional
    public PlatformIncident createIncident(String title, String description, String serviceName, String severity) {
        Instant now = Instant.now();
        PlatformIncident incident = PlatformIncident.builder()
                .title(title)
                .description(description)
                .serviceName(serviceName)
                .severity(severity != null && !severity.isBlank() ? severity.toLowerCase(Locale.ROOT) : "minor")
                .status("open")
                .startedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return incidentRepository.save(incident);
    }

    @Transactional
    public PlatformIncident updateIncident(Long id, String status, String severity, String description) {
        PlatformIncident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        if (status != null && !status.isBlank()) {
            String normalized = status.toLowerCase(Locale.ROOT);
            incident.setStatus(normalized);
            if ("resolved".equals(normalized)) {
                incident.setResolvedAt(Instant.now());
            }
        }
        if (severity != null && !severity.isBlank()) {
            incident.setSeverity(severity.toLowerCase(Locale.ROOT));
        }
        if (description != null) {
            incident.setDescription(description);
        }
        incident.setUpdatedAt(Instant.now());
        return incidentRepository.save(incident);
    }
}
