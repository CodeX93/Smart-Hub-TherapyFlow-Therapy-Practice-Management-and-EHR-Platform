package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.subscription.entity.AppFeature;
import com.smart.therapy.flow.subscription.entity.FeatureUsage;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.superadmin.dto.SuperAdminUsageResponse;
import com.smart.therapy.flow.superadmin.service.SuperAdminUsageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuperAdminUsageService tests")
class SuperAdminUsageServiceTest {

    @Mock
    private OrganisationRepository organisationRepository;

    @Mock
    private SubscriptionFeatureService subscriptionFeatureService;

    @InjectMocks
    private SuperAdminUsageService superAdminUsageService;

    @Test
    @DisplayName("Returns ORG_NOT_FOUND when organisation is missing")
    void shouldThrowOrgNotFound() {
        when(organisationRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> superAdminUsageService.getUsage(99L, "2026-03", null))
                .isInstanceOf(StoryApiException.class)
                .satisfies(ex -> {
                    StoryApiException e = (StoryApiException) ex;
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(e.getCode()).isEqualTo("ORG_NOT_FOUND");
                });
    }

    @Test
    @DisplayName("Returns INVALID_PERIOD for malformed period")
    void shouldValidatePeriodFormat() {
        when(organisationRepository.existsById(99L)).thenReturn(true);

        assertThatThrownBy(() -> superAdminUsageService.getUsage(99L, "03-2026", null))
                .isInstanceOf(StoryApiException.class)
                .satisfies(ex -> {
                    StoryApiException e = (StoryApiException) ex;
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo("INVALID_PERIOD");
                });
    }

    @Test
    @DisplayName("Builds metrics response for requested period")
    void shouldBuildUsageResponse() {
        when(organisationRepository.existsById(99L)).thenReturn(true);

        AppFeature feature = new AppFeature();
        feature.setCode("AI_REPORTS_PER_MONTH");

        FeatureUsage usage = new FeatureUsage();
        usage.setFeature(feature);
        usage.setUsageCount(500L);
        usage.setPeriodStart(Instant.parse("2026-03-01T00:00:00Z"));

        when(subscriptionFeatureService.listUsageForPeriod(99L, YearMonth.of(2026, 3), null))
                .thenReturn(List.of(usage));
        when(subscriptionFeatureService.getEffectiveLimit(eq(99L), org.mockito.ArgumentMatchers.isNull(String.class), any(), eq(Instant.parse("2026-03-01T00:00:00Z"))))
                .thenReturn(500);

        SuperAdminUsageResponse response = superAdminUsageService.getUsage(99L, "2026-03", null);

        assertThat(response.getOrganisationId()).isEqualTo(99L);
        assertThat(response.getPeriod()).isEqualTo("2026-03");
        assertThat(response.getTargetKey()).isNull();
        assertThat(response.getMetrics()).hasSize((int) java.util.Arrays.stream(
                com.smart.therapy.flow.subscription.feature.CoreFeature.values()).filter(
                com.smart.therapy.flow.subscription.feature.CoreFeature::isLimitType).count());
        assertThat(response.getMetrics()).allSatisfy(metric -> assertThat(metric.getLimit()).isEqualTo(500));
        assertThat(response.getMetrics()).filteredOn(m -> !"AI_REPORTS_PER_MONTH".equals(m.getFeatureKey()))
                .allSatisfy(metric -> assertThat(metric.getUsed()).isZero());
        assertThat(response.getMetrics().stream()
                .filter(m -> "AI_REPORTS_PER_MONTH".equals(m.getFeatureKey()))
                .findFirst()
                .orElseThrow()
                .getUsed()).isEqualTo(500L);
    }
}
