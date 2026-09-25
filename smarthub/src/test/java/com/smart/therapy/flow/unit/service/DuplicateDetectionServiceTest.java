package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.dto.DuplicatesResponse;
import com.smart.therapy.flow.client.dto.MarkDuplicateRequest;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientService;
import com.smart.therapy.flow.client.service.DuplicateDetectionService;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DuplicateDetectionService Unit Tests")
class DuplicateDetectionServiceTest {
    private User therapist;
    private User admin;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientService clientService;

    @Mock
    private CurrentUserService currentUserService;

    @Spy
    private PermissionChecker permissionChecker = new PermissionChecker();

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DuplicateDetectionService duplicateDetectionService;

    private AuthPrincipal adminPrincipal;
    private AuthPrincipal therapistPrincipal;
    private Client client1;
    private Client client2;

    @BeforeEach
    void setUp() {
        admin = TestDataFactory.createTestAdmin();
        therapist = TestDataFactory.createTestTherapist();
        adminPrincipal = TestDataFactory.createAuthPrincipal(admin, "ROLE_ADMIN", "CLIENT_VIEW_ALL");
        therapistPrincipal = TestDataFactory.createAuthPrincipal(therapist);

        client1 = TestDataFactory.createTestClient();
        client1.setId(1L);
        client1.setFullName("John Doe");
        // Note: Email is now stored in normalized ClientContact entity, not directly on Client
        client1.setIsDuplicate(false);

        client2 = TestDataFactory.createTestClient();
        client2.setId(2L);
        client2.setFullName("John Doe");
        // Note: Email is now stored in normalized ClientContact entity, not directly on Client
        client2.setIsDuplicate(false);
    }

    @Test
    @DisplayName("Should detect duplicates successfully when admin")
    void shouldDetectDuplicatesSuccessfullyWhenAdmin() {
        // Arrange
        when(clientRepository.findAll()).thenReturn(List.of(client1, client2));

        // Act
        DuplicatesResponse response = duplicateDetectionService.detectDuplicates(adminPrincipal);

        // Assert
        assertThat(response).isNotNull();
        verify(clientRepository).findAll();
    }

    @Test
    @DisplayName("Should throw ForbiddenException when non-admin detects duplicates")
    void shouldThrowExceptionWhenNonAdminDetectsDuplicates() {
        // Act & Assert
        assertThatThrownBy(() -> duplicateDetectionService.detectDuplicates(therapistPrincipal))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Insufficient permissions to access duplicate detection");

        verify(clientRepository, never()).findAll();
    }

    @Test
    @DisplayName("Should mark duplicate successfully")
    void shouldMarkDuplicateSuccessfully() {
        when(userRepository.findById(any())).thenReturn(Optional.of(admin));
        when(currentUserService.requireCurrentUser(adminPrincipal)).thenReturn(admin);
        // Arrange
        Long clientId = 2L;
        Long primaryClientId = 1L;
        MarkDuplicateRequest request = new MarkDuplicateRequest();
        request.setDuplicateOfClientId(primaryClientId);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client2));
        when(clientRepository.findById(primaryClientId)).thenReturn(Optional.of(client1));
        when(clientRepository.save(any(Client.class))).thenReturn(client2);

        // Act
        duplicateDetectionService.markDuplicate(clientId, request, adminPrincipal);

        // Assert
        assertThat(client2.getIsDuplicate()).isTrue();
        verify(clientRepository).findById(clientId);
        verify(clientRepository).findById(primaryClientId);
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when client not found")
    void shouldThrowExceptionWhenClientNotFound() {
        // Arrange
        Long clientId = 999L;
        MarkDuplicateRequest request = new MarkDuplicateRequest();
        request.setDuplicateOfClientId(1L);

        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> duplicateDetectionService.markDuplicate(clientId, request, adminPrincipal))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Client not found");

        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when marking client as duplicate of itself")
    void shouldThrowExceptionWhenMarkingClientAsDuplicateOfItself() {
        // Arrange
        Long clientId = 1L;
        MarkDuplicateRequest request = new MarkDuplicateRequest();
        request.setDuplicateOfClientId(1L); // Same as clientId

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client1));

        // Act & Assert
        assertThatThrownBy(() -> duplicateDetectionService.markDuplicate(clientId, request, adminPrincipal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Client cannot be a duplicate of itself");

        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    @DisplayName("Should throw ForbiddenException when non-admin marks duplicate")
    void shouldThrowExceptionWhenNonAdminMarksDuplicate() {
        // Arrange
        Long clientId = 2L;
        MarkDuplicateRequest request = new MarkDuplicateRequest();
        request.setDuplicateOfClientId(1L);

        // Act & Assert
        assertThatThrownBy(() -> duplicateDetectionService.markDuplicate(clientId, request, therapistPrincipal))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only administrators and supervisors can mark duplicates");

        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    @DisplayName("Should throw exception when requester is null")
    void shouldThrowExceptionWhenRequesterIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> duplicateDetectionService.detectDuplicates(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Requester is required");
    }
}
