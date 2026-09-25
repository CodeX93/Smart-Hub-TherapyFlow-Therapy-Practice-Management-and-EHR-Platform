package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.superadmin.dto.SuperAdminDashboardKpiCardsResponse;
import com.smart.therapy.flow.superadmin.entity.PlatformIncident;
import com.smart.therapy.flow.superadmin.repository.PlatformIncidentRepository;
import com.smart.therapy.flow.superadmin.service.SuperAdminDashboardOverviewService;
import com.smart.therapy.flow.superadmin.service.SuperAdminSystemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Super admin dashboard KPI cards")
class SuperAdminDashboardKpiCardsTest {

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Mock
    private SuperAdminSystemService superAdminSystemService;
    @Mock
    private PlatformIncidentRepository platformIncidentRepository;

    @InjectMocks
    private SuperAdminDashboardOverviewService service;

    @Test
    @DisplayName("Splits end users into staff and portal clients and sums them")
    void splitsEndUsersByIdentityType() {
        stubScalarQueries();
        when(namedParameterJdbcTemplate.queryForList(anyString(), any(MapSqlParameterSource.class)))
                .thenReturn(List.of(
                        Map.of("identity_type", "STAFF", "identities", 30L),
                        Map.of("identity_type", "CLIENT", "identities", 166L)));

        SuperAdminDashboardKpiCardsResponse response = service.getKpiCards("UTC");

        assertThat(response.getStaffUsers()).isEqualTo(30L);
        assertThat(response.getPortalClients()).isEqualTo(166L);
        assertThat(response.getTotalEndUsers()).isEqualTo(196L);
    }

    @Test
    @DisplayName("Counts only tenant-scoped identities, so platform super admins are excluded")
    void excludesPlatformIdentities() {
        stubScalarQueries();
        when(namedParameterJdbcTemplate.queryForList(anyString(), any(MapSqlParameterSource.class)))
                .thenReturn(List.of(Map.of("identity_type", "STAFF", "identities", 30L)));

        SuperAdminDashboardKpiCardsResponse response = service.getKpiCards("UTC");

        assertThat(response.getPortalClients()).isZero();
        assertThat(response.getTotalEndUsers()).isEqualTo(30L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(namedParameterJdbcTemplate).queryForList(sqlCaptor.capture(), any(MapSqlParameterSource.class));
        String sql = sqlCaptor.getValue();
        // An inner join on organisations drops identities with no organisation on either side,
        // which is exactly what a platform super admin looks like.
        assertThat(sql).contains("JOIN public.organisations o ON o.id = COALESCE(uo.organisation_id, ai.organisation_id)");
        assertThat(sql).doesNotContain("LEFT JOIN public.organisations");
        assertThat(sql).doesNotContain("o.id IS NULL");
    }

    @Test
    @DisplayName("Reports full availability when no outage is on record")
    void fullAvailabilityWithoutIncidents() {
        stubScalarQueries();
        when(namedParameterJdbcTemplate.queryForList(anyString(), any(MapSqlParameterSource.class)))
                .thenReturn(List.of());

        SuperAdminDashboardKpiCardsResponse response = service.getKpiCards("UTC");

        assertThat(response.getPlatformUptimePercent()).isEqualByComparingTo("100.00");
        assertThat(response.getDowntimeMinutes()).isZero();
        assertThat(response.getUptimeWindowDays()).isEqualTo(30L);
    }

    @Test
    @DisplayName("Burns availability for outage severities only, merging overlapping windows")
    void countsOutageWindowsOnce() {
        stubScalarQueries();
        when(namedParameterJdbcTemplate.queryForList(anyString(), any(MapSqlParameterSource.class)))
                .thenReturn(List.of());
        Instant base = Instant.now().minus(Duration.ofDays(2));
        when(platformIncidentRepository.findOverlapping(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(
                        incident("critical", base, base.plus(Duration.ofHours(2))),
                        // Overlaps the first one: together they are 3 hours of downtime, not 4.
                        incident("major", base.plus(Duration.ofHours(1)), base.plus(Duration.ofHours(3))),
                        // Degraded, not down — recorded but must not cost availability.
                        incident("minor", base.plus(Duration.ofDays(1)), base.plus(Duration.ofDays(1)).plus(Duration.ofHours(5)))));

        SuperAdminDashboardKpiCardsResponse response = service.getKpiCards("UTC");

        assertThat(response.getDowntimeMinutes()).isEqualTo(180L);
        assertThat(response.getIncidentsInWindow()).isEqualTo(3L);
        // 180 minutes out of 30 days = 0.42% unavailable.
        assertThat(response.getPlatformUptimePercent()).isEqualByComparingTo("99.58");
    }

    @Test
    @DisplayName("An unresolved outage keeps burning availability up to now")
    void openIncidentRunsToNow() {
        stubScalarQueries();
        when(namedParameterJdbcTemplate.queryForList(anyString(), any(MapSqlParameterSource.class)))
                .thenReturn(List.of());
        when(platformIncidentRepository.findOverlapping(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(incident("critical", Instant.now().minus(Duration.ofHours(1)), null)));

        SuperAdminDashboardKpiCardsResponse response = service.getKpiCards("UTC");

        assertThat(response.getDowntimeMinutes()).isEqualTo(60L);
        assertThat(response.getPlatformUptimePercent()).isLessThan(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Clips an outage that started before the window to the window itself")
    void clipsOutageToWindow() {
        stubScalarQueries();
        when(namedParameterJdbcTemplate.queryForList(anyString(), any(MapSqlParameterSource.class)))
                .thenReturn(List.of());
        when(platformIncidentRepository.findOverlapping(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(incident("critical", Instant.now().minus(Duration.ofDays(400)), null)));

        SuperAdminDashboardKpiCardsResponse response = service.getKpiCards("UTC");

        assertThat(response.getPlatformUptimePercent()).isEqualByComparingTo("0.00");
        assertThat(response.getDowntimeMinutes()).isEqualTo(30L * 24L * 60L);
    }

    private static PlatformIncident incident(String severity, Instant startedAt, Instant resolvedAt) {
        PlatformIncident incident = new PlatformIncident();
        incident.setSeverity(severity);
        incident.setStartedAt(startedAt);
        incident.setResolvedAt(resolvedAt);
        incident.setStatus(resolvedAt == null ? "open" : "resolved");
        return incident;
    }

    private void stubScalarQueries() {
        when(namedParameterJdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(1L);
        when(namedParameterJdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(BigDecimal.class)))
                .thenReturn(new BigDecimal("300.00"));
        when(platformIncidentRepository.findOverlapping(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
    }
}
