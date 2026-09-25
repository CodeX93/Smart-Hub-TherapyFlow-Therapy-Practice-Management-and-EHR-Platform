package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.superadmin.service.SuperAdminOrganisationQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuperAdminOrganisationIdentifierTest {
    @Mock OrganisationRepository organisations;
    @InjectMocks SuperAdminOrganisationQueryService service;

    @Test
    void resolvesNumericIdWithSurroundingWhitespace() {
        when(organisations.existsById(42L)).thenReturn(true);
        assertThat(service.resolveOrganisationId(" 42 ")).isEqualTo(42L);
        verify(organisations, never()).findBySlug(anyString());
    }

    @Test
    void resolvesTrimmedSlug() {
        when(organisations.findBySlug("acme")).thenReturn(Optional.of(Organisation.builder().id(42L).build()));
        assertThat(service.resolveOrganisationId(" acme ")).isEqualTo(42L);
        verify(organisations, never()).existsById(anyLong());
    }

    @Test
    void rejectsMissingIdentifiersWithoutQuerying() {
        for (String key : new String[]{null, "", "  "}) {
            assertThatThrownBy(() -> service.resolveOrganisationId(key)).isInstanceOfSatisfying(
                    StoryApiException.class, e -> {
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(e.getCode()).isEqualTo("VALIDATION_ERROR");
                    });
        }
        verifyNoInteractions(organisations);
    }

    @Test
    void rejectsUnknownNumericIdWithoutFallingBackToSlug() {
        assertThatThrownBy(() -> service.resolveOrganisationId("42")).isInstanceOfSatisfying(
                StoryApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(e.getCode()).isEqualTo("ORG_NOT_FOUND");
                });
        verify(organisations, never()).findBySlug(anyString());
    }

    @Test
    void rejectsUnknownSlug() {
        assertThatThrownBy(() -> service.resolveOrganisationId("missing")).isInstanceOfSatisfying(
                StoryApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(e.getCode()).isEqualTo("ORG_NOT_FOUND");
                });
    }
}
