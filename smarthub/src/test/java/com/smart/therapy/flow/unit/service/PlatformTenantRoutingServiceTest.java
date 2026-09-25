package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.superadmin.dto.PlatformTenantRoutingRequest;
import com.smart.therapy.flow.superadmin.dto.PlatformTenantRoutingResponse;
import com.smart.therapy.flow.superadmin.entity.PlatformTenantRoutingSettings;
import com.smart.therapy.flow.superadmin.repository.PlatformTenantRoutingSettingsRepository;
import com.smart.therapy.flow.superadmin.service.PlatformTenantRoutingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformTenantRoutingService tests")
class PlatformTenantRoutingServiceTest {

    @Mock
    private PlatformTenantRoutingSettingsRepository repository;

    @Mock
    private OrganisationRepository organisationRepository;

    @InjectMocks
    private PlatformTenantRoutingService service;

    @Test
    @DisplayName("Rejects invalid pathPrefix")
    void shouldRejectInvalidPathPrefix() {
        PlatformTenantRoutingRequest request = new PlatformTenantRoutingRequest();
        request.setEmailAutoRouting(true);
        request.setPathBasedRouting(true);
        request.setPathPrefix("org");
        request.setOrgIdentifier("slug");

        assertThatThrownBy(() -> service.upsert(request, 10L))
                .isInstanceOf(StoryApiException.class)
                .satisfies(ex -> {
                    StoryApiException e = (StoryApiException) ex;
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo("VALIDATION_ERROR");
                });
    }

    @Test
    @DisplayName("Rejects invalid orgIdentifier")
    void shouldRejectInvalidOrgIdentifier() {
        PlatformTenantRoutingRequest request = new PlatformTenantRoutingRequest();
        request.setEmailAutoRouting(true);
        request.setPathBasedRouting(true);
        request.setPathPrefix("/org");
        request.setOrgIdentifier("name");

        assertThatThrownBy(() -> service.upsert(request, 10L))
                .isInstanceOf(StoryApiException.class)
                .satisfies(ex -> {
                    StoryApiException e = (StoryApiException) ex;
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo("VALIDATION_ERROR");
                });
    }

    @Test
    @DisplayName("Rejects when no organisations exist")
    void shouldRejectWithoutOrgs() {
        PlatformTenantRoutingRequest request = new PlatformTenantRoutingRequest();
        request.setEmailAutoRouting(true);
        request.setPathBasedRouting(true);
        request.setPathPrefix("/org");
        request.setOrgIdentifier("slug");

        when(organisationRepository.count()).thenReturn(0L);

        assertThatThrownBy(() -> service.upsert(request, 10L))
                .isInstanceOf(StoryApiException.class)
                .satisfies(ex -> {
                    StoryApiException e = (StoryApiException) ex;
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo("VALIDATION_ERROR");
                });
    }

    @Test
    @DisplayName("Upserts routing settings and returns response")
    void shouldUpsertRoutingSettings() {
        PlatformTenantRoutingRequest request = new PlatformTenantRoutingRequest();
        request.setEmailAutoRouting(true);
        request.setPathBasedRouting(true);
        request.setPathPrefix("/org");
        request.setOrgIdentifier("slug");

        when(organisationRepository.count()).thenReturn(5L);
        when(repository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            PlatformTenantRoutingSettings row = invocation.getArgument(0);
            row.setId(1L);
            row.setCreatedAt(Instant.parse("2026-03-26T00:00:00Z"));
            row.setUpdatedAt(Instant.parse("2026-03-26T00:00:00Z"));
            return row;
        });

        PlatformTenantRoutingResponse response = service.upsert(request, 99L);
        assertThat(response).isNotNull();
        assertThat(response.getPathPrefix()).isEqualTo("/org");
        assertThat(response.getOrgIdentifier()).isEqualTo("slug");
        assertThat(response.getEmailAutoRouting()).isTrue();
    }
}

