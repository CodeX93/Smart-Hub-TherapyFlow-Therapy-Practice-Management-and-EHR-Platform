package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.session.dto.CreateSessionNoteAiTemplateRequest;
import com.smart.therapy.flow.session.dto.UpdateSessionNoteAiTemplateRequest;
import com.smart.therapy.flow.session.entity.SessionNoteAiTemplate;
import com.smart.therapy.flow.session.repository.SessionNoteAiTemplateRepository;
import com.smart.therapy.flow.session.service.SessionNoteAiTemplateService;
import com.smart.therapy.flow.common.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionNoteAiTemplateServiceTest {

    @Mock
    private SessionNoteAiTemplateRepository templateRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private SessionNoteAiTemplateService service;

    private AuthPrincipal principal;
    private User user;
    private static final Long AUTH_ID = 145L;

    @BeforeEach
    void setUp() {
        principal = org.mockito.Mockito.mock(AuthPrincipal.class);
        lenient().when(principal.getAuthId()).thenReturn(AUTH_ID);
        user = TestDataFactory.createTestTherapist();
        user.setId(7L);
        lenient().when(currentUserService.requireCurrentUser(principal)).thenReturn(user);
    }

    @Test
    void createTemplate_persistsOwnedTemplate() {
        CreateSessionNoteAiTemplateRequest request = new CreateSessionNoteAiTemplateRequest();
        request.setName("CBT Template");
        request.setInstructions("Use CBT format with homework.");

        when(templateRepository.save(any(SessionNoteAiTemplate.class))).thenAnswer(invocation -> {
            SessionNoteAiTemplate saved = invocation.getArgument(0);
            saved.setId(12L);
            return saved;
        });

        var response = service.createTemplate(request, principal);

        assertThat(response.getId()).isEqualTo(12L);
        assertThat(response.getName()).isEqualTo("CBT Template");
        assertThat(response.getInstructions()).isEqualTo("Use CBT format with homework.");
        verify(templateRepository).save(any(SessionNoteAiTemplate.class));
    }

    @Test
    void createTemplate_rejectsBlankInstructions() {
        CreateSessionNoteAiTemplateRequest request = new CreateSessionNoteAiTemplateRequest();
        request.setName("CBT Template");
        request.setInstructions("   ");

        assertThatThrownBy(() -> service.createTemplate(request, principal))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void resolveInstructions_returnsOwnedTemplateText() {
        SessionNoteAiTemplate template = SessionNoteAiTemplate.builder()
                .name("CBT")
                .instructions("Instruction body")
                .build();
        template.setId(3L);
        template.setCreatedBy(AUTH_ID);

        when(templateRepository.findByIdAndCreatedByAndIsDeletedFalse(3L, AUTH_ID))
                .thenReturn(Optional.of(template));

        assertThat(service.resolveInstructions(3L, principal)).isEqualTo("Instruction body");
    }

    @Test
    void resolveInstructions_throwsWhenNotOwned() {
        when(templateRepository.findByIdAndCreatedByAndIsDeletedFalse(99L, AUTH_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveInstructions(99L, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listTemplates_returnsOnlyCurrentUserTemplates() {
        SessionNoteAiTemplate template = SessionNoteAiTemplate.builder()
                .name("A")
                .instructions("x")
                .build();
        template.setId(1L);

        when(templateRepository.findByCreatedByAndIsDeletedFalseOrderByLastUsedAtDescUpdatedAtDesc(AUTH_ID))
                .thenReturn(List.of(template));

        assertThat(service.listTemplates(principal)).hasSize(1);
    }

    @Test
    void updateTemplate_updatesNameAndInstructions() {
        SessionNoteAiTemplate template = SessionNoteAiTemplate.builder()
                .name("Old")
                .instructions("Old body")
                .build();
        template.setId(4L);
        template.setCreatedBy(AUTH_ID);

        when(templateRepository.findByIdAndCreatedByAndIsDeletedFalse(4L, AUTH_ID))
                .thenReturn(Optional.of(template));
        when(templateRepository.save(template)).thenReturn(template);

        UpdateSessionNoteAiTemplateRequest request = new UpdateSessionNoteAiTemplateRequest();
        request.setName("New");
        request.setInstructions("New body");

        var response = service.updateTemplate(4L, request, principal);

        assertThat(response.getName()).isEqualTo("New");
        assertThat(response.getInstructions()).isEqualTo("New body");
    }
}
